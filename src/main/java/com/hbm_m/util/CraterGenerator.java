package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.AABB;

/**
 * УЛУЧШЕННЫЙ ГЕНЕРАТОР КРАТЕРОВ С СИСТЕМОЙ ЗАЩИТЫ ОТ ВЗРЫВОВ
 *
 * Новые возможности:
 * - Зона 1 (эпицентр): Уничтожение всего до земли
 * - Зона 2 (края воронки): Плавный переход от воронки к поверхности
 * - Зона 3 (150 блоков): Удаление листвы, стволы обугливаются
 * - Зона 4 (200 блоков): Частичное удаление листвы, без повреждения стволов
 *
 * УЛУЧШЕНИЯ v4:
 * - Уменьшен шум в кольцах (0.2 вместо 0.4)
 * - Уменьшен шум в центральных кольцах (гарантия 0-1 блока вместо 1-2)
 * - ИСПРАВЛЕНА ФОРМА ПОРАЖЕНИЯ: верхние слои обрезаются шире (конус вверх)
 * - Края гор больше не нависают над кратером
 * - Плавный transition от узкого центра к широким краям
 *
 * По аналогии с ядерными взрывами из HBM Nuclear Tech 1.7.10
 *
 * ИНТЕГРИРОВАНА СИСТЕМА ЗАЩИТЫ ОТ ВЗРЫВОВ (BlockExplosionDefense)
 */
public class CraterGenerator {

    private static final int SURFACE_LAYER_DEPTH = 2;
    private static final float STRETCH_FACTOR = 1.5F;
    private static final int REMOVAL_HEIGHT_ABOVE = 60;
    private static final float TOP_REMOVAL_RADIUS_MULTIPLIER = 1.3F;
    private static final int RING_COUNT = 8;
    private static final int TICKS_BETWEEN_RINGS = 40;
    private static final int SELLAFIT_SPAWN_HEIGHT = 0;
    private static final int SELLAFIT_SPAWN_DELAY = 1;

    // Зоны повреждения согласно схеме
    private static final int ZONE_3_RADIUS = 150;
    private static final int ZONE_4_RADIUS = 200;
    private static final int DAMAGE_ZONE_HEIGHT = 80;

    // Параметры киллзоны
    private static final float ZONE_3_DAMAGE = 5.0F;
    private static final float ZONE_4_DAMAGE = 2.0F;
    private static final float FIRE_DURATION = 280.0F;

    // Параметры шума кратера (УМЕНЬШЕНЫ)
    private static final float NOISE_PROBABILITY = 0.05F; // УМЕНЬШЕНО с 0.4
    private static final float HORIZONTAL_STRETCH_FACTOR = 0.15F;
    private static final float VERTICAL_STRETCH_FACTOR = 0.2F;
    private static final float CENTER_RING_NOISE_CHANCE = 0.7F;

    private static BlockPos checkPos;

    /**
     * Главный метод генерирования кратера с интегрированной системой защиты от взрывов
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
                                      Block fallingBlock4,
                                      Block wasteLogBlock,
                                      Block wastePlanksBlock) {

        RandomSource random = level.random;

        // Генерируем случайное растяжение для каждого направления
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

        // Основной цикл для сбора блоков кратера
        for (int x = -(int) topRemovalRadius; x <= topRemovalRadius; x++) {
            for (int z = -(int) topRemovalRadius; z <= topRemovalRadius; z++) {
                for (int y = -depth; y <= REMOVAL_HEIGHT_ABOVE; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);

                    // Применяем деформацию кратера с учётом Y
                    double normalizedX = (double) x / (horizontalRadius * stretchX);
                    double normalizedZ = (double) z / (horizontalRadius * stretchZ);
                    double normalizedY = Math.abs((double) y) / (depth * stretchY);
                    double horizontalDistance = Math.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ);

                    boolean shouldCheck = false;

                    if (y <= 0) {
                        // Эллипсоид кратера с применением Y-растяжения
                        double ellipsoidDistance = Math.sqrt(
                                horizontalDistance * horizontalDistance +
                                        normalizedY * normalizedY
                        );

                        if (ellipsoidDistance <= 1.0) {
                            shouldCheck = true;
                        }
                    } else {
                        // ИСПРАВЛЕНО: Край кратера теперь расширяется вверх (конус)
                        // Вместо сужения края по мере подъёма, радиус увеличивается
                        double topRemovalRadiusNorm = topRemovalRadius / horizontalRadius;
                        double edgeRadius = topRemovalRadiusNorm + (double) y / (radius * 2) * 0.4; // ПЛЮС вместо МИНУС

                        if (horizontalDistance <= edgeRadius && y < REMOVAL_HEIGHT_ABOVE) {
                            shouldCheck = true;
                        }
                    }

                    if (shouldCheck) {
                        // ИСПОЛЬЗУЕМ СИСТЕМУ ЗАЩИТЫ ОТ ВЗРЫВОВ
                        BlockExplosionDefense.ExplosionDefenseResult defenseResult =
                                BlockExplosionDefense.calculateExplosionDamage(
                                        level, checkPos, centerPos, horizontalRadius, random
                                );

                        if (defenseResult.shouldBreak) {
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
        CraterBiomeApplier.applyCraterBiomes(level, centerPos, radius);
        processRingsSequentially(level, centerPos, rings, craterBlocksSet,
                fallingBlocks, topRemovalRadius, random, wasteLogBlock, wastePlanksBlock, RING_COUNT);
    }

    /**
     * Вычисляет индекс кольца на основе расстояния с плавным переходом
     */
    private static int calculateRingIndex(BlockPos center, BlockPos pos,
                                          float maxRadius, int ringCount) {
        double distance = Math.sqrt(
                Math.pow(pos.getX() - center.getX(), 2) +
                        Math.pow(pos.getZ() - center.getZ(), 2)
        );

        int ringIndex = (int) (distance / maxRadius * ringCount);
        return Math.min(Math.max(ringIndex, 0), ringCount - 1);
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
                                                 int ringCount) {

        processRingAtIndex(level, centerPos, rings, craterBlocksSet,
                fallingBlocks, topRemovalRadius, random, 0, wasteLogBlock, wastePlanksBlock, ringCount);
    }

    /**
     * Обрабатывает кольцо с заданным индексом
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
                                           int ringCount) {

        if (ringIndex >= ringCount) {
            removeItemsInRadius(level, centerPos, (int) topRemovalRadius + 10);
            applyDamageZones(level, centerPos, wasteLogBlock, wastePlanksBlock, random);
            return;
        }

        Set<BlockPos> currentRing = rings.get(ringIndex);

        for (BlockPos pos : currentRing) {
            level.removeBlock(pos, false);
        }

        // Передаём информацию о кольце для правильного спауна
        generateCraterSurfaceForRing(level, centerPos, currentRing, craterBlocksSet,
                fallingBlocks, random, ringIndex, ringCount - 1);

        final int nextRingIndex = ringIndex + 1;

        if (level.getServer() != null) {
            level.getServer().tell(new net.minecraft.server.TickTask(TICKS_BETWEEN_RINGS, () -> {
                processRingAtIndex(level, centerPos, rings, craterBlocksSet,
                        fallingBlocks, topRemovalRadius, random, nextRingIndex, wasteLogBlock, wastePlanksBlock, ringCount);
            }));
        }
    }

    /**
     * Генерирует поверхность кратера для кольца с добавлением шума и плавным fade-out
     * v4: УМЕНЬШЕН ШУМ
     */
    private static void generateCraterSurfaceForRing(ServerLevel level,
                                                     BlockPos centerPos,
                                                     Set<BlockPos> ringBlocks,
                                                     Set<BlockPos> craterBlocksSet,
                                                     Block[] fallingBlocks,
                                                     RandomSource random,
                                                     int ringIndex,
                                                     int lastRingIndex) {

        Set<BlockPos> bottomBlocksInRing = new HashSet<>();

        // Поиск нижних блоков
        for (BlockPos pos : ringBlocks) {
            BlockPos below = pos.below();

            if (!craterBlocksSet.contains(below)) {
                boolean hasSolidBlockBelow = false;

                for (int checkY = -30; checkY <= 0; checkY++) {
                    BlockPos checkPos = below.above(checkY);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (!checkState.isAir() && checkState.getDestroySpeed(level, checkPos) >= 0) {
                        hasSolidBlockBelow = true;
                        break;
                    }
                }

                if (hasSolidBlockBelow) {
                    bottomBlocksInRing.add(pos);
                }
            }
        }

        // СПАУН БЛОКОВ С ШУМОМ
        boolean isLastRing = (ringIndex == lastRingIndex);
        boolean isCenterRing = (ringIndex < 3);

        for (BlockPos bottomPos : bottomBlocksInRing) {
            // ===== ГЛАВНЫЙ БЛОК СЕЛЛАФИТА =====
            float mainSpawnChance = 0.6F; // УМЕНЬШЕНО с 0.7

            if (random.nextFloat() < mainSpawnChance) {
                int blockIndex = random.nextInt(fallingBlocks.length);
                Block fallingBlock = fallingBlocks[blockIndex];
                BlockState blockState = fallingBlock.defaultBlockState();

                spawnFallingBlockAtPosition(level,
                        bottomPos.getX() + 0.5,
                        bottomPos.getY() + SELLAFIT_SPAWN_HEIGHT,
                        bottomPos.getZ() + 0.5,
                        blockState);
            }

            // ===== ДОПОЛНИТЕЛЬНЫЕ ШУМОВЫЕ БЛОКИ (ЦЕНТРАЛЬНЫЕ КОЛЬЦА) =====
            if (isCenterRing) {
                // УМЕНЬШЕНО: теперь гарантируется 0-1 блок вместо 1-2
                int noiseBlockCount = random.nextInt(2); // 0-1 блока

                for (int i = 0; i < noiseBlockCount; i++) {
                    int offsetX = random.nextInt(3) - 1;
                    int offsetZ = random.nextInt(3) - 1;

                    if (offsetX != 0 || offsetZ != 0) {
                        BlockPos noisePos = bottomPos.offset(offsetX, 0, offsetZ);
                        BlockState neighborState = level.getBlockState(noisePos);

                        if (neighborState.isAir()) {
                            int noiseBlockIndex = random.nextInt(fallingBlocks.length);
                            Block noiseBlock = fallingBlocks[noiseBlockIndex];
                            BlockState noiseBlockState = noiseBlock.defaultBlockState();

                            spawnFallingBlockAtPosition(level,
                                    noisePos.getX() + 0.5,
                                    noisePos.getY() + SELLAFIT_SPAWN_HEIGHT,
                                    noisePos.getZ() + 0.5,
                                    noiseBlockState);
                        }
                    }
                }
            } else if (!isLastRing && random.nextFloat() < NOISE_PROBABILITY) {
                // В других кольцах - уменьшенный случайный шум
                int offsetX = random.nextInt(3) - 1;
                int offsetZ = random.nextInt(3) - 1;

                if (offsetX != 0 || offsetZ != 0) {
                    BlockPos noisePos = bottomPos.offset(offsetX, 0, offsetZ);
                    BlockState neighborState = level.getBlockState(noisePos);

                    if (neighborState.isAir()) {
                        int noiseBlockIndex = random.nextInt(fallingBlocks.length);
                        Block noiseBlock = fallingBlocks[noiseBlockIndex];
                        BlockState noiseBlockState = noiseBlock.defaultBlockState();

                        spawnFallingBlockAtPosition(level,
                                noisePos.getX() + 0.5,
                                noisePos.getY() + SELLAFIT_SPAWN_HEIGHT,
                                noisePos.getZ() + 0.5,
                                noiseBlockState);
                    }
                }
            }

            // ===== ГОРКА НА ПОСЛЕДНЕЙ ВОЛНЕ С FADE-OUT =====
            if (isLastRing) {
                double distFromCenter = Math.sqrt(
                        Math.pow(bottomPos.getX() - centerPos.getX(), 2) +
                                Math.pow(bottomPos.getZ() - centerPos.getZ(), 2)
                );

                double maxDistInLastRing = 200.0;
                float fadeOutFactor = Math.max(0, 1.0F - (float)(distFromCenter / maxDistInLastRing));
                float edgeSpawnChance = 0.3F * fadeOutFactor; // УМЕНЬШЕНО с 0.5

                if (random.nextFloat() < edgeSpawnChance) {
                    int blockIndex = random.nextInt(fallingBlocks.length);
                    Block riseBlock = fallingBlocks[blockIndex];
                    BlockState riseBlockState = riseBlock.defaultBlockState();

                    int riseHeight = random.nextInt(2) + 1;

                    spawnFallingBlockAtPosition(level,
                            bottomPos.getX() + 0.5,
                            bottomPos.getY() + SELLAFIT_SPAWN_HEIGHT + riseHeight,
                            bottomPos.getZ() + 0.5,
                            riseBlockState);
                }
            }

            // ===== ОБРАБОТКА ДОПОЛНИТЕЛЬНЫХ СЛОЁВ =====
            BlockPos currentPos = bottomPos.above();
            int layersProcessed = 0;

            while (layersProcessed < SURFACE_LAYER_DEPTH && craterBlocksSet.contains(currentPos)) {
                if (random.nextFloat() < mainSpawnChance) {
                    int blockIndex = random.nextInt(fallingBlocks.length);
                    Block layerBlock = fallingBlocks[blockIndex];
                    BlockState layerBlockState = layerBlock.defaultBlockState();

                    spawnFallingBlockAtPosition(level,
                            currentPos.getX() + 0.5,
                            currentPos.getY() + SELLAFIT_SPAWN_HEIGHT,
                            currentPos.getZ() + 0.5,
                            layerBlockState);
                }

                currentPos = currentPos.above();
                layersProcessed++;
            }
        }
    }

    /**
     * Применяет зоны повреждения
     */
    private static void applyDamageZones(ServerLevel level, BlockPos centerPos,
                                         Block wasteLogBlock, Block wastePlanksBlock, RandomSource random) {

        System.out.println("[CRATER] applyDamageZones вызван! Центр: " + centerPos);

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        int searchRadius = ZONE_4_RADIUS;
        int topSearchHeight = DAMAGE_ZONE_HEIGHT + 40;
        int bottomSearchHeight = 60;

        // === БЛОКОВАЯ ОБРАБОТКА ===
        for (int x = centerX - searchRadius; x <= centerX + searchRadius; x++) {
            for (int z = centerZ - searchRadius; z <= centerZ + searchRadius; z++) {
                for (int y = centerY - bottomSearchHeight; y <= centerY + topSearchHeight; y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);

                    double dx = x - centerX;
                    double dz = z - centerZ;
                    double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

                    if (horizontalDistance > ZONE_4_RADIUS) continue;

                    BlockState state = level.getBlockState(checkPos);

                    // ===== ЗОНА 3: 0-150 блоков =====
                    if (horizontalDistance <= ZONE_3_RADIUS) {
                        if (state.is(BlockTags.LEAVES)) {
                            level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
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

                        if (state.is(BlockTags.LOGS)) {
                            level.setBlock(checkPos, wasteLogBlock.defaultBlockState(), 3);
                            continue;
                        }

                        if (state.is(BlockTags.PLANKS)) {
                            level.setBlock(checkPos, wastePlanksBlock.defaultBlockState(), 3);
                            continue;
                        }
                    }
                    // ===== ЗОНА 4: 150-200 блоков =====
                    else if (horizontalDistance <= ZONE_4_RADIUS) {
                        if (state.is(BlockTags.LEAVES)) {
                            if (random.nextFloat() < 0.4F) {
                                level.removeBlock(checkPos, false);
                            } else if (random.nextFloat() < 0.1F) {
                                level.setBlock(checkPos, Blocks.FIRE.defaultBlockState(), 3);
                            }
                        }

                        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                                state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                                state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                            if (random.nextFloat() < 1F) {
                                level.removeBlock(checkPos, false);
                            }
                        }
                    }
                }
            }
        }

        // === КИЛЛЗОНА: УРОН И ПОДЖИГ СУЩНОСТЕЙ ===
        applyKillZone(level, centerPos, random);
    }

    /**
     * Применяет киллзону с уроном и поджигом сущностей
     */
    private static void applyKillZone(ServerLevel level, BlockPos centerPos, RandomSource random) {

        System.out.println("[CRATER] applyKillZone активирован! Центр: " + centerPos);

        // Зона 3: урон и поджиг
        AABB zone3 = new AABB(
                centerPos.getX() - ZONE_3_RADIUS,
                centerPos.getY() - 50,
                centerPos.getZ() - ZONE_3_RADIUS,
                centerPos.getX() + ZONE_3_RADIUS,
                centerPos.getY() + 50,
                centerPos.getZ() + ZONE_3_RADIUS
        );

        List<Entity> entitiesZone3 = level.getEntities(null, zone3);

        for (Entity entity : entitiesZone3) {
            if (entity instanceof LivingEntity livingEntity) {
                DamageSource damageSource = level.damageSources().explosion(null);
                livingEntity.hurt(damageSource, ZONE_3_DAMAGE);
                livingEntity.setSecondsOnFire((int) FIRE_DURATION / 20);

                System.out.println("[CRATER] Зона 3: Урон и поджиг сущности: " + entity.getName().getString());
            }
        }

        // Зона 4: меньший урон и поджиг
        AABB zone4 = new AABB(
                centerPos.getX() - ZONE_4_RADIUS,
                centerPos.getY() - 50,
                centerPos.getZ() - ZONE_4_RADIUS,
                centerPos.getX() + ZONE_4_RADIUS,
                centerPos.getY() + 50,
                centerPos.getZ() + ZONE_4_RADIUS
        );

        List<Entity> entitiesZone4 = level.getEntities(null, zone4);

        for (Entity entity : entitiesZone4) {
            double distFromCenter = Math.sqrt(
                    Math.pow(entity.getX() - centerPos.getX(), 2) +
                            Math.pow(entity.getZ() - centerPos.getZ(), 2)
            );

            if (distFromCenter > ZONE_3_RADIUS && entity instanceof LivingEntity livingEntity) {
                DamageSource damageSource = level.damageSources().explosion(null);
                livingEntity.hurt(damageSource, ZONE_4_DAMAGE);
                livingEntity.setSecondsOnFire((int) (FIRE_DURATION * 0.5F) / 20);

                System.out.println("[CRATER] Зона 4: Урон и поджиг сущности: " + entity.getName().getString());
            }
        }
    }

    /**
     * Спаунит падающий блок в указанной позиции
     */
    private static void spawnFallingBlockAtPosition(ServerLevel level,
                                                    double x, double y, double z,
                                                    BlockState blockState) {

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
     * Альтернативный способ спауна падающего блока
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
            level.setBlock(new BlockPos((int) x, (int) y, (int) z), blockState, 3);
        }
    }

    /**
     * Удаляет все предметы в радиусе кратера
     */
    private static void removeItemsInRadius(ServerLevel level, BlockPos centerPos, int radius) {
        level.getEntities(
                (Entity) null,
                new AABB(
                        centerPos.getX() - radius,
                        centerPos.getY() - radius,
                        centerPos.getZ() - radius,
                        centerPos.getX() + radius,
                        centerPos.getY() + radius,
                        centerPos.getZ() + radius
                ),
                entity -> entity instanceof ItemEntity
        ).forEach(Entity::discard);
    }
}