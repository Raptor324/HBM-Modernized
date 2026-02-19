package com.hbm_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.powerarmor.ModEventHandlerClient;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Когда тепловизор активен в режиме FULL_SHADER, фиксируем гамму и тьму,
 * чтобы пользовательские настройки яркости не ломали шейдерный рендер.
 *
 * В режиме ORIGINAL_FALLBACK эти правки отключены: работает обычное освещение.
 */
@Mixin(LightTexture.class)
public class LightTextureMixin {

    private static boolean isFullShaderThermalActive() {
        if (!ModEventHandlerClient.isThermalActive()) {
            return false;
        }
        return ModClothConfig.get().thermalRenderMode == ModClothConfig.ThermalRenderMode.FULL_SHADER;
    }

    @Inject(method = "getDarknessGamma(F)F", at = @At("RETURN"), cancellable = true)
    private void hbm_m$getDarknessGamma(float pPartialTick, CallbackInfoReturnable<Float> cir) {
        if (isFullShaderThermalActive()) {
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
        if (isFullShaderThermalActive()) {
            cir.cancel();
            cir.setReturnValue(0.25f);
        }
    }

    /**
     * Жёстко переписываем расчёт яркости lightmap для тепловизора.
     * Это вызывается до финального формирования карты освещения, так что
     * реально влияет на то, что потом видит post‑process thermal.fsh.
     */
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void hbm_m$getBrightness(
        DimensionType pDimensionType,
        int pLightValue,
        CallbackInfoReturnable<Float> cir
    ) {
        if (!isFullShaderThermalActive()) {
            return;
        }

        // Ванильный диапазон pLightValue: 0..15.
        // Делаем мягкий, детерминированный буст:
        //  - ночью (низкие значения) чуть светлее за счёт поднятого минимума,
        //  - днём (высокие значения) чуть темнее за счёт пониженного максимума.
        float base = pLightValue / 15.0f;
        float boosted = base * 1.6f + 0.30f;
        if (boosted < 0.45f) boosted = 0.45f;
        if (boosted > 0.70f) boosted = 0.70f;

        cir.setReturnValue(boosted);
    }
}
