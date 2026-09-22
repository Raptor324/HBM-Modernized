package com.hbm_m.client.render.shader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.hbm_m.main.MainRegistry;

/**
 * Выбор инстансного gbuffer-энкодера для активного shaderpack'а.
 * <p>
 * <b>Зачем:</b> deferred-паки пишут gbuffer СВОИМИ программами в собственном
 * формате; наш инстансный ExtendedShader обязан воспроизводить формат пака
 * побайтово, иначе композит декодирует мусор (чёрные/битые машины).
 * <p>
 * <b>Как детектим (автоматически, без имён паков):</b> достаём через рефлексию
 * предобработанный исходник gbuffer-программы пака
 * ({@code ShaderPack.getProgramSet(...).getGbuffersBlock().getFragmentSource()}) и
 * ищем сигнатуру схемы кодирования. Если схема знакома — включается
 * соответствующий энкодер ({@code shaders/core/iris/block_lit_instanced_packed}),
 * иначе {@link #hasEncoder()} == false и машины рисуются через pack-программы
 * (companion per-instance) — корректно под ЛЮБЫМ паком.
 * <p>
 * Энкодер "packed": gbuffer-таргет пакуется парами 8-битных каналов
 * ({@code pack_unorm_2x8}: albedo.rg / albedo.b+mask / oct-encoded normal /
 * light levels), нормали в мировых осях, свет в gbuffer НЕ умножается
 * (считается в deferred-композите).
 * <p>
 * Кеш — на поколение пайплайна (смена пака/F3+R инвалидирует автоматически).
 */
public final class IrisInstancedEncoders {

    private static volatile long detectionGeneration = -1L;
    private static volatile boolean detectionDone = false;
    private static volatile boolean encoderAvailable = false;

    private static boolean reflected = false;
    private static Method getPipelineManager;
    private static Method getPipelineNullable;
    private static Field packField;
    private static Method getProgramSet;
    private static Object overworldId;
    private static Method getGbuffersBlock;
    /** Аргумент ProgramId.Block для единого get(ProgramId) Iris 1.8+ (null = именованный геттер). */
    private static Object gbuffersProgramIdArg;
    private static Method getFragmentSource;

    private IrisInstancedEncoders() {}

    /** Есть ли для активного пака инстансный gbuffer-энкодер. */
    public static boolean hasEncoder() {
        long gen = IrisExtendedShaderAccess.getPipelineGeneration();
        if (detectionDone && detectionGeneration == gen) {
            return encoderAvailable;
        }
        boolean detected = detect();
        if (detected != encoderAvailable) {
            MainRegistry.LOGGER.info(
                    "IrisInstancedEncoders: gbuffer encoder {} (pipeline generation {})",
                    detected ? "ENABLED - pack gbuffer scheme recognized" : "disabled", gen);
        }
        detectionGeneration = gen;
        detectionDone = true;
        encoderAvailable = detected;
        return detected;
    }

    /**
     * Имя ресурса инстансного шейдера (под shaders/core/). Shadow-слот ВСЕГДА
     * использует базовый vanilla-parity шейдер: теневому FB не нужен pack-формат
     * gbuffer (глубина + простой цвет), поэтому инстансинг теней универсален
     * для любого пака. Packed-энкодер — только для main gbuffer.
     */
    public static String shaderName(boolean shadowPass) {
        return shadowPass || !encoderAvailable
                ? "hbm_m:iris/block_lit_instanced_iris"
                : "hbm_m:iris/block_lit_instanced_packed";
    }

    private static boolean detect() {
        try {
            if (!reflected) {
                reflected = true;
                Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
                getPipelineManager = iris.getMethod("getPipelineManager");
                Class<?> pmClass = Class.forName("net.irisshaders.iris.pipeline.PipelineManager");
                getPipelineNullable = pmClass.getMethod("getPipelineNullable");
                Class<?> pipelineClass = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
                packField = pipelineClass.getDeclaredField("pack");
                packField.setAccessible(true);
                Class<?> shaderPack = Class.forName("net.irisshaders.iris.shaderpack.ShaderPack");
                getProgramSet = shaderPack.getMethod("getProgramSet",
                        Class.forName("net.irisshaders.iris.shaderpack.materialmap.NamespacedId"));
                Class<?> namespacedId = Class.forName("net.irisshaders.iris.shaderpack.materialmap.NamespacedId");
                overworldId = namespacedId.getConstructor(String.class, String.class)
                        .newInstance("minecraft", "overworld");
                Class<?> programSet = Class.forName("net.irisshaders.iris.shaderpack.programs.ProgramSet");
                // Геттеры возвращают Optional<ProgramSource>!
                // Iris 1.8+ (1.21.1) УДАЛИЛ именованные getGbuffersXxx() в пользу
                // единого get(ProgramId) — пробуем оба (Oculus 1.20.1 держит
                // именованные, Iris 1.8.14 — только get(ProgramId.Block)).
                Method blockGetter = null;
                for (String name : new String[] {"getGbuffersBlock", "getGbuffersTerrain"}) {
                    try {
                        blockGetter = programSet.getMethod(name);
                        break;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
                Object programIdArg = null;
                if (blockGetter == null) {
                    try {
                        Class<?> programIdClass = Class.forName("net.irisshaders.iris.shaderpack.loading.ProgramId");
                        programIdArg = enumValueOf(programIdClass, "Block");
                        if (programIdArg == null) {
                            throw new NoSuchMethodException("ProgramId.Block not found");
                        }
                        blockGetter = programSet.getMethod("get", programIdClass);
                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        MainRegistry.LOGGER.warn("[HBM-M] IrisInstancedEncoders: ProgramSet gbuffers getters not found");
                        return false;
                    }
                }
                getGbuffersBlock = blockGetter;
                gbuffersProgramIdArg = programIdArg;
                Class<?> programSource = Class.forName("net.irisshaders.iris.shaderpack.programs.ProgramSource");
                getFragmentSource = programSource.getMethod("getFragmentSource");
            }
            Object manager = getPipelineManager.invoke(null);
            if (manager == null) {
                return logMiss("pipeline manager null");
            }
            Object pipeline = getPipelineNullable.invoke(manager);
            if (pipeline == null) {
                return logMiss("pipeline null");
            }
            Object pack = packField.get(pipeline);
            if (pack == null) {
                return logMiss("pack null");
            }
            Object programSet = getProgramSet.invoke(pack, overworldId);
            if (programSet == null) {
                return logMiss("programSet null");
            }
            Object sourceOpt = (gbuffersProgramIdArg != null)
                    ? getGbuffersBlock.invoke(programSet, gbuffersProgramIdArg)
                    : getGbuffersBlock.invoke(programSet);
            if (sourceOpt instanceof java.util.Optional<?> opt) {
                sourceOpt = opt.orElse(null);
            }
            if (sourceOpt == null) {
                return logMiss("gbuffers source absent");
            }
            Object fragment = getFragmentSource.invoke(sourceOpt);
            if (!(fragment instanceof java.util.Optional<?> fopt)) {
                return logMiss("fragment source not Optional");
            }
            Object f = fopt.orElse(null);
            if (!(f instanceof String glsl)) {
                return logMiss("fragment source empty");
            }
            // Сигнатура схемы "packed": пары unorm2x8 + октаздрическая нормаль.
            // Обе функции приходят из include'ов пака (ProgramSet резолвит includes
            // при сборке через IncludeProcessor), так что совпадение означает, что
            // пак реально кодирует gbuffer этой схемой.
            boolean hasPacking = glsl.contains("pack_unorm_2x8");
            boolean hasOctNormal = glsl.contains("encode_unit_vector");
            if (!hasPacking || !hasOctNormal) {
                return logMiss("scheme signature not matched (pack2x8=" + hasPacking
                        + ", octNormal=" + hasOctNormal + ", len=" + glsl.length() + ")");
            }
            return true;
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-M] IrisInstancedEncoders: detection failed: {}", t.toString());
            return false;
        }
    }

    private static boolean logMiss(String reason) {
        MainRegistry.LOGGER.info("[HBM-M] IrisInstancedEncoders: no known gbuffer scheme ({})", reason);
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValueOf(Class<?> enumClass, String name) {
        try {
            return Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
