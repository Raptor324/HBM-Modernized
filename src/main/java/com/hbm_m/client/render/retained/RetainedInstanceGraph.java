package com.hbm_m.client.render.retained;

import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.NucleusDispatcherBypass;
import com.hbm_m.client.render.machine.MachineRenderRegistry;
import com.hbm_m.client.render.machine.MachineSpec;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Lightweight asynchronous Render-Thread handoff facade for machine lifecycle events.
 * Delegates spatial bucket tracking and slot teardown directly to {@link NucleusDispatcherBypass}
 * without duplicating per-chunk or per-position maps.
 */
public final class RetainedInstanceGraph {

    private static final RetainedInstanceGraph INSTANCE = new RetainedInstanceGraph();
    private static final int MAX_HANDOFF_TASKS_PER_FRAME = 64;

    public static RetainedInstanceGraph get() {
        return INSTANCE;
    }

    private final ConcurrentLinkedQueue<Runnable> handoffQueue = new ConcurrentLinkedQueue<>();
    private final IdentityHashMap<BlockEntityType<?>, MachineSpec<?>> specsByType = new IdentityHashMap<>(64);

    private RetainedInstanceGraph() {
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
        LightSampleCache.onSectionLightUpdate(sectionPosKey);
    }

    // ── Render Thread Execution ──

    public void drainHandoffQueue() {
        int processed = 0;
        Runnable task;
        while (processed < MAX_HANDOFF_TASKS_PER_FRAME && (task = handoffQueue.poll()) != null) {
            task.run();
            processed++;
        }
    }

    public void processDirtyLightSections() {
    }

    public void onChunkLoaded(LevelChunk chunk) {
        if (chunk == null) return;
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (isManaged(be) && !be.isRemoved()
                    && !NucleusDispatcherBypass.isDemolished(be.getBlockPos().asLong())) {
                NucleusDispatcherBypass.addMachine(be);
            }
        }
    }

    public void onChunkUnloaded(long chunkKey) {
        NucleusDispatcherBypass.evictChunk(chunkKey);
    }

    public void onMachineAdded(BlockEntity be) {
        if (be == null || be.isRemoved() || !isManaged(be)) return;
        if (NucleusDispatcherBypass.isDemolished(be.getBlockPos().asLong())) return;
        NucleusDispatcherBypass.addMachine(be);
    }

    public void onMachineRemoved(long blockKey) {
        onMachineRemoved(blockKey, false);
    }

    public void onMachineRemoved(long blockKey, boolean slotsAlreadyFreed) {
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

    public void onMachineRemoved(BlockEntity be) {
        if (be == null) return;
        onMachineRemoved(be.getBlockPos().asLong(), false);
        if (be instanceof com.hbm_m.client.render.NucleusTrackedBlockEntity tracked) {
            tracked.hbm_m$setNucleusRegistered(false);
        }
    }

    public void reconcileStaleHandles(net.minecraft.client.multiplayer.ClientLevel level) {
        // Handled by NucleusDispatcherBypass safety sweep
    }

    public void onRenderOriginChanged(int deltaX, int deltaY, int deltaZ) {
        if (deltaX == 0 && deltaY == 0 && deltaZ == 0) return;
        NucleusDispatcherBypass.markAllMachinesDirty();
    }

    public void onMachineDirty(BlockEntity be, int dirtyMask) {
        if (be instanceof com.hbm_m.api.render.RenderDirtyTracker tracker) {
            tracker.markRenderDirty();
        }
    }

    public synchronized void clear() {
        handoffQueue.clear();
    }

    public boolean hasActiveInstances(com.hbm_m.client.render.InstancedStaticPartRenderer renderer) {
        return renderer != null && renderer.getInstanceCount() > 0;
    }
}
