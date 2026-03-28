package com.hbm_m.item;

import net.minecraft.world.item.Item;

/**
 * Mineral Pipe - A pipe item for a specific mineral type.
 * Uses the universal pipe.png texture tinted with the mineral's color via ItemColor handler.
 */
public class MineralPipeItem extends Item {

    private final int tintColor;

    public MineralPipeItem(Properties properties, int tintColor) {
        super(properties);
        this.tintColor = tintColor;
    }

    /** Returns the tint color for this pipe type. */
    public int getTintColor() {
        return this.tintColor;
    }
}
