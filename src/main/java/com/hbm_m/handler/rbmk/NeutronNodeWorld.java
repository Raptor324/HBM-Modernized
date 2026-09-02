package com.hbm_m.handler.rbmk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;

import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;

public class NeutronNodeWorld {

    private static final Map<Level, StreamWorld> streamWorlds = new WeakHashMap<>();

    /**
     * CE freshens the node cache only every {@code CACHE_TIME} ticks, not every tick: the sweep is
     * O(nodes * fluxRange^2) and running it each tick on a live reactor costs far more than the
     * memory it reclaims.
     */
    private static final int CACHE_TIME = 20;
    private static int ticks = 0;

    public static RBMKNeutronHandler.RBMKNeutronNode getNode(Level level, BlockPos pos) {
        StreamWorld sw = streamWorlds.get(level);
        return sw != null ? sw.nodeCache.get(pos) : null;
    }

    public static void removeNode(Level level, BlockPos pos) {
        StreamWorld sw = streamWorlds.get(level);
        if (sw != null) sw.nodeCache.remove(pos);
    }

    public static StreamWorld getOrAddWorld(Level level) {
        return streamWorlds.computeIfAbsent(level, k -> new StreamWorld());
    }

    public static void removeAllWorlds() {
        streamWorlds.clear();
    }

    /** CE's {@code removeEmptyWorlds}: drop stream worlds that carry no streams at all. */
    public static void removeEmptyWorlds() {
        streamWorlds.values().removeIf(sw -> sw.streams.isEmpty() && sw.nodeCache.isEmpty());
    }

    /**
     * Advances the shared tick counter once per server tick. Must be called exactly once before the
     * per-level {@link #tick(Level)} calls, otherwise the cache sweep fires once per loaded level
     * instead of once per interval.
     */
    public static boolean advanceTick() {
        boolean cacheClear = ticks >= CACHE_TIME;
        if (cacheClear) ticks = 0;
        ticks++;
        return cacheClear;
    }

    /** Called from server post-tick to process all pending neutron streams for a given level. */
    public static void tick(Level level, boolean cacheClear) {
        StreamWorld sw = streamWorlds.get(level);
        if (sw == null) return;

        // Dial cache: read once per level per tick, exactly as CE's NeutronHandler.onServerTick does.
        RBMKNeutronHandler.reflectorEfficiency    = RBMKDials.getReflectorEfficiency(level);
        RBMKNeutronHandler.absorberEfficiency     = RBMKDials.getAbsorberEfficiency(level);
        RBMKNeutronHandler.moderatorEfficiency    = RBMKDials.getModeratorEfficiency(level);
        RBMKNeutronHandler.absorberHeatConversion = RBMKDials.getAbsorberHeatConversion(level);
        // The +1 is deliberate and load-bearing: the handler wants the *total* stacked block count,
        // while getColumnHeight returns the offset form (total - 1).
        RBMKNeutronHandler.columnHeight = RBMKDials.getColumnHeight(level) + 1;
        RBMKNeutronHandler.fluxRange    = RBMKDials.getFluxRange(level);

        sw.runStreamInteractions(level);
        sw.streams.clear();

        if (cacheClear) sw.cleanNodes(level);
    }

    /** Back-compat single-level entry point; advances the tick counter itself. */
    public static void tick(Level level) {
        tick(level, advanceTick());
    }

    public static class StreamWorld {

        final List<RBMKNeutronHandler.RBMKNeutronStream> streams = new ArrayList<>();
        final Map<BlockPos, RBMKNeutronHandler.RBMKNeutronNode> nodeCache = new HashMap<>();

        public void runStreamInteractions(Level level) {
            // Indexed: receiveFlux on an outgasser/rod may queue further streams during the sweep.
            for (int i = 0; i < streams.size(); i++) {
                streams.get(i).runStreamInteraction(level, this);
            }
        }

        public void addStream(RBMKNeutronHandler.RBMKNeutronStream stream) {
            streams.add(stream);
        }

        public void removeAllStreams() {
            streams.clear();
        }

        public void cleanNodes(Level level) {
            List<BlockPos> toRemove = new ArrayList<>();
            for (RBMKNeutronHandler.RBMKNeutronNode node : new ArrayList<>(nodeCache.values())) {
                toRemove.addAll(node.checkNode(this, level));
            }
            for (BlockPos pos : toRemove) nodeCache.remove(pos);
        }

        /** Drops the entry if the column behind it was removed since it was cached. */
        public RBMKNeutronHandler.RBMKNeutronNode getNode(BlockPos pos) {
            RBMKNeutronHandler.RBMKNeutronNode node = nodeCache.get(pos);
            if (node != null && node.tile.isRemoved()) {
                nodeCache.remove(pos);
                return null;
            }
            return node;
        }

        public void addNode(RBMKNeutronHandler.RBMKNeutronNode node) {
            nodeCache.put(node.pos.immutable(), node);
        }

        public void removeNode(BlockPos pos) {
            nodeCache.remove(pos);
        }
    }
}
