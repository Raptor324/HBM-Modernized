package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.NukeFleijaMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BE бомбы FLEIJA: 11 слотов — 2 детонатора, 3 ускорителя, 6 ядер.
 */
public class NukeFleijaBlockEntity extends NukeBaseBlockEntity {

    public static final int SLOTS = 11;

    public NukeFleijaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_FLEIJA_BE.get(), pos, state, SLOTS);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_fleija");
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        var item = stack.getItem();
        return item == ModItems.FLEIJA_IGNITER.get()
                || item == ModItems.FLEIJA_PROPELLANT.get()
                || item == ModItems.FLEIJA_CORE.get();
    }

    @Override
    public boolean isReady() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (slots.get(slot).isEmpty()) return false;
        }
        return slots.get(0).is(ModItems.FLEIJA_IGNITER.get())
                && slots.get(1).is(ModItems.FLEIJA_IGNITER.get())
                && slots.get(2).is(ModItems.FLEIJA_PROPELLANT.get())
                && slots.get(3).is(ModItems.FLEIJA_PROPELLANT.get())
                && slots.get(4).is(ModItems.FLEIJA_PROPELLANT.get())
                && slots.get(5).is(ModItems.FLEIJA_CORE.get())
                && slots.get(6).is(ModItems.FLEIJA_CORE.get())
                && slots.get(7).is(ModItems.FLEIJA_CORE.get())
                && slots.get(8).is(ModItems.FLEIJA_CORE.get())
                && slots.get(9).is(ModItems.FLEIJA_CORE.get())
                && slots.get(10).is(ModItems.FLEIJA_CORE.get());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NukeFleijaMenu(id, inventory, this);
    }
}
