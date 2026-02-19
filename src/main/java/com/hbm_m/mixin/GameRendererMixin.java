package com.hbm_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.powerarmor.ModEventHandlerClient;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // Этот метод отвечает за силу эффекта ночного видения.
    // В режиме ORIGINAL_FALLBACK усиливаем эффект ПНВ при включённом тепловизоре,
    // чтобы контур и сущности были хорошо видны ночью.
    // В FULL_SHADER режиме этот миксин больше не вмешивается.
    @Inject(method = "getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At("RETURN"), cancellable = true)
    private static void hbm_m$getNightVisionScale(LivingEntity pLivingEntity, float pNanoTime, CallbackInfoReturnable<Float> cir) {
        if (!ModEventHandlerClient.isThermalActive()) {
            return;
        }
        if (ModClothConfig.get().thermalRenderMode != ModClothConfig.ThermalRenderMode.ORIGINAL_FALLBACK) {
            return;
        }

        float base = cir.getReturnValue();
        // Ослабляем "костыль": не жёстко 1.0, а минимум 0.85,
        // чтобы ночное зрение было сильным, но не выжигало глаза.
        if (base < 0.85f) {
            cir.setReturnValue(0.85f);
        }
    }
}