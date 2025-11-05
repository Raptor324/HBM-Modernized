package com.hbm_m.mixin;

import com.hbm_m.client.render.shader.ImmediateFallbackRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Интеграция с Oculus shadow rendering
 */
@OnlyIn(Dist.CLIENT)
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shadows.ShadowRenderer", remap = false)
public class ShadowIntegrationMixin {

    @Inject(method = "renderShadows", at = @At("HEAD"), remap = false)
    private void onShadowRenderStart(CallbackInfo ci) {
        ImmediateFallbackRenderer.beginShadowPass();
    }

    @Inject(method = "renderShadows", at = @At("RETURN"), remap = false)
    private void onShadowRenderEnd(CallbackInfo ci) {
        ImmediateFallbackRenderer.endShadowPass();
    }
}
