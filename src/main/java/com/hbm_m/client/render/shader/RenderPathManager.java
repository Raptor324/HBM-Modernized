package com.hbm_m.client.render.shader;

import com.hbm_m.client.render.GlobalMeshCache;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.DoorPartAABBRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.concurrent.atomic.AtomicReference;

@OnlyIn(Dist.CLIENT)
public class RenderPathManager {

    public enum RenderPath {
        VBO_OPTIMIZED,
        IMMEDIATE_FALLBACK
    }

    private static final AtomicReference<RenderPath> currentPath = 
            new AtomicReference<>(RenderPath.VBO_OPTIMIZED);
    
    private static volatile boolean hasNotifiedUser = false;
    private static volatile boolean lastKnownShaderState = false;
    private static volatile long lastCheckTime = 0;
    private static final long CHECK_INTERVAL_MS = 1000;
    
    // НОВОЕ: Флаг для ручного переключения
    private static volatile boolean manualOverride = false;
    private static volatile RenderPath manualPath = null;

    /**
     * УПРОЩЕННЫЙ: Обновление пути рендера
     */
    public static void updateRenderPath() {
        boolean shaderActive = ShaderCompatibilityDetector.isExternalShaderActive();
        lastKnownShaderState = shaderActive;
        lastCheckTime = System.currentTimeMillis();

        // ИСПРАВЛЕНО: Учитываем ручное переключение
        RenderPath newPath;
        if (manualOverride) {
            // Если есть ручное переключение - используем его
            newPath = manualPath;
            
            // Если шейдер активировался - отменяем ручное переключение и переходим на fallback
            if (shaderActive) {
                manualOverride = false;
                manualPath = null;
                newPath = RenderPath.IMMEDIATE_FALLBACK;
                MainRegistry.LOGGER.info("Shader activated, overriding manual path selection");
            }
        } else {
            // Автоматический выбор на основе шейдера
            newPath = shaderActive ? RenderPath.IMMEDIATE_FALLBACK : RenderPath.VBO_OPTIMIZED;
        }

        RenderPath oldPath = currentPath.getAndSet(newPath);
        if (oldPath != newPath) {
            notifyPathChange(newPath);
            MainRegistry.LOGGER.info("Render path changed: {} -> {}", oldPath, newPath);
            
            // Очищаем кеши при смене пути
            clearCaches();
            
            ImmediateFallbackRenderer.onShaderReload();
        }
    }

    /**
     * УПРОЩЕННАЯ: Проверка и обновление
     */
    public static void checkAndUpdate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime < CHECK_INTERVAL_MS) {
            return;
        }

        boolean currentShaderState = ShaderCompatibilityDetector.isExternalShaderActive();
        
        // ИСПРАВЛЕНО: Обновляем только если шейдер изменился
        if (currentShaderState != lastKnownShaderState) {
            MainRegistry.LOGGER.info("Shader state changed dynamically: {} -> {}", 
                    lastKnownShaderState, currentShaderState);
            updateRenderPath();
        } else {
            // Просто обновляем время последней проверки
            lastCheckTime = currentTime;
        }
    }

    public static RenderPath getCurrentPath() {
        return currentPath.get();
    }

    public static boolean shouldUseFallback() {
        // ИСПРАВЛЕНО: Не вызываем checkAndUpdate() здесь
        // Проверка должна происходить только в onClientTick
        return currentPath.get() == RenderPath.IMMEDIATE_FALLBACK;
    }

    /**
     * УПРОЩЕННАЯ: Принудительная установка пути
     */
    public static void forceSetPath(RenderPath path) {
        RenderPath oldPath = currentPath.getAndSet(path);
        
        // НОВОЕ: Устанавливаем флаг ручного переключения
        manualOverride = true;
        manualPath = path;
        
        if (oldPath != path) {
            MainRegistry.LOGGER.warn("Render path forcibly changed: {} -> {}", oldPath, path);
            
            // Очищаем кеши при смене пути
            clearCaches();
            
            // Уведомление игроку
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(
                            Component.literal("§a[HBM] §7Render path: " + path),
                            true
                    );
                }
            });
        }
    }

    /**
     * УПРОЩЕННОЕ: Уведомление о смене пути
     */
    private static void notifyPathChange(RenderPath newPath) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        minecraft.execute(() -> {
            var player = minecraft.player;
            if (player == null) {
                return;
            }

            String message;
            if (newPath == RenderPath.IMMEDIATE_FALLBACK) {
                message = "§e[HBM] §7Shader detected. Switching to compatible renderer...";
            } else {
                message = "§a[HBM] §7Shader disabled. Returning to optimized renderer.";
            }

            player.displayClientMessage(Component.literal(message), false);
        });
    }

    /**
     * НОВОЕ: Очистка кешей
     */
    private static void clearCaches() {
        try {
            GlobalMeshCache.clearAll();
            DoorPartAABBRegistry.clear();
            MainRegistry.LOGGER.info("Caches cleared after render path change");
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Error clearing caches", e);
        }
    }

    /**
     * УПРОЩЕННЫЙ: Простой сброс
     */
    public static void reset() {
        currentPath.set(RenderPath.VBO_OPTIMIZED);
        hasNotifiedUser = false;
        lastKnownShaderState = false;
        lastCheckTime = 0;
        manualOverride = false;
        manualPath = null;
        
        // Очищаем кеши при сбросе
        clearCaches();
        
        MainRegistry.LOGGER.info("RenderPathManager reset to VBO_OPTIMIZED");
    }
    
    /**
     * НОВОЕ: Проверка, активно ли ручное переключение
     */
    public static boolean isManualOverride() {
        return manualOverride;
    }
    
    /**
     * НОВОЕ: Отмена ручного переключения
     */
    public static void clearManualOverride() {
        manualOverride = false;
        manualPath = null;
        MainRegistry.LOGGER.info("Manual override cleared");
        updateRenderPath();
    }
}