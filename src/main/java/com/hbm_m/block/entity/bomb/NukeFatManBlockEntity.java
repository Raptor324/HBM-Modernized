package com.hbm_m.block.entity.bomb;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.menu.NukeFatManMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class NukeFatManBlockEntity extends NukeBaseBlockEntity {

    public NukeFatManBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_FAT_MAN_BE.get(), pos, state, 6);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_fat_man");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var item = stack.getItem();
        return item == ModItems.FAT_MAN_EXPLOSIVE.get()
                || item == ModItems.FAT_MAN_IGNITER.get()
                || item == ModItems.FAT_MAN_CORE.get();
    }

    @Override
    public boolean isReady() {
        return slots.get(0).getItem() == ModItems.FAT_MAN_EXPLOSIVE.get()
                && slots.get(1).getItem() == ModItems.FAT_MAN_EXPLOSIVE.get()
                && slots.get(2).getItem() == ModItems.FAT_MAN_EXPLOSIVE.get()
                && slots.get(3).getItem() == ModItems.FAT_MAN_EXPLOSIVE.get()
                && slots.get(4).getItem() == ModItems.FAT_MAN_IGNITER.get()
                && slots.get(5).getItem() == ModItems.FAT_MAN_CORE.get();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NukeFatManMenu(id, inventory, this);
    }
}
