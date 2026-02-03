package com.hbm_m.mixin;

import com.hbm_m.powerarmor.ModEventHandlerClient;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // Этот метод отвечает за силу эффекта ночного видения.
    // Мы перехватываем его и говорим "1.0" (полная сила), если включен тепловизор.
    @Inject(method = "getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At("RETURN"), cancellable = true)
    private static void hbm_m$getNightVisionScale(LivingEntity pLivingEntity, float pNanoTime, CallbackInfoReturnable<Float> cir) {
        if (ModEventHandlerClient.isThermalActive()) {
            cir.cancel();
            cir.setReturnValue(1.0f);
        }
    }
}