package com.hbm_m.blockentity.machines.rbmk;

import net.minecraft.world.item.ItemStack;

public interface IRBMKLoadable {

    boolean canLoad(ItemStack toLoad);

    void load(ItemStack toLoad);

    boolean canUnload();

    ItemStack provideNext();

    void unload();
}

