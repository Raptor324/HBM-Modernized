package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CraterGenerator {

    // Параметры системы разрушения
    private static final float BASE_HARDNESS_THRESHOLD = 3.0F;
    private static final float MAX_HARDNESS_THRESHOLD = 50.0F;
    private static final int SURFACE_LAYER_DEPTH = 3;

    // Множитель вытянутости для горизонтальной оси (больше = более вытянутая)
    private static final float STRETCH_FACTOR = 1.5F;

    // УВЕЛИЧЕННАЯ высота удаления блоков над воронкой (в блоках)
    private static final int REMOVAL_HEIGHT_ABOVE = 60;

    // Увеличенный горизонтальный радиус удаления блоков над воронкой
    private static final float TOP_REMOVAL_RADIUS_MULTIPLIER = 1.3F;

    /**
     * Создаёт вытянутую воронку от взрыва (эллиптоидная форма)
     * ПОЛАЯ воронка с селлафитом только на дне
     * @param level Серверный мир
     * @param centerPos Центр взрыва
     * @param radius Радиус воронки
     * @param depth Глубина воронки
     * @param surfaceBlock1 Первый блок селлафита для дна воронки
     * @param surfaceBlock2 Второй блок селлафита для дна воронки
     * @param surfaceBlock3 Третий блок селлафита для дна воронки
     * @param surfaceBlock4 Четвёртый блок селлафита для дна воронки (НОВЫЙ)
     */
    public static void generateCrater(ServerLevel level, BlockPos centerPos,
                                      int radius, int depth,
                                      Block surfaceBlock1,
                                      Block surfaceBlock2,
                                      Block surfaceBlock3,
                                      Block surfaceBlock4) {
        RandomSource random = level.random;
        List<BlockPos> craterBlocks = new ArrayList<>();
        Set<BlockPos> craterBlocksSet = new HashSet<>();

        // Применяем вытянутость к горизонтальному радиусу
        float horizontalRadius = radius * STRETCH_FACTOR;

        // Увеличенный радиус для удаления блоков над воронкой
        float topRemovalRadius = horizontalRadius * TOP_REMOVAL_RADIUS_MULTIPLIER;

        // Фаза 1: Расчёт блоков для удаления (вытянутая полусфера)
        for (int x = -(int)topRemovalRadius; x <= topRemovalRadius; x++) {
            for (int z = -(int)topRemovalRadius; z <= topRemovalRadius; z++) {
                for (int y = -depth; y <= REMOVAL_HEIGHT_ABOVE; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);

                    // Вычисляем эллиптическое расстояние (вытянутое по оси X/Z)
                    double normalizedX = (double) x / horizontalRadius;
                    double normalizedZ = (double) z / horizontalRadius;
                    double horizontalDistance = Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ);

                    boolean shouldCheck = false;

                    // Нижняя часть - вытянутая полусфера
                    if (y <= 0) {
                        // Проверяем, входит ли точка в эллипсоид
                        double normalizedY = Math.abs((double) y) / depth;
                        double ellipsoidDistance = Math.sqrt(horizontalDistance * horizontalDistance + normalizedY * normalizedY);
                        if (ellipsoidDistance <= 1.0) {
                            shouldCheck = true;
                        }
                    } else {
                        // Верхняя часть - приподнятые края и зона удаления
                        double topRemovalRadiusNorm = topRemovalRadius / horizontalRadius;
                        double edgeRadius = topRemovalRadiusNorm - (double) y / (radius * 2) * 0.4;
                        if (horizontalDistance <= edgeRadius && y < REMOVAL_HEIGHT_ABOVE) {
                            shouldCheck = true;
                        }
                    }

                    if (shouldCheck) {
                        BlockState state = level.getBlockState(checkPos);
                        float hardness = state.getDestroySpeed(level, checkPos);
                        float breakChance = calculateBreakChance(hardness);
                        if (random.nextFloat() < breakChance) {
                            craterBlocks.add(checkPos);
                            craterBlocksSet.add(checkPos);
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
        int blocksPerTick = Math.max(1, craterBlocks.size() / 40);
        removeCraterBlocks(level, craterBlocks, blocksPerTick, 0,
                surfaceBlock1, surfaceBlock2, surfaceBlock3, surfaceBlock4,
                centerPos, craterBlocksSet, radius * STRETCH_FACTOR);
    }

    /**
     * Рассчитывает шанс разрушения блока на основе его прочности
     */
    private static float calculateBreakChance(float hardness) {
        if (hardness < 0) {
            return 0.0F;
        }
        if (hardness <= BASE_HARDNESS_THRESHOLD) {
            return 1.0F;
        }
        if (hardness >= MAX_HARDNESS_THRESHOLD) {
            return 0.0F;
        }
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
                                           Block surfaceBlock4,
                                           BlockPos centerPos,
                                           Set<BlockPos> craterBlocksSet,
                                           double removalRadius) {
        if (currentIndex >= blocks.size()) {
            generateCraterSurface(level, centerPos, craterBlocksSet,
                    surfaceBlock1, surfaceBlock2, surfaceBlock3, surfaceBlock4);
            // Удаляем предметы в радиусе генерации
            removeItemsInRadius(level, centerPos, (int)removalRadius + 10);
            return;
        }

        int endIndex = Math.min(currentIndex + blocksPerTick, blocks.size());
        for (int i = currentIndex; i < endIndex; i++) {
            BlockPos pos = blocks.get(i);
            level.removeBlock(pos, false);
        }

        int nextIndex = endIndex;
        if (level.getServer() != null) {
            level.getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                removeCraterBlocks(level, blocks, blocksPerTick, nextIndex,
                        surfaceBlock1, surfaceBlock2, surfaceBlock3, surfaceBlock4,
                        centerPos, craterBlocksSet, removalRadius);
            }));
        }
    }

    /**
     * Генерирует слой селлафита ТОЛЬКО на ДНЕ воронки (2-3 блока от самого низа)
     * Остальная воронка остается ПОЛОЙ (заполнена воздухом)
     * УЛУЧШЕНИЕ: Теперь использует 4 типа селлафита (включая СЕЛЛАФИТ3)
     */
    private static void generateCraterSurface(ServerLevel level,
                                              BlockPos centerPos,
                                              Set<BlockPos> craterBlocksSet,
                                              Block surfaceBlock1,
                                              Block surfaceBlock2,
                                              Block surfaceBlock3,
                                              Block surfaceBlock4) {
        RandomSource random = level.random;
        Block[] surfaceBlocks = {surfaceBlock1, surfaceBlock2, surfaceBlock3, surfaceBlock4};

        // Находим только блоки ДНА воронки (где нет воронки ниже)
        Set<BlockPos> bottomBlocks = new HashSet<>();
        for (BlockPos pos : craterBlocksSet) {
            // Проверяем, может ли это быть дном
            boolean isBottom = false;
            BlockPos below = pos.below();

            // Если блок ниже не входит в воронку - это потенциальное дно
            if (!craterBlocksSet.contains(below)) {
                BlockState stateBelow = level.getBlockState(below);
                // Проверяем, находится ли ниже твердый блок (не воздух)
                if (!stateBelow.isAir()) {
                    // Прямо под нами твердый блок - это определенно дно!
                    isBottom = true;
                } else {
                    // Это воздух - спускаемся ниже, чтобы найти твердый блок
                    // ИСПРАВЛЕНИЕ: Ищем первый твердый блок под слоем воздуха
                    BlockPos checkPos = below;
                    boolean foundSolid = false;
                    int airSkipLimit = 20; // Максимум блоков воздуха для пропуска
                    int airCount = 0;

                    while (!foundSolid && airCount < airSkipLimit) {
                        if (!craterBlocksSet.contains(checkPos)) {
                            BlockState checkState = level.getBlockState(checkPos);
                            if (!checkState.isAir()) {
                                // Нашли твердый блок - это дно!
                                isBottom = true;
                                foundSolid = true;
                            } else {
                                // Продолжаем спускаться через воздух
                                checkPos = checkPos.below();
                                airCount++;
                                if (checkPos.getY() < level.getMinBuildHeight()) {
                                    // Достигли дна мира
                                    isBottom = true;
                                    foundSolid = true;
                                }
                            }
                        } else {
                            // Еще один блок из воронки, спускаемся ниже
                            checkPos = checkPos.below();
                            airCount++;
                        }
                    }
                }
            }

            if (isBottom) {
                bottomBlocks.add(pos);
            }
        }

        // КЛЮЧЕВОЙ МОМЕНТ: Заменяем селлафитом ТОЛЬКО блоки дна и 2-3 слоя выше
        // Все остальные блоки воронки остаются пустыми (удаленными)
        for (BlockPos bottomPos : bottomBlocks) {
            Block randomBlock = surfaceBlocks[random.nextInt(surfaceBlocks.length)];
            level.setBlock(bottomPos, randomBlock.defaultBlockState(), 3);

            // Добавляем селлафит вверх от дна, но ТОЛЬКО если это тоже была воронка
            BlockPos currentPos = bottomPos.above();
            for (int layer = 1; layer < SURFACE_LAYER_DEPTH; layer++) {
                if (craterBlocksSet.contains(currentPos)) {
                    randomBlock = surfaceBlocks[random.nextInt(surfaceBlocks.length)];
                    level.setBlock(currentPos, randomBlock.defaultBlockState(), 3);
                    currentPos = currentPos.above();
                } else {
                    // НЕ часть воронки - прекращаем добавлять селлафит вверх!
                    break;
                }
            }
        }
    }

    /**
     * Удаляет все предметы (ItemEntity) в указанном радиусе
     * Вызывается при генерации кратера для имитации ядерного взрыва
     */
    private static void removeItemsInRadius(ServerLevel level, BlockPos centerPos, int radius) {
        level.getEntities(
                (Entity) null,
                new net.minecraft.world.phys.AABB(
                        centerPos.getX() - radius,
                        centerPos.getY() - radius,
                        centerPos.getZ() - radius,
                        centerPos.getX() + radius,
                        centerPos.getY() + radius,
                        centerPos.getZ() + radius
                ),
                entity -> entity instanceof ItemEntity
        ).forEach(net.minecraft.world.entity.Entity::discard);
    }

    /**
     * Упрощённая версия - мгновенное создание вытянутой воронки
     */
    public static void generateCraterInstant(ServerLevel level, BlockPos centerPos,
                                             int radius, int depth,
                                             Block surfaceBlock1,
                                             Block surfaceBlock2,
                                             Block surfaceBlock3,
                                             Block surfaceBlock4) {
        RandomSource random = level.random;
        Block[] surfaceBlocks = {surfaceBlock1, surfaceBlock2, surfaceBlock3, surfaceBlock4};
        Set<BlockPos> removedBlocks = new HashSet<>();

        // Применяем вытянутость к горизонтальному радиусу
        float horizontalRadius = radius * STRETCH_FACTOR;
        float topRemovalRadius = horizontalRadius * TOP_REMOVAL_RADIUS_MULTIPLIER;

        // Быстрое удаление блоков без задержек
        for (int x = -(int)topRemovalRadius; x <= topRemovalRadius; x++) {
            for (int z = -(int)topRemovalRadius; z <= topRemovalRadius; z++) {
                for (int y = -depth; y <= REMOVAL_HEIGHT_ABOVE; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);

                    // Вычисляем эллиптическое расстояние
                    double normalizedX = (double) x / horizontalRadius;
                    double normalizedZ = (double) z / horizontalRadius;
                    double horizontalDistance = Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ);

                    boolean shouldRemove = false;

                    // Нижняя часть - вытянутая полусфера
                    if (y <= 0) {
                        double normalizedY = Math.abs((double) y) / depth;
                        double ellipsoidDistance = Math.sqrt(horizontalDistance * horizontalDistance + normalizedY * normalizedY);
                        if (ellipsoidDistance <= 1.0) {
                            shouldRemove = true;
                        }
                    } else {
                        // Верхняя часть - увеличенная зона удаления
                        double topRemovalRadiusNorm = topRemovalRadius / horizontalRadius;
                        double edgeRadius = topRemovalRadiusNorm - (double) y / (radius * 2) * 0.4;
                        if (horizontalDistance <= edgeRadius && y < REMOVAL_HEIGHT_ABOVE) {
                            shouldRemove = true;
                        }
                    }

                    if (shouldRemove) {
                        BlockState state = level.getBlockState(checkPos);
                        float hardness = state.getDestroySpeed(level, checkPos);
                        if (random.nextFloat() < calculateBreakChance(hardness)) {
                            level.removeBlock(checkPos, false);
                            removedBlocks.add(checkPos);
                        }
                    }
                }
            }
        }

        // Генерируем селлафит на дне
        generateCraterSurface(level, centerPos, removedBlocks,
                surfaceBlock1, surfaceBlock2, surfaceBlock3, surfaceBlock4);

        // Удаляем предметы
        removeItemsInRadius(level, centerPos, (int)topRemovalRadius + 10);
    }
}