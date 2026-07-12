package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.NukePrototypeMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class NukePrototypeBlockEntity extends NukeBaseBlockEntity {

    public NukePrototypeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_PROTOTYPE_BE.get(), pos, state, 14);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var item = stack.getItem();
        return item == ModItems.CELL_SAS3.get()
            || item == ModItems.ROD_QUAD_LEAD.get()
            || item == ModItems.ROD_QUAD_NP237.get()
            || item == ModItems.ROD_QUAD_URANIUM.get();
    }

    @Override
    public boolean isReady() {
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_prototype");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NukePrototypeMenu(id, inventory, this);
    }
}
