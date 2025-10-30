package com.hbm_m.client.render.shader;

import com.hbm_m.main.MainRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Детектор активных шейдерпаков через Reflection API
 * Вызывается только при событиях перезагрузки ресурсов
 */
@OnlyIn(Dist.CLIENT)
public class ShaderCompatibilityDetector {
    
    /**
     * Проверяет, активен ли какой-либо шейдерпак
     * @return true если шейдер активен, false иначе
     */
    public static boolean isExternalShaderActive() {
        return checkOculusShader() || checkIrisShader();
    }
    
    /**
     * Проверка Oculus через Reflection API
     */
    private static boolean checkOculusShader() {
        if (!ModList.get().isLoaded("oculus")) {
            return false;
        }
        
        try {
            // IrisApi - общий API для Oculus и Iris
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstanceMethod = irisApiClass.getMethod("getInstance");
            Object apiInstance = getInstanceMethod.invoke(null);
            
            Method isShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");
            Boolean inUse = (Boolean) isShaderPackInUseMethod.invoke(apiInstance);
            
            if (inUse != null && inUse) {
                MainRegistry.LOGGER.info("Oculus shader pack detected as active");
                return true;
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.debug("Oculus shader detection failed: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Проверка Iris (для Fabric порта или совместимости)
     */
    private static boolean checkIrisShader() {
        if (!ModList.get().isLoaded("iris")) {
            return false;
        }
        
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstanceMethod = irisApiClass.getMethod("getInstance");
            Object apiInstance = getInstanceMethod.invoke(null);
            
            Method isShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");
            Boolean inUse = (Boolean) isShaderPackInUseMethod.invoke(apiInstance);
            
            if (inUse != null && inUse) {
                MainRegistry.LOGGER.info("Iris shader pack detected as active");
                return true;
            }
        } catch (Exception e) {
            MainRegistry.LOGGER.debug("Iris shader detection failed: {}", e.getMessage());
        }
        
        return false;
    }
}
