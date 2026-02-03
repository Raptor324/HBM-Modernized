package com.hbm_m.mixin;

import com.hbm_m.powerarmor.ModEventHandlerClient;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When thermal vision is active, override gamma and darkness scale
 * so client brightness settings do not affect the render (same as Superb Warfare thermal).
 */
@Mixin(LightTexture.class)
public class LightTextureMixin {

    @Inject(method = "getDarknessGamma(F)F", at = @At("RETURN"), cancellable = true)
    private void hbm_m$getDarknessGamma(float pPartialTick, CallbackInfoReturnable<Float> cir) {
        if (ModEventHandlerClient.isThermalActive()) {
            cir.cancel();
            cir.setReturnValue(8f);
        }
    }

    @Inject(
        method = "calculateDarknessScale(Lnet/minecraft/world/entity/LivingEntity;FF)F",
        at = @At("RETURN"),
        cancellable = true
    )
    private void hbm_m$calculateDarknessScale(
        LivingEntity pEntity,
        float pGamma,
        float pPartialTick,
        CallbackInfoReturnable<Float> cir
    ) {
        if (ModEventHandlerClient.isThermalActive()) {
            cir.cancel();
            cir.setReturnValue(0.25f);
        }
    }
}
