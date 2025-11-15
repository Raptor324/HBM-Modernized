package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Система защиты от взрывов для блоков
 *
 * Шкала прочности: 0-15
 * - 0: Выпадают 100%, независимо от расстояния
 * - 1-5: Уничтожаются или становятся селлафитом (90%, 85%, 80%, 75%, 70%)
 * - 6-10: Становятся селлафитом или ничего (70%, 65%, 60%, 55%, 50%)
 * - 11-14: Становятся селлафитом или ничего (30%, 15%, 5%, 1%)
 * - 15: Невозможно уничтожить
 */
public class BlockExplosionDefense {

    // Система защиты от взрывов по кольцам
    public static class ExplosionDefenseResult {
        public boolean shouldBreak;        // Разрушить ли блок?
        public boolean replaceWithSellafit; // Заменить ли на селлафит?

        public ExplosionDefenseResult(boolean shouldBreak, boolean replaceWithSellafit) {
            this.shouldBreak = shouldBreak;
            this.replaceWithSellafit = replaceWithSellafit;
        }
    }

    /**
     * Возвращает уровень защиты блока (0-15)
     * Если уровень не задан, использует прочность блока для расчета
     */
    public static int getExplosionDefenseLevel(BlockState state, ServerLevel level, BlockPos pos) {
        // Здесь можно добавить кастомную систему через NBT данные блока
        // Пока используем прочность как основу
        float hardness = state.getDestroySpeed(level, pos);

        // Преобразуем прочность в уровень защиты (0-15)
        if (hardness < 0) return 15; // Bedrock-подобные блоки
        if (hardness < 1.0F) return 0;  // Мягкие блоки (доски, листва и тп)
        if (hardness < 2.0F) return 1;  // Булыжник, земля
        if (hardness < 3.0F) return 2;  // Камень
        if (hardness < 5.0F) return 3;  // Железная руда
        if (hardness < 10.0F) return 5; // Ок, алмазная руда
        if (hardness < 15.0F) return 8; // Обсидиан
        if (hardness < 30.0F) return 10; // Древний обсидиан
        if (hardness < 50.0F) return 14; // Очень прочные материалы
        return 15; // Bedrock
    }

    /**
     * Вычисляет, что случится с блоком при взрыве
     *
     * @param level Уровень
     * @param pos Позиция блока
     * @param centerPos Центр взрыва
     * @param maxRadius Максимальный радиус взрыва
     * @param random Random
     * @return ExplosionDefenseResult с информацией о судьбе блока
     */
    public static ExplosionDefenseResult calculateExplosionDamage(
            ServerLevel level,
            BlockPos pos,
            BlockPos centerPos,
            float maxRadius,
            RandomSource random) {

        BlockState state = level.getBlockState(pos);
        int defenseLevel = getExplosionDefenseLevel(state, level, pos);

        // Вычисляем расстояние до центра в блоках
        double dx = pos.getX() - centerPos.getX();
        double dz = pos.getZ() - centerPos.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Определяем номер кольца (0-5)
        int ring = calculateRingIndex(horizontalDistance, maxRadius, 6);

        // Применяем систему защиты в зависимости от кольца и уровня защиты
        return applyExplosionDefense(defenseLevel, ring, random);
    }

    /**
     * Определяет номер кольца на основе расстояния
     */
    private static int calculateRingIndex(double distance, float maxRadius, int totalRings) {
        if (distance >= maxRadius) return totalRings - 1;
        int ring = (int) (distance / maxRadius * totalRings);
        return Math.min(ring, totalRings - 1);
    }

    /**
     * Применяет систему защиты от взрывов в зависимости от кольца и уровня защиты
     *
     * Логика:
     * КОЛЬЦА 0-3 (БЛИЗКО К ЦЕНТРУ, 0-66% расстояния):
     *   - Уровень 0: Разрушить (100%)
     *   - Уровень 1-10: Разрушить (100%)
     *   - Уровень 11-14: Селлафит (100%)
     *   - Уровень 15: Ничего (0%)
     *
     * КОЛЬЦА 4-5 (ДАЛЕКО ОТ ЦЕНТРА, 66-100% расстояния):
     *   - Уровень 0: Разрушить (100%)
     *   - Уровень 1-5: Разрушить (100%)
     *   - Уровень 6-10: Селлафит (100%)
     *   - Уровень 11-14: Применить вероятность
     *   - Уровень 15: Ничего (0%)
     */
    private static ExplosionDefenseResult applyExplosionDefense(int defenseLevel, int ring, RandomSource random) {
        // Уровень 15 - невозможно уничтожить
        if (defenseLevel == 15) {
            return new ExplosionDefenseResult(false, false);
        }

        // Уровень 0 - выпадает всегда, независимо от кольца
        if (defenseLevel == 0) {
            return new ExplosionDefenseResult(true, false);
        }

        // КОЛЬЦА 0-3 (близко к центру)
        if (ring <= 1) {
            // Уровень 1-10: Разрушить (100%)
            if (defenseLevel >= 1 && defenseLevel <= 10) {
                return new ExplosionDefenseResult(true, false);
            }
            // Уровень 11-14: Селлафит (100%)
            if (defenseLevel >= 11 && defenseLevel <= 14) {
                return new ExplosionDefenseResult(true, true);
            }
        }

        // КОЛЬЦА 2-5 (далеко от центра)
        if (ring >= 2) {
            // Уровень 1-5: Разрушить (100%)
            if (defenseLevel >= 1 && defenseLevel <= 5) {
                return new ExplosionDefenseResult(true, false);
            }
            // Уровень 6-10: Применить вероятность
            if (defenseLevel >= 6 && defenseLevel <= 10) {
                float sellafitChance = getSelafitChanceForDefenseLevel(defenseLevel);
                boolean becomesSellafit = random.nextFloat() < sellafitChance;
                return new ExplosionDefenseResult(becomesSellafit, becomesSellafit);
            }
            // Уровень 11-14: Применить вероятность
            if (defenseLevel >= 11 && defenseLevel <= 14) {
                float sellafitChance = getSelafitChanceForDefenseLevel(defenseLevel);
                boolean becomesSellafit = random.nextFloat() < sellafitChance;
                return new ExplosionDefenseResult(becomesSellafit, becomesSellafit);
            }
        }

        // Не произошло ничего
        return new ExplosionDefenseResult(false, false);
    }

    /**
     * Возвращает шанс того, что блок станет селлафитом для уровней 11-14
     * 11 - 30%, 12 - 15%, 13 - 5%, 14 - 1%
     */
    private static float getSelafitChanceForDefenseLevel(int level) {
        return switch (level) {
            case 11 -> 0.30F;
            case 12 -> 0.15F;
            case 13 -> 0.05F;
            case 14 -> 0.01F;
            default -> 0.0F;
        };
    }

    /**
     * Альтернативная система с вероятностью разрушения для блоков 1-10
     * (Если вы хотите использовать старую систему с шансами для всех блоков)
     */
    public static ExplosionDefenseResult calculateExplosionDamageWithBaseProbability(
            ServerLevel level,
            BlockPos pos,
            BlockPos centerPos,
            float maxRadius,
            RandomSource random) {

        BlockState state = level.getBlockState(pos);
        int defenseLevel = getExplosionDefenseLevel(state, level, pos);

        double dx = pos.getX() - centerPos.getX();
        double dz = pos.getZ() - centerPos.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        int ring = calculateRingIndex(horizontalDistance, maxRadius, 6);

        // Уровень 0 - выпадает всегда
        if (defenseLevel == 0) {
            return new ExplosionDefenseResult(true, false);
        }

        // Уровень 15 - не трогать
        if (defenseLevel == 15) {
            return new ExplosionDefenseResult(false, false);
        }

        // Уровень 1-5: Шанс уничтожиться или стать селлафитом
        if (defenseLevel >= 1 && defenseLevel <= 5) {
            float destroyChance = getDestroyChanceForLevel1to5(defenseLevel);
            if (random.nextFloat() < destroyChance) {
                return new ExplosionDefenseResult(true, false);
            } else {
                return new ExplosionDefenseResult(true, true);
            }
        }

        // Уровень 6-10: Шанс стать селлафитом или ничего
        if (defenseLevel >= 6 && defenseLevel <= 10) {
            float selafitChance = getSelafitChanceForLevel6to10(defenseLevel);
            if (random.nextFloat() < selafitChance) {
                return new ExplosionDefenseResult(true, true);
            } else {
                return new ExplosionDefenseResult(false, false);
            }
        }

        // Уровень 11-14
        if (defenseLevel >= 11 && defenseLevel <= 14) {
            float selafitChance = getSelafitChanceForDefenseLevel(defenseLevel);
            if (random.nextFloat() < selafitChance) {
                return new ExplosionDefenseResult(true, true);
            } else {
                return new ExplosionDefenseResult(false, false);
            }
        }

        return new ExplosionDefenseResult(false, false);
    }

    /**
     * Шанс разрушения для уровней 1-5
     * 1 - 90%, 2 - 85%, 3 - 80%, 4 - 75%, 5 - 70%
     */
    private static float getDestroyChanceForLevel1to5(int level) {
        return switch (level) {
            case 1 -> 0.90F;
            case 2 -> 0.85F;
            case 3 -> 0.80F;
            case 4 -> 0.75F;
            case 5 -> 0.70F;
            default -> 0.0F;
        };
    }

    /**
     * Шанс превращения в селлафит для уровней 6-10
     * 6 - 70%, 7 - 65%, 8 - 60%, 9 - 55%, 10 - 50%
     */
    private static float getSelafitChanceForLevel6to10(int level) {
        return switch (level) {
            case 6 -> 0.70F;
            case 7 -> 0.65F;
            case 8 -> 0.60F;
            case 9 -> 0.55F;
            case 10 -> 0.50F;
            default -> 0.0F;
        };
    }
}