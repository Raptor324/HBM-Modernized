package com.hbm_m.compat.flywheel;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

/**
 * Точка входа без прямых ссылок на Flywheel: при отсутствии мода {@code flywheel}
 * классы {@code dev.engine_room.*} не загружаются.
 */
@OnlyIn(Dist.CLIENT)
public final class FlywheelClientHooks {

    private static final String SETUP_CLASS = "com.hbm_m.client.render.flywheel.AdvancedAssemblerFlywheelSetup";

    private FlywheelClientHooks() {
    }

    public static boolean isFlywheelModPresent() {
        return ModList.get().isLoaded("flywheel");
    }

    /**
     * Полный путь Flywheel для Advanced Assembler: флаг конфига и установленный мод Flywheel.
     */
    public static boolean useFlywheelPathForAdvancedAssembler() {
        return ModClothConfig.useFlywheelAdvancedAssemblerPath() && isFlywheelModPresent();
    }

    /** После сброса визуализатора — повторная регистрация (например после перезагрузки ресурсов). */
    public static void refreshAdvancedAssemblerVisualizer() {
        clearAdvancedAssemblerVisualizerCaches();
        registerAdvancedAssemblerVisualizer();
    }

    public static void registerAdvancedAssemblerVisualizer() {
        if (!isFlywheelModPresent()) {
            return;
        }
        try {
            Class.forName(SETUP_CLASS).getMethod("register").invoke(null);
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("[HBM-M] Failed to register Flywheel visualizer for Advanced Assembler", t);
        }
    }

    public static void clearAdvancedAssemblerVisualizerCaches() {
        if (!isFlywheelModPresent()) {
            return;
        }
        try {
            Class.forName(SETUP_CLASS).getMethod("clear").invoke(null);
        } catch (Throwable t) {
            MainRegistry.LOGGER.debug("[HBM-M] Flywheel Advanced Assembler clear: {}", t.getMessage());
        }
    }
}
