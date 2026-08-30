package com.hbm_m.client.render.shader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} else if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?} else if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

/**
 * Кросс-лоадерный детектор состояния шейдеров Iris/Oculus.
 *
 * <p>Определяет активность Iris-пайплайна через рефлекшн, чтобы VBO-рендер
 * корректно маршрутизировал draw-вызовы через Iris ExtendedShader вместо
 * raw-GL против ванильного шейдера, который Iris подменил (иначе «GL No active program»).</p>
 *
 * <p><b>Loader-agnostic:</b> проверка загруженности мода делается через
 * {@link dev.architectury.platform.Platform} (работает на Forge/NeoForge/Fabric),
 * поэтому тело класса 100% общее — гейтится только клиентская аннотация
 * ({@code @OnlyIn} на forge/neoforge, {@code @Environment} на fabric).</p>
 */
//? if forge || neoforge {
@OnlyIn(Dist.CLIENT)
//?} else if fabric {
/*@Environment(EnvType.CLIENT)
*///?}
public class ShaderCompatibilityDetector {

    private ShaderCompatibilityDetector() {}

    private static boolean initialized = false;
    private static Method irisIsShaderPackInUse = null;
    private static Method irisIsRenderingShadowPass = null;
    private static Object irisApiInstance = null;

    /**
     * Hot-path MethodHandles для двух запросов к Iris API, вызываемых каждый кадр
     * (часто per-BE per-pass). {@link Method#invoke} упаковывает аргументы в
     * {@code Object[]} и каждый раз проходит access-checks рефлекшна;
     * {@link MethodHandle#invokeExact} дружелюбен к JIT и избегает обоих. Биндится
     * через {@code asType()} к {@code (Object)boolean}, чтобы call-сайты могли
     * invokeExact без знания конкретного класса IrisApi.
     */
    private static MethodHandle irisIsShaderPackInUseMH = null;
    private static MethodHandle irisIsRenderingShadowPassMH = null;

    // Кэш для оптимизации и для обращений с background-потоков (Sodium chunk builder)
    private static boolean lastState = false;
    /**
     * Thread-safe кэш: обновляется только с render-потока, читается с любых потоков.
     * Sodium строит чанки на фоновых потоках — они не могут вызывать Iris API напрямую.
     */
    private static volatile boolean cachedShaderActive = false;
    /** Отложенная инвалидация — обрабатывается в ClientTickEvent.END */
    private static volatile boolean pendingChunkInvalidation = false;

    private static void init() {
        if (initialized) return;

        if (Platform.isModLoaded("oculus") || Platform.isModLoaded("iris")) {
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
                OcclusionCullingHelper.clearCache();
                // Откладываем инвалидацию — вызов из render loop ломает итерацию Sodium (wrapped is null)
                pendingChunkInvalidation = true;
            }
            return isActive;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Вызывать из ClientTickEvent.END — инвалидирует чанки при смене шейдера.
     * НЕ вызывать из render loop — ломает итерацию Sodium (ReferenceOpenHashSet.wrapped is null).
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
     */
    public static boolean isRenderingShadowPass() {
        if (!initialized) init();
        if (irisApiInstance == null) return false;
        try {
            boolean result;
            if (irisIsRenderingShadowPassMH != null) {
                result = (boolean) irisIsRenderingShadowPassMH.invokeExact((Object) irisApiInstance);
            } else if (irisIsRenderingShadowPass != null) {
                Boolean boxed = (Boolean) irisIsRenderingShadowPass.invoke(irisApiInstance);
                result = boxed != null && boxed;
            } else {
                return false;
            }
            // Диагностика 1.21.1 (тени не отбрасываются): подтверждаем, что
            // shadow pass вообще детектится через IrisApi на этом лоадере.
            if (result && !loggedShadowPassDetected) {
                loggedShadowPassDetected = true;
                MainRegistry.LOGGER.info(
                        "ShaderCompatibilityDetector: Iris shadow pass detected (isRenderingShadowPass=true) — API works on this loader");
            }
            return result;
        } catch (Throwable e) {
            return false;
        }
    }

    private static boolean loggedShadowPassDetected = false;

    /**
     * {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer#shouldRenderOffScreen}.
     * When {@code true}, Sodium/vanilla still invoke BER even if the BE AABB is outside
     * the main camera frustum — required so shader-pack shadow maps include off-screen
     * casters whose shadows remain visible on screen.
     */
    public static boolean shouldRenderBlockEntityOffScreen() {
        return isExternalShaderActive();
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
