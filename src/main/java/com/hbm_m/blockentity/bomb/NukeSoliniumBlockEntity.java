package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.NukeSoliniumMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BE солиниевой бомбы: 9 слотов крестом — 4 детонатора по углам,
 * 4 ускорителя по сторонам, ядро в центре.
 */
public class NukeSoliniumBlockEntity extends NukeBaseBlockEntity {

    public static final int SLOTS = 9;

    public NukeSoliniumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_SOLINIUM_BE.get(), pos, state, SLOTS);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_solinium");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var item = stack.getItem();
        return item == ModItems.SOLINIUM_IGNITER.get()
                || item == ModItems.SOLINIUM_PROPELLANT.get()
                || item == ModItems.SOLINIUM_CORE.get();
    }

    @Override
    public boolean isReady() {
        return slots.get(0).is(ModItems.SOLINIUM_IGNITER.get())
                && slots.get(1).is(ModItems.SOLINIUM_PROPELLANT.get())
                && slots.get(2).is(ModItems.SOLINIUM_PROPELLANT.get())
                && slots.get(3).is(ModItems.SOLINIUM_IGNITER.get())
                && slots.get(4).is(ModItems.SOLINIUM_CORE.get())
                && slots.get(5).is(ModItems.SOLINIUM_IGNITER.get())
                && slots.get(6).is(ModItems.SOLINIUM_PROPELLANT.get())
                && slots.get(7).is(ModItems.SOLINIUM_PROPELLANT.get())
                && slots.get(8).is(ModItems.SOLINIUM_IGNITER.get());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NukeSoliniumMenu(id, inventory, this);
    }
}
