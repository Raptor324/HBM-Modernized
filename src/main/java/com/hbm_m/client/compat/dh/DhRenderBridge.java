package com.hbm_m.client.compat.dh;

import org.joml.Matrix4f;

import com.hbm_m.main.MainRegistry;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiRenderPass;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeApplyShaderRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;

/**
 * Официальный мост в пайплайн Distant Horizons через DhApiBeforeApplyShaderRenderEvent.
 *
 * Событие стреляет внутри LodRenderer.renderTerrain прямо перед applyToMcTexture:
 * в этот момент забинден DH FBO (цвет + DEPTH32F с глубиной LOD-террейна),
 * а матрицы rp.dhProjectionMatrix/dhModelViewMatrix соответствуют его проходу.
 * Мы рисуем дальние ракеты/частицы гриба depth-tested (LEQUAL) против LOD-глубины,
 * после чего DH сам композитит свой FBO в ванильный.
 *
 * Заменяет удалённый LodRendererMixin (инжект в приватный renderTerrain —
 * ломался между версиями DH 2.x/3.x). Событие существует начиная с API 2.0.0,
 * т.е. покрывает и старые релизы DH 2.1+.
 *
 * ВАЖНО: класс трогается только при установленном DH (см. register()/EngineHandler),
 * поэтому compileOnly-зависимость безопасна в рантайме без DH.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
 *///?}
public final class DhRenderBridge extends DhApiBeforeApplyShaderRenderEvent {

    private static volatile boolean registered = false;

    private DhRenderBridge() {}

    /**
     * Идемпотентная привязка слушателя. Вызывается лениво (ClientSetup + каждый
     * клиентский тик из EngineHandler). DI-реестр DH ПЕРЕСОЗДАЁТСЯ во время его
     * инициализации (мы биндимся на ресурслоаде раньше DhApiBeforeDhInit), поэтому
     * после регистрации каждый тик проверяем, что наш слушатель ещё жив, и
     * перебиндиваем при необходимости.
     */
    public static void tryRegister() {
        if (!com.hbm_m.compat.dh.DhCompat.isModPresent()) return;
        synchronized (DhRenderBridge.class) {
            try {
                if (registered && isStillBound()) return;
                DhApi.events.bind(DhApiBeforeApplyShaderRenderEvent.class, new DhRenderBridge());
                if (!registered) {
                    MainRegistry.LOGGER.info("HBM: DH render bridge bound to DhApiBeforeApplyShaderRenderEvent");
                } else {
                    // Реестр DH был пересоздан/очищен — перебиндились.
                    firedOnce = false;
                    MainRegistry.LOGGER.info("HBM: DH render bridge re-bound (registry was reset by DH init)");
                }
                registered = true;
            } catch (Throwable t) {
                // Не валим игру: DH может быть ещё не готов — попробуем на следующем тике.
                MainRegistry.LOGGER.debug("HBM: DH bridge bind deferred: {}", t.toString());
            }
        }
    }

    private static boolean isStillBound() {
        try {
            for (Object listener : DhApi.events.getAll(DhApiBeforeApplyShaderRenderEvent.class)) {
                if (listener instanceof DhRenderBridge) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    @Override
    public boolean removeAfterFiring() {
        return false;
    }

    @Override
    public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> param) {
        try {
            DhApiRenderParam rp = param.value;
            if (rp == null) return;
            if (!firedOnce) {
                firedOnce = true;
                MainRegistry.LOGGER.info("HBM: DH bridge event firing, pass={}, proj={}",
                        rp.renderPass, rp.dhProjectionMatrix != null ? "ok" : "null");
            }
            // TRANSPARENT — отложенный проход полупрозрачной воды (Iris/deferred):
            // там второй раз стреляет это же событие, дублировать отрисовку нельзя.
            if (rp.renderPass == EDhApiRenderPass.TRANSPARENT) return;

            if (++frameLogCounter % 600 == 1) {
                MainRegistry.LOGGER.debug("HBM bridge frame #{}: pass={}, proj={}",
                        frameLogCounter, rp.renderPass, rp.dhProjectionMatrix != null ? "ok" : "null");
            }

            Matrix4f dhProj = toJoml(rp.dhProjectionMatrix);
            if (dhProj == null) return;

            // Мост больше НИЧЕГО не рисует: запись геометрии в DH FBO отравляла
            // композит apply.frag («чёрная земля»). Задача моста — фиксировать
            // факт DH-кадра и его проекцию; весь дальний контент рисует
            // EngineHandler (AFTER_WEATHER) в главный FBO.
            boolean irisLod = isIrisLodOverrideActive();
            DhClientState.beginDhPass(dhProj, rp.nearClipPlane, rp.farClipPlane, irisLod);
            DhClientState.endDhPass();
            if (++clipDiagCounter % 600 == 1) {
                MainRegistry.LOGGER.debug(
                        "HBM DH depth clips: rp=(near={}, far={}), irisLodOverride={}, effective=(near={}, far={})",
                        String.format("%.2f", rp.nearClipPlane), String.format("%.2f", rp.farClipPlane),
                        irisLod,
                        String.format("%.2f", DhClientState.dhNear()), String.format("%.2f", DhClientState.dhFar()));
            }
        } catch (Throwable t) {
            // Никогда не роняем рендер DH. Полный стек — один раз, дальше кратко.
            if (errorCount++ == 0) {
                MainRegistry.LOGGER.warn("HBM DH bridge render failed (first occurrence):", t);
            } else if (errorCount % 600 == 0) {
                MainRegistry.LOGGER.warn("HBM DH bridge render still failing (x{}): {}", errorCount, t.toString());
            }
        }
    }

    private static long errorCount = 0;
    private static volatile boolean firedOnce = false;
    private static long frameLogCounter = 0;
    private static long clipDiagCounter = 0;

    // ── Определение Iris DH-compat override ─────────────────────────────────

    private static volatile boolean dhCompatQueryInitialized = false;
    // Кешируются ТОЛЬКО синглтоны и рефлексивная механика. Инстансы pipeline/
    // DHCompat/DHCompatInternal Iris пересоздаёт при каждом reload пака —
    // их кеширование давало протухший shouldOverride=false навсегда.
    private static Object dhPipelineManager;
    private static java.lang.reflect.Method dhGetPipelineNullable;
    private static java.lang.reflect.Method dhGetDHCompat;
    private static java.lang.reflect.Method dhGetInstance;
    private static java.lang.reflect.Field dhCompatShouldOverrideField;

    /**
     * true, если LOD'ы под паком рисует Iris (DH-compat программы), а не
     * нативный DH-шейдер. В этом режиме проекция LOD строится из СЫРЫХ
     * rp.nearClipPlane/farClipPlane (LodRendererEvents → setPerspective,
     * БЕЗ клампа near до 7.5), и декод DEPTH32F должен идти по ним, а не по
     * клампнутой матрице нативного DH.
     *
     * Путь: Iris.getPipelineManager().getPipelineNullable() → pipeline
     * .getDHCompat() → getInstance() → поле shouldOverride класса
     * net.irisshaders.iris.compat.dh.DHCompatInternal.
     *
     * ВАЖНО: инстансы переразрешаются при КАЖДОМ вызове (раз в DH-кадр —
     * дёшево). Раньше кешировался сам DHCompatInternal: Iris пересоздаёт его
     * при каждом reload пайплайна, у старого инстанса shouldOverride
     * навсегда false (см. DHCompatInternal.clear()), детекция врала, декод
     * шёл по клампнутой near=7.5 матрице → LOD-глубина декодировалась
     * (7.5/n_real)·true «ближе» и гора позади перетирала гриб (при rd≤4
     * баг маскировался: под паком near=7.34 < 7.5, кламп не срабатывал).
     *
     * Фолбэк: если рефлексия не удалась — считаем override активным при
     * любом активном паке (isExternalShaderActive): у большинства паков
     * с DH-поддержкой это так.
     */
    private static boolean isIrisLodOverrideActive() {
        try {
            if (!dhCompatQueryInitialized) {
                dhCompatQueryInitialized = true;
                Class<?> irisC = Class.forName("net.irisshaders.iris.Iris");
                // PipelineManager — синглтон на время жизни игры, кешируем.
                dhPipelineManager = irisC.getMethod("getPipelineManager").invoke(null);
            }
            if (dhPipelineManager == null) {
                // Iris ещё не инициализирован — LOD'ы рисует нативный DH.
                return false;
            }
            if (dhGetPipelineNullable == null) {
                dhGetPipelineNullable = dhPipelineManager.getClass().getMethod("getPipelineNullable");
            }
            Object pipeline = dhGetPipelineNullable.invoke(dhPipelineManager);
            if (pipeline == null) {
                // Пак неактивен — LOD'ы рисует нативный DH.
                return false;
            }
            if (dhGetDHCompat == null) {
                dhGetDHCompat = pipeline.getClass().getMethod("getDHCompat");
            }
            Object dhCompat = dhGetDHCompat.invoke(pipeline);
            if (dhCompat == null) {
                return false;
            }
            if (dhGetInstance == null) {
                dhGetInstance = dhCompat.getClass().getMethod("getInstance");
            }
            Object internal = dhGetInstance.invoke(dhCompat);
            if (internal == null) {
                return false;
            }
            if (dhCompatShouldOverrideField == null) {
                dhCompatShouldOverrideField = internal.getClass().getField("shouldOverride");
            }
            return dhCompatShouldOverrideField.getBoolean(internal);
        } catch (Throwable ignored) {}
        // Фолбэк: активный пак → предполагаем override
        return com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive();
    }

    /**
     * DhApiMat4f -> JOML Matrix4f.
     * Матрицы DH хранятся ТРАНСПОНИРОВАННО относительно JOML (см. DhMat4f.createJomlMatrix
     * в исходниках DH), плюс getValuesAsArray() отдаёт row-major по DH-именованию.
     * Конвертация повторяет официальный DhMat4f.createJomlMatrix.
     */
    private static Matrix4f toJoml(com.seibel.distanthorizons.api.objects.math.DhApiMat4f m) {
        if (m == null) return null;
        return new Matrix4f(
                m.m00, m.m10, m.m20, m.m30,
                m.m01, m.m11, m.m21, m.m31,
                m.m02, m.m12, m.m22, m.m32,
                m.m03, m.m13, m.m23, m.m33);
    }
}
