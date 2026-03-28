package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Refinery BlockEntity - processes crude oil into petroleum products.
 */
public class MachineRefineryBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 4;

    private static final long ENERGY_CAPACITY = 500_000L;
    private static final long ENERGY_RECEIVE_RATE = 10_000L;

    public MachineRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFINERY_BE.get(), pos, state,
              INVENTORY_SIZE, ENERGY_CAPACITY, ENERGY_RECEIVE_RATE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRefineryBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();
        be.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.refinery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.refinery");
    }
}
