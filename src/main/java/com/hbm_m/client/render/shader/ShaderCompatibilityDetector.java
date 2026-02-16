package com.hbm_m.client.render.shader;

import java.lang.reflect.Method;

import com.hbm_m.main.MainRegistry;

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
    
    // Кэш результата для оптимизации внутри одного кадра
    private static boolean lastState = false;

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
        if (!initialized) {
            init();
        }

        // Если API не найдено, значит шейдеров точно нет
        if (irisIsShaderPackInUse == null || irisApiInstance == null) {
            return false;
        }

        // Оптимизация: проверяем только один раз за кадр (Java-side frame counter можно эмулировать временем)
        // Но даже без этого reflection call быстрый.
        try {
            Boolean inUse = (Boolean) irisIsShaderPackInUse.invoke(irisApiInstance);
            boolean isActive = inUse != null && inUse;
            
            if (isActive != lastState) {
                MainRegistry.LOGGER.info("Shader state changed: {}", isActive ? "Active" : "Inactive");
                lastState = isActive;
                scheduleChunkInvalidation();
            }
            return isActive;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Инвалидирует чанки при смене шейдера, чтобы BakedModel пересобрал геометрию.
     * Без этого при переключении Iris геометрия остаётся старой до F3+A.
     */
    private static void scheduleChunkInvalidation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.levelRenderer != null) {
            mc.execute(() -> {
                try {
                    mc.levelRenderer.allChanged();
                } catch (Exception e) {
                    MainRegistry.LOGGER.debug("Chunk invalidation on shader change: {}", e.getMessage());
                }
            });
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