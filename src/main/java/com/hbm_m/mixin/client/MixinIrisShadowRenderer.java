package com.hbm_m.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Глобальный батч shadow-прохода: флаш накопленных машинных инстансов ОДИН раз
 * в конце shadow BE-фазы Iris, вместо per-BE apply/clear + per-part immediate
 * дроуков (см. {@link com.hbm_m.client.render.IrisShadowBatchCollector}).
 * <p>
 * Якорь — вызов {@code copyPreTranslucentDepth} в {@code renderShadows}, сразу
 * после {@code bufferSource.endBatch()}: непрозрачная геометрия машин пишется
 * до копирования translucent-глубины и до translucent shadow terrain.
 * Проверено на Iris 1.20.1 (Iris 1.7.6, строки ~513-536) и 1.21.1 (Iris 1.8+,
 * строки ~521-536) — сигнатура идентична; Oculus наследует те же классы.
 * <p>
 * Мягкая зависимость: {@code @Pseudo} + строковый target + {@code require = 0} —
 * без Iris/Oculus миксин молча не применяется (коллектор сам откатывается на
 * немедленный shadow-путь, см. {@code IrisShadowBatchCollector.onMainPassFrameStart}).
 * {@code remap = false}: Iris не обфусцирован; якорь и target — только
 * iris-классы (LevelRendererAccessor — тоже iris-тип), поэтому миксин один на
 * 1.20.1-forge и 1.21.1-neoforge.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shadows.ShadowRenderer", remap = false)
public class MixinIrisShadowRenderer {

    @Inject(method = "renderShadows",
            at = @At(value = "INVOKE",
                     target = "Lnet/irisshaders/iris/shadows/ShadowRenderer;copyPreTranslucentDepth(Lnet/irisshaders/iris/mixin/LevelRendererAccessor;)V",
                     remap = false,
                     shift = At.Shift.BEFORE),
            remap = false,
            expect = 0,
            require = 0)
    private void nucleus$flushGlobalShadowBatch(CallbackInfo ci) {
        com.hbm_m.client.render.IrisShadowBatchCollector.flushGlobalShadowBatch();
    }
}
