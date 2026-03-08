package com.hbm_m.api.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Interface for items that can designate a target position (e.g. for launch pads).
 * Port from 1.7.10 api.hbm.item.IDesignatorItem.
 */
public interface IDesignatorItem {

    /**
     * Whether the designator has a valid target.
     *
     * @param level for dimension/entity checks if needed
     * @param stack to read NBT
     * @param x     launch pad X
     * @param y     launch pad Y
     * @param z     launch pad Z
     * @return true if target is set and valid
     */
    boolean isReady(Level level, ItemStack stack, int x, int y, int z);

    /**
     * Target position when the designator is ready.
     *
     * @param level world
     * @param stack designator stack
     * @param x     launch pad X
     * @param y     launch pad Y
     * @param z     launch pad Z
     * @return target coordinates (Y is typically 0 for ground target)
     */
    Vec3 getCoords(Level level, ItemStack stack, int x, int y, int z);
}
