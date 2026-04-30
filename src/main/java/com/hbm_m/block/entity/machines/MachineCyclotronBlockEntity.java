package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineCyclotronMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineCyclotronBlockEntity extends BaseMachineBlockEntity {

    public MachineCyclotronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CYCLOTRON_BE.get(), pos, state, 0, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCyclotronBlockEntity blockEntity) {
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.cyclotron");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCyclotronMenu.create(id, inventory, this);
    }
}