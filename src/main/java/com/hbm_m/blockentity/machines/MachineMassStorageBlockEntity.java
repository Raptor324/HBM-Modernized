package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityMassStorage} (1.7.10 Original) - a single-item-type stockpile with a big
 * integer counter instead of individual item stacks. Slot 0 = input (auto-consumed into the
 * counter), slot 1 = filter (defines/locks the accepted item, can't change once stockpile &gt; 0),
 * slot 2 = output buffer (auto-refilled from the counter).
 * <p>
 * SCOPE-Vereinfachung: Das Original hat 4 Groessen-Stufen (Holz/Eisen/Stahl/Desh, je per Metadaten-
 * Subitem, 100 bis 1.000.000 Kapazitaet) - hier nur eine Stufe (100.000), da im modernisierten
 * Ressourcenbaum nur ein Texturset vorhanden ist. AE2-ME-Anbindung (getTotalStockpile/erhoehen/
 * verringern) und Redstone-Lock-Pin-System entfallen (keine Entsprechung in diesem Port).
 */
public class MachineMassStorageBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FILTER = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int INVENTORY_SIZE = 3;

    private static final long CAPACITY = 100_000L;

    private long stockpile = 0L;

    public MachineMassStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_MASS_STORAGE_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineMassStorageBlockEntity be) {
        if (level.isClientSide) return;

        ItemStack filter = be.inventory.getStackInSlot(SLOT_FILTER);
        if (filter.isEmpty()) return;
        Item type = filter.getItem();

        ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);
        if (!input.isEmpty() && input.getItem() == type && be.stockpile < CAPACITY) {
            long room = CAPACITY - be.stockpile;
            int toConsume = (int) Math.min(input.getCount(), room);
            if (toConsume > 0) {
                input.shrink(toConsume);
                be.stockpile += toConsume;
                be.setChanged();
            }
        }

        ItemStack output = be.inventory.getStackInSlot(SLOT_OUTPUT);
        //? if < 1.21.1 {
        int outputSpace = output.isEmpty() ? type.getMaxStackSize() : (output.getItem() == type ? type.getMaxStackSize() - output.getCount() : 0);
        //?} else {
        /*int outputSpace = output.isEmpty() ? type.getMaxStackSize(ItemStack.EMPTY) : (output.getItem() == type ? type.getMaxStackSize(ItemStack.EMPTY) - output.getCount() : 0);
        *///?}
        if (outputSpace > 0 && be.stockpile > 0) {
            int toRelease = (int) Math.min(outputSpace, be.stockpile);
            if (toRelease > 0) {
                if (output.isEmpty()) {
                    be.inventory.setStackInSlot(SLOT_OUTPUT, new ItemStack(type, toRelease));
                } else {
                    output.grow(toRelease);
                }
                be.stockpile -= toRelease;
                be.setChanged();
            }
        }
    }

    public long getStockpile() { return stockpile; }
    public long getCapacity() { return CAPACITY; }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == SLOT_FILTER) {
            ItemStack current = this.inventory.getStackInSlot(SLOT_FILTER);
            return stockpile <= 0 || current.isEmpty() || current.getItem() == stack.getItem();
        }
        if (slot == SLOT_INPUT) {
            ItemStack filter = this.inventory.getStackInSlot(SLOT_FILTER);
            return filter.isEmpty() || filter.getItem() == stack.getItem();
        }
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.mass_storage");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineMassStorageMenu.create(id, inventory, this);
    }

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putLong("stockpile", stockpile);
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tag.putLong("stockpile", stockpile);
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        stockpile = tag.getLong("stockpile");
    }
}
