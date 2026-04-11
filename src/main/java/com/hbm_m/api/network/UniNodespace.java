package com.hbm_m.api.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Универсальное пространство узлов (порт UniNodespace из 1.7.10).
 * Хранит все узлы всех сетей по всем измерениям.
 * Обновляется раз в серверный тик: {@link #updateNodespace(MinecraftServer)}.
 */
public class UniNodespace {

    /** Ключ узловой карты: (позиция, провайдер сети). */
    public static final class NodeKey {
        public final BlockPos pos;
        public final INetworkProvider<?> provider;

        public NodeKey(BlockPos pos, INetworkProvider<?> provider) {
            this.pos = pos.immutable();
            this.provider = provider;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NodeKey other)) return false;
            return pos.equals(other.pos) && provider == other.provider;
        }

        @Override
        public int hashCode() {
            return 31 * pos.hashCode() + System.identityHashCode(provider);
        }
    }

    /** Карта узлов на одном измерении. Используем LinkedHashMap для предсказуемого порядка. */
    public static class UniNodeWorld {
        public final Map<NodeKey, GenNode<?>> nodes = new LinkedHashMap<>();

        public void pushNode(GenNode<?> node) {
            for (BlockPos pos : node.positions) {
                nodes.put(new NodeKey(pos, node.networkProvider), node);
            }
        }

        public void popNode(GenNode<?> node) {
            if (node.net != null) node.net.destroy();
            for (BlockPos pos : node.positions) {
                nodes.remove(new NodeKey(pos, node.networkProvider));
            }
            node.expired = true;
        }
    }

    /** Все активные сети (для обновления). Ссылки живут пока сеть valid. */
    @SuppressWarnings("rawtypes")
    public static final Set<NodeNet> activeNodeNets = new HashSet<>();

    /** Карта измерение → узловой мир. */
    private static final Map<ResourceKey<Level>, UniNodeWorld> worlds = new HashMap<>();

    private static int reapTimer = 0;

    // --- Node CRUD ---

    @Nullable
    @SuppressWarnings("unchecked")
    public static <N extends NodeNet<?, ?, ?>> GenNode<N> getNode(
            ServerLevel level, BlockPos pos, INetworkProvider<N> provider) {
        UniNodeWorld world = worlds.get(level.dimension());
        if (world == null) return null;
        return (GenNode<N>) world.nodes.get(new NodeKey(pos, provider));
    }

    public static void createNode(ServerLevel level, GenNode<?> node) {
        worlds.computeIfAbsent(level.dimension(), k -> new UniNodeWorld()).pushNode(node);
    }

    public static void destroyNode(ServerLevel level, BlockPos pos, INetworkProvider<?> provider) {
        UniNodeWorld world = worlds.get(level.dimension());
        if (world == null) return;
        GenNode<?> node = world.nodes.get(new NodeKey(pos, provider));
        if (node != null) world.popNode(node);
    }

    public static void destroyNode(ServerLevel level, GenNode<?> node) {
        UniNodeWorld world = worlds.get(level.dimension());
        if (world != null) world.popNode(node);
    }

    /** Вызывается при выгрузке измерения — очищает все узлы и сети. */
    public static void onLevelUnload(ServerLevel level) {
        UniNodeWorld world = worlds.remove(level.dimension());
        if (world == null) return;
        for (GenNode<?> node : world.nodes.values()) {
            if (node.net != null) node.net.destroy();
            node.expired = true;
        }
    }

    /** Вызывается при остановке сервера. */
    public static void onServerStop() {
        worlds.clear();
        activeNodeNets.clear();
        reapTimer = 0;
    }

    // --- Main update loop (called once per server tick) ---

    public static void updateNodespace(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            UniNodeWorld nodeWorld = worlds.get(level.dimension());
            if (nodeWorld == null) continue;

            // Копируем entrySet для безопасной итерации (createNode может изменить карту)
            for (Map.Entry<NodeKey, GenNode<?>> entry : new ArrayList<>(nodeWorld.nodes.entrySet())) {
                GenNode<?> node = entry.getValue();
                INetworkProvider<?> provider = entry.getKey().provider;

                if (!node.hasValidNet() || node.recentlyChanged) {
                    checkNodeConnection(level, node, provider);
                    node.recentlyChanged = false;
                }
            }
        }

        updateNetworks();
        updateReapTimer();
    }

    // --- Internal helpers ---

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void checkNodeConnection(ServerLevel level, GenNode node, INetworkProvider provider) {
        for (NodeDirPos con : node.connections) {
            GenNode conNode = getNode(level, con.getPos(), provider);
            if (conNode == null) continue;
            if (conNode.hasValidNet() && conNode.net == node.net) continue;
            if (checkConnection(conNode, con, false)) {
                connectToNode(node, conNode);
            }
        }

        if (!node.hasValidNet()) {
            provider.createNetwork().joinLink(node);
        }
    }

    /**
     * Проверяет, что узел {@code connectsTo} имеет обратную связь на позицию {@code connectFrom}.
     * Аналог checkConnection из 1.7.10.
     */
    public static boolean checkConnection(GenNode<?> connectsTo, NodeDirPos connectFrom, boolean skipSideCheck) {
        for (NodeDirPos revCon : connectsTo.connections) {
            // revCon.pos - revCon.dir.step == connectFrom.pos
            BlockPos revBack = revCon.getDir() != null
                    ? revCon.getPos().relative(revCon.getDir().getOpposite())
                    : revCon.getPos(); // direction=null → "any", принимаем позицию как есть

            if (!revBack.equals(connectFrom.getPos())) continue;

            if (skipSideCheck) return true;

            // Оба направления должны быть противоположными или оба null
            if (revCon.getDir() == null && connectFrom.getDir() == null) return true;
            if (revCon.getDir() != null && connectFrom.getDir() != null
                    && revCon.getDir() == connectFrom.getDir().getOpposite()) return true;
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void connectToNode(GenNode origin, GenNode connection) {
        if (origin.hasValidNet() && connection.hasValidNet()) {
            if (origin.net.links.size() > connection.net.links.size()) {
                origin.net.joinNetworks(connection.net);
            } else {
                connection.net.joinNetworks(origin.net);
            }
        } else if (!origin.hasValidNet() && connection.hasValidNet()) {
            connection.net.joinLink(origin);
        } else if (origin.hasValidNet()) {
            origin.net.joinLink(connection);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void updateNetworks() {
        for (NodeNet net : activeNodeNets) net.resetTrackers();
        for (NodeNet net : activeNodeNets) net.update();

        if (reapTimer <= 0) {
            activeNodeNets.forEach(net -> net.links.removeIf(link -> ((GenNode<?>) link).expired));
            activeNodeNets.removeIf(net -> net.links.isEmpty());
        }
    }

    private static void updateReapTimer() {
        if (reapTimer <= 0) reapTimer = 5 * 60 * 20; // 5 минут
        else reapTimer--;
    }
}
