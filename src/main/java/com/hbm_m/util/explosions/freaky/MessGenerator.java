package com.hbm_m.util.explosions.freaky;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessGenerator {
    /**
     * Создаёт круглую воронку от взрыва
     * @param level Серверный мир
     * @param centerPos Центр взрыва
     * @param radius Радиус воронки
     * @param depth Глубина воронки
     * @param surfaceBlock1 Первый блок для поверхности
     * @param surfaceBlock2 Второй блок для поверхности
     * @param surfaceBlock3 Третий блок для поверхности
     */
    public static void generateCrater(ServerLevel level, BlockPos centerPos,
                                      int radius, int depth,
                                      Block surfaceBlock1,
                                      Block surfaceBlock2,
                                      Block surfaceBlock3) {
        // ИСПРАВЛЕНИЕ №1: Используем RandomSource вместо Random
        RandomSource random = level.random;
        List<BlockPos> craterBlocks = new ArrayList<>();

        // Фаза 1: Расчёт блоков для удаления (сферическая воронка)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -depth; y <= radius; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);

                    // Вычисляем расстояние от центра
                    double distance = Math.sqrt(x * x + z * z + y * y);

                    // Создаём полусферу (только нижняя часть)
                    if (y <= 0) {
                        // Нижняя часть - полная полусфера
                        if (distance <= radius) {
                            craterBlocks.add(checkPos);
                        }
                    } else {
                        // Верхняя часть - более пологая (эффект приподнятого края)
                        double edgeRadius = radius * (1.0 - (double) y / radius * 0.3);
                        double horizontalDistance = Math.sqrt(x * x + z * z);
                        if (horizontalDistance <= edgeRadius && y < radius * 0.2) {
                            craterBlocks.add(checkPos);
                        }
                    }
                }
            }
        }

        // Фаза 2: Удаление блоков (от края к центру, как в HBM)
        // Сортируем блоки по расстоянию от центра (дальние первые)
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
            // Проверяем, что блок не неразрушимый
            BlockState state = level.getBlockState(pos);
            if (state.getDestroySpeed(level, pos) >= 0) {
                level.removeBlock(pos, false);
            }
        }

        // Планируем следующую итерацию через 1 тик
        int nextIndex = endIndex;

        // ИСПРАВЛЕНИЕ №2: Проверка на null
        if (level.getServer() != null) {
            level.getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                removeCraterBlocks(level, blocks, blocksPerTick, nextIndex,
                        surfaceBlock1, surfaceBlock2, surfaceBlock3, centerPos);
            }));
        }
    }

    /**
     * Генерирует поверхностный слой воронки из случайных блоков
     */
    private static void generateCraterSurface(ServerLevel level,
                                              BlockPos centerPos,
                                              List<BlockPos> craterBlocks,
                                              Block surfaceBlock1,
                                              Block surfaceBlock2,
                                              Block surfaceBlock3) {
        // ИСПРАВЛЕНИЕ №1: Используем RandomSource вместо Random
        RandomSource random = level.random;
        Block[] surfaceBlocks = {surfaceBlock1, surfaceBlock2, surfaceBlock3};

        // ИСПРАВЛЕНИЕ №3: Используем HashSet для быстрой проверки
        Set<BlockPos> craterBlocksSet = new HashSet<>(craterBlocks);

        // Проходим по всем удалённым блокам и находим верхний слой
        for (BlockPos pos : craterBlocks) {
            // Проверяем, является ли этот блок частью поверхности
            // (есть ли над ним воздух или блок, который не был удалён)
            BlockPos above = pos.above();
            boolean isAirAbove = level.getBlockState(above).isAir();

            // ИСПРАВЛЕНИЕ №3: Используем HashSet вместо contains() на ArrayList
            boolean wasNotRemoved = !craterBlocksSet.contains(above);

            if (isAirAbove || wasNotRemoved) {
                // Это поверхностный блок - заменяем на случайный из трёх
                Block randomBlock = surfaceBlocks[random.nextInt(surfaceBlocks.length)];
                level.setBlock(pos, randomBlock.defaultBlockState(), 3);
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
        // ИСПРАВЛЕНИЕ №1: Используем RandomSource вместо Random
        RandomSource random = level.random;
        Block[] surfaceBlocks = {surfaceBlock1, surfaceBlock2, surfaceBlock3};

        // Удаляем блоки и сразу создаём поверхность
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -depth; y <= radius; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);
                    double distance = Math.sqrt(x * x + z * z + y * y);
                    boolean shouldRemove = false;

                    if (y <= 0) {
                        // Нижняя полусфера
                        if (distance <= radius) {
                            shouldRemove = true;
                        }
                    } else {
                        // Верхний край
                        double edgeRadius = radius * (1.0 - (double) y / radius * 0.3);
                        double horizontalDistance = Math.sqrt(x * x + z * z);
                        if (horizontalDistance <= edgeRadius && y < radius * 0.2) {
                            shouldRemove = true;
                        }
                    }

                    if (shouldRemove) {
                        BlockState state = level.getBlockState(checkPos);
                        if (state.getDestroySpeed(level, checkPos) >= 0) {
                            // Проверяем, это поверхностный блок?
                            BlockPos above = checkPos.above();
                            boolean isAirAbove = level.getBlockState(above).isAir();

                            if (isAirAbove) {
                                // Поверхностный блок - заменяем на случайный
                                Block randomBlock = surfaceBlocks[random.nextInt(surfaceBlocks.length)];
                                level.setBlock(checkPos, randomBlock.defaultBlockState(), 3);
                            } else {
                                // Обычное удаление
                                level.removeBlock(checkPos, false);
                            }
                        }
                    }
                }
            }
        }
    }
}