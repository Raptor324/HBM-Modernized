package com.hbm_m.client.render;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.hbm_m.client.render.machine.MachineBer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
/**
 * <b>BlockEntity dispatcher bypass in the MAIN pass</b> for Nucleus engine
 * machines ({@code MachineRenderers}) — and ONLY for them; all other mod BEs
 * go through the normal path.
 * <p>
 * The shadow pass was historically NOT bypassed (profile 0914 04:04: a naive skip
 * broke shadows). SHADOW-EPOCH (2026-10-07) now replaces the BER for provably
 * covered machines only: records are produced in MAIN and the shadow pass
 * consumes them through a per-machine fail-closed gate - see
 * {@code IrisShadowBatchCollector} (SHADOW-EPOCH section) for the mechanism.
 * <p>
 * Scheme:
 * <ol>
 *   <li>{@code collectMain} is called explicitly at {@code AFTER_ENTITIES} —
 *       BEFORE Embeddium's BE iteration: flat loop over {@link #LIVE},
 *       pose = eventPose * T(be-cam); the {@code mainCollected} flag is set
 *       EARLY.</li>
 *   <li>Dispatcher mixin in main: managed BEs are cancelled (already drawn),
 *       newcomers are registered in {@link #LIVE} (drawn from the next frame;
 *       to avoid flicker a newcomer is rendered singly right away — see
 *       shouldBypass).</li>
 *   <li>Shadow pass: machines whose main-pass record set is complete and calibrated
 *       consume prebaked shadow records without running the BER (SHADOW-EPOCH,
 *       {@code IrisShadowBatchCollector.consumePrebaked}); everything else -
 *       newcomers, main-frustum outsides, hook/immediate-part machines - takes the
 *       normal shadow path (fail-closed fallback, also calibrates the prebake).</li>
 * </ol>
 * Unloading is handled by pruning via {@code be.isRemoved()/level} on every
 * collectMain. Kill-switch: {@code -Dhbm.dispatcherBypass=false}.
 */
public final class NucleusDispatcherBypass {

    /** Emergency JVM override -Dhbm.dispatcherBypass=false; primary source is the nucleusDispatcherBypass config. */
    private static final boolean KILL_SWITCH =
            !"false".equalsIgnoreCase(System.getProperty("hbm.dispatcherBypass", "true"));

    // BE types managed by the MachineRenderers factory (registerManaged at spec
    // registration) carry a boolean flag via the NucleusManagedType duck
    // interface (client.MixinBlockEntityType) - a per-BE-per-frame HashSet
    // probe (1.23% of the frame on high-BE scenes per the profile) became a
    // field read. Type instances are stable under DeferredRegister, so no
    // removal handling is needed.

    /**
     * Spatial chunk bucket partitioning for Nucleus machines.
     * Contains conservative world-space bounding box enclosing all machines in this chunk.
     */
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
        private static final int BOUNDS_REFRESH_TICKS = 20; // 1 second safety refresh

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
            // else: a remove() shrank the machine list but not min/max - committing
            // the cached AABB here would clear boundsDirty over bounds that still
            // enclose the removed machine (over-wide culling box until the 20-tick
            // forced refresh). Leave the flag set: getBounds() recomputes lazily.
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

        /** Accumulates bounds into primitive min/max without allocating intermediate AABB. */
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

        /**
         * Squared distance from camera to the closest point of the expanded AABB.
         * Prevents premature culling of multiblocks extending towards the camera across chunk boundaries.
         */
        public double distSq(Vec3 cam) {
            return getBounds().distanceToSqr(cam);
        }

        public double distSq(Vec3 cam, long tick) {
            return getBounds(tick).distanceToSqr(cam);
        }

        /** 2D horizontal clamped distance variant (ignoring vertical altitude). */
        public double distSq2D(Vec3 cam) {
            AABB b = getBounds();
            double dx = Math.max(Math.max(b.minX - cam.x, cam.x - b.maxX), 0.0);
            double dz = Math.max(Math.max(b.minZ - cam.z, cam.z - b.maxZ), 0.0);
            return dx * dx + dz * dz;
        }
    }

    /** Spatial chunk bucket index keyed by ChunkPos.asLong(chunkX, chunkZ). */
    private static final Long2ObjectOpenHashMap<ChunkBucket> CHUNK_BUCKETS = new Long2ObjectOpenHashMap<>(64);
    /** Reverse lookup mapping BlockEntity -> ChunkBucket for O(1) removal. */
    private static final IdentityHashMap<BlockEntity, ChunkBucket> BE_TO_BUCKET = new IdentityHashMap<>(256);

    /**
     * Tombstones for demolished multiblock controllers.
     * Prevents client-side resurrection race conditions where the controller BE is still
     * alive in client world storage while the casing break cascade is executing.
     * Synchronized: written from chunk-apply AND render threads (read via isDemolished
     * from the retained-graph paths).
     */
     private static final it.unimi.dsi.fastutil.longs.LongOpenHashSet DEMOLISHED_CONTROLLERS =
             new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
     private static final Object DEMOLISHED_LOCK = new Object();
     /** Fast-path gate: tombstones are rare (only after a multiblock demolition),
      * so collectMain's per-machine query takes a plain volatile read 99.99% of
      * the time instead of a synchronized lock (0.69 ms/frame in spark). */
     private static volatile boolean hasDemolishedControllers = false;

     /** Tombstone query for the retained-graph registration paths (thread-safe, alloc-free). */
     public static boolean isDemolished(long posKey) {
         if (!hasDemolishedControllers) {
             return false;
         }
         synchronized (DEMOLISHED_LOCK) {
             return DEMOLISHED_CONTROLLERS.contains(posKey);
         }
     }

    /** Marks the chunk bucket enclosing this machine dirty for bounds re-evaluation. */
    public static void markMachineBoundsDirty(BlockEntity be) {
        if (be == null) return;
        ChunkBucket bucket = BE_TO_BUCKET.get(be);
        if (bucket != null) {
            bucket.markBoundsDirty();
        }
    }

    public static void addMachine(BlockEntity be) {
        if (be == null || be.isRemoved()) return;
        if (isDemolished(be.getBlockPos().asLong())) {
            com.hbm_m.multiblock.TeardownTrace.trace(
                    "addMachine: REFUSED - pos is tombstoned (resurrection blocked) pos={} type={}",
                    be.getBlockPos().toShortString(), be.getType());
            return;
        }
        if (BE_TO_BUCKET.containsKey(be)) return;
        BlockPos pos = be.getBlockPos();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        long key = ChunkPos.asLong(cx, cz);
        ChunkBucket bucket = CHUNK_BUCKETS.get(key);
        if (bucket == null) {
            bucket = new ChunkBucket(key, cx, cz);
            CHUNK_BUCKETS.put(key, bucket);
        }
        bucket.add(be);
        BE_TO_BUCKET.put(be, bucket);
        com.hbm_m.multiblock.TeardownTrace.log("addMachine: registered pos={} type={} bucketMachines={}",
                pos.toShortString(), be.getType(), bucket.machines.size());
        if (be instanceof NucleusTrackedBlockEntity tracked) {
            tracked.hbm_m$setNucleusRegistered(true);
        }
    }

    public static void removeMachine(BlockEntity be) {
        if (be == null) return;
        // Evict immediately - the identity-keyed bounds cache pins the BE (and its
        // Level through it); the amortized sweep only runs every 256 frames now.
        RenderBoundsCache.remove(be);
        ChunkBucket bucket = BE_TO_BUCKET.remove(be);
        com.hbm_m.multiblock.TeardownTrace.log("removeMachine: pos={} type={} wasRegistered={}",
                be.getBlockPos().toShortString(), be.getType(), bucket != null);
        if (bucket != null) {
            bucket.remove(be);
            if (bucket.isEmpty()) {
                CHUNK_BUCKETS.remove(bucket.chunkKey);
            }
        }
        if (be instanceof NucleusTrackedBlockEntity tracked) {
            tracked.hbm_m$setNucleusRegistered(false);
        }
        // Render-thread safe cache eviction
        MachineBer<?> ber = BER_FAST_CACHE.get(be.getType());
        if (ber != null) {
            ber.dropPerPositionCaches(be.getBlockPos().asLong());
        }
        com.hbm_m.client.render.retained.RetainedInstanceGraph.get().onMachineRemoved(be);
    }

    // ── Transactional presence (CrankShaft BlockEntityStorage pattern) ──
    // World-event mixins enqueue add/remove transactions; collectMain drains them
    // at frame start. Presence = vanilla client chunk lifecycle (chunk unload fires
    // BlockEntity.setRemoved), no per-frame range scan. Main-thread only.
    private static final java.util.concurrent.ConcurrentLinkedQueue<BlockEntity> PENDING_ADD =
            new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final java.util.concurrent.ConcurrentLinkedQueue<BlockEntity> PENDING_REMOVE =
            new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static long presenceSweepCounter = 0L;
    /** One-shot: has the "Nucleus disabled" trace been emitted this session? */
    private static boolean NUCLEUS_DISABLED_LOGGED = false;

    /** World-event hook (client {@code LevelChunk.setBlockEntity}): machine placed / chunk loaded. */
    public static void onMachineAdded(BlockEntity be) {
        if (be == null || be.isRemoved() || be.getLevel() == null || !be.getLevel().isClientSide()) {
            return;
        }
        // Tombstone очищается, когда на этой позиции снова легитимно появляется
        // управляемая машина (PENDING_ADD всё равно проверит isDemolished в
        // addMachine). Раньше проверялся только БЛОК-контроллер
        // (IMultiblockController): одиночная машина или дверь, переставленная на
        // место сноса, оставалась затомбстоунной навсегда — addMachine/RIG
        // отказывали в регистрации, а shouldBypass гасил ванильный рендер
        // (невидимость до перезахода в мир). BE, реально доставленный через
        // setBlockEntity, означает, что сервер действительно держит блок на этой
        // позиции — гонка resurrection каскада сноса сюда не попадает.
        if (isDemolished(be.getBlockPos().asLong())) {
            boolean controllerBlock = be.getLevel() != null
                    && be.getLevel().getBlockState(be.getBlockPos()).getBlock() instanceof com.hbm_m.interfaces.IMultiblockController;
            boolean managedArrival = isManagedType(be.getType());
            if (controllerBlock || managedArrival) {
                synchronized (DEMOLISHED_LOCK) {
                    if (DEMOLISHED_CONTROLLERS.remove(be.getBlockPos().asLong())) {
                        hasDemolishedControllers = !DEMOLISHED_CONTROLLERS.isEmpty();
                        com.hbm_m.multiblock.TeardownTrace.trace(
                                "onMachineAdded: legitimate arrival at tombstoned pos {} ({}) - tombstone cleared",
                                be.getBlockPos().toShortString(),
                                controllerBlock ? "controller block" : "managed BE " + be.getType());
                    }
                }
            }
        }
        if (!isEnabled()) {
            // The dispatcher bypass is the ONLY thing feeding the Nucleus presence graph;
            // when disabled, every spec machine (doors, assembler, press, ...) draws via the
            // vanilla per-frame BER path instead. One-time trace: this single line explains
            // why a spec-registered machine can still ghost through vanilla stale lists.
            if (isManagedType(be.getType()) && !NUCLEUS_DISABLED_LOGGED) {
                NUCLEUS_DISABLED_LOGGED = true;
                com.hbm_m.multiblock.TeardownTrace.trace(
                        "onMachineAdded: Nucleus dispatcher DISABLED (config/flags) - spec machines are NOT tracked and render via the vanilla BER path, e.g. {} @ {}",
                        be.getType(), be.getBlockPos().toShortString());
            }
            return;
        }
        if (!isManagedType(be.getType())) {
            return;
        }
        PENDING_ADD.add(be);
        com.hbm_m.client.render.retained.RetainedInstanceGraph.get().onMachineAddedAsync(be);
    }

    /** World-event hook (client {@code BlockEntity.setRemoved}): machine broken / chunk unloaded. */
    public static void onMachineRemoved(BlockEntity be) {
        // Client-level gate: in single-player, MixinBlockEntitySetRemoved intercepts
        // setRemoved() on the integrated server thread for server block entities.
        // We must strictly reject any null or server-side level so only client-side
        // block entities are enqueued into the render-thread presence queue.
        if (be == null || be.getLevel() == null || !be.getLevel().isClientSide()) {
            if (be instanceof com.hbm_m.interfaces.IMultiblockPart) {
                // Integrated-server BE in single-player: the client render must not touch it.
                com.hbm_m.multiblock.TeardownTrace.log(
                        "onMachineRemoved: REJECTED - level is server/null (integrated-server BE), part={} type={}",
                        be == null ? "?" : be.getBlockPos().toShortString(), be == null ? "?" : be.getType());
            }
            return;
        }
        if (be instanceof com.hbm_m.interfaces.IMultiblockPart part && part.getControllerPos() != null) {
            BlockPos corePos = part.getControllerPos();
            if (!corePos.equals(be.getBlockPos())) {
                com.hbm_m.multiblock.TeardownTrace.trace(
                        "onMachineRemoved: part routes to controller eviction, part={} -> core={}",
                        be.getBlockPos().toShortString(), corePos.toShortString());
                onMachineControllerRemoved(be.getLevel(), corePos);
            }
        }
        if (!isManagedType(be.getType()) && !com.hbm_m.client.render.retained.RetainedInstanceGraph.get().isManaged(be)) {
            if (be instanceof com.hbm_m.interfaces.IMultiblockPart) {
                com.hbm_m.multiblock.TeardownTrace.log(
                        "onMachineRemoved: UNMANAGED part type - teardown dropped here, part={} type={}",
                        be.getBlockPos().toShortString(), be.getType());
            }
            return;
        }
        PENDING_REMOVE.add(be);
        long posKey = be.getBlockPos().asLong();
        if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            com.hbm_m.client.render.retained.RetainedInstanceGraph.get().onMachineRemoved(be);
            MachineBer<?> ber = BER_FAST_CACHE.get(be.getType());
            if (ber != null) {
                ber.dropPerPositionCaches(posKey);
            }
        } else {
            com.hbm_m.client.render.retained.RetainedInstanceGraph.get().onMachineRemovedAsync(be);
        }
    }

    /**
     * Dedicated notification when a door skin changes to cleanly drop all cached models
     * and freed GPU instance slots across modern OBJ and legacy DAE variants.
     */
    public static void onDoorSkinChanged(BlockEntity door) {
        if (door == null) return;
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> onDoorSkinChanged(door));
            return;
        }
        long posKey = door.getBlockPos().asLong();
        MachineBer<?> ber = BER_FAST_CACHE.get(door.getType());
        if (ber != null) {
            ber.dropPerPositionCaches(posKey);
        }
        com.hbm_m.client.render.retained.RetainedInstanceGraph.get().onMachineRemoved(posKey);
        markMachineBoundsDirty(door);
    }

    /**
     * Evicts a multiblock controller and its retained GPU instances when a dummy casing or controller is removed.
     */
    public static void onMachineControllerRemoved(net.minecraft.world.level.Level level, BlockPos corePos) {
        if (corePos == null || level == null || !level.isClientSide()) {
            com.hbm_m.multiblock.TeardownTrace.trace(
                    "onMachineControllerRemoved: NO-OP coreNull={} levelNull={} clientSide={}",
                    corePos == null, level == null, level != null && level.isClientSide());
            return;
        }
        long coreKey = corePos.asLong();
        synchronized (DEMOLISHED_LOCK) {
            // Idempotency guard: this fires once per shell block of a multiblock and
            // twice (block onRemove + setRemoved mixin) per player block-break, so a
            // repeat call must not re-run the full eviction pipeline.
            if (!DEMOLISHED_CONTROLLERS.add(coreKey)) {
                com.hbm_m.multiblock.TeardownTrace.log(
                        "onMachineControllerRemoved: tombstone ALREADY present at {} (repeat eviction, idempotent)",
                        corePos.toShortString());
                return;
            }
            hasDemolishedControllers = true;
        }
        com.hbm_m.multiblock.TeardownTrace.trace("onMachineControllerRemoved: BEGIN core={} nucleusEnabled={} thread={}",
                corePos.toShortString(), isEnabled(),
                com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread() ? "render" : "main/other");

        // 1+2. Evict the presence record (BE_TO_BUCKET / CHUNK_BUCKETS / bucket.machines
        // + the synchronous RIG.onMachineRemoved + dropPerPositionCaches inside
        // removeMachine). These structures are iterated by collectMain on the render
        // thread with no synchronization - a direct mutation from the network/main
        // thread is a data race, so off the render thread the eviction is marshalled
        // through the render-thread executor as a single task (the RIG eviction and
        // cache-drop fallback are folded into the same task so their order relative
        // to the presence eviction is guaranteed).
        // removeMachine already runs RIG.onMachineRemoved(be) and
        // ber.dropPerPositionCaches(posKey), so when the controller was found (live BE
        // or bucket entry) the key-based step 3 below would only duplicate that work
        // and is skipped. It only runs as a fallback when nothing was found.
        if (!com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
            com.hbm_m.multiblock.TeardownTrace.log(
                    "onMachineControllerRemoved: NOT on render thread - eviction marshalled to render thread, core={}",
                    corePos.toShortString());
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                if (!evictControllerPresence(level, corePos)) {
                    // Neither a live BE nor a bucket entry: evict by position so the
                    // GPU slots for this key are still freed.
                    for (MachineBer<?> ber : snapshotBers()) {
                        ber.dropPerPositionCaches(coreKey);
                    }
                    com.hbm_m.client.render.retained.RetainedInstanceGraph.get().onMachineRemoved(coreKey, true);
                }
            });
            return;
        }
        if (evictControllerPresence(level, corePos)) {
            return;
        }

        // 3. Fallback: the controller was neither a live BE nor in a bucket, evict
        // from RetainedInstanceGraph by position; dropPerPositionCaches frees the spec
        // slots for this key, so the RIG all-specs sweep is skipped via the flag.
        for (MachineBer<?> ber : snapshotBers()) {
            ber.dropPerPositionCaches(coreKey);
        }
        com.hbm_m.client.render.retained.RetainedInstanceGraph.get().onMachineRemoved(coreKey, true);
    }

    /**
     * Render-thread only (see the marshalling in {@link #onMachineControllerRemoved}).
     *
     * @return true if a controller was found (live BE or bucket entry) and evicted via
     *         {@link #removeMachine} - which already runs RIG.onMachineRemoved and
     *         dropPerPositionCaches for it; false if nothing was found.
     */
    private static boolean evictControllerPresence(net.minecraft.world.level.Level level, BlockPos corePos) {
        // 1. If controller BE is still in chunk, evict normally:
        BlockEntity coreBe = level.getBlockEntity(corePos);
        if (coreBe != null) {
            com.hbm_m.multiblock.TeardownTrace.log("onMachineControllerRemoved: live controller BE at {}, evicting normally",
                    corePos.toShortString());
            removeMachine(coreBe);
            return true;
        }
        // 2. Controller BE already cleared/null: look up in spatial chunk bucket:
        int cx = corePos.getX() >> 4;
        int cz = corePos.getZ() >> 4;
        long chunkKey = net.minecraft.world.level.ChunkPos.asLong(cx, cz);
        ChunkBucket bucket = CHUNK_BUCKETS.get(chunkKey);
        boolean bucketHit = false;
        if (bucket != null) {
            for (int i = 0; i < bucket.machines.size(); i++) {
                BlockEntity m = bucket.machines.get(i);
                if (m.getBlockPos().equals(corePos)) {
                    bucketHit = true;
                    removeMachine(m);
                    break;
                }
            }
        }
        com.hbm_m.multiblock.TeardownTrace.log(
                "onMachineControllerRemoved: controller BE gone from world, bucket scan hit={} (bucket={})",
                bucketHit, bucket != null ? "present, " + bucket.machines.size() + " machines" : "absent");
        return bucketHit;
    }

    /** Drains presence transactions + a low-frequency safety sweep (1/128 frames). */
    private static void processPresenceQueue(Minecraft mc) {
        BlockEntity be;
        while ((be = PENDING_REMOVE.poll()) != null) {
            removeMachine(be);
        }
        while ((be = PENDING_ADD.poll()) != null) {
            if (!be.isRemoved() && be.getLevel() == mc.level && isManaged(be)) {
                addMachine(be);
            }
        }
        // Safety sweep for missed events / dimension switches (was per-frame,
        // now amortized 128x - the mixins own the per-event cleanup).
        if ((presenceSweepCounter++ & 127) == 0) {
            var it = BE_TO_BUCKET.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                BlockEntity b = entry.getKey();
                if (b.isRemoved() || b.getLevel() != mc.level || !isManaged(b)) {
                    com.hbm_m.multiblock.TeardownTrace.log(
                            "safety sweep: evicting stale machine pos={} type={} removed={} wrongLevel={} unmanaged={}",
                            b.getBlockPos().toShortString(), b.getType(), b.isRemoved(),
                            b.getLevel() != mc.level, !isManaged(b));
                    it.remove();
                    entry.getValue().remove(b);
                    if (entry.getValue().isEmpty()) {
                        CHUNK_BUCKETS.remove(entry.getValue().chunkKey);
                    }
                    if (b instanceof NucleusTrackedBlockEntity tracked) {
                        tracked.hbm_m$setNucleusRegistered(false);
                    }
                    MachineBer<?> ber = BER_FAST_CACHE.get(b.getType());
                    if (ber != null) {
                        ber.dropPerPositionCaches(b.getBlockPos().asLong());
                    }
                    com.hbm_m.client.render.retained.RetainedInstanceGraph.get().onMachineRemoved(b);
                }
            }
            // Amortized fail-safe against missed teardown hooks (any target): a
            // retained handle whose BE is gone while its chunk is loaded gets
            // evicted here, so a missed hook costs at most ~128 frames of ghost,
            // never a permanent one. See RetainedInstanceGraph.reconcileStaleHandles.
            com.hbm_m.client.render.retained.RetainedInstanceGraph.get()
                    .reconcileStaleHandles(mc.level);
        }
    }

    private static boolean mainCollected = false;

    /** Scratch: normal matrix part of the basis for the shared PoseStack. */
    private static final Matrix3f NORMAL_SCRATCH = new Matrix3f();
    // collectMain scratch: basis + PoseStack are reused every frame
    // (previously 2 allocations per frame were wasted). Not reentrant: collectOne
    // sets pose/base at the start of each call, no nested collects occur.
    private static final Matrix4f BASE_SCRATCH = new Matrix4f();
    private static final PoseStack POSE_SCRATCH = new PoseStack();

    /** Direct BER cache mapping BlockEntityType -> MachineBer to eliminate mc.getBlockEntityRenderDispatcher().getRenderer() (4.79% CPU). */
    private static final java.util.Map<BlockEntityType<?>, MachineBer<?>> BER_FAST_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        // Central teardown: without this, collectMain returns before
        // processPresenceQueue when disabled or mc.level == null, so LIVE keeps
        // the old world's machine BEs (each pins its whole stale ClientLevel)
        // for the entire menu period (audit 2026-10-05 §3.9).
        com.hbm_m.client.render.cache.RenderCacheManager.register(reason -> clearPresence());
    }

    private NucleusDispatcherBypass() {}

    public static void registerManaged(BlockEntityType<?> type) {
        ((NucleusManagedType) type).hbm_m$setNucleusManaged(true);
    }

    public static void registerBer(BlockEntityType<?> type, MachineBer<?> ber) {
        BER_FAST_CACHE.put(type, ber);
    }

    public static void clearBerCache() {
        BER_FAST_CACHE.clear();
    }

    /** Snapshot of the live BERs for central teardown (see MachineBer static init). */
    public static java.util.List<MachineBer<?>> snapshotBers() {
        return new java.util.ArrayList<>(BER_FAST_CACHE.values());
    }

    /**
     * Session/reload teardown: drop every presence reference. NOT gated on
     * isEnabled() - the event hooks and processPresenceQueue are unreachable on
     * the disabled path, so only an unconditional drain frees the stale world.
     */
    public static void clearPresence() {
        for (BlockEntity be : BE_TO_BUCKET.keySet()) {
            if (be instanceof NucleusTrackedBlockEntity tracked) {
                tracked.hbm_m$setNucleusRegistered(false);
            }
        }
        CHUNK_BUCKETS.clear();
        BE_TO_BUCKET.clear();
        synchronized (DEMOLISHED_LOCK) {
            DEMOLISHED_CONTROLLERS.clear();
            hasDemolishedControllers = false;
        }
        PENDING_ADD.clear();
        PENDING_REMOVE.clear();
        mainCollected = false;
        com.hbm_m.client.render.retained.RetainedInstanceGraph.get().clear();
        for (MachineBer<?> ber : snapshotBers()) {
            ber.clearRosterCache();
        }
    }

    static boolean isEnabled() {
        return com.hbm_m.config.ModClothConfig.get().nucleusDispatcherBypass && KILL_SWITCH
                && !ClientRenderFlags.forceVanillaImmediate()
                && ClientRenderFlags.useInstancedBatching();
    }

    /** Resets the main-pass flag (called from IrisShadowBatchCollector.noteMainFrameStart). */
    public static void noteMainFrameStart() {
        mainCollected = false;
    }

    /**
     * Dispatcher mixin decision point. MAIN: cancel collected machines + register
     * newcomers (singly rendered newcomer prevents flicker). SHADOW: consume
     * prebaked records for provably covered machines (SHADOW-EPOCH), normal path
     * otherwise (fail-closed).
     */
    public static boolean shouldBypass(BlockEntity be, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffers) {
        if (!isEnabled() || !isManaged(be)) {
            return false;
        }
        // Diagram capture (Simulated) re-invokes the dispatcher with its own FBO,
        // an ortho projection and a shifted pose to sketch machine previews. Never
        // intercept there: cancelling a collected machine erases it from the
        // sketch, and the newcomer collect below would file capture-basis records
        // into the retained batch (flushed into the world beside the real machine
        // on the next frame). Fail open - the vanilla BER runs and
        // MachinePartRenderer degrades to the immediate path for the capture.
        if (com.hbm_m.compat.simulated.DiagramRenderCompat.isRenderingDiagram()) {
            return false;
        }
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isRenderingShadowPass()) {
            // SHADOW-EPOCH: machines with a complete, calibrated main-pass record set
            // consume prebaked shadow records (IrisShadowBatchCollector.consumePrebaked)
            // instead of re-running the full BER CPU collection. Fail-closed: anything
            // unverified here returns false and the normal shadow path runs - its
            // records also calibrate the prebake for the rest of the frame. The
            // isEnabled()/isManaged() gates above own the kill switch: the shadow skip
            // is fully disabled whenever the dispatcher bypass is.
            if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
                return false;
            }
            // Demolished / removed machine that still reached the shadow dispatcher
            // (Embeddium's compiled section can list it for a frame or two after
            // removal): veto the vanilla shadow draw so no phantom shadow flashes.
            if (be.isRemoved() || isDemolished(be.getBlockPos().asLong())) {
                return true;
            }
            MachineBer<?> ber = BER_FAST_CACHE.get(be.getType());
            return ber != null && ber.tryConsumePrebakedShadow(be);
        }
        // Sable sublevel / Create contraption: the dispatcher (Sable) supplies a
        // ready transform, while collectOne cannot translate the BE position
        // correctly (beyond 20M+ blocks the distance cap and float precision
        // destroy it). Never intercept such a render - it goes through the
        // normal dispatcher path.
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
            return false;
        }
        boolean known = (be instanceof NucleusTrackedBlockEntity tracked)
                ? tracked.hbm_m$isNucleusRegistered()
                : BE_TO_BUCKET.containsKey(be);
        // Tombstoned pos: cancel the vanilla BER only for a machine the retained
        // graph actually tracks. An UNTRACKED BE here (refused by a stale
        // tombstone, or a newcomer not yet drained from PENDING_ADD) must stay on
        // the vanilla path - bypassing it renders nothing and the machine turns
        // invisible until world re-entry.
        if (isDemolished(be.getBlockPos().asLong())) {
            return known;
        }
        if (!known) {
            addMachine(be);
        }
        if (!mainCollected) {
            return false; // collect has not run yet - draw normally and accumulate the list
        }
        if (known) {
            return true; // already drawn by the collect loop
        }
        // Newcomer of the frame: render singly so it does not flicker until the next frame.
        collectOne(be, partialTick, new PoseStack(), buffers, bypassBase(poseStack));
        return true;
    }

    /**
     * Backward-compatible overload without explicit frustum.
     */
    public static void collectMain(PoseStack levelPose, MultiBufferSource buffers,
                                   float partialTick) {
        collectMain(levelPose, buffers, partialTick, null);
    }

    private static @Nullable net.minecraft.client.multiplayer.ClientLevel lastLevel = null;

    /**
     * Chunk-partitioned 2-level frustum-culled loop over live machines of the main pass.
     * Called at AFTER_ENTITIES — BEFORE the BE iteration; {@code levelPose} is the event poseStack.
     */
    public static void collectMain(PoseStack levelPose, MultiBufferSource buffers,
                                   float partialTick, @Nullable Frustum frustum) {
        if (!isEnabled() || mainCollected) {
            return;
        }
        // No instanced collect may be attributed to a diagram pass: its basis is
        // the capture viewport, and records built with it flush into the main
        // framebuffer beside the real machines. mainCollected stays false on this
        // path, so the dispatcher keeps the vanilla BER (immediate per-part path)
        // running for the capture instead of cancelling anything.
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
        // Instant dimension switch detection: purge old level buckets immediately
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

        // Level 1: Iterate chunk buckets (~50-100 buckets)
        for (ChunkBucket bucket : CHUNK_BUCKETS.values()) {
            if (bucket.isEmpty()) continue;

            // LEVEL 1: Chunk-level distance and frustum culling on expanded bounds
            AABB bucketBounds = bucket.getBounds(gameTime);
            if (bucketBounds.distanceToSqr(cam) > maxDistSq) continue;
            if (frustum != null && !frustum.isVisible(bucketBounds)) {
                continue; // All machines in this chunk culled in ~25 ns!
            }

            // LEVEL 1: Static chunk retained short-circuit (Milestone 2)
            long chunkKey = bucket.chunkKey;
            com.hbm_m.client.render.retained.RetainedInstanceGraph rig =
                    com.hbm_m.client.render.retained.RetainedInstanceGraph.get();
            if (rig != null) {
                com.hbm_m.client.render.retained.ChunkInstancePartition partition =
                        rig.getPartitions().get(chunkKey);
                if (partition != null && partition.getHandles().size() == bucket.machines.size()
                        && partition.isPurelyStaticAndClean()) {
                    // F3 honesty: the chunk is skipped wholesale, but every machine
                    // in it IS rendered (retained instance records) - count per
                    // machine so the overlay does not show 0 models for
                    // fully-static chunks.
                    for (com.hbm_m.client.render.retained.MachineInstanceHandle retainedHandle
                            : partition.getHandles().values()) {
                        NucleusDebug.recordMachineRendered();
                    }
                    continue; // All machines in this chunk are already retained in GPU memory!
                }
            }

            // LEVEL 2: Visible Chunk -> Machine Iteration
            ArrayList<BlockEntity> list = bucket.machines;
            for (int i = 0; i < list.size(); i++) {
                BlockEntity be = list.get(i);
                if (be.isRemoved() || isDemolished(be.getBlockPos().asLong())) {
                    removeMachine(be);
                    i--;
                    continue;
                }

                if (rig != null && rig.isMachineStaticAndRetained(be)) {
                    // Counted, not vanished: the machine renders through its
                    // retained records even though the BER collect is skipped.
                    NucleusDebug.recordMachineRendered();
                    continue;
                }

                // Individual machine frustum test before touching BER or matrices.
                // The box is fetched ONCE here and threaded into collectOne - the
                // old code re-fetched it 5 lines later inside collectOne (2 ms/frame
                // of duplicate cache hits at 5000 machines in spark).
                AABB beBox = RenderBoundsCache.get(be);
                if (frustum != null && beBox != null && !frustum.isVisible(beBox)) {
                    continue; // Machine outside frustum; skipped!
                }

                collectOne(be, beBox, partialTick, pose, buffers, base, cam, mc, gameTime, maxDistCapSq);
            }
        }
        mainCollected = true;
    }

    /**
     * Basis for the newcomer's single render: in a dispatcher call the poseStack
     * already carries T(be-cam) — so the basis is the pose without translation.
     */
    private static Matrix4f bypassBase(PoseStack dispatcherPose) {
        Matrix4f base = new Matrix4f(dispatcherPose.last().pose());
        base.m30(0f).m31(0f).m32(0f);
        return base;
    }

    private static boolean isManaged(BlockEntity be) {
        return be != null && be.getLevel() != null
                && be.getLevel() == Minecraft.getInstance().level
                && isManagedType(be.getType());
    }

    /**
     * Public read of the effective bypass state ({@link #isEnabled}) for mixins
     * outside this package (SodiumWorldRendererMixin main-pass cancel).
     */
    public static boolean isBypassActive() {
        return isEnabled();
    }

    /** Flag read on the duck interface (see NucleusManagedType) - no hash probe. */
    private static boolean isManagedType(BlockEntityType<?> type) {
        return ((NucleusManagedType) type).hbm_m$isNucleusManaged();
    }

    /** One machine: distance check -> renderer -> pose = base * T(be-cam) -> MachineBer.collectRender. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collectOne(BlockEntity be, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, Matrix4f base) {
        // Newcomer path (once per newcomer, not a per-frame loop): fetch here.
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
    private static void collectOne(BlockEntity be, @javax.annotation.Nullable AABB beBox, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, Matrix4f base,
                                   Vec3 cam, Minecraft mc, long gameTime, double maxDistCapSq) {
        BlockPos pos = be.getBlockPos();
        // Safety: sublevel/contraption BEs must not be collected by the bypass
        // (see shouldBypass) - their transform comes from the external dispatcher.
        if (com.hbm_m.compat.ContraptionRenderCompat.isContraptionRender(be)) {
            return;
        }
        double dx = pos.getX() - cam.x;
        double dy = pos.getY() - cam.y;
        double dz = pos.getZ() - cam.z;
        double distSq = (beBox != null)
                ? beBox.distanceToSqr(cam)
                : (dx * dx + dy * dy + dz * dz);
        // Safety distance cap: slightly above the maximum CONFIG distance
        // (static/animated draw sliders) - the real soft fade lives inside
        // renderParts anyway; this is just a cheap early exit.
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
        // Fast-path dirty skip: a clean machine (no dirty flag, no worldGen
        // change, no expired light/fade TTL) confirms its presence with a
        // roster assert instead of a full rebuild (matrices/light/comparison
        // of 46 floats per part).
        if (((MachineBer<?>) machineBer).tryFastAssertRender(be, gameTime)) {
            return;
        }
        pose.last().pose().set(base);
        // Normal matrix part of the basis - otherwise hook icon normals ignore camera rotation.
        NORMAL_SCRATCH.set(base);
        pose.last().normal().set(NORMAL_SCRATCH);
        pose.translate((float) dx, (float) dy, (float) dz);
        // Light goes through the tick cache (LightSampleCache): vanilla getLightColor
        // per machine per frame cost ~1.5-4% of the frame on farms.
        int packedLight = com.hbm_m.client.render.LightSampleCache.getOrSamplePacked(be, 0);
        try {
            ((MachineBer<BlockEntity>) machineBer).collectRender(
                    be, partialTick, pose, buffers, packedLight, true);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error(
                    "[HBM-M] NucleusDispatcherBypass: collect failed for {}", pos, t);
            // Fail-closed: collectRender's finally stamps onRenderCollected even on a
            // throw, so without this the machine could fast-assert next frame over
            // PARTIALLY written instance records. Marking dirty forces a full rebuild
            // instead; the MDI coordinator's one-frame drop grace keeps the previous
            // window drawn until the rebuild commits (no vanish).
            if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker) {
                tracker.markRenderDirty();
            }
        }
    }

    /**
     * Shadow-pass dispatch of live Nucleus machines (1.21.1 + Iris, invoked
     * from the BE function installed by {@link IrisShadowBeFunction121}).
     * <p>
     * Mirrors the loop contract of Iris {@code ShadowRenderer.renderBlockEntities}
     * (Iris 1.21.1 ShadowRenderer.java:591-617): the passed {@code modelView}
     * stack IS the shadow MODELVIEW-derived pose and is mutated per BE -
     * {@code pushPose → translate(pos − camera) → dispatcher.render(be,
     * tickDelta, modelView, buffers) → popPose} - exactly what the vanilla list
     * loop does, so our BERs observe an identical pose basis whether they are
     * dispatched from the vanilla list (1.20.1) or from here (1.21.1). The BER
     * call records into {@link IrisShadowBatchCollector} - the same Tier 1/2/3
     * shadow record pipeline as on 1.20.1.
     * <p>
     * Filtering, mirrored from the vanilla loop:
     * <ul>
     *   <li>{@code lightsOnly} (Iris "light block entities only" sub-pass):
     *       vanilla skips BEs with {@code getBlockState().getLightEmission() == 0};
     *       we apply the identical test per machine controller.</li>
     *   <li>{@code hasEntityFrustum}: vanilla culls each BE's 1-block box via
     *       an Iris-internal {@code BoxCuller} positioned at the camera; that
     *       culler and the shadow entity frustum are not exposed, so we keep
     *       ALL live machines (conservative, geometry past the light frustum is
     *       clipped by the shadow projection; machines are already
     *       distance-limited by their own presence lifetime). No distance cap
     *       here on purpose - a wrong cap would pop machine shadows before the
     *       light frustum does.</li>
     *   <li>Removed / demolished machines are skipped (teardown ghost veto -
     *       same policy as the Sodium mixins). Presence hygiene (eviction)
     *       stays owned by the main pass; this loop only skips.</li>
     * </ul>
     *
     * @return number of machines dispatched (added to the vanilla count by the caller)
     */
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
        int count = 0;
        for (ChunkBucket bucket : CHUNK_BUCKETS.values()) {
            if (bucket.isEmpty()) {
                continue;
            }
            ArrayList<BlockEntity> list = bucket.machines;
            for (int i = 0; i < list.size(); i++) {
                BlockEntity be = list.get(i);
                if (be.isRemoved() || isDemolished(be.getBlockPos().asLong())) {
                    continue;
                }
                if (lightsOnly && be.getBlockState().getLightEmission() == 0) {
                    continue;
                }
                BlockPos pos = be.getBlockPos();
                modelView.pushPose();
                modelView.translate(pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
                dispatcher.render(be, tickDelta, modelView, buffers);
                modelView.popPose();
                count++;
            }
        }
        return count;
    }
}
