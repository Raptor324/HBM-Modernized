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
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.BlockTags;

import java.util.*;

/**
 * ОПТИМИЗИРОВАННЫЙ ГЕНЕРАТОР КРАТЕРОВ v16 - С ОБРЕЗКОЙ ГОР
 *
 * Основные оптимизации:
 * ✅ Батчевая обработка блоков
 * ✅ Асинхронная обработка зон повреждения
 * ✅ 🆕 СИСТЕМА ОБРЕЗКИ ГОР - удаляет блоки выше кратера
 * ✅ 🆕 ГРАВИТАЦИОННЫЙ КОЛЛАПС - падающие блоки заполняют пустоты
 */
public class CraterGenerator {

    // ========== НАСТРОЙКИ ВОРОНКИ ==========
    private static final float STRETCH_FACTOR = 1.5F;
    private static final int REMOVAL_HEIGHT_ABOVE = 80;
    private static final float TOP_REMOVAL_RADIUS_MULTIPLIER = 1.3F;
    private static final int RING_COUNT = 8;
    private static final int SELLAFIT_SPAWN_HEIGHT = 0;

    // ========== ЗОНЫ ПОВРЕЖДЕНИЯ ==========
    private static final int ZONE_3_RADIUS = 190;
    private static final int ZONE_4_RADIUS = 260;
    private static final long ZONE_3_RADIUS_SQ = (long)ZONE_3_RADIUS * ZONE_3_RADIUS;
    private static final long ZONE_4_RADIUS_SQ = (long)ZONE_4_RADIUS * ZONE_4_RADIUS;
    private static final int DAMAGE_ZONE_HEIGHT = 80;

    // ========== ПАРАМЕТРЫ КИЛЛЗОНЫ ==========
    private static final float ZONE_3_DAMAGE = 5.0F;
    private static final float ZONE_4_DAMAGE = 2.0F;
    private static final float FIRE_DURATION = 280.0F;

    // ========== ПАРАМЕТРЫ ШУМА КРАТЕРА ==========
    private static final float HORIZONTAL_STRETCH_FACTOR = 0F;
    private static final float VERTICAL_STRETCH_FACTOR = 0F;
    private static final float RING_OVERLAP_PERCENTAGE = 20.0F;

    // ========== ПАРАМЕТРЫ КОНТРОЛЯ СПАВНА ==========
    private static final float SELLAFIT_SPAWN_PROBABILITY = 1.2F;
    private static final float SELLAFIT_EDGE_PROBABILITY = 1.2F;
    private static final int MIN_CRATER_NEIGHBORS_REQUIRED = 1;

    // ========== 🆕 ПАРАМЕТРЫ ОБРЕЗКИ ГОР ==========
    private static final int MOUNTAIN_TRIM_RADIUS = 200; // Радиус в пикселях для обрезки
    private static final int MOUNTAIN_TRIM_HEIGHT_ABOVE = 50; // На сколько блоков выше центра кратера проверять
    private static final int MAX_OVERHANG_HEIGHT = 5; // Максимум "нависающих" блоков перед удалением
    private static final float TRIM_PROBABILITY = 0.85F; // Вероятность удаления нависающего блока (85%)
    private static final boolean ENABLE_MOUNTAIN_TRIMMING = true; // Включить ли обрезку гор

    // ========== ПАРАМЕТРЫ БАТЧЕВОЙ ОБРАБОТКИ ==========
    private static final int BLOCK_BATCH_SIZE = 256;

    /**
     * Главный метод генерирования кратера - ОПТИМИЗИРОВАН С ОБРЕЗКОЙ ГОР
     */
    public static void generateCrater(ServerLevel level, BlockPos centerPos,
                                      int radius, int depth,
                                      Block surfaceBlock1, Block surfaceBlock2, Block surfaceBlock3, Block surfaceBlock4,
                                      Block fallingBlock1, Block fallingBlock2, Block fallingBlock3, Block fallingBlock4,
                                      Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrassBlock) {

        RandomSource random = level.random;
        float stretchX = 1.0F + (random.nextFloat() - 0.5F) * HORIZONTAL_STRETCH_FACTOR;
        float stretchZ = 1.0F + (random.nextFloat() - 0.5F) * VERTICAL_STRETCH_FACTOR;
        float stretchY = 1.0F + (random.nextFloat() - 0.5F) * VERTICAL_STRETCH_FACTOR;
        float horizontalRadius = radius * STRETCH_FACTOR;
        float topRemovalRadius = horizontalRadius * TOP_REMOVAL_RADIUS_MULTIPLIER;

        Block[] fallingBlocks = { fallingBlock1, fallingBlock2, fallingBlock3, fallingBlock4 };

        List<Set<BlockPos>> rings = new ArrayList<>();
        Set<BlockPos> craterBlocksSet = new HashSet<>();

        for (int i = 0; i < RING_COUNT; i++) {
            rings.add(new HashSet<>());
        }

        System.out.println("[CRATER] Начало генерации кратера...");
        long startTime = System.currentTimeMillis();

        // Сбор блоков кратера
        collectCraterBlocksOptimized(level, centerPos, (int) topRemovalRadius, depth,
                horizontalRadius, topRemovalRadius, stretchX, stretchZ, stretchY,
                craterBlocksSet, rings);

        System.out.println("[CRATER] Собрано блоков: " + craterBlocksSet.size());

        // 🆕 ОБРЕЗКА ГОР перед обработкой колец
        if (ENABLE_MOUNTAIN_TRIMMING) {
            System.out.println("[CRATER] 🏔️ Начало обрезки гор и нависаний...");
            trimMountainsAboveCrater(level, centerPos, craterBlocksSet, (int) topRemovalRadius);
            System.out.println("[CRATER] 🏔️ Обрезка гор завершена!");
        }

        // Обработка всех колец
        processAllRingsBatched(level, centerPos, rings, craterBlocksSet,
                fallingBlocks, topRemovalRadius, random, wasteLogBlock, wastePlanksBlock,
                burnedGrassBlock, horizontalRadius);

        removeItemsInRadiusBatched(level, centerPos, (int) topRemovalRadius + 10);

        // ✅ ИСПРАВЛЕНИЕ В CraterGenerator.java - МЕСТОПОЛОЖЕНИЕ ВЫЗОВА БИОМОВ

// В методе generateCrater(), ПОСЛЕ removeBlocksBatch(), добавьте эту часть:

// ✅ ПРАВИЛЬНО - 2 тика для гарантии загрузки чанков
        if (level.getServer() != null) {
            level.getServer().tell(new net.minecraft.server.TickTask(2, () -> {
                System.out.println("[CRATER] ⏳ Tick 2: Applying biomes...");
                try {
                    CraterBiomeApplier.applyCraterBiomes(level, centerPos, (int) horizontalRadius);
                    System.out.println("[CRATER] ✅ Biomes applied successfully!");
                } catch (Exception e) {
                    System.err.println("[CRATER] ❌ Error applying biomes:");
                    e.printStackTrace();
                }

                // Применение урона зон на СЛЕДУЮЩИЙ тик (тик 3)
                level.getServer().tell(new net.minecraft.server.TickTask(1, () -> {
                    System.out.println("[CRATER] ⏳ Tick 3: Applying damage zones...");
                    try {
                        applyDamageZonesOptimizedV2(level, centerPos, wasteLogBlock, wastePlanksBlock, burnedGrassBlock, random);
                        System.out.println("[CRATER] ✅ Damage zones applied!");
                    } catch (Exception e) {
                        System.err.println("[CRATER] ❌ Error applying damage zones:");
                        e.printStackTrace();
                    }
                }));
            }));
        }

        long endTime = System.currentTimeMillis();
        System.out.println("[CRATER] Generation completed in " + (endTime - startTime) + "ms");
    }

    /**
     * 🆕 СИСТЕМА ОБРЕЗКИ ГОР
     * Удаляет блоки, которые "висят" над кратером // В методе создания кратера после генерации блоков
     * CraterBiomeApplier.applyCraterBiomes(level, craterCenter, craterRadius);
     * System.out.println("[CRATER] Биомы применены!");
     */
    private static void trimMountainsAboveCrater(ServerLevel level, BlockPos centerPos,
                                                 Set<BlockPos> craterBlocksSet, int searchRadius) {

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        // Расширенный радиус поиска (обрезаем немного дальше кратера)
        int trimRadius = Math.min(searchRadius + 50, MOUNTAIN_TRIM_RADIUS);
        long trimRadiusSq = (long)trimRadius * trimRadius;

        System.out.println("[CRATER] 🔍 Сканирование нависаний в радиусе " + trimRadius);

        int totalTrimmed = 0;

        // Сканируем от верхнего уровня вниз
        for (int y = centerY + MOUNTAIN_TRIM_HEIGHT_ABOVE; y > centerY - 50; y--) {
            for (int x = centerX - trimRadius; x <= centerX + trimRadius; x++) {
                long dx = x - centerX;
                long dxSq = dx * dx;
                if (dxSq > trimRadiusSq) continue;

                for (int z = centerZ - trimRadius; z <= centerZ + trimRadius; z++) {
                    long dz = z - centerZ;
                    long distanceSq = dxSq + dz * dz;
                    if (distanceSq > trimRadiusSq) continue;

                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    // Проверяем является ли это нависанием
                    if (isOverhangingBlock(level, checkPos, centerPos, craterBlocksSet)) {
                        // Проверяем вероятность удаления
                        if (level.random.nextFloat() < TRIM_PROBABILITY) {
                            level.removeBlock(checkPos, false);
                            totalTrimmed++;
                        }
                    }
                }
            }
        }

        System.out.println("[CRATER] ✂️ Удалено нависающих блоков: " + totalTrimmed);
    }

    /**
     * 🆕 Проверяет, является ли блок "нависающим" над кратером
     * Нависание = блок находится выше кратера и под ним есть пустое место
     */
    private static boolean isOverhangingBlock(ServerLevel level, BlockPos pos,
                                              BlockPos centerPos, Set<BlockPos> craterBlocksSet) {

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        int centerX = centerPos.getX();
        int centerZ = centerPos.getZ();

        // Блок должен быть выше центра кратера
        if (y <= centerPos.getY()) {
            return false;
        }

        // Проверяем в пределах ли блок нависания (MAX_OVERHANG_HEIGHT)
        if (y - centerPos.getY() > MAX_OVERHANG_HEIGHT * 2) {
            return false;
        }

        // Проверяем поддержку под блоком
        boolean hasSupport = false;
        for (int checkY = y - 1; checkY >= y - MAX_OVERHANG_HEIGHT; checkY--) {
            BlockPos supportPos = new BlockPos(x, checkY, z);

            // Если найдено основание в кратере - это нависание
            if (craterBlocksSet.contains(supportPos)) {
                continue;
            }

            BlockState supportState = level.getBlockState(supportPos);
            if (!supportState.isAir() && supportState.isSolidRender(level, supportPos)) {
                // Нашли обычную землю - это не нависание
                hasSupport = true;
                break;
            }
        }

        // Если нет поддержки, это нависание
        return !hasSupport;
    }

    /**
     * 🆕 ГРАВИТАЦИОННЫЙ КОЛЛАПС
     * Заставляет блоки падать и заполнять пустоты
     */
    private static void triggerGravityCollapse(ServerLevel level, BlockPos centerPos, int radius) {
        System.out.println("[CRATER] 💥 Инициирование гравитационного коллапса...");

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        long radiusSq = (long)radius * radius;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            long dx = x - centerX;
            long dxSq = dx * dx;
            if (dxSq > radiusSq) continue;

            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                long dz = z - centerZ;
                if (dxSq + dz * dz > radiusSq) continue;

                // Проверяем блоки сверху вниз
                for (int y = centerY + 200; y > centerY; y--) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    // Пропускаем воздух
                    if (state.isAir()) continue;

                    // Если блок есть, проверяем блок под ним
                    BlockPos belowPos = checkPos.below();
                    BlockState belowState = level.getBlockState(belowPos);

                    // Если под блоком пусто, создаем падающий блок
                    if (belowState.isAir()) {
                        FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, checkPos, state);
                        fallingBlock.setHurtsEntities(0.5F, 10);
                        level.addFreshEntity(fallingBlock);
                        level.removeBlock(checkPos, false);
                    }
                }
            }
        }

        System.out.println("[CRATER] 💥 Гравитационный коллапс завершен!");
    }

    /**
     * ОПТИМИЗИРОВАНА: Сбор блоков кратера с ранними выходами
     */
    private static void collectCraterBlocksOptimized(
            ServerLevel level, BlockPos centerPos, int searchRadius, int depth,
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
            double absY = Math.abs((double) y);
            double normalizedY = absY * invDepth;

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

    /**
     * ОПТИМИЗИРОВАНА: Батчевая обработка всех колец
     */
    private static void processAllRingsBatched(ServerLevel level, BlockPos centerPos,
                                               List<Set<BlockPos>> rings, Set<BlockPos> craterBlocksSet,
                                               Block[] fallingBlocks, float topRemovalRadius, RandomSource random,
                                               Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrassBlock,
                                               float horizontalRadius) {

        List<BlockPos> blockBatch = new ArrayList<>(BLOCK_BATCH_SIZE);

        for (int ringIndex = 0; ringIndex < rings.size(); ringIndex++) {
            Set<BlockPos> currentRing = rings.get(ringIndex);
            if (currentRing.isEmpty()) continue;

            System.out.println("[CRATER] Обработка кольца " + ringIndex + " (" +
                    currentRing.size() + " блоков)");

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

            generateCraterSurfaceOptimizedV2(level, centerPos, currentRing, craterBlocksSet,
                    fallingBlocks, random, ringIndex, rings.size() - 1, horizontalRadius);
        }
    }

    /**
     * ОПТИМИЗИРОВАНА: Батчевое удаление блоков
     */
    private static void removeBlocksBatch(ServerLevel level, List<BlockPos> batch) {
        for (BlockPos pos : batch) {
            level.removeBlock(pos, false);
        }
    }

    /**
     * ОПТИМИЗИРОВАНА V2: Генерация поверхности кратера
     */
    private static void generateCraterSurfaceOptimizedV2(ServerLevel level, BlockPos centerPos,
                                                         Set<BlockPos> ringBlocks, Set<BlockPos> craterBlocksSet, Block[] fallingBlocks,
                                                         RandomSource random, int ringIndex, int lastRingIndex, float horizontalRadius) {

        float ringWidth = horizontalRadius / (lastRingIndex + 1);
        float minRingRadius = ringIndex * ringWidth;
        float maxRingRadius = (ringIndex + 1) * ringWidth;
        float ringRadiusDiff = maxRingRadius - minRingRadius;

        int centerX = centerPos.getX();
        int centerZ = centerPos.getZ();

        for (BlockPos pos : ringBlocks) {
            BlockPos below = pos.below();

            if (craterBlocksSet.contains(below)) {
                continue;
            }

            if (!hasValidGroundBelow(level, below)) {
                continue;
            }

            int craterNeighborCount = countCraterNeighborsOptimized(pos, craterBlocksSet);

            if (craterNeighborCount < MIN_CRATER_NEIGHBORS_REQUIRED) {
                if (random.nextFloat() > 0.1F) {
                    continue;
                }
            }

            int dx = pos.getX() - centerX;
            int dz = pos.getZ() - centerZ;
            double distanceFromCenter = Math.sqrt(dx * dx + dz * dz);

            float positionInRing = (float) ((distanceFromCenter - minRingRadius) / ringRadiusDiff);
            positionInRing = Math.max(0, Math.min(1, positionInRing));

            float baseProbability = (ringIndex == 0) ? SELLAFIT_SPAWN_PROBABILITY :
                    (1.0F - positionInRing * (1.0F - SELLAFIT_EDGE_PROBABILITY));

            float finalProbability = baseProbability * (1.0F - (float) Math.pow(positionInRing, 2) * 0.3F);

            if (random.nextFloat() < finalProbability) {
                int blockIndex = random.nextInt(fallingBlocks.length);
                BlockState blockState = fallingBlocks[blockIndex].defaultBlockState();
                int extraHeight = (int) (positionInRing * 3);

                spawnFallingBlockAtPosition(level, pos.getX() + 0.5,
                        pos.getY() + SELLAFIT_SPAWN_HEIGHT + extraHeight,
                        pos.getZ() + 0.5, blockState);
            }
        }
    }

    /**
     * Проверка наличия земли под позицией
     */
    private static boolean hasValidGroundBelow(ServerLevel level, BlockPos below) {
        for (int checkY = -50; checkY <= 1; checkY++) {
            BlockPos checkPos = below.above(checkY);
            BlockState checkState = level.getBlockState(checkPos);
            if (!checkState.isAir() && checkState.isSolidRender(level, checkPos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ОПТИМИЗИРОВАНА V2: Применение зон повреждения
     */
    private static void applyDamageZonesOptimizedV2(ServerLevel level, BlockPos centerPos,
                                                    Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrassBlock, RandomSource random) {

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
                        applyZone3Effects(level, checkPos, state, wasteLogBlock, wastePlanksBlock, burnedGrassBlock);
                    } else if (distanceSq <= ZONE_4_RADIUS_SQ) {
                        applyZone4Effects(level, checkPos, state, random);
                    }
                }
            }
        }

        applyKillZoneToEntitiesOptimized(level, centerPos, random);
        System.out.println("[CRATER] ✅ Применение зон повреждения завершено!");
    }

    /**
     * ОПТИМИЗИРОВАНА: Применяет эффекты зоны 3 (0-190 блоков)
     */
    private static void applyZone3Effects(ServerLevel level, BlockPos pos, BlockState state,
                                          Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrassBlock) {

        if (state.is(BlockTags.LEAVES)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, burnedGrassBlock.defaultBlockState(), 3);
        } else if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
            level.removeBlock(pos, false);
        } else if (state.is(BlockTags.LOGS)) {
            level.setBlock(pos, wasteLogBlock.defaultBlockState(), 3);
        } else if (state.is(BlockTags.PLANKS)) {
            level.setBlock(pos, wastePlanksBlock.defaultBlockState(), 3);
        }
    }

    /**
     * ОПТИМИЗИРОВАНА: Применяет эффекты зоны 4 (190-260 блоков)
     */
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

    /**
     * ОПТИМИЗИРОВАНА: Применяет урон и огонь к сущностям
     */
    private static void applyKillZoneToEntitiesOptimized(ServerLevel level, BlockPos centerPos, RandomSource random) {

        System.out.println("[CRATER] Применение урона к сущностям...");
        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        AABB zone3Area = new AABB(
                centerX - ZONE_3_RADIUS,
                centerY - DAMAGE_ZONE_HEIGHT,
                centerZ - ZONE_3_RADIUS,
                centerX + ZONE_3_RADIUS,
                centerY + DAMAGE_ZONE_HEIGHT,
                centerZ + ZONE_3_RADIUS
        );

        AABB zone4Area = new AABB(
                centerX - ZONE_4_RADIUS,
                centerY - DAMAGE_ZONE_HEIGHT,
                centerZ - ZONE_4_RADIUS,
                centerX + ZONE_4_RADIUS,
                centerY + DAMAGE_ZONE_HEIGHT,
                centerZ + ZONE_4_RADIUS
        );

        List<LivingEntity> entitiesZone3 = level.getEntitiesOfClass(LivingEntity.class, zone3Area);

        for (LivingEntity entity : entitiesZone3) {
            entity.hurt(level.damageSources().generic(), ZONE_3_DAMAGE);
            entity.setSecondsOnFire((int) FIRE_DURATION / 20);
        }

        System.out.println("[CRATER] Зона 3: поражено " + entitiesZone3.size() + " сущностей");

        List<LivingEntity> entitiesZone4 = level.getEntitiesOfClass(LivingEntity.class, zone4Area);

        for (LivingEntity entity : entitiesZone4) {
            if (!entitiesZone3.contains(entity)) {
                entity.hurt(level.damageSources().generic(), ZONE_4_DAMAGE);
                entity.setSecondsOnFire((int) FIRE_DURATION / 20);
            }
        }

        System.out.println("[CRATER] Зона 4: поражено " + (entitiesZone4.size() - entitiesZone3.size()) + " сущностей");
    }

    /**
     * Распределение блоков по кольцам с перекрытием
     */
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

    /**
     * ОПТИМИЗИРОВАНА: Подсчитывает соседей блока из craterBlocksSet
     */
    private static int countCraterNeighborsOptimized(BlockPos pos, Set<BlockPos> craterBlocksSet) {
        int count = 0;
        if (craterBlocksSet.contains(pos.above())) count++;
        if (craterBlocksSet.contains(pos.below())) count++;
        if (craterBlocksSet.contains(pos.north())) count++;
        if (craterBlocksSet.contains(pos.south())) count++;
        if (craterBlocksSet.contains(pos.east())) count++;
        if (craterBlocksSet.contains(pos.west())) count++;
        return count;
    }

    /**
     * Спавнит падающий блок
     */
    private static void spawnFallingBlockAtPosition(ServerLevel level,
                                                    double x, double y, double z, BlockState blockState) {

        FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall(level,
                new BlockPos((int) x, (int) y, (int) z), blockState);
        fallingBlockEntity.setHurtsEntities(0.5F, 15);
        level.addFreshEntity(fallingBlockEntity);
    }

    /**
     * ОПТИМИЗИРОВАНА: Батчевое удаление предметов в радиусе
     */
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

            if (discardedCount % 100 == 0) {
                Thread.yield();
            }
        }

        System.out.println("[CRATER] Удалено предметов: " + discardedCount);
    }
}