package com.hbm_m.util.explosions.nuclear;

import com.hbm_m.util.explosions.nuclear.CraterBiomeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.BlockTags;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crater Generator v21.0 - BIOME SYNC FIX
 * ✅ FIXED: Y-coordinate calculation synchronized with biomes
 * ✅ FIXED: Distance calculation uses only XZ (2D radius) for zone boundaries
 * ✅ ENHANCED: Biome layer detection for seamless vertical coverage
 * ✅ OPTIMIZED: Proper wavy border consistency
 */
public class CraterGenerator {

    private static final int CRATER_GENERATION_DELAY = 30;
    private static final int RING_COUNT = 8;
    private static final int BLOCK_BATCH_SIZE = 512;
    private static final int TOTAL_RAYS = 103680;
    private static final int RAYS_PER_TICK = 500;
    private static final double MAX_PENETRATION = 2000.0;
    private static final double MIN_PENETRATION = 500.0;
    private static final double MAX_RAY_DISTANCE = 100.0;
    private static final int RAY_THICKNESS = 3;
    private static final float ZONE_3_RADIUS_MULTIPLIER = 0.9F;
    private static final float ZONE_4_RADIUS_MULTIPLIER = 1.8F;
    private static final int DAMAGE_ZONE_HEIGHT = 80;
    private static final float ZONE_3_DAMAGE = 5000.0F;
    private static final float ZONE_4_DAMAGE = 2000.0F;
    private static final float FIRE_DURATION = 380.0F;
    private static final int CENTER_SPHERE_RADIUS = 5;
    private static final float CLEANUP_DEFENSE_THRESHOLD = 1500.0F;

    public static long RAY_TRAIL_DURATION = 60_000L;

    // Wave border constants - SYNCHRONIZED with CraterBiomeHelper
    private static final double ZONE_NOISE_SCALE = 0.15;
    private static final double ZONE_NOISE_STRENGTH = 0.25;

    // *** NEW: Gradient zone for smooth biome transitions ***
    private static final float ZONE_GRADIENT_THICKNESS = 1.15F;  // 15% transition zone

    private static final Map<BlockPos, List<RayData>> rayDebugData = new ConcurrentHashMap<>();

    public static class RayData {
        public final double startX, startY, startZ;
        public final double dirX, dirY, dirZ;
        public final List<BlockPos> hitBlocks;
        public final long timestamp;
        public final long expirationTime;

        RayData(double startX, double startY, double startZ,
                double dirX, double dirY, double dirZ) {
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.dirX = dirX;
            this.dirY = dirY;
            this.dirZ = dirZ;
            this.hitBlocks = Collections.synchronizedList(new ArrayList<>());
            this.timestamp = System.currentTimeMillis();
            this.expirationTime = timestamp + RAY_TRAIL_DURATION;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

        public float getRemainingSeconds() {
            long remaining = expirationTime - System.currentTimeMillis();
            return Math.max(0, remaining / 1000.0f);
        }
    }

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
            Block burnedGrassBlock,
            Block deadDirtBlocks) {

        BlockPos groundCenterPos = centerPos;
        RandomSource random = level.random;
        Block[] selafitBlocks = {sellafit1, sellafit2, sellafit3, sellafit4};
        Set<BlockPos> craterBlocksSet = Collections.synchronizedSet(new HashSet<>());
        List<Set<BlockPos>> rings = new ArrayList<>();
        RayTerminationData terminationData = new RayTerminationData();
        rayDebugData.clear();
        List<RayData> currentRayDebugList = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < RING_COUNT; i++) {
            rings.add(Collections.synchronizedSet(new HashSet<>()));
        }

        long startTime = System.currentTimeMillis();
        System.out.println("\n========================================");
        System.out.println("[CRATER_GENERATOR] START: Generating crater...");
        System.out.println("Bomb Position: " + centerPos);
        System.out.println("Total Rays: " + TOTAL_RAYS);
        System.out.println("========================================\n");

        BlockPos below = groundCenterPos.below();
        BlockState belowState = level.getBlockState(below);

        if (!belowState.isAir() && !belowState.is(Blocks.BEDROCK)) {
            craterBlocksSet.add(below);
            distributeBlockToRings(groundCenterPos, below, (int) MAX_RAY_DISTANCE, rings);
        }

        collectSphericRaysAsync(level, groundCenterPos, craterBlocksSet, rings, terminationData, currentRayDebugList, () -> {
            System.out.println("\n[CRATER_GENERATOR] Step 1: Finalizing crater structure...");
            if (!currentRayDebugList.isEmpty()) {
                rayDebugData.put(groundCenterPos, new ArrayList<>(currentRayDebugList));
                System.out.println("[DEBUG] Stored " + currentRayDebugList.size() + " rays for visualization");
            }

            finalizeCrater(level, groundCenterPos, rings, craterBlocksSet,
                    selafitBlocks, random,
                    wasteLogBlock, wastePlanksBlock, burnedGrassBlock, startTime);

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.tell(new TickTask(5, () -> {
                    System.out.println("\n[CRATER_GENERATOR] Step 1.5: Cleaning center sphere...");
                    cleanupCenterSphere(level, groundCenterPos);

                    server.tell(new TickTask(5, () -> {
                        System.out.println("\n[CRATER_GENERATOR] Step 2: Calculating and applying dynamic damage zones...");
                        double zone3Radius = Math.max(terminationData.maxDistance * ZONE_3_RADIUS_MULTIPLIER, 50);
                        double zone4Radius = Math.max(terminationData.maxDistance * ZONE_4_RADIUS_MULTIPLIER, 80);

                        System.out.println("[CRATER_GENERATOR] Zone 3 Radius: " + (int)zone3Radius + "m");
                        System.out.println("[CRATER_GENERATOR] Zone 4 Radius: " + (int)zone4Radius + "m");

                        applyDynamicDamageZones(level, groundCenterPos, zone3Radius, zone4Radius,
                                wasteLogBlock, wastePlanksBlock, burnedGrassBlock, deadDirtBlocks, selafitBlocks, random);

                        server.tell(new TickTask(1, () -> {
                            System.out.println("\n[CRATER_GENERATOR] Step 3: Applying crater biomes to zones...");
                            CraterBiomeHelper.applyBiomesAsync(level, groundCenterPos, zone3Radius, zone4Radius);
                            System.out.println("\n[CRATER_GENERATOR] All steps complete!");
                        }));
                    }));
                }));
            }
        });
    }

    public static List<RayData> getDebugRays(BlockPos centerPos) {
        List<RayData> rays = rayDebugData.getOrDefault(centerPos, new ArrayList<>());
        rays.removeIf(RayData::isExpired);
        return rays;
    }

    public static List<RayData> getAllDebugRays() {
        List<RayData> allRays = new ArrayList<>();
        for (List<RayData> rayList : rayDebugData.values()) {
            allRays.addAll(rayList);
        }
        allRays.removeIf(RayData::isExpired);
        return allRays;
    }

    private static boolean isDebugScreenEnabled() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc != null && mc.options.renderDebug;
        } catch (Exception e) {
            return false;
        }
    }

    // *** FIXED: Wave noise function ***
    private static double getSimpleWaveNoise(double x, double z) {
        double wave1 = Math.sin(x * 0.1) * Math.cos(z * 0.1) * 0.5;
        double wave2 = Math.sin(x * 0.05 + z * 0.08) * 0.3;
        double wave3 = Math.cos(x * 0.15 - z * 0.12) * 0.2;
        return (wave1 + wave2 + wave3) / 2.0;
    }

    // *** FIXED: Sync with CraterBiomeHelper ***
    private static double getZoneRadiusWithNoise(double baseRadius, double centerX, double centerZ, int x, int z) {
        double relX = (x - centerX) * ZONE_NOISE_SCALE;
        double relZ = (z - centerZ) * ZONE_NOISE_SCALE;
        double noise = getSimpleWaveNoise(relX, relZ);
        double noiseInfluence = 1.0 + (noise * ZONE_NOISE_STRENGTH);
        return baseRadius * noiseInfluence;
    }

    private static void cleanupCenterSphere(ServerLevel level, BlockPos centerPos) {
        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();
        int removed = 0;

        for (int x = centerX - CENTER_SPHERE_RADIUS; x <= centerX + CENTER_SPHERE_RADIUS; x++) {
            long dx = x - centerX;
            long dxSq = dx * dx;
            if (dxSq > CENTER_SPHERE_RADIUS * CENTER_SPHERE_RADIUS) continue;

            for (int z = centerZ - CENTER_SPHERE_RADIUS; z <= centerZ + CENTER_SPHERE_RADIUS; z++) {
                long dz = z - centerZ;
                long distSq = dxSq + dz * dz;
                if (distSq > CENTER_SPHERE_RADIUS * CENTER_SPHERE_RADIUS) continue;

                for (int y = centerY - CENTER_SPHERE_RADIUS; y <= centerY + CENTER_SPHERE_RADIUS; y++) {
                    long dy = y - centerY;
                    long totalDistSq = distSq + dy * dy;
                    if (totalDistSq > CENTER_SPHERE_RADIUS * CENTER_SPHERE_RADIUS) continue;

                    BlockPos checkPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(checkPos);

                    if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                        float defense = BlockExplosionDefense.getBlockDefenseValue(level, checkPos, state);
                        if (defense < CLEANUP_DEFENSE_THRESHOLD) {
                            level.removeBlock(checkPos, false);
                            removed++;
                        }
                    }
                }
            }
        }

        System.out.println("[CRATER] Center sphere cleanup: " + removed + " blocks removed");
    }

    private static double calculatePenetrationFromAngle(double verticalAngleDegrees) {
        double absAngle = Math.abs(verticalAngleDegrees);
        if (absAngle > 90.0) absAngle = 90.0;
        double angleRatio = absAngle / 90.0;
        double penetrationFactor = 1.0 - (angleRatio * angleRatio * 0.60);
        double penetration = MIN_PENETRATION + (MAX_PENETRATION - MIN_PENETRATION) * penetrationFactor;
        return Math.max(MIN_PENETRATION, Math.min(MAX_PENETRATION, penetration));
    }

    private static void collectSphericRaysAsync(
            ServerLevel level,
            BlockPos centerPos,
            Set<BlockPos> craterBlocksSet,
            List<Set<BlockPos>> rings,
            RayTerminationData terminationData,
            List<RayData> debugRays,
            Runnable onComplete) {

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            onComplete.run();
            return;
        }

        int[] rayIndex = new int[1];
        rayIndex[0] = 0;
        processRaysBatchSpheric(level, centerPos, craterBlocksSet, rings, terminationData,
                rayIndex, server, debugRays, onComplete);
    }

    private static void processRaysBatchSpheric(
            ServerLevel level,
            BlockPos centerPos,
            Set<BlockPos> craterBlocksSet,
            List<Set<BlockPos>> rings,
            RayTerminationData terminationData,
            int[] currentRayIndex,
            MinecraftServer server,
            List<RayData> debugRays,
            Runnable onComplete) {

        double startX = centerPos.getX() + 0.5;
        double startY = centerPos.getY() + 0.5;
        double startZ = centerPos.getZ() + 0.5;

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
            if (elevationDegrees < 0) {
                elevationDegrees *= 0.85;
            }

            double basePenetration = calculatePenetrationFromAngle(elevationDegrees);

            double noiseScale = 0.08;
            double noiseStrength = 0.35;

            int maxStepsModified = CraterNoiseGenerator.getNoiseModifiedMaxDistance(
                    (int)MAX_RAY_DISTANCE,
                    dirX, dirY, dirZ,
                    noiseScale,
                    noiseStrength
            );

            RayData debugRay = null;
            if (isDebugScreenEnabled()) {
                debugRay = new RayData(startX, startY, startZ, dirX, dirY, dirZ);
            }

            traceRay(level, startX, startY, startZ, dirX, dirY, dirZ, basePenetration,
                    maxStepsModified,
                    centerPos, craterBlocksSet, rings, terminationData, debugRay);

            if (debugRay != null && !debugRay.hitBlocks.isEmpty()) {
                debugRays.add(debugRay);
            }

            currentRayIndex[0]++;
            processed++;
        }

        if (currentRayIndex[0] < TOTAL_RAYS) {
            server.tell(new TickTask(1, () ->
                    processRaysBatchSpheric(level, centerPos, craterBlocksSet, rings, terminationData,
                            currentRayIndex, server, debugRays, onComplete)
            ));
        } else {
            onComplete.run();
        }
    }

    private static int floorCoordinate(double coord) {
        return (int) Math.floor(coord);
    }

    private static void traceRay(
            ServerLevel level,
            double startX, double startY, double startZ,
            double dirX, double dirY, double dirZ,
            double basePenetration,
            int maxSteps,
            BlockPos centerPos,
            Set<BlockPos> craterBlocksSet,
            List<Set<BlockPos>> rings,
            RayTerminationData terminationData,
            RayData debugRay) {

        double penetration = basePenetration;
        if (penetration < MIN_PENETRATION || penetration > MAX_PENETRATION) {
            return;
        }

        BlockPos lastBlockPos = centerPos;
        double perpX1, perpY1, perpZ1;
        double perpX2, perpY2, perpZ2;

        double absX = Math.abs(dirX);
        double absY = Math.abs(dirY);
        double absZ = Math.abs(dirZ);

        if (absZ <= absX && absZ <= absY) {
            perpX1 = dirY;
            perpY1 = -dirX;
            perpZ1 = 0;
        } else if (absY <= absX && absY <= absZ) {
            perpX1 = dirZ;
            perpY1 = 0;
            perpZ1 = -dirX;
        } else {
            perpX1 = 0;
            perpY1 = dirZ;
            perpZ1 = -dirY;
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
            int baseX = floorCoordinate(startX + dirX * step);
            int baseY = floorCoordinate(startY + dirY * step);
            int baseZ = floorCoordinate(startZ + dirZ * step);

            boolean blockedAtStep = false;

            double defenseCenter = processBlockInRay(level, baseX, baseY, baseZ,
                    centerPos, craterBlocksSet, rings);

            if (defenseCenter > 0) {
                lastBlockPos = new BlockPos(baseX, baseY, baseZ);
                if (debugRay != null) {
                    debugRay.hitBlocks.add(lastBlockPos);
                }

                penetration -= defenseCenter;
                if (penetration < 0) penetration = 0;

                if (defenseCenter >= 10_000) {
                    blockedAtStep = true;
                }
            }

            if (!blockedAtStep) {
                int[] thicknessOffsets = {1, -1, 2, -2, 3, -3};
                for (int thickness : thicknessOffsets) {
                    if (blockedAtStep) break;

                    int x1 = baseX + (int) Math.round(perpX1 * thickness);
                    int y1 = baseY + (int) Math.round(perpY1 * thickness);
                    int z1 = baseZ + (int) Math.round(perpZ1 * thickness);

                    double defense1 = processBlockInRay(level, x1, y1, z1, centerPos, craterBlocksSet, rings);
                    penetration -= defense1;
                    if (penetration < 0) penetration = 0;
                    if (defense1 >= 10_000) blockedAtStep = true;

                    int x2 = baseX + (int) Math.round(perpX2 * thickness);
                    int y2 = baseY + (int) Math.round(perpY2 * thickness);
                    int z2 = baseZ + (int) Math.round(perpZ2 * thickness);

                    double defense2 = processBlockInRay(level, x2, y2, z2, centerPos, craterBlocksSet, rings);
                    penetration -= defense2;
                    if (penetration < 0) penetration = 0;
                    if (defense2 >= 10_000) blockedAtStep = true;
                }
            }
        }

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

        // Если воздух — пропускаем (сопротивление 0)
        if (state.isAir()) {
            return 0;
        }

        // Бедрок останавливает луч
        if (state.is(Blocks.BEDROCK)) {
            return 10_000;
        }

        // [НОВОЕ]: Жидкости (Вода/Лава) теперь тоже считаются "мягкими" препятствиями,
        // но они ДОЛЖНЫ быть удалены.
        // Раньше мы могли возвращать 0 или малую величину, не добавляя их в craterBlocksSet.
        if (!state.getFluidState().isEmpty()) {
            // Добавляем жидкость в список уничтожения
            synchronized (craterBlocksSet) {
                if (!craterBlocksSet.contains(pos)) {
                    craterBlocksSet.add(pos);
                    distributeBlockToRings(centerPos, pos, (int) MAX_RAY_DISTANCE, rings);
                }
            }
            // Жидкость почти не гасит луч (как воздух или чуть плотнее)
            return 0.2;
        }

        // Пропускаем "прозрачные" для коллизии блоки (трава, цветы), но удаляем их
        // (Они имеют resistance ~0, но лучше явно обработать)
        if (state.canBeReplaced() || state.getCollisionShape(level, pos).isEmpty()) {
            synchronized (craterBlocksSet) {
                if (!craterBlocksSet.contains(pos)) {
                    craterBlocksSet.add(pos);
                    distributeBlockToRings(centerPos, pos, (int) MAX_RAY_DISTANCE, rings);
                }
            }
            return 0.1;
        }

        // Стандартная обработка твердых блоков
        float defense = BlockExplosionDefense.getBlockDefenseValue(level, pos, state);

        if (defense < 10_000) {
            synchronized (craterBlocksSet) {
                if (!craterBlocksSet.contains(pos)) {
                    craterBlocksSet.add(pos);
                    distributeBlockToRings(centerPos, pos, (int) MAX_RAY_DISTANCE, rings);
                }
            }
            return defense;
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
            // Используем tickCount + задержку для равномерного распределения нагрузки
            server.tell(new TickTask(server.getTickCount() + i + 1, () -> {
                int start = batchIndex * BLOCK_BATCH_SIZE;
                int end = Math.min(start + BLOCK_BATCH_SIZE, totalBlocks);

                for (int j = start; j < end; j++) {
                    BlockPos pos = allBlocksList.get(j);
                    boolean isBorder = false;

                    // Проверка на границу кратера (есть ли рядом блок не из кратера?)
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) {
                                    BlockPos neighbor = pos.offset(dx, dy, dz);
                                    BlockState neighborState = level.getBlockState(neighbor);

                                    // Если сосед не в кратере и твердый -> значит мы на границе
                                    if (!allCraterBlocks.contains(neighbor) &&
                                            !neighborState.isAir() &&
                                            !neighborState.getCollisionShape(level, neighbor).isEmpty()) {
                                        isBorder = true;
                                        break;
                                    }
                                }
                            }
                            if (isBorder) break;
                        }
                        if (isBorder) break;
                    }

                    if (isBorder) {
                        BlockState state = level.getBlockState(pos);

                        // [ИСПРАВЛЕНИЕ]: Мягкие блоки просто удаляются, а не превращаются в камень
                        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) ||
                                state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) ||
                                state.getCollisionShape(level, pos).isEmpty()) {

                            level.removeBlock(pos, false);

                        } else {
                            // Твердые блоки становятся селлафитом
                            int sIndex = random.nextInt(selafitBlocks.length);
                            level.setBlock(pos, selafitBlocks[sIndex].defaultBlockState(), 3);
                        }
                    } else {
                        // Внутренность кратера - удаляем блок
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
            double zone3Radius,
            double zone4Radius,
            Block wasteLogBlock,
            Block wastePlanksBlock,
            Block burnedGrassBlock,
            Block deadDirtBlock,
            Block[] selafitBlocks,
            RandomSource random) {

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        int scanHeightUp = 256;
        int scanHeightDown = 64;

        // Радиус поиска +40 блоков для захвата зон 5 и 6
        int searchRadius = (int) zone4Radius + 40;

        for (int x = centerX - searchRadius; x <= centerX + searchRadius; x++) {
            long dx = x - centerX;
            for (int z = centerZ - searchRadius; z <= centerZ + searchRadius; z++) {
                long dz = z - centerZ;

                double distance2D = Math.sqrt(dx * dx + dz * dz);

                // Радиусы всех зон (с шумом)
                double r3 = getZoneRadiusWithNoise(zone3Radius, centerX, centerZ, x, z);
                double r4 = getZoneRadiusWithNoise(zone4Radius, centerX, centerZ, x, z);
                double r5 = getZoneRadiusWithNoise(zone4Radius + 24.0, centerX, centerZ, x, z); // Обугливание
                double r6 = getZoneRadiusWithNoise(zone4Radius + 48.0, centerX, centerZ, x, z); // Снос листвы

                // Если мы за пределами самой дальней зоны (6) — пропускаем
                if (distance2D > r6) continue;

                boolean isZone3 = distance2D <= r3;
                boolean isZone4 = distance2D <= r4;
                boolean isZone5 = distance2D <= r5;

                BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

                for (int y = centerY + scanHeightUp; y >= centerY - scanHeightDown; y--) {
                    mutablePos.set(x, y, z);
                    BlockState originalState = level.getBlockState(mutablePos);

                    if (originalState.isAir()) continue;

                    BlockPos fixedPos = mutablePos.immutable();

                    if (isZone3) {
                        // === ZONE 3: Эпицентр ===
                        transformBlockInZone3(level, fixedPos, originalState, selafitBlocks, random);
                        break; // Тут поверхность уничтожена, осадок не нужен

                    } else {
                        // === ZONE 4, 5, 6 (Внешние зоны) ===

                        // 1. Применяем разрушения в зависимости от зоны
                        if (isZone4) {
                            // ZONE 4: Полное выжигание
                            applyZone4Effects(level, fixedPos, originalState, wasteLogBlock, wastePlanksBlock, burnedGrassBlock, deadDirtBlock, random);
                        } else if (isZone5) {
                            // ZONE 5: Обугливание леса
                            applyZone5Effects(level, fixedPos, originalState, wasteLogBlock, wastePlanksBlock, random);
                        } else {
                            // ZONE 6: Снос листвы
                            applyZone6Effects(level, fixedPos, originalState, random);
                        }

                        // 2. [ИЗМЕНЕНИЕ]: Ядерный осадок выпадает во ВСЕХ внешних зонах (4, 5, 6)
                        BlockState newState = level.getBlockState(fixedPos);
                        if (!newState.isAir()) {
                            tryApplyNuclearFallout(level, fixedPos.above(), newState, random);
                        }

                        // Мы не делаем break, чтобы в лесу снег мог теоретически упасть на ветки ниже,
                        // если верхние сгорели, но так как мы идем сверху вниз и проверяем above().isAir(),
                        // снег ляжет на самую верхнюю твердую поверхность.
                    }
                }
            }
        }

        applyDamageToEntities(level, centerPos, zone3Radius, zone4Radius, random);
        cleanupItems(level, centerPos, zone3Radius + 10);
    }




    private static void transformBlockInZone3(ServerLevel level, BlockPos pos, BlockState state, Block[] selafitBlocks, RandomSource random) {
        if (state.is(Blocks.BEDROCK)) return;

        // Удаляем декорации и слабые блоки
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) ||
                state.is(BlockTags.FLOWERS) || state.is(Blocks.GRASS) ||
                state.is(Blocks.TALL_GRASS) || state.is(Blocks.SNOW) ||
                state.getCollisionShape(level, pos).isEmpty()) {
            level.removeBlock(pos, false);
            return;
        }

        // Превращаем твердый блок в случайный селафит
        Block selafitBlock = selafitBlocks[random.nextInt(selafitBlocks.length)];
        level.setBlock(pos, selafitBlock.defaultBlockState(), 3);
    }

    private static void applyZone4Effects(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Block wasteLogBlock,
            Block wastePlanksBlock,
            Block burnedGrassBlock,
            Block deadDirtBlock,
            RandomSource random) {

        if (state.isAir()) return;
        if (state.is(Blocks.BEDROCK)) return;

        if (state.is(BlockTags.LOGS)) {
            level.setBlock(pos, wasteLogBlock.defaultBlockState(), 3);
        } else if (state.is(BlockTags.PLANKS)) {
            level.setBlock(pos, wastePlanksBlock.defaultBlockState(), 3);
        }
        else if (state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.LEAVES)  || state.is(BlockTags.WOODEN_TRAPDOORS) ||
                state.is(Blocks.TORCH) || state.is(BlockTags.WOOL_CARPETS) || state.is(BlockTags.WOOL) ||
                state.is(BlockTags.WOODEN_FENCES) || state.is(Blocks.PUMPKIN) || state.is(BlockTags.WOODEN_DOORS)) {
            level.removeBlock(pos, false);
        } else if (state.is(Blocks.GRASS_BLOCK) ||
                state.is(Blocks.DIRT_PATH) ||
                state.is(Blocks.MYCELIUM) ||
                state.is(Blocks.PODZOL)) {
            level.setBlock(pos, burnedGrassBlock.defaultBlockState(), 3);
        } else if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) ||
                state.is(Blocks.ROOTED_DIRT)) {
            level.setBlock(pos, deadDirtBlock.defaultBlockState(), 3);
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

    // ZONE 5: Только деревья страдают (стволы чернеют)
    private static void applyZone5Effects(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            Block wasteLogBlock,
            Block wastePlanksBlock,
            RandomSource random) {

        // Листва: сносится или загорается
        if (state.is(BlockTags.LEAVES)) {
            if (random.nextFloat() < 0.7F) {
                level.removeBlock(pos, false); // Сдуло
            } else if (random.nextFloat() < 0.3F) {
                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3); // Загорелась
            }
            return;
        }

        // Бревна: превращаются в радиоактивные/жженые
        if (state.is(BlockTags.LOGS)) {
            level.setBlock(pos, wasteLogBlock.defaultBlockState(), 3);
            return;
        }

        // Доски (например, дома): тоже портятся
        if (state.is(BlockTags.PLANKS)) {
            level.setBlock(pos, wastePlanksBlock.defaultBlockState(), 3);
            return;
        }
        else if (state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_TRAPDOORS) ||
                state.is(Blocks.TORCH) || state.is(BlockTags.WOOL_CARPETS) || state.is(BlockTags.WOOL) ||
                state.is(BlockTags.WOODEN_FENCES) || state.is(Blocks.PUMPKIN)  || state.is(BlockTags.WOODEN_DOORS)) {
            level.removeBlock(pos, false);
        }
        // Траву и землю НЕ трогаем
    }

    // ZONE 6: Только листва сдувается
    private static void applyZone6Effects(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {

        // Листва: просто сносится ветром, иногда загорается (редко)
        if (state.is(BlockTags.LEAVES)) {
            if (random.nextFloat() < 0.5F) {
                level.removeBlock(pos, false); // Сдуло
            }
            else if (random.nextFloat() < 0.1F) {
                // Маленький шанс огня на самом краю
                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }


    private static void tryApplyNuclearFallout(ServerLevel level, BlockPos pos, BlockState blockBelowState, RandomSource random) {
        // Шанс 5%
        if (random.nextFloat() < 0.1F) {
            // 1. Проверяем, что место для снега свободно
            if (!level.getBlockState(pos).isAir()) return;

            // 2. [ВАЖНО] Проверяем, что блок СНИЗУ твердый сверху.
            // Это предотвратит левитацию на неполных блоках (если они вдруг выжили)
            if (!Block.isFaceFull(blockBelowState.getCollisionShape(level, pos.below()), net.minecraft.core.Direction.UP)) {
                return;
            }

            // 3. Проверяем, что блок снизу не горячий/жидкий
            if (!blockBelowState.getFluidState().isEmpty() ||
                    blockBelowState.is(Blocks.FIRE) ||
                    blockBelowState.is(Blocks.SOUL_FIRE) ||
                    blockBelowState.is(Blocks.MAGMA_BLOCK)) {
                return;
            }

            try {
                // Ставим блок
                level.setBlock(pos, com.hbm_m.block.ModBlocks.NUCLEAR_FALLOUT.get().defaultBlockState(), 3);
            } catch (Exception ignored) {}
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

    private static boolean isEntityExposed(ServerLevel level, Vec3 startPos, LivingEntity entity) {
        Vec3 currentPos = startPos;
        Vec3 targetPos = entity.getEyePosition();
        Vec3 direction = targetPos.subtract(startPos).normalize();
        double maxDistSq = startPos.distanceToSqr(targetPos);

        // Ограничитель итераций, чтобы не зависнуть в бесконечном цикле
        int safetyLoopCount = 0;

        while (safetyLoopCount++ < 50) {
            // Если мы уже прошли точку цели (или очень близко), значит препятствий нет
            if (currentPos.distanceToSqr(startPos) >= maxDistSq) {
                return true;
            }

            ClipContext context = new ClipContext(
                    currentPos,
                    targetPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    entity
            );

            BlockHitResult result = level.clip(context);

            // Если ничего не задели — путь чист
            if (result.getType() == HitResult.Type.MISS) {
                return true;
            }

            // Если задели блок, проверяем его защиту
            BlockPos hitPos = result.getBlockPos();
            BlockState hitState = level.getBlockState(hitPos);

            // Получаем кастомную защиту блока
            float defense = BlockExplosionDefense.getBlockDefenseValue(level, hitPos, hitState);

            // УСЛОВИЕ: Если защита >= 50, то это надежное укрытие -> урон не проходит
            if (defense >= 50.0F) {
                return false;
            }

            // Если защита < 50 (стекло, листва, забор), мы "пробиваем" блок.
            // Смещаем позицию луча чуть дальше точки попадания по вектору движения
            currentPos = result.getLocation().add(direction.scale(0.1));
        }

        return true; // Если цикл кончился (редкий случай), считаем, что задело
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

        // Радиусы для новых зон (синхронизировано с applyDynamicDamageZones)
        double zone5Radius = zone4Radius + 12.0;
        double zone6Radius = zone4Radius + 24.0;

        // Урон для зон
        float ZONE_5_DAMAGE = 500.0F;
        float ZONE_6_DAMAGE = 100.0F;

        Vec3 explosionCenter = new Vec3(centerX + 0.5, centerY + 0.5, centerZ + 0.5);

        // Используем самую большую зону (6) для поиска всех сущностей разом, чтобы не делать 4 поиска
        // zone6Radius + запас (например, 5 блоков), чтобы точно зацепить хитбоксы на границе
        double maxSearchRadius = zone6Radius + 5.0;

        AABB searchArea = new AABB(
                centerX - maxSearchRadius, centerY - DAMAGE_ZONE_HEIGHT, centerZ - maxSearchRadius,
                centerX + maxSearchRadius, centerY + DAMAGE_ZONE_HEIGHT, centerZ + maxSearchRadius
        );

        List<LivingEntity> allEntities = level.getEntitiesOfClass(LivingEntity.class, searchArea);

        // Квадраты радиусов для быстрой проверки дистанции (оптимизация)
        double r3Sq = zone3Radius * zone3Radius;
        double r4Sq = zone4Radius * zone4Radius;
        double r5Sq = zone5Radius * zone5Radius;
        double r6Sq = zone6Radius * zone6Radius;

        for (LivingEntity entity : allEntities) {
            // Считаем дистанцию до сущности (только горизонтальную, или полную - зависит от вашей логики)
            // Обычно для ядерного взрыва лучше использовать 2D дистанцию (x/z), так как он идет столбом
            // Но здесь используем 3D дистанцию от центра, как в оригинальном методе knockback
            double distSq = entity.distanceToSqr(explosionCenter);

            // Проверяем, видит ли взрыв сущность через твердые блоки (>50 def)
            if (!isEntityExposed(level, explosionCenter, entity)) {
                continue; // Сущность в надежном укрытии
            }

            if (distSq <= r3Sq) {
                // === ZONE 3 ===
                entity.hurt(level.damageSources().generic(), ZONE_3_DAMAGE);
                entity.setSecondsOnFire((int) FIRE_DURATION / 20);
                applyExplosionKnockback(entity, centerPos, zone3Radius);

            } else if (distSq <= r4Sq) {
                // === ZONE 4 ===
                entity.hurt(level.damageSources().generic(), ZONE_4_DAMAGE);
                entity.setSecondsOnFire((int) FIRE_DURATION / 20);
                applyExplosionKnockback(entity, centerPos, zone4Radius);

            } else if (distSq <= r5Sq) {
                // === ZONE 5 ===
                entity.hurt(level.damageSources().generic(), ZONE_5_DAMAGE);
                entity.setSecondsOnFire((int) (FIRE_DURATION * 0.5) / 20); // Меньше огня
                applyExplosionKnockback(entity, centerPos, zone5Radius);

            } else if (distSq <= r6Sq) {
                // === ZONE 6 ===
                entity.hurt(level.damageSources().generic(), ZONE_6_DAMAGE);
                // В 6 зоне не поджигаем, только урон и толчок
                applyExplosionKnockback(entity, centerPos, zone6Radius);
            }
        }
    }

    private static void cleanupItems(ServerLevel level, BlockPos center, double radius) {
        // Создаем коробку поиска (AABB)
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(center).inflate(radius, 100, radius); // 100 высота

        // Находим все предметы (дроп)
        List<net.minecraft.world.entity.item.ItemEntity> items = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area);

        for (net.minecraft.world.entity.item.ItemEntity item : items) {
            item.discard(); // Удаляем
        }
    }
}