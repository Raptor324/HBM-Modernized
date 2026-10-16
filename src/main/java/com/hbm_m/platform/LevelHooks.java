package com.hbm_m.platform;

import net.minecraft.world.level.Level;

/**
 * Высотные аксессоры уровня. Имена getMinBuildHeight/getMinSection одинаковы на
 * 1.20.1 и 1.21.1 (переименование в getMinY/getMinSectionY случилось только в
 * 1.21.2+, куда мы не таргетируемся), но хук оставлен как единая точка на случай
 * будущих таргетов.
 */
public final class LevelHooks {

    private LevelHooks() {}

    public static int minY(Level level) {
        return level.getMinBuildHeight();
    }

    public static int maxY(Level level) {
        return level.getMaxBuildHeight();
    }

    public static int minSectionY(Level level) {
        return level.getMinSection();
    }

    /** Округлённая вверх до секций высота мира. */
    public static int worldHeight(Level level) {
        int raw = maxY(level) - minY(level) + 1;
        return (raw + 15) & ~15;
    }
}
