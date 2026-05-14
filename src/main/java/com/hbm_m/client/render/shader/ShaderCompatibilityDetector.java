//? if forge {
package com.hbm_m.client.render.shader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

@OnlyIn(Dist.CLIENT)
public class ShaderCompatibilityDetector {

    private static boolean initialized = false;
    private static Method irisIsShaderPackInUse = null;
    private static Method irisIsRenderingShadowPass = null;
    private static Object irisApiInstance = null;

    /**
     * Hot-path MethodHandles for the two Iris API queries called every frame
     * (often per-BE per-pass). {@link Method#invoke} boxes args into an
     * {@code Object[]} and goes through reflection access checks on every call;
     * {@link MethodHandle#invokeExact} is JIT-friendly and avoids both. Bound
     * with {@code asType()} to {@code (Object)boolean} so call sites can
     * invokeExact without knowing the concrete IrisApi class.
     */
    private static MethodHandle irisIsShaderPackInUseMH = null;
    private static MethodHandle irisIsRenderingShadowPassMH = null;
    
    // Кэш для оптимизации и для обращений с background-потоков (Sodium chunk builder)
    private static boolean lastState = false;
    /**
     * Thread-safe кэш: обновляется только с render-потока, читается с любых потоков.
     * Sodium строит чанки на фоновых потоках - они не могут вызывать Iris API напрямую.
     */
    private static volatile boolean cachedShaderActive = false;
    /** Отложенная инвалидация - обрабатывается в ClientTickEvent.END */
    private static volatile boolean pendingChunkInvalidation = false;

    private static void init() {
        if (initialized) return;
        
        if (ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris")) {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstanceMethod = irisApiClass.getMethod("getInstance");
                irisApiInstance = getInstanceMethod.invoke(null);
                irisIsShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
                irisIsRenderingShadowPass = irisApiClass.getMethod("isRenderingShadowPass");

                // MethodHandle bind. Both methods return primitive `boolean`,
                // so adapt to (Object)boolean so the call sites can invokeExact
                // without an extra unboxing hop.
                try {
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    irisIsShaderPackInUse.setAccessible(true);
                    irisIsRenderingShadowPass.setAccessible(true);
                    irisIsShaderPackInUseMH = lookup.unreflect(irisIsShaderPackInUse)
                            .asType(MethodType.methodType(boolean.class, Object.class));
                    irisIsRenderingShadowPassMH = lookup.unreflect(irisIsRenderingShadowPass)
                            .asType(MethodType.methodType(boolean.class, Object.class));
                } catch (Throwable mhFail) {
                    MainRegistry.LOGGER.warn("ShaderCompatibilityDetector: MethodHandle binding failed ({}), using Method.invoke", mhFail.toString());
                    irisIsShaderPackInUseMH = null;
                    irisIsRenderingShadowPassMH = null;
                }

                MainRegistry.LOGGER.info("ShaderCompatibilityDetector: API found and cached (MH={}).",
                        irisIsShaderPackInUseMH != null);
            } catch (Exception e) {
                MainRegistry.LOGGER.error("ShaderCompatibilityDetector: Failed to cache API", e);
            }
        }
        initialized = true;
    }

    public static boolean isExternalShaderActive() {
        // Sodium строит чанки на фоновых потоках. Вызов Iris API с фонового потока небезопасен
        // (Iris хранит состояние в thread-locals render-потока). Возвращаем кэш.
        if (!RenderSystem.isOnRenderThread()) {
            return cachedShaderActive;
        }

        if (!initialized) {
            init();
        }

        // Если API не найдено, значит шейдеров точно нет
        if (irisApiInstance == null || (irisIsShaderPackInUseMH == null && irisIsShaderPackInUse == null)) {
            return false;
        }

        try {
            boolean isActive;
            if (irisIsShaderPackInUseMH != null) {
                isActive = (boolean) irisIsShaderPackInUseMH.invokeExact((Object) irisApiInstance);
            } else {
                Boolean inUse = (Boolean) irisIsShaderPackInUse.invoke(irisApiInstance);
                isActive = inUse != null && inUse;
            }

            // Обновляем кэш для фоновых потоков
            cachedShaderActive = isActive;

            if (isActive != lastState) {
                MainRegistry.LOGGER.info("Shader state changed: {}", isActive ? "Active" : "Inactive");
                lastState = isActive;
                // Откладываем инвалидацию - вызов из render loop ломает итерацию Sodium (wrapped is null)
                pendingChunkInvalidation = true;
            }
            return isActive;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Вызывать из ClientTickEvent.END - инвалидирует чанки при смене шейдера.
     * НЕ вызывать из render loop - ломает итерацию Sodium (ReferenceOpenHashSet.wrapped is null).
     */
    public static void processPendingChunkInvalidation() {
        if (!pendingChunkInvalidation) return;
        pendingChunkInvalidation = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            try {
                mc.levelRenderer.allChanged();
            } catch (Exception e) {
                MainRegistry.LOGGER.debug("Chunk invalidation on shader change: {}", e.getMessage());
            }
        }
    }

    /**
     * Проверяет, рендерится ли сейчас shadow pass Iris (для realtime shadows).
     * Используется для пропуска рендера HBM-моделей в shadow pass - даёт ~2x FPS при включённых тенях.
     */
    public static boolean isRenderingShadowPass() {
        if (!initialized) init();
        if (irisApiInstance == null) return false;
        try {
            if (irisIsRenderingShadowPassMH != null) {
                return (boolean) irisIsRenderingShadowPassMH.invokeExact((Object) irisApiInstance);
            }
            if (irisIsRenderingShadowPass == null) return false;
            Boolean result = (Boolean) irisIsRenderingShadowPass.invoke(irisApiInstance);
            return result != null && result;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * True when an active Iris pipeline can hand out an {@code ExtendedShader} for our raw-GL
     * draws. When false, callers should fall back to the vanilla shader path or to
     * {@code bufferSource.putBulkData} delegation.
     */
    public static boolean canUseIrisExtendedShader() {
        return isExternalShaderActive() && IrisExtendedShaderAccess.isReflectionAvailable();
    }

    /**
     * Статическая геометрия машин/дверей всегда предоставляется BER/VBO системой.
     * Baked world quads для этих моделей не используются.
     */
    public static boolean useVboGeometry() {
        return true;
    }
}
//?}

//? if fabric {
/*package com.hbm_m.client.render.shader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;

/^*
 * Fabric Iris/Oculus integration.
 * Detects Iris shader state via reflection so the VBO render pipeline
 * correctly routes draws through the Iris ExtendedShader path instead
 * of attempting raw GL draws against a vanilla shader that Iris has
 * replaced (which produces "GL No active program" errors).
 ^/
public class ShaderCompatibilityDetector {

    private ShaderCompatibilityDetector() {}

    private static boolean initialized = false;
    private static Object irisApiInstance = null;
    private static MethodHandle irisIsShaderPackInUseMH = null;
    private static MethodHandle irisIsRenderingShadowPassMH = null;
    private static Method irisIsShaderPackInUse = null;
    private static Method irisIsRenderingShadowPass = null;

    private static boolean lastState = false;
    private static volatile boolean cachedShaderActive = false;
    private static volatile boolean pendingChunkInvalidation = false;

    private static void init() {
        if (initialized) return;

        if (Platform.isModLoaded("iris") || Platform.isModLoaded("oculus")) {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstanceMethod = irisApiClass.getMethod("getInstance");
                irisApiInstance = getInstanceMethod.invoke(null);
                irisIsShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
                irisIsRenderingShadowPass = irisApiClass.getMethod("isRenderingShadowPass");

                try {
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    irisIsShaderPackInUse.setAccessible(true);
                    irisIsRenderingShadowPass.setAccessible(true);
                    irisIsShaderPackInUseMH = lookup.unreflect(irisIsShaderPackInUse)
                            .asType(MethodType.methodType(boolean.class, Object.class));
                    irisIsRenderingShadowPassMH = lookup.unreflect(irisIsRenderingShadowPass)
                            .asType(MethodType.methodType(boolean.class, Object.class));
                } catch (Throwable mhFail) {
                    MainRegistry.LOGGER.warn("ShaderCompatibilityDetector: MethodHandle binding failed ({}), using Method.invoke", mhFail.toString());
                    irisIsShaderPackInUseMH = null;
                    irisIsRenderingShadowPassMH = null;
                }

                MainRegistry.LOGGER.info("ShaderCompatibilityDetector: Iris API found and cached (MH={}).",
                        irisIsShaderPackInUseMH != null);
            } catch (Exception e) {
                MainRegistry.LOGGER.error("ShaderCompatibilityDetector: Failed to cache Iris API", e);
            }
        }
        initialized = true;
    }

    public static boolean isExternalShaderActive() {
        if (!RenderSystem.isOnRenderThread()) {
            return cachedShaderActive;
        }

        if (!initialized) {
            init();
        }

        if (irisApiInstance == null || (irisIsShaderPackInUseMH == null && irisIsShaderPackInUse == null)) {
            return false;
        }

        try {
            boolean isActive;
            if (irisIsShaderPackInUseMH != null) {
                isActive = (boolean) irisIsShaderPackInUseMH.invokeExact((Object) irisApiInstance);
            } else {
                Boolean inUse = (Boolean) irisIsShaderPackInUse.invoke(irisApiInstance);
                isActive = inUse != null && inUse;
            }

            cachedShaderActive = isActive;

            if (isActive != lastState) {
                MainRegistry.LOGGER.info("Shader state changed: {}", isActive ? "Active" : "Inactive");
                lastState = isActive;
                pendingChunkInvalidation = true;
            }
            return isActive;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void processPendingChunkInvalidation() {
        if (!pendingChunkInvalidation) return;
        pendingChunkInvalidation = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            try {
                mc.levelRenderer.allChanged();
            } catch (Exception e) {
                MainRegistry.LOGGER.debug("Chunk invalidation on shader change: {}", e.getMessage());
            }
        }
    }

    public static boolean isRenderingShadowPass() {
        if (!initialized) init();
        if (irisApiInstance == null) return false;
        try {
            if (irisIsRenderingShadowPassMH != null) {
                return (boolean) irisIsRenderingShadowPassMH.invokeExact((Object) irisApiInstance);
            }
            if (irisIsRenderingShadowPass == null) return false;
            Boolean result = (Boolean) irisIsRenderingShadowPass.invoke(irisApiInstance);
            return result != null && result;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean canUseIrisExtendedShader() {
        return isExternalShaderActive() && IrisExtendedShaderAccess.isReflectionAvailable();
    }

    public static boolean useVboGeometry() {
        return true;
    }
}
*///?}