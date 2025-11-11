package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
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
    private static final float BASE_HARDNESS_THRESHOLD = 5.0F;
    private static final float MAX_HARDNESS_THRESHOLD = 50.0F;
    private static final int SURFACE_LAYER_DEPTH = 3;

    // Множитель вытянутости для горизонтальной оси (больше = более вытянутая)
    private static final float STRETCH_FACTOR = 1.5F;

    // УВЕЛИЧЕННАЯ высота удаления блоков над воронкой (в блоках)
    private static final int REMOVAL_HEIGHT_ABOVE = 60;

    // Увеличенный горизонтальный радиус удаления блоков над воронкой
    private static final float TOP_REMOVAL_RADIUS_MULTIPLIER = 1.3F;

    // ОПТИМИЗАЦИЯ: Параметры для кольцевого разбиения
    private static final int RING_COUNT = 7; // Количество колец (от центра к краям)
    private static final int TICKS_BETWEEN_RINGS = 20; // Тиков между спауном каждого кольца

    // ИСПРАВЛЕННЫЕ ЗНАЧЕНИЯ ДЛЯ СЕЛЛАФИТА:
    private static final int SELLAFIT_SPAWN_HEIGHT = 0;
    private static final int SELLAFIT_SPAWN_DELAY = 1; // Уменьшено для лучшей производительности

    /**
     * Создаёт вытянутую воронку от взрыва (эллиптоидная форма)
     * С ОПТИМИЗАЦИЕЙ: разбивка на кольца для лучшей производительности
     */
    public static void generateCrater(ServerLevel level, BlockPos centerPos,
                                      int radius, int depth,
                                      Block surfaceBlock1,
                                      Block surfaceBlock2,
                                      Block surfaceBlock3,
                                      Block surfaceBlock4,
                                      Block fallingBlock1,
                                      Block fallingBlock2,
                                      Block fallingBlock3,
                                      Block fallingBlock4) {
        RandomSource random = level.random;

        // Применяем вытянутость к горизонтальному радиусу
        float horizontalRadius = radius * STRETCH_FACTOR;
        float topRemovalRadius = horizontalRadius * TOP_REMOVAL_RADIUS_MULTIPLIER;

        Block[] fallingBlocks = {fallingBlock1, fallingBlock2, fallingBlock3, fallingBlock4};

        // Фаза 1: Разбиваем кратер на кольца и сохраняем блоки в каждом
        List<Set<BlockPos>> rings = new ArrayList<>();
        Set<BlockPos> craterBlocksSet = new HashSet<>();

        // Инициализируем кольца
        for (int i = 0; i < RING_COUNT; i++) {
            rings.add(new HashSet<>());
        }

        // Рассчитываем блоки для удаления и распределяем их по кольцам
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
                            craterBlocksSet.add(checkPos);

                            // Определяем, в какое кольцо добавить этот блок
                            int ringIndex = calculateRingIndex(centerPos, checkPos, horizontalRadius, RING_COUNT);
                            if (ringIndex >= 0 && ringIndex < RING_COUNT) {
                                rings.get(ringIndex).add(checkPos);
                            }
                        }
                    }
                }
            }
        }

        // Фаза 2: Обработка колец последовательно, от центра к краям
        processRingsSequentially(level, centerPos, rings, craterBlocksSet,
                fallingBlocks, topRemovalRadius, random);
    }

    /**
     * Рассчитывает индекс кольца для позиции
     * Кольцо 0 - центр, Кольцо RING_COUNT-1 - край
     */
    private static int calculateRingIndex(BlockPos center, BlockPos pos,
                                          float maxRadius, int ringCount) {
        double distance = Math.sqrt(
                Math.pow(pos.getX() - center.getX(), 2) +
                        Math.pow(pos.getZ() - center.getZ(), 2)
        );

        int ringIndex = (int) (distance / maxRadius * ringCount);
        return Math.min(ringIndex, ringCount - 1);
    }

    /**
     * НОВЫЙ МЕТОД: Обработка колец последовательно
     * Каждое кольцо обрабатывается с задержкой для оптимизации
     */
    private static void processRingsSequentially(ServerLevel level,
                                                 BlockPos centerPos,
                                                 List<Set<BlockPos>> rings,
                                                 Set<BlockPos> craterBlocksSet,
                                                 Block[] fallingBlocks,
                                                 float topRemovalRadius,
                                                 RandomSource random) {
        // Начинаем обработку с первого кольца
        processRingAtIndex(level, centerPos, rings, craterBlocksSet,
                fallingBlocks, topRemovalRadius, random, 0);
    }

    /**
     * Обрабатывает одно кольцо и запланирует следующее
     */
    private static void processRingAtIndex(ServerLevel level,
                                           BlockPos centerPos,
                                           List<Set<BlockPos>> rings,
                                           Set<BlockPos> craterBlocksSet,
                                           Block[] fallingBlocks,
                                           float topRemovalRadius,
                                           RandomSource random,
                                           int ringIndex) {
        // Защита от выхода за границы
        if (ringIndex >= RING_COUNT) {
            // Все кольца обработаны!
            removeItemsInRadius(level, centerPos, (int)topRemovalRadius + 10);
            return;
        }

        Set<BlockPos> currentRing = rings.get(ringIndex);

        // Удаляем блоки в текущем кольце
        for (BlockPos pos : currentRing) {
            level.removeBlock(pos, false);
        }

        // Генерируем селлафит для текущего кольца
        generateCraterSurfaceForRing(level, centerPos, currentRing, craterBlocksSet,
                fallingBlocks, random);

        // Запланируем следующее кольцо с задержкой
        final int nextRingIndex = ringIndex + 1;
        if (level.getServer() != null) {
            level.getServer().tell(new net.minecraft.server.TickTask(TICKS_BETWEEN_RINGS, () -> {
                processRingAtIndex(level, centerPos, rings, craterBlocksSet,
                        fallingBlocks, topRemovalRadius, random, nextRingIndex);
            }));
        }
    }

    /**
     * Генерирует селлафит для одного кольца
     */
    private static void generateCraterSurfaceForRing(ServerLevel level,
                                                     BlockPos centerPos,
                                                     Set<BlockPos> ringBlocks,
                                                     Set<BlockPos> craterBlocksSet,
                                                     Block[] fallingBlocks,
                                                     RandomSource random) {
        // Находим блоки ДНА в этом кольце
        Set<BlockPos> bottomBlocksInRing = new HashSet<>();

        for (BlockPos pos : ringBlocks) {
            BlockPos below = pos.below();

            if (!craterBlocksSet.contains(below)) {
                BlockState stateBelow = level.getBlockState(below);
                if (!stateBelow.isAir()) {
                    bottomBlocksInRing.add(pos);
                }
            }
        }

        // Спауним селлафит для этого кольца
        for (BlockPos bottomPos : bottomBlocksInRing) {
            int blockIndex = random.nextInt(fallingBlocks.length);
            Block fallingBlock = fallingBlocks[blockIndex];
            BlockState blockState = fallingBlock.defaultBlockState();

            spawnFallingBlockAtPosition(level,
                    bottomPos.getX() + 0.5,
                    bottomPos.getY() + SELLAFIT_SPAWN_HEIGHT,
                    bottomPos.getZ() + 0.5,
                    blockState);

            // Спауним слои выше
            BlockPos currentPos = bottomPos.above();
            for (int layer = 1; layer < SURFACE_LAYER_DEPTH; layer++) {
                if (craterBlocksSet.contains(currentPos)) {
                    blockIndex = random.nextInt(fallingBlocks.length);
                    Block layerBlock = fallingBlocks[blockIndex];
                    BlockState layerBlockState = layerBlock.defaultBlockState();

                    spawnFallingBlockAtPosition(level,
                            currentPos.getX() + 0.5,
                            currentPos.getY() + SELLAFIT_SPAWN_HEIGHT,
                            currentPos.getZ() + 0.5,
                            layerBlockState);

                    currentPos = currentPos.above();
                } else {
                    break;
                }
            }
        }
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
     * ОПТИМИЗИРОВАННЫЙ СПОСОБ: Создание FallingBlockEntity
     * Вариант 1: FallingBlockEntity.fall()
     * Вариант 2: Reflection (fallback)
     */
    private static void spawnFallingBlockAtPosition(ServerLevel level,
                                                    double x, double y, double z,
                                                    BlockState blockState) {
        try {
            // ВАРИАНТ 1: Используем FallingBlockEntity.fall()
            FallingBlockEntity fallingEntity = FallingBlockEntity.fall(level,
                    new BlockPos((int)x, (int)y, (int)z),
                    blockState);

            if (fallingEntity != null) {
                fallingEntity.setPos(x, y, z);
                fallingEntity.time = 0;
                level.addFreshEntity(fallingEntity);
            }
        } catch (Exception e) {
            // Fallback: Используем Reflection
            spawnFallingBlockReflection(level, x, y, z, blockState);
        }
    }

    /**
     * Fallback: Reflection для создания FallingBlockEntity
     */
    private static void spawnFallingBlockReflection(ServerLevel level,
                                                    double x, double y, double z,
                                                    BlockState blockState) {
        try {
            var constructor = FallingBlockEntity.class.getDeclaredConstructor(
                    net.minecraft.world.level.Level.class,
                    double.class, double.class, double.class,
                    BlockState.class);

            constructor.setAccessible(true);

            FallingBlockEntity fallingEntity = constructor.newInstance(level, x, y, z, blockState);

            fallingEntity.time = 0;
            level.addFreshEntity(fallingEntity);

        } catch (Exception e) {
            // Ultimate fallback: просто ставим блок на место
            level.setBlock(new BlockPos((int)x, (int)y, (int)z), blockState, 3);
        }
    }

    /**
     * Удаляет все предметы (ItemEntity) в указанном радиусе
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
     * БЕЗ оптимизации (использует старый способ)
     */
    public static void generateCraterInstant(ServerLevel level, BlockPos centerPos,
                                             int radius, int depth,
                                             Block surfaceBlock1,
                                             Block surfaceBlock2,
                                             Block surfaceBlock3,
                                             Block surfaceBlock4,
                                             Block fallingBlock1,
                                             Block fallingBlock2,
                                             Block fallingBlock3,
                                             Block fallingBlock4) {
        RandomSource random = level.random;
        Block[] fallingBlocks = {fallingBlock1, fallingBlock2, fallingBlock3, fallingBlock4};
        Set<BlockPos> removedBlocks = new HashSet<>();
        Set<BlockPos> bottomBlocks = new HashSet<>();

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
        for (BlockPos pos : removedBlocks) {
            BlockPos below = pos.below();
            if (!removedBlocks.contains(below)) {
                BlockState stateBelow = level.getBlockState(below);
                if (!stateBelow.isAir()) {
                    bottomBlocks.add(pos);
                }
            }
        }

        // Размещаем гравитирующий селлафит
        for (BlockPos bottomPos : bottomBlocks) {
            int blockIndex = random.nextInt(fallingBlocks.length);
            Block fallingBlock = fallingBlocks[blockIndex];
            BlockState blockState = fallingBlock.defaultBlockState();

            spawnFallingBlockAtPosition(level,
                    bottomPos.getX() + 0.5,
                    bottomPos.getY() + SELLAFIT_SPAWN_HEIGHT,
                    bottomPos.getZ() + 0.5,
                    blockState);

            // Спауним слои выше
            BlockPos currentPos = bottomPos.above();
            for (int layer = 1; layer < SURFACE_LAYER_DEPTH; layer++) {
                if (removedBlocks.contains(currentPos)) {
                    blockIndex = random.nextInt(fallingBlocks.length);
                    Block layerBlock = fallingBlocks[blockIndex];
                    BlockState layerBlockState = layerBlock.defaultBlockState();

                    spawnFallingBlockAtPosition(level,
                            currentPos.getX() + 0.5,
                            currentPos.getY() + SELLAFIT_SPAWN_HEIGHT,
                            currentPos.getZ() + 0.5,
                            layerBlockState);

                    currentPos = currentPos.above();
                } else {
                    break;
                }
            }
        }

        // Удаляем предметы
        removeItemsInRadius(level, centerPos, (int)topRemovalRadius + 10);
    }
}