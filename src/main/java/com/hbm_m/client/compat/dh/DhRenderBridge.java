package com.hbm_m.client.compat.dh;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
@OnlyIn(Dist.CLIENT)
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
            boolean reverseDepth = isReverseZDepthActive(rp);
            // Клип-плоскости декода: нативный рендер — точная репликация
            // RenderUtil.setDhProjectionMatrix (с клампом near до 7.5 без
            // height-override); под Iris-override — сырые rp-значения (Iris
            // строит перспективу из них без клампа).
            float[] nativePlanes = irisLod ? null : resolveNativeRenderPlanes();
            float near, far;
            if (nativePlanes != null) {
                near = nativePlanes[0];
                far = nativePlanes[1];
            } else {
                near = rp.nearClipPlane;
                far = rp.farClipPlane;
            }
            DhClientState.beginDhPass(dhProj, near, far, irisLod, reverseDepth);
            DhClientState.endDhPass();
            if (++clipDiagCounter % 600 == 1) {
                MainRegistry.LOGGER.debug(
                        "HBM DH depth clips: rp=(near={}, far={}), nativePlanes={}, reverseZ={}, decode=(near={}, far={})",
                        String.format("%.2f", rp.nearClipPlane), String.format("%.2f", rp.farClipPlane),
                        nativePlanes != null,
                        reverseDepth,
                        String.format("%.2f", near), String.format("%.2f", far));
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

    // Кешируются ТОЛЬКО рефлексивная механика и классы-хозяева. Инстансы
    // pipeline/DHCompat/DHCompatInternal Iris пересоздаёт при каждом reload
    // пака — их кеширование давало протухший shouldOverride=false навсегда.
    private static Class<?> lastPipelineClass;
    private static java.lang.reflect.Method dhGetDHCompat;
    private static Class<?> lastDhCompatClass;
    private static java.lang.reflect.Method dhGetInstance;
    private static Class<?> lastInternalClass;
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
    public static boolean isIrisLodOverrideActive() {
        if (!com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()) {
            return false;
        }
        try {
            Object pipeline = com.hbm_m.client.render.shader.IrisReflectionBridge.getLivePipeline();
            if (pipeline == null) {
                return false;
            }
            Class<?> pipeCls = pipeline.getClass();
            if (dhGetDHCompat == null || pipeCls != lastPipelineClass) {
                dhGetDHCompat = pipeCls.getMethod("getDHCompat");
                dhGetDHCompat.setAccessible(true);
                lastPipelineClass = pipeCls;
            }
            Object dhCompat = dhGetDHCompat.invoke(pipeline);
            if (dhCompat == null) {
                return false;
            }
            Class<?> compatCls = dhCompat.getClass();
            if (dhGetInstance == null || compatCls != lastDhCompatClass) {
                dhGetInstance = compatCls.getMethod("getInstance");
                dhGetInstance.setAccessible(true);
                lastDhCompatClass = compatCls;
            }
            Object internal = dhGetInstance.invoke(dhCompat);
            if (internal == null) {
                return false;
            }
            Class<?> internalCls = internal.getClass();
            if (dhCompatShouldOverrideField == null || internalCls != lastInternalClass) {
                dhCompatShouldOverrideField = internalCls.getField("shouldOverride");
                dhCompatShouldOverrideField.setAccessible(true);
                lastInternalClass = internalCls;
            }
            return dhCompatShouldOverrideField.getBoolean(internal);
        } catch (Throwable ignored) {}
        return com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive();
    }

    /**
     * true, если DH 3.3.1+ рендерит LOD-глубину в конвенции REVERSE_Z
     * (близко=1, даль/небо=0, glClearDepth = EDhRenderDepth.farDepth = 0).
     *
     * ИСТОЧНИКИ ИСТИНЫ (по убыванию приоритета), опрашиваются каждый
     * DH-кадр — пользователь может включать/выключать шейдеры на лету:
     *
     *  1. rp.renderDefinition (поле DhApiRenderParam, DH API 7.3.0+):
     *     активное определение рендера едет В САМОМ СОБЫТИИ моста, и DH
     *     сам решает конвенцию ровно через renderDefinition
     *     .getDepthDirection() (GlDhMetaRenderer выбирает glDepthFunc/
     *     glClearDepth по этому же объекту). Критично для СВЕЖИХ сборок DH:
     *     коммит 1537c29 «Allow hot swapping Rendering Definitions»
     *     (2026-10-02) удалил RenderUtil.RENDER_DEF — у RenderUtil остались
     *     только LOGGER/MC/MC_RENDER/IRIS_ACCESSOR, а определение стало
     *     параметром рендера и поля события.
     *
     *  2. Статическое RenderUtil.RENDER_DEF / RENDER_API_DEF (DH 3.3.1-3.3.4):
     *     приватное поле хранит забинденное AbstractDhRenderApiDefinition.
     *
     *  3. Фолбэк по ВЕРСИИ DH: >= 3.3.1 и нативный рендер (без пака) =>
     *     REVERSE_Z. DH 3.2.x и старше — FORWARD_Z. Под активным паком у
     *     DH 3.3.x форвард (GlDhRenderApiDefinition.getDepthDirection:
     *     реверс только когда пак не активен или сам реверсивный).
     *
     * ИМЯ МЕТОДА ЗАВИСИТ ОТ ВЕРСИИ DH (оба возвращают енум с name()=="REVERSE_Z"):
     *  - DH <= 3.3.1: getRenderDepth();
     *  - DH >= 3.3.2: getDepthDirection() (переименование, семантика та же).
     *   БЕЗ фолбэка на getDepthDirection детект в 3.3.2 молча падал в
     *   FORWARD_Z, реверс-глубина без пака декодировалась форвард-формулой
     *   (близкий LOD → «дальний» depth ≈ 1.0) — гриб просвечивал сквозь
     *   LOD-рельеф.
     *
     * СРАВНЕНИЕ ЧЕРЕЗ name(): класс енума EDhApiDepthDirection между
     * версиями DH переезжал (api.enums.config ↔ api.enums.rendering) —
     * строковое сравнение исключает коллизии версий/загрузчиков.
     *
     * ПОЧЕМУ НЕ МАТРИЦА ИЗ СОБЫТИЯ: rp.dhProjectionMatrix — это ФОРВАРД-копия
     * с сырыми клип-плоскостями (проверено логом 3.3.1: декод матрицы даёт
     * ровно rp.near/far, признаков реверса нет), реальную реверс-матрицу DH
     * строит отдельно для своего GL-стейта (RenderUtil.setClipPlanes мутирует
     * другой экземпляр). Детект по знаку m32 события давал ложно-FORWARD и
     * реверс-глубина декодировалась форвард-формулой — вся копия глубины
     * уходила под fade-маску («гриб сквозь блоки», DH 3.3.1 без пака).
     */
    private static boolean isReverseZDepthActive(DhApiRenderParam rp) {
        Boolean fromParam = reverseZFromEventParam(rp);
        if (fromParam != null) {
            return logDepthConvention(fromParam, "eventParam.renderDefinition");
        }
        Boolean fromStatic = reverseZFromRenderUtilStatic();
        if (fromStatic != null) {
            return logDepthConvention(fromStatic, "RenderUtil.RENDER_DEF");
        }
        boolean fallback = isDhVersionAtLeast(3, 3, 1)
                && !com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive();
        return logDepthConvention(fallback, "version>=3.3.1 fallback");
    }

    /** Смена конвенции ИЛИ источника детекта — диагностически важное событие. */
    private static boolean logDepthConvention(boolean reverse, String source) {
        if (reverse != lastReverseDepth || !source.equals(lastReverseSource)) {
            lastReverseDepth = reverse;
            lastReverseSource = source;
            MainRegistry.LOGGER.info("HBM DH depth convention: {} (source: {})",
                    reverse ? "REVERSE_Z" : "FORWARD_Z", source);
        }
        return reverse;
    }

    private static String lastReverseSource = "(none)";

    // ── 1) rp.renderDefinition (DH API 7.3.0+, сборки после 1537c29) ────────
    // Кешируются ТОЛЬКО Field/Method и классы-хозяева (паттерн
    // isIrisLodOverrideActive): сами определения DH горячо заменяет
    // («hot swapping Rendering Definitions») — инстансы не кешируем.
    private static Class<?> lastParamClass;
    private static java.lang.reflect.Field renderDefinitionField;
    private static Class<?> lastDefinitionClass;
    private static java.lang.reflect.Method definitionDepthMethod;

    /** null = источник недоступен на этой сборке/в этом кадре (не ошибка). */
    private static Boolean reverseZFromEventParam(DhApiRenderParam rp) {
        if (rp == null) {
            return null;
        }
        try {
            Class<?> rpCls = rp.getClass();
            if (rpCls != lastParamClass) {
                lastParamClass = rpCls;
                renderDefinitionField = null;
                // DH <= 3.3.4: у параметра события поля renderDefinition ещё нет.
                try {
                    renderDefinitionField = rpCls.getField("renderDefinition");
                } catch (NoSuchFieldException ignored) {}
            }
            if (renderDefinitionField == null) {
                return null;
            }
            Object def = renderDefinitionField.get(rp);
            if (def == null) {
                return null;
            }
            Class<?> defCls = def.getClass();
            if (defCls != lastDefinitionClass) {
                lastDefinitionClass = defCls;
                try {
                    definitionDepthMethod = defCls.getMethod("getDepthDirection");
                } catch (NoSuchMethodException ignored) {
                    definitionDepthMethod = defCls.getMethod("getRenderDepth");
                }
            }
            Object depth = definitionDepthMethod.invoke(def);
            return depth instanceof Enum<?> e ? (Boolean) "REVERSE_Z".equals(e.name()) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ── 2) Статическое RenderUtil.RENDER_DEF / RENDER_API_DEF (3.3.1-3.3.4) ─

    /** null = источник недоступен на этой сборке/в этом кадре (не ошибка). */
    private static Boolean reverseZFromRenderUtilStatic() {
        try {
            if (reverseDefField == null) {
                Class<?> renderUtil = Class.forName("com.seibel.distanthorizons.core.util.RenderUtil");
                try {
                    reverseDefField = renderUtil.getDeclaredField("RENDER_DEF");
                } catch (NoSuchFieldException ignored) {}
                if (reverseDefField == null) {
                    try {
                        reverseDefField = renderUtil.getDeclaredField("RENDER_API_DEF");
                    } catch (NoSuchFieldException ignored) {}
                }
                if (reverseDefField == null) {
                    return null; // поля нет: DH 3.2.x или сборки после 1537c29
                }
                reverseDefField.setAccessible(true);
                Class<?> defClass = Class.forName(
                        "com.seibel.distanthorizons.core.wrapperInterfaces.render.AbstractDhRenderApiDefinition");
                // DH <= 3.3.1 → getRenderDepth(); DH >= 3.3.2 → getDepthDirection().
                try {
                    reverseDepthMethod = defClass.getMethod("getRenderDepth");
                } catch (NoSuchMethodException ignored) {
                    reverseDepthMethod = defClass.getMethod("getDepthDirection");
                }
            }
            Object def = reverseDefField.get(null);
            if (def == null) {
                return null; // DH ещё не инициализирован — пусть сработает следующий источник
            }
            Object depth = reverseDepthMethod.invoke(def);
            return depth instanceof Enum<?> e ? (Boolean) "REVERSE_Z".equals(e.name()) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static java.lang.reflect.Field reverseDefField;
    private static java.lang.reflect.Method reverseDepthMethod;
    private static boolean lastReverseDepth;

    // ── 3) Версионный фолбэк ────────────────────────────────────────────────

    /**
     * true, если версия мода DH >= указанной. Парсит ведущие числа
     * ("3.3.5-dev" → [3,3,5], "2.3.2-b" → [2,3,2], "3.3.1" → [3,3,1]).
     */
    private static boolean isDhVersionAtLeast(int major, int minor, int patch) {
        try {
            String version = dev.architectury.platform.Platform.getMod("distanthorizons").getVersion();
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?").matcher(version);
            if (!m.find()) {
                return false;
            }
            int[] v = {
                    Integer.parseInt(m.group(1)),
                    m.group(2) != null ? Integer.parseInt(m.group(2)) : 0,
                    m.group(3) != null ? Integer.parseInt(m.group(3)) : 0
            };
            if (v[0] != major) return v[0] > major;
            if (v[1] != minor) return v[1] > minor;
            return v[2] >= patch;
        } catch (Throwable ignored) {}
        return false;
    }

    // ── Нативные клип-плоскости рендера DH ──────────────────────────────────

    private static java.lang.reflect.Method nearClipBlocksMethod;
    private static java.lang.reflect.Method heightOverrideMethod;
    private static java.lang.reflect.Method farClipMethod;
    private static boolean nativePlanesResolved;

    /**
     * Клип-плоскости, КОТОРЫМИ DH реально растеризовал LOD-глубину в этом
     * кадре — точная репликация RenderUtil.setDhProjectionMatrix (байткод
     * 3.2.x/3.3.x идентичен в этой части):
     * <pre>
     * near = getNearClipPlaneInBlocks();                      // R-формула, RD-зависимая
     * if (getHeightBasedNearClipOverrideBlockDistance() == -1)
     *     near = min(near, 7.5f);                             // кламп БЕЗ override
     * far  = getFarClipPlaneDistanceInBlocks();
     * </pre>
     *
     * КРИТИЧНО (DH 3.3.1, rd≥3): rp.nearClipPlane несёт НЕклампленное R
     * (при rd=8 R≈44, матрица же использует 7.5). Декод реверс-глубины по
     * сырому R кодировал горы в разы дальше реального (1000 → 3570) —
     * «гриб перед LOD-горами, чем больше RD тем ближе». При rd≤2 R&lt;7.5,
     * кламп не срабатывал — потому маленькие RD выглядели корректными.
     *
     * @return {near, far} или null (рефлексия не удалась — фолбэк на rp).
     */
    private static float[] resolveNativeRenderPlanes() {
        if (!nativePlanesResolved) {
            nativePlanesResolved = true;
            try {
                Class<?> renderUtil = Class.forName("com.seibel.distanthorizons.core.util.RenderUtil");
                nearClipBlocksMethod = renderUtil.getMethod("getNearClipPlaneInBlocks");
                farClipMethod = renderUtil.getMethod("getFarClipPlaneDistanceInBlocks");
                try {
                    heightOverrideMethod = renderUtil.getMethod("getHeightBasedNearClipOverrideBlockDistance");
                } catch (NoSuchMethodException ignored) {}
            } catch (Throwable t) {
                MainRegistry.LOGGER.debug("HBM DH native planes reflection unavailable: {}", t.toString());
                return null;
            }
        }
        try {
            float near = (Float) nearClipBlocksMethod.invoke(null);
            // Кламп только при НЕактивном height-based override (ровно как в DH).
            if (heightOverrideMethod != null) {
                float override = (Float) heightOverrideMethod.invoke(null);
                if (override == -1.0F) {
                    near = Math.min(near, 7.5F);
                }
            } else {
                near = Math.min(near, 7.5F);
            }
            float far = (Float) farClipMethod.invoke(null);
            if (near > 0.0F && far > near && Float.isFinite(near) && Float.isFinite(far)) {
                return new float[] {near, far};
            }
        } catch (Throwable ignored) {}
        return null;
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
