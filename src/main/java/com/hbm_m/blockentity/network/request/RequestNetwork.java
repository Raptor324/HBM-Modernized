package com.hbm_m.blockentity.network.request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Port von {@code com.hbm.tileentity.network.RequestNetwork} (1.7.10 Original, Pipeline B). Rein
 * statische, chunk-bucketed Registry aller aktiven {@link PathNode}s, die sich selbst per Lease
 * (2000ms Timeout) am Leben halten - kein manuelles An-/Abmelden noetig, ein Block der aufhoert zu
 * ticken (Chunk entladen, kaputt) faellt automatisch nach spaetestens 2s aus dem Netz.
 * <p>
 * SCOPE-Vereinfachung: Das Original nutzt eine eigene {@code HashedSet}-Klasse (reine
 * Positions-Deduplizierung). {@link PathNode#equals}/{@link PathNode#hashCode} bereits nach Position
 * ueberschrieben, deshalb reicht hier ein normales {@link java.util.HashSet}.
 */
public final class RequestNetwork {

    public static final int MAX_AGE_MS = 2_000;

    private static int timer = 0;
    private static final Map<Level, Map<ChunkPos, Set<PathNode>>> ACTIVE_WAYPOINTS = new HashMap<>();

    private RequestNetwork() {}

    public static void updateEntries() {
        if (timer > 0) {
            timer--;
            return;
        }
        timer = 20;

        Iterator<Map.Entry<Level, Map<ChunkPos, Set<PathNode>>>> worldIt = ACTIVE_WAYPOINTS.entrySet().iterator();
        while (worldIt.hasNext()) {
            var worldEntry = worldIt.next();
            Iterator<Map.Entry<ChunkPos, Set<PathNode>>> chunkIt = worldEntry.getValue().entrySet().iterator();

            while (chunkIt.hasNext()) {
                var chunkEntry = chunkIt.next();
                Iterator<PathNode> pathIt = chunkEntry.getValue().iterator();

                while (pathIt.hasNext()) {
                    PathNode node = pathIt.next();
                    if (node.lease < System.currentTimeMillis() - MAX_AGE_MS) {
                        node.reachableNodes.clear();
                        pathIt.remove();
                    }
                }

                if (chunkEntry.getValue().isEmpty()) chunkIt.remove();
            }

            if (worldEntry.getValue().isEmpty()) worldIt.remove();
        }
    }

    public static void push(Level level, PathNode node) {
        Map<ChunkPos, Set<PathNode>> coordMap = ACTIVE_WAYPOINTS.computeIfAbsent(level, l -> new HashMap<>());
        ChunkPos chunkPos = new ChunkPos(node.pos.getX() >> 4, node.pos.getZ() >> 4);
        Set<PathNode> posList = coordMap.computeIfAbsent(chunkPos, c -> new HashSet<>());
        posList.remove(node);
        posList.add(node);
    }

    public static Set<PathNode> getAllLocalNodes(Level level, int x, int z, int range) {
        Set<PathNode> nodes = new HashSet<>();
        Map<ChunkPos, Set<PathNode>> coordMap = ACTIVE_WAYPOINTS.get(level);
        if (coordMap == null) return nodes;

        int cx = x >> 4;
        int cz = z >> 4;

        for (int i = -range; i <= range; i++) {
            for (int j = -range; j <= range; j++) {
                Set<PathNode> nodeList = coordMap.get(new ChunkPos(cx + i, cz + j));
                if (nodeList != null) nodes.addAll(nodeList);
            }
        }
        return nodes;
    }

    // ── Node types ──────────────────────────────────────────────────────────

    /** Generic path node - nothing but a position, a lease timestamp, and its currently-reachable neighbours. */
    public static class PathNode {
        public final BlockPos pos;
        public long lease;
        public boolean active = true;
        public boolean torchWaypoint;
        public final Set<PathNode> reachableNodes;

        public PathNode(BlockPos pos, Set<PathNode> reachableNodes) {
            this.pos = pos;
            this.reachableNodes = new HashSet<>(reachableNodes);
            this.lease = System.currentTimeMillis();
            this.torchWaypoint = true;
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof PathNode node)) return false;
            return pos.equals(node.pos);
        }
    }

    /** Node created by providers - snapshots the item stacks currently available. */
    public static class OfferNode extends PathNode {
        public final List<ItemStack> offer;

        public OfferNode(BlockPos pos, Set<PathNode> reachableNodes, List<ItemStack> offer) {
            super(pos, reachableNodes);
            this.offer = offer;
            this.torchWaypoint = false;
        }
    }

    /** Node created by requesters - lists the (simplified, exact-match) wanted item stacks. */
    public static class RequestNode extends PathNode {
        public final List<ItemStack> request;

        public RequestNode(BlockPos pos, Set<PathNode> reachableNodes, List<ItemStack> request) {
            super(pos, reachableNodes);
            this.request = request;
            this.torchWaypoint = false;
        }
    }
}
