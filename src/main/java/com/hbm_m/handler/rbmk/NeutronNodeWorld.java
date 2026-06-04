package com.hbm_m.handler.rbmk;

import com.hbm_m.block.entity.machines.rbmk.RBMKColumnBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;

public class NeutronNodeWorld {

    private static final Map<Level, StreamWorld> streamWorlds = new WeakHashMap<>();

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

    /** Called from server post-tick to process all pending neutron streams for a given level. */
    public static void tick(Level level) {
        StreamWorld sw = streamWorlds.get(level);
        if (sw == null) return;
        sw.runStreamInteractions(level);
        sw.cleanNodes(level);
        sw.streams.clear();
    }

    public static class StreamWorld {

        final List<RBMKNeutronHandler.RBMKNeutronStream> streams = new ArrayList<>();
        final Map<BlockPos, RBMKNeutronHandler.RBMKNeutronNode> nodeCache = new HashMap<>();

        public void runStreamInteractions(Level level) {
            for (RBMKNeutronHandler.RBMKNeutronStream stream : streams) {
                stream.runStreamInteraction(level, this);
            }
        }

        public void addStream(RBMKNeutronHandler.RBMKNeutronStream stream) {
            streams.add(stream);
        }

        public void cleanNodes(Level level) {
            List<BlockPos> toRemove = new ArrayList<>();
            for (RBMKNeutronHandler.RBMKNeutronNode node : nodeCache.values()) {
                toRemove.addAll(node.checkNode(this, level));
            }
            for (BlockPos pos : toRemove) nodeCache.remove(pos);
        }

        public RBMKNeutronHandler.RBMKNeutronNode getNode(BlockPos pos) {
            return nodeCache.get(pos.immutable());
        }

        public void addNode(RBMKNeutronHandler.RBMKNeutronNode node) {
            nodeCache.put(node.pos.immutable(), node);
        }

        public void removeNode(BlockPos pos) {
            nodeCache.remove(pos);
        }
    }
}
