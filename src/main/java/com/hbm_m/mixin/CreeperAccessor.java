package com.hbm_m.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Доступ к private-полям {@link Creeper} без MethodHandles (ломается в production/SRG).
 */
@Mixin(Creeper.class)
public interface CreeperAccessor {

    @Accessor("DATA_IS_POWERED")
    static EntityDataAccessor<Boolean> hbm_m$getDataIsPowered() {
        throw new AssertionError();
    }

    @Accessor("maxSwell")
    void hbm_m$setMaxSwell(int maxSwell);
}
