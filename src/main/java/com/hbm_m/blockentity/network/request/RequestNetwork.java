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
 * Das Original nutzt {@code HashedSet} (Schluessel = hashCode, ein neues Element ersetzt das alte). Hier
 * gleichwertig: {@link PathNode#equals}/{@link PathNode#hashCode} nach Position, {@link #push} entfernt vor dem
 * Einfuegen, und der Drohnenhafen sucht die aktuelle Instanz per equals ({@code getMap().get(hash)}).
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

    /** Node created by requesters - Original {@code List<AStack>}. */
    public static class RequestNode extends PathNode {
        public final List<RequestStack> request;

        public RequestNode(BlockPos pos, Set<PathNode> reachableNodes, List<RequestStack> request) {
            super(pos, reachableNodes);
            this.request = request;
            this.torchWaypoint = false;
        }
    }

    /**
     * Original {@code AStack} des Wunschzettels: {@code ComparableStack} (Item + Schaden, oder Item mit
     * WILDCARD_VALUE) bzw. {@code OreDictStack} (hier der Filtermodus, im Port {@code "tag:<id>"}).
     * Vergleich wie {@code matchesRecipe(stack, true)} - die Stueckzahl zaehlt nicht.
     */
    public static final class RequestStack {
        public final ItemStack item;
        public final boolean wildcard;
        @org.jetbrains.annotations.Nullable public final String dict;

        private RequestStack(ItemStack item, boolean wildcard, @org.jetbrains.annotations.Nullable String dict) {
            this.item = item;
            this.wildcard = wildcard;
            this.dict = dict;
        }

        public static RequestStack comp(ItemStack filter, boolean wildcard) {
            ItemStack single = filter.copy();
            single.setCount(1);
            return new RequestStack(single, wildcard, null);
        }

        public static RequestStack dict(String name) {
            return new RequestStack(ItemStack.EMPTY, false, name);
        }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            if (dict != null) {
                if (!dict.startsWith(com.hbm_m.inventory.filter.ModulePatternMatcher.MODE_TAG_PREFIX)) return false;
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(
                        dict.substring(com.hbm_m.inventory.filter.ModulePatternMatcher.MODE_TAG_PREFIX.length()));
                return id != null && stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, id));
            }
            if (stack.getItem() != item.getItem()) return false;
            return wildcard || stack.getDamageValue() == item.getDamageValue();
        }

        /** Original writeEntityToNBT: "comp" (id, meta) bzw. "dict". */
        public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag) {
            if (dict != null) {
                tag.putString("type", "dict");
                tag.putString("dict", dict);
            } else {
                tag.putString("type", "comp");
                tag.putString("id", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
                tag.putInt("meta", wildcard ? -1 : item.getDamageValue());
            }
            return tag;
        }

        @org.jetbrains.annotations.Nullable
        public static RequestStack load(net.minecraft.nbt.CompoundTag tag) {
            if ("dict".equals(tag.getString("type"))) return dict(tag.getString("dict"));
            if (!"comp".equals(tag.getString("type"))) return null;
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("id"));
            if (id == null) return null;
            ItemStack stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));
            int meta = tag.getInt("meta");
            if (meta > 0) stack.setDamageValue(meta);
            return new RequestStack(stack, meta < 0, null);
        }
    }
}
