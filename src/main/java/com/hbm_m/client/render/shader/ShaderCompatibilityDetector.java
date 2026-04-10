package com.hbm_m.client.render.shader;

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
        
        if (ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris")) {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstanceMethod = irisApiClass.getMethod("getInstance");
                irisApiInstance = getInstanceMethod.invoke(null);
                irisIsShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
                irisIsRenderingShadowPass = irisApiClass.getMethod("isRenderingShadowPass");
                MainRegistry.LOGGER.info("ShaderCompatibilityDetector: API found and cached.");
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
        if (irisIsShaderPackInUse == null || irisApiInstance == null) {
            return false;
        }

        try {
            Boolean inUse = (Boolean) irisIsShaderPackInUse.invoke(irisApiInstance);
            boolean isActive = inUse != null && inUse;

            // Обновляем кэш для фоновых потоков
            cachedShaderActive = isActive;

            if (isActive != lastState) {
                MainRegistry.LOGGER.info("Shader state changed: {}", isActive ? "Active" : "Inactive");
                lastState = isActive;
                // Откладываем инвалидацию — вызов из render loop ломает итерацию Sodium (wrapped is null)
                pendingChunkInvalidation = true;
            }
            return isActive;
        } catch (Exception e) {
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
     * Используется для пропуска рендера HBM-моделей в shadow pass — даёт ~2x FPS при включённых тенях.
     */
    public static boolean isRenderingShadowPass() {
        if (!initialized) init();
        if (irisIsRenderingShadowPass == null || irisApiInstance == null) return false;
        try {
            Boolean result = (Boolean) irisIsRenderingShadowPass.invoke(irisApiInstance);
            return result != null && result;
        } catch (Exception e) {
            return false;
        }
    }
}