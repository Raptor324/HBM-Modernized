package com.hbm_m.client.render;

/**
 * Duck interface stamped on {@link net.minecraft.world.level.block.entity.BlockEntity}
 * to provide O(1) bypass lookups without HashSet.contains() overhead.
 */
public interface NucleusTrackedBlockEntity {

    boolean hbm_m$isNucleusRegistered();

    void hbm_m$setNucleusRegistered(boolean registered);

    default Object hbm_m$getRenderState() {
        return null;
    }

    default void hbm_m$setRenderState(Object state) {
    }

    default Object hbm_m$getBoundsEntry() {
        return null;
    }

    default void hbm_m$setBoundsEntry(Object entry) {
    }
}
