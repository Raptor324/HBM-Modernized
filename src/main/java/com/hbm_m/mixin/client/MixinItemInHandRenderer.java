package com.hbm_m.mixin.client;

import com.hbm_m.client.render.shader.PackAdditiveQueue;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Post-composite дрен аддитивной очереди сразу после hand-прохода.
 *
 * <p>Под Iris/Oculus финал пака уже в главном render target (finalizeLevelRendering
 * идёт в конце renderLevel, а hand-проход — после него, поверх композита). Без этого
 * дрена вспышки первого лица, поставленные в очередь во время hand-прохода, доились
 * бы только на TAIL следующего кадра — на кадр позже, с позой камеры прошлой секунды
 * (симптом «вспышка слишком далеко / уехала под землю» при движении). Дрен здесь
 * рисует их в тот же кадр поверх финальной картинки с честным depth-test против
 * глубины сцены. Без пака очередь пуста — вызов no-op.</p>
 */
@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "renderHandsWithItems", at = @At("TAIL"), require = 0)
    private void hbm_m$drainPackAdditiveAfterHand(CallbackInfo ci) {
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()) {
            return;
        }
        com.hbm_m.client.render.shader.PackAdditiveQueue.drainAndClear();
    }
}
