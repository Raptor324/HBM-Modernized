package com.hbm_m.client.render;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.ArrayList;
import java.util.IdentityHashMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.client.render.machine.MachineBer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
public final class NucleusDispatcherBypass {

    private static final boolean KILL_SWITCH =
            !"false".equalsIgnoreCase(System.getProperty("hbm.dispatcherBypass", "true"));

    public static final class ChunkBucket {
        public final long chunkKey;
        public final int chunkX;
        public final int chunkZ;
        public final ArrayList<BlockEntity> machines = new ArrayList<>(16);

        private double minX, maxX;
        private double minZ, maxZ;
        private double minY = Double.MAX_VALUE;
        private double maxY = -Double.MAX_VALUE;
        private AABB cachedAabb = null;
        private boolean boundsDirty = true;
        private long lastBoundsTick = -1L;
        private static final int BOUNDS_REFRESH_TICKS = 20;

        public ChunkBucket(long chunkKey, int chunkX, int chunkZ) {
            this.chunkKey = chunkKey;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            resetBaseBounds();
        }

        private void resetBaseBounds() {
            this.minX = (double) (chunkX << 4);
            this.maxX = this.minX + 16.0;
            this.minZ = (double) (chunkZ << 4);
            this.maxZ = this.minZ + 16.0;
            this.minY = Double.MAX_VALUE;
            this.maxY = -Double.MAX_VALUE;
            this.boundsDirty = true;
        }

        public void add(BlockEntity be) {
            machines.add(be);
            if (!boundsDirty) {
                expandBoundsOnly(be);
                commitCachedAabb();
            }
        }

        public void remove(BlockEntity be) {
            machines.remove(be);
            boundsDirty = true;
        }

        public boolean isEmpty() {
            return machines.isEmpty();
        }

        public void markBoundsDirty() {
            this.boundsDirty = true;
        }

        private void expandBoundsOnly(BlockEntity be) {
            AABB box = RenderBoundsCache.get(be);
            if (box != null) {
                minX = Math.min(minX, box.minX);
                maxX = Math.max(maxX, box.maxX);
                minY = Math.min(minY, box.minY);
                maxY = Math.max(maxY, box.maxY);
                minZ = Math.min(minZ, box.minZ);
                maxZ = Math.max(maxZ, box.maxZ);
            } else {
                BlockPos p = be.getBlockPos();
                minX = Math.min(minX, p.getX() - 1.0);
                maxX = Math.max(maxX, p.getX() + 2.0);
                minY = Math.min(minY, p.getY() - 1.0);
                maxY = Math.max(maxY, p.getY() + 3.0);
                minZ = Math.min(minZ, p.getZ() - 1.0);
                maxZ = Math.max(maxZ, p.getZ() + 2.0);
            }
        }

        private void commitCachedAabb() {
            double finalMinY = (minY > maxY) ? -64.0 : minY;
            double finalMaxY = (minY > maxY) ? 320.0 : maxY;
            cachedAabb = new AABB(minX, finalMinY, minZ, maxX, finalMaxY, maxZ);
            boundsDirty = false;
        }

        public void recomputeBounds() {
            resetBaseBounds();
            for (int i = 0; i < machines.size(); i++) {
                expandBoundsOnly(machines.get(i));
            }
            commitCachedAabb();
        }

        public AABB getBounds() {
            var lvl = Minecraft.getInstance().level;
            long tick = lvl != null ? lvl.getGameTime() : 0L;
            return getBounds(tick);
        }

        public AABB getBounds(long tick) {
            if (boundsDirty || (tick - lastBoundsTick >= BOUNDS_REFRESH_TICKS)) {
                recomputeBounds();
                lastBoundsTick = tick;
            }
            return cachedAabb;
        }

        public double distSq(Vec3 cam) {
            return getBounds().distanceToSqr(cam);
        }

        public double distSq(Vec3 cam, long tick) {
            return getBounds(tick).distanceToSqr(cam);
        }

        public double distSq2D(Vec3 cam) {
            AABB b = getBounds();
            double dx = Math.max(Math.max(b.minX - cam.x, cam.x - b.maxX), 0.0);
            double dz = Math.max(Math.max(b.minZ - cam.z, cam.z - b.maxZ), 0.0);
            return dx * dx + dz * dz;
        }
    }

    private static final Long2ObjectOpenHashMap<ChunkBucket> CHUNK_BUCKETS = new Long2ObjectOpenHashMap<>(64);
    private static final IdentityHashMap<BlockEntity, ChunkBucket> BE_TO_BUCKET = new IdentityHashMap<>(256);
    private static final Long2ObjectOpenHashMap<BlockEntity> POS_TO_BE = new Long2ObjectOpenHashMap<>(256);

    private static final java.util.concurrent.ConcurrentLinkedQueue<BlockEntity> PENDING_ADD =
            new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final java.util.concurrent.ConcurrentLinkedQueue<BlockEntity> PENDING_REMOVE =
            new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static long presenceSweepCounter = 0L;

    private static boolean mainCollected = false;
    private static boolean inShadowBypassLoop = false;
    private static final PoseStack NEWCOMER_POSE_SCRATCH = new PoseStack();
    private static final Matrix4f NEWCOMER_BASE_SCRATCH = new Matrix4f();
    private static final Matrix3f NORMAL_SCRATCH = new Matrix3f();
    private static final Matrix4f BASE_SCRATCH = new Matrix4f();
    private static final PoseStack POSE_SCRATCH = new PoseStack();

    private static final java.util.Map<BlockEntityType<?>, MachineBer<?>> BER_FAST_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    static {
        com.hbm_m.client.render.cache.RenderCacheManager.register(reason -> {
            if (reason == com.hbm_m.client.render.cache.RenderCacheManager.Reason.SESSION_END) {
                clearPresence();
            } else {
                refreshPresenceOnReload();
            }
        });
    }

    private NucleusDispatcherBypass() {}

    /**
     * Single authoritative liveness check for a BlockEntity in the active client world.
     * Contraption virtual worlds are exempt from the ClientLevel identity check.
     */
    public static boolean isLiveInClientWorld(@Nullable BlockEntity be) {
        if (be == null || be.isRemoved()) {
            return false;
        }
        Level lvl = be.getLevel();
        if (lvl == null || !lvl.isClientSide()) {
            return false;
        }
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
            return true;
        }
        ClientLevel activeLevel = Minecraft.getInstance().level;
        return lvl == activeLevel && activeLevel.getBlockEntity(be.getBlockPos()) == be;
    }

    /**
     * Authoritative check whether a live managed machine is currently registered at {@code posKey}.
     * Used by InstancedStaticPartRenderer during flush to self-heal any orphaned GPU slots.
     */
    public static boolean isPositionLive(long posKey) {
        BlockEntity be = POS_TO_BE.get(posKey);
        return be != null && !be.isRemoved();
    }

    public static void markMachineBoundsDirty(BlockEntity be) {
        if (be == null) return;
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            Minecraft.getInstance().execute(() -> markMachineBoundsDirty(be));
            return;
        }
        ChunkBucket bucket = BE_TO_BUCKET.get(be);
        if (bucket != null) {
            bucket.markBoundsDirty();
        }
    }

    public static void evictChunk(long chunkKey) {
        ChunkBucket bucket = CHUNK_BUCKETS.remove(chunkKey);
        if (bucket == null) return;
        for (int i = bucket.machines.size() - 1; i >= 0; i--) {
            BlockEntity be = bucket.machines.get(i);
            RenderBoundsCache.remove(be);
            BE_TO_BUCKET.remove(be);
            if (be instanceof NucleusTrackedBlockEntity tracked) {
                tracked.hbm_m$setNucleusRegistered(false);
                tracked.hbm_m$setRenderState(null);
            }
            long posKey = be.getBlockPos().asLong();
            POS_TO_BE.remove(posKey, be);
            dropMachineCaches(be.getType(), posKey, be);
        }
        bucket.machines.clear();
    }

    public static void markAllMachinesDirty() {
        for (BlockEntity be : BE_TO_BUCKET.keySet()) {
            if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker && !be.isRemoved()) {
                tracker.markRenderDirty();
            }
        }
    }

    public static void addMachine(BlockEntity be) {
        if (!isLiveInClientWorld(be)) return;
        if (BE_TO_BUCKET.containsKey(be)) return;
        BlockPos pos = be.getBlockPos();
        long posKey = pos.asLong();
        LightSampleCache.invalidatePosition(posKey);

        // Evict any previous machine at the same position first
        BlockEntity previous = POS_TO_BE.get(posKey);
        if (previous != null && previous != be) {
            removeMachine(previous);
        }

        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        long key = ChunkPos.asLong(cx, cz);
        ChunkBucket bucket = CHUNK_BUCKETS.computeIfAbsent(key, k -> new ChunkBucket(k, cx, cz));
        bucket.add(be);
        BE_TO_BUCKET.put(be, bucket);
        POS_TO_BE.put(posKey, be);
        if (be instanceof NucleusTrackedBlockEntity tracked) {
            tracked.hbm_m$setNucleusRegistered(true);
        }
    }

    public static void removeMachine(BlockEntity be) {
        if (be == null) return;
        RenderBoundsCache.remove(be);
        ChunkBucket bucket = BE_TO_BUCKET.remove(be);
        if (bucket != null) {
            bucket.remove(be);
            if (bucket.isEmpty()) {
                CHUNK_BUCKETS.remove(bucket.chunkKey);
            }
        }
        if (be instanceof NucleusTrackedBlockEntity tracked) {
            tracked.hbm_m$setNucleusRegistered(false);
            tracked.hbm_m$setRenderState(null);
        }
        long posKey = be.getBlockPos().asLong();
        POS_TO_BE.remove(posKey, be);
        LightSampleCache.invalidatePosition(posKey);
        dropMachineCaches(be.getType(), posKey, be);
    }

    public static void onMachineAdded(BlockEntity be) {
        if (be == null || be.isRemoved() || be.getLevel() == null || !be.getLevel().isClientSide()) {
            return;
        }
        if (!isEnabled() || !isManagedType(be.getType())) {
            return;
        }
        PENDING_ADD.add(be);
    }

    public static void onMachineRemoved(BlockEntity be) {
        if (be == null || be.getLevel() == null || !be.getLevel().isClientSide()) {
            return;
        }
        if (be instanceof com.hbm_m.interfaces.IMultiblockPart part && part.getControllerPos() != null) {
            BlockPos corePos = part.getControllerPos();
            if (!corePos.equals(be.getBlockPos())) {
                onMachineControllerRemoved(be.getLevel(), corePos);
            }
        }
        if (!isManagedType(be.getType())) {
            return;
        }
        PENDING_REMOVE.add(be);
        long posKey = be.getBlockPos().asLong();
        if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            removeMachine(be);
        } else {
            Minecraft.getInstance().execute(() -> {
                removeMachine(be);
                dropMachineCaches(be.getType(), posKey, be);
            });
        }
    }

    public static void onDoorSkinChanged(BlockEntity door) {
        if (door == null) return;
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            Minecraft.getInstance().execute(() -> onDoorSkinChanged(door));
            return;
        }
        long posKey = door.getBlockPos().asLong();
        MachineBer<?> ber = BER_FAST_CACHE.get(door.getType());
        if (ber != null) {
            ber.dropPerPositionCaches(posKey);
        }
        markMachineBoundsDirty(door);
    }

    public static void onMachineControllerRemoved(Level level, BlockPos corePos) {
        if (corePos == null || level == null || !level.isClientSide()) {
            return;
        }
        Runnable task = () -> {
            long coreKey = corePos.asLong();
            BlockEntity trackedBe = POS_TO_BE.get(coreKey);
            if (trackedBe != null) {
                removeMachine(trackedBe);
                return;
            }
            for (MachineBer<?> ber : snapshotBers()) {
                ber.dropPerPositionCaches(coreKey);
            }
            for (com.hbm_m.client.render.machine.MachineSpec<?> spec : com.hbm_m.client.render.machine.MachineRenderRegistry.specs()) {
                if (spec != null) {
                    spec.freeMachineSlots(coreKey);
                }
            }
        };
        if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            task.run();
        } else {
            Minecraft.getInstance().execute(task);
        }
    }

    private static void processPresenceQueue(Minecraft mc) {
        BlockEntity be;
        while ((be = PENDING_REMOVE.poll()) != null) {
            removeMachine(be);
        }
        while ((be = PENDING_ADD.poll()) != null) {
            if (isLiveInClientWorld(be) && isManaged(be)) {
                addMachine(be);
            }
        }
        if ((presenceSweepCounter++ & 127) == 0 && !BE_TO_BUCKET.isEmpty()) {
            ArrayList<BlockEntity> snapshot = new ArrayList<>(BE_TO_BUCKET.keySet());
            for (int i = 0; i < snapshot.size(); i++) {
                BlockEntity b = snapshot.get(i);
                if (!isLiveInClientWorld(b) || !isManaged(b)) {
                    removeMachine(b);
                }
            }
        }
    }

    public static void registerManaged(BlockEntityType<?> type) {
        ((NucleusManagedType) type).hbm_m$setNucleusManaged(true);
    }

    public static void registerBer(BlockEntityType<?> type, MachineBer<?> ber) {
        BER_FAST_CACHE.put(type, ber);
    }

    public static void dropMachineCaches(BlockEntityType<?> type, long posKey, @Nullable BlockEntity be) {
        if (type != null) {
            MachineBer<?> ber = BER_FAST_CACHE.get(type);
            if (ber == null && be != null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.getBlockEntityRenderDispatcher() != null) {
                    BlockEntityRenderer<?> r = mc.getBlockEntityRenderDispatcher().getRenderer(be);
                    if (r instanceof MachineBer<?> mb) {
                        ber = mb;
                        BER_FAST_CACHE.put(type, mb);
                    }
                }
            }
            if (ber != null) {
                ber.dropPerPositionCaches(posKey);
                return;
            }
        }
        for (com.hbm_m.client.render.machine.MachineSpec<?> spec : com.hbm_m.client.render.machine.MachineRenderRegistry.specs()) {
            if (spec != null) {
                spec.freeMachineSlots(posKey);
            }
        }
    }

    public static void clearBerCache() {
        BER_FAST_CACHE.clear();
    }

    public static java.util.List<MachineBer<?>> snapshotBers() {
        return new java.util.ArrayList<>(BER_FAST_CACHE.values());
    }

    public static void clearPresence() {
        for (BlockEntity be : BE_TO_BUCKET.keySet()) {
            if (be instanceof NucleusTrackedBlockEntity tracked) {
                tracked.hbm_m$setNucleusRegistered(false);
                tracked.hbm_m$setRenderState(null);
            }
        }
        CHUNK_BUCKETS.clear();
        BE_TO_BUCKET.clear();
        POS_TO_BE.clear();
        PENDING_ADD.clear();
        PENDING_REMOVE.clear();
        mainCollected = false;
        com.hbm_m.client.render.retained.RetainedInstanceGraph.get().clear();
        for (MachineBer<?> ber : snapshotBers()) {
            ber.clearRosterCache();
        }
    }

    public static void refreshPresenceOnReload() {
        for (BlockEntity be : BE_TO_BUCKET.keySet()) {
            if (be instanceof NucleusTrackedBlockEntity tracked) {
                tracked.hbm_m$setRenderState(null);
            }
            if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker && !be.isRemoved()) {
                tracker.markRenderDirty();
            }
        }
        for (ChunkBucket bucket : CHUNK_BUCKETS.values()) {
            bucket.markBoundsDirty();
        }
        mainCollected = false;
        for (MachineBer<?> ber : snapshotBers()) {
            ber.clearRosterCache();
        }
    }

    static boolean isEnabled() {
        return com.hbm_m.config.ModClothConfig.get().nucleusDispatcherBypass && KILL_SWITCH
                && !ClientRenderFlags.forceVanillaImmediate()
                && ClientRenderFlags.useInstancedBatching();
    }

    public static void noteMainFrameStart() {
        mainCollected = false;
    }

    public static boolean shouldBypass(BlockEntity be, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffers) {
        if (!isEnabled() || !isManaged(be)) {
            return false;
        }
        if (com.hbm_m.compat.simulated.DiagramRenderCompat.isRenderingDiagram()) {
            return false;
        }
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
            return false;
        }
        // Centralized liveness invariant: if a stale compiled chunk section or globalBlockEntities
        // dispatches a removed/replaced BlockEntity, evict it and cancel the draw immediately.
        // Never allow it to reach the newcomer path below and re-allocate a zombie GPU slot!
        if (!isLiveInClientWorld(be)) {
            removeMachine(be);
            return true;
        }
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass()) {
            if (inShadowBypassLoop) {
                return false;
            }
            IrisShadowBatchCollector.ensureAnchorSpaceShadowFromBePose(poseStack.last().pose(), be.getBlockPos());
            MachineBer<?> ber = BER_FAST_CACHE.get(be.getType());
            return ber != null && ber.tryConsumePrebakedShadow(be);
        }
        boolean known = (be instanceof NucleusTrackedBlockEntity tracked)
                ? tracked.hbm_m$isNucleusRegistered()
                : BE_TO_BUCKET.containsKey(be);
        if (!known) {
            addMachine(be);
        }
        if (!mainCollected) {
            return false;
        }
        if (known) {
            return true;
        }
        NEWCOMER_POSE_SCRATCH.last().pose().identity();
        NEWCOMER_POSE_SCRATCH.last().normal().identity();
        collectOne(be, partialTick, NEWCOMER_POSE_SCRATCH, buffers, bypassBase(poseStack));
        return true;
    }

    public static void collectMain(PoseStack levelPose, MultiBufferSource buffers,
                                   float partialTick) {
        collectMain(levelPose, buffers, partialTick, null);
    }

    private static @Nullable ClientLevel lastLevel = null;

    public static void collectMain(PoseStack levelPose, MultiBufferSource buffers,
                                   float partialTick, @Nullable Frustum frustum) {
        if (!isEnabled() || mainCollected) {
            return;
        }
        if (com.hbm_m.compat.simulated.DiagramRenderCompat.isRenderingDiagram()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (lastLevel != null) {
                clearPresence();
                lastLevel = null;
            }
            return;
        }
        if (lastLevel != mc.level) {
            clearPresence();
            lastLevel = mc.level;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        processPresenceQueue(mc);

        Matrix4f base = BASE_SCRATCH.set(levelPose.last().pose());
        PoseStack pose = POSE_SCRATCH;
        long gameTime = mc.level.getGameTime();

        double maxDistBlock = Math.max(RenderDistanceHelper.getStaticDistanceBlocks(),
                RenderDistanceHelper.getAnimatedDistanceBlocks());
        double maxDist = maxDistBlock + 24.0;
        double maxDistSq = maxDist * maxDist;

        double maxDistCap = maxDistBlock + 8.0;
        double maxDistCapSq = maxDistCap * maxDistCap;

        boolean gpuCullCoversMain = com.hbm_m.client.render.culling.GpuCullCoverage.isActive()
                || (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()
                    && NucleusGpuBaker.isEnabled());

        var bucketIter = CHUNK_BUCKETS.values().iterator();
        while (bucketIter.hasNext()) {
            ChunkBucket bucket = bucketIter.next();
            if (bucket.isEmpty()) {
                bucketIter.remove();
                continue;
            }

            AABB bucketBounds = bucket.getBounds(gameTime);
            if (bucketBounds.distanceToSqr(cam) > maxDistSq) continue;
            if (!gpuCullCoversMain && frustum != null && !frustum.isVisible(bucketBounds)) {
                continue;
            }

            ArrayList<BlockEntity> list = bucket.machines;
            for (int i = 0; i < list.size(); i++) {
                BlockEntity be = list.get(i);
                if (!isLiveInClientWorld(be)) {
                    removeMachine(be);
                    i--;
                    continue;
                }

                AABB beBox = null;
                if (!gpuCullCoversMain && frustum != null) {
                    beBox = RenderBoundsCache.get(be);
                    if (beBox != null && !frustum.isVisible(beBox)) {
                        continue;
                    }
                }

                collectOne(be, beBox, partialTick, pose, buffers, base, cam, mc, gameTime, maxDistCapSq);
            }
        }
        mainCollected = true;
    }

    private static Matrix4f bypassBase(PoseStack dispatcherPose) {
        NEWCOMER_BASE_SCRATCH.set(dispatcherPose.last().pose());
        NEWCOMER_BASE_SCRATCH.m30(0f).m31(0f).m32(0f);
        return NEWCOMER_BASE_SCRATCH;
    }

    private static boolean isManaged(BlockEntity be) {
        return be != null && be.getLevel() != null
                && be.getLevel() == Minecraft.getInstance().level
                && isManagedType(be.getType());
    }

    public static boolean isBypassActive() {
        return isEnabled();
    }

    public static boolean isManagedType(BlockEntityType<?> type) {
        if (type == null) return false;
        return (type instanceof NucleusManagedType duck && duck.hbm_m$isNucleusManaged())
                || com.hbm_m.client.render.machine.MachineRenderRegistry.isManaged(type);
    }

    private static void collectOne(BlockEntity be, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, Matrix4f base) {
        if (!isLiveInClientWorld(be)) {
            removeMachine(be);
            return;
        }
        double maxDistBlock = Math.max(RenderDistanceHelper.getStaticDistanceBlocks(),
                RenderDistanceHelper.getAnimatedDistanceBlocks());
        double maxDistCap = maxDistBlock + 8.0;
        collectOne(be, RenderBoundsCache.get(be), partialTick, pose, buffers, base,
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),
                Minecraft.getInstance(),
                Minecraft.getInstance().level.getGameTime(),
                maxDistCap * maxDistCap);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collectOne(BlockEntity be, @Nullable AABB beBox, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, Matrix4f base,
                                   Vec3 cam, Minecraft mc, long gameTime, double maxDistCapSq) {
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
            return;
        }
        BlockPos pos = be.getBlockPos();
        double dx = pos.getX() - cam.x;
        double dy = pos.getY() - cam.y;
        double dz = pos.getZ() - cam.z;
        double cdx = dx + 0.5;
        double cdy = dy + 0.5;
        double cdz = dz + 0.5;
        double centerDistSq = cdx * cdx + cdy * cdy + cdz * cdz;
        double distSq = (beBox != null) ? beBox.distanceToSqr(cam) : centerDistSq;
        if (distSq > maxDistCapSq) {
            return;
        }

        MachineBer<?> machineBer = BER_FAST_CACHE.get(be.getType());
        if (machineBer == null) {
            BlockEntityRenderer<?> ber = mc.getBlockEntityRenderDispatcher().getRenderer(be);
            if (ber instanceof MachineBer<?> mb) {
                machineBer = mb;
                BER_FAST_CACHE.put(be.getType(), mb);
            } else {
                return;
            }
        }
        if (machineBer.tryFastAssertRender(be, gameTime, centerDistSq)) {
            return;
        }
        pose.last().pose().set(base);
        NORMAL_SCRATCH.set(base);
        pose.last().normal().set(NORMAL_SCRATCH);
        pose.translate((float) dx, (float) dy, (float) dz);
        int packedLight = com.hbm_m.client.render.LightSampleCache.getOrSamplePacked(be, 0);
        try {
            ((MachineBer<BlockEntity>) machineBer).collectRender(
                    be, partialTick, pose, buffers, packedLight, true);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error(
                    "[HBM-M] NucleusDispatcherBypass: collect failed for {}", pos, t);
            if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker) {
                tracker.markRenderDirty();
            }
        }
    }

    public static int renderShadowBlockEntities(PoseStack modelView,
                                                MultiBufferSource.BufferSource buffers,
                                                double camX, double camY, double camZ,
                                                float tickDelta,
                                                boolean hasEntityFrustum,
                                                boolean lightsOnly) {
        if (!isEnabled() || CHUNK_BUCKETS.isEmpty()) {
            return 0;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 0;
        }
        BlockEntityRenderDispatcher dispatcher = mc.getBlockEntityRenderDispatcher();
        long gameTime = mc.level.getGameTime();
        Vec3 cam = new Vec3(camX, camY, camZ);
        IrisShadowBatchCollector.beginShadowPass(
                modelView.last().pose(), com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix(), camX, camY, camZ);
        if (!lightsOnly) {
            IrisShadowBatchCollector.activateAnchorSpacePrebakes();
        }
        double maxDistBlock = RenderDistanceHelper.getStaticDistanceBlocks() + 24.0;
        double maxDistSq = maxDistBlock * maxDistBlock;

        int count = 0;
        for (ChunkBucket bucket : CHUNK_BUCKETS.values()) {
            if (bucket.isEmpty()) {
                continue;
            }
            if (bucket.distSq(cam, gameTime) > maxDistSq) {
                continue;
            }
            ArrayList<BlockEntity> list = bucket.machines;
            for (int i = 0; i < list.size(); i++) {
                BlockEntity be = list.get(i);
                if (!isLiveInClientWorld(be)) {
                    continue;
                }
                if (lightsOnly && be.getBlockState().getLightEmission() == 0) {
                    continue;
                }
                if (!com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
                    MachineBer<?> ber = BER_FAST_CACHE.get(be.getType());
                    if (ber != null && ber.tryConsumePrebakedShadow(be)) {
                        count++;
                        continue;
                    }
                }
                BlockPos pos = be.getBlockPos();
                modelView.pushPose();
                inShadowBypassLoop = true;
                try {
                    modelView.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
                    dispatcher.render(be, tickDelta, modelView, buffers);
                } finally {
                    inShadowBypassLoop = false;
                    modelView.popPose();
                }
                count++;
            }
        }
        return count;
    }
}
