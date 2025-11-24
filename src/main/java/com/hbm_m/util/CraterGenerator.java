package com.hbm_m.util;

import com.hbm_m.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;

import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;

import java.util.*;

public class CraterGenerator {

    // --- Константы демонстрируют ключевые параметры кратера и зон урона ---

    /** Нормализует форму кратера по горизонтали, образуя эллипсоидный профиль */
    private static final float STRETCH_FACTOR = 1.5F;
    /** Высота удаления блоков над центром кратера (высшая точка удаляемых блоков) */
    private static final int REMOVAL_HEIGHT_ABOVE = 80;
    /** Коэффициент радиуса удаления в верхней части, увеличивает радиус действия */
    private static final float TOP_REMOVAL_RADIUS_MULTIPLIER = 1.3F;
    /** Количество колец вокруг центра кратера для постепенной обработки */
    private static final int RING_COUNT = 8;
    /** Максимальное число слоев селлафита под поверхностью кратера */
    private static final int SELLAFIT_REPLACEMENT_LAYERS = 4;
    /** Глубина кратера — глубина генерации по вертикали */
    private static final int CRATER_DEPTH = 30;
    /** Радиус внутренней зоны высокой опасности и замещения (3 зона) */
    private static final int ZONE_3_RADIUS = 170;
    /** Радиус внешней зоны средней опасности (4 зона) */
    private static final int ZONE_4_RADIUS = 280;
    /** Квадраты радиусов для оптимизации дистанционных проверок */
    private static final long ZONE_3_RADIUS_SQ = (long)ZONE_3_RADIUS * ZONE_3_RADIUS;
    private static final long ZONE_4_RADIUS_SQ = (long)ZONE_4_RADIUS * ZONE_4_RADIUS;
    /** Высота по Y для зоны повреждения */
    private static final int DAMAGE_ZONE_HEIGHT = 80;

    /** Урон, наносимый живым сущностям в 3 зоне */
    private static final float ZONE_3_DAMAGE = 500.0F;
    /** Урон в 4 зоне */
    private static final float ZONE_4_DAMAGE = 200.0F;
    /** Длительность эффекта огня для сущностей (в тиках) */
    private static final float FIRE_DURATION = 380.0F;

    /** Коэффициенты растяжения кратера в процессе генерации, дающие вариации */
    private static final float HORIZONTAL_STRETCH_FACTOR = 0F;
    private static final float VERTICAL_STRETCH_FACTOR = 0F;
    /** Процент перекрытия колец — плавность переходов зон */
    private static final float RING_OVERLAP_PERCENTAGE = 20.0F;

    /** Размер пакетов для батчевой обработки блоков (для производительности) */
    private static final int BLOCK_BATCH_SIZE = 256;
    private static final int SELLAFIT_OVERHANG_CHECK_DEPTH = 5; // Глубина проверки твердого блока снизу для затвердевания селлафита
    /**
     * Генерация кратера — главный метод с основным циклом и планировкой задач.
     *
     * @param level игровой мир
     * @param centerPos позиция центра кратера
     * @param radius радиус кратера
     * @param depth глубина кратера
     * @param wasteLogBlock блок выжженных брёвен
     * @param wastePlanksBlock блок выжженных досок
     * @param burnedGrassBlock блок выжженной травы
     */
    public static void generateCrater(ServerLevel level, BlockPos centerPos,
                                      int radius, int depth,
                                      Block sellafit1, Block sellafit2, Block sellafit3, Block sellafit4,
                                      Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrassBlock) {
        RandomSource random = level.random;

        float stretchX = 1.0F + (random.nextFloat() - 0.5F) * HORIZONTAL_STRETCH_FACTOR;
        float stretchZ = 1.0F + (random.nextFloat() - 0.5F) * VERTICAL_STRETCH_FACTOR;
        float stretchY = 1.0F + (random.nextFloat() - 0.5F) * VERTICAL_STRETCH_FACTOR;
        float horizontalRadius = radius * STRETCH_FACTOR;
        float topRemovalRadius = horizontalRadius * TOP_REMOVAL_RADIUS_MULTIPLIER;

        Block[] selafitBlocks = {sellafit1, sellafit2, sellafit3, sellafit4};

        List<Set<BlockPos>> rings = new ArrayList<>();
        Set<BlockPos> craterBlocksSet = new HashSet<>();

        for (int i = 0; i < RING_COUNT; i++) {
            rings.add(new HashSet<>());
        }

        long startTime = System.currentTimeMillis();

        collectCraterBlocksOptimized(level, centerPos, (int) topRemovalRadius, depth,
                horizontalRadius, topRemovalRadius, stretchX, stretchZ, stretchY,
                craterBlocksSet, rings);

        processAllRingsBatched(level, centerPos, rings, craterBlocksSet,
                selafitBlocks, random, topRemovalRadius, depth,
                wasteLogBlock, wastePlanksBlock, burnedGrassBlock);

        removeItemsInRadiusBatched(level, centerPos, (int) topRemovalRadius + 20);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.tell(new TickTask(2, () -> {
                try {
                    CraterBiomeApplier.applyCraterBiomes(level, centerPos, (int) horizontalRadius);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                server.tell(new TickTask(1, () -> {
                    try {
                        applyDamageZonesOptimizedV2(level, centerPos, wasteLogBlock, wastePlanksBlock, burnedGrassBlock, selafitBlocks, random);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));

            }));
        }

        long endTime = System.currentTimeMillis();
        System.out.println("[CRATER] Генерация кратера завершена за " + (endTime - startTime) + " мс");
    }


    private static void collectCraterBlocksOptimized(ServerLevel level, BlockPos centerPos, int searchRadius, int depth,
                                                     float horizontalRadius, float topRemovalRadius,
                                                     float stretchX, float stretchZ, float stretchY,
                                                     Set<BlockPos> craterBlocksSet, List<Set<BlockPos>> rings) {
        double invHorizontalRadiusX = 1.0 / (horizontalRadius * stretchX);
        double invHorizontalRadiusZ = 1.0 / (horizontalRadius * stretchZ);
        double invDepth = 1.0 / (depth * stretchY);
        double topRemovalRadiusNorm = topRemovalRadius / horizontalRadius;

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        for (int y = -depth; y <= REMOVAL_HEIGHT_ABOVE; y++) {
            double normalizedY = Math.abs((double) y) * invDepth;

            if (normalizedY > 1.5) continue;

            double spheroidalFactor = Math.sqrt(Math.max(0, 1.0 - normalizedY * normalizedY));
            double edgeRadius = topRemovalRadiusNorm * spheroidalFactor;

            for (int x = -searchRadius; x <= searchRadius; x++) {
                double normalizedX = (double) x * invHorizontalRadiusX;
                double normalizedXSq = normalizedX * normalizedX;

                if (normalizedXSq > 1.1) continue;

                for (int z = -searchRadius; z <= searchRadius; z++) {
                    double normalizedZ = (double) z * invHorizontalRadiusZ;
                    double horizontalDistanceSq = normalizedXSq + normalizedZ * normalizedZ;

                    if (horizontalDistanceSq > 1.1) continue;

                    double horizontalDistance = Math.sqrt(horizontalDistanceSq);
                    boolean shouldCheck = false;

                    if (y <= 0) {
                        double ellipsoidDistance = Math.sqrt(horizontalDistanceSq + normalizedY * normalizedY);
                        shouldCheck = ellipsoidDistance <= 1.0;
                    } else if (y < REMOVAL_HEIGHT_ABOVE && horizontalDistance <= edgeRadius) {
                        shouldCheck = true;
                    }

                    if (!shouldCheck) continue;

                    BlockPos checkPos = centerPos.offset(x, y, z);
                    BlockExplosionDefense.ExplosionDefenseResult defenseResult =
                            BlockExplosionDefense.calculateExplosionDamage(
                                    level, checkPos, centerPos, horizontalRadius, level.random
                            );

                    if (defenseResult.shouldBreak) {
                        craterBlocksSet.add(checkPos);
                        distributeBlockToRingsWithOverlap(centerPos, checkPos,
                                horizontalRadius, rings);
                    }
                }
            }
        }
    }

    private static void distributeBlockToRingsWithOverlap(BlockPos center, BlockPos pos,
                                                          float maxRadius, List<Set<BlockPos>> rings) {
        double distance = Math.sqrt(
                Math.pow(pos.getX() - center.getX(), 2) +
                        Math.pow(pos.getZ() - center.getZ(), 2)
        );

        double ringWidth = maxRadius / rings.size();
        double idealRingIndex = distance / ringWidth;

        int primaryRing = Math.min(Math.max((int) idealRingIndex, 0), rings.size() - 1);
        rings.get(primaryRing).add(pos);

        double distanceToBoundary = Math.abs(idealRingIndex - primaryRing);
        float overlapThreshold = RING_OVERLAP_PERCENTAGE / 100.0F;

        if (primaryRing < rings.size() - 1 && distanceToBoundary > (1.0 - overlapThreshold)) {
            rings.get(primaryRing + 1).add(pos);
        }

        if (primaryRing > 0 && distanceToBoundary > (1.0 - overlapThreshold)) {
            rings.get(primaryRing - 1).add(pos);
        }
    }

    private static void processAllRingsBatched(ServerLevel level, BlockPos centerPos,
                                               List<Set<BlockPos>> rings, Set<BlockPos> craterBlocksSet,
                                               Block[] selafitBlocks, RandomSource random,
                                               float topRemovalRadius, int craterDepth,
                                               Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrassBlock) {

        List<BlockPos> blockBatch = new ArrayList<>(BLOCK_BATCH_SIZE);

        for (int ringIndex = 0; ringIndex < rings.size(); ringIndex++) {
            Set<BlockPos> currentRing = rings.get(ringIndex);
            if (currentRing.isEmpty()) continue;

            blockBatch.clear();
            for (BlockPos pos : currentRing) {
                blockBatch.add(pos);
                if (blockBatch.size() >= BLOCK_BATCH_SIZE) {
                    removeBlocksBatch(level, blockBatch);
                    blockBatch.clear();
                }
            }
            if (!blockBatch.isEmpty()) {
                removeBlocksBatch(level, blockBatch);
            }

            // Обработка волн
            if (ringIndex == 1) { // Вторая волна: листья убираем, селлафит кладём
                for (BlockPos pos : currentRing) {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BlockTags.LEAVES)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                generateSelafitAtBottom(level, centerPos, currentRing, craterBlocksSet,
                        selafitBlocks, random, craterDepth);
            } else if (ringIndex >= 2) { // Третья и четвёртая волны - обычная логика
                generateSelafitAtBottom(level, centerPos, currentRing, craterBlocksSet,
                        selafitBlocks, random, craterDepth);
            }
        }
    }

    private static void removeBlocksBatch(ServerLevel level, List<BlockPos> batch) {
        for (BlockPos pos : batch) {
            level.removeBlock(pos, false);
        }
    }

    private static void generateSelafitAtBottom(ServerLevel level, BlockPos centerPos, Set<BlockPos> ringBlocks,
                                                Set<BlockPos> craterBlocksSet,
                                                Block[] selafitBlocks, RandomSource random, int craterDepth) {
        int centerY = centerPos.getY();
        int centerX = centerPos.getX();
        int centerZ = centerPos.getZ();

        for (BlockPos pos : ringBlocks) {
            if (!craterBlocksSet.contains(pos.below())) {
                if (!hasValidGroundBelow(level, pos.below()))
                    continue;

                int targetX = pos.getX();
                int targetZ = pos.getZ();
                int bottomY = centerY - craterDepth;

                BlockPos destPos = null;
                for (int y = bottomY; y <= centerY; y++) {
                    BlockPos checkPos = new BlockPos(targetX, y, targetZ);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (!checkState.getFluidState().isEmpty()) {
                        level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }

                    if (checkState.isAir()) {
                        BlockPos belowPos = checkPos.below();
                        for (int depthCheck = 0; depthCheck < 60; depthCheck++) {
                            BlockPos checkBelow = belowPos.below(depthCheck);
                            BlockState belowState = level.getBlockState(checkBelow);
                            if (!belowState.isAir() && belowState.isSolidRender(level, checkBelow)) {
                                destPos = checkPos;
                                break;
                            }
                        }
                        if (destPos != null) break;
                    }
                }
                if (destPos == null) continue;

                double distance = Math.sqrt(Math.pow(targetX - centerX, 2) + Math.pow(targetZ - centerZ, 2));
                double maxDistance = craterDepth * 1.5;
                double ratio = Math.min(distance / maxDistance, 1.0);
                int layers = (int) ((1.0 - ratio) * SELLAFIT_REPLACEMENT_LAYERS);
                if (layers < 1) layers = 1;

                Block selafitBlock = selafitBlocks[random.nextInt(selafitBlocks.length)];

                placeSelafitLayers(level, destPos, selafitBlock, layers, craterBlocksSet);
            }
        }
    }



    private static boolean hasValidGroundBelow(ServerLevel level, BlockPos below) {
        for (int y = -50; y <= 1; y++) {
            BlockPos checkPos = below.above(y);
            BlockState checkState = level.getBlockState(checkPos);
            if (!checkState.isAir() && checkState.isSolidRender(level, checkPos)) {
                return true;
            }
        }
        return false;
    }

    private static void placeSelafitLayers(ServerLevel level, BlockPos basePos,
                                           Block selafitBlock, int layerCount,
                                           Set<BlockPos> craterBlocksSet) {
        for (int layer = 0; layer < layerCount; layer++) {
            BlockPos layerPos = basePos.above(layer);
            BlockState layerState = level.getBlockState(layerPos);
            if (layerState.isAir() || layerState.is(BlockTags.REPLACEABLE)) {
                level.setBlock(layerPos, selafitBlock.defaultBlockState(), 3);
                craterBlocksSet.add(layerPos);
            } else {
                break;
            }
        }
    }

    private static void removeItemsInRadiusBatched(ServerLevel level, BlockPos centerPos, int radius) {
        AABB removalArea = new AABB(
                centerPos.getX() - radius,
                centerPos.getY() - 100,
                centerPos.getZ() - radius,
                centerPos.getX() + radius,
                centerPos.getY() + 100,
                centerPos.getZ() + radius
        );
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, removalArea);
        int discardedCount = 0;
        for (ItemEntity item : items) {
            item.discard();
            discardedCount++;
            if (discardedCount % 100 == 0) Thread.yield();
        }
        System.out.println("[CRATER] Удалено предметов: " + discardedCount);
    }


    private static void applyDamageZonesOptimizedV2(ServerLevel level, BlockPos centerPos,
                                                    Block wasteLogBlock, Block wastePlanksBlock,
                                                    Block burnedGrassBlock, Block[] selafitBlocks,
                                                    RandomSource random) {
        System.out.println("[CRATER] Применение зон повреждения начато!");
        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        int searchRadius = ZONE_4_RADIUS + 20;

        for (int x = centerX - searchRadius; x <= centerX + searchRadius; x++) {
            long dx = x - centerX;
            long dxSq = dx * dx;
            if (dxSq > ZONE_4_RADIUS_SQ) continue;
            for (int z = centerZ - searchRadius; z <= centerZ + searchRadius; z++) {
                long dz = z - centerZ;
                long distanceSq = dxSq + dz * dz;
                if (distanceSq > ZONE_4_RADIUS_SQ) continue;
                for (int y = centerY - 100; y <= centerY + DAMAGE_ZONE_HEIGHT + 60; y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    if (distanceSq <= ZONE_3_RADIUS_SQ) {
                        if (state.is(BlockTags.LEAVES)) {
                            level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                        } else if (
                                state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.SAND) ||
                                        state.is(Blocks.MYCELIUM) || state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) ||
                                        state.is(Blocks.SANDSTONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.GRAVEL)
                        ) {
                            Block selafitBlock = selafitBlocks[random.nextInt(selafitBlocks.length)];
                            level.setBlock(checkPos, selafitBlock.defaultBlockState(), 3);
                        } else if (
                                state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                                        state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                                        state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                        ) {
                            level.removeBlock(checkPos, false);
                        }
                    } else if (distanceSq <= ZONE_4_RADIUS_SQ) {
                        if (state.is(BlockTags.LEAVES)) {
                            if (random.nextFloat() < 0.4F) {
                                level.removeBlock(checkPos, false);
                            } else if (random.nextFloat() < 0.1F) {
                                level.setBlock(checkPos, Blocks.FIRE.defaultBlockState(), 3);
                            }
                        } else if (state.is(Blocks.GRASS_BLOCK)) {
                            level.setBlock(checkPos, burnedGrassBlock.defaultBlockState(), 3);
                        } else if (state.is(BlockTags.LOGS)) {
                            level.setBlock(checkPos, wasteLogBlock.defaultBlockState(), 3);
                        } else if (state.is(BlockTags.PLANKS)) {
                            level.setBlock(checkPos, wastePlanksBlock.defaultBlockState(), 3);
                        } else if (
                                state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                                        state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                                        state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) ||
                                        state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)
                        ) {
                            level.removeBlock(checkPos, false);
                        } else if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS)) {
                            level.removeBlock(checkPos, false);
                        } else if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)) {
                            if (random.nextFloat() < 0.6F) {
                                level.removeBlock(checkPos, false);
                            }
                        }
                    }
                }
            }
        }
        AABB zone3Area = new AABB(centerX - ZONE_3_RADIUS, centerY - DAMAGE_ZONE_HEIGHT,
                centerZ - ZONE_3_RADIUS, centerX + ZONE_3_RADIUS, centerY + DAMAGE_ZONE_HEIGHT,
                centerZ + ZONE_3_RADIUS);
        List<ItemEntity> itemsZone3 = level.getEntitiesOfClass(ItemEntity.class, zone3Area);
        for (ItemEntity item : itemsZone3) {
            item.discard();
        }

        AABB zone4Area = new AABB(centerX - ZONE_4_RADIUS, centerY - DAMAGE_ZONE_HEIGHT,
                centerZ - ZONE_4_RADIUS, centerX + ZONE_4_RADIUS, centerY + DAMAGE_ZONE_HEIGHT,
                centerZ + ZONE_4_RADIUS);
        List<ItemEntity> itemsZone4 = level.getEntitiesOfClass(ItemEntity.class, zone4Area);
        for (ItemEntity item : itemsZone4) {
            item.discard();
        }
        applyKillZoneToEntitiesOptimized(level, centerPos, random);
        System.out.println("[CRATER] ✅ Применение зон повреждения завершено!");
    }


    private static void applyZone4Effects(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.is(BlockTags.LEAVES)) {
            if (random.nextFloat() < 0.4F) {
                level.removeBlock(pos, false);
            } else if (random.nextFloat() < 0.1F) {
                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
            }
        } else if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) ||
                state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) ||
                state.is(Blocks.PODZOL)) {
            level.removeBlock(pos, false);
        } else if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS)) {
            level.removeBlock(pos, false);
        } else if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)) {
            if (random.nextFloat() < 0.6F) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static void applyKillZoneToEntitiesOptimized(ServerLevel level, BlockPos centerPos, RandomSource random) {
        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        AABB zone3Area = new AABB(centerX - ZONE_3_RADIUS, centerY - DAMAGE_ZONE_HEIGHT,
                centerZ - ZONE_3_RADIUS, centerX + ZONE_3_RADIUS, centerY + DAMAGE_ZONE_HEIGHT,
                centerZ + ZONE_3_RADIUS);

        AABB zone4Area = new AABB(centerX - ZONE_4_RADIUS, centerY - DAMAGE_ZONE_HEIGHT,
                centerZ - ZONE_4_RADIUS, centerX + ZONE_4_RADIUS, centerY + DAMAGE_ZONE_HEIGHT,
                centerZ + ZONE_4_RADIUS);

        List<LivingEntity> entitiesZone3 = level.getEntitiesOfClass(LivingEntity.class, zone3Area);
        for (LivingEntity entity : entitiesZone3) {
            entity.hurt(level.damageSources().generic(), ZONE_3_DAMAGE);
            entity.setSecondsOnFire((int) FIRE_DURATION / 20);
        }

        List<LivingEntity> entitiesZone4 = level.getEntitiesOfClass(LivingEntity.class, zone4Area);
        for (LivingEntity entity : entitiesZone4) {
            if (!entitiesZone3.contains(entity)) {
                entity.hurt(level.damageSources().generic(), ZONE_4_DAMAGE);
                entity.setSecondsOnFire((int) FIRE_DURATION / 20);
            }
        }
    }
}
