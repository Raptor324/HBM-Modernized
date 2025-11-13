package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.tags.BlockTags;

/**
 * УЛУЧШЕННЫЙ ГЕНЕРАТОР КРАТЕРОВ С ЗОНАМИ ПОВРЕЖДЕНИЯ
 *
 * Новые возможности:
 * - Зона 1 (эпицентр): Уничтожение всего до земли
 * - Зона 2 (края воронки): Плавный переход от воронки к поверхности
 * - Зона 3 (10 блоков): Удаление листвы, стволы обугливаются
 * - Зона 4 (25 блоков): Частичное удаление листвы, без повреждения стволов
 *
 * По аналогии с ядерными взрывами из HBM Nuclear Tech 1.7.10
 */
public class CraterGenerator {

    private static final float BASE_HARDNESS_THRESHOLD = 5.0F;
    private static final float MAX_HARDNESS_THRESHOLD = 50.0F;
    private static final int SURFACE_LAYER_DEPTH = 1;
    private static final float STRETCH_FACTOR = 1.5F;
    private static final int REMOVAL_HEIGHT_ABOVE = 60;
    private static final float TOP_REMOVAL_RADIUS_MULTIPLIER = 1.3F;
    private static final int RING_COUNT = 6;
    private static final int TICKS_BETWEEN_RINGS = 20;
    private static final int SELLAFIT_SPAWN_HEIGHT = 0;
    private static final int SELLAFIT_SPAWN_DELAY = 1;

    // Зоны повреждения согласно схеме
    private static final int ZONE_3_RADIUS = 150;        // Зона уничтожения деревьев
    private static final int ZONE_4_RADIUS = 200;        // Зона частичного урона
    private static final int DAMAGE_ZONE_HEIGHT = 80;   // Высота обработки по Y
    private static BlockPos checkPos;

    public static void generateCrater(ServerLevel level, BlockPos centerPos,
                                      int radius, int depth,
                                      Block surfaceBlock1,
                                      Block surfaceBlock2,
                                      Block surfaceBlock3,
                                      Block surfaceBlock4,
                                      Block fallingBlock1,
                                      Block fallingBlock2,
                                      Block fallingBlock3,
                                      Block fallingBlock4,
                                      Block wasteLogBlock,
                                      Block wastePlanksBlock) {

        RandomSource random = level.random;
        float horizontalRadius = radius * STRETCH_FACTOR;
        float topRemovalRadius = horizontalRadius * TOP_REMOVAL_RADIUS_MULTIPLIER;
        Block[] fallingBlocks = {fallingBlock1, fallingBlock2, fallingBlock3, fallingBlock4};
        List<Set<BlockPos>> rings = new ArrayList<>();
        Set<BlockPos> craterBlocksSet = new HashSet<>();

        for (int i = 0; i < RING_COUNT; i++) {
            rings.add(new HashSet<>());
        }

        // Основной цикл для сбора блоков кратера
        for (int x = -(int) topRemovalRadius; x <= topRemovalRadius; x++) {
            for (int z = -(int) topRemovalRadius; z <= topRemovalRadius; z++) {
                for (int y = -depth; y <= REMOVAL_HEIGHT_ABOVE; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);

                    double normalizedX = (double) x / horizontalRadius;
                    double normalizedZ = (double) z / horizontalRadius;
                    double horizontalDistance = Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ);

                    boolean shouldCheck = false;

                    if (y <= 0) {
                        double normalizedY = Math.abs((double) y) / depth;
                        double ellipsoidDistance = Math.sqrt(horizontalDistance * horizontalDistance + normalizedY * normalizedY);

                        if (ellipsoidDistance <= 1.0) {
                            shouldCheck = true;
                        }
                    } else {
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
                            int ringIndex = calculateRingIndex(centerPos, checkPos, horizontalRadius, RING_COUNT);

                            if (ringIndex >= 0 && ringIndex < RING_COUNT) {
                                rings.get(ringIndex).add(checkPos);
                            }
                        }
                    }
                }
            }
        }

        processRingsSequentially(level, centerPos, rings, craterBlocksSet,
                fallingBlocks, topRemovalRadius, random, wasteLogBlock, wastePlanksBlock);
    }

    private static int calculateRingIndex(BlockPos center, BlockPos pos,
                                          float maxRadius, int ringCount) {
        double distance = Math.sqrt(
                Math.pow(pos.getX() - center.getX(), 2) +
                        Math.pow(pos.getZ() - center.getZ(), 2)
        );
        int ringIndex = (int) (distance / maxRadius * ringCount);
        return Math.min(ringIndex, ringCount - 1);
    }

    private static void processRingsSequentially(ServerLevel level,
                                                 BlockPos centerPos,
                                                 List<Set<BlockPos>> rings,
                                                 Set<BlockPos> craterBlocksSet,
                                                 Block[] fallingBlocks,
                                                 float topRemovalRadius,
                                                 RandomSource random,
                                                 Block wasteLogBlock,
                                                 Block wastePlanksBlock) {
        processRingAtIndex(level, centerPos, rings, craterBlocksSet,
                fallingBlocks, topRemovalRadius, random, 0, wasteLogBlock, wastePlanksBlock);
    }

    private static void processRingAtIndex(ServerLevel level,
                                           BlockPos centerPos,
                                           List<Set<BlockPos>> rings,
                                           Set<BlockPos> craterBlocksSet,
                                           Block[] fallingBlocks,
                                           float topRemovalRadius,
                                           RandomSource random,
                                           int ringIndex,
                                           Block wasteLogBlock,
                                           Block wastePlanksBlock) {
        if (ringIndex >= RING_COUNT) {
            removeItemsInRadius(level, centerPos, (int) topRemovalRadius + 10);
            applyDamageZones(level, centerPos, wasteLogBlock, wastePlanksBlock);
            return;
        }

        Set<BlockPos> currentRing = rings.get(ringIndex);

        for (BlockPos pos : currentRing) {
            level.removeBlock(pos, false);
        }

        generateCraterSurfaceForRing(level, centerPos, currentRing, craterBlocksSet,
                fallingBlocks, random);

        final int nextRingIndex = ringIndex + 1;
        if (level.getServer() != null) {
            level.getServer().tell(new net.minecraft.server.TickTask(TICKS_BETWEEN_RINGS, () -> {
                processRingAtIndex(level, centerPos, rings, craterBlocksSet,
                        fallingBlocks, topRemovalRadius, random, nextRingIndex, wasteLogBlock, wastePlanksBlock);


            }));
        }
    }

    private static void generateCraterSurfaceForRing(ServerLevel level,
                                                     BlockPos centerPos,
                                                     Set<BlockPos> ringBlocks,
                                                     Set<BlockPos> craterBlocksSet,
                                                     Block[] fallingBlocks,
                                                     RandomSource random) {

        Set<BlockPos> bottomBlocksInRing = new HashSet<>();

        for (BlockPos pos : ringBlocks) {
            BlockPos below = pos.below();

            // ИСПРАВЛЕНИЕ: Селлафит спаунится если:
            // 1. Блок ниже НЕ входит в множество удаляемых блоков кратера (это база, на которую падает селлафит)
            // 2. Если блок ниже - воздух, но ещё ниже есть земля - селлафит всё равно спаунится (над пещерами)
            if (!craterBlocksSet.contains(below)) {
                // Проверяем, есть ли ЛЮБОЙ твёрдый блок где-то ниже этой позиции
                // (чтобы селлафит падал на что-то, а не в бесконечность)
                boolean hasSolidBlockBelow = false;

                for (int checkY = -30; checkY <= 0; checkY++) {
                    BlockPos checkPos = below.above(checkY);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (!checkState.isAir() && checkState.getDestroySpeed(level, checkPos) >= 0) {
                        hasSolidBlockBelow = true;
                        break;
                    }
                }

                // Спаунить селлафит только если под ним есть твёрдый блок
                if (hasSolidBlockBelow) {
                    bottomBlocksInRing.add(pos);
                }
            }
        }

        for (BlockPos bottomPos : bottomBlocksInRing) {
            int blockIndex = random.nextInt(fallingBlocks.length);
            Block fallingBlock = fallingBlocks[blockIndex];
            BlockState blockState = fallingBlock.defaultBlockState();

            spawnFallingBlockAtPosition(level,
                    bottomPos.getX() + 0.5,
                    bottomPos.getY() + SELLAFIT_SPAWN_HEIGHT,
                    bottomPos.getZ() + 0.5,
                    blockState);

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
     * КЛЮЧЕВОЙ МЕТОД: Применяет зоны повреждения согласно схеме
     * Зона 3 (0-10 блоков): Удаление листвы, обугливание стволов и досок
     * Зона 4 (10-25 блоков): Частичное удаление листвы
     */
    private static void applyDamageZones(ServerLevel level, BlockPos centerPos,
                                         Block wasteLogBlock, Block wastePlanksBlock) {

        System.out.println("[CRATER] applyDamageZones вызван! Центр: " + centerPos);

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        int searchRadius = ZONE_4_RADIUS;
        int topSearchHeight = DAMAGE_ZONE_HEIGHT + 40; // увеличил для надёжности
        int bottomSearchHeight = 60;

        RandomSource random = level.random;

        for (int x = centerX - searchRadius; x <= centerX + searchRadius; x++) {
            for (int z = centerZ - searchRadius; z <= centerZ + searchRadius; z++) {
                for (int y = centerY - bottomSearchHeight; y <= centerY + topSearchHeight; y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);

                    double dx = x - centerX;
                    double dz = z - centerZ;
                    double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

                    if (horizontalDistance > ZONE_4_RADIUS) continue;

                    BlockState state = level.getBlockState(checkPos);

                    // ===== ЗОНА 3: 0-80 блоков =====
                    if (horizontalDistance <= ZONE_3_RADIUS) {
                        // Удаляем всю листву
                        if (state.is(BlockTags.LEAVES)) {
                            level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                            continue;
                        }

                        // Удаляем траву, снег и прочую растительность
                        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                                state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                                state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) ||
                                state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) ||
                                state.is(Blocks.PODZOL)) {
                            level.removeBlock(checkPos, false);
                            continue;
                        }

                        // Заменяем брёвна на обугленные
                        if (state.is(BlockTags.LOGS)) {
                            level.setBlock(checkPos, wasteLogBlock.defaultBlockState(), 3);
                            continue;
                        }

                        // Заменяем доски на обугленные
                        if (state.is(BlockTags.PLANKS)) {
                            level.setBlock(checkPos, wastePlanksBlock.defaultBlockState(), 3);
                            continue;
                        }
                    }
                    // ===== ЗОНА 4: 80-105 блоков =====
                    else if (horizontalDistance <= ZONE_4_RADIUS) {
                        // Удаляем листву с вероятностью 40%
                        if (state.is(BlockTags.LEAVES)) {

                            if (random.nextFloat() < 0.4F) {
                                level.removeBlock(checkPos, false);
                            }

                            if (state.is(BlockTags.LEAVES)) {
                                if (random.nextFloat() < 0.1F) {
                                    level.setBlock(checkPos, Blocks.FIRE.defaultBlockState(), 3);
                                }
                                continue;
                            }
                        }
                            // Удаляем траву и снег с вероятностью 100%
                            if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                                    state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                                    state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                                if (random.nextFloat() < 1F) {
                                    level.removeBlock(checkPos, false);
                                }
                                continue;
                            }
                            // Стволы и доски не трогаем!
                        }
                    }
                }
            }
        }

        /**
         * Проверяет, является ли блок брёвном (включая всевозможные варианты)
         */
        private static boolean isLogBlock (Block block){
            return block.defaultBlockState().is(BlockTags.LOGS);
        }

        private static boolean isPlanksBlock (Block block){
            return block.defaultBlockState().is(BlockTags.PLANKS);
        }

        /**
         * Вычисляет шанс разрушения блока на основе его твёрдости
         */
        private static float calculateBreakChance ( float hardness){
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
         * Спаунит падающий блок в указанной позиции
         */
        private static void spawnFallingBlockAtPosition (ServerLevel level,
        double x, double y, double z,
        BlockState blockState){
            try {
                FallingBlockEntity fallingEntity = FallingBlockEntity.fall(level,
                        new BlockPos((int) x, (int) y, (int) z),
                        blockState);

                if (fallingEntity != null) {
                    fallingEntity.setPos(x, y, z);
                    fallingEntity.time = 0;
                    level.addFreshEntity(fallingEntity);
                }
            } catch (Exception e) {
                spawnFallingBlockReflection(level, x, y, z, blockState);
            }
        }

        /**
         * Альтернативный способ спауна падающего блока через рефлексию
         */
        private static void spawnFallingBlockReflection (ServerLevel level,
        double x, double y, double z,
        BlockState blockState){
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
                level.setBlock(new BlockPos((int) x, (int) y, (int) z), blockState, 3);
            }
        }

        /**
         * Удаляет все предметы в радиусе кратера
         */
        private static void removeItemsInRadius (ServerLevel level, BlockPos centerPos,int radius){
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
    }