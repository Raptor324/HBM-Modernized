package com.hbm_m.util.explosions.freaky;

import com.hbm_m.util.explosions.ExplosionResistanceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Продвинутый генератор кратеров с системой прочности блоков HBM Nuclear Tech
 * Блоки могут защищать более слабые блоки позади них
 */
public class WasteBlastGenerator {

    // Параметры системы разрушения
    private static final int SURFACE_LAYER_DEPTH = 4;

    /**
     * Проверяет, является ли блок водой
     */
    private static boolean isWaterBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.WATER;
    }

    /**
     * Проверяет, является ли блок воздухом
     */
    private static boolean isAirBlock(BlockState state) {
        return state.isAir();
    }

    /**
     * Мгновенное создание воронки без анимации с продвинутой системой прочности
     */
    public static void generateCraterInstant(ServerLevel level, BlockPos centerPos,
                                             int radius, int depth,
                                             Block[] sellafieldBlocks) {
        RandomSource random = level.random;

        // Инициализируем реестр прочности
        ExplosionResistanceRegistry.init();

        if (sellafieldBlocks.length != 4) {
            throw new IllegalArgumentException("sellafieldBlocks должен содержать ровно 4 блока");
        }

        Set<BlockPos> removedBlocks = new HashSet<>();
        Set<BlockPos> upperPartBlocks = new HashSet<>();
        Map<BlockPos, Integer> blocksToProcess = new HashMap<>();

        // Фаза 1: Сбор всех блоков в радиусе взрыва с информацией о расстоянии
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -depth; y <= radius * 2; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);
                    double horizontalDistance = Math.sqrt(x * x + z * z);

                    boolean shouldCheck = false;
                    boolean isUpperPart = false;

                    if (y <= 0) {
                        // Нижняя часть - эллипсоидная форма
                        double normalizedDepth = (double) (-y) / depth;
                        double depthRadius = radius * (1.0 - normalizedDepth * normalizedDepth);

                        if (horizontalDistance <= depthRadius) {
                            shouldCheck = true;
                        }
                    } else {
                        // Верхняя часть - конусообразная форма
                        double edgeRadius = radius * Math.max(0, 1.0 - (double) y / (radius * 2));

                        if (horizontalDistance <= edgeRadius) {
                            shouldCheck = true;
                            isUpperPart = true;
                        }
                    }

                    if (shouldCheck) {
                        BlockState state = level.getBlockState(checkPos);

                        // Игнорируем воду и воздух
                        if (isWaterBlock(state) || isAirBlock(state)) {
                            continue;
                        }

                        if (isUpperPart) {
                            upperPartBlocks.add(checkPos);
                        }

                        // Вычисляем расстояние от эпицентра
                        double distanceFromCenter = Math.sqrt(x * x + y * y + z * z);
                        int distanceKey = (int) Math.round(distanceFromCenter);
                        blocksToProcess.put(checkPos, distanceKey);
                    }
                }
            }
        }

        // Фаза 2: Обработка блоков с учётом прочности и защиты
        processBlocksWithShielding(level, blocksToProcess, removedBlocks, upperPartBlocks,
                centerPos, radius);

        // Фаза 3: Создание селлафитного слоя на дне
        generateCraterSurfaceInstant(level, removedBlocks, upperPartBlocks,
                sellafieldBlocks, random);
    }

    /**
     * Обрабатывает блоки с учётом системы прочности и защиты
     */
    private static void processBlocksWithShielding(ServerLevel level,
                                                   Map<BlockPos, Integer> blocksToProcess,
                                                   Set<BlockPos> removedBlocks,
                                                   Set<BlockPos> upperPartBlocks,
                                                   BlockPos centerPos,
                                                   int radius) {
        RandomSource random = level.random;

        // Сортируем блоки по расстоянию от эпицентра (от центра к краям)
        List<Map.Entry<BlockPos, Integer>> sortedBlocks = new ArrayList<>(blocksToProcess.entrySet());
        sortedBlocks.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        Set<BlockPos> protectedBlocks = new HashSet<>();

        // Обрабатываем блоки от центра к краям
        for (Map.Entry<BlockPos, Integer> entry : sortedBlocks) {
            BlockPos pos = entry.getKey();
            Block block = level.getBlockState(pos).getBlock();
            int resistance = ExplosionResistanceRegistry.getResistance(block);

            // Неуничтожаемые блоки (прочность 15)
            if (resistance >= 15) {
                protectedBlocks.add(pos);
                continue;
            }

            // Блок защищён более прочным блоком впереди
            if (protectedBlocks.contains(pos)) {
                // Проверяем, есть ли блок с высокой прочностью между взрывом и этим блоком
                if (isBlockShielded(level, centerPos, pos, protectedBlocks)) {
                    protectedBlocks.add(pos);
                    continue;
                }
            }

            // Вычисляем шанс уничтожения на основе прочности
            float survivalChance = calculateSurvivalChance(resistance, entry.getValue(), radius);

            if (random.nextFloat() < survivalChance) {
                // Блок выживает
                protectedBlocks.add(pos);

                // Если это блок с хорошей прочностью, он может защищать соседние блоки
                if (resistance >= 6 && resistance < 15) {
                    protectNearbyBlocks(pos, protectedBlocks, level, centerPos);
                }
            } else {
                // Блок уничтожается
                level.removeBlock(pos, false);
                removedBlocks.add(pos);
            }
        }
    }

    /**
     * Проверяет, защищён ли блок более прочными блоками впереди
     */
    private static boolean isBlockShielded(ServerLevel level, BlockPos centerPos, BlockPos targetPos,
                                           Set<BlockPos> protectedBlocks) {
        // Вектор направления от центра к целевому блоку
        int dx = targetPos.getX() - centerPos.getX();
        int dy = targetPos.getY() - centerPos.getY();
        int dz = targetPos.getZ() - centerPos.getZ();

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 1.0) return false;

        // Нормализуем вектор
        double ndx = dx / distance;
        double ndy = dy / distance;
        double ndz = dz / distance;

        // Проверяем блоки между центром и целью на расстоянии 0.5 - distance
        for (double d = 0.5; d < distance; d += 1.0) {
            int checkX = centerPos.getX() + (int) Math.round(ndx * d);
            int checkY = centerPos.getY() + (int) Math.round(ndy * d);
            int checkZ = centerPos.getZ() + (int) Math.round(ndz * d);

            BlockPos checkPos = new BlockPos(checkX, checkY, checkZ);

            if (protectedBlocks.contains(checkPos)) {
                Block shieldBlock = level.getBlockState(checkPos).getBlock();
                int shieldResistance = ExplosionResistanceRegistry.getResistance(shieldBlock);

                // Если впереди прочный блок, он защищает блоки позади
                if (shieldResistance >= 10 && shieldResistance < 15) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Защищает соседние блоки вокруг укрепленного блока
     */
    private static void protectNearbyBlocks(BlockPos shieldPos, Set<BlockPos> protectedBlocks,
                                            ServerLevel level, BlockPos centerPos) {
        int shieldDistance = 3; // На каком расстоянии блок может защищать

        for (int x = -shieldDistance; x <= shieldDistance; x++) {
            for (int y = -shieldDistance; y <= shieldDistance; y++) {
                for (int z = -shieldDistance; z <= shieldDistance; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    BlockPos nearbyPos = shieldPos.offset(x, y, z);

                    // Проверяем, находится ли соседний блок на "теневой стороне" от взрыва
                    if (isInShadow(centerPos, shieldPos, nearbyPos)) {
                        protectedBlocks.add(nearbyPos);
                    }
                }
            }
        }
    }

    /**
     * Проверяет, находится ли целевой блок на теневой стороне от щита относительно центра взрыва
     */
    private static boolean isInShadow(BlockPos centerPos, BlockPos shieldPos, BlockPos targetPos) {
        // Вектор от центра к щиту
        int sX = shieldPos.getX() - centerPos.getX();
        int sY = shieldPos.getY() - centerPos.getY();
        int sZ = shieldPos.getZ() - centerPos.getZ();

        // Вектор от центра к цели
        int tX = targetPos.getX() - centerPos.getX();
        int tY = targetPos.getY() - centerPos.getY();
        int tZ = targetPos.getZ() - centerPos.getZ();

        // Скалярное произведение (если положительное, цель в похожем направлении)
        int dotProduct = sX * tX + sY * tY + sZ * tZ;

        // Цель в "тени" если она дальше щита и в похожем направлении
        double shieldDist = Math.sqrt(sX * sX + sY * sY + sZ * sZ);
        double targetDist = Math.sqrt(tX * tX + tY * tY + tZ * tZ);

        return dotProduct > 0 && targetDist > shieldDist * 0.8;
    }

    /**
     * Вычисляет шанс выживания блока на основе его прочности и расстояния от эпицентра
     * Возвращает значение от 0.0 до 1.0
     * 0.0 = 100% уничтожится, 1.0 = 100% выживет
     */
    private static float calculateSurvivalChance(int resistance, int distanceFromCenter, int maxRadius) {
        // Нормализуем расстояние (0 = эпицентр, 1 = край)
        float normalizedDistance = Math.min(1.0f, (float) distanceFromCenter / maxRadius);

        float survivalChance = 0.0f;

        switch (resistance) {
            case 0:
                // Прочность 0 - 0% выживают в любом случае (всегда уничтожаются)
                survivalChance = 0.0f;
                break;

            case 1:
            case 2:
            case 3:
            case 4:
                // Прочность 1-4 - крайне низкий шанс выживания, только на краях
                survivalChance = normalizedDistance * 0.2f;
                break;

            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                // Прочность 5-9 - средний шанс, особенно на краях
                survivalChance = 0.3f + (normalizedDistance * 0.4f);
                break;

            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
                // Прочность 10-14 - высокий шанс выживания
                survivalChance = 0.6f + (normalizedDistance * 0.35f);
                break;

            case 15:
                // Прочность 15 - неуничтожаемые (никогда не уничтожаются)
                survivalChance = 1.0f;
                break;
        }

        return survivalChance;
    }

    /**
     * Генерирует слой селлафита на ДНЕ воронки - МГНОВЕННАЯ версия
     */
    private static void generateCraterSurfaceInstant(ServerLevel level,
                                                     Set<BlockPos> removedBlocks,
                                                     Set<BlockPos> upperPartBlocks,
                                                     Block[] sellafieldBlocks,
                                                     RandomSource random) {
        Set<BlockPos> bottomBlocks = new HashSet<>();

        for (BlockPos pos : removedBlocks) {
            // Пропускаем блоки верхней части
            if (upperPartBlocks.contains(pos)) {
                continue;
            }

            BlockPos below = pos.below();

            // Если под блоком НЕТ удалённого блока - это дно воронки
            if (!removedBlocks.contains(below)) {
                bottomBlocks.add(pos);
            }
        }

        // Заменяем блоки дна на селлафит
        for (BlockPos bottomPos : bottomBlocks) {
            Block randomBlock = sellafieldBlocks[random.nextInt(sellafieldBlocks.length)];
            level.setBlock(bottomPos, randomBlock.defaultBlockState(), 3);

            // Заменяем слои выше дна
            BlockPos currentPos = bottomPos.above();
            for (int layer = 1; layer < SURFACE_LAYER_DEPTH; layer++) {
                if (removedBlocks.contains(currentPos) && !upperPartBlocks.contains(currentPos)) {
                    randomBlock = sellafieldBlocks[random.nextInt(sellafieldBlocks.length)];
                    level.setBlock(currentPos, randomBlock.defaultBlockState(), 3);
                    currentPos = currentPos.above();
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Создаёт круглую воронку от взрыва с анимацией
     */
    public static void generateCrater(ServerLevel level, BlockPos centerPos,
                                      int radius, int depth,
                                      Block[] sellafieldBlocks) {
        RandomSource random = level.random;
        ExplosionResistanceRegistry.init();

        if (sellafieldBlocks.length != 4) {
            throw new IllegalArgumentException("sellafieldBlocks должен содержать ровно 4 блока");
        }

        Map<BlockPos, Integer> blocksToProcess = new HashMap<>();
        Set<BlockPos> upperPartBlocks = new HashSet<>();

        // Сбор блоков
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -depth; y <= radius * 2; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);
                    double horizontalDistance = Math.sqrt(x * x + z * z);

                    boolean shouldCheck = false;
                    boolean isUpperPart = false;

                    if (y <= 0) {
                        double normalizedDepth = (double) (-y) / depth;
                        double depthRadius = radius * (1.0 - normalizedDepth * normalizedDepth);

                        if (horizontalDistance <= depthRadius) {
                            shouldCheck = true;
                        }
                    } else {
                        double edgeRadius = radius * Math.max(0, 1.0 - (double) y / (radius * 2));

                        if (horizontalDistance <= edgeRadius) {
                            shouldCheck = true;
                            isUpperPart = true;
                        }
                    }

                    if (shouldCheck) {
                        BlockState state = level.getBlockState(checkPos);

                        if (isWaterBlock(state) || isAirBlock(state)) {
                            continue;
                        }

                        if (isUpperPart) {
                            upperPartBlocks.add(checkPos);
                        }

                        double distanceFromCenter = Math.sqrt(x * x + y * y + z * z);
                        int distanceKey = (int) Math.round(distanceFromCenter);
                        blocksToProcess.put(checkPos, distanceKey);
                    }
                }
            }
        }

        // Сортируем по расстоянию
        List<Map.Entry<BlockPos, Integer>> sortedBlocks = new ArrayList<>(blocksToProcess.entrySet());
        sortedBlocks.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        // Планируем удаление с анимацией
        Set<BlockPos> removedBlocks = new HashSet<>();
        int blocksPerTick = Math.max(1, sortedBlocks.size() / 40);
        processBlocksAnimated(level, sortedBlocks, blocksPerTick, 0, removedBlocks,
                upperPartBlocks, centerPos, sellafieldBlocks);
    }

    /**
     * Обработка блоков с анимацией
     */
    private static void processBlocksAnimated(ServerLevel level,
                                              List<Map.Entry<BlockPos, Integer>> sortedBlocks,
                                              int blocksPerTick,
                                              int currentIndex,
                                              Set<BlockPos> removedBlocks,
                                              Set<BlockPos> upperPartBlocks,
                                              BlockPos centerPos,
                                              Block[] sellafieldBlocks) {
        RandomSource random = level.random;

        if (currentIndex >= sortedBlocks.size()) {
            generateCraterSurfaceInstant(level, removedBlocks, upperPartBlocks,
                    sellafieldBlocks, random);
            return;
        }

        Set<BlockPos> protectedBlocks = new HashSet<>();

        int endIndex = Math.min(currentIndex + blocksPerTick, sortedBlocks.size());
        for (int i = currentIndex; i < endIndex; i++) {
            BlockPos pos = sortedBlocks.get(i).getKey();
            Block block = level.getBlockState(pos).getBlock();
            int resistance = ExplosionResistanceRegistry.getResistance(block);

            if (resistance >= 15 || protectedBlocks.contains(pos)) {
                protectedBlocks.add(pos);
                continue;
            }

            int distance = sortedBlocks.get(i).getValue();
            float survivalChance = calculateSurvivalChance(resistance, distance, centerPos.distManhattan(pos));

            if (random.nextFloat() < survivalChance) {
                protectedBlocks.add(pos);
                if (resistance >= 6 && resistance < 15) {
                    protectNearbyBlocks(pos, protectedBlocks, level, centerPos);
                }
            } else {
                level.removeBlock(pos, false);
                removedBlocks.add(pos);
            }
        }

        int nextIndex = endIndex;
        if (level.getServer() != null) {
            level.getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                processBlocksAnimated(level, sortedBlocks, blocksPerTick, nextIndex,
                        removedBlocks, upperPartBlocks, centerPos, sellafieldBlocks);
            }));
        }
    }
}