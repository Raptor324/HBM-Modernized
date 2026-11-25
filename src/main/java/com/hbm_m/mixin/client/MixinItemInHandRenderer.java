package com.hbm_m.mixin.client;

import com.hbm_m.client.render.shader.PackAdditiveQueue;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Post-hand дрен аддитивной очереди в ванильном пайплайне (без активного шейдерпака).
 *
 * <p>В ванилле {@code GameRenderer.renderLevel} сначала отрисовывает уровень
 * (включая {@code AFTER_WEATHER}, где дренятся мировые трассеры), затем очищает
 * глубину и вызывает {@code renderHandsWithItems} — вспышки от первого лица попадают
 * в {@link PackAdditiveQueue} уже после {@code AFTER_WEATHER} и без дрена здесь
 * зависали бы до {@code AFTER_WEATHER} следующего кадра.</p>
 *
 * <p>Под Iris/Oculus {@code HandRenderer.renderSolid} вызывает {@code renderHandsWithItems}
 * <em>до</em> {@code finalizeLevelRendering}, который перезаписывает главный render target
 * финальным композитом. Поэтому при активном шейдерпаке выходим сразу: всю очередь
 * (и мировые трассеры, и вспышки руки) дренит {@link MixinIrisRenderingPipelinePreComposite}
 * на {@code TAIL finalizeLevelRendering}.</p>
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
