package com.hbm_m.client.render.shader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hbm_m.main.MainRegistry;

/**
 * Detects the shadow-map distortion ("fisheye" around the player) of the
 * active shader pack.
 *
 * <p>Attribution / Credits:
 * Adapted from Iris (https://github.com/IrisShaders/Iris, LGPL-3.0) and
 * Oculus (https://github.com/Asek3/Oculus, LGPL-3.0).
 * Authors: coderbot, IMS212, Iris Team, Asek3.
 * Cross-referenced with CrankShaft GuestShadows (https://github.com/Engine-Room/CrankShaft, LGPL-3.0).</p>
 *
 * <p>The pack distorts the shadow map in its shadow vertex shader
 * ({@code gl_Position.xy /= distortionFactor; gl_Position.z *= scale}) and
 * compensates for the distortion when sampling shadows. Any geometry drawn
 * into the shadow map WITHOUT the same distortion samples the wrong texels -
 * shadows "crawl" with the player. The instanced shadow path must therefore
 * reproduce the pack's distortion.</p>
 *
 * <p>Detection works on the preprocessed (includes expanded) source of the
 * pack's shadow vertex program ({@code ProgramSet.getShadow()}), without
 * hardcoding pack names:
 * <ul>
 *   <li><b>quartic</b> - the {@code quartic_length} family (Photon):
 *       {@code factor = quartic_length(xy) * SHADOW_DISTORTION + (1-SHADOW_DISTORTION)},
 *       {@code z *= SHADOW_DEPTH_SCALE};</li>
 *   <li><b>linear</b> - the classic family (BSL/Complementary):
 *       {@code factor = length(xy) * bias + (1-bias)}, {@code z *= 0.2};</li>
 *   <li>no {@code distort} in the source - no distortion at all.</li>
 * </ul>
 * If the scheme is not recognized, {@link #isCompatible()} returns false and
 * the shadow batch is drawn through pack programs (per-record: correct but
 * slower). Constants are taken from {@code #define}s in the same source
 * (values already substituted by pack settings at load time).</p>
 */
public final class IrisShadowDistortion {

    /** 0 = no distortion, 1 = quartic (Photon family), 2 = linear (BSL classic). */
    private static volatile int mode = 0;
    private static volatile float distortion = 0.85f;
    private static volatile float depthScale = 1.0f;
    private static volatile boolean compatible = true; // absence of distortion is compatible

    private static volatile long detectionGeneration = -1L;
    private static volatile boolean detectionDone = false;

    private IrisShadowDistortion() {}

    /** Returns true if the instanced shadow path is compatible with the active pack. */
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
        try {
            // Default: no distortion
            mode = 0;
            distortion = 0.85f;
            depthScale = 1.0f;
            compatible = true;

            Object pipeline = IrisReflectionBridge.getLivePipeline();
            if (pipeline == null) {
                compatible = false;
                return;
            }
            Object pack = IrisReflectionBridge.getActivePack(pipeline);
            if (pack == null) {
                compatible = false;
                return;
            }
            Object programSetObj = IrisReflectionBridge.getProgramSetOverworld(pack);
            if (programSetObj == null) {
                compatible = false;
                MainRegistry.LOGGER.warn(
                        "[HBM-M] IrisShadowDistortion: ProgramSet null - instanced shadows disabled (safe fallback)");
                return;
            }
            String glsl = IrisReflectionBridge.getShadowVertexSource(programSetObj);
            if (glsl == null || glsl.isEmpty()) {
                MainRegistry.LOGGER.info(
                        "[HBM-M] IrisShadowDistortion: pack has no shadow program or vertex source - no distortion, instanced shadows on");
                return;
            }
            int len = glsl.length();
            boolean hasQuartic = glsl.contains("quartic_length");
            boolean hasDistortFactor = glsl.contains("distortFactor");
            boolean hasDistort = glsl.contains("distort");

            if (!hasDistort) {
                MainRegistry.LOGGER.info(
                        "[HBM-M] IrisShadowDistortion: no distortion in pack shadow vsh (len={}) - instanced shadows on",
                        len);
                return; // no distortion - compatible with mode 0
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
                // Iris's IncludeProcessor only expands #include under shaders/include/;
                // #include "/settings.glsl" (where Photon declares the constants) is
                // silently dropped - vshLen 29346 instead of 188k, defines absent
                // (debug.log 0913 23:40, Photon 1.2a). The settings.glsl constants are
                // not settings sliders - use the Photon family defaults.
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
            // Classic: factor = length(xy) * bias + (1-bias); z *= 0.2
            boolean linearFormula = hasDistortFactor
                    && Pattern.compile("gl_Position\\.z\\s*=\\s*gl_Position\\.z\\s*\\*\\s*0\\.2")
                            .matcher(glsl).find();
            if (linearFormula) {
                // Pack bias: a #define/const literal, or the BSL-classic formula
                // "1.0 - <C> / shadowDistance" (the definition itself lives in
                // lib/settings.glsl, which Iris does NOT expand into the source).
                // The early default of 0.85 at shadowDistance=256 gave bias 0.9, so
                // shadows "flew away" from machines (log 0913 23:41, BSL 10.1.1).
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

    private static Float extractDefine(String glsl, String name) {
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

    /** "const float shadowMapBias = 0.9;" -> 0.9 (literal; formula does not match). */
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
     * -> 25.6. Default 25.6 if no match.
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

    /** Real shadowDistance from the pack directives (Iris parses it from a const directive). */
    private static float resolveShadowDistance(Object programSetObj) {
        Float dist = IrisReflectionBridge.getShadowDistance(programSetObj);
        if (dist != null && dist > 1.0f) {
            return dist;
        }
        return 256.0f;
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
