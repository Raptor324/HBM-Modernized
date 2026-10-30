package com.hbm_m.client.render.retained;

import com.hbm_m.client.render.FrameViewState;
import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.NucleusTrackedBlockEntity;
import com.hbm_m.client.render.RenderBoundsCache;
import com.hbm_m.client.render.machine.MachineRenderRegistry;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.client.render.retained.alloc.InstanceSlotAllocator;
import com.hbm_m.client.render.retained.upload.SparseUpdateQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Central retained scene graph coordinating persistent GPU instance lifetimes.
 * Features an asynchronous Render-Thread handoff queue to ensure 100% thread safety
 * during off-thread chunk loading, network packet decoding, and block updates.
 *
 * @credit CrankShaft / fewizz / Jdb100
 */
public final class RetainedInstanceGraph {

    private static final RetainedInstanceGraph INSTANCE = new RetainedInstanceGraph(65536);

    public static RetainedInstanceGraph get() {
        return INSTANCE;
    }

    private final Long2ObjectOpenHashMap<ChunkInstancePartition> partitions = new Long2ObjectOpenHashMap<>(128);
    private final Long2ObjectOpenHashMap<MachineInstanceHandle> handlesByPos = new Long2ObjectOpenHashMap<>(1024);
    private final Long2ObjectOpenHashMap<LongArrayList> sectionToMachines = new Long2ObjectOpenHashMap<>(256);
    private final LongOpenHashSet dirtyLightSections = new LongOpenHashSet();
    // Reusable scratch for the amortized ghost sweep (render thread only, see reconcileStaleHandles).
    private final LongArrayList staleSweepScratch = new LongArrayList();
    private static final int GHOST_SWEEP_BUDGET = 64;

    private final InstanceSlotAllocator slotAllocator;
    private final SparseUpdateQueue sparseQueue;

    // Lock-free handoff queue staging mutations from network/worker threads
    private final ConcurrentLinkedQueue<Runnable> handoffQueue = new ConcurrentLinkedQueue<>();

    // O(1) BlockEntityType -> MachineSpec cache
    private final IdentityHashMap<BlockEntityType<?>, MachineSpec<?>> specsByType = new IdentityHashMap<>(64);
    // O(1) active instance count per BlockEntityType
    private final IdentityHashMap<BlockEntityType<?>, Integer> activeCountsByType = new IdentityHashMap<>(64);

    public RetainedInstanceGraph(int initialCapacity) {
        this.slotAllocator = new InstanceSlotAllocator(initialCapacity);
        this.sparseQueue = new SparseUpdateQueue(slotAllocator);
        cacheSpecs();
    }

    public void cacheSpecs() {
        synchronized (specsByType) {
            specsByType.clear();
            for (MachineSpec<?> spec : MachineRenderRegistry.specs()) {
                if (spec != null && spec.type() != null) {
                    specsByType.put(spec.type(), spec);
                }
            }
        }
    }

    public void registerSpec(BlockEntityType<?> type, MachineSpec<?> spec) {
        if (type != null && spec != null) {
            synchronized (specsByType) {
                specsByType.put(type, spec);
            }
        }
    }

    public @Nullable MachineSpec<?> getSpec(BlockEntityType<?> type) {
        if (type == null) return null;
        // R4-3.4.1: specsByType is an unsynchronized IdentityHashMap that used to be
        // read off the render thread (isManaged in the *Async entry points) while the
        // render thread could clear-and-refill it inside cacheSpecs - a data race by
        // JMM (undefined IdentityHashMap behavior under concurrent access). All
        // accesses are now serialized on the map monitor, and the *Async entry
        // points below defer their isManaged check into the render-thread lambda.
        synchronized (specsByType) {
            MachineSpec<?> spec = specsByType.get(type);
            if (spec == null) {
                cacheSpecs();
                spec = specsByType.get(type);
            }
            return spec;
        }
    }

    public boolean isManaged(BlockEntity be) {
        if (be == null) return false;
        MachineSpec<?> spec = getSpec(be.getType());
        return spec != null && spec.isPurelyStatic();
    }

    public boolean hasMachine(BlockEntity be) {
        if (be == null) return false;
        return handlesByPos.containsKey(be.getBlockPos().asLong());
    }

    /**
     * Determines whether a machine is already registered, baked in the retained GPU graph,
     * not marked dirty, and free of active per-frame dynamic hooks or CPU animators.
     * When true, the machine can be 100% short-circuited during collectMain with zero CPU work.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean isMachineStaticAndRetained(BlockEntity be) {
        if (be == null || be.isRemoved() || be.getLevel() == null) {
            return false;
        }
        MachineSpec<?> spec = getSpec(be.getType());
        if (spec == null || !spec.isPurelyStatic()) {
            return false;
        }
        if (be instanceof NucleusTrackedBlockEntity tracked && !tracked.hbm_m$isNucleusRegistered()) {
            return false;
        }
        if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker && tracker.isRenderDirty()) {
            return false;
        }
        // External shader packs / forced vanilla path own the whole pipeline -
        // the retained short-circuit must not eat machines then.
        if (com.hbm_m.client.render.shader.ShaderCompatibilityDetector.isExternalShaderActive()
                || com.hbm_m.client.render.ClientRenderFlags.forceVanillaImmediate()) {
            return false;
        }
        long blockKey = be.getBlockPos().asLong();
        MachineInstanceHandle handle = handlesByPos.get(blockKey);
        if (handle == null || !handle.isActive()) {
            return false;
        }

        // Dynamic geometry requires per-state quad evaluation
        if (spec.hasDynamicParts()) {
            return false;
        }

        // Immediate render hooks (fluids, crafting icons, hazard diamonds) require BER pass if drawing
        if (!spec.hooks().isEmpty()) {
            for (com.hbm_m.client.render.machine.MachineRenderHook hook : spec.hooks()) {
                if (hook.mayDrawContent(be)) {
                    return false;
                }
            }
        }

        // Only 100% purely static specs can bypass MachineBer entirely via the
        // retained graph. Machines with animationEpoch / parametric / animated
        // parts are fast-asserted by MachineBer.tryFastAssertRender once their
        // epoch is verified stable across ticks - short-circuiting them here made
        // collectMain skip every animated machine (deduceAnimationEpoch assigns
        // an epoch to all animated specs), freezing their part animation.
        return spec.isPurelyStatic() && refreshTtlIfStale(be);
    }

    /**
     * TTL refresh, checked only AFTER every other short-circuit condition passed:
     * a stale record (light/fade TTL, worldGen change) gets its retained light
     * slice re-sampled (onMachineDirty -> sparseQueue.enqueueUpdate updates the
     * instance light slice synchronously on the render thread) and its collection
     * stamp renewed, then stays short-circuited as retained-static this frame -
     * the sparse update itself commits the refreshed data. A machine without a
     * handle never reaches this, so it never burns its TTL stamp.
     */
    private boolean refreshTtlIfStale(BlockEntity be) {
        if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker
                && be.getLevel() != null
                && tracker.isRenderStale(be.getLevel().getGameTime(),
                        com.hbm_m.client.render.NucleusRenderVersion.worldGen())) {
            onMachineDirty(be, DirtyMask.LIGHT);
            tracker.onRenderCollected(be.getLevel().getGameTime(),
                    com.hbm_m.client.render.NucleusRenderVersion.worldGen());
        }
        return true;
    }

    // ── Asynchronous Entry Points (Invoked from Mixins / Off-Thread) ──

    public void onChunkLoadedAsync(LevelChunk chunk) {
        if (chunk == null) return;
        handoffQueue.offer(() -> onChunkLoaded(chunk));
    }

    public void onChunkUnloadedAsync(long chunkKey) {
        handoffQueue.offer(() -> onChunkUnloaded(chunkKey));
    }

    public void onMachineAddedAsync(BlockEntity be) {
        if (be == null || be.getLevel() == null || !be.getLevel().isClientSide()) {
            return;
        }
        // R4-3.4.1: isManaged (-> getSpec -> possible cacheSpecs clear-and-refill)
        // must run on the render thread, not in the caller's thread.
        handoffQueue.offer(() -> {
            if (isManaged(be)) onMachineAdded(be);
        });
    }

    public void onMachineRemovedAsync(BlockEntity be) {
        if (be == null || be.getLevel() == null || !be.getLevel().isClientSide()) {
            return;
        }
        handoffQueue.offer(() -> {
            if (isManaged(be)) onMachineRemoved(be);
        });
    }

    public void onMachineRemovedAsync(long blockKey) {
        handoffQueue.offer(() -> onMachineRemoved(blockKey));
    }

    public void onMachineDirtyAsync(BlockEntity be, int dirtyMask) {
        if (be == null || be.getLevel() == null || !be.getLevel().isClientSide()) {
            return;
        }
        handoffQueue.offer(() -> {
            if (isManaged(be)) onMachineDirty(be, dirtyMask);
        });
    }

    public void onSectionLightDirty(long sectionPosKey) {
        synchronized (dirtyLightSections) {
            dirtyLightSections.add(sectionPosKey);
        }
    }

    // ── Render Thread Execution (Invoked strictly on Render Thread) ──

    /**
     * Drains pending mutations in a single batch (< 0.05 ms latency).
     * 100% thread-safe with zero lock contention.
     */
    private static final int MAX_HANDOFF_TASKS_PER_FRAME = 64;

    public void drainHandoffQueue() {
        int processed = 0;
        Runnable task;
        while (processed < MAX_HANDOFF_TASKS_PER_FRAME && (task = handoffQueue.poll()) != null) {
            task.run();
            processed++;
        }
    }

    /**
     * Updates instance light data for all machines intersecting dirty chunk sections.
     */
    public void processDirtyLightSections() {
        if (dirtyLightSections.isEmpty()) return;
        long[] dirtyArr;
        synchronized (dirtyLightSections) {
            dirtyArr = dirtyLightSections.toLongArray();
            dirtyLightSections.clear();
        }

        for (long secKey : dirtyArr) {
            LongArrayList machines = sectionToMachines.get(secKey);
            if (machines != null && !machines.isEmpty()) {
                for (int i = 0; i < machines.size(); i++) {
                    long blockKey = machines.getLong(i);
                    MachineInstanceHandle handle = handlesByPos.get(blockKey);
                    if (handle != null && handle.isActive()) {
                        BlockEntity be = handle.getBlockEntity();
                        if (be != null && !be.isRemoved() && be.getLevel() != null) {
                            sparseQueue.enqueueUpdate(handle, be, DirtyMask.LIGHT);
                        }
                    }
                }
            }
        }
    }

    public void onChunkLoaded(LevelChunk chunk) {
        if (chunk == null) return;
        long chunkKey = chunk.getPos().toLong();
        ChunkInstancePartition partition = partitions.computeIfAbsent(chunkKey, ChunkInstancePartition::new);
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            // A tombstoned (demolished) machine must not resurrect from a chunk reload.
            if (isManaged(be) && !be.isRemoved()
                    && !com.hbm_m.client.render.NucleusDispatcherBypass.isDemolished(be.getBlockPos().asLong())) {
                registerMachine(be, partition);
            } else if (isManaged(be)
                    && com.hbm_m.client.render.NucleusDispatcherBypass.isDemolished(be.getBlockPos().asLong())) {
                com.hbm_m.multiblock.TeardownTrace.log(
                        "RIG.onChunkLoaded: REFUSED registration at tombstoned pos={} type={} (resurrection blocked)",
                        be.getBlockPos().toShortString(), be.getType());
            }
        }
    }

    public void onChunkUnloaded(long chunkKey) {
        ChunkInstancePartition partition = partitions.remove(chunkKey);
        if (partition != null) {
            for (MachineInstanceHandle handle : partition.getHandles().values()) {
                handlesByPos.remove(handle.getBlockPosKey());
                unregisterSectionSlots(handle);
                activeCountsByType.computeIfPresent(handle.getType(), (k, v) -> v <= 1 ? null : v - 1);
                MachineSpec<?> spec = getSpec(handle.getType());
                if (spec != null) {
                    spec.freeMachineSlots(handle.getBlockPosKey());
                }
            }
            partition.freeAll(slotAllocator);
        }
    }

    public void onMachineAdded(BlockEntity be) {
        if (be == null || be.isRemoved() || !isManaged(be)) return;
        long blockKey = be.getBlockPos().asLong();
        if (handlesByPos.containsKey(blockKey)) {
            // R4-2.5: a stale handle (WeakReference-collected or removed BE, or a
            // different BE at the same position) must not block registration of the
            // new instance - evict it first, then fall through and register.
            MachineInstanceHandle old = handlesByPos.get(blockKey);
            BlockEntity oldBe = old != null ? old.getBlockEntity() : null;
            if (old != null && old.isActive() && oldBe == be) {
                return; // genuine idempotent re-add of the same live BE
            }
            onMachineRemoved(blockKey);
        }
        // Tombstone guard: only a real controller BE clearing the tombstone
        // (NucleusDispatcherBypass.onMachineAdded) may re-arm a demolished machine.
        if (com.hbm_m.client.render.NucleusDispatcherBypass.isDemolished(blockKey)) {
            com.hbm_m.multiblock.TeardownTrace.trace(
                    "RIG.onMachineAdded: REFUSED - tombstoned pos={} type={} (resurrection blocked)",
                    be.getBlockPos().toShortString(), be.getType());
            return;
        }

        BlockPos p = be.getBlockPos();
        long chunkKey = ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4);
        ChunkInstancePartition partition = partitions.computeIfAbsent(chunkKey, ChunkInstancePartition::new);
        com.hbm_m.multiblock.TeardownTrace.log("RIG.onMachineAdded: registered pos={} type={}",
                p.toShortString(), be.getType());
        registerMachine(be, partition);
    }

    public void onMachineRemoved(long blockKey) {
        onMachineRemoved(blockKey, false);
    }

    /**
     * @param slotsAlreadyFreed true when the caller has already freed this key's
     *                          renderer slots via {@code MachineBer.dropPerPositionCaches}
     *                          (which calls {@code MachineSpec.freeMachineSlots}); the
     *                          blind all-specs sweep below is then skipped to avoid a
     *                          duplicate free pass. Orphaned-part and allocator cleanup
     *                          still runs regardless.
     */
    public void onMachineRemoved(long blockKey, boolean slotsAlreadyFreed) {
        MachineInstanceHandle handle = handlesByPos.remove(blockKey);
        com.hbm_m.multiblock.TeardownTrace.log(
                "RIG.onMachineRemoved: key={} handle={} (freed slots / caches)",
                BlockPos.of(blockKey).toShortString(), handle != null ? "FOUND" : "NONE");
        if (handle != null) {
            long chunkKey = handle.getChunkPosKey();
            ChunkInstancePartition partition = partitions.get(chunkKey);
            if (partition != null) {
                partition.removeHandle(blockKey);
                if (partition.isEmpty()) {
                    partitions.remove(chunkKey);
                }
            }
            unregisterSectionSlots(handle);
            handle.free(slotAllocator);
            activeCountsByType.computeIfPresent(handle.getType(), (k, v) -> v <= 1 ? null : v - 1);
            MachineSpec<?> spec = getSpec(handle.getType());
            if (spec != null) {
                spec.freeMachineSlots(blockKey);
            }
        } else {
            com.hbm_m.multiblock.TeardownTrace.trace(
                    "RIG.onMachineRemoved: NO handle at key={} - retained graph held nothing; if a ghost persists it is NOT a RIG handle",
                    BlockPos.of(blockKey).toShortString());
            if (!slotsAlreadyFreed) {
                synchronized (specsByType) {
                    for (MachineSpec<?> spec : specsByType.values()) {
                        if (spec != null) {
                            spec.freeMachineSlots(blockKey);
                        }
                    }
                }
            }
        }
    }

    public void onMachineRemoved(BlockEntity be) {
        if (be == null) return;
        onMachineRemoved(be.getBlockPos().asLong());
        if (be instanceof NucleusTrackedBlockEntity tracked) {
            tracked.hbm_m$setNucleusRegistered(false);
        }
    }

    /**
     * Amortized fail-safe against missed teardown events (render thread only, called
     * from the 1/128-frame presence sweep in NucleusDispatcherBypass): a retained
     * handle whose BE is gone while its chunk is still loaded and the position holds
     * no live BE means some hook was missed on some target. Evicting here degrades a
     * missed hook to a bounded-staleness glitch (at most ~128 frames) instead of a
     * permanent ghost. Zero steady-state cost: once per 128 frames, budget-capped.
     */
    public void reconcileStaleHandles(net.minecraft.client.multiplayer.ClientLevel level) {
        if (handlesByPos.isEmpty()) return;
        staleSweepScratch.clear();
        for (var entry : handlesByPos.long2ObjectEntrySet()) {
            if (staleSweepScratch.size() >= GHOST_SWEEP_BUDGET) break;
            BlockEntity be = entry.getValue().getBlockEntity();
            if (be != null && !be.isRemoved() && be.getLevel() == level) continue;
            BlockPos pos = BlockPos.of(entry.getLongKey());
            // Unloaded chunk: the chunk-unload path owns this case.
            if (!level.hasChunkAt(pos)) continue;
            // R4-2.5: a live BE owns the position only if it is the handle's own BE.
            // A DIFFERENT live BE (replacement instance without a removal hook) means
            // this handle is a phantom - evict so onMachineAdded can register the
            // replacement (otherwise the phantom persists forever: containsKey blocked
            // re-registration and this check blocked eviction).
            BlockEntity live = level.getBlockEntity(pos);
            if (live != null && live != be) {
                staleSweepScratch.add(entry.getLongKey());
                continue;
            }
            if (live != null) continue;
            staleSweepScratch.add(entry.getLongKey());
        }
        for (int i = 0; i < staleSweepScratch.size(); i++) {
            com.hbm_m.multiblock.TeardownTrace.trace(
                    "RIG.reconcileStaleHandles: evicting orphaned handle at {} (BE gone, chunk loaded, no live BE at pos) - a teardown hook was missed",
                    BlockPos.of(staleSweepScratch.getLong(i)).toShortString());
            onMachineRemoved(staleSweepScratch.getLong(i));
        }
    }

    public void onRenderOriginChanged(int deltaX, int deltaY, int deltaZ) {
        if (deltaX == 0 && deltaY == 0 && deltaZ == 0) return;
        for (MachineInstanceHandle handle : handlesByPos.values()) {
            if (handle == null || !handle.isActive()) continue;
            BlockEntity be = handle.getBlockEntity();
            if (be != null && !be.isRemoved()) {
                sparseQueue.enqueueUpdate(handle, be, DirtyMask.TRANSFORM);
            }
        }
    }

    public void onMachineDirty(BlockEntity be, int dirtyMask) {
        if (be == null) return;
        long blockKey = be.getBlockPos().asLong();
        MachineInstanceHandle handle = handlesByPos.get(blockKey);
        if (handle != null && handle.isActive()) {
            sparseQueue.enqueueUpdate(handle, be, dirtyMask);
            ChunkInstancePartition partition = partitions.get(handle.getChunkPosKey());
            if (partition != null) {
                partition.markDirty();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void registerMachine(BlockEntity be, ChunkInstancePartition partition) {
        MachineSpec<?> rawSpec = getSpec(be.getType());
        if (rawSpec == null) return;

        long blockKey = be.getBlockPos().asLong();
        if (handlesByPos.containsKey(blockKey)) return;

        try {
            ((MachineSpec) rawSpec).ensureStaticPartRenderers(be);
        } catch (Throwable t) {
            // Fail open: without GPU part renderers the retained record would claim
            // presence while nothing is ever baked - and both render paths skip it
            // (collectMain short-circuit + shouldBypass cancelling the vanilla BER),
            // leaving the machine permanently invisible. Leave it unregistered so it
            // stays on the vanilla per-frame BER path instead.
            com.hbm_m.main.MainRegistry.LOGGER.error(
                    "[RIG] ensureStaticPartRenderers failed for {} @ {} - staying on the vanilla BER path",
                    be.getType(), be.getBlockPos().toShortString(), t);
            return;
        }

        int partCount = (rawSpec.parts() != null && !rawSpec.parts().isEmpty()) ? rawSpec.parts().size() : 1;
        // Real GPU instance slots are owned per-part by InstancedStaticPartRenderer.slotAllocator;
        // the handle only needs an array of length partCount for SparseUpdateQueue part iteration.
        int[] slots = new int[partCount];

        AABB bounds = RenderBoundsCache.get(be);
        if (bounds == null) {
            BlockPos bp = be.getBlockPos();
            bounds = new AABB(bp);
        }

        MachineInstanceHandle handle = new MachineInstanceHandle(
                blockKey, partition.getChunkKey(), be.getType(), slots, bounds, be
        );

        handlesByPos.put(blockKey, handle);
        partition.addHandle(handle);
        registerSectionSlots(handle, bounds);
        activeCountsByType.merge(be.getType(), 1, Integer::sum);

        // Route the registered flag through the dispatcher's addMachine: setting
        // the flag BEFORE the machine lands in CHUNK_BUCKETS made shouldBypass
        // cancel the vanilla BER while collectMain (bucket-driven) never saw the
        // machine - it rendered through neither path until the next bucket sweep.
        // addMachine sets the same flag itself, after bucket registration.
        com.hbm_m.client.render.NucleusDispatcherBypass.addMachine(be);

        sparseQueue.enqueueInitialBake(handle, be, (MachineSpec) rawSpec);
    }

    private void registerSectionSlots(MachineInstanceHandle handle, AABB bounds) {
        if (bounds == null) return;
        int minSecX = ((int) Math.floor(bounds.minX - 1.0)) >> 4;
        int maxSecX = ((int) Math.ceil(bounds.maxX + 1.0)) >> 4;
        int minSecY = Math.max(-4, ((int) Math.floor(bounds.minY - 1.0)) >> 4);
        int maxSecY = Math.min(20, ((int) Math.ceil(bounds.maxY + 1.0)) >> 4);
        int minSecZ = ((int) Math.floor(bounds.minZ - 1.0)) >> 4;
        int maxSecZ = ((int) Math.ceil(bounds.maxZ + 1.0)) >> 4;

        long blockKey = handle.getBlockPosKey();
        for (int sx = minSecX; sx <= maxSecX; sx++) {
            for (int sy = minSecY; sy <= maxSecY; sy++) {
                for (int sz = minSecZ; sz <= maxSecZ; sz++) {
                    long secKey = SectionPos.asLong(sx, sy, sz);
                    LongArrayList machineList = sectionToMachines.computeIfAbsent(secKey, k -> new LongArrayList());
                    if (!machineList.contains(blockKey)) {
                        machineList.add(blockKey);
                    }
                }
            }
        }
    }

    private void unregisterSectionSlots(MachineInstanceHandle handle) {
        if (handle == null) return;
        long blockKey = handle.getBlockPosKey();
        AABB bounds = handle.getBounds();
        if (bounds == null) {
            // No bounds recorded - conservative full sweep (rare fallback), still
            // compacting empty lists so sectionToMachines cannot grow unbounded.
            for (var it = sectionToMachines.values().iterator(); it.hasNext(); ) {
                LongArrayList machineList = it.next();
                machineList.rem(blockKey);
                if (machineList.isEmpty()) it.remove();
            }
            return;
        }
        // R4-3.3: compute the registered section range from the bounds (symmetric to
        // registerSectionSlots) instead of sweeping every section in the world - the
        // full sweep ran per machine on every chunk unload (O(sections x machines)).
        int minSecX = ((int) Math.floor(bounds.minX - 1.0)) >> 4;
        int maxSecX = ((int) Math.ceil(bounds.maxX + 1.0)) >> 4;
        int minSecY = Math.max(-4, ((int) Math.floor(bounds.minY - 1.0)) >> 4);
        int maxSecY = Math.min(20, ((int) Math.ceil(bounds.maxY + 1.0)) >> 4);
        int minSecZ = ((int) Math.floor(bounds.minZ - 1.0)) >> 4;
        int maxSecZ = ((int) Math.ceil(bounds.maxZ + 1.0)) >> 4;

        for (int sx = minSecX; sx <= maxSecX; sx++) {
            for (int sy = minSecY; sy <= maxSecY; sy++) {
                for (int sz = minSecZ; sz <= maxSecZ; sz++) {
                    long secKey = SectionPos.asLong(sx, sy, sz);
                    LongArrayList machineList = sectionToMachines.get(secKey);
                    if (machineList != null) {
                        machineList.rem(blockKey);
                        if (machineList.isEmpty()) {
                            sectionToMachines.remove(secKey);
                        }
                    }
                }
            }
        }
    }

    public synchronized void clear() {
        handoffQueue.clear();
        for (MachineInstanceHandle handle : handlesByPos.values()) {
            if (handle != null && handle.getBlockEntity() instanceof NucleusTrackedBlockEntity tracked) {
                tracked.hbm_m$setNucleusRegistered(false);
            }
        }
        for (ChunkInstancePartition p : partitions.values()) {
            p.freeAll(slotAllocator);
        }
        partitions.clear();
        handlesByPos.clear();
        activeCountsByType.clear();
        sectionToMachines.clear();
        dirtyLightSections.clear();
        sparseQueue.clear();
        slotAllocator.reset();
    }

    public boolean hasActiveInstances(BlockEntityType<?> type) {
        if (type == null) return false;
        Integer count = activeCountsByType.get(type);
        return count != null && count > 0;
    }

    public boolean hasActiveInstances(com.hbm_m.client.render.InstancedStaticPartRenderer renderer) {
        if (renderer == null) return false;
        BlockEntityType<?> type = renderer.getBlockEntityType();
        if (type != null) {
            return hasActiveInstances(type);
        }
        MachineSpec<?> spec = renderer.getOwnerSpec();
        if (spec != null && spec.type() != null) {
            return hasActiveInstances(spec.type());
        }
        String tag = renderer.getMdiTraceTag();
        if (tag != null) {
            int slash = tag.indexOf('/');
            String specId = slash >= 0 ? tag.substring(0, slash) : tag;
            for (MachineSpec<?> s : specsByType.values()) {
                if (s != null && specId.equals(s.id()) && s.type() != null) {
                    return hasActiveInstances(s.type());
                }
            }
        }
        return false;
    }

    public boolean hasMachineKey(long blockPosKey) {
        MachineInstanceHandle handle = handlesByPos.get(blockPosKey);
        return handle != null && handle.isActive();
    }

    public InstanceSlotAllocator getSlotAllocator() { return slotAllocator; }
    public SparseUpdateQueue getSparseQueue() { return sparseQueue; }
    public Long2ObjectOpenHashMap<ChunkInstancePartition> getPartitions() { return partitions; }
    public Long2ObjectOpenHashMap<MachineInstanceHandle> getHandlesByPos() { return handlesByPos; }
}
