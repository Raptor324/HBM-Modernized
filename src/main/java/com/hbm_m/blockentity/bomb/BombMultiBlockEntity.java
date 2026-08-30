package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.BombMultiMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BE многоцелевой бомбы: 4 заряда ТНТ по углам (слоты 0-3) + 2 модификатора.
 */
public class BombMultiBlockEntity extends NukeBaseBlockEntity {

    public static final int SLOTS = 6;

    public BombMultiBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOMB_MULTI_BE.get(), pos, state, SLOTS);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.bomb_multi");
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 4) return stack.is(Blocks.TNT.asItem());
        var item = stack.getItem();
        return item == Items.GUNPOWDER || item == Items.TNT
                || item == Items.FIRE_CHARGE || item == ModItems.PELLET_GAS.get();
    }

    @Override
    public boolean isReady() {
        return slots.get(0).is(Items.TNT) && slots.get(1).is(Items.TNT)
                && slots.get(2).is(Items.TNT) && slots.get(3).is(Items.TNT);
    }

    /** Модификатор мощности (слот 4): 0 = нет, 1 = порох, 2 = ТНТ. */
    public int return2type() {
        ItemStack stack = slots.get(4);
        if (stack.is(Items.GUNPOWDER)) return 1;
        if (stack.is(Items.TNT)) return 2;
        return 0;
    }

    /** Модификатор эффекта (слот 5): 0 = нет, 4 = огонь, 6 = газ. */
    public int return5type() {
        ItemStack stack = slots.get(5);
        if (stack.is(Items.FIRE_CHARGE)) return 4;
        if (stack.is(ModItems.PELLET_GAS.get())) return 6;
        return 0;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BombMultiMenu(id, inventory, this);
    }
}
