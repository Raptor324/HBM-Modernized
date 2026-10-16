package com.hbm_m.mixin.client;

import com.hbm_m.client.render.NucleusManagedType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Carries the Nucleus "managed type" flag on every BlockEntityType instance
 * (defaults to {@code false} - only types registered through
 * {@code NucleusDispatcherBypass.registerManaged} are flipped). Backs the
 * dispatcher-bypass gate with a field read instead of a per-BE-per-frame
 * HashSet probe.
 */
@Mixin(BlockEntityType.class)
public class MixinBlockEntityType implements NucleusManagedType {

    @Unique
    private boolean hbm_m$nucleusManaged;

    @Override
    public boolean hbm_m$isNucleusManaged() {
        return this.hbm_m$nucleusManaged;
    }

    @Override
    public void hbm_m$setNucleusManaged(boolean managed) {
        this.hbm_m$nucleusManaged = managed;
    }
}
