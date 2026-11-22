package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineFunnelMenu;
import com.hbm_m.recipe.index.CachedRecipeResolver;
import com.hbm_m.util.SimpleCraftingContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Funnel ("Combinator Funnel") - Port von {@code TileEntityMachineFunnel} (1.7.10 Original). Kein
 * Hopper- oder Foerderband-Bezug trotz des Namens: eine passive Automations-Maschine, die in 9
 * gepaarten Input/Output-Slots stapelweise Rohmaterial zu seinem gecrafteten Vanilla-Aequivalent
 * "verdichtet" (z.B. 9x Eisenbarren -&gt; Eisenblock, 4x Bretter -&gt; die entsprechende 2x2-Rezept-
 * Ausgabe), ueber das echte Vanilla-{@link RecipeType#CRAFTING}-System (kein eigenes Rezeptformat,
 * analog zu {@link MachineAutocrafterBlockEntity}). {@link #mode} (0=3x3-dann-2x2, 1=nur-3x3,
 * 2=nur-2x2) ist 1:1 aus dem Original, per GUI-Knopf umschaltbar.
 * <p>
 * Kein Strom, kein Treibstoff - das Original ist rein inventar-getrieben (Hopper/Rohre fuellen die
 * Input-Slots 0-8, entnehmen aus den Output-Slots 9-17).
 */
public class MachineFunnelBlockEntity extends BaseMachineBlockEntity {

    private static final int INPUT_START = 0;
    private static final int SLOT_PAIRS = 9;
    private static final int OUTPUT_START = 9;
    private static final int SLOT_COUNT = 18;

    private static final int MODE_3X3_THEN_2X2 = 0;
    private static final int MODE_3X3_ONLY = 1;
    private static final int MODE_2X2_ONLY = 2;

    private int mode = MODE_3X3_THEN_2X2;

    // Кэш скана CRAFTING-рецепта: по одному резолверу на (пара слотов, размер сетки),
    // ключ — фингерпринт входного стека + объём сетки. Пока вход не изменился, скан
    // RecipeManager не повторяется; проверка выходного слота остаётся per-tick.
    @SuppressWarnings("unchecked")
    private final CachedRecipeResolver<CraftingRecipe>[] gridResolvers =
            new CachedRecipeResolver[SLOT_PAIRS * 2];

    public MachineFunnelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUNNEL_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFunnelBlockEntity be) {
        if (level.isClientSide) return;
        be.serverTick(level);
    }

    private void serverTick(Level level) {
        // 1:1 Original: alle 9 Slot-Paare pro Tick durchgehen. Wenn ein Paar verarbeitet
        // werden konnte, wird sofort der Block getriggert (setChanged) und der Tick fuer
        // diesen Trichter ist fertig - pro Tick hoechstens eine Verdichtung.
        for (int i = 0; i < SLOT_PAIRS; i++) {
            if (processSlotPair(level, i)) {
                setChanged();
                return;
            }
        }
    }

    private boolean processSlotPair(Level level, int index) {
        int inputSlot = INPUT_START + index;
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) return false;

        if (mode == MODE_3X3_THEN_2X2 || mode == MODE_3X3_ONLY) {
            if (tryCompact(level, index, input, 3, 3, 9)) return true;
        }

        if (mode == MODE_3X3_THEN_2X2 || mode == MODE_2X2_ONLY) {
            if (tryCompact(level, index, input, 2, 2, 4)) return true;
        }

        return false;
    }

    private boolean tryCompact(Level level, int index, ItemStack input, int width, int height, int count) {
        if (input.getCount() < count) return false;

        int inputSlot = INPUT_START + index;

        long sig = CachedRecipeResolver.sign(index,
                com.hbm_m.platform.StackNbt.fingerprint(input), count);
        // Scanner чистый: только матч по входам из подписи; выходной слот проверяется per-tick ниже.
        CraftingRecipe recipe = gridResolver(index, width).resolve(level, RecipeType.CRAFTING, sig, (lvl, all) -> {
            NonNullList<ItemStack> gridItems = NonNullList.withSize(width * height, ItemStack.EMPTY);
            ItemStack sample = input.copyWithCount(1);
            for (int i = 0; i < count; i++) {
                gridItems.set(i, sample);
            }
            // 1.21.1: getRecipeFor/assemble требуют CraftingInput — хелперы RecipeHooks.
            return com.hbm_m.platform.recipe.RecipeHooks.getCraftingRecipeFor(
                    lvl, new SimpleCraftingContainer(gridItems, width, height)).orElse(null);
        });
        if (recipe == null) return false;

        SimpleCraftingContainer grid = buildGrid(input, count, width, height);

        ItemStack result = com.hbm_m.platform.recipe.RecipeHooks.assembleCrafting(recipe, grid, level);
        if (result.isEmpty()) return false;

        int outputSlot = OUTPUT_START + index;
        ItemStack currentOutput = inventory.getStackInSlot(outputSlot);

        if (!currentOutput.isEmpty()) {
            if (!com.hbm_m.platform.StackNbt.sameItemSameTags(currentOutput, result)) return false;
            if (currentOutput.getCount() + result.getCount() > currentOutput.getMaxStackSize()) return false;
        }

        input.shrink(count);
        inventory.setStackInSlot(inputSlot, input);

        if (currentOutput.isEmpty()) {
            inventory.setStackInSlot(outputSlot, result.copy());
        } else {
            currentOutput.grow(result.getCount());
            inventory.setStackInSlot(outputSlot, currentOutput);
        }

        return true;
    }

    private SimpleCraftingContainer buildGrid(ItemStack input, int count, int width, int height) {
        NonNullList<ItemStack> gridItems = NonNullList.withSize(width * height, ItemStack.EMPTY);
        ItemStack sample = input.copyWithCount(1);
        for (int i = 0; i < count; i++) {
            gridItems.set(i, sample);
        }
        return new SimpleCraftingContainer(gridItems, width, height);
    }

    private CachedRecipeResolver<CraftingRecipe> gridResolver(int index, int width) {
        int slot = index * 2 + (width >= 3 ? 1 : 0);
        if (gridResolvers[slot] == null) gridResolvers[slot] = new CachedRecipeResolver<>();
        return gridResolvers[slot];
    }

    public void cycleMode() {
        mode = (mode + 1) % 3;
        setChanged();
        sendUpdateToClient();
    }

    public int getMode() {
        return mode;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("mode", mode);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        mode = tag.getInt("mode");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.funnel");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= INPUT_START && slot < INPUT_START + SLOT_PAIRS;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineFunnelMenu(containerId, playerInventory, this);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: unten {9-17}, sonst {0-8}; unten nur Ausgaenge, seitlich Eingaenge heraus, oben nichts. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return side == net.minecraft.core.Direction.DOWN ? com.hbm_m.blockentity.SidedItemAccess.range(9, 17) : com.hbm_m.blockentity.SidedItemAccess.range(0, 8); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot < 9 && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { if (side == net.minecraft.core.Direction.DOWN) return slot > 8; return side != net.minecraft.core.Direction.UP && slot < 9; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: unten {9-17}, sonst {0-8}; unten nur Ausgaenge, seitlich Eingaenge heraus, oben nichts. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return side == net.minecraft.core.Direction.DOWN ? com.hbm_m.blockentity.SidedItemAccess.range(9, 17) : com.hbm_m.blockentity.SidedItemAccess.range(0, 8); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot < 9 && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { if (side == net.minecraft.core.Direction.DOWN) return slot > 8; return side != net.minecraft.core.Direction.UP && slot < 9; }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}
}
