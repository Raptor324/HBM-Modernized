package com.hbm_m.client.render.shader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hbm_m.main.MainRegistry;

/**
 * Определение тене-дисторсии ("рыбий глаз" вокруг игрока) активного пакa.
 * <p>
 * Пак искажает shadow-map в своём shadow-вершинном шейдере
 * ({@code gl_Position.xy /= distortionFactor; gl_Position.z *= scale}) и
 * компенсирует искажение при сэмплировании теней. Любая геометрия, рисуемая в
 * shadow-map БЕЗ того же искажения, сэмплится не в тех текселях — тени
 * «ползают» за игроком. Поэтому инстансный теневой путь обязан воспроизводить
 * дисторсию пака.
 * <p>
 * Распознавание — по предобработанному (includes развёрнуты) исходнику
 * shadow-вершинной программы пака ({@code ProgramSet.getShadow()}), без имён
 * паков:
 * <ul>
 *   <li><b>quartic</b> — {@code quartic_length}-семейство (Photon):
 *       {@code factor = quartic_length(xy) * SHADOW_DISTORTION + (1-SHADOW_DISTORTION)},
 *       {@code z *= SHADOW_DEPTH_SCALE};</li>
 *   <li><b>linear</b> — классика (BSL/Complementary-семейство):
 *       {@code factor = length(xy) * bias + (1-bias)}, {@code z *= 0.2};</li>
 *   <li>нет {@code distort} в исходнике — дисторсии нет вообще.</li>
 * </ul>
 * Если схема не распознана — {@link #isCompatible()} false и теневой батч
 * рисуется через pack-программу (per-record, корректно, но медленнее).
 * Константы берутся из {@code #define} в том же исходнике (значения уже
 * подставлены настройками пака на этапе загрузки).
 */
public final class IrisShadowDistortion {

    /** 0 = нет дисторсии, 1 = quartic (Photon-семейство), 2 = linear (BSL-классика). */
    private static volatile int mode = 0;
    private static volatile float distortion = 0.85f;
    private static volatile float depthScale = 1.0f;
    private static volatile boolean compatible = true; // отсутствие дисторсии совместимо

    private static volatile long detectionGeneration = -1L;
    private static volatile boolean detectionDone = false;

    private static boolean reflected = false;
    private static Method getProgramSet;
    private static Method getShadow;
    /** Аргумент ProgramId.Shadow для единого get(ProgramId) Iris 1.8+ (null = именованный getShadow()). */
    private static Object shadowProgramId;
    private static Method getVertexSource;
    private static Method getPackDirectives;
    private static Method getShadowDirectives;
    private static Method getShadowDistance;
    private static Object overworldId;

    private IrisShadowDistortion() {}

    /** Совместим ли инстансный теневой путь с активным паком. */
    public static boolean isCompatible() {
        long gen = IrisExtendedShaderAccess.getPipelineGeneration();
        if (detectionDone && detectionGeneration == gen) {
            return compatible;
        }
        detect();
        detectionGeneration = gen;
        detectionDone = true;
        return compatible;
    }

    public static int mode() {
        return mode;
    }

    public static float distortion() {
        return distortion;
    }

    public static float depthScale() {
        return depthScale;
    }

    private static synchronized void detect() {
        boolean wasCompatible = compatible;
        try {
            if (!reflected) {
                reflected = true;
                Class<?> programSet = Class.forName("net.irisshaders.iris.shaderpack.programs.ProgramSet");
                // Iris 1.8+ (1.21.1) УДАЛИЛ getShadow() в пользу единого
                // get(ProgramId) — пробуем оба (Oculus 1.20.1 держит getShadow).
                try {
                    getShadow = programSet.getMethod("getShadow");
                } catch (NoSuchMethodException e) {
                    Class<?> programIdClass = Class.forName("net.irisshaders.iris.shaderpack.loading.ProgramId");
                    shadowProgramId = enumValueOf(programIdClass, "Shadow");
                    if (shadowProgramId == null) {
                        throw e;
                    }
                    getShadow = programSet.getMethod("get", programIdClass);
                }
                Class<?> programSource = Class.forName("net.irisshaders.iris.shaderpack.programs.ProgramSource");
                getVertexSource = programSource.getMethod("getVertexSource");
                // Порядок вызова как в IrisInstancedEncoders (там цепочка рабочая):
                // ShaderPack.getProgramSet(NamespacedId) → ProgramSet, и ТОЛЬКО затем
                // ProgramSet.getShadow(). Прежний getShadow.invoke(ShaderPack) падал
                // IllegalArgumentException "not an instance of declaring class" (debug.log
                // 0913 23:33) и навсегда уводил инстансные тени в per-record фолбэк.
                Class<?> shaderPack = Class.forName("net.irisshaders.iris.shaderpack.ShaderPack");
                getProgramSet = shaderPack.getMethod("getProgramSet",
                        Class.forName("net.irisshaders.iris.shaderpack.materialmap.NamespacedId"));
                Class<?> namespacedId = Class.forName("net.irisshaders.iris.shaderpack.materialmap.NamespacedId");
                overworldId = namespacedId.getConstructor(String.class, String.class)
                        .newInstance("minecraft", "overworld");
                // Директивы пака: реальный shadowDistance (BSL-classic считает bias из него:
                // const float shadowMapBias = 1.0 - 25.6 / shadowDistance; lib/settings.glsl
                // НЕ попадает в include-развёрнутый исходник — см. quartic-ветку ниже).
                getPackDirectives = programSet.getMethod("getPackDirectives");
                getShadowDirectives = Class.forName(
                        "net.irisshaders.iris.shaderpack.properties.PackDirectives").getMethod("getShadowDirectives");
                getShadowDistance = Class.forName(
                        "net.irisshaders.iris.shaderpack.properties.PackShadowDirectives").getMethod("getDistance");
            }
            // Default: дисторсии нет
            mode = 0;
            distortion = 0.85f;
            depthScale = 1.0f;
            compatible = true;

            Object pack = currentPackHolder()[0];
            Object programSetObj = getProgramSet.invoke(pack, overworldId);
            if (programSetObj == null) {
                compatible = false;
                MainRegistry.LOGGER.warn(
                        "[HBM-M] IrisShadowDistortion: ProgramSet null - instanced shadows disabled (safe fallback)");
                return;
            }
            Object shadowOpt = (shadowProgramId != null)
                    ? getShadow.invoke(programSetObj, shadowProgramId)
                    : getShadow.invoke(programSetObj, NO_ARGS);
            if (shadowOpt instanceof java.util.Optional<?> opt) {
                shadowOpt = opt.orElse(null);
            }
            if (shadowOpt == null) {
                MainRegistry.LOGGER.info(
                        "[HBM-M] IrisShadowDistortion: pack has no shadow program - no distortion, instanced shadows on");
                return; // нет shadow-программы — нет и дисторсии
            }
            Object vshOpt = getVertexSource.invoke(shadowOpt);
            if (!(vshOpt instanceof java.util.Optional<?> v) || v.isEmpty()) {
                MainRegistry.LOGGER.info(
                        "[HBM-M] IrisShadowDistortion: shadow program has no vertex source - no distortion, instanced shadows on");
                return;
            }
            String glsl = (String) v.get();
            int len = glsl.length();
            boolean hasQuartic = glsl.contains("quartic_length");
            boolean hasDistortFactor = glsl.contains("distortFactor");
            boolean hasDistort = glsl.contains("distort");

            if (!hasDistort) {
                MainRegistry.LOGGER.info(
                        "[HBM-M] IrisShadowDistortion: no distortion in pack shadow vsh (len={}) - instanced shadows on",
                        len);
                return; // дисторсии нет — совместимо с mode 0
            }
            if (hasQuartic) {
                Float d = extractDefine(glsl, "SHADOW_DISTORTION");
                Float s = extractDefine(glsl, "SHADOW_DEPTH_SCALE");
                if (d != null && s != null) {
                    mode = 1;
                    distortion = d;
                    depthScale = s;
                    compatible = true;
                    logResult("quartic (Photon-family)", d, s, len);
                    return;
                }
                // Iris IncludeProcessor разворачивает только #include под shaders/include/;
                // #include "/settings.glsl" (где у Photon объявлены константы) молча
                // выбрасывается — vshLen 29346 вместо 188k, defines отсутствуют
                // (debug.log 0913 23:40, Photon 1.2a). Константы в settings.glsl не
                // являются слайдерами настроек — берём семейственные дефолты Photon.
                mode = 1;
                distortion = 0.85f;
                depthScale = 0.2f;
                compatible = true;
                MainRegistry.LOGGER.warn(
                        "[HBM-M] IrisShadowDistortion: quartic family, defines not in include-expanded "
                                + "source (Iris drops non-include/ #include's, vshLen={}) - using Photon defaults "
                                + "(distortion=0.85, depthScale=0.2)", len);
                return;
            }
            // Классика: factor = length(xy) * bias + (1-bias); z *= 0.2
            boolean linearFormula = hasDistortFactor
                    && Pattern.compile("gl_Position\\.z\\s*=\\s*gl_Position\\.z\\s*\\*\\s*0\\.2")
                            .matcher(glsl).find();
            if (linearFormula) {
                // bias пака: #define/const-литерал, либо формула BSL-classic
                // "1.0 - <C> / shadowDistance" (само определение живёт в
                // lib/settings.glsl, который Iris в исходник НЕ разворачивает).
                // Ранний дефолт 0.85 при shadowDistance=256 давал bias 0.9 → тени
                // «улетали» от станков (лог 0913 23:41, BSL 10.1.1).
                float bias;
                Float literal = extractDefine(glsl, "shadowMapBias");
                if (literal == null) {
                    literal = extractConstFloat(glsl, "shadowMapBias");
                }
                if (literal != null) {
                    bias = literal;
                } else {
                    float divisor = extractShadowDistanceDivisor(glsl);
                    bias = 1.0f - divisor / resolveShadowDistance(programSetObj);
                }
                mode = 2;
                distortion = bias;
                depthScale = 0.2f;
                compatible = true;
                logResult("linear (BSL-classic)", bias, 0.2f, len);
                return;
            }
            compatible = false;
            logIncompatible("unknown distortion family", len, hasDistortFactor);
        } catch (Throwable t) {
            compatible = false;
            MainRegistry.LOGGER.warn("[HBM-M] IrisShadowDistortion: detection failed ({}), "
                    + "instanced shadows disabled", t.toString());
        } finally {
            MainRegistry.LOGGER.info(
                    "[HBM-M] IrisShadowDistortion: mode={}, distortion={}, depthScale={}, instanced shadows {}",
                    mode, distortion, depthScale, compatible ? "ENABLED" : "DISABLED (pack-program fallback)");
        }
    }

    private static final Object[] NO_ARGS = new Object[0];

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValueOf(Class<?> enumClass, String name) {
        try {
            return Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * [0] = ShaderPack активного пайплайна, [1] = имя конкретного класса
     * пайплайна (для диагностики). Поле {@code pack} ищется по ВСЕЙ иерархии
     * класса: рантайм-пайплайн может быть подклассом IrisRenderingPipeline
     * (NewWorldRenderingPipeline и т.п.) — getDeclaredField на одном классе
     * здесь падал и молча отключал инстансные тени.
     */
    private static Object[] currentPackHolder() throws Exception {
        Object manager = Class.forName("net.irisshaders.iris.Iris")
                .getMethod("getPipelineManager").invoke(null);
        if (manager == null) {
            throw new IllegalStateException("pipeline manager null");
        }
        Object pipeline = Class.forName("net.irisshaders.iris.pipeline.PipelineManager")
                .getMethod("getPipelineNullable").invoke(manager);
        if (pipeline == null) {
            throw new IllegalStateException("pipeline null");
        }
        Class<?> c = pipeline.getClass();
        while (c != null) {
            try {
                Field packField = c.getDeclaredField("pack");
                packField.setAccessible(true);
                MainRegistry.LOGGER.info(
                        "[HBM-M] IrisShadowDistortion: pack field resolved via {}",
                        c.getName());
                return new Object[] {packField.get(pipeline), c.getName()};
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new IllegalStateException("no 'pack' field on pipeline class "
                + pipeline.getClass().getName());
    }

    private static Float extractDefine(String glsl, String name) {
        // Значение может быть "0.85", ".85", "0.85F" — допускаем F-суффикс.
        Matcher m = Pattern.compile("#define\\s+" + Pattern.quote(name)
                + "\\s+([0-9]*\\.?[0-9]+)[fF]?\\b").matcher(glsl);
        if (m.find()) {
            try {
                return Float.parseFloat(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /** "const float shadowMapBias = 0.9;" → 0.9 (литерал; формула не матчится). */
    private static Float extractConstFloat(String glsl, String name) {
        Matcher m = Pattern.compile("const\\s+float\\s+" + Pattern.quote(name)
                + "\\s*=\\s*([0-9]*\\.?[0-9]+)[fF]?\\s*;").matcher(glsl);
        if (m.find()) {
            try {
                return Float.parseFloat(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    /**
     * BSL-classic: "const float shadowMapBias = 1.0 - 25.6 / shadowDistance;"
     * → 25.6. Нет совпадения — семейственный дефолт 25.6.
     */
    private static float extractShadowDistanceDivisor(String glsl) {
        Matcher m = Pattern.compile("=\\s*1\\.0?[fF]?\\s*-\\s*([0-9]*\\.?[0-9]+)\\s*/\\s*shadowDistance")
                .matcher(glsl);
        if (m.find()) {
            try {
                return Float.parseFloat(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 25.6f;
    }

    /** Реальный shadowDistance из директив пака (Iris парсит его из const-директивы). */
    private static float resolveShadowDistance(Object programSetObj) {
        try {
            Object packDirectives = getPackDirectives.invoke(programSetObj);
            Object shadowDirectives = getShadowDirectives.invoke(packDirectives);
            Object dist = getShadowDistance.invoke(shadowDirectives);
            if (dist instanceof Number n && n.floatValue() > 1.0f) {
                resolvedShadowDistance = n.floatValue();
                return n.floatValue();
            }
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn(
                    "[HBM-M] IrisShadowDistortion: shadowDistance directive unavailable ({}) - assuming 256.0",
                    t.toString());
        }
        return 256.0f;
    }

    /** Последняя известная дистанция теней пака (для дистанц-куллинга теневого байпас-обхода). */
    private static volatile float resolvedShadowDistance = 160.0f;

    public static float shadowDistance() {
        return resolvedShadowDistance;
    }

    public static float shadowDistanceSq() {
        float d = resolvedShadowDistance + 8.0f; // запас на большие AABB машин
        return d * d;
    }

    private static void logResult(String family, float d, float s, int sourceLen) {
        MainRegistry.LOGGER.info(
                "[HBM-M] IrisShadowDistortion: {} family matched (distortion={}, depthScale={}, vshLen={})",
                family, d, s, sourceLen);
    }

    private static void logIncompatible(String reason, int sourceLen, boolean hasDistortFactor) {
        MainRegistry.LOGGER.info(
                "[HBM-M] IrisShadowDistortion: {} - instanced shadows disabled "
                        + "(vshLen={}, hasDistortFactor={})", reason, sourceLen, hasDistortFactor);
    }
}
