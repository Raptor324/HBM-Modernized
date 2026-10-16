package com.hbm_m.blockentity.network.pneumatic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.PneumoStorageClutterMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPneumoStorageClutter} (1.7.10): das Ramschlager.
 *
 * <p>Vierundfuenfzig ganz gewoehnliche Plaetze - jeder haelt einen Stapel, jeder nimmt alles an.
 * Damit ist es das Lager fuer alles, was in kleinen Mengen anfaellt; fuer grosse Mengen eines
 * einzigen Gegenstands gibt es das Massenlager.</p>
 */
public class PneumoStorageClutterBlockEntity extends PneumaticStorageBlockEntity {

    /** Original: {@code super(6 * 9)}. */
    public static final int INVENTORY_SIZE = 6 * 9;

    public PneumoStorageClutterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMO_STORAGE_CLUTTER_BE.get(), pos, state, INVENTORY_SIZE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PneumoStorageClutterBlockEntity be) {
        if (level.isClientSide()) return;
        be.tickStorage(level, pos);
        be.setChanged();
        be.sendUpdateToClient();
    }

    @Override
    public long getAmountAt(int index) {
        return getInventory().getStackInSlot(index).getCount();
    }

    /** Das Ramschlager nimmt ueber das Terminal auch neue Gegenstandsarten an. */
    @Override
    public boolean allowTypeSetting() {
        return true;
    }

    @Override
    public long useUpItem(int index, long amount) {
        ItemStack stack = getInventory().getStackInSlot(index);
        if (stack.isEmpty()) return amount;

        int toRemove = (int) Math.min(stack.getCount(), amount);
        stack.shrink(toRemove);
        setChanged();
        return amount - toRemove;
    }

    @Override
    public long addItem(int index, long amount) {
        ItemStack stack = getInventory().getStackInSlot(index);
        if (stack.isEmpty()) return amount;

        int capacity = Math.min(stack.getMaxStackSize(), getInventory().getSlotLimit(index));
        int toAdd = (int) Math.min(amount, capacity - stack.getCount());
        if (toAdd <= 0) return amount;

        stack.grow(toAdd);
        setChanged();
        return amount - toAdd;
    }

    @Override
    public long setupType(int index, ItemStack zeroStack, long amount) {
        int capacity = Math.min(zeroStack.getMaxStackSize(), getInventory().getSlotLimit(index));
        int finalSize = (int) Math.min(amount, capacity);

        ItemStack placed = zeroStack.copy();
        placed.setCount(finalSize);
        getInventory().setStackInSlot(index, placed);
        setChanged();

        return amount - finalSize;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pneumatic_storage_clutter");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PneumoStorageClutterMenu(id, inv, this);
    }
}
