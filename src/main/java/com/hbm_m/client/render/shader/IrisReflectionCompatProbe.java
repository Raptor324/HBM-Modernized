package com.hbm_m.client.render.shader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.hbm_m.main.MainRegistry;
import dev.architectury.platform.Platform;

import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.ShaderInstance;

/**
 * One-shot startup validation of the reflective member set the Iris/Oculus
 * compat layer in this package depends on.
 *
 * <p>Why: the whole compat surface rests on reflection over Iris/Oculus
 * internals and - for the bind-state fixes - over private statics of vanilla
 * classes. Almost every lookup has a lazy per-class init log, but the three
 * {@code ShaderBindResync} locate-by-type probes fail SILENTLY, and a partial
 * break (e.g. after an Iris update reshuffles a class) otherwise surfaces only
 * as rendering symptoms - a black sky, stuck blend state, masked frames - with
 * zero log evidence. This probe resolves every member once, at the same point
 * the production lookups first run (end of
 * {@link IrisExtendedShaderAccess#initReflection}), and produces a consolidated
 * report: one INFO line listing what resolved, plus one WARN per missing
 * member phrased as "compat degraded: &lt;member&gt; - expect &lt;symptom&gt;".
 * Running once per JVM means each WARN is inherently rate-limited to once per
 * member.</p>
 *
 * <p>Constraints, by design:
 * <ul>
 *   <li>Plain code only - no -D JVM flags, no config schema entries.</li>
 *   <li>Read-only. The probe resolves classes/methods/fields and checks enum
 *       constants; it never writes fields, never invokes Iris methods and
 *       never constructs Iris objects. Resolution semantics mirror the
 *       production walks exactly (same candidate lists - shared with
 *       {@link IrisExtendedShaderAccess} - same first-match field walks), so a
 *       member reported OK here is a member the production lookup will
 *       resolve.</li>
 *   <li>Any unexpected throwable inside a check is captured as that member's
 *       problem string; {@link #validate()} itself never propagates into
 *       {@code initReflection}.</li>
 * </ul>
 */
final class IrisReflectionCompatProbe {

    private IrisReflectionCompatProbe() {}

    private static volatile boolean validated;

    /** One reflective resolution attempt; returns null when the member resolved. */
    private interface ReflectCheck {
        String run() throws Throwable;
    }

    /** A compat-layer reflective member, its failure symptom, and the probe result. */
    private static final class Member {
        final String name;
        final String symptom;
        final boolean optional;
        final String problem; // null = resolved

        Member(String name, String symptom, boolean optional, String problem) {
            this.name = name;
            this.symptom = symptom;
            this.optional = optional;
            this.problem = problem;
        }
    }

    /**
     * Runs the whole member table once per JVM. Later calls are cheap no-ops.
     * Called from {@code IrisExtendedShaderAccess.initReflection} (guarded
     * inside: without Iris/Oculus this returns silently, matching the same
     * guard {@code initReflection} applies).
     */
    public static synchronized void validate() {
        if (validated) {
            return;
        }
        validated = true;
        if (!Platform.isModLoaded("oculus") && !Platform.isModLoaded("iris")) {
            return;
        }

        List<Member> members = new ArrayList<>();
        try {
            checkShaderBindResyncTrio(members);
            checkPipelineChain(members);
            checkPhaseGuard(members);
            checkBufferHelper(members);
            checkInstancedShaderConstruction(members);
            checkPackIntrospection(members);
            checkBlockIdDisguises(members);
            checkIrisApi(members);
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn(
                    "[HBM-M] IrisReflectionCompat: validation pass failed unexpectedly ({}) - no member report produced",
                    t.toString());
            return;
        }

        int ok = 0;
        List<String> degraded = new ArrayList<>();
        List<String> optionalAbsent = new ArrayList<>();
        for (Member m : members) {
            if (m.problem == null) {
                ok++;
            } else if (m.optional) {
                optionalAbsent.add(m.name);
            } else {
                degraded.add(m.name + " - expect " + m.symptom);
            }
        }

        if (degraded.isEmpty() && optionalAbsent.isEmpty()) {
            MainRegistry.LOGGER.info(
                    "[HBM-M] IrisReflectionCompat: all {} reflective members of the Iris compat layer resolved",
                    members.size());
            return;
        }
        MainRegistry.LOGGER.info(
                "[HBM-M] IrisReflectionCompat: {}/{} reflective members resolved, {} degraded, {} optional absent{}",
                ok, members.size(), degraded.size(), optionalAbsent.size(),
                optionalAbsent.isEmpty() ? "" : " (absent: " + String.join("; ", optionalAbsent) + ")");
        for (String line : degraded) {
            MainRegistry.LOGGER.warn("[HBM-M] IrisReflectionCompat: compat degraded: {}", line);
        }
    }

    // -- member table ---------------------------------------------------------

    /**
     * The three {@code ShaderBindResync} locate-by-type probes. Their runtime
     * lookups are catch-and-ignore, so these are the members whose failure used
     * to be completely silent (the "black screen without log evidence" class of
     * break). Each check mirrors the production field walk exactly - same
     * filters, same first-match order - so agreement here guarantees the
     * production walk resolves the field.
     */
    private static void checkShaderBindResyncTrio(List<Member> out) {
        addMember(out, "ShaderInstance.lastProgramId (static non-final int, located by type)",
                "sky/world can stay black after Oculus resets the GL program to 0 "
                        + "(ShaderBindResync.invalidateStaticProgramCache silently no-ops)",
                false, () -> {
                    for (Field f : ShaderInstance.class.getDeclaredFields()) {
                        if (f.getType() == int.class && Modifier.isStatic(f.getModifiers())
                                && !Modifier.isFinal(f.getModifiers()) && !f.isSynthetic()) {
                            return null;
                        }
                    }
                    return "no static non-final int field on ShaderInstance";
                });

        addMember(out, "Iris DepthColorStorage lock (depthColorLocked / isDepthColorLocked)",
                "the depth-color mask flag cannot be read or reset "
                        + "(ShaderBindResync.irisDepthColorDisabled returns null) - the reset falls back to direct RenderSystem mask restores",
                false, () -> {
                    Class<?> cl = Class.forName("net.irisshaders.iris.gl.blending.DepthColorStorage");
                    try {
                        cl.getDeclaredMethod("isDepthColorLocked");
                        return null;
                    } catch (Throwable ignored) {
                    }
                    for (Field f : cl.getDeclaredFields()) {
                        if (f.getType() == boolean.class && Modifier.isStatic(f.getModifiers())
                                && "depthColorLocked".equals(f.getName())) {
                            return null;
                        }
                    }
                    return "no isDepthColorLocked method or depthColorLocked boolean field on DepthColorStorage";
                });

        addMember(out, "BlendMode.lastApplied (static BlendMode field, located by type)",
                "stale blend-state cache cannot be reset - vignette/sun-moon can render as an opaque black overlay",
                false, () -> {
                    for (Field f : BlendMode.class.getDeclaredFields()) {
                        if (Modifier.isStatic(f.getModifiers()) && !f.isSynthetic() && f.getType() == BlendMode.class) {
                            return null;
                        }
                    }
                    return "no static BlendMode-typed field on BlendMode";
                });
    }

    /**
     * The pipeline-resolution chain of {@link IrisExtendedShaderAccess},
     * re-resolved through the SAME candidate lists the production lookups use
     * (referenced directly so the two cannot drift apart).
     */
    private static void checkPipelineChain(List<Member> out) {
        String symptom = "pack shaders unresolvable - all mod draws fall back to the vanilla block_lit shader under a pack";

        addMember(out, "Iris.getPipelineManager()", symptom, false, () -> {
            firstClass(IrisExtendedShaderAccess.IRIS_CLASS_CANDIDATES).getMethod("getPipelineManager");
            return null;
        });
        addMember(out, "PipelineManager.getPipelineNullable()", symptom, false, () -> {
            firstClass(IrisExtendedShaderAccess.PIPELINE_MANAGER_CANDIDATES).getMethod("getPipelineNullable");
            return null;
        });
        addMember(out, "ShaderRenderingPipeline.getShaderMap()", symptom, false, () -> {
            firstClass(IrisExtendedShaderAccess.SHADER_RENDERING_PIPELINE_CANDIDATES).getMethod("getShaderMap");
            return null;
        });
        addMember(out, "ShaderMap.getShader(ShaderKey) + ShaderKey enum class", symptom, false, () -> {
            Class<?> shaderMap = firstClass(IrisExtendedShaderAccess.SHADER_MAP_CANDIDATES);
            Class<?> shaderKey = firstClass(IrisExtendedShaderAccess.SHADER_KEY_CANDIDATES);
            shaderMap.getMethod("getShader", shaderKey);
            return null;
        });

        String beSymptom = "blockEntityId cannot be reset around our flushes - pack special-case ids "
                + "(emissive recolor, end portal) can stick to whole batches";
        addMember(out, "CapturedRenderingState.INSTANCE", beSymptom, false, () -> {
            Field instance = firstClass(IrisExtendedShaderAccess.CAPTURED_RENDERING_STATE_CANDIDATES)
                    .getField("INSTANCE");
            return Modifier.isStatic(instance.getModifiers()) ? null : "INSTANCE is not static";
        });
        addMember(out, "CapturedRenderingState.setCurrentBlockEntity(int)", beSymptom, false, () -> {
            firstClass(IrisExtendedShaderAccess.CAPTURED_RENDERING_STATE_CANDIDATES)
                    .getMethod("setCurrentBlockEntity", int.class);
            return null;
        });
        addMember(out, "CapturedRenderingState.getCurrentRenderedBlockEntity()",
                "save/restore of the previous blockEntityId falls back to 0",
                true, () -> {
                    firstClass(IrisExtendedShaderAccess.CAPTURED_RENDERING_STATE_CANDIDATES)
                            .getMethod("getCurrentRenderedBlockEntity");
                    return null;
                });
    }

    /** Mirrors IrisPhaseGuard's lookups (hardcoded net.irisshaders, like production). */
    private static void checkPhaseGuard(List<Member> out) {
        String symptom = "raw-GL batches are not tagged BLOCK_ENTITIES - pack effects keying on the "
                + "render phase can misroute them";

        addMember(out, "WorldRenderingPipeline.setPhase(WorldRenderingPhase)", symptom, false, () -> {
            Class<?> pipeline = Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPipeline");
            Class<?> phase = Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPhase");
            pipeline.getMethod("setPhase", phase);
            return null;
        });
        addMember(out, "WorldRenderingPhase.BLOCK_ENTITIES / NONE constants",
                "same as above - the phase guard degrades to a no-op", false, () -> {
                    Class<?> phase = Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPhase");
                    if (enumConstant(phase, "BLOCK_ENTITIES") == null || enumConstant(phase, "NONE") == null) {
                        return "enum constants missing";
                    }
                    return null;
                });
        addMember(out, "WorldRenderingPipeline.getPhase()",
                "phase restore takes the NONE fallback instead of the true previous phase",
                true, () -> {
                    Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPipeline").getMethod("getPhase");
                    return null;
                });
    }

    /** Mirrors IrisBufferHelper's ExtendingBufferBuilder resolution. */
    private static void checkBufferHelper(List<Member> out) {
        addMember(out, "ExtendingBufferBuilder.iris$beginWithoutExtending(Mode, VertexFormat)",
                "Iris vertex-format extension cannot be suppressed - our GUI/overlay vertex data can be "
                        + "corrupted by the extended stride",
                false, () -> probeBeginWithoutExtending(new String[] {
                        "net.irisshaders.iris.vertices.ExtendingBufferBuilder",
                        "net.coderbot.iris.vertices.ExtendingBufferBuilder"}));
        addMember(out, "Connector/Indium ExtendingBufferBuilder iris$beginWithoutExtending",
                "format-extension suppression unavailable under Connector/Indium - plain begin() fallback",
                true, () -> probeBeginWithoutExtending(new String[] {
                        "com.sinytra.forgified_fabric_api.fabric.mixin.renderer.indigo.MixinBufferBuilder",
                        "link.infra.indium.renderer.render.ExtendingBufferBuilder"}));
    }

    /** Mirrors IrisInstancedShaders' ExtendedShader construction set. */
    private static void checkInstancedShaderConstruction(List<Member> out) {
        String symptom = "instanced Iris path disabled at first use - machines fall back to per-part "
                + "rendering under a pack (high draw-call count)";

        addMember(out, "ExtendedShader 14-arg constructor", symptom, false, () -> {
            Class<?> extendedShader = Class.forName("net.irisshaders.iris.pipeline.programs.ExtendedShader");
            Class<?> glFramebuffer = Class.forName("net.irisshaders.iris.gl.framebuffer.GlFramebuffer");
            extendedShader.getDeclaredConstructor(
                    net.minecraft.server.packs.resources.ResourceProvider.class,
                    String.class,
                    VertexFormat.class,
                    boolean.class,
                    glFramebuffer,
                    glFramebuffer,
                    Class.forName("net.irisshaders.iris.gl.blending.BlendModeOverride"),
                    Class.forName("net.irisshaders.iris.gl.blending.AlphaTest"),
                    java.util.function.Consumer.class,
                    java.util.function.BiConsumer.class,
                    boolean.class,
                    Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline"),
                    java.util.List.class,
                    Class.forName("net.irisshaders.iris.uniforms.custom.CustomUniforms"));
            return null;
        });
        addMember(out, "AlphaTests.OFF", symptom, false, () -> {
            Field off = Class.forName("net.irisshaders.iris.gl.blending.AlphaTests").getField("OFF");
            return Modifier.isStatic(off.getModifiers()) ? null : "OFF is not static";
        });
        addMember(out, "CustomUniforms$Builder ctor + build(Consumer[])", symptom, false, () -> {
            Class<?> builder = Class.forName("net.irisshaders.iris.uniforms.custom.CustomUniforms$Builder");
            builder.getDeclaredConstructor();
            builder.getMethod("build", java.util.function.Consumer[].class);
            return null;
        });
        addMember(out, "GlFramebuffer ctor/addColorAttachment/addDepthAttachment/drawBuffers/destroy",
                symptom, false, () -> {
                    Class<?> fb = Class.forName("net.irisshaders.iris.gl.framebuffer.GlFramebuffer");
                    fb.getDeclaredConstructor();
                    fb.getMethod("addColorAttachment", int.class, int.class);
                    fb.getMethod("addDepthAttachment", int.class);
                    fb.getMethod("drawBuffers", int[].class);
                    fb.getMethod("destroy");
                    return null;
                });
    }

    /** Mirrors IrisInstancedEncoders + IrisShadowDistortion pack introspection. */
    private static void checkPackIntrospection(List<Member> out) {
        addMember(out, "IrisRenderingPipeline.pack (declared field, hierarchy walk)",
                "instanced gbuffer encoder and shadow distortion cannot reach the active ShaderPack - "
                        + "conservative companion paths are used",
                false, () -> {
                    Class<?> c = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
                    while (c != null) {
                        try {
                            c.getDeclaredField("pack");
                            return null;
                        } catch (NoSuchFieldException ignored) {
                            c = c.getSuperclass();
                        }
                    }
                    return "no 'pack' field in the IrisRenderingPipeline hierarchy";
                });
        addMember(out, "ShaderPack.getProgramSet(NamespacedId) + NamespacedId(String,String) ctor",
                "pack program introspection unavailable - companion paths as above",
                false, () -> {
                    Class<?> shaderPack = Class.forName("net.irisshaders.iris.shaderpack.ShaderPack");
                    Class<?> namespacedId = Class.forName(
                            "net.irisshaders.iris.shaderpack.materialmap.NamespacedId");
                    shaderPack.getMethod("getProgramSet", namespacedId);
                    namespacedId.getConstructor(String.class, String.class);
                    return null;
                });
        addMember(out, "ProgramSet gbuffers source getters (Block/Terrain)",
                "instanced gbuffer encoder stays unavailable under every pack - companion path",
                false, () -> {
                    Class<?> programSet = Class.forName("net.irisshaders.iris.shaderpack.programs.ProgramSet");
                    Class<?> programId = null;
                    try {
                        programId = Class.forName("net.irisshaders.iris.shaderpack.loading.ProgramId");
                    } catch (ClassNotFoundException ignored) {
                    }
                    if (resolveSourceGetter(programSet, "getGbuffersBlock", programId, "Block") != null
                            || resolveSourceGetter(programSet, "getGbuffersTerrain", programId, "Terrain") != null) {
                        return null;
                    }
                    return "neither named gbuffers getters nor ProgramId-based get() resolved";
                });
        addMember(out, "ProgramSource.getFragmentSource",
                "instanced gbuffer encoder cannot read pack program sources - companion path",
                false, () -> {
                    Class.forName("net.irisshaders.iris.shaderpack.programs.ProgramSource")
                            .getMethod("getFragmentSource");
                    return null;
                });
        addMember(out, "ProgramSet.getShadow (named getter or ProgramId.Shadow + get)",
                "instanced shadow distortion falls back to per-record defaults",
                false, () -> {
                    Class<?> programSet = Class.forName("net.irisshaders.iris.shaderpack.programs.ProgramSet");
                    try {
                        programSet.getMethod("getShadow");
                        return null;
                    } catch (NoSuchMethodException ignored) {
                    }
                    Class<?> programId = Class.forName("net.irisshaders.iris.shaderpack.loading.ProgramId");
                    if (enumConstant(programId, "Shadow") == null) {
                        return "neither getShadow() nor ProgramId.Shadow + get() resolved";
                    }
                    programSet.getMethod("get", programId);
                    return null;
                });
        addMember(out, "ProgramSource.getVertexSource",
                "instanced shadow distortion cannot read the shadow program source - per-record fallback",
                false, () -> {
                    Class.forName("net.irisshaders.iris.shaderpack.programs.ProgramSource")
                            .getMethod("getVertexSource");
                    return null;
                });
        addMember(out, "PackDirectives.getShadowDirectives / PackShadowDirectives.getDistance",
                "shadow distance read from conservative defaults instead of the pack directives",
                true, () -> {
                    Class.forName("net.irisshaders.iris.shaderpack.programs.ProgramSet")
                            .getMethod("getPackDirectives");
                    Class.forName("net.irisshaders.iris.shaderpack.properties.PackDirectives")
                            .getMethod("getShadowDirectives");
                    Class.forName("net.irisshaders.iris.shaderpack.properties.PackShadowDirectives")
                            .getMethod("getDistance");
                    return null;
                });
    }

    /** Mirrors ShaderBlockIdDisguises' lookups (IdMap/BlockEntry resolved by name here). */
    private static void checkBlockIdDisguises(List<Member> out) {
        String symptom = "block-id disguises disabled - pack special-case ids (end portal, emissive "
                + "recolor) can apply to our machines";

        addMember(out, "shaderpack.Iris.getInstance()/getCurrentPack()", symptom, false, () -> {
            Class<?> iris = firstClass(new String[] {
                    "net.irisshaders.iris.shaderpack.Iris",
                    "net.coderbot.iris.shaderpack.Iris"});
            iris.getMethod("getInstance");
            iris.getMethod("getCurrentPack");
            return null;
        });
        addMember(out, "ShaderPack.getIdMap()", symptom, false, () -> {
            Class.forName("net.irisshaders.iris.shaderpack.ShaderPack").getMethod("getIdMap");
            return null;
        });
        addMember(out, "IdMap.blockPropertiesMap (declared field)", symptom, false, () -> {
            Class<?> idMap = firstClass(new String[] {
                    "net.irisshaders.iris.shaderpack.IdMap",
                    "net.coderbot.iris.shaderpack.IdMap"});
            idMap.getDeclaredField("blockPropertiesMap");
            return null;
        });
        addMember(out, "BlockEntry.id() / BlockEntry(NamespacedId, Map) ctor", symptom, false, () -> {
            Class<?> namespacedId = Class.forName("net.irisshaders.iris.shaderpack.materialmap.NamespacedId");
            Class<?> blockEntry = Class.forName("net.irisshaders.iris.shaderpack.materialmap.BlockEntry");
            blockEntry.getMethod("id");
            blockEntry.getConstructor(namespacedId, java.util.Map.class);
            return null;
        });
    }

    /** Mirrors ShaderCompatibilityDetector's IrisApi v0 lookups. */
    private static void checkIrisApi(List<Member> out) {
        addMember(out, "IrisApi v0 getInstance/isShaderPackInUse/isRenderingShadowPass",
                "shader-pack state not detectable - Iris routing disabled, the vanilla path is used "
                        + "even under a pack",
                false, () -> {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    api.getMethod("getInstance");
                    api.getMethod("isShaderPackInUse");
                    api.getMethod("isRenderingShadowPass");
                    return null;
                });
    }

    // -- helpers ---------------------------------------------------------------

    private static void addMember(List<Member> out, String name, String symptom,
                                  boolean optional, ReflectCheck check) {
        String problem;
        try {
            problem = check.run();
        } catch (Throwable t) {
            problem = t.toString();
        }
        out.add(new Member(name, symptom, optional, problem));
    }

    /** Mirrors IrisBufferHelper's resolution: exact signature first, then a name+2-params walk. */
    private static String probeBeginWithoutExtending(String[] candidates) {
        for (String name : candidates) {
            try {
                Class<?> cl = Class.forName(name);
                try {
                    cl.getMethod("iris$beginWithoutExtending", VertexFormat.Mode.class, VertexFormat.class);
                    return null;
                } catch (NoSuchMethodException e) {
                    for (Method m : cl.getMethods()) {
                        if ("iris$beginWithoutExtending".equals(m.getName()) && m.getParameterCount() == 2) {
                            return null;
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return "class or method not found in any candidate package";
    }

    /** Mirrors IrisInstancedEncoders.resolveSourceGetter (named getter first, then ProgramId.get). */
    private static Method resolveSourceGetter(Class<?> programSet, String namedGetter,
                                              Class<?> programIdClass, String enumName) {
        try {
            return programSet.getMethod(namedGetter);
        } catch (NoSuchMethodException ignored) {
        }
        if (programIdClass == null) {
            return null;
        }
        try {
            if (enumConstant(programIdClass, enumName) == null) {
                return null;
            }
            return programSet.getMethod("get", programIdClass);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /** Same candidate walk as IrisExtendedShaderAccess.firstClass (kept package-visible there). */
    private static Class<?> firstClass(String[] candidates) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        throw last != null ? last : new ClassNotFoundException(String.join(", ", candidates));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(Class<?> enumClass, String name) {
        try {
            return Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
