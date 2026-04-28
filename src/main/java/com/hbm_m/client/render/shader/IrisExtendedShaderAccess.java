package com.hbm_m.client.render.shader;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.hbm_m.client.render.ModShaders;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.ShaderInstance;
import dev.architectury.platform.Platform;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
*///?}
//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;//?}


/**
 * Reflective bridge to Iris/Oculus internals that exposes the
 * {@link ShaderInstance} (an {@code ExtendedShader} when Iris is active) to be
 * used for our own raw GL draws while a shader pack is in use.
 * <p>
 * Iris does not provide a public API for this; we rely on:
 * <pre>{@code
 *   Iris.getPipelineManager().getPipelineNullable()
 *      instanceof ShaderRenderingPipeline srp
 *      ? srp.getShaderMap().getShader(ShaderKey.BLOCK_ENTITY)
 *      : null;
 * }</pre>
 * If Iris is not loaded, lookup fails, or any exception occurs, this class returns
 * the vanilla simple block_lit shader so the renderer still produces output.
 */
//? if forge {
/*@OnlyIn(Dist.CLIENT)
*///?}
//? if fabric {
@Environment(EnvType.CLIENT)//?}
public final class IrisExtendedShaderAccess {

    private IrisExtendedShaderAccess() {}

    private static volatile boolean reflectionInitialized = false;
    private static volatile boolean reflectionAvailable = false;

    /** {@code Iris.getPipelineManager()} returning a {@code PipelineManager}. */
    private static Method getPipelineManager;
    /** {@code PipelineManager.getPipelineNullable()} returning a {@code WorldRenderingPipeline}. */
    private static Method getPipelineNullable;
    /** {@code ShaderRenderingPipeline.getShaderMap()} returning a {@code ShaderMap}. */
    private static Method getShaderMap;
    /** {@code ShaderMap.getShader(ShaderKey)} returning a {@code ShaderInstance}. */
    private static Method shaderMapGetShader;

    /** {@code Class<ShaderRenderingPipeline>} used for {@code instanceof} checks. */
    private static Class<?> shaderRenderingPipelineClass;
    /** {@code Class<ShaderKey>} (an enum). */
    private static Class<?> shaderKeyClass;

    /** Cached resolved {@code ShaderKey} enum constants, by candidate name. */
    private static final Map<String, Object> shaderKeyCache = new HashMap<>();

    /**
     * Prebuilt arrays of resolved {@code ShaderKey} candidates, ordered by
     * preference (see {@link #resolveShaderKeyCandidatesArray}). Built once on
     * reflection init and reused on every {@link #lookupExtendedShader} call so
     * we never allocate a new {@code ArrayList} in the per-frame hot path.
     * <p>
     * Built lazily in {@link #ensureShaderKeyCandidatesBuilt()} since
     * {@link #initReflection()} runs before the {@code ShaderKey} class is on
     * the classpath in some Iris/Oculus build orderings.
     */
    private static volatile Object[] mainShaderKeysResolved;
    private static volatile Object[] shadowShaderKeysResolved;
    private static volatile boolean shaderKeyCandidatesBuilt = false;

    /** Last actually-used keys, for one-time logging when they change (pack swap). */
    private static volatile Object lastResolvedMainKey = null;
    private static volatile Object lastResolvedShadowKey = null;

    /**
     * Per-render-pass cache of the resolved {@link ShaderInstance}. The lookup
     * walks several reflective hops (Iris → PipelineManager → ShaderRenderingPipeline
     * → ShaderMap → ShaderKey enum lookup) and was the single largest CPU cost in
     * the per-part Iris path (~8.78% of frame time per profiler trace) because each
     * draw of each part of each visible machine repeated the entire chain.
     * <p>
     * The cache is invalidated by an external counter from
     * {@code ClientModEvents.onRenderLevelStage(AFTER_BLOCK_ENTITIES)} - that hook
     * fires once per render frame, so within a single frame all draws see the cached
     * value, and pipeline rebuilds (F3+T, shader pack swap) recover within one frame.
     */
    private static volatile long currentPassId = 0L;
    private static volatile long cachedMainPassId = -1L;
    private static volatile long cachedShadowPassId = -1L;
    private static volatile ShaderInstance cachedMainShader = null;
    private static volatile ShaderInstance cachedShadowShader = null;

    /**
     * Monotonically-increasing generation counter that bumps every time the
     * underlying {@code WorldRenderingPipeline} identity changes (shader-pack
     * swap, settings-apply, dimension change, F3+T). Consumers that cache
     * program-ID-dependent state (uniform locations, resolved {@code Uniform}
     * handles, per-VAO attribute bindings) should store the generation they
     * were built against and re-resolve whenever the current value differs.
     * <p>
     * <b>Why a generation counter and not just program-ID compare.</b> Iris
     * rebuilds the pipeline by calling {@code shader.close()} on every loaded
     * shader (which hits {@code glDeleteProgram}) and then links fresh
     * programs. GL drivers - notably nvidia - freely <i>recycle</i> deleted
     * program IDs, so the post-rebuild program can land on the same integer
     * ID as one of the programs we had cached. Our cached
     * {@code cachedShaderProgram == shader.getId()} comparison then false-
     * matches and we hand stale uniform locations (from the DEAD program) to
     * {@code glUniformMatrix4fv}, triggering
     * {@code GL_INVALID_OPERATION: Uniform must be a matrix type in call to
     * UniformMatrix*} because the new program may have a non-matrix uniform
     * at that integer location. Identity-comparison via this generation
     * counter is bullet-proof against ID recycling.
     */
    private static volatile long pipelineGeneration = 0L;

    /**
     * Identity of the last-seen {@code WorldRenderingPipeline} object. Stored
     * as {@code Object} because we only care about {@code ==} comparison -
     * never call methods on it (avoids bringing Iris types onto our
     * classpath). Volatile read is cheap and happens once per frame.
     */
    private static volatile Object lastSeenPipelineIdentity = null;

    /** @return current pipeline generation; consumers compare against a stored value. */
    public static long getPipelineGeneration() {
        return pipelineGeneration;
    }

    /**
     * Bump the pass counter so the next {@link #getBlockShader} call re-resolves
     * shader instances. Call from a render-level stage event handler that fires
     * once per frame (e.g. {@code AFTER_BLOCK_ENTITIES}).
     * <p>
     * Also samples the current Iris {@code WorldRenderingPipeline} identity and
     * bumps {@link #pipelineGeneration} whenever it changes, then immediately
     * invalidates all shader/uniform caches via {@link #invalidateShaderCache}
     * and {@link IrisRenderBatch#invalidateCaches}. This is the central
     * dispatch point for pipeline-rebuild detection; called once per frame
     * from {@code RenderLevelStageEvent.AFTER_BLOCK_ENTITIES}.
     */
    public static void tickPass() {
        currentPassId++;

        // Cheap pipeline-identity check. Skips allocation when reflection is
        // unavailable (Iris not loaded) - no-op in that case. The reflective
        // hops are the same ones lookupExtendedShader() uses; they run once
        // per frame here, vs. once per BE draw there, so the cost is trivial.
        if (!reflectionInitialized) {
            initReflection();
        }
        if (!reflectionAvailable) return;
        try {
            Object pipelineManager = getPipelineManager.invoke(null);
            if (pipelineManager == null) return;
            Object pipeline = getPipelineNullable.invoke(pipelineManager);
            // Identity comparison: a different object means the pipeline was
            // rebuilt. Null→non-null and non-null→null both trigger a bump
            // because both indicate a state transition that invalidates
            // cached program IDs from the previous state.
            if (pipeline != lastSeenPipelineIdentity) {
                lastSeenPipelineIdentity = pipeline;
                pipelineGeneration++;
                invalidateShaderCache();
                IrisRenderBatch.invalidateCaches();
                MainRegistry.LOGGER.debug("IrisExtendedShaderAccess: pipeline identity changed, generation bumped to {}",
                        pipelineGeneration);
            }
        } catch (Throwable ignored) {
            // Silent: reflection failure here just means we miss a rebuild
            // event; ShaderReloadListener is still a safety net for F3+T.
        }
    }

    /**
     * Drop cached shader instances immediately - used when the Iris pipeline is
     * known to have been rebuilt (F3+T, shader pack swap) and we cannot afford
     * to wait for the next {@link #tickPass}.
     * <p>
     * Resolved shader-key arrays survive - {@code ShaderKey} enum values do not
     * change across pipeline rebuilds, only the {@code ShaderInstance} they map
     * to, which we re-resolve on the next call anyway.
     */
    public static void invalidateShaderCache() {
        cachedMainShader = null;
        cachedShadowShader = null;
        cachedMainPassId = -1L;
        cachedShadowPassId = -1L;
    }

    /** {@code CapturedRenderingState.INSTANCE} singleton. */
    private static Object capturedRenderingStateInstance;
    /** {@code CapturedRenderingState.setCurrentBlockEntity(int)}. */
    private static Method setCurrentBlockEntityMethod;
    /** {@code CapturedRenderingState.getCurrentRenderedBlockEntity()}. */
    private static Method getCurrentBlockEntityMethod;

    /**
     * MethodHandles for the per-draw {@code blockEntityId} reset path. These are
     * called from every {@code IrisRenderBatch.begin}, every {@code flushBatchIris}
     * and every {@code drawSingleWithIrisExtended} fall-through, so JIT-friendly
     * MethodHandle invocation amortises significantly better than {@link Method#invoke}
     * (which wraps args in {@code Object[]} and unboxes returns even on
     * {@code int}-returning calls). Held alongside the {@link Method} fields rather
     * than replacing them so the existing fall-back paths still compile.
     */
    private static MethodHandle setCurrentBlockEntityMH;
    private static MethodHandle getCurrentBlockEntityMH;

    /**
     * Returns an {@link ShaderInstance} suitable for rendering block entity geometry under the
     * currently-active shader pack. When no Iris pipeline is available this returns the vanilla
     * {@link ModShaders#getBlockLitSimpleShader()} as a safe fallback.
     *
     * @param shadowPass when {@code true}, attempts to fetch a {@code SHADOW_*} variant first
     */
    public static ShaderInstance getBlockShader(boolean shadowPass) {
        // Per-pass cache: avoid the multi-hop reflective walk on every part draw.
        // A shader pipeline is stable for the duration of a render frame; the
        // tickPass() bump from RenderLevelStageEvent handles invalidation between
        // frames so pipeline rebuilds (F3+T, pack swap) recover within one frame.
        long pass = currentPassId;
        if (shadowPass) {
            ShaderInstance cached = cachedShadowShader;
            if (cached != null && cachedShadowPassId == pass) {
                return cached;
            }
        } else {
            ShaderInstance cached = cachedMainShader;
            if (cached != null && cachedMainPassId == pass) {
                return cached;
            }
        }

        Object shaderInstance = lookupExtendedShader(shadowPass);
        ShaderInstance result;
        if (shaderInstance instanceof ShaderInstance casted) {
            result = casted;
        } else {
            result = ModShaders.getBlockLitSimpleShader();
        }

        if (shadowPass) {
            cachedShadowShader = result;
            cachedShadowPassId = pass;
        } else {
            cachedMainShader = result;
            cachedMainPassId = pass;
        }
        return result;
    }

    /**
     * Pack shaders (notably BSL) read the {@code blockEntityId} uniform and switch
     * branches based on {@code blockEntityId / 100}: {@code 155} triggers
     * {@code EMISSIVE_RECOLOR} which paints the surface with the warm
     * {@code blocklightCol} (the mysterious "solid red" texture players see),
     * {@code 252} triggers the {@code DrawEndPortal} path, and so on.
     * <p>
     * Iris updates this uniform from {@code CapturedRenderingState.currentRenderedBlockEntity}
     * before each block entity. Our raw GL batch never tells Iris which BE we are
     * about to draw, so the uniform sticks at whatever the previously rendered BE
     * left behind - and which BE that is depends on the camera frustum, hence the
     * "sometimes red, sometimes fine" symptom that scales worse with batching
     * (a poisoned value affects every instance in the batch). Calling this with
     * {@code 0} before our flush forces a neutral block id that does not match
     * any of BSL's special-case branches, and restoring the previous value in
     * {@code finally} keeps Iris's own subsequent draws unaffected.
     *
     * @return the previous value, or {@code Integer.MIN_VALUE} if reflection was
     *         unavailable. Pass that result back to {@link #restoreCurrentRenderedBlockEntity(int)}.
     */
    public static int setCurrentRenderedBlockEntity(int value) {
        if (!reflectionInitialized) {
            initReflection();
        }
        if (!reflectionAvailable || capturedRenderingStateInstance == null) {
            return Integer.MIN_VALUE;
        }
        try {
            // Fast path: MethodHandle.invokeExact through the asType()-adapted
            // (Object, int)void / (Object)int signature. JIT folds this to a
            // direct call in steady state.
            if (setCurrentBlockEntityMH != null) {
                int previous = (getCurrentBlockEntityMH != null)
                        ? (int) getCurrentBlockEntityMH.invokeExact(capturedRenderingStateInstance)
                        : 0;
                setCurrentBlockEntityMH.invokeExact(capturedRenderingStateInstance, value);
                return previous;
            }
            // Slow fallback: Method.invoke (only when MH binding failed at init).
            if (setCurrentBlockEntityMethod == null) return Integer.MIN_VALUE;
            int previous = (getCurrentBlockEntityMethod != null)
                    ? (int) getCurrentBlockEntityMethod.invoke(capturedRenderingStateInstance)
                    : 0;
            setCurrentBlockEntityMethod.invoke(capturedRenderingStateInstance, value);
            return previous;
        } catch (Throwable t) {
            return Integer.MIN_VALUE;
        }
    }

    /** Counterpart to {@link #setCurrentRenderedBlockEntity(int)}. No-op when {@code previous == Integer.MIN_VALUE}. */
    public static void restoreCurrentRenderedBlockEntity(int previous) {
        if (previous == Integer.MIN_VALUE) return;
        if (!reflectionAvailable || capturedRenderingStateInstance == null) return;
        try {
            if (setCurrentBlockEntityMH != null) {
                setCurrentBlockEntityMH.invokeExact(capturedRenderingStateInstance, previous);
                return;
            }
            if (setCurrentBlockEntityMethod != null) {
                setCurrentBlockEntityMethod.invoke(capturedRenderingStateInstance, previous);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Returns whether the reflective path resolved successfully (i.e. Iris classes were found).
     * Call sites can use this to decide whether attempting Iris-specific behaviour is worthwhile.
     */
    public static boolean isReflectionAvailable() {
        if (!reflectionInitialized) {
            initReflection();
        }
        return reflectionAvailable;
    }

    private static Object lookupExtendedShader(boolean shadowPass) {
        if (!reflectionInitialized) {
            initReflection();
        }
        if (!reflectionAvailable) return null;

        try {
            Object pipelineManager = getPipelineManager.invoke(null);
            if (pipelineManager == null) return null;

            Object pipeline = getPipelineNullable.invoke(pipelineManager);
            if (pipeline == null) return null;

            if (!shaderRenderingPipelineClass.isInstance(pipeline)) return null;

            Object shaderMap = getShaderMap.invoke(pipeline);
            if (shaderMap == null) return null;

            ensureShaderKeyCandidatesBuilt();
            Object[] candidates = shadowPass ? shadowShaderKeysResolved : mainShaderKeysResolved;
            // Walk the candidate list and accept the first key whose shader instance
            // ShaderMap actually exposes (a key may be present in the enum but
            // map to null if the pack didn't compile that variant - we must not
            // stop at the first name match, otherwise we get a null shader and
            // fall through to the vanilla `block_lit_simple` whose vertex format
            // does not line up with our IrisVertexFormats.ENTITY companion VBO).
            for (int i = 0; i < candidates.length; i++) {
                Object key = candidates[i];
                Object instance = shaderMapGetShader.invoke(shaderMap, key);
                if (instance != null) {
                    if (shadowPass && lastResolvedShadowKey != key) {
                        lastResolvedShadowKey = key;
                        MainRegistry.LOGGER.info("IrisExtendedShaderAccess: shadow shader resolved to {}", key);
                    } else if (!shadowPass && lastResolvedMainKey != key) {
                        lastResolvedMainKey = key;
                        MainRegistry.LOGGER.info("IrisExtendedShaderAccess: main shader resolved to {}", key);
                    }
                    return instance;
                }
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * ORDER MATTERS - every candidate's ShaderInstance has a different
     * <i>vertex format</i> baked into its {@code glBindAttribLocation} calls.
     * We build our IrisCompanionMesh in IrisVertexFormats.ENTITY, so we MUST
     * pick a key whose ShaderInstance was constructed with the same format,
     * otherwise the bound program reads "Color" out of the mc_Entity slot,
     * normals from the wrong offset, etc. - silent garbage rather than a
     * GL error.
     * <p>
     * Shadow pass: SHADOW_TERRAIN_CUTOUT is TERRAIN-format and would corrupt
     * the cascaded shadow map (which BSL then samples from in the main pass -
     * that's the "missing pieces / broken textures, depends on camera angle"
     * symptom, since the shadow frustum follows the camera).
     * SHADOW_ENTITIES_CUTOUT is ENTITY-format → safe.
     * <p>
     * Main pass: BLOCK_ENTITY is ENTITY-format, BLOCK is TERRAIN-format -
     * again prefer the ENTITY-formatted variant.
     */
    private static final String[] MAIN_SHADER_KEY_NAMES = {
            "BLOCK_ENTITY", "BLOCK_ENTITY_DIFFUSE", "ENTITIES_CUTOUT",
            "BLOCK", "TERRAIN_CUTOUT", "TERRAIN"};
    private static final String[] SHADOW_SHADER_KEY_NAMES = {
            "SHADOW_ENTITIES_CUTOUT", "SHADOW_BLOCK_ENTITY",
            "SHADOW_TERRAIN_CUTOUT", "SHADOW_TERRAIN"};

    /**
     * Builds {@link #mainShaderKeysResolved} / {@link #shadowShaderKeysResolved}
     * once. Idempotent and cheap on subsequent calls - checked under
     * {@code shaderKeyCandidatesBuilt} fence so the per-frame
     * {@link #lookupExtendedShader} path skips the {@code synchronized}.
     */
    private static void ensureShaderKeyCandidatesBuilt() {
        if (shaderKeyCandidatesBuilt) return;
        synchronized (shaderKeyCache) {
            if (shaderKeyCandidatesBuilt) return;
            mainShaderKeysResolved = resolveShaderKeyCandidatesArray(MAIN_SHADER_KEY_NAMES);
            shadowShaderKeysResolved = resolveShaderKeyCandidatesArray(SHADOW_SHADER_KEY_NAMES);
            shaderKeyCandidatesBuilt = true;
        }
    }

    private static Object[] resolveShaderKeyCandidatesArray(String[] names) {
        Object[] tmp = new Object[names.length];
        int n = 0;
        for (String name : names) {
            Object k = resolveSingleKey(name);
            if (k != null) tmp[n++] = k;
        }
        if (n == names.length) return tmp;
        Object[] trimmed = new Object[n];
        System.arraycopy(tmp, 0, trimmed, 0, n);
        return trimmed;
    }

    private static Object resolveSingleKey(String name) {
        Object cached = shaderKeyCache.get(name);
        if (cached != null) return cached;
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object value = Enum.valueOf((Class<Enum>) shaderKeyClass.asSubclass(Enum.class), name);
            shaderKeyCache.put(name, value);
            return value;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        if (!Platform.isModLoaded("oculus") && !Platform.isModLoaded("iris")) {
            return;
        }
        // Iris/Oculus has shuffled package layout across versions. Try a few well-known names.
        String[] irisClassCandidates = {
                "net.irisshaders.iris.Iris",
                "net.coderbot.iris.Iris"
        };
        String[] pipelineManagerCandidates = {
                "net.irisshaders.iris.pipeline.PipelineManager",
                "net.coderbot.iris.pipeline.PipelineManager"
        };
        // ShaderRenderingPipeline lives at .pipeline.* in current Oculus/Iris on 1.20.1,
        // and historically at .pipeline.programs.* in some earlier mid-cycle builds.
        String[] shaderRenderingPipelineCandidates = {
                "net.irisshaders.iris.pipeline.ShaderRenderingPipeline",
                "net.irisshaders.iris.pipeline.programs.ShaderRenderingPipeline",
                "net.coderbot.iris.pipeline.ShaderRenderingPipeline",
                "net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline"
        };
        String[] shaderMapCandidates = {
                "net.irisshaders.iris.pipeline.programs.ShaderMap",
                "net.coderbot.iris.pipeline.newshader.ShaderMap"
        };
        String[] shaderKeyCandidates = {
                "net.irisshaders.iris.pipeline.programs.ShaderKey",
                "net.coderbot.iris.pipeline.newshader.ShaderKey"
        };
        String[] capturedRenderingStateCandidates = {
                "net.irisshaders.iris.uniforms.CapturedRenderingState",
                "net.coderbot.iris.uniforms.CapturedRenderingState"
        };
        try {
            Class<?> irisClass = firstClass(irisClassCandidates);
            getPipelineManager = irisClass.getMethod("getPipelineManager");

            Class<?> pipelineManagerClass = firstClass(pipelineManagerCandidates);
            getPipelineNullable = pipelineManagerClass.getMethod("getPipelineNullable");

            shaderRenderingPipelineClass = firstClass(shaderRenderingPipelineCandidates);
            getShaderMap = shaderRenderingPipelineClass.getMethod("getShaderMap");

            Class<?> shaderMapClass = firstClass(shaderMapCandidates);
            shaderKeyClass = firstClass(shaderKeyCandidates);
            shaderMapGetShader = shaderMapClass.getMethod("getShader", shaderKeyClass);

            // Optional: CapturedRenderingState lookup. Failure here does not disable
            // the rest of the reflection - it just means we cannot reset blockEntityId
            // around our flushes (the user will see BSL's "red" symptom under bad luck).
            try {
                Class<?> capturedClass = firstClass(capturedRenderingStateCandidates);
                capturedRenderingStateInstance = capturedClass.getField("INSTANCE").get(null);
                setCurrentBlockEntityMethod = capturedClass.getMethod("setCurrentBlockEntity", int.class);
                try {
                    getCurrentBlockEntityMethod = capturedClass.getMethod("getCurrentRenderedBlockEntity");
                } catch (NoSuchMethodException ignored) {
                    getCurrentBlockEntityMethod = null;
                }

                // Hot-path acceleration: convert the per-draw reflective hops
                // into MethodHandles. They invoke through the same JIT-friendly
                // path as direct calls once the call site stabilises, avoiding
                // Method.invoke's Object[] boxing and reflection access checks.
                //
                // We asType() to (Object, int)void / (Object)int so the call
                // sites can use invokeExact without knowing the concrete owner
                // class - invokeExact is significantly faster than invoke()
                // because it skips runtime signature adaptation.
                try {
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    setCurrentBlockEntityMethod.setAccessible(true);
                    setCurrentBlockEntityMH = lookup.unreflect(setCurrentBlockEntityMethod)
                            .asType(MethodType.methodType(void.class, Object.class, int.class));
                    if (getCurrentBlockEntityMethod != null) {
                        getCurrentBlockEntityMethod.setAccessible(true);
                        getCurrentBlockEntityMH = lookup.unreflect(getCurrentBlockEntityMethod)
                                .asType(MethodType.methodType(int.class, Object.class));
                    } else {
                        getCurrentBlockEntityMH = null;
                    }
                } catch (Throwable mhFail) {
                    MainRegistry.LOGGER.warn("IrisExtendedShaderAccess: MethodHandle binding failed ({}), falling back to Method.invoke",
                            mhFail.toString());
                    setCurrentBlockEntityMH = null;
                    getCurrentBlockEntityMH = null;
                }
            } catch (Throwable t) {
                MainRegistry.LOGGER.warn("IrisExtendedShaderAccess: CapturedRenderingState lookup failed ({}), blockEntityId reset disabled", t.toString());
                capturedRenderingStateInstance = null;
                setCurrentBlockEntityMethod = null;
                getCurrentBlockEntityMethod = null;
                setCurrentBlockEntityMH = null;
                getCurrentBlockEntityMH = null;
            }

            reflectionAvailable = true;
            MainRegistry.LOGGER.info("IrisExtendedShaderAccess: reflection cached successfully (pipeline class: {}, blockEntityId reset: {})",
                    shaderRenderingPipelineClass.getName(),
                    setCurrentBlockEntityMethod != null);
        } catch (Throwable t) {
            MainRegistry.LOGGER.warn("IrisExtendedShaderAccess: reflection unavailable ({}), falling back to vanilla", t.toString());
        }
    }

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
}
