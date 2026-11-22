package com.hbm_m.client.render;

/**
 * Duck interface mixed into {@link net.minecraft.world.level.block.entity.BlockEntityType}
 * (see {@code com.hbm_m.mixin.client.MixinBlockEntityType}): replaces the
 * MANAGED_TYPES HashSet probe in {@link NucleusDispatcherBypass} with a plain
 * field read on the per-BE-per-frame hot path. The flag is written once at
 * spec registration ({@code NucleusDispatcherBypass.registerManaged}); every
 * type that never registers (all vanilla and third-party BEs) keeps the
 * {@code false} default, which matches the failed contains() lookup it
 * replaces.
 */
public interface NucleusManagedType {

    boolean hbm_m$isNucleusManaged();

    void hbm_m$setNucleusManaged(boolean managed);
}
