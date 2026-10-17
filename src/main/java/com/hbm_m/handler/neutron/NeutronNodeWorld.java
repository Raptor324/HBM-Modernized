package com.hbm_m.handler.neutron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Port of NEXT {@code com.hbm.handler.neutron.NeutronNodeWorld}: the PILE-typed parallel of
 * {@code com.hbm_m.handler.rbmk.NeutronNodeWorld} (which is concrete-typed to
 * {@code RBMKNeutronHandler.RBMKNeutronStream/Node} and was left untouched on purpose).
 *
 * <p>Differences from NEXT, all deliberate:
 * <ul>
 * <li>{@code cleanNodes()} is omitted - its only NEXT content prunes RBMK nodes
 * ({@code NeutronType.RBMK}), and RBMK nodes live in {@code handler/rbmk} here; PILE nodes are
 * instead removed eagerly via {@code BlockEntityPileBase.setRemoved}.</li>
 * <li>{@link #removeWorld(Level)} has no NEXT analog - it is the Modernized explicit-lifecycle
 * glue (same shape as {@code handler/rbmk/NeutronNodeWorld.removeWorld}), called from
 * {@code MainRegistry} on SERVER_LEVEL_UNLOAD because a plain {@code HashMap} keyed by
 * {@link Level} never garbage-collects unloaded levels on its own.</li>
 * </ul></p>
 */
public class NeutronNodeWorld {

    public static final HashMap<Level, StreamWorld> streamWorlds = new HashMap<>();

    public static NeutronNode getNode(Level world, BlockPos pos) {
        StreamWorld streamWorld = streamWorlds.get(world);
        return streamWorld != null ? streamWorld.nodeCache.get(pos) : null;
    }

    public static void removeNode(Level world, BlockPos pos) {
        StreamWorld streamWorld = streamWorlds.get(world);
        if (streamWorld == null) return;
        streamWorld.removeNode(pos);
    }

    public static StreamWorld getOrAddWorld(Level world) {
        return streamWorlds.computeIfAbsent(world, w -> new StreamWorld());
    }

    public static void removeAllWorlds() {
        streamWorlds.clear();
    }

    /**
     * NEXT {@code removeEmptyWorlds} verbatim: drop stream worlds that carry no streams at all.
     * Node-only worlds are dropped too - lossless, because a {@code PileNeutronNode} carries no
     * state beyond its position and tile and is re-created on the next {@code castRay}.
     */
    public static void removeEmptyWorlds() {
        streamWorlds.values().removeIf(streamWorld -> streamWorld.streams.isEmpty());
    }

    /** Drops the stream world of a level that is going away (Modernized lifecycle, no NEXT analog). */
    public static void removeWorld(Level world) {
        streamWorlds.remove(world);
    }

    public static class StreamWorld {

        private final List<NeutronStream> streams = new ArrayList<>();
        private final HashMap<BlockPos, NeutronNode> nodeCache = new HashMap<>();

        public void runStreamInteractions(Level world) {
            for (NeutronStream stream : streams) {
                stream.runStreamInteraction(world, this);
            }
        }

        public void addStream(NeutronStream stream) {
            streams.add(stream);
        }

        public void removeAllStreams() {
            streams.clear();
        }

        public NeutronNode getNode(BlockPos pos) {
            return nodeCache.get(pos);
        }

        public void addNode(NeutronNode node) {
            nodeCache.put(node.pos, node);
        }

        public void removeNode(BlockPos pos) {
            nodeCache.remove(pos);
        }
    }
}
