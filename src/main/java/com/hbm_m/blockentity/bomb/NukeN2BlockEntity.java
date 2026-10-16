package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.NukeN2Menu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BE мины N2: 12 слотов, все — заряды N2.
 */
public class NukeN2BlockEntity extends NukeBaseBlockEntity {

    public static final int SLOTS = 12;

    public NukeN2BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_N2_BE.get(), pos, state, SLOTS);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_n2");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModItems.N2_CHARGE.get());
    }

    @Override
    public boolean isReady() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (!slots.get(slot).is(ModItems.N2_CHARGE.get())) return false;
        }
        return true;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NukeN2Menu(id, inventory, this);
    }
}
