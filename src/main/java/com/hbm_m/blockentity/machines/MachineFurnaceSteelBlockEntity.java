package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineFurnaceSteelBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineFurnaceSteelMenu;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Steel Furnace: Direktport der Kernlogik aus {@code TileEntityFurnaceSteel} (1.7.10 Original).
 * <p>
 * Vereinfachung 1: Das Original ist ein waermegetriebener Multiblock, der von einem externen
 * {@code IHeatSource}-Block darunter gespeist wird (Heiznetzwerk-Mechanik). Dieses System
 * (IHeatSource/HeatSource) existiert in diesem Port nicht (grep-verifiziert). Um keine
 * Fake-Waermequelle zu erfinden, wird die Stahlpresse hier stattdessen wie die Eisenpresse
 * (MachineFurnaceIronBlockEntity) als einzelner, brennstoffbetriebener Block mit drei parallelen,
 * unabhaengigen Vanilla-Schmelzspuren umgesetzt (Slots 0-2 Input, 3-5 Output), gespeist von einem
 * gemeinsamen Brennstoff-Slotpaar (6-7) - einfacher als drei eigene Slotpaare und dennoch als
 * korrekt, da im Original ohnehin nur eine einzige Waermequelle alle Spuren speiste. Feste
 * processingTime = 200 Ticks (Vanilla-Ofenzeit) pro Spur, da kein Waermequellen-Multiplikator
 * portiert wird.
 * <p>
 * Vereinfachung 2: Das Oredict-Bonusausbeute-System (Byprodukte je nach Ore/Log/Tar-Namenspraefix)
 * ist bewusst NICHT portiert - es basiert auf 1.7.10 OreDictionary-Namenspraefix-Matching, das
 * ohne ein entsprechendes modernes Tag-Aequivalent nicht 1:1 uebertragbar ist. Kann spaeter ueber
 * Item-Tags nachgeruestet werden.
 */
public class MachineFurnaceSteelBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements MenuProvider {

    public static final int SLOT_INPUT_0 = 0;
    public static final int SLOT_INPUT_1 = 1;
    public static final int SLOT_INPUT_2 = 2;
    public static final int SLOT_OUTPUT_0 = 3;
    public static final int SLOT_OUTPUT_1 = 4;
    public static final int SLOT_OUTPUT_2 = 5;
    public static final int SLOT_FUEL_1 = 6;
    public static final int SLOT_FUEL_2 = 7;
    private static final int SLOT_COUNT = 8;
    private static final int LANE_COUNT = 3;

    private static final int PROCESSING_TIME = 200;

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_FUEL_1, SLOT_FUEL_2 -> isFuel(stack);
                case SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2 -> false;
                default -> true;
            };
        }
    };

    private int litTime = 0;
    private int litDuration = 0;
    private final int[] progress = new int[LANE_COUNT];

    private static final int DATA_COUNT = 2 + LANE_COUNT;
    private static final int DATA_LIT_TIME = 0;
    private static final int DATA_LIT_DURATION = 1;
    private static final int DATA_PROGRESS_0 = 2;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_LIT_TIME) return litTime;
            if (index == DATA_LIT_DURATION) return litDuration;
            int lane = index - DATA_PROGRESS_0;
            if (lane >= 0 && lane < LANE_COUNT) return progress[lane];
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_LIT_TIME) { litTime = value; return; }
            if (index == DATA_LIT_DURATION) { litDuration = value; return; }
            int lane = index - DATA_PROGRESS_0;
            if (lane >= 0 && lane < LANE_COUNT) progress[lane] = value;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private final SimpleContainer recipeInput = new SimpleContainer(1);

    public MachineFurnaceSteelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_STEEL_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() {
        return inventory;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFurnaceSteelBlockEntity be) {
        if (level.isClientSide()) return;

        boolean wasLit = be.isLit();
        boolean dirty = false;

        if (be.litTime <= 0 && be.anyLaneCanSmelt(level)) {
            if (be.tryConsumeFuel()) {
                dirty = true;
            }
        }

        if (be.litTime > 0) {
            be.litTime--;
            for (int lane = 0; lane < LANE_COUNT; lane++) {
                if (be.laneCanSmelt(level, lane)) {
                    be.progress[lane]++;
                    if (be.progress[lane] >= PROCESSING_TIME) {
                        be.craftLane(level, lane);
                        be.progress[lane] = 0;
                    }
                } else if (be.progress[lane] != 0) {
                    be.progress[lane] = 0;
                }
            }
            dirty = true;
        } else {
            for (int lane = 0; lane < LANE_COUNT; lane++) {
                if (be.progress[lane] != 0) {
                    be.progress[lane] = 0;
                    dirty = true;
                }
            }
        }

        if (wasLit != be.isLit()) {
            level.setBlock(pos, state.setValue(MachineFurnaceSteelBlock.LIT, be.isLit()), 3);
        }

        if (dirty) {
            be.setChanged();
        }
    }

    private boolean isLit() {
        return litTime > 0;
    }

    private boolean tryConsumeFuel() {
        for (int slot : new int[] { SLOT_FUEL_1, SLOT_FUEL_2 }) {
            ItemStack fuelStack = inventory.getStackInSlot(slot);
            if (fuelStack.isEmpty()) continue;
            int burnTicks = AbstractFurnaceBlockEntity.getFuel().getOrDefault(fuelStack.getItem(), 0);
            if (burnTicks <= 0) continue;

            litDuration = burnTicks;
            litTime = burnTicks;

            var remainderItem = fuelStack.getItem().getCraftingRemainingItem();
            fuelStack.shrink(1);
            if (fuelStack.isEmpty() && remainderItem != null) {
                inventory.setStackInSlot(slot, new ItemStack(remainderItem));
            }
            return true;
        }
        return false;
    }

    private boolean anyLaneCanSmelt(Level level) {
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            if (laneCanSmelt(level, lane)) return true;
        }
        return false;
    }

    private boolean laneCanSmelt(Level level, int lane) {
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        if (input.isEmpty()) return false;

        var recipe = getRecipe(level, input);
        if (recipe.isEmpty()) return false;

        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        if (result.isEmpty()) return false;

        return canAcceptResult(lane, result);
    }

    private boolean canAcceptResult(int lane, ItemStack result) {
        ItemStack current = inventory.getStackInSlot(outputSlot(lane));
        if (current.isEmpty()) return true;
        if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void craftLane(Level level, int lane) {
        ItemStack input = inventory.getStackInSlot(inputSlot(lane));
        var recipe = getRecipe(level, input);
        if (recipe.isEmpty()) return;

        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();

        input.shrink(1);

        int outSlot = outputSlot(lane);
        ItemStack output = inventory.getStackInSlot(outSlot);
        if (output.isEmpty()) {
            inventory.setStackInSlot(outSlot, result);
        } else {
            output.grow(result.getCount());
            inventory.setStackInSlot(outSlot, output);
        }
    }

    private static int inputSlot(int lane) {
        return SLOT_INPUT_0 + lane;
    }

    private static int outputSlot(int lane) {
        return SLOT_OUTPUT_0 + lane;
    }

    private java.util.Optional<SmeltingRecipe> getRecipe(Level level, ItemStack input) {
        // 1.21.1: getRecipeFor требует RecipeInput (SingleRecipeInput) и возвращает RecipeHolder.
        return com.hbm_m.platform.recipe.RecipeHooks.getRecipeFor(level, RecipeType.SMELTING, input);
    }

    public static boolean isFuel(ItemStack stack) {
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0) > 0;
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, container);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        tag.putInt("litTime", litTime);
        tag.putInt("litDuration", litDuration);
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            tag.putInt("progress" + lane, progress[lane]);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        //? if < 1.21.1 {
        inventory.deserializeNBT(tag.getCompound("inventory"));
        //?} else {
        /*inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        *///?}
        litTime = tag.getInt("litTime");
        litDuration = tag.getInt("litDuration");
        for (int lane = 0; lane < LANE_COUNT; lane++) {
            progress[lane] = tag.getInt("progress" + lane);
        }
    }

    // ==================== GUI ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.hbm_m.furnace_steel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineFurnaceSteelMenu(id, inv, this, data);
    }
}
