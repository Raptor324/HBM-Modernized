package com.hbm_m.util;

import com.hbm_m.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.BlockTags;

import java.util.*;

/**
 * ✅ Crater Generator v12.0 - With Proper Biome Integration
 *
 * ✅ Новое в этой версии:
 * - Полная интеграция с CraterBiomeHelper
 * - Биомы применяются после генерации кратера
 * - Правильная синхронизация зон поражения с биомами
 * - Улучшенное логирование процесса
 */
public class CraterGenerator {

    private static final int RING_COUNT = 8;
    private static final int BLOCK_BATCH_SIZE = 256;
    private static final int TOTAL_RAYS = 32400;
    private static final int RAYS_PER_TICK = 150;
    private static final double MAX_PENETRATION = 2000.0;
    private static final double MIN_PENETRATION = 500.0;
    private static final double MAX_RAY_DISTANCE = 100.0;
    private static final int RAY_THICKNESS = 2;

    // Динамические зоны поражения
    private static final float ZONE_3_RADIUS_MULTIPLIER = 0.8F; // 0-60%
    private static final float ZONE_4_RADIUS_MULTIPLIER = 1.6F; // 60-100%
    private static final int DAMAGE_ZONE_HEIGHT = 80;
    private static final float ZONE_3_DAMAGE = 500.0F;
    private static final float ZONE_4_DAMAGE = 200.0F;
    private static final float FIRE_DURATION = 380.0F;

    // Эффект горения
    private static final boolean APPLY_FIRE_EFFECT_ZONE_3 = true;
    private static final float FIRE_SPREAD_CHANCE = 0.15F;

    // Отслеживание крайних точек лучей
    private static class RayTerminationData {
        double maxDistance = 0;
        Set<BlockPos> terminationPoints = Collections.synchronizedSet(new HashSet<>());
    }

    // ✅ ИСПРАВЛЕННЫЙ ФРАГМЕНТ КОДА ДЛЯ CraterGenerator.java
// Замени этот метод в своем CraterGenerator

    /**
     * ✅ ИСПРАВЛЕННЫЙ МЕТОД - Правильная синхронизация трех шагов генерации
     */
    public static void generateCrater(
            ServerLevel level,
            BlockPos centerPos,
            int radius,
            int depth,
            Block sellafit1,
            Block sellafit2,
            Block sellafit3,
            Block sellafit4,
            Block wasteLogBlock,
            Block wastePlanksBlock,
            Block burnedGrassBlock) {

        RandomSource random = level.random;
        Block[] selafitBlocks = {sellafit1, sellafit2, sellafit3, sellafit4};
        Set<BlockPos> craterBlocksSet = Collections.synchronizedSet(new HashSet<>());
        List<Set<BlockPos>> rings = new ArrayList<>();
        RayTerminationData terminationData = new RayTerminationData();

        for (int i = 0; i < RING_COUNT; i++) {
            rings.add(Collections.synchronizedSet(new HashSet<>()));
        }

        long startTime = System.currentTimeMillis();

        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║ [CRATER_GENERATOR] 🚀 START: Generating crater...             ║");
        System.out.println("║ Center: " + String.format("%-50s║", centerPos));
        System.out.println("║ Radius: " + String.format("%-50s║", radius + " blocks"));
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        // ✅ Шаг 0: Асинхронное собирание лучей (основная генерация)
        collectSphericRaysAsync(level, centerPos, craterBlocksSet, rings, terminationData, () -> {

            // ✅ Шаг 1: ЗАВЕРШАЕМ КРАТЕР (блоки)
            System.out.println("\n[CRATER_GENERATOR] 📍 Step 1: Finalizing crater structure...");
            finalizeCrater(level, centerPos, rings, craterBlocksSet,
                    selafitBlocks, random, depth,
                    wasteLogBlock, wastePlanksBlock, burnedGrassBlock, startTime);

            // ✅ Шаг 2: Применяем динамические ЗОНЫ ПОРАЖЕНИЯ (блоки + урон)
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.tell(new TickTask(5, () -> {
                    System.out.println("\n[CRATER_GENERATOR] 🎯 Step 2: Applying damage zones and effects...");

                    double zone3Radius = Math.max(terminationData.maxDistance * ZONE_3_RADIUS_MULTIPLIER, 50);
                    double zone4Radius = Math.max(terminationData.maxDistance * ZONE_4_RADIUS_MULTIPLIER, 80);

                    System.out.println("[CRATER_GENERATOR] Zone 3 (INNER): " + (int)zone3Radius + "m");
                    System.out.println("[CRATER_GENERATOR] Zone 4 (OUTER): " + (int)zone4Radius + "m");

                    // Применяем зоны поражения
                    applyDynamicDamageZones(level, centerPos, terminationData,
                            wasteLogBlock, wastePlanksBlock, burnedGrassBlock,
                            selafitBlocks, random);

                    // ✅ Шаг 3: Применяем БИОМЫ к зонам (САМЫЙ ВАЖНЫЙ)
                    // Задержка в 10 тиков для гарантии обновления зон
                    server.tell(new TickTask(10, () -> {
                        System.out.println("\n[CRATER_GENERATOR] 🌍 Step 3: Applying crater biomes to zones...");
                        System.out.println("[CRATER_GENERATOR] Zone 3 Radius: " + zone3Radius);
                        System.out.println("[CRATER_GENERATOR] Zone 4 Radius: " + zone4Radius);

                        // ✅ КЛЮЧЕВОЙ МОМЕНТ: Применяем биомы асинхронно
                        // НО gарантируем что зоны уже созданы
                        CraterBiomeHelper.applyBiomesAsync(level, centerPos, zone3Radius, zone4Radius);

                        System.out.println("\n[CRATER_GENERATOR] ✅ All steps complete!");
                        System.out.println("[CRATER_GENERATOR] 💾 Chunks will be marked as modified for saving.\n");
                    }));
                }));
            }
        });
    }

    private static double calculatePenetrationFromAngle(double verticalAngleDegrees) {
        double absAngle = Math.abs(verticalAngleDegrees);
        double cosAngle = Math.cos(Math.toRadians(absAngle));
        return MIN_PENETRATION + (MAX_PENETRATION - MIN_PENETRATION) * cosAngle;
    }

    private static void collectSphericRaysAsync(
            ServerLevel level,
            BlockPos centerPos,
            Set<BlockPos> craterBlocksSet,
            List<Set<BlockPos>> rings,
            RayTerminationData terminationData,
            Runnable onComplete) {

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            onComplete.run();
            return;
        }

        int[] rayIndex = new int[1];
        rayIndex[0] = 0;
        processRaysBatchSpheric(level, centerPos, craterBlocksSet, rings, terminationData,
                rayIndex, server, onComplete);
    }

    private static void processRaysBatchSpheric(
            ServerLevel level,
            BlockPos centerPos,
            Set<BlockPos> craterBlocksSet,
            List<Set<BlockPos>> rings,
            RayTerminationData terminationData,
            int[] currentRayIndex,
            MinecraftServer server,
            Runnable onComplete) {

        int cx = centerPos.getX();
        int cy = centerPos.getY();
        int cz = centerPos.getZ();
        int raysToProcess = Math.min(RAYS_PER_TICK, TOTAL_RAYS - currentRayIndex[0]);
        int processed = 0;

        while (processed < raysToProcess && currentRayIndex[0] < TOTAL_RAYS) {
            int rayIndex = currentRayIndex[0];

            double phi = Math.PI * (3.0 - Math.sqrt(5.0));
            double theta = Math.acos(1.0 - (2.0 * rayIndex) / TOTAL_RAYS);
            double psi = phi * rayIndex;

            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);
            double cosPsi = Math.cos(psi);
            double sinPsi = Math.sin(psi);

            double dirX = sinTheta * cosPsi;
            double dirY = cosTheta;
            double dirZ = sinTheta * sinPsi;

            double elevationDegrees = Math.toDegrees(Math.asin(dirY));
            double penetration = calculatePenetrationFromAngle(elevationDegrees);

            traceRay(level, cx, cy, cz, dirX, dirY, dirZ, penetration,
                    centerPos, craterBlocksSet, rings, terminationData);

            currentRayIndex[0]++;
            processed++;
        }

        if (currentRayIndex[0] < TOTAL_RAYS) {
            server.tell(new TickTask(1, () ->
                    processRaysBatchSpheric(level, centerPos, craterBlocksSet, rings, terminationData,
                            currentRayIndex, server, onComplete)
            ));
        } else {
            onComplete.run();
        }
    }

    private static void traceRay(
            ServerLevel level,
            int cx, int cy, int cz,
            double dirX, double dirY, double dirZ,
            double basePenetration,
            BlockPos centerPos,
            Set<BlockPos> craterBlocksSet,
            List<Set<BlockPos>> rings,
            RayTerminationData terminationData) {

        double penetration = basePenetration;
        int maxSteps = (int) MAX_RAY_DISTANCE;
        BlockPos lastBlockPos = centerPos;

        double perpX1, perpY1, perpZ1;
        double perpX2, perpY2, perpZ2;

        if (Math.abs(dirX) < 0.9) {
            perpX1 = 0;
            perpY1 = dirZ;
            perpZ1 = -dirY;
        } else {
            perpX1 = dirY;
            perpY1 = -dirX;
            perpZ1 = 0;
        }

        double len1 = Math.sqrt(perpX1 * perpX1 + perpY1 * perpY1 + perpZ1 * perpZ1);
        if (len1 > 0) {
            perpX1 /= len1;
            perpY1 /= len1;
            perpZ1 /= len1;
        }

        perpX2 = dirY * perpZ1 - dirZ * perpY1;
        perpY2 = dirZ * perpX1 - dirX * perpZ1;
        perpZ2 = dirX * perpY1 - dirY * perpX1;

        double len2 = Math.sqrt(perpX2 * perpX2 + perpY2 * perpY2 + perpZ2 * perpZ2);
        if (len2 > 0) {
            perpX2 /= len2;
            perpY2 /= len2;
            perpZ2 /= len2;
        }

        for (int step = 1; step <= maxSteps && penetration > 0; step++) {
            int baseX = cx + (int) Math.round(dirX * step);
            int baseY = cy + (int) Math.round(dirY * step);
            int baseZ = cz + (int) Math.round(dirZ * step);

            boolean blockedAtStep = false;

            for (int thickness = 0; thickness <= RAY_THICKNESS; thickness++) {
                if (thickness == 0) {
                    double defenseCenter = processBlockInRay(level, baseX, baseY, baseZ,
                            centerPos, craterBlocksSet, rings);

                    if (defenseCenter > 0) {
                        lastBlockPos = new BlockPos(baseX, baseY, baseZ);
                    }

                    penetration -= defenseCenter;
                    if (defenseCenter >= 10_000) {
                        blockedAtStep = true;
                        break;
                    }

                } else {
                    int[] offsets = {thickness, -thickness};
                    for (int offset : offsets) {
                        int x1 = baseX + (int) Math.round(perpX1 * offset);
                        int y1 = baseY + (int) Math.round(perpY1 * offset);
                        int z1 = baseZ + (int) Math.round(perpZ1 * offset);

                        double defense1 = processBlockInRay(level, x1, y1, z1,
                                centerPos, craterBlocksSet, rings);

                        penetration -= defense1;
                        if (defense1 >= 10_000) {
                            blockedAtStep = true;
                        }

                        int x2 = baseX + (int) Math.round(perpX2 * offset);
                        int y2 = baseY + (int) Math.round(perpY2 * offset);
                        int z2 = baseZ + (int) Math.round(perpZ2 * offset);

                        double defense2 = processBlockInRay(level, x2, y2, z2,
                                centerPos, craterBlocksSet, rings);

                        penetration -= defense2;
                        if (defense2 >= 10_000) {
                            blockedAtStep = true;
                        }
                    }
                }

                if (blockedAtStep) {
                    break;
                }
            }

            // Отслеживаем крайнюю точку луча
            if (lastBlockPos != centerPos) {
                terminationData.terminationPoints.add(lastBlockPos);
                double distance = Math.sqrt(
                        Math.pow(lastBlockPos.getX() - centerPos.getX(), 2) +
                                Math.pow(lastBlockPos.getZ() - centerPos.getZ(), 2)
                );
                if (distance > terminationData.maxDistance) {
                    terminationData.maxDistance = distance;
                }
            }
        }
    }

    private static double processBlockInRay(
            ServerLevel level,
            int x, int y, int z,
            BlockPos centerPos,
            Set<BlockPos> craterBlocksSet,
            List<Set<BlockPos>> rings) {

        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            return 0;
        }

        if (state.is(Blocks.BEDROCK)) {
            return 10_000;
        }

        float defense = BlockExplosionDefense.getBlockDefenseValue(level, pos, state);

        if (defense < 10_000 && !craterBlocksSet.contains(pos)) {
            craterBlocksSet.add(pos);
            distributeBlockToRings(centerPos, pos, (int) MAX_RAY_DISTANCE, rings);
        }

        return defense;
    }

    private static void distributeBlockToRings(
            BlockPos center, BlockPos pos, int maxRadius, List<Set<BlockPos>> rings) {

        double distSq = center.distSqr(pos);
        double maxDistSq = (double) maxRadius * maxRadius;
        double ratio = Math.min(distSq / maxDistSq, 1.0);
        int ringIndex = (int) (ratio * RING_COUNT);

        if (ringIndex < 0) ringIndex = 0;
        if (ringIndex >= RING_COUNT) ringIndex = RING_COUNT - 1;

        rings.get(ringIndex).add(pos);
    }

    private static void finalizeCrater(
            ServerLevel level,
            BlockPos centerPos,
            List<Set<BlockPos>> rings,
            Set<BlockPos> allCraterBlocks,
            Block[] selafitBlocks,
            RandomSource random,
            int depth,
            Block wasteLog,
            Block wastePlanks,
            Block burnedGrass,
            long startTime) {

        processAllRingsBatched(level, centerPos, rings, allCraterBlocks,
                selafitBlocks, random, depth, wasteLog, wastePlanks, burnedGrass);

        removeItemsInRadiusBatched(level, centerPos, (int) MAX_RAY_DISTANCE + 20);

        long endTime = System.currentTimeMillis();
        System.out.println("[CRATER] Generation complete! Time: " +
                (endTime - startTime) + "ms | Total Rays: " + TOTAL_RAYS);
    }

    private static void processAllRingsBatched(
            ServerLevel level,
            BlockPos centerPos,
            List<Set<BlockPos>> rings,
            Set<BlockPos> allCraterBlocks,
            Block[] selafitBlocks,
            RandomSource random,
            int depth,
            Block wasteLog,
            Block wastePlanks,
            Block burnedGrass) {

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        List<BlockPos> allBlocksList = new ArrayList<>(allCraterBlocks);
        int totalBlocks = allBlocksList.size();
        int totalBatches = (int) Math.ceil((double) totalBlocks / BLOCK_BATCH_SIZE);

        for (int i = 0; i < totalBatches; i++) {
            final int batchIndex = i;
            server.tell(new TickTask(i + 1, () -> {
                int start = batchIndex * BLOCK_BATCH_SIZE;
                int end = Math.min(start + BLOCK_BATCH_SIZE, totalBlocks);

                for (int j = start; j < end; j++) {
                    BlockPos pos = allBlocksList.get(j);
                    boolean isBorder = false;

                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) {
                                    BlockPos neighbor = pos.offset(dx, dy, dz);
                                    if (!allCraterBlocks.contains(neighbor) &&
                                            !level.isEmptyBlock(neighbor)) {
                                        isBorder = true;
                                        break;
                                    }
                                }
                                if (isBorder) break;
                            }
                            if (isBorder) break;
                        }
                        if (isBorder) break;
                    }

                    if (isBorder) {
                        int sIndex = random.nextInt(selafitBlocks.length);
                        level.setBlock(pos, selafitBlocks[sIndex].defaultBlockState(), 3);
                    } else {
                        level.removeBlock(pos, false);
                    }
                }
            }));
        }
    }

    private static void removeItemsInRadiusBatched(ServerLevel level, BlockPos center, int radius) {
        AABB box = new AABB(center).inflate(radius);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box);
        for (ItemEntity item : items) {
            item.discard();
        }
    }

    /**
     * Применение динамических зон поражения (блоки и урон)
     */
    private static void applyDynamicDamageZones(
            ServerLevel level,
            BlockPos centerPos,
            RayTerminationData terminationData,
            Block wasteLogBlock,
            Block wastePlanksBlock,
            Block burnedGrassBlock,
            Block[] selafitBlocks,
            RandomSource random) {

        System.out.println("\n[CRATER_GENERATOR] 🎯 Step 2: Applying dynamic damage zones!");
        System.out.println("[CRATER_GENERATOR] Max ray distance: " + terminationData.maxDistance);

        double zone3Radius = Math.max(terminationData.maxDistance * ZONE_3_RADIUS_MULTIPLIER, 50);
        double zone4Radius = Math.max(terminationData.maxDistance * ZONE_4_RADIUS_MULTIPLIER, 80);

        long zone3RadiusSq = (long) zone3Radius * (long) zone3Radius;
        long zone4RadiusSq = (long) zone4Radius * (long) zone4Radius;

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();
        int searchRadius = (int) zone4Radius + 20;

        // Применяем эффекты зон
        for (int x = centerX - searchRadius; x <= centerX + searchRadius; x++) {
            long dx = x - centerX;
            long dxSq = dx * dx;
            if (dxSq > zone4RadiusSq) continue;

            for (int z = centerZ - searchRadius; z <= centerZ + searchRadius; z++) {
                long dz = z - centerZ;
                long distanceSq = dxSq + dz * dz;
                if (distanceSq > zone4RadiusSq) continue;

                for (int y = centerY - 100; y <= centerY + DAMAGE_ZONE_HEIGHT + 60; y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (distanceSq <= zone3RadiusSq) {
                        applyZone3Effects(level, checkPos, state, selafitBlocks, random);
                    } else if (distanceSq <= zone4RadiusSq) {
                        applyZone4Effects(level, checkPos, state, wasteLogBlock, wastePlanksBlock, burnedGrassBlock, random);
                    }
                }
            }
        }

        // Удаляем предметы
        AABB zone3Area = new AABB(
                centerX - zone3Radius, centerY - DAMAGE_ZONE_HEIGHT, centerZ - zone3Radius,
                centerX + zone3Radius, centerY + DAMAGE_ZONE_HEIGHT, centerZ + zone3Radius
        );

        List<ItemEntity> itemsZone3 = level.getEntitiesOfClass(ItemEntity.class, zone3Area);
        for (ItemEntity item : itemsZone3) {
            item.discard();
        }

        AABB zone4Area = new AABB(
                centerX - zone4Radius, centerY - DAMAGE_ZONE_HEIGHT, centerZ - zone4Radius,
                centerX + zone4Radius, centerY + DAMAGE_ZONE_HEIGHT, centerZ + zone4Radius
        );

        List<ItemEntity> itemsZone4 = level.getEntitiesOfClass(ItemEntity.class, zone4Area);
        for (ItemEntity item : itemsZone4) {
            item.discard();
        }

        // Применяем урон сущностям
        applyDamageToEntities(level, centerPos, zone3Radius, zone4Radius, random);

        System.out.println("[CRATER_GENERATOR] ✅ Damage zones applied! Zone3: " + zone3Radius + "m, Zone4: " + zone4Radius + "m");
    }

    private static void applyZone3Effects(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Block[] selafitBlocks,
            RandomSource random) {

        if (state.isAir()) {
            return;
        }

        if (state.is(BlockTags.LEAVES)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PUMPKIN) || state.is(Blocks.ROOTED_DIRT)
                || state.is(ModBlocks.DEAD_DIRT.get())
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.SAND) ||
                state.is(Blocks.MYCELIUM) || state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) ||
                state.is(Blocks.SANDSTONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.GRAVEL)) {

            Block selafitBlock = selafitBlocks[random.nextInt(selafitBlocks.length)];
            level.setBlock(pos, selafitBlock.defaultBlockState(), 3);
        }
    }

    private static void applyZone4Effects(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Block wasteLogBlock,
            Block wastePlanksBlock,
            Block burnedGrassBlock,
            RandomSource random) {

        if (state.isAir()) {
            return;
        }

        if (state.is(BlockTags.LEAVES)) {
            if (random.nextFloat() < 0.5F) {
                level.removeBlock(pos, false);
            } else if (random.nextFloat() < 0.5F) {
                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
            }
            return;
        }

        if (state.is(BlockTags.LOGS)) {
            level.setBlock(pos, wasteLogBlock.defaultBlockState(), 3);
        } else if (state.is(BlockTags.PLANKS)) {
            level.setBlock(pos, wastePlanksBlock.defaultBlockState(), 3);
        } else if (state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS) ||
                state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.WOODEN_DOORS)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PUMPKIN) || state.is(Blocks.ROOTED_DIRT)
                || state.is(ModBlocks.DEAD_DIRT.get())
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.MYCELIUM) ||
                state.is(Blocks.PODZOL)) {
            level.setBlock(pos, burnedGrassBlock.defaultBlockState(), 3);
        } else if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) ||
                state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) ||
                state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE) ||
                state.is(Blocks.BLUE_ICE) || state.is(Blocks.PACKED_ICE) ||
                state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS)) {
            level.removeBlock(pos, false);
        } else if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)) {
            if (random.nextFloat() < 0.6F) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static void applyDamageToEntities(
            ServerLevel level,
            BlockPos centerPos,
            double zone3Radius,
            double zone4Radius,
            RandomSource random) {

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        AABB zone3Area = new AABB(
                centerX - zone3Radius, centerY - DAMAGE_ZONE_HEIGHT, centerZ - zone3Radius,
                centerX + zone3Radius, centerY + DAMAGE_ZONE_HEIGHT, centerZ + zone3Radius
        );

        AABB zone4Area = new AABB(
                centerX - zone4Radius, centerY - DAMAGE_ZONE_HEIGHT, centerZ - zone4Radius,
                centerX + zone4Radius, centerY + DAMAGE_ZONE_HEIGHT, centerZ + zone4Radius
        );

        List<LivingEntity> entitiesZone3 = level.getEntitiesOfClass(LivingEntity.class, zone3Area);
        for (LivingEntity entity : entitiesZone3) {
            entity.hurt(level.damageSources().generic(), ZONE_3_DAMAGE);
            entity.setSecondsOnFire((int) FIRE_DURATION / 20);
            applyExplosionKnockback(entity, centerPos, zone3Radius);
        }

        List<LivingEntity> entitiesZone4 = level.getEntitiesOfClass(LivingEntity.class, zone4Area);
        for (LivingEntity entity : entitiesZone4) {
            if (!entitiesZone3.contains(entity)) {
                entity.hurt(level.damageSources().generic(), ZONE_4_DAMAGE);
                entity.setSecondsOnFire((int) FIRE_DURATION / 20);
                applyExplosionKnockback(entity, centerPos, zone4Radius);
            }
        }
    }

    private static void applyExplosionKnockback(LivingEntity entity, BlockPos centerPos, double radius) {
        double dx = entity.getX() - centerPos.getX();
        double dz = entity.getZ() - centerPos.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.1) {
            distance = 0.1;
        }

        double dirX = dx / distance;
        double dirZ = dz / distance;
        double knockbackStrength = Math.max(0, 1.0 - (distance / radius)) * 1.5;

        entity.push(dirX * knockbackStrength, 0.5, dirZ * knockbackStrength);
    }
}