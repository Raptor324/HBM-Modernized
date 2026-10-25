package com.hbm_m.client.render;

/**
 * Central cross-frame cache of BlockEntity render bounds.
 *
 * <p>{@code getRenderBoundingBox} on multiblock BEs walks the whole structure map
 * and allocates a fresh AABB on every call (profile AwszOJcOkb: 8.8% of the render
 * thread - Embeddium/Sodium calls it for every BE every frame, our Nucleus collect
 * path and light sampling several more times). The cache sits IN FRONT of the
 * virtual call, so every caller shares one cache and no BE class carries its own.
 *
 * <p>Bounds are structural: they only change when the multiblock re-forms, which
 * marks the {@link com.hbm_m.api.render.RenderDirtyTracker} stale; a TTL is the
 * belt-and-braces guard for BEs without a tracker. The map is keyed by identity
 * and cleared on render-world changes; removed BEs are evicted on touch.
 */
public final class RenderBoundsCache {

    private static final int TTL_TICKS = 600;

    private static final java.util.IdentityHashMap<net.minecraft.world.level.block.entity.BlockEntity, Entry> CACHE =
            new java.util.IdentityHashMap<>();

    private static int lastWorldGen = -1;
    /** Game time, memoized per render frame (a per-BE virtual call in hot loops otherwise). */
    private static long frameStampTick = -1L;
    private static long frameGameTime = 0L;

    static {
        // Central teardown: the cache pins BEs by strong identity keys (each key
        // pins its whole Level through BlockEntity.level), so it must not survive
        // a world exit or resource reload (audit 2026-10-05 §3.1).
        com.hbm_m.client.render.cache.RenderCacheManager.register(reason -> clear());
    }

    /** Bulk clear on world exit / resource reload; the next get() re-syncs with worldGen. */
    public static void clear() {
        CACHE.clear();
        lastWorldGen = -1;
    }

    /** Immediate eviction when a BlockEntity is removed from the client world. */
    public static void remove(net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be != null) {
            CACHE.remove(be);
        }
    }

    private static final class Entry {
        net.minecraft.world.phys.AABB box;
        net.minecraft.world.phys.AABB physicalBox;
        net.minecraft.world.phys.AABB localPhysicalBox;
        long stampTick;
        int worldGen;
    }

    /**
     * Per-frame sweep (audit 2026-10-13 R2-2.1): entries are otherwise evicted
     * only on touch, so BEs of unloaded chunks / destroyed machines stayed
     * pinned by the strong identity key (each key pins its whole Level) until
     * a worldGen bulk clear. Evicts removed BEs and BEs whose chunk is no
     * longer loaded - MachineInstanceHandle only holds them via WeakReference,
     * so this cache must not outlive the in-world lifetime.
     */
    private static void sweepUnreferenced() {
        if (CACHE.isEmpty()) return;
        var it = CACHE.entrySet().iterator();
        while (it.hasNext()) {
            var be = it.next().getKey();
            var lvl = be.getLevel();
            if (be.isRemoved() || lvl == null || !lvl.hasChunkAt(be.getBlockPos())) {
                it.remove();
            }
        }
    }

    public static net.minecraft.world.phys.AABB get(net.minecraft.world.level.block.entity.BlockEntity be) {
        Entry e = getOrCreateEntry(be);
        return e != null ? e.box : com.hbm_m.platform.RenderBounds.get(be);
    }

    /**
     * Uninflated world-space physical bounds of the machine or multiblock structure.
     * Used for 8-corner spatial lighting sampling (avoids underground/solid culling inflation).
     */
    public static net.minecraft.world.phys.AABB getPhysicalBounds(net.minecraft.world.level.block.entity.BlockEntity be) {
        Entry e = getOrCreateEntry(be);
        return e != null ? e.physicalBox : computePhysicalBounds(be);
    }

    /**
     * Uninflated model-space (controller-local, Direction.NORTH) physical bounds of the structure.
     * Used as objBbox for InstBboxMin / InstBboxSize and spatial lighting in model coordinates.
     *
     * <p>Model space is centered on the controller block center
     * ({@code translate(0.5, 0, 0.5)} in {@code LegacyAnimator.setupBlockTransform}),
     * so a single block is (-0.5, 0, -0.5)..(0.5, 1, 0.5) - matching the
     * enqueueUpdate hardcode in SparseUpdateQueue. Grid-space (0..N) boxes are
     * converted via {@link #gridToModelSpace} (audit 2026-10-13 R2-2.2).
     */
    public static net.minecraft.world.phys.AABB getLocalPhysicalBounds(net.minecraft.world.level.block.entity.BlockEntity be) {
        Entry e = getOrCreateEntry(be);
        return e != null ? e.localPhysicalBox : computeLocalPhysicalBounds(be);
    }

    private static Entry getOrCreateEntry(net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be == null) return null;
        long wg = NucleusRenderVersion.worldGen();
        if ((int) wg != lastWorldGen) {
            // Chunk reload / render-origin shift: bulk invalidate.
            CACHE.clear();
            lastWorldGen = (int) wg;
        }
        long frame = IrisShadowBatchCollector.renderFrame();
        if (frameStampTick != frame) {
            var lvl = net.minecraft.client.Minecraft.getInstance().level;
            frameGameTime = lvl != null ? lvl.getGameTime() : 0L;
            frameStampTick = frame;
            // Amortize the safety sweep to once every 256 frames: per-frame it
            // walked all ~5000 IdentityHashMap entries calling hasChunkAt
            // (0.57 ms/frame in spark). Explicit removal goes through remove(be)
            // from NucleusDispatcherBypass.removeMachine.
            if ((frame & 255L) == 0L) {
                sweepUnreferenced();
            }
        }
        long tick = frameGameTime;
        Entry e = CACHE.get(be);
        if (be.isRemoved()) {
            CACHE.remove(be);
            return e;
        }
        // Tracker BEs (every HBM machine/multiblock) recompute ONLY on an actual
        // dirty event - the old unconditional 40-tick TTL threw away and rebuilt
        // every machine's three AABBs every 2 seconds (1.13 ms/frame in spark)
        // although bounds never change without RenderDirtyTracker. Non-tracker
        // BEs keep the TTL as their only staleness guard.
        boolean hasTracker = be instanceof com.hbm_m.api.render.RenderDirtyTracker;
        boolean needsRecompute = (e == null)
                || (e.worldGen != (int) wg)
                || (hasTracker
                        ? ((com.hbm_m.api.render.RenderDirtyTracker) be).isRenderDirty()
                        : (tick - e.stampTick > TTL_TICKS));
        if (needsRecompute) {
            net.minecraft.world.phys.AABB oldBox = e != null ? e.box : null;
            e = new Entry();
            e.box = com.hbm_m.platform.RenderBounds.get(be);
            e.physicalBox = computePhysicalBounds(be);
            e.localPhysicalBox = computeLocalPhysicalBounds(be);
            e.stampTick = tick;
            e.worldGen = (int) wg;
            CACHE.put(be, e);

            // If bounds changed dynamically, notify the enclosing chunk bucket immediately
            if (oldBox != null && !oldBox.equals(e.box)) {
                com.hbm_m.client.render.NucleusDispatcherBypass.markMachineBoundsDirty(be);
            }
        }
        return e;
    }

    public static net.minecraft.world.phys.AABB computePhysicalBounds(net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be == null) return null;
        try {
            var state = be.getBlockState();
            if (state != null && state.getBlock() instanceof com.hbm_m.interfaces.IMultiblockController ctrl) {
                var helper = ctrl.getStructureHelper();
                if (helper != null) {
                    net.minecraft.core.Direction facing = net.minecraft.core.Direction.NORTH;
                    if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                        facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
                    }
                    return helper.getRenderBoundingBox(be.getBlockPos(), facing, 0.0);
                }
            }
            if (be instanceof com.hbm_m.block.entity.doors.DoorBlockEntity door) {
                var decl = door.getDoorDecl();
                if (decl != null) {
                    int[] dims = decl.getDimensions();
                    if (dims != null && dims.length >= 6) {
                        net.minecraft.core.BlockPos bp = be.getBlockPos();
                        int minX = dims[0], minY = dims[1], minZ = dims[2];
                        int maxX = minX + dims[3] + 1, maxY = minY + dims[4] + 1, maxZ = minZ + dims[5] + 1;
                        net.minecraft.world.phys.AABB local = new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ);
                        return local.move(bp.getX(), bp.getY(), bp.getZ());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return new net.minecraft.world.phys.AABB(be.getBlockPos());
    }

    public static net.minecraft.world.phys.AABB computeLocalPhysicalBounds(net.minecraft.world.level.block.entity.BlockEntity be) {
        if (be == null) return null;
        try {
            var state = be.getBlockState();
            if (state != null && state.getBlock() instanceof com.hbm_m.interfaces.IMultiblockController ctrl) {
                var helper = ctrl.getStructureHelper();
                if (helper != null) {
                    return gridToModelSpace(helper.getLocalRenderBoundingBox(net.minecraft.core.Direction.NORTH));
                }
            }
            if (be instanceof com.hbm_m.block.entity.doors.DoorBlockEntity door) {
                var decl = door.getDoorDecl();
                if (decl != null) {
                    int[] dims = decl.getDimensions();
                    if (dims != null && dims.length >= 6) {
                        int minX = dims[0], minY = dims[1], minZ = dims[2];
                        int maxX = minX + dims[3] + 1, maxY = minY + dims[4] + 1, maxZ = minZ + dims[5] + 1;
                        return gridToModelSpace(new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ));
                    }
                }
            }
        } catch (Throwable ignored) {}
        return new net.minecraft.world.phys.AABB(-0.5, 0, -0.5, 0.5, 1, 0.5);
    }

    /**
     * Converts a block-grid box (0..N, cell-aligned, controller cell = (0,0,0)..(1,1,1))
     * into model space centered on the controller block center (see
     * {@link #getLocalPhysicalBounds}): shifted by the FIXED controller-cell half
     * (0.5) in X/Z, Y kept at the block-grid origin. The block transform only
     * offsets X/Z by the controller block center - NOT by half the whole
     * structure - so a structure-size-proportional shift misplaced every
     * multiblock larger than one block.
     */
    private static net.minecraft.world.phys.AABB gridToModelSpace(net.minecraft.world.phys.AABB grid) {
        return new net.minecraft.world.phys.AABB(
                grid.minX - 0.5, grid.minY, grid.minZ - 0.5,
                grid.maxX - 0.5, grid.maxY, grid.maxZ - 0.5);
    }

    private RenderBoundsCache() {}
}
