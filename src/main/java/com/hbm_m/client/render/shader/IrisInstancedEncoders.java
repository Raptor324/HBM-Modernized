package com.hbm_m.client.render.shader;

import java.util.ArrayList;

import com.hbm_m.main.MainRegistry;

/**
 * Selects the instanced gbuffer encoder for the active shader pack.
 *
 * <p><b>Why:</b> deferred packs write the gbuffer with THEIR OWN programs in
 * THEIR OWN format; our instanced ExtendedShader must land its outputs in the
 * pack's targets through the pack's own draw-buffers mapping, otherwise the
 * composite decodes garbage (black/broken machines).</p>
 *
 * <p><b>How the render targets work:</b> the pack's program is applied first
 * (it binds its gbuffer FBO with the draw-buffers list parsed from the
 * {@code /* RENDERTARGETS: ... *&#47;} / {@code /* DRAWBUFFERS:... *&#47;}
 * directive); we then only swap the GL program. Fragment output locations map
 * positionally onto that list, and outputs beyond the list length are
 * discarded by the GL - so we never touch draw-buffer state ourselves.</p>
 *
 * <p><b>Layout taxonomy</b> (detected per pipeline generation from the pack's
 * preprocessed gbuffer program source via reflection - mechanism signatures,
 * never pack names):</p>
 * <ul>
 *   <li>{@link Layout#PACKED_SCHEME} - gbuffer targets byte-packed in pairs
 *       of 8-bit channels: signature {@code pack_unorm_2x8} +
 *       {@code encode_unit_vector} in the pack's gbuffers source. Encoder:
 *       {@code shaders/core/iris/block_lit_instanced_packed} (single target;
 *       the pack computes lighting in its deferred composite).</li>
 *   <li>{@link Layout#EXOTIC} - anything else: pack-defined layouts (byte-paired
 *       albedo in non-zero targets, octahedral normals in high attachments, ...)
 *       AND identity-directive packs whose content encoding we cannot verify.
 *       No encoder: machines are drawn via the pack's own program (companion
 *       per-instance) - correct, just slower.</li>
 *   <li>{@link Layout#UNAVAILABLE} - Iris/Oculus reflection failed; Tier 2
 *       stays off entirely.</li>
 * </ul>
 *
 * <p><b>Why there is no "conventional identity" encoder anymore:</b> a general
 * MRT encoder (vanilla-parity colortex0..3 content) behind an identity
 * draw-buffers directive was tried and DISABLED (in-game verified 2026-10-05):
 * an identity directive does NOT imply target CONTENT parity. BSL 10.x,
 * Insanity and Flashlight all classify as identity-directive and rendered fully
 * black through the encoder - their deferred composites decode their own
 * lightmap/material encodings from colortex2/3, so our vanilla-parity content
 * (material zeros, raw light levels) composites as black. Only the PACKED_SCHEME
 * signature proves the pack's content encoding matches a shader we ship; every
 * other pack keeps the companion path (the pack's own BLOCK_ENTITY program).
 * Re-introduce a general encoder only together with a per-pack CONTENT check,
 * never from the directive alone.</p>
 *
 * <p><b>Why the directive is still parsed:</b> it no longer gates any encoder -
 * with only PACKED vs EXOTIC left, the parse feeds the EXOTIC reason string
 * only. That distinction is kept because it is diagnosable: a non-identity
 * directive is a DEFINITE mismatch (our outputs would land on the wrong
 * attachments even if the content matched), while an identity directive without
 * the packing signature is an UNVERIFIED content encoding (looks compatible,
 * is not). Log lines distinguishing the two make shader-pack triage possible.</p>
 *
 * <p>Cache is per pipeline generation (pack switch / F3+R invalidates it
 * automatically).</p>
 */
public final class IrisInstancedEncoders {

    /** Number of leading targets the identity-prefix diagnostic compares. */
    private static final int IDENTITY_PREFIX_TARGETS = 4;

    /** G-buffer layout family of the active pack (per pipeline generation). */
    private enum Layout {
        /** Reflection unavailable - Tier 2 stays off (legacy companion path). */
        UNAVAILABLE,
        /** Pack layout we cannot reproduce or verify - legacy companion path. */
        EXOTIC,
        /** Byte-paired packing scheme (pack_unorm_2x8 + encode_unit_vector). */
        PACKED_SCHEME
    }

    private static volatile long detectionGeneration = -1L;
    private static volatile boolean detectionDone = false;
    private static volatile Layout layout = Layout.UNAVAILABLE;

    private IrisInstancedEncoders() {}

    /**
     * Returns true if the active pack has a recognized instanced gbuffer
     * encoder. Only the byte-paired packing scheme qualifies - see the class
     * javadoc for why the former identity-directive ("conventional") encoder
     * is gone.
     */
    public static boolean hasEncoder() {
        return detectLayout() == Layout.PACKED_SCHEME;
    }

    /**
     * Resource name of the instanced shader (under shaders/core/). The shadow
     * slot ALWAYS uses the base vanilla-parity shader: the shadow framebuffer
     * needs no pack-format gbuffer (depth + simple color), so shadow
     * instancing is universal for any pack. The main gbuffer uses the packing
     * scheme encoder - callers must only request a main-pass name when
     * {@link #hasEncoder()} is true (there IS no main-pass instanced shader
     * for any other layout; {@code IrisInstancedShaders.getOrCreate} gates on
     * exactly that).
     */
    public static String shaderName(boolean shadowPass) {
        if (shadowPass) {
            return "hbm_m:iris/block_lit_instanced_iris";
        }
        return "hbm_m:iris/block_lit_instanced_packed";
    }

    private static Layout detectLayout() {
        long gen = IrisExtendedShaderAccess.getPipelineGeneration();
        if (detectionDone && detectionGeneration == gen) {
            return layout;
        }
        Layout detected = detect();
        if (detected != layout) {
            MainRegistry.LOGGER.info(
                    "IrisInstancedEncoders: instanced gbuffer encoder {} (pipeline generation {})",
                    describe(detected), gen);
        }
        detectionGeneration = gen;
        detectionDone = true;
        layout = detected;
        return detected;
    }

    private static String describe(Layout l) {
        switch (l) {
            case PACKED_SCHEME: return "ENABLED - byte-paired packing scheme recognized (packed encoder)";
            case EXOTIC: return "disabled - pack-defined or unverifiable content layout, companion path";
            default: return "unavailable - pipeline introspection failed";
        }
    }

    private static Layout detect() {
        try {
            if (!IrisReflectionBridge.isIrisPresent() || !IrisReflectionBridge.isReflectionAvailable()) {
                return Layout.UNAVAILABLE;
            }
            Object pipeline = IrisReflectionBridge.getLivePipeline();
            if (pipeline == null) {
                return logMiss("pipeline null");
            }
            Object pack = IrisReflectionBridge.getActivePack(pipeline);
            if (pack == null) {
                return logMiss("pack null");
            }
            Object programSet = IrisReflectionBridge.getProgramSetOverworld(pack);
            if (programSet == null) {
                return logMiss("programSet null");
            }
            // The effective program for block entities: gbuffers_block, falling
            // back to gbuffers_terrain (the same resolution Iris's program
            // resolver applies, so the directive we read governs the FBO that
            // is actually bound at draw time).
            String glsl = IrisReflectionBridge.getGbuffersBlockFragmentSource(programSet);
            if (glsl == null) {
                glsl = IrisReflectionBridge.getGbuffersTerrainFragmentSource(programSet);
            }
            if (glsl == null) {
                // No custom block/terrain programs at all: the pack either has
                // no deferred composites or synthesizes vanilla-parity fallbacks.
                // With no source to verify the content encoding against, there
                // is no proof our packed encoder matches either - companion path.
                return logLayout(Layout.EXOTIC, "no custom block/terrain programs - content encoding unverifiable");
            }
            // Byte-paired packing scheme first: unorm2x8 pairs + octahedral
            // normal. Both functions come from pack includes (ProgramSet
            // resolves includes at build time via IncludeProcessor), so a match
            // means the pack really encodes its gbuffer with this scheme.
            boolean hasPacking = glsl.contains("pack_unorm_2x8");
            boolean hasOctNormal = glsl.contains("encode_unit_vector");
            if (hasPacking && hasOctNormal) {
                return Layout.PACKED_SCHEME;
            }
            // Byte-packing helpers beyond the recognized scheme mean target
            // content is compressed (albedo/normal/lightmap packed into shared
            // channels) - our encoder would decode as garbage: companion path.
            if (usesContentPacking(glsl)) {
                return logLayout(Layout.EXOTIC, "content packing helper found");
            }
            // Diagnostic-only directive classification (see the class javadoc):
            // neither outcome enables an encoder, the reason string just says
            // WHY the pack is EXOTIC.
            int[] directive = parseDrawBuffers(glsl);
            if (!isIdentityPrefix(directive)) {
                return logLayout(Layout.EXOTIC, "draw-buffers directive is not an identity prefix: "
                        + directiveToString(directive));
            }
            return logLayout(Layout.EXOTIC, "identity directive but no packing signature - content encoding unverifiable");
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("[HBM-M] IrisInstancedEncoders: detection failed: {}", t.toString());
            return Layout.UNAVAILABLE;
        }
    }

    /** Signature of user-defined byte-packing helpers compressing target content. */
    private static boolean usesContentPacking(String glsl) {
        return glsl.contains("encodeVec2(")
                || glsl.contains("encodeVec3(")
                || glsl.contains("packUnorm4x8(")
                || glsl.contains("packUnorm2x16(")
                || glsl.contains("packSnorm2x16(")
                || glsl.contains("packHalf2x16(");
    }

    /**
     * Parses the {@code /* RENDERTARGETS: ... *&#47;} / {@code /* DRAWBUFFERS:... *&#47;}
     * directive (whichever appears first) into the ordered attachment-index
     * list. Returns an EMPTY array when no directive is present (GL default:
     * the program writes the first color attachments in order), or null when
     * a directive is present but cannot be parsed (treated as non-identity).
     */
    private static int[] parseDrawBuffers(String glsl) {
        int rt = glsl.indexOf("RENDERTARGETS");
        int db = glsl.indexOf("DRAWBUFFERS");
        if (rt < 0 && db < 0) {
            return new int[0];
        }
        if (db < 0 || (rt >= 0 && rt < db)) {
            return parseRenderTargets(glsl, rt);
        }
        return parseLegacyDrawBuffers(glsl, db);
    }

    /** Modern syntax: {@code /* RENDERTARGETS: 0,1,2,3 *&#47;} - indices 0..15. */
    private static int[] parseRenderTargets(String glsl, int at) {
        int colon = glsl.indexOf(':', at);
        int end = glsl.indexOf("*/", at);
        if (colon < 0 || end < 0 || colon > end) {
            return null;
        }
        ArrayList<Integer> list = new ArrayList<>();
        for (String token : glsl.substring(colon + 1, end).split("[,\\s]+")) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                int v = Integer.parseInt(token.trim());
                if (v < 0 || v > 15) {
                    return null;
                }
                list.add(v);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    /** Legacy syntax: {@code /* DRAWBUFFERS:0123 *&#47;} - hex digits 0..f. */
    private static int[] parseLegacyDrawBuffers(String glsl, int at) {
        int colon = glsl.indexOf(':', at);
        if (colon < 0) {
            return null;
        }
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = colon + 1; i < glsl.length(); i++) {
            int v = Character.digit(glsl.charAt(i), 16);
            if (v < 0) {
                break;
            }
            list.add(v);
        }
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    /**
     * True when output position i maps to color attachment i for the first
     * min(length, {@link #IDENTITY_PREFIX_TARGETS}) entries. Diagnostic only:
     * it does not enable anything - it just separates the "definite mapping
     * mismatch" EXOTIC reason from the "mapping fine, content unverified" one.
     */
    private static boolean isIdentityPrefix(int[] directive) {
        if (directive == null) {
            return false;
        }
        int n = Math.min(directive.length, IDENTITY_PREFIX_TARGETS);
        for (int i = 0; i < n; i++) {
            if (directive[i] != i) {
                return false;
            }
        }
        return true;
    }

    private static String directiveToString(int[] directive) {
        if (directive == null) {
            return "<unparseable>";
        }
        if (directive.length == 0) {
            return "<none - GL default order>";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < directive.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(directive[i]);
        }
        return sb.append(']').toString();
    }

    private static Layout logLayout(Layout l, String reason) {
        MainRegistry.LOGGER.info("[HBM-M] IrisInstancedEncoders: {} ({})", describe(l), reason);
        return l;
    }

    private static Layout logMiss(String reason) {
        MainRegistry.LOGGER.info("[HBM-M] IrisInstancedEncoders: no known gbuffer scheme ({})", reason);
        return Layout.UNAVAILABLE;
    }
}
