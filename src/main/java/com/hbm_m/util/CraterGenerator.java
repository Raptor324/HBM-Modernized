package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CraterGenerator {

    // Параметры системы разрушения
    private static final float BASE_HARDNESS_THRESHOLD = 3.0F; // До этого значения - 100% шанс
    private static final float MAX_HARDNESS_THRESHOLD = 50.0F; // После этого значения - 0% шанс
    private static final int SURFACE_LAYER_DEPTH = 3; // Количество слоёв от дна воронки (2-3 блока)

    /**
     * Создаёт круглую воронку от взрыва с системой прочности блоков
     * @param level Серверный мир
     * @param centerPos Центр взрыва
     * @param radius Радиус воронки
     * @param depth Глубина воронки
     * @param surfaceBlock1 Первый блок для дна воронки
     * @param surfaceBlock2 Второй блок для дна воронки
     * @param surfaceBlock3 Третий блок для дна воронки
     */
    public static void generateCrater(ServerLevel level, BlockPos centerPos,
                                      int radius, int depth,
                                      Block surfaceBlock1,
                                      Block surfaceBlock2,
                                      Block surfaceBlock3) {
        RandomSource random = level.random;
        List<BlockPos> craterBlocks = new ArrayList<>();

        // Фаза 1: Расчёт блоков для удаления (сферическая воронка)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -depth; y <= radius; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);

                    // Вычисляем расстояние от центра
                    double distance = Math.sqrt(x * x + z * z + y * y);

                    boolean shouldCheck = false;

                    // Создаём полусферу (только нижняя часть)
                    if (y <= 0) {
                        // Нижняя часть - полная полусфера
                        if (distance <= radius) {
                            shouldCheck = true;
                        }
                    } else {
                        // Верхняя часть - более пологая (эффект приподнятого края)
                        double edgeRadius = radius * (1.0 - (double) y / radius * 0.3);
                        double horizontalDistance = Math.sqrt(x * x + z * z);
                        if (horizontalDistance <= edgeRadius && y < radius * 0.2) {
                            shouldCheck = true;
                        }
                    }

                    // Проверяем, можно ли разрушить блок на основе его прочности
                    if (shouldCheck) {
                        BlockState state = level.getBlockState(checkPos);
                        float hardness = state.getDestroySpeed(level, checkPos);

                        // Рассчитываем шанс разрушения
                        float breakChance = calculateBreakChance(hardness);

                        // Проверяем, разрушается ли блок по шансу
                        if (random.nextFloat() < breakChance) {
                            craterBlocks.add(checkPos);
                        }
                    }
                }
            }
        }

        // Фаза 2: Удаление блоков (от края к центру)
        craterBlocks.sort((pos1, pos2) -> {
            double dist1 = centerPos.distSqr(pos1);
            double dist2 = centerPos.distSqr(pos2);
            return Double.compare(dist2, dist1);
        });

        // Удаляем блоки с небольшой задержкой для эффекта
        int blocksPerTick = Math.max(1, craterBlocks.size() / 40); // ~2 секунды
        removeCraterBlocks(level, craterBlocks, blocksPerTick, 0,
                surfaceBlock1, surfaceBlock2, surfaceBlock3, centerPos);
    }

    /**
     * Рассчитывает шанс разрушения блока на основе его прочности
     * @param hardness Прочность блока
     * @return Шанс разрушения от 0.0 до 1.0
     */
    private static float calculateBreakChance(float hardness) {
        // Неразрушимые блоки (бедрок, барьер и т.д.)
        if (hardness < 0) {
            return 0.0F;
        }

        // Лёгкие блоки (камень, земля, дерево и т.д.) - 100% шанс
        if (hardness <= BASE_HARDNESS_THRESHOLD) {
            return 1.0F;
        }

        // Очень прочные блоки (обсидиан и прочнее) - 0% шанс
        if (hardness >= MAX_HARDNESS_THRESHOLD) {
            return 0.0F;
        }

        // Градиентный переход от 100% до 0%
        float range = MAX_HARDNESS_THRESHOLD - BASE_HARDNESS_THRESHOLD;
        return 1.0F - ((hardness - BASE_HARDNESS_THRESHOLD) / range);
    }

    /**
     * Рекурсивное удаление блоков воронки по частям
     */
    private static void removeCraterBlocks(ServerLevel level,
                                           List<BlockPos> blocks,
                                           int blocksPerTick,
                                           int currentIndex,
                                           Block surfaceBlock1,
                                           Block surfaceBlock2,
                                           Block surfaceBlock3,
                                           BlockPos centerPos) {
        if (currentIndex >= blocks.size()) {
            // Завершили удаление - создаём поверхностный слой
            generateCraterSurface(level, centerPos, blocks,
                    surfaceBlock1, surfaceBlock2, surfaceBlock3);
            return;
        }

        // Удаляем следующую партию блоков
        int endIndex = Math.min(currentIndex + blocksPerTick, blocks.size());
        for (int i = currentIndex; i < endIndex; i++) {
            BlockPos pos = blocks.get(i);
            level.removeBlock(pos, false);
        }

        // Планируем следующую итерацию через 1 тик
        int nextIndex = endIndex;

        if (level.getServer() != null) {
            level.getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                removeCraterBlocks(level, blocks, blocksPerTick, nextIndex,
                        surfaceBlock1, surfaceBlock2, surfaceBlock3, centerPos);
            }));
        }
    }

    /**
     * Генерирует слой на ДНЕ воронки (2-3 блока от самого низа вверх)
     */
    private static void generateCraterSurface(ServerLevel level,
                                              BlockPos centerPos,
                                              List<BlockPos> craterBlocks,
                                              Block surfaceBlock1,
                                              Block surfaceBlock2,
                                              Block surfaceBlock3) {
        RandomSource random = level.random;
        Block[] surfaceBlocks = {surfaceBlock1, surfaceBlock2, surfaceBlock3};

        // Используем HashSet для быстрой проверки
        Set<BlockPos> craterBlocksSet = new HashSet<>(craterBlocks);

        // Сначала находим все блоки дна (под которыми нет удалённых блоков)
        Set<BlockPos> bottomBlocks = new HashSet<>();

        for (BlockPos pos : craterBlocks) {
            BlockPos below = pos.below();

            // Если под блоком НЕТ удалённого блока - это дно воронки
            if (!craterBlocksSet.contains(below)) {
                bottomBlocks.add(pos);
            }
        }

        // Теперь заменяем блоки дна и 2-3 слоя выше
        for (BlockPos bottomPos : bottomBlocks) {
            // Заменяем сам блок дна
            Block randomBlock = surfaceBlocks[random.nextInt(surfaceBlocks.length)];
            level.setBlock(bottomPos, randomBlock.defaultBlockState(), 3);

            // Заменяем 2-3 слоя выше дна
            BlockPos currentPos = bottomPos.above();
            for (int layer = 1; layer < SURFACE_LAYER_DEPTH; layer++) {
                // Проверяем, что этот блок был удалён (часть воронки)
                if (craterBlocksSet.contains(currentPos)) {
                    randomBlock = surfaceBlocks[random.nextInt(surfaceBlocks.length)];
                    level.setBlock(currentPos, randomBlock.defaultBlockState(), 3);
                    currentPos = currentPos.above();
                } else {
                    // Если блок не был удалён - прекращаем
                    break;
                }
            }
        }
    }

    /**
     * Упрощённая версия - мгновенное создание воронки без анимации
     */
    public static void generateCraterInstant(ServerLevel level, BlockPos centerPos,
                                             int radius, int depth,
                                             Block surfaceBlock1,
                                             Block surfaceBlock2,
                                             Block surfaceBlock3) {
        RandomSource random = level.random;
        Block[] surfaceBlocks = {surfaceBlock1, surfaceBlock2, surfaceBlock3};
        Set<BlockPos> removedBlocks = new HashSet<>();

        // Фаза 1: Удаление блоков с учётом прочности
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -depth; y <= radius; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);
                    double distance = Math.sqrt(x * x + z * z + y * y);
                    boolean shouldCheck = false;

                    if (y <= 0) {
                        if (distance <= radius) {
                            shouldCheck = true;
                        }
                    } else {
                        double edgeRadius = radius * (1.0 - (double) y / radius * 0.3);
                        double horizontalDistance = Math.sqrt(x * x + z * z);
                        if (horizontalDistance <= edgeRadius && y < radius * 0.2) {
                            shouldCheck = true;
                        }
                    }

                    if (shouldCheck) {
                        BlockState state = level.getBlockState(checkPos);
                        float hardness = state.getDestroySpeed(level, checkPos);
                        float breakChance = calculateBreakChance(hardness);

                        if (random.nextFloat() < breakChance) {
                            level.removeBlock(checkPos, false);
                            removedBlocks.add(checkPos);
                        }
                    }
                }
            }
        }

        // Фаза 2: Находим дно воронки и заменяем 2-3 слоя
        Set<BlockPos> bottomBlocks = new HashSet<>();

        for (BlockPos pos : removedBlocks) {
            BlockPos below = pos.below();

            // Если под блоком НЕТ удалённого блока - это дно
            if (!removedBlocks.contains(below)) {
                bottomBlocks.add(pos);
            }
        }

        // Заменяем дно и слои выше
        for (BlockPos bottomPos : bottomBlocks) {
            // Дно
            Block randomBlock = surfaceBlocks[random.nextInt(surfaceBlocks.length)];
            level.setBlock(bottomPos, randomBlock.defaultBlockState(), 3);

            // Слои выше (2-3 блока)
            BlockPos currentPos = bottomPos.above();
            for (int layer = 1; layer < SURFACE_LAYER_DEPTH; layer++) {
                if (removedBlocks.contains(currentPos)) {
                    randomBlock = surfaceBlocks[random.nextInt(surfaceBlocks.length)];
                    level.setBlock(currentPos, randomBlock.defaultBlockState(), 3);
                    currentPos = currentPos.above();
                } else {
                    break;
                }
            }
        }
    }
}