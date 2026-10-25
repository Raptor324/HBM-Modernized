package com.hbm_m.item.weapon;

import net.minecraft.world.item.Item;

/** 1:1 {@code com.hbm.items.weapon.ItemGrenade}: Wurfgranate mit Zuender in Sekunden, stapelt bis 16. */
public class ItemGrenade extends Item {

    public int fuse = 4;

    public ItemGrenade(int fuse, Properties properties) {
        super(properties.stacksTo(16));
        this.fuse = fuse;
    }

    public static int getFuseTicks(Item grenade) {
        return ((ItemGrenade) grenade).fuse * 20;
    }
}
