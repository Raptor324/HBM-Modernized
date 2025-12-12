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
 * Crater Generator v17.1 - EXPLOSION CENTER AT GROUND LEVEL
 *
 * ✅ FIXED: Raycast distortion when triggered from blocks
 * ✅ FIXED: Math.floor() rounding errors for negative directions
 * ✅ FIXED: Proper center-to-center ray calculation
 * ✅ FIXED: Race condition in block collection
 * ✅ FIXED: Explosion center now at ground level (below the bomb)
 * ✅ VERIFIED: Works correctly from both entities and blocks
 */

public class CraterGenerator {

    private static final int CRATER_GENERATION_DELAY = 30;
    private static final int RING_COUNT = 8;
    private static final int BLOCK_BATCH_SIZE = 256;
    private static final int TOTAL_RAYS = 51840;
    private static final int RAYS_PER_TICK = 200;
    private static final double MAX_PENETRATION = 2000.0;
    private static final double MIN_PENETRATION = 500.0;
    private static final double MAX_RAY_DISTANCE = 100.0;
    private static final int RAY_THICKNESS = 2;
    private static final float ZONE_3_RADIUS_MULTIPLIER = 0.9F;
    private static final float ZONE_4_RADIUS_MULTIPLIER = 1.4F;
    private static final int DAMAGE_ZONE_HEIGHT = 80;
    private static final float ZONE_3_DAMAGE = 500.0F;
    private static final float ZONE_4_DAMAGE = 200.0F;
    private static final float FIRE_DURATION = 380.0F;

    private static class RayTerminationData {
        double maxDistance = 0;
        Set<BlockPos> terminationPoints = Collections.synchronizedSet(new HashSet<>());
    }

    public static void generateCrater(
            ServerLevel level,
            BlockPos centerPos,
            Block sellafit1,
            Block sellafit2,
            Block sellafit3,
            Block sellafit4,
            Block wasteLogBlock,
            Block wastePlanksBlock,
            Block burnedGrassBlock) {

        // ✅ CRITICAL FIX v17.1: Center is at ground level (below the bomb position)
        BlockPos groundCenterPos = centerPos.below();

        RandomSource random = level.random;
        Block[] selafitBlocks = {sellafit1, sellafit2, sellafit3, sellafit4};
        Set<BlockPos> craterBlocksSet = Collections.synchronizedSet(new HashSet<>());
        List<Set<BlockPos>> rings = new ArrayList<>();
        RayTerminationData terminationData = new RayTerminationData();

        for (int i = 0; i < RING_COUNT; i++) {
            rings.add(Collections.synchronizedSet(new HashSet<>()));
        }

        long startTime = System.currentTimeMillis();
        System.out.println("\n========================================");
        System.out.println("[CRATER_GENERATOR] START: Generating crater...");
        System.out.println("Bomb Position: " + centerPos);
        System.out.println("Crater Center (Ground): " + groundCenterPos);
        System.out.println("Total Rays: " + TOTAL_RAYS);
        System.out.println("========================================\n");

        BlockPos below = groundCenterPos.below();
        BlockState belowState = level.getBlockState(below);

        if (!belowState.isAir() && !belowState.is(Blocks.BEDROCK)) {
            craterBlocksSet.add(below);
            distributeBlockToRings(groundCenterPos, below, (int) MAX_RAY_DISTANCE, rings);
        }

        collectSphericRaysAsync(level, groundCenterPos, craterBlocksSet, rings, terminationData, () -> {
            System.out.println("\n[CRATER_GENERATOR] Step 1: Finalizing crater structure...");
            finalizeCrater(level, groundCenterPos, rings, craterBlocksSet,
                    selafitBlocks, random,
                    wasteLogBlock, wastePlanksBlock, burnedGrassBlock, startTime);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.tell(new TickTask(5, () -> {
                    System.out.println("\n[CRATER_GENERATOR] Step 2: Applying dynamic damage zones...");
                    double zone3Radius = Math.max(terminationData.maxDistance * ZONE_3_RADIUS_MULTIPLIER, 50);
                    double zone4Radius = Math.max(terminationData.maxDistance * ZONE_4_RADIUS_MULTIPLIER, 80);

                    applyDynamicDamageZones(level, groundCenterPos, terminationData,
                            wasteLogBlock, wastePlanksBlock, burnedGrassBlock,
                            selafitBlocks, random);

                    server.tell(new TickTask(10, () -> {
                        System.out.println("\n[CRATER_GENERATOR] Step 3: Applying crater biomes to zones...");
                        CraterBiomeHelper.applyBiomesAsync(level, groundCenterPos, zone3Radius, zone4Radius);
                        System.out.println("\n[CRATER_GENERATOR] All steps complete!");
                    }));
                }));
            }
        });
    }

    private static double calculatePenetrationFromAngle(double verticalAngleDegrees) {
        double absAngle = Math.abs(verticalAngleDegrees);
        if (absAngle > 90.0) absAngle = 90.0;

        double angleRatio = absAngle / 90.0;
        double penetrationFactor = 1.0 - (angleRatio * angleRatio * 0.6);
        double penetration = MIN_PENETRATION + (MAX_PENETRATION - MIN_PENETRATION) * penetrationFactor;

        if (penetration < MIN_PENETRATION) penetration = MIN_PENETRATION;
        if (penetration > MAX_PENETRATION) penetration = MAX_PENETRATION;

        return penetration;
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

        // ✅ CRITICAL FIX: Convert BlockPos to exact center (double coordinates)
        double startX = centerPos.getX() + 0.5;
        double startY = centerPos.getY() + 0.5;
        double startZ = centerPos.getZ() + 0.5;

        int raysToProcess = Math.min(RAYS_PER_TICK, TOTAL_RAYS - currentRayIndex[0]);
        int processed = 0;

        while (processed < raysToProcess && currentRayIndex[0] < TOTAL_RAYS) {
            int rayIndex = currentRayIndex[0];

            // Golden-spiral sphere sampling algorithm
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

            if (elevationDegrees < 0) {
                elevationDegrees *= 0.85;
            }

            double penetration = calculatePenetrationFromAngle(elevationDegrees);

            // ✅ Pass exact double coordinates
            traceRay(level, startX, startY, startZ, dirX, dirY, dirZ, penetration,
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

    // ✅ CRITICAL FIX: Proper float-to-int conversion with correct floor behavior
    private static int floorCoordinate(double coord) {
        return (int) Math.floor(coord);
    }

    private static void traceRay(
            ServerLevel level,
            double startX, double startY, double startZ,
            double dirX, double dirY, double dirZ,
            double basePenetration,
            BlockPos centerPos,
            Set<BlockPos> craterBlocksSet,
            List<Set<BlockPos>> rings,
            RayTerminationData terminationData) {

        double penetration = basePenetration;

        if (penetration < MIN_PENETRATION || penetration > MAX_PENETRATION) {
            return;
        }

        int maxSteps = (int) MAX_RAY_DISTANCE;
        BlockPos lastBlockPos = centerPos;

        // Calculate perpendicular vectors for ray thickness
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
            // ✅ CRITICAL FIX: Use proper floor for all coordinates
            // This handles both positive and negative directions correctly
            int baseX = floorCoordinate(startX + dirX * step);
            int baseY = floorCoordinate(startY + dirY * step);
            int baseZ = floorCoordinate(startZ + dirZ * step);

            boolean blockedAtStep = false;

            // Process center of ray
            {
                double defenseCenter = processBlockInRay(level, baseX, baseY, baseZ,
                        centerPos, craterBlocksSet, rings);

                if (defenseCenter > 0) {
                    lastBlockPos = new BlockPos(baseX, baseY, baseZ);
                    penetration -= defenseCenter;

                    if (penetration < 0) penetration = 0;

                    if (defenseCenter >= 10_000) {
                        blockedAtStep = true;
                    }
                }
            }

            // Process perpendicular thickness
            if (!blockedAtStep) {
                int[] thicknessOffsets = {1, -1, 2, -2};

                for (int thickness : thicknessOffsets) {
                    if (blockedAtStep) break;

                    // ✅ Use round for perpendicular offsets (these are local adjustments)
                    int x1 = baseX + (int) Math.round(perpX1 * thickness);
                    int y1 = baseY + (int) Math.round(perpY1 * thickness);
                    int z1 = baseZ + (int) Math.round(perpZ1 * thickness);

                    double defense1 = processBlockInRay(level, x1, y1, z1,
                            centerPos, craterBlocksSet, rings);

                    penetration -= defense1;
                    if (penetration < 0) penetration = 0;
                    if (defense1 >= 10_000) blockedAtStep = true;

                    // Second perpendicular direction
                    int x2 = baseX + (int) Math.round(perpX2 * thickness);
                    int y2 = baseY + (int) Math.round(perpY2 * thickness);
                    int z2 = baseZ + (int) Math.round(perpZ2 * thickness);

                    double defense2 = processBlockInRay(level, x2, y2, z2,
                            centerPos, craterBlocksSet, rings);

                    penetration -= defense2;
                    if (penetration < 0) penetration = 0;
                    if (defense2 >= 10_000) blockedAtStep = true;
                }
            }

            if (blockedAtStep) break;
        }

        // Register termination point
        if (lastBlockPos != centerPos) {
            terminationData.terminationPoints.add(lastBlockPos);

            double distance = Math.sqrt(
                    Math.pow(lastBlockPos.getX() - centerPos.getX(), 2) +
                            Math.pow(lastBlockPos.getZ() - centerPos.getZ(), 2)
            );

            synchronized (terminationData) {
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

        if (defense < 10_000) {
            synchronized (craterBlocksSet) {
                if (!craterBlocksSet.contains(pos)) {
                    craterBlocksSet.add(pos);
                    distributeBlockToRings(centerPos, pos, (int) MAX_RAY_DISTANCE, rings);
                }
            }
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
            Block wasteLog,
            Block wastePlanks,
            Block burnedGrass,
            long startTime) {

        processAllRingsBatched(level, centerPos, rings, allCraterBlocks,
                selafitBlocks, random, wasteLog, wastePlanks, burnedGrass);

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

    private static void applyDynamicDamageZones(
            ServerLevel level,
            BlockPos centerPos,
            RayTerminationData terminationData,
            Block wasteLogBlock,
            Block wastePlanksBlock,
            Block burnedGrassBlock,
            Block[] selafitBlocks,
            RandomSource random) {

        double zone3Radius = Math.max(terminationData.maxDistance * ZONE_3_RADIUS_MULTIPLIER, 50);
        double zone4Radius = Math.max(terminationData.maxDistance * ZONE_4_RADIUS_MULTIPLIER, 80);
        long zone3RadiusSq = (long) zone3Radius * (long) zone3Radius;
        long zone4RadiusSq = (long) zone4Radius * (long) zone4Radius;

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        int searchRadius = (int) zone4Radius + 20;

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

        applyDamageToEntities(level, centerPos, zone3Radius, zone4Radius, random);
    }

    private static void applyZone3Effects(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Block[] selafitBlocks,
            RandomSource random) {

        if (state.isAir()) return;

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

        if (state.isAir()) return;

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

        if (distance < 0.1) distance = 0.1;

        double dirX = dx / distance;
        double dirZ = dz / distance;
        double knockbackStrength = Math.max(0, 1.0 - (distance / radius)) * 1.5;

        entity.push(dirX * knockbackStrength, 0.5, dirZ * knockbackStrength);
    }
}
