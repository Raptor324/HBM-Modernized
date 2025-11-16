package com.hbm_m.util;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.AABB;

/**
 * ГЕНЕРАТОР КРАТЕРОВ v11 - ДОБАВЛЕНА ОБУГЛЕННАЯ ТРАВА В ЗОНУ 4
 *
 * КЛЮЧЕВЫЕ ИЗМЕНЕНИЯ v11:
 * - ДОБАВЛЕНА поддержка BURNED_GRASS в параметры метода generateCrater
 * - ИНТЕГРИРОВАНА замена травы на BURNED_GRASS в зоне 4 (190-240 блоков)
 * - МЕТОДЫ applyDamageZones теперь получает burnedGrassBlock как параметр
 */

public class CraterGenerator {

    // Настройки воронки
    private static final float STRETCH_FACTOR = 1.5F;
    private static final int REMOVAL_HEIGHT_ABOVE = 80;
    private static final float TOP_REMOVAL_RADIUS_MULTIPLIER = 1.3F;
    private static final int RING_COUNT = 10;
    private static final int SELLAFIT_SPAWN_HEIGHT = 0;

    // Зоны повреждения согласно схеме
    private static final int ZONE_3_RADIUS = 190;
    private static final int ZONE_4_RADIUS = 260;
    private static final int DAMAGE_ZONE_HEIGHT = 80;

    // Параметры киллзоны
    private static final float ZONE_3_DAMAGE = 5.0F;
    private static final float ZONE_4_DAMAGE = 2.0F;
    private static final float FIRE_DURATION = 280.0F;

    // Параметры шума кратера
    private static final float HORIZONTAL_STRETCH_FACTOR = 0F;
    private static final float VERTICAL_STRETCH_FACTOR = 0F;

    // v9: УЛУЧШЕННЫЕ параметры перекрытия
    private static final float RING_OVERLAP_PERCENTAGE = 20.0F;
    private static final float EDGE_SOFTENING_FACTOR = 0.5F;

    // v10: ПАРАМЕТРЫ КОНТРОЛЯ СПАВНА
    private static final float SELLAFIT_SPAWN_PROBABILITY = 1.2F;
    private static final float SELLAFIT_EDGE_PROBABILITY = 1.2F;
    private static final int MIN_CRATER_NEIGHBORS_REQUIRED = 1;

    /**
     * Главный метод генерирования кратера
     * v11: ДОБАВЛЕН параметр burnedGrassBlock
     */
    public static void generateCrater(ServerLevel level, BlockPos centerPos,
                                      int radius, int depth,
                                      Block surfaceBlock1, Block surfaceBlock2, Block surfaceBlock3, Block surfaceBlock4,
                                      Block fallingBlock1, Block fallingBlock2, Block fallingBlock3, Block fallingBlock4,
                                      Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrassBlock) {

        RandomSource random = level.random;
        float stretchX = 1.0F + (random.nextFloat() - 0.5F) * HORIZONTAL_STRETCH_FACTOR;
        float stretchZ = 1.0F + (random.nextFloat() - 0.5F) * HORIZONTAL_STRETCH_FACTOR;
        float stretchY = 1.0F + (random.nextFloat() - 0.5F) * VERTICAL_STRETCH_FACTOR;

        float horizontalRadius = radius * STRETCH_FACTOR;
        float topRemovalRadius = horizontalRadius * TOP_REMOVAL_RADIUS_MULTIPLIER;

        Block[] fallingBlocks = { fallingBlock1, fallingBlock2, fallingBlock3, fallingBlock4 };

        List<Set<BlockPos>> rings = new ArrayList<>();
        Set<BlockPos> craterBlocksSet = new HashSet<>();

        for (int i = 0; i < RING_COUNT; i++) {
            rings.add(new HashSet<>());
        }

        // ОСНОВНОЙ ЦИКЛ - Сбор блоков кратера
        for (int x = -(int) topRemovalRadius; x <= topRemovalRadius; x++) {
            for (int z = -(int) topRemovalRadius; z <= topRemovalRadius; z++) {
                for (int y = -depth; y <= REMOVAL_HEIGHT_ABOVE; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);

                    double normalizedX = (double) x / (horizontalRadius * stretchX);
                    double normalizedZ = (double) z / (horizontalRadius * stretchZ);
                    double normalizedY = Math.abs((double) y) / (depth * stretchY);

                    double horizontalDistance = Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ);

                    boolean shouldCheck = false;

                    if (y <= 0) {
                        double ellipsoidDistance = Math.sqrt(
                                horizontalDistance * horizontalDistance +
                                        normalizedY * normalizedY
                        );

                        if (ellipsoidDistance <= 1.0) {
                            shouldCheck = true;
                        }
                    } else {
                        double topRemovalRadiusNorm = topRemovalRadius / horizontalRadius;
                        double normalizedHeight = (double) y / REMOVAL_HEIGHT_ABOVE;
                        double spheroidalFactor = Math.sqrt(Math.max(0, 1.0 - normalizedHeight * normalizedHeight));
                        double edgeRadius = topRemovalRadiusNorm * spheroidalFactor;

                        if (horizontalDistance <= edgeRadius && y < REMOVAL_HEIGHT_ABOVE) {
                            shouldCheck = true;
                        }
                    }

                    if (shouldCheck) {
                        BlockExplosionDefense.ExplosionDefenseResult defenseResult =
                                BlockExplosionDefense.calculateExplosionDamage(
                                        level, checkPos, centerPos, horizontalRadius, random
                                );

                        if (defenseResult.shouldBreak) {
                            craterBlocksSet.add(checkPos);
                            distributeBlockToRingsWithOverlap(centerPos, checkPos,
                                    horizontalRadius, RING_COUNT, rings);
                        }
                    }
                }
            }
        }

        CraterBiomeApplier.applyCraterBiomes(level, centerPos, radius);

        processRingsSequentially(level, centerPos, rings, craterBlocksSet,
                fallingBlocks, topRemovalRadius, random, wasteLogBlock, wastePlanksBlock,
                burnedGrassBlock, RING_COUNT, horizontalRadius, depth, stretchX, stretchZ, stretchY);
    }

    /**
     * Распределение блоков по кольцам с перекрытием
     */
    private static void distributeBlockToRingsWithOverlap(BlockPos center, BlockPos pos,
                                                          float maxRadius, int ringCount, List<Set<BlockPos>> rings) {
        double distance = Math.sqrt(
                Math.pow(pos.getX() - center.getX(), 2) +
                        Math.pow(pos.getZ() - center.getZ(), 2)
        );

        double ringWidth = maxRadius / ringCount;
        double idealRingIndex = distance / ringWidth;
        int primaryRing = Math.min(Math.max((int) idealRingIndex, 0), ringCount - 1);

        rings.get(primaryRing).add(pos);

        double distanceToBoundary = Math.abs(idealRingIndex - primaryRing);
        float overlapThreshold = RING_OVERLAP_PERCENTAGE / 100.0F;

        if (primaryRing < ringCount - 1 && distanceToBoundary > (1.0 - overlapThreshold)) {
            rings.get(primaryRing + 1).add(pos);
        }

        if (primaryRing > 0 && distanceToBoundary > (1.0 - overlapThreshold)) {
            rings.get(primaryRing - 1).add(pos);
        }
    }

    /**
     * Обрабатывает кольца последовательно
     */
    private static void processRingsSequentially(ServerLevel level,
                                                 BlockPos centerPos,
                                                 List<Set<BlockPos>> rings,
                                                 Set<BlockPos> craterBlocksSet,
                                                 Block[] fallingBlocks,
                                                 float topRemovalRadius,
                                                 RandomSource random,
                                                 Block wasteLogBlock,
                                                 Block wastePlanksBlock,
                                                 Block burnedGrassBlock,
                                                 int ringCount,
                                                 float horizontalRadius,
                                                 int depth,
                                                 float stretchX,
                                                 float stretchZ,
                                                 float stretchY) {
        processRingAtIndex(level, centerPos, rings, craterBlocksSet,
                fallingBlocks, topRemovalRadius, random, 0, wasteLogBlock, wastePlanksBlock,
                burnedGrassBlock, ringCount, horizontalRadius, depth, stretchX, stretchZ, stretchY);
    }

    /**
     * Обработка одного кольца
     */
    private static void processRingAtIndex(ServerLevel level,
                                           BlockPos centerPos,
                                           List<Set<BlockPos>> rings,
                                           Set<BlockPos> craterBlocksSet,
                                           Block[] fallingBlocks,
                                           float topRemovalRadius,
                                           RandomSource random,
                                           int ringIndex,
                                           Block wasteLogBlock,
                                           Block wastePlanksBlock,
                                           Block burnedGrassBlock,
                                           int ringCount,
                                           float horizontalRadius,
                                           int depth,
                                           float stretchX,
                                           float stretchZ,
                                           float stretchY) {

        if (ringIndex >= ringCount) {
            removeItemsInRadius(level, centerPos, (int) topRemovalRadius + 10);
            applyDamageZones(level, centerPos, wasteLogBlock, wastePlanksBlock, burnedGrassBlock, random);
            System.out.println("[CRATER] Генерация кратера завершена!");
            return;
        }

        Set<BlockPos> currentRing = rings.get(ringIndex);
        Map<Long, List<BlockPos>> blocksByChunk = groupBlocksByChunk(currentRing);

        System.out.println("[CRATER] Обработка кольца " + ringIndex + " (" + currentRing.size() +
                " блоков в " + blocksByChunk.size() + " чанках)");

        for (long chunkKey : blocksByChunk.keySet()) {
            int[] chunkCoords = decodeChunkKey(chunkKey);
            SellafitSolidificationTracker.registerChunkStart(level, chunkCoords[0], chunkCoords[1]);
        }

        // Удаляем блоки в текущем кольце
        for (BlockPos pos : currentRing) {
            level.removeBlock(pos, false);
        }

        // v10: ПЕРЕРАБОТАННАЯ генерация поверхности
        generateCraterSurfaceForRingV10(level, centerPos, currentRing, craterBlocksSet,
                fallingBlocks, random, ringIndex, ringCount - 1, horizontalRadius);

        int nextRingIndex = ringIndex + 1;
        int delayTicks = calculateDelayForNextRing(level, blocksByChunk);

        if (level.getServer() != null) {
            level.getServer().tell(new net.minecraft.server.TickTask(delayTicks, () -> {
                System.out.println("[CRATER] Переход к кольцу " + nextRingIndex +
                        " (задержка: " + delayTicks + " тиков)");
                processRingAtIndex(level, centerPos, rings, craterBlocksSet,
                        fallingBlocks, topRemovalRadius, random, nextRingIndex,
                        wasteLogBlock, wastePlanksBlock, burnedGrassBlock, ringCount,
                        horizontalRadius, depth, stretchX, stretchZ, stretchY);
            }));
        }
    }

    /**
     * v10: ИСПРАВЛЕННАЯ генерация поверхности кратера
     *
     * Ключевые отличия:
     * - Проверяем что блок действительно был удален из craterBlocksSet
     * - Проверяем соседние блоки, чтобы убедиться в целостности края
     * - Используем вероятность спавна для контроля кол-ва сущностей
     * - Никогда не спавним селлафит в "дырах" между кольцами
     */
    private static void generateCraterSurfaceForRingV10(ServerLevel level,
                                                        BlockPos centerPos,
                                                        Set<BlockPos> ringBlocks,
                                                        Set<BlockPos> craterBlocksSet,
                                                        Block[] fallingBlocks,
                                                        RandomSource random,
                                                        int ringIndex,
                                                        int lastRingIndex,
                                                        float horizontalRadius) {

        float ringWidth = horizontalRadius / (lastRingIndex + 1);
        float minRingRadius = ringIndex * ringWidth;
        float maxRingRadius = (ringIndex + 1) * ringWidth;

        for (BlockPos pos : ringBlocks) {
            double distanceFromCenter = Math.sqrt(
                    Math.pow(pos.getX() - centerPos.getX(), 2) +
                            Math.pow(pos.getZ() - centerPos.getZ(), 2)
            );

            BlockPos below = pos.below();

            // v10: КРИТИЧЕСКАЯ ПРОВЕРКА - блок должен быть из craterBlocksSet
            if (craterBlocksSet.contains(below)) {
                // Не спавним под блоками которые еще в кратере
                continue;
            }

            // v10: Проверяем, что ниже есть земля/камень
            boolean hasSolidBlockBelow = false;
            for (int checkY = -50; checkY <= 1; checkY++) {
                BlockPos checkPos = below.above(checkY);
                BlockState checkState = level.getBlockState(checkPos);
                if (!checkState.isAir() && checkState.isSolidRender(level, checkPos)) {
                    hasSolidBlockBelow = true;
                    break;
                }
            }

            if (!hasSolidBlockBelow) {
                continue;
            }

            // v10: НОВАЯ ЛОГИКА - проверяем соседей из craterBlocksSet
            // Если у блока недостаточно соседей из кратера, это может быть "дыра"
            int craterNeighborCount = countCraterNeighbors(pos, craterBlocksSet);

            if (craterNeighborCount < MIN_CRATER_NEIGHBORS_REQUIRED) {
                // Этот блок на краю или в дыре - пропускаем или спавним с низкой вероятностью
                if (random.nextFloat() > 0.1F) {
                    continue; // 90% шанс пропустить сомнительные блоки
                }
            }

            // v10: Определяем позицию в кольце для контроля вероятности
            float positionInRing = (float) (distanceFromCenter - minRingRadius) /
                    (maxRingRadius - minRingRadius);
            positionInRing = Math.max(0, Math.min(1, positionInRing));

            // v10: Вероятность зависит от позиции в кольце
            float baseProbability = (ringIndex == 0) ? SELLAFIT_SPAWN_PROBABILITY :
                    (1.0F - positionInRing * (1.0F - SELLAFIT_EDGE_PROBABILITY));

            // Дополнительное уменьшение вероятности на краях для плавности
            float finalProbability = baseProbability * (1.0F - (float) Math.pow(positionInRing, 2) * 0.3F);

            if (random.nextFloat() < finalProbability) {
                int blockIndex = random.nextInt(fallingBlocks.length);
                Block fallingBlock = fallingBlocks[blockIndex];
                BlockState blockState = fallingBlock.defaultBlockState();

                // v10: Высота спавна зависит от позиции в кольце
                int extraHeight = (int) (positionInRing * 3);

                spawnFallingBlockAtPosition(level,
                        pos.getX() + 0.5,
                        pos.getY() + SELLAFIT_SPAWN_HEIGHT + extraHeight,
                        pos.getZ() + 0.5,
                        blockState);
            }
        }
    }

    /**
     * v10: НОВЫЙ метод - подсчитывает соседей блока из craterBlocksSet
     * Это помогает выявить "дыры" в селлафите
     */
    private static int countCraterNeighbors(BlockPos pos, Set<BlockPos> craterBlocksSet) {
        int count = 0;

        // Проверяем 6 соседей (вверх, вниз, север, юг, запад, восток)
        BlockPos[] neighbors = {
                pos.above(),
                pos.below(),
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west()
        };

        for (BlockPos neighbor : neighbors) {
            if (craterBlocksSet.contains(neighbor)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Группирует блоки кольца по чанкам для оптимизации загрузки
     */
    private static Map<Long, List<BlockPos>> groupBlocksByChunk(Set<BlockPos> blocks) {
        Map<Long, List<BlockPos>> result = new HashMap<>();

        for (BlockPos pos : blocks) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);

            result.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(pos);
        }

        return result;
    }

    /**
     * Декодирует ключ чанка в координаты
     */
    private static int[] decodeChunkKey(long chunkKey) {
        int chunkX = (int) (chunkKey >> 32);
        int chunkZ = (int) (chunkKey & 0xFFFFFFFFL);
        return new int[]{chunkX, chunkZ};
    }

    /**
     * Вычисляет оптимальную задержку до следующего кольца
     */
    private static int calculateDelayForNextRing(ServerLevel level,
                                                 Map<Long, List<BlockPos>> blocksByChunk) {
        final int MIN_DELAY = 10;

        if (blocksByChunk.isEmpty()) {
            return MIN_DELAY;
        }

        int maxRemainingTicks = 0;

        for (long chunkKey : blocksByChunk.keySet()) {
            int[] chunkCoords = decodeChunkKey(chunkKey);
            int remainingTicks = SellafitSolidificationTracker.getRemainingTicks(
                    level, chunkCoords[0], chunkCoords[1]);
            maxRemainingTicks = Math.max(maxRemainingTicks, remainingTicks);
        }

        return Math.max(MIN_DELAY, maxRemainingTicks + 10);
    }

    /**
     * Спавнит падающий блок в указанной позиции
     */
    private static void spawnFallingBlockAtPosition(ServerLevel level,
                                                    double x, double y, double z, BlockState blockState) {
        FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall(level,
                new BlockPos((int) x, (int) y, (int) z), blockState);
        fallingBlockEntity.setHurtsEntities(0.5F, 15);
        level.addFreshEntity(fallingBlockEntity);
    }

    /**
     * Удаляет предметы в радиусе
     */
    private static void removeItemsInRadius(ServerLevel level, BlockPos centerPos, int radius) {
        AABB removalArea = new AABB(
                centerPos.getX() - radius,
                centerPos.getY() - 100,
                centerPos.getZ() - radius,
                centerPos.getX() + radius,
                centerPos.getY() + 100,
                centerPos.getZ() + radius
        );

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, removalArea);
        for (ItemEntity item : items) {
            item.discard();
        }
    }

    /**
     * Применяет зоны повреждения
     * v11: ДОБАВЛЕНА поддержка burnedGrassBlock
     */
    private static void applyDamageZones(ServerLevel level, BlockPos centerPos,
                                         Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrassBlock, RandomSource random) {
        System.out.println("[CRATER] applyDamageZones активирован! Центр: " + centerPos);

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        int searchRadius = ZONE_4_RADIUS;
        int topSearchHeight = DAMAGE_ZONE_HEIGHT + 40;
        int bottomSearchHeight = 60;

        // БЛОКОВАЯ ОБРАБОТКА
        for (int x = centerX - searchRadius; x <= centerX + searchRadius; x++) {
            for (int z = centerZ - searchRadius; z <= centerZ + searchRadius; z++) {
                for (int y = centerY - bottomSearchHeight; y <= centerY + topSearchHeight; y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);

                    double dx = x - centerX;
                    double dz = z - centerZ;
                    double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

                    if (horizontalDistance > ZONE_4_RADIUS) continue;

                    BlockState state = level.getBlockState(checkPos);

                    // ЗОНА 3: 0-190 блоков
                    if (horizontalDistance <= ZONE_3_RADIUS) {
                        if (state.is(BlockTags.LEAVES)) {
                            level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                            continue;
                        }
                        if (state.is(Blocks.GRASS_BLOCK)) {
                            level.setBlock(checkPos, burnedGrassBlock.defaultBlockState(), 3);
                            continue;
                        }
                        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                                state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                                state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                            level.removeBlock(checkPos, false);
                            continue;
                        }

                        if (state.is(BlockTags.LOGS)) {
                            level.setBlock(checkPos, wasteLogBlock.defaultBlockState(), 3);
                            continue;
                        }

                        if (state.is(BlockTags.PLANKS)) {
                            level.setBlock(checkPos, wastePlanksBlock.defaultBlockState(), 3);
                            continue;
                        }
                    }
                    // ЗОНА 4: 190-240 блоков
                    else if (horizontalDistance <= ZONE_4_RADIUS) {
                        if (state.is(BlockTags.LEAVES)) {
                            if (random.nextFloat() < 0.4F) {
                                level.removeBlock(checkPos, false);
                            } else if (random.nextFloat() < 0.1F) {
                                level.setBlock(checkPos, Blocks.FIRE.defaultBlockState(), 3);
                            }
                            continue;
                        }

                        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                                state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                                state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) ||
                                state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) ||
                                state.is(Blocks.PODZOL)) {
                            level.removeBlock(checkPos, false);
                            continue;
                        }

                        if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS)) {
                            level.removeBlock(checkPos, false);
                            continue;
                        }

                        if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)) {
                            if (random.nextFloat() < 0.6F) {
                                level.removeBlock(checkPos, false);
                            }
                            continue;
                        }
                    }
                }
            }
        }

        applyKillZone(level, centerPos, random);
    }

    /**
     * Применяет киллзону с уроном и поджигом сущностей
     */
    private static void applyKillZone(ServerLevel level, BlockPos centerPos, RandomSource random) {
        System.out.println("[CRATER] applyKillZone активирован! Центр: " + centerPos);

        AABB zone3 = new AABB(
                centerPos.getX() - ZONE_3_RADIUS,
                centerPos.getY() - DAMAGE_ZONE_HEIGHT,
                centerPos.getZ() - ZONE_3_RADIUS,
                centerPos.getX() + ZONE_3_RADIUS,
                centerPos.getY() + DAMAGE_ZONE_HEIGHT,
                centerPos.getZ() + ZONE_3_RADIUS
        );

        AABB zone4 = new AABB(
                centerPos.getX() - ZONE_4_RADIUS,
                centerPos.getY() - DAMAGE_ZONE_HEIGHT,
                centerPos.getZ() - ZONE_4_RADIUS,
                centerPos.getX() + ZONE_4_RADIUS,
                centerPos.getY() + DAMAGE_ZONE_HEIGHT,
                centerPos.getZ() + ZONE_4_RADIUS
        );

        List<LivingEntity> entitiesZone3 = level.getEntitiesOfClass(LivingEntity.class, zone3);
        List<LivingEntity> entitiesZone4 = level.getEntitiesOfClass(LivingEntity.class, zone4);

        for (LivingEntity entity : entitiesZone3) {
            entity.hurt(level.damageSources().generic(), ZONE_3_DAMAGE);
            entity.setSecondsOnFire((int) FIRE_DURATION / 20);
        }

        for (LivingEntity entity : entitiesZone4) {
            if (!entitiesZone3.contains(entity)) {
                entity.hurt(level.damageSources().generic(), ZONE_4_DAMAGE);
            }
        }
    }
}