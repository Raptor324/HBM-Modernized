package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineTurbofanBlockEntity extends BaseMachineBlockEntity {
    public MachineTurbofanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBOFAN_BE.get(), pos, state, 4, 2_000_000L, 0L, 80_000L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTurbofanBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.ensureNetworkInitialized();
        be.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }
}
