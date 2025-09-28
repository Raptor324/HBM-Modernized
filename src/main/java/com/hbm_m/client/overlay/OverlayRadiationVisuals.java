package com.hbm_m.client.overlay;

// Оверлей для визуальных эффектов радиации (пиксели на экране).
// Пиксели появляются с вероятностью, зависящей от уровня радиации в окружающей среде.
// Использует IGuiOverlay из Forge для интеграции с GUI игры.
import com.hbm_m.config.ModClothConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class OverlayRadiationVisuals {
    private static final RandomSource random = RandomSource.create();
    private static final List<RadiationPixel> activePixels = new ArrayList<>();

    private static class RadiationPixel {
        int x, y, color;
        int timeToLive;

        public RadiationPixel(int x, int y, int color, int timeToLive) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.timeToLive = timeToLive;
        }

        public boolean tick() {
            this.timeToLive--;
            return this.timeToLive > 0;
        }
    }

    public static final IGuiOverlay RADIATION_PIXELS_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        ModClothConfig config = ModClothConfig.get();
        var player = Minecraft.getInstance().player;
        
        activePixels.removeIf(pixel -> !pixel.tick());

        if (!config.enableRadiationPixelEffect || player == null || player.isCreative() || player.isSpectator()) {
            // ... (логика отключения остается прежней)
        } else {
            float incomingRadiation = OverlayGeiger.clientTotalEnvironmentRadiation;
            
            // --- ИСПРАВЛЕНИЕ №1: Используем порог из конфига ---
            // Теперь эта настройка действительно работает.
            float threshold = config.radiationPixelEffectThreshold; 

            if (incomingRadiation >= threshold) {
                int maxDots = config.radiationPixelEffectMaxDots;
                float maxIntensityRad = config.radiationPixelMaxIntensityRad;

                float intensity = Mth.clamp(Mth.inverseLerp(incomingRadiation, threshold, maxIntensityRad), 0.0f, 1.0f);

                // --- ИСПРАВЛЕНИЕ №2: Вероятностный спавн пикселей ---
                // 1. Вычисляем желаемое количество НОВЫХ пикселей как float, не отбрасывая дробную часть.
                float desiredNewPixels = maxDots * intensity * 0.1f;

                // 2. Целая часть - это количество пикселей, которые мы спавним гарантированно.
                int guaranteedPixels = (int) desiredNewPixels;
                
                // 3. Дробная часть - это шанс заспавнить еще один дополнительный пиксель.
                float chanceForExtraPixel = desiredNewPixels - guaranteedPixels;

                // 4. Спавним гарантированные пиксели
                for (int i = 0; i < guaranteedPixels; i++) {
                    spawnRandomPixel(width, height, config);
                }

                // 5. Проверяем шанс на спавн дополнительного пикселя
                if (random.nextFloat() < chanceForExtraPixel) {
                    spawnRandomPixel(width, height, config);
                }
            }
        }
        
        for (RadiationPixel pixel : activePixels) {
            guiGraphics.fill(pixel.x, pixel.y, pixel.x + 1, pixel.y + 1, pixel.color);
        }
    };

    /**
     * Вспомогательный метод, чтобы не дублировать код создания пикселя.
     */
    private static void spawnRandomPixel(int width, int height, ModClothConfig config) {
        int x = random.nextInt(width);
        int y = random.nextInt(height);

        int color;
        if (random.nextFloat() < config.radiationPixelEffectGreenChance) {
            color = new Color(100, 255, 100, 180).getRGB();
        } else {
            color = new Color(255, 255, 255, 180).getRGB();
        }
        
        int lifetime = Mth.nextInt(random, config.radiationPixelMinLifetime, config.radiationPixelMaxLifetime);

        activePixels.add(new RadiationPixel(x, y, color, lifetime));
    }
}