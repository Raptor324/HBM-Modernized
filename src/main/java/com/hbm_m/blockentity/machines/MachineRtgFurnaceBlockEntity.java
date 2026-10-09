package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineRtgFurnaceBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.machine.ItemRTGPellet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityRtgFurnace}: Ofen mit Vanilla-Schmelzrezepten, beheizt von drei RTG-Pellets. Jedes eingelegte
 * Pellet reicht ({@code RTGUtil.hasHeat}); der Fortschritt waechst je Tick um die Summe der Pelletwaerme bis 1000.
 * Die Pellets altern jeden Tick, auch ohne Arbeit. Slots: 0 Eingang (oben), 1-3 Pellets (Seiten), 4 Ausgang (unten).
 */
public class MachineRtgFurnaceBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 5;
    public static final int processingSpeed = 1000;

    private static final int[] slots_top = new int[] { 0 };
    private static final int[] slots_bottom = new int[] { 4 };
    private static final int[] slots_side = new int[] { 1, 2, 3 };

    public int dualCookTime;

    public MachineRtgFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_RTG_FURNACE_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRtgFurnaceBlockEntity be) {
        if (level.isClientSide) return;
        be.serverTick(level, pos, state);
    }

    /** Original {@code updateEntity}. */
    private void serverTick(Level level, BlockPos pos, BlockState state) {
        boolean flag1 = false;

        if (hasPower() && canProcess()) {
            dualCookTime += ItemRTGPellet.updateRTGs(inventory, slots_side);

            if (this.dualCookTime >= processingSpeed) {
                this.dualCookTime = 0;
                this.processItem();
                flag1 = true;
            }
        } else {
            dualCookTime = 0;
            ItemRTGPellet.updateRTGs(inventory, slots_side);
        }

        boolean trigger = true;

        if (hasPower() && canProcess() && this.dualCookTime == 0) {
            trigger = false;
        }

        if (trigger) {
            flag1 = true;
            // Original MachineRtgFurnace.updateBlockState: an/aus je nach dualCookTime
            boolean lit = this.dualCookTime > 0;
            if (state.getValue(MachineRtgFurnaceBlock.LIT) != lit) {
                level.setBlock(pos, state.setValue(MachineRtgFurnaceBlock.LIT, lit), 2);
            }
        }

        if (flag1) {
            this.setChanged();
        }
    }

    public boolean isLoaded() {
        return ItemRTGPellet.hasHeat(inventory, slots_side);
    }

    public boolean hasPower() {
        return isLoaded();
    }

    public boolean isProcessing() {
        return this.dualCookTime > 0;
    }

    public int getDiFurnaceProgressScaled(int i) {
        return (dualCookTime * i) / processingSpeed;
    }

    private Optional<SmeltingRecipe> getRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) return Optional.empty();
        return com.hbm_m.platform.recipe.RecipeHooks.getRecipeFor(level, RecipeType.SMELTING, input);
    }

    public boolean canProcess() {
        ItemStack in = inventory.getStackInSlot(0);
        if (in.isEmpty()) return false;

        Optional<SmeltingRecipe> recipe = getRecipe(in);
        if (recipe.isEmpty()) return false;
        ItemStack itemStack = recipe.get().getResultItem(level.registryAccess());
        if (itemStack.isEmpty()) return false;

        ItemStack out = inventory.getStackInSlot(4);
        if (out.isEmpty()) return true;

        if (!ItemStack.isSameItem(out, itemStack)) return false;

        if (out.getCount() < 64 && out.getCount() < out.getMaxStackSize()) {
            return true;
        } else {
            return out.getCount() < itemStack.getMaxStackSize();
        }
    }

    private void processItem() {
        if (canProcess()) {
            ItemStack itemStack = getRecipe(inventory.getStackInSlot(0)).get().getResultItem(level.registryAccess());
            ItemStack out = inventory.getStackInSlot(4);

            if (out.isEmpty()) {
                inventory.setStackInSlot(4, itemStack.copy());
            } else if (ItemStack.isSameItem(out, itemStack)) {
                ItemStack grown = out.copy();
                grown.grow(itemStack.getCount());
                inventory.setStackInSlot(4, grown);
            }

            ItemStack in = inventory.getStackInSlot(0).copy();
            in.shrink(1);
            inventory.setStackInSlot(0, in.isEmpty() ? ItemStack.EMPTY : in);
        }
    }

    /** Original {@code isItemValidForSlot}: alles ueberall. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    /** Original {@code getAccessibleSlotsFromSide}: unten Ausgang, oben Eingang, Seiten Pellets. */
    private static int[] accessible(Direction side) {
        return side == Direction.DOWN ? slots_bottom : (side == Direction.UP ? slots_top : slots_side);
    }

    private static boolean isAccessible(int slot, Direction side) {
        for (int s : accessible(side)) if (s == slot) return true;
        return false;
    }

    /** Original {@code canExtractItem}: {@code j != 0 || i != 1 || Eimer}. */
    private boolean canExtractFrom(int slot, Direction side) {
        return side != Direction.DOWN || slot != 1 || inventory.getStackInSlot(slot).getItem() == Items.BUCKET;
    }

    //? if forge {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return inventory.getSlots(); }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (!isAccessible(slot, d) || !isItemValidForSlot(slot, stack)) return stack;
                    return inventory.insertItem(slot, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (!isAccessible(slot, d) || !canExtractFrom(slot, d)) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isAccessible(slot, d) && isItemValidForSlot(slot, stack); }
            })).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sided.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        sided.clear();
    }
    //?} elif neoforge {
    /*private final Map<Direction, com.hbm_m.platform.LazyCap<net.neoforged.neoforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> com.hbm_m.platform.LazyCap.of(() -> new net.neoforged.neoforge.items.IItemHandler() {
                @Override public int getSlots() { return inventory.getSlots(); }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (!isAccessible(slot, d) || !isItemValidForSlot(slot, stack)) return stack;
                    return inventory.insertItem(slot, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (!isAccessible(slot, d) || !canExtractFrom(slot, d)) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isAccessible(slot, d) && isItemValidForSlot(slot, stack); }
            })).cast();
        }
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sided.values().forEach(com.hbm_m.platform.LazyCap::invalidate);
        sided.clear();
    }
    *///?}

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.rtg_furnace");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineRtgFurnaceMenu.create(id, inventory, this);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putShort("cookTime", (short) dualCookTime);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        // Original liest "CookTime", schreibt aber "cookTime" - der Fortschritt ging beim Laden also immer verloren.
        dualCookTime = tag.getShort("CookTime");
    }
}
