package com.hbm_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.handler.EntityEffectHandler;

import net.minecraft.world.entity.LivingEntity;

/**
 * Серверный тик живых сущностей для радиационных эффектов (аналог {@code LivingUpdateEvent} → {@code EntityEffectHandler.onUpdate} в 1.7.10).
 */
@Mixin(LivingEntity.class)
public class LivingEntityRadiationMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void hbm_m$radiationTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && ModClothConfig.get().enableRadiation) {
            EntityEffectHandler.onUpdate(self);
        }
    }
}
