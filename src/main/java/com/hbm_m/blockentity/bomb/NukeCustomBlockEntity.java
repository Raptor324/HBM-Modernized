package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.NukeCustomMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BE кастомной бомбы: 27 произвольных слотов.
 */
public class NukeCustomBlockEntity extends NukeBaseBlockEntity {

    public static final int SLOTS = 27;

    public NukeCustomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_CUSTOM_BE.get(), pos, state, SLOTS);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_custom");
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Любой предмет — состав определяет характер взрыва.
        return true;
    }

    @Override
    public boolean isReady() {
        return !isEmpty();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NukeCustomMenu(id, inventory, this);
    }
}
