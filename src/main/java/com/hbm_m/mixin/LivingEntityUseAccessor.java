package com.hbm_m.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Zugriff auf den Benutzungszaehler (Original: {@code EntityPlayer.itemInUseCount}), z.B. fuer {@code ItemBDCL}. */
@Mixin(LivingEntity.class)
public interface LivingEntityUseAccessor {

    @Accessor("useItemRemaining")
    int hbm_m$getUseItemRemaining();

    @Accessor("useItemRemaining")
    void hbm_m$setUseItemRemaining(int value);
}
