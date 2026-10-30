package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Позиционный свет ядерного взрыва: вызов raw-GL пасса в TAIL
 * {@code finalizeLevelRendering} (Iris/Oculus 1.8.x) - финальная картинка пака
 * уже в главном render target, его depth-текстура всё ещё держит глубину сцены.
 * TAIL выбран по данным перехвата 2026-10-11: флип-буферы Iris на пересозданных
 * пайплайнах Oculus недоступны снаружи (кэш биндингов композитов), а главный
 * render target доходит до экрана всегда.
 *
 * <p>HEAD-инъекция (2026-10-11): снапшот gbuffer-матриц и камеры ДО композитов
 * в {@link RawScreenSpaceLight#captureFrameMatrices}. В Iris 1.8.14/Oculus 1.8
 * композиты CapturedRenderingState НЕ перезаписывают - gbuffer-сеттеры имеют
 * ровно один call-site за кадр (renderLevel HEAD, аудит IRIS-F1), так что
 * снапшот оборонительный: на других версиях Iris гарантий нет, а TAIL всегда
 * рисует по снимку своего же кадра. Неудача HEAD-резолва - TAIL ведёт себя как
 * раньше (резолв на месте, skipFrame), latch не ставится.</p>
 *
 * <p>Мягкая зависимость: {@code @Pseudo} + строковый target + {@code require = 0}
 * — без Iris/Oculus миксин молча не применяется. Применение дополнительно гейтится
 * {@code HbmMixinConfigPlugin#shouldApplyMixin}. Один якорь на 1.20.1-forge
 * (Oculus) и 1.21.1-neoforge (Iris): сигнатуры сверены javap по обоим джарникам.</p>
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
public class MixinIrisRenderingPipelinePreComposite {

    @Unique
    private static boolean hbm$firedOnce;

    @Inject(method = "finalizeLevelRendering", at = @At("HEAD"), remap = false, expect = 0, require = 0)
    private void hbm$headCaptureFrameMatrices(CallbackInfo ci) {
        com.hbm_m.client.render.shader.RawScreenSpaceLight.captureFrameMatrices();
        com.hbm_m.client.render.shader.PackAdditiveQueue.captureFrameMatrices();
    }

    @Inject(method = "finalizeLevelRendering", at = @At("TAIL"), remap = false, expect = 0, require = 0)
    private void hbm$tailScreenSpaceLight(CallbackInfo ci) {
        if (!hbm$firedOnce) {
            hbm$firedOnce = true;
            com.hbm_m.main.MainRegistry.LOGGER.info(
                    "MixinIrisRenderingPipelinePreComposite: injection fired (finalizeLevelRendering TAIL, main RT)");
        }
        com.hbm_m.client.render.shader.PackAdditiveQueue.drainAndClear();
        com.hbm_m.client.render.shader.RawScreenSpaceLight.drawIfReady();
    }
}
