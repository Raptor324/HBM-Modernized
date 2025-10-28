package com.hbm_m.client.render.shader;

import com.hbm_m.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ✅ УПРОЩЕННЫЙ: Менеджер путей рендера для простого immediate рендера
 * 
 * УПРОЩЕНИЯ:
 * - Убрана сложная очистка состояния
 * - Нет управления батчами
 * - Простое переключение между путями
 */
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
    
    /**
     * ✅ УПРОЩЕННЫЙ: Обновление пути рендера
     */
    public static void updateRenderPath() {
        boolean shaderActive = ShaderCompatibilityDetector.isExternalShaderActive();
        lastKnownShaderState = shaderActive;
        lastCheckTime = System.currentTimeMillis();
        
        RenderPath newPath = shaderActive ? RenderPath.IMMEDIATE_FALLBACK : RenderPath.VBO_OPTIMIZED;
        RenderPath oldPath = currentPath.getAndSet(newPath);
        
        if (oldPath != newPath) {
            notifyPathChange(newPath);
            MainRegistry.LOGGER.info("Render path changed: {} -> {}", oldPath, newPath);
        }
    }
    
    /**
     * ✅ УПРОЩЕННАЯ: Проверка и обновление
     */
    public static void checkAndUpdate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime < CHECK_INTERVAL_MS) {
            return;
        }
        
        boolean currentShaderState = ShaderCompatibilityDetector.isExternalShaderActive();
        if (currentShaderState != lastKnownShaderState) {
            MainRegistry.LOGGER.info("Shader state changed dynamically: {} -> {}",
                lastKnownShaderState, currentShaderState);
            updateRenderPath();
        } else {
            lastCheckTime = currentTime;
        }
    }
    
    public static RenderPath getCurrentPath() {
        return currentPath.get();
    }
    
    public static boolean shouldUseFallback() {
        checkAndUpdate();
        return currentPath.get() == RenderPath.IMMEDIATE_FALLBACK;
    }
    
    /**
     * ✅ УПРОЩЕННАЯ: Принудительная установка пути
     */
    public static void forceSetPath(RenderPath path) {
        RenderPath oldPath = currentPath.getAndSet(path);
        if (oldPath != path) {
            MainRegistry.LOGGER.warn("Render path forcibly changed: {} -> {}", oldPath, path);
            
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
     * ✅ УПРОЩЕННОЕ: Уведомление о смене пути
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
     * ✅ УПРОЩЕННЫЙ: Простой сброс
     */
    public static void reset() {
        currentPath.set(RenderPath.VBO_OPTIMIZED);
        hasNotifiedUser = false;
        lastKnownShaderState = false;
        lastCheckTime = 0;
        MainRegistry.LOGGER.info("RenderPathManager reset to VBO_OPTIMIZED");
    }
}
