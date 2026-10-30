package com.hbm_m.client.render.shader;

import dev.architectury.platform.Platform;
import com.hbm_m.main.MainRegistry;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified reflective bridge between HBM-M and Iris/Oculus shader subsystems.
 * Centralizes all reflection lookup, class resolution across package variations
 * (net.irisshaders.iris vs net.coderbot.iris), pipeline traversal, shaderpack introspection,
 * phase management, block ID disguises, and ExtendedShader/GlFramebuffer instantiation.
 */
@OnlyIn(Dist.CLIENT)
public final class IrisReflectionBridge {

    private IrisReflectionBridge() {}

    private static final String[] IRIS_PACKAGES = {
            "net.irisshaders.iris",
            "net.coderbot.iris"
    };

    private static volatile boolean initialized = false;
    private static volatile boolean reflectionAvailable = false;

    // Pipeline & Shader Management
    private static Method getPipelineManagerMethod;
    private static Method getPipelineNullableMethod;
    private static Method getShaderMapMethod;
    private static Method shaderMapGetShaderMethod;
    private static Class<?> shaderRenderingPipelineClass;
    private static Class<?> shaderKeyClass;
    private static final Map<String, Object> shaderKeyCache = new ConcurrentHashMap<>();

    // CapturedRenderingState
    private static Object capturedRenderingStateInstance;
    private static Method setCurrentBlockEntityMethod;
    private static Method getCurrentBlockEntityMethod;
    private static MethodHandle setCurrentBlockEntityMH;
    private static MethodHandle getCurrentBlockEntityMH;
    private static Method getGbufferProjectionMethod;
    private static Method getGbufferModelViewMethod;

    // Phase Management
    private static Class<?> phaseEnumClass;
    private static Method setPhaseMethod;
    private static Method getPhaseMethod;
    private static MethodHandle setPhaseMH;
    private static MethodHandle getPhaseMH;
    private static Object phaseBlockEntities;
    private static Object phaseNone;

    // ShaderPack & Introspection
    private static volatile Field cachedPackField;
    private static Method shaderpackIrisGetInstance;
    private static Method shaderpackIrisGetCurrentPack;
    private static Class<?> shaderPackClass;
    private static Method getProgramSetMethod;
    private static Class<?> namespacedIdClass;
    private static Constructor<?> namespacedIdCtor;
    private static Object overworldId;

    private static Class<?> programSetClass;
    private static Class<?> programSourceClass;
    private static Method getFragmentSourceMethod;
    private static Method getVertexSourceMethod;
    private static SourceGetterRef gbuffersBlockRef;
    private static SourceGetterRef gbuffersTerrainRef;
    private static SourceGetterRef shadowRef;

    private static Method getPackDirectivesMethod;
    private static Method getShadowDirectivesMethod;
    private static Method getShadowDistanceMethod;

    private static Class<?> blockEntryClass;
    private static Constructor<?> blockEntryCtor;
    private static Method blockEntryGetIdMethod;

    // ExtendedShader & Framebuffers
    private static Constructor<?> extendedShaderCtor;
    private static Object alphaTestOff;
    private static Constructor<?> customUniformsBuilderCtor;
    private static Method customUniformsBuildMethod;
    private static Constructor<?> glFramebufferCtor;
    private static Method glFramebufferAddColor;
    private static Method glFramebufferAddDepth;
    private static Method glFramebufferDrawBuffers;
    private static Method glResourceDestroyMethod;

    public static boolean isIrisPresent() {
        return Platform.isModLoaded("iris") || Platform.isModLoaded("oculus");
    }

    public static boolean isReflectionAvailable() {
        if (!initialized) {
            init();
        }
        return reflectionAvailable;
    }

    public static Class<?> findIrisClass(String suffix) throws ClassNotFoundException {
        ClassNotFoundException lastEx = null;
        for (String pkg : IRIS_PACKAGES) {
            try {
                return Class.forName(pkg + "." + suffix);
            } catch (ClassNotFoundException e) {
                lastEx = e;
            }
        }
        throw lastEx != null ? lastEx : new ClassNotFoundException(suffix);
    }

    public static Class<?> firstClass(String... candidates) throws ClassNotFoundException {
        ClassNotFoundException lastEx = null;
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                lastEx = e;
            }
        }
        throw lastEx != null ? lastEx : new ClassNotFoundException(String.join(", ", candidates));
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        try {
            if (!isIrisPresent()) {
                return;
            }

            List<String> resolved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Pipeline lookups
        try {
            Class<?> irisClass = firstClass("net.irisshaders.iris.Iris", "net.coderbot.iris.Iris");
            getPipelineManagerMethod = irisClass.getMethod("getPipelineManager");
            Class<?> pipelineManagerClass = firstClass(
                    "net.irisshaders.iris.pipeline.PipelineManager",
                    "net.coderbot.iris.pipeline.PipelineManager");
            getPipelineNullableMethod = pipelineManagerClass.getMethod("getPipelineNullable");

            shaderRenderingPipelineClass = firstClass(
                    "net.irisshaders.iris.pipeline.ShaderRenderingPipeline",
                    "net.irisshaders.iris.pipeline.programs.ShaderRenderingPipeline",
                    "net.coderbot.iris.pipeline.ShaderRenderingPipeline",
                    "net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline");
            getShaderMapMethod = shaderRenderingPipelineClass.getMethod("getShaderMap");

            Class<?> shaderMapClass = firstClass(
                    "net.irisshaders.iris.pipeline.programs.ShaderMap",
                    "net.coderbot.iris.pipeline.newshader.ShaderMap");
            shaderKeyClass = firstClass(
                    "net.irisshaders.iris.pipeline.programs.ShaderKey",
                    "net.coderbot.iris.pipeline.newshader.ShaderKey");
            shaderMapGetShaderMethod = shaderMapClass.getMethod("getShader", shaderKeyClass);

            reflectionAvailable = true;
            resolved.add("Pipeline & ShaderMap");
        } catch (Throwable t) {
            warnings.add("Pipeline/ShaderMap resolution failed: " + t.getMessage());
        }

        // 2. CapturedRenderingState
        try {
            Class<?> capturedClass = firstClass(
                    "net.irisshaders.iris.uniforms.CapturedRenderingState",
                    "net.coderbot.iris.uniforms.CapturedRenderingState");
            capturedRenderingStateInstance = capturedClass.getField("INSTANCE").get(null);
            setCurrentBlockEntityMethod = capturedClass.getMethod("setCurrentBlockEntity", int.class);
            try {
                getCurrentBlockEntityMethod = capturedClass.getMethod("getCurrentRenderedBlockEntity");
            } catch (NoSuchMethodException ignored) {
                getCurrentBlockEntityMethod = null;
            }
            try {
                getGbufferProjectionMethod = capturedClass.getMethod("getGbufferProjection");
                getGbufferProjectionMethod.setAccessible(true);
            } catch (NoSuchMethodException ignored) {
                getGbufferProjectionMethod = null;
            }
            try {
                getGbufferModelViewMethod = capturedClass.getMethod("getGbufferModelView");
                getGbufferModelViewMethod.setAccessible(true);
            } catch (NoSuchMethodException ignored) {
                getGbufferModelViewMethod = null;
            }

            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                setCurrentBlockEntityMethod.setAccessible(true);
                setCurrentBlockEntityMH = lookup.unreflect(setCurrentBlockEntityMethod)
                        .asType(MethodType.methodType(void.class, Object.class, int.class));
                if (getCurrentBlockEntityMethod != null) {
                    getCurrentBlockEntityMethod.setAccessible(true);
                    getCurrentBlockEntityMH = lookup.unreflect(getCurrentBlockEntityMethod)
                            .asType(MethodType.methodType(int.class, Object.class));
                }
            } catch (Throwable ignored) {
                setCurrentBlockEntityMH = null;
                getCurrentBlockEntityMH = null;
            }
            resolved.add("CapturedRenderingState");
        } catch (Throwable t) {
            warnings.add("CapturedRenderingState unavailable: " + t.getMessage());
        }

        // 3. Phase Management
        try {
            Class<?> worldRenderingPipelineClass = firstClass(
                    "net.irisshaders.iris.pipeline.WorldRenderingPipeline",
                    "net.coderbot.iris.pipeline.WorldRenderingPipeline");
            phaseEnumClass = firstClass(
                    "net.irisshaders.iris.pipeline.WorldRenderingPhase",
                    "net.coderbot.iris.pipeline.WorldRenderingPhase");
            setPhaseMethod = worldRenderingPipelineClass.getMethod("setPhase", phaseEnumClass);
            try {
                getPhaseMethod = worldRenderingPipelineClass.getMethod("getPhase");
            } catch (NoSuchMethodException ignored) {
                getPhaseMethod = null;
            }

            phaseBlockEntities = enumValueOf(phaseEnumClass, "BLOCK_ENTITIES");
            phaseNone = enumValueOf(phaseEnumClass, "NONE");

            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                setPhaseMethod.setAccessible(true);
                setPhaseMH = lookup.unreflect(setPhaseMethod)
                        .asType(MethodType.methodType(void.class, Object.class, Object.class));
                if (getPhaseMethod != null) {
                    getPhaseMethod.setAccessible(true);
                    getPhaseMH = lookup.unreflect(getPhaseMethod)
                            .asType(MethodType.methodType(Object.class, Object.class));
                }
            } catch (Throwable ignored) {
                setPhaseMH = null;
                getPhaseMH = null;
            }
            resolved.add("WorldRenderingPhase");
        } catch (Throwable t) {
            warnings.add("WorldRenderingPhase unavailable: " + t.getMessage());
        }

        // 4. ShaderPack Introspection & Directives
        try {
            Class<?> spIris = firstClass(
                    "net.irisshaders.iris.shaderpack.Iris",
                    "net.coderbot.iris.shaderpack.Iris");
            shaderpackIrisGetInstance = spIris.getMethod("getInstance");
            shaderpackIrisGetCurrentPack = spIris.getMethod("getCurrentPack");

            shaderPackClass = findIrisClass("shaderpack.ShaderPack");
            namespacedIdClass = findIrisClass("shaderpack.materialmap.NamespacedId");
            namespacedIdCtor = namespacedIdClass.getConstructor(String.class, String.class);
            getProgramSetMethod = shaderPackClass.getMethod("getProgramSet", namespacedIdClass);
            overworldId = namespacedIdCtor.newInstance("minecraft", "overworld");

            programSetClass = findIrisClass("shaderpack.programs.ProgramSet");
            programSourceClass = findIrisClass("shaderpack.programs.ProgramSource");
            getFragmentSourceMethod = programSourceClass.getMethod("getFragmentSource");
            getVertexSourceMethod = programSourceClass.getMethod("getVertexSource");

            Class<?> programIdClass = null;
            try {
                programIdClass = findIrisClass("shaderpack.loading.ProgramId");
            } catch (ClassNotFoundException ignored) {
            }

            gbuffersBlockRef = resolveSourceGetter(programSetClass, "getGbuffersBlock", programIdClass, "Block");
            gbuffersTerrainRef = resolveSourceGetter(programSetClass, "getGbuffersTerrain", programIdClass, "Terrain");
            shadowRef = resolveSourceGetter(programSetClass, "getShadow", programIdClass, "Shadow");

            try {
                getPackDirectivesMethod = programSetClass.getMethod("getPackDirectives");
                Class<?> packDirClass = findIrisClass("shaderpack.properties.PackDirectives");
                getShadowDirectivesMethod = packDirClass.getMethod("getShadowDirectives");
                Class<?> shadowDirClass = findIrisClass("shaderpack.properties.PackShadowDirectives");
                getShadowDistanceMethod = shadowDirClass.getMethod("getDistance");
            } catch (Throwable ignored) {
                getPackDirectivesMethod = null;
                getShadowDirectivesMethod = null;
                getShadowDistanceMethod = null;
            }

            try {
                blockEntryClass = findIrisClass("shaderpack.materialmap.BlockEntry");
                try {
                    blockEntryCtor = blockEntryClass.getConstructor(namespacedIdClass, Map.class);
                } catch (NoSuchMethodException e) {
                    blockEntryCtor = blockEntryClass.getConstructor(namespacedIdClass);
                }
                try {
                    blockEntryGetIdMethod = blockEntryClass.getMethod("id");
                } catch (NoSuchMethodException e) {
                    blockEntryGetIdMethod = blockEntryClass.getMethod("getId");
                }
            } catch (Throwable ignored) {
                blockEntryClass = null;
            }

            resolved.add("ShaderPack/Directives/MaterialMap");
        } catch (Throwable t) {
            warnings.add("ShaderPack introspection partially degraded: " + t.getMessage());
        }

        // 5. ExtendedShader & Framebuffers
        try {
            Class<?> extendedShaderClass = findIrisClass("pipeline.programs.ExtendedShader");
            Class<?> glFramebufferClass = findIrisClass("gl.framebuffer.GlFramebuffer");

            extendedShaderCtor = extendedShaderClass.getDeclaredConstructor(
                    net.minecraft.server.packs.resources.ResourceProvider.class,
                    String.class,
                    com.mojang.blaze3d.vertex.VertexFormat.class,
                    boolean.class,
                    glFramebufferClass,
                    glFramebufferClass,
                    findIrisClass("gl.blending.BlendModeOverride"),
                    findIrisClass("gl.blending.AlphaTest"),
                    java.util.function.Consumer.class,
                    java.util.function.BiConsumer.class,
                    boolean.class,
                    findIrisClass("pipeline.IrisRenderingPipeline"),
                    java.util.List.class,
                    findIrisClass("uniforms.custom.CustomUniforms"));
            extendedShaderCtor.setAccessible(true);

            Field alphaOffField = findIrisClass("gl.blending.AlphaTests").getField("OFF");
            alphaTestOff = alphaOffField.get(null);

            Class<?> builderClass = findIrisClass("uniforms.custom.CustomUniforms$Builder");
            customUniformsBuilderCtor = builderClass.getDeclaredConstructor();
            customUniformsBuildMethod = builderClass.getMethod("build", java.util.function.Consumer[].class);

            glFramebufferCtor = glFramebufferClass.getDeclaredConstructor();
            glFramebufferCtor.setAccessible(true);
            glFramebufferAddColor = glFramebufferClass.getMethod("addColorAttachment", int.class, int.class);
            glFramebufferAddDepth = glFramebufferClass.getMethod("addDepthAttachment", int.class);
            glFramebufferDrawBuffers = glFramebufferClass.getMethod("drawBuffers", int[].class);

            Class<?> glResourceClass = findIrisClass("gl.GlResource");
            glResourceDestroyMethod = glResourceClass.getMethod("destroy");

            resolved.add("ExtendedShader/GlFramebuffer");
        } catch (Throwable t) {
            warnings.add("ExtendedShader/GlFramebuffer unavailable: " + t.getMessage());
        }

        MainRegistry.LOGGER.info("[HBM-M] IrisReflectionBridge: initialized (resolved: [{}], warnings: {})",
                String.join(", ", resolved), warnings.size());
        for (String w : warnings) {
            MainRegistry.LOGGER.warn("[HBM-M] IrisReflectionBridge: {}", w);
        }
        } finally {
            initialized = true;
        }
    }

    // ── Pipeline & Shader Access ──────────────────────────────────────────

    public static Object getPipelineManager() {
        if (!isReflectionAvailable() || getPipelineManagerMethod == null) return null;
        try {
            return getPipelineManagerMethod.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field isBeforeTranslucentField;
    private static boolean isBeforeTranslucentFieldChecked;

    public static boolean isAfterTranslucentPhase() {
        Object pipeline = getLivePipeline();
        if (pipeline == null) return false;
        try {
            if (!isBeforeTranslucentFieldChecked) {
                isBeforeTranslucentFieldChecked = true;
                Field f = pipeline.getClass().getField("isBeforeTranslucent");
                f.setAccessible(true);
                isBeforeTranslucentField = f;
            }
            if (isBeforeTranslucentField != null) {
                return !isBeforeTranslucentField.getBoolean(pipeline);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static Object getLivePipeline() {
        Object manager = getPipelineManager();
        if (manager == null || getPipelineNullableMethod == null) return null;
        try {
            return getPipelineNullableMethod.invoke(manager);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Class<?> getShaderRenderingPipelineClass() {
        ensureInitialized();
        return shaderRenderingPipelineClass;
    }

    public static Object getShaderMap(Object pipeline) {
        if (pipeline == null || getShaderMapMethod == null) return null;
        try {
            return getShaderMapMethod.invoke(pipeline);
        } catch (Throwable t) {
            return null;
        }
    }

    public static ShaderInstance getShaderFromMap(Object shaderMap, Object shaderKey) {
        if (shaderMap == null || shaderKey == null || shaderMapGetShaderMethod == null) return null;
        try {
            return (ShaderInstance) shaderMapGetShaderMethod.invoke(shaderMap, shaderKey);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object resolveShaderKey(String name) {
        ensureInitialized();
        if (shaderKeyClass == null || name == null) return null;
        return shaderKeyCache.computeIfAbsent(name, n -> {
            try {
                return enumValueOf(shaderKeyClass, n);
            } catch (Throwable t) {
                return null;
            }
        });
    }

    // ── Captured Rendering State ──────────────────────────────────────────

    public static int setCurrentBlockEntity(int id) {
        if (capturedRenderingStateInstance == null) return 0;
        int prev = 0;
        try {
            if (getCurrentBlockEntityMH != null) {
                prev = (int) getCurrentBlockEntityMH.invokeExact(capturedRenderingStateInstance);
            } else if (getCurrentBlockEntityMethod != null) {
                prev = (int) getCurrentBlockEntityMethod.invoke(capturedRenderingStateInstance);
            }
            if (setCurrentBlockEntityMH != null) {
                setCurrentBlockEntityMH.invokeExact(capturedRenderingStateInstance, id);
            } else if (setCurrentBlockEntityMethod != null) {
                setCurrentBlockEntityMethod.invoke(capturedRenderingStateInstance, id);
            }
        } catch (Throwable ignored) {
        }
        return prev;
    }

    public static void restoreCurrentBlockEntity(int prevId) {
        if (capturedRenderingStateInstance == null) return;
        try {
            if (setCurrentBlockEntityMH != null) {
                setCurrentBlockEntityMH.invokeExact(capturedRenderingStateInstance, prevId);
            } else if (setCurrentBlockEntityMethod != null) {
                setCurrentBlockEntityMethod.invoke(capturedRenderingStateInstance, prevId);
            }
        } catch (Throwable ignored) {
        }
    }

    public static Matrix4fc getGbufferProjection() {
        ensureInitialized();
        if (capturedRenderingStateInstance == null || getGbufferProjectionMethod == null) return null;
        try {
            return (Matrix4fc) getGbufferProjectionMethod.invoke(capturedRenderingStateInstance);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Matrix4fc getGbufferModelView() {
        ensureInitialized();
        if (capturedRenderingStateInstance == null || getGbufferModelViewMethod == null) return null;
        try {
            return (Matrix4fc) getGbufferModelViewMethod.invoke(capturedRenderingStateInstance);
        } catch (Throwable t) {
            return null;
        }
    }

    // ── Phase Management ──────────────────────────────────────────────────

    public static Object getPhaseBlockEntities() {
        ensureInitialized();
        return phaseBlockEntities;
    }

    public static Object getPhaseNone() {
        ensureInitialized();
        return phaseNone;
    }

    public static void setPhase(Object pipeline, Object phase) {
        if (pipeline == null || phase == null) return;
        try {
            if (setPhaseMH != null) {
                setPhaseMH.invokeExact(pipeline, phase);
            } else if (setPhaseMethod != null) {
                setPhaseMethod.invoke(pipeline, phase);
            }
        } catch (Throwable ignored) {
        }
    }

    public static Object getPhase(Object pipeline) {
        if (pipeline == null) return null;
        try {
            if (getPhaseMH != null) {
                return getPhaseMH.invokeExact(pipeline);
            } else if (getPhaseMethod != null) {
                return getPhaseMethod.invoke(pipeline);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // ── ShaderPack & Introspection ────────────────────────────────────────

    public static Object getActivePack(Object pipeline) {
        if (pipeline == null) return null;
        Field cached = cachedPackField;
        if (cached != null) {
            try {
                return cached.get(pipeline);
            } catch (Throwable ignored) {
            }
        }
        Class<?> c = pipeline.getClass();
        while (c != null && c != Object.class) {
            try {
                Field f = c.getDeclaredField("pack");
                f.setAccessible(true);
                cachedPackField = f;
                return f.get(pipeline);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (Throwable t) {
                break;
            }
        }
        return null;
    }

    public static Object getCurrentPack() {
        ensureInitialized();
        if (shaderpackIrisGetInstance == null || shaderpackIrisGetCurrentPack == null) return null;
        try {
            Object instance = shaderpackIrisGetInstance.invoke(null);
            Object packOpt = shaderpackIrisGetCurrentPack.invoke(instance);
            if (packOpt instanceof Optional<?> opt) {
                return opt.orElse(null);
            }
            return packOpt;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object getProgramSetOverworld(Object pack) {
        ensureInitialized();
        if (pack == null || getProgramSetMethod == null || overworldId == null) return null;
        try {
            return getProgramSetMethod.invoke(pack, overworldId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String getGbuffersBlockFragmentSource(Object programSet) {
        return extractFragmentSource(programSet, gbuffersBlockRef);
    }

    public static String getGbuffersTerrainFragmentSource(Object programSet) {
        return extractFragmentSource(programSet, gbuffersTerrainRef);
    }

    public static String getShadowVertexSource(Object programSet) {
        if (programSet == null || shadowRef == null || getVertexSourceMethod == null) return null;
        try {
            Object sourceOpt = shadowRef.invoke(programSet);
            if (sourceOpt instanceof Optional<?> opt) {
                sourceOpt = opt.orElse(null);
            }
            if (sourceOpt == null) return null;
            Object vshOpt = getVertexSourceMethod.invoke(sourceOpt);
            if (vshOpt instanceof Optional<?> opt) {
                Object res = opt.orElse(null);
                return res instanceof String s ? s : null;
            }
            return vshOpt instanceof String s ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Float getShadowDistance(Object programSet) {
        if (programSet == null || getPackDirectivesMethod == null ||
                getShadowDirectivesMethod == null || getShadowDistanceMethod == null) {
            return null;
        }
        try {
            Object packDirectives = getPackDirectivesMethod.invoke(programSet);
            Object shadowDirectives = getShadowDirectivesMethod.invoke(packDirectives);
            Object dist = getShadowDistanceMethod.invoke(shadowDirectives);
            if (dist instanceof Number n && n.floatValue() > 1.0f) {
                return n.floatValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Object getIdMap(Object pack) {
        if (pack == null) return null;
        try {
            Method m = pack.getClass().getMethod("getIdMap");
            return m.invoke(pack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<Integer, List<Object>> getBlockPropertiesMap(Object idMap) {
        if (idMap == null) return null;
        try {
            Field field = idMap.getClass().getDeclaredField("blockPropertiesMap");
            field.setAccessible(true);
            Object raw = field.get(idMap);
            if (raw instanceof Map) {
                return (Map<Integer, List<Object>>) raw;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Object createBlockEntry(String name) {
        ensureInitialized();
        if (blockEntryCtor == null || namespacedIdCtor == null || name == null) return null;
        try {
            int colon = name.indexOf(':');
            String ns = colon >= 0 ? name.substring(0, colon) : "minecraft";
            String path = colon >= 0 ? name.substring(colon + 1) : name;
            Object nsId = namespacedIdCtor.newInstance(ns, path);
            if (blockEntryCtor.getParameterCount() == 2) {
                return blockEntryCtor.newInstance(nsId, java.util.Collections.emptyMap());
            } else {
                return blockEntryCtor.newInstance(nsId);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String getBlockEntryId(Object blockEntry) {
        if (blockEntry == null || blockEntryGetIdMethod == null) return null;
        try {
            Object id = blockEntryGetIdMethod.invoke(blockEntry);
            if (id == null) return null;
            try {
                String ns = (String) id.getClass().getMethod("getNamespace").invoke(id);
                String name = (String) id.getClass().getMethod("getName").invoke(id);
                return ns + ":" + name;
            } catch (Throwable t) {
                return id.toString();
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ── ExtendedShader & Framebuffers ─────────────────────────────────────

    public static Object getAlphaTestOff() {
        ensureInitialized();
        return alphaTestOff;
    }

    public static Object createEmptyCustomUniforms() {
        ensureInitialized();
        if (customUniformsBuilderCtor == null || customUniformsBuildMethod == null) return null;
        try {
            Object builder = customUniformsBuilderCtor.newInstance();
            return customUniformsBuildMethod.invoke(builder, new Object[]{ new java.util.function.Consumer[0] });
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static ShaderInstance createExtendedShader(
            net.minecraft.server.packs.resources.ResourceProvider resourceProvider,
            String name,
            com.mojang.blaze3d.vertex.VertexFormat vertexFormat,
            boolean defines,
            Object fb1,
            Object fb2,
            Object blendModeOverride,
            Object alphaTest,
            java.util.function.Consumer<?> consumer,
            java.util.function.BiConsumer<?, ?> biConsumer,
            boolean flag,
            Object pipeline,
            List<?> list,
            Object customUniforms) throws Exception {
        ensureInitialized();
        if (extendedShaderCtor == null) {
            throw new IllegalStateException("ExtendedShader constructor not available");
        }
        return (ShaderInstance) extendedShaderCtor.newInstance(
                resourceProvider, name, vertexFormat, defines, fb1, fb2,
                blendModeOverride, alphaTest, consumer, biConsumer, flag,
                pipeline, list, customUniforms);
    }

    public static Object createGlFramebuffer() throws Exception {
        ensureInitialized();
        if (glFramebufferCtor == null) {
            throw new IllegalStateException("GlFramebuffer constructor not available");
        }
        return glFramebufferCtor.newInstance();
    }

    public static void addFramebufferColorAttachment(Object fb, int index, int texture) throws Exception {
        if (fb == null || glFramebufferAddColor == null) return;
        glFramebufferAddColor.invoke(fb, index, texture);
    }

    public static void addFramebufferDepthAttachment(Object fb, int texture) throws Exception {
        if (fb == null || glFramebufferAddDepth == null) return;
        glFramebufferAddDepth.invoke(fb, texture);
    }

    public static void setFramebufferDrawBuffers(Object fb, int count) throws Exception {
        if (fb == null || glFramebufferDrawBuffers == null) return;
        int[] buffers = new int[count];
        for (int i = 0; i < count; i++) {
            buffers[i] = i;
        }
        glFramebufferDrawBuffers.invoke(fb, (Object) buffers);
    }

    public static void destroyGlFramebuffer(Object fb) {
        if (fb == null) return;
        try {
            if (glResourceDestroyMethod != null) {
                glResourceDestroyMethod.invoke(fb);
            } else {
                fb.getClass().getMethod("destroy").invoke(fb);
            }
        } catch (Throwable ignored) {
        }
    }

    // ── Internal Helpers ──────────────────────────────────────────────────

    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    private static String extractFragmentSource(Object programSet, SourceGetterRef ref) {
        if (programSet == null || ref == null || getFragmentSourceMethod == null) return null;
        try {
            Object sourceOpt = ref.invoke(programSet);
            if (sourceOpt instanceof Optional<?> opt) {
                sourceOpt = opt.orElse(null);
            }
            if (sourceOpt == null) return null;
            Object fOpt = getFragmentSourceMethod.invoke(sourceOpt);
            if (fOpt instanceof Optional<?> opt) {
                Object res = opt.orElse(null);
                return res instanceof String s ? s : null;
            }
            return fOpt instanceof String s ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static SourceGetterRef resolveSourceGetter(Class<?> programSet, String namedGetter,
                                                       Class<?> programIdClass, String enumName) {
        try {
            return new SourceGetterRef(programSet.getMethod(namedGetter), null);
        } catch (NoSuchMethodException ignored) {
        }
        if (programIdClass == null) return null;
        try {
            Object arg = enumValueOf(programIdClass, enumName);
            if (arg == null) return null;
            return new SourceGetterRef(programSet.getMethod("get", programIdClass), arg);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValueOf(Class<?> enumClass, String name) {
        try {
            return Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static final class SourceGetterRef {
        final Method method;
        final Object arg;

        SourceGetterRef(Method method, Object arg) {
            this.method = method;
            this.arg = arg;
        }

        Object invoke(Object target) throws Exception {
            return (arg != null) ? method.invoke(target, arg) : method.invoke(target);
        }
    }

    // ── IrisApi v0 (pack-active / shadow-pass probes) ─────────────────────
    // Centralized so every consumer gets the same dual-package lookup
    // (net.irisshaders.iris AND the legacy net.coderbot.iris) instead of each
    // class re-rolling its own single-package Class.forName.

    private static boolean irisApiChecked;
    private static Object irisApiInstance;
    private static java.lang.invoke.MethodHandle irisPackInUseMH, irisShadowPassMH;
    private static Method irisPackInUseMethod, irisShadowPassMethod;

    /** Resolves the Iris/Oculus IrisApi v0 singleton once (no-op afterwards). */
    public static synchronized void ensureIrisApi() {
        if (irisApiChecked) {
            return;
        }
        irisApiChecked = true;
        try {
            Class<?> apiClass = findIrisClass("api.v0.IrisApi");
            Method getInstance = apiClass.getMethod("getInstance");
            irisApiInstance = getInstance.invoke(null);
            irisPackInUseMethod = apiClass.getMethod("isShaderPackInUse");
            irisShadowPassMethod = apiClass.getMethod("isRenderingShadowPass");
            try {
                java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles.lookup();
                irisPackInUseMethod.setAccessible(true);
                irisShadowPassMethod.setAccessible(true);
                irisPackInUseMH = lookup.unreflect(irisPackInUseMethod)
                        .asType(java.lang.invoke.MethodType.methodType(boolean.class, Object.class));
                irisShadowPassMH = lookup.unreflect(irisShadowPassMethod)
                        .asType(java.lang.invoke.MethodType.methodType(boolean.class, Object.class));
            } catch (Throwable mhFail) {
                irisPackInUseMH = null;
                irisShadowPassMH = null;
            }
        } catch (VirtualMachineError t) {
            throw t;
        } catch (Throwable ignored) {
            // Iris/Oculus absent or an unusable build - callers fall back to vanilla routing
        }
    }

    public static Object irisApiInstance() { return irisApiInstance; }

    public static java.lang.invoke.MethodHandle irisPackInUseHandle() { return irisPackInUseMH; }

    public static java.lang.invoke.MethodHandle irisShadowPassHandle() { return irisShadowPassMH; }

    public static Method irisPackInUseMethod() { return irisPackInUseMethod; }

    public static Method irisShadowPassMethod() { return irisShadowPassMethod; }

    // ── DepthColorStorage (Iris draw-mask lock) ───────────────────────────

    private static boolean depthColorChecked;
    private static Method depthColorIsLockedMethod;
    private static Method depthColorUnlockMethod;
    private static java.lang.reflect.Field depthColorLockedField;

    /** Resolves DepthColorStorage once (no-op afterwards). Never throws. */
    public static synchronized void ensureDepthColorReflected() {
        if (depthColorChecked) {
            return;
        }
        depthColorChecked = true;
        try {
            Class<?> cl = findIrisClass("gl.blending.DepthColorStorage");
            try {
                depthColorIsLockedMethod = cl.getDeclaredMethod("isDepthColorLocked");
                depthColorIsLockedMethod.setAccessible(true);
            } catch (Throwable ignored) {
            }
            try {
                depthColorUnlockMethod = cl.getDeclaredMethod("unlockDepthColor");
                depthColorUnlockMethod.setAccessible(true);
            } catch (Throwable ignored) {
            }
            try {
                depthColorLockedField = cl.getDeclaredField("depthColorLocked");
                depthColorLockedField.setAccessible(true);
            } catch (Throwable ignored) {
                for (java.lang.reflect.Field f : cl.getDeclaredFields()) {
                    if (f.getType() == boolean.class && java.lang.reflect.Modifier.isStatic(f.getModifiers())
                            && "depthColorLocked".equals(f.getName())) {
                        f.setAccessible(true);
                        depthColorLockedField = f;
                        break;
                    }
                }
            }
        } catch (VirtualMachineError t) {
            throw t;
        } catch (Throwable ignored) {
            // expected when Iris/Oculus is absent or relocated
        }
    }

    /**
     * Reads Iris's depth/color write lock: {@code true} = locked,
     * {@code false} = unlocked, {@code null} = Iris absent/unreadable.
     */
    public static Boolean depthColorIsLocked() {
        try {
            if (depthColorIsLockedMethod != null) {
                return (Boolean) depthColorIsLockedMethod.invoke(null);
            }
            if (depthColorLockedField != null) {
                return (Boolean) depthColorLockedField.get(null);
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Forcibly unlocks the mask (unlockDepthColor + depthColorLocked = false).
     * @return {@code true} when something was actually unlocked.
     */
    public static boolean depthColorUnlock() {
        boolean unlocked = false;
        try {
            if (depthColorUnlockMethod != null) {
                depthColorUnlockMethod.invoke(null);
                unlocked = true;
            }
            if (depthColorLockedField != null && depthColorLockedField.getBoolean(null)) {
                depthColorLockedField.setBoolean(null, false);
                unlocked = true;
            }
        } catch (Throwable ignored) {
        }
        return unlocked;
    }

    // ── ExtendingBufferBuilder (iris$beginWithoutExtending) ───────────────

    private static boolean bweChecked;
    private static Class<?> bweIrisClass;
    private static Method bweIrisMethod;
    private static Class<?> bweConnectorClass;
    private static Method bweConnectorMethod;

    /** Resolves the iris$beginWithoutExtending holders once (Iris/Oculus + Connector/Indium). */
    public static synchronized void ensureBeginWithoutExtending() {
        if (bweChecked) {
            return;
        }
        bweChecked = true;
        for (String className : new String[]{
                "net.irisshaders.iris.vertices.ExtendingBufferBuilder",
                "net.coderbot.iris.vertices.ExtendingBufferBuilder"
        }) {
            try {
                Class<?> iface = Class.forName(className);
                Method m = null;
                try {
                    m = iface.getMethod("iris$beginWithoutExtending", com.mojang.blaze3d.vertex.VertexFormat.Mode.class,
                            com.mojang.blaze3d.vertex.VertexFormat.class);
                } catch (NoSuchMethodException e) {
                    for (Method candidate : iface.getMethods()) {
                        if ("iris$beginWithoutExtending".equals(candidate.getName()) && candidate.getParameterCount() == 2) {
                            m = candidate;
                            break;
                        }
                    }
                }
                if (m != null) {
                    m.setAccessible(true);
                    bweIrisClass = iface;
                    bweIrisMethod = m;
                    break;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        for (String className : new String[]{
                "com.sinytra.forgified_fabric_api.fabric.mixin.renderer.indigo.MixinBufferBuilder",
                "link.infra.indium.renderer.render.ExtendingBufferBuilder"
        }) {
            try {
                Class<?> iface = Class.forName(className);
                for (Method candidate : iface.getMethods()) {
                    if ("iris$beginWithoutExtending".equals(candidate.getName()) && candidate.getParameterCount() == 2) {
                        candidate.setAccessible(true);
                        bweConnectorClass = iface;
                        bweConnectorMethod = candidate;
                        return;
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
    }

    private static final int BWE_GL_QUADS = 7;

    private static int bweGlMode(com.mojang.blaze3d.vertex.VertexFormat.Mode mode) {
        return switch (mode) {
            case QUADS -> BWE_GL_QUADS;
            case TRIANGLES -> 4;
            case LINES -> 1;
            case LINE_STRIP -> 3;
            case TRIANGLE_STRIP -> 5;
            case TRIANGLE_FAN -> 6;
            default -> BWE_GL_QUADS;
        };
    }

    /**
     * {@code begin} without the Iris/Connector format extension; {@code false}
     * when neither provider is present or the buffer is not extended.
     */
    public static boolean beginWithoutExtending(com.mojang.blaze3d.vertex.BufferBuilder buffer,
                                                com.mojang.blaze3d.vertex.VertexFormat.Mode mode,
                                                com.mojang.blaze3d.vertex.VertexFormat format) {
        if (buffer == null) {
            return false;
        }
        ensureBeginWithoutExtending();
        if (bweIrisClass != null && bweIrisMethod != null && bweIrisClass.isInstance(buffer)) {
            try {
                bweIrisMethod.invoke(buffer, mode, format);
                return true;
            } catch (Exception ignored) {
            }
        }
        if (bweConnectorClass != null && bweConnectorMethod != null && bweConnectorClass.isInstance(buffer)) {
            try {
                bweConnectorMethod.invoke(buffer, bweGlMode(mode), format);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
