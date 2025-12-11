package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 💥 СИСТЕМА ЗАЩИТЫ БЛОКОВ С КОЭФФИЦИЕНТОМ ПРОБИТИЯ
 *
 * ✅ Конвертирует взрывоустойчивость ванильных блоков в коэффициент защиты
 * ✅ Слабые блоки (камень, земля): 5-10
 * ✅ Средние блоки (обсидиан): 25-50
 * ✅ Супер-прочные (древние обломки): 100-400
 * ✅ Интегрируется с лучевой системой пробития
 */
public class BlockExplosionDefense {

    /**
     * ✅ ГЛАВНЫЙ МЕТОД: Получить коэффициент защиты блока
     *
     * Основано на взрывоустойчивости (BLAST_RESISTANCE)
     * Большинство ванильных блоков: 5-10
     */
    public static float getBlockDefenseValue(ServerLevel level, BlockPos pos, BlockState state) {

        // Бедрок - абсолютная защита
        if (state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) < 0) {
            return 10_000.0F;
        }

        Block block = state.getBlock();

        // === ЯВНО СУПЕР-ПРОЧНЫЕ БЛОКИ ===
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return 250.0F;
        }
        if (block == Blocks.ANCIENT_DEBRIS) {
            return 400.0F;
        }
        if (block == Blocks.NETHERITE_BLOCK) {
            return 300.0F;
        }

        // Если блок невозможно сломать
        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed < 0) {
            return 10_000.0F;
        }

        // === ВЗРЫВОУСТОЙЧИВОСТЬ → КОЭФФИЦИЕНТ ЗАЩИТЫ ===
        float blastRes = getBlastResistance(state);

        // Диапазон 0-50: защита 5-10 (линейно)
        if (blastRes <= 50.0F) {
            // 0 → 5, 50 → 10
            float t = blastRes / 50.0F;
            return 5.0F + t * 5.0F;
        }

        // Диапазон 50-250: защита 25
        if (blastRes <= 250.0F) {
            return 25.0F;
        }

        // Диапазон 250-1000: защита 50
        if (blastRes <= 1000.0F) {
            return 50.0F;
        }

        // 1000+: защита 100 (очень тяжело пробить)
        return 100.0F;
    }

    /**
     * ✅ Получить взрывоустойчивость блока
     */
    public static float getBlastResistance(BlockState state) {
        if (state == null) return 0.0F;
        return state.getBlock().getExplosionResistance();
    }

    /**
     * ✅ Получить уровень защиты по диапазонам (для обратной совместимости, если нужно)
     */
    public static int getDefenseLevelFromResistance(float blastRes) {
        if (blastRes < 0) return 15;
        if (blastRes < 1.0F) return 0;
        if (blastRes < 2.0F) return 1;
        if (blastRes < 5.0F) return 2;
        if (blastRes < 10.0F) return 3;
        if (blastRes < 20.0F) return 4;
        if (blastRes < 30.0F) return 5;
        if (blastRes < 50.0F) return 6;
        if (blastRes < 75.0F) return 7;
        if (blastRes < 100.0F) return 8;
        if (blastRes < 150.0F) return 9;
        if (blastRes < 250.0F) return 10;
        if (blastRes < 500.0F) return 11;
        if (blastRes < 1000.0F) return 12;
        if (blastRes < 5000.0F) return 13;
        return 14;
    }
}