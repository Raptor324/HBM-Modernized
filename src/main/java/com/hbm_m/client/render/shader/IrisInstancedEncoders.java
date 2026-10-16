package com.hbm_m.client.render.shader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.hbm_m.main.MainRegistry;

/**
 * Selects the instanced gbuffer encoder for the active shader pack.
 *
 * <p><b>Why:</b> deferred packs write the gbuffer with THEIR OWN programs in
 * their own format; our instanced ExtendedShader must reproduce the pack's
 * format byte-for-byte, otherwise the composite decodes garbage
 * (black/broken machines).</p>
 *
 * <p><b>How detection works (automatic, no pack names):</b> fetch the pack's
 * preprocessed gbuffer program source via reflection
 * ({@code ShaderPack.getProgramSet(...).getGbuffersBlock().getFragmentSource()})
 * and look for the encoding scheme signature. If the scheme is recognized,
 * the matching encoder is enabled
 * ({@code shaders/core/iris/block_lit_instanced_packed}); otherwise
 * {@link #hasEncoder()} returns false and machines are drawn via pack
 * programs (companion per-instance) - correct under ANY pack.</p>
 *
 * <p>The "packed" encoder: gbuffer targets are packed in pairs of 8-bit
 * channels ({@code pack_unorm_2x8}: albedo.rg / albedo.b+mask / oct-encoded
 * normal / light levels), normals in world axes; light is NOT multiplied
 * into the gbuffer (computed in the deferred composite).</p>
 *
 * <p>Cache is per pipeline generation (pack switch / F3+R invalidates it
 * automatically).</p>
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
    /** ProgramId.Block argument for the unified get(ProgramId) in Iris 1.8+ (null = named getter). */
    private static Object gbuffersProgramIdArg;
    private static Method getFragmentSource;

    private IrisInstancedEncoders() {}

    /** Returns true if the active pack has a recognized instanced gbuffer encoder. */
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
     * Resource name of the instanced shader (under shaders/core/). The shadow
     * slot ALWAYS uses the base vanilla-parity shader: the shadow framebuffer
     * needs no pack-format gbuffer (depth + simple color), so shadow
     * instancing is universal for any pack. The packed encoder is for the
     * main gbuffer only.
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
                // Getters return Optional<ProgramSource>!
                // Iris 1.8+ (1.21.1) removed the named getGbuffersXxx() methods in
                // favor of a unified get(ProgramId) - try both (Oculus 1.20.1 keeps
                // the named ones; Iris 1.8.14 has only get(ProgramId.Block)).
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
            // Signature of the "packed" scheme: unorm2x8 pairs + octahedral normal.
            // Both functions come from pack includes (ProgramSet resolves includes
            // at build time via IncludeProcessor), so a match means the pack really
            // encodes its gbuffer with this scheme.
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
