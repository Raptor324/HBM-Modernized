package com.hbm_m.api.energy;

import com.hbm_m.capability.ModCapabilities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.*;

/**
 * Энергетическая сеть HBM.
 * Содержит узлы (провода и машины) и управляет передачей энергии между ними.
 */
public class EnergyNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EnergyNetworkManager manager;
    private final Set<EnergyNode> nodes = new HashSet<>();
    private final UUID id = UUID.randomUUID();

    public EnergyNetwork(EnergyNetworkManager manager) {
        this.manager = manager;
        LOGGER.debug("[NETWORK] Created new network with ID {}", id);
    }

    /**
     * Тик сети - передача энергии от провайдеров к приемникам
     */
    public void tick(ServerLevel level) {
        // Удаляем невалидные узлы
        nodes.removeIf(node -> !node.isValid(level) || node.getNetwork() != this);

        if (nodes.size() < 2) {
            LOGGER.debug("[NETWORK] Network {} has {} nodes, skipping tick", id, nodes.size());
            return;
        }

        LOGGER.info("[NETWORK] Network {} ticking with {} nodes", id, nodes.size());

        // Собираем валидные BlockEntity
        List<BlockEntity> validEntities = new ArrayList<>();
        for (EnergyNode node : nodes) {
            BlockEntity be = level.getBlockEntity(node.getPos());
            if (be != null) {
                validEntities.add(be);
            }
        }

        if (validEntities.isEmpty()) {
            LOGGER.debug("[NETWORK]   No valid block entities found");
            return;
        }

        // Разделяем на провайдеров и приемников
        List<IEnergyProvider> providers = new ArrayList<>();
        List<IEnergyReceiver> receivers = new ArrayList<>();

        for (BlockEntity be : validEntities) {
            be.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).ifPresent(p -> {
                providers.add(p);
                LOGGER.debug("[NETWORK]   Found provider at {} with {} HE", be.getBlockPos(), p.getEnergyStored());
            });
            be.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).ifPresent(r -> {
                receivers.add(r);
                LOGGER.debug("[NETWORK]   Found receiver at {} ({}/{})", be.getBlockPos(), r.getEnergyStored(), r.getMaxEnergyStored());
            });
        }

        if (providers.isEmpty() || receivers.isEmpty()) {
            LOGGER.debug("[NETWORK]   Providers: {}, Receivers: {} - cannot transfer", providers.size(), receivers.size());
            return;
        }

        // ШАГ 1: ИЗВЛЕКАЕМ энергию из провайдеров
        long totalExtracted = 0;
        Map<IEnergyProvider, Long> extractedMap = new IdentityHashMap<>();

        for (IEnergyProvider provider : providers) {
            if (!provider.canExtract()) continue;

            long canProvide = Math.min(provider.getEnergyStored(), provider.getProvideSpeed());
            if (canProvide > 0) {
                long extracted = provider.extractEnergy(canProvide, false);
                if (extracted > 0) {
                    extractedMap.put(provider, extracted);
                    totalExtracted += extracted;
                    LOGGER.debug("[NETWORK]   Extracted {} HE from provider", extracted);
                }
            }
        }

        if (totalExtracted == 0) {
            LOGGER.debug("[NETWORK]   No energy extracted");
            return;
        }

        LOGGER.info("[NETWORK]   Total extracted: {} HE", totalExtracted);

        // ШАГ 2: РАЗДАЕМ энергию приемникам по приоритету
        receivers.sort(Comparator.comparing(IEnergyReceiver::getPriority).reversed());

        long remaining = totalExtracted;
        for (IEnergyReceiver receiver : receivers) {
            if (remaining <= 0) break;
            if (!receiver.canReceive()) continue;

            long needed = Math.min(
                    receiver.getMaxEnergyStored() - receiver.getEnergyStored(),
                    receiver.getReceiveSpeed()
            );

            if (needed <= 0) continue;

            long toGive = Math.min(remaining, needed);
            long actuallyReceived = receiver.receiveEnergy(toGive, false);
            remaining -= actuallyReceived;

            LOGGER.debug("[NETWORK]   Transferred {} HE to receiver (needed: {})", actuallyReceived, needed);
        }

        LOGGER.info("[NETWORK]   Transfer complete. Remaining: {} HE", remaining);

        // ШАГ 3: Если осталась энергия - возвращаем провайдерам пропорционально
        if (remaining > 0) {
            LOGGER.debug("[NETWORK]   Returning {} HE to providers", remaining);
            for (Map.Entry<IEnergyProvider, Long> entry : extractedMap.entrySet()) {
                IEnergyProvider provider = entry.getKey();

                if (provider instanceof IEnergyReceiver receiverProvider) {
                    double share = (double) entry.getValue() / totalExtracted;
                    long toReturn = (long)(remaining * share);

                    if (toReturn > 0) {
                        receiverProvider.receiveEnergy(toReturn, false);
                        LOGGER.debug("[NETWORK]   Returned {} HE to provider", toReturn);
                    }
                }
            }
        }
    }

    /**
     * Добавляет узел в сеть
     */
    public void addNode(EnergyNode node) {
        if (nodes.contains(node)) {
            LOGGER.debug("[NETWORK] Node at {} already in network {}", node.getPos(), id);
            return;
        }

        LOGGER.info("[NETWORK] Adding node at {} to network {}", node.getPos(), id);
        nodes.add(node);
        node.setNetwork(this);

        // Ищем соседние сети
        Set<EnergyNetwork> neighborNetworks = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = node.getPos().relative(dir);
            EnergyNode neighborNode = manager.getNode(neighborPos);

            if (neighborNode != null && neighborNode.getNetwork() != null && neighborNode.getNetwork() != this) {
                neighborNetworks.add(neighborNode.getNetwork());
                LOGGER.debug("[NETWORK]   Found neighbor network {} in direction {}", neighborNode.getNetwork().getId(), dir);
            }
        }

        // Объединяем найденные сети
        for (EnergyNetwork netToMerge : neighborNetworks) {
            if (netToMerge != this) {
                merge(netToMerge);
            }
        }
    }

    /**
     * Удаляет узел из сети
     */
    public void removeNode(EnergyNode node) {
        if (!nodes.remove(node)) {
            LOGGER.debug("[NETWORK] Node at {} not in network {}", node.getPos(), id);
            return;
        }

        LOGGER.info("[NETWORK] Removing node at {} from network {}", node.getPos(), id);
        node.setNetwork(null);

        if (nodes.isEmpty()) {
            LOGGER.info("[NETWORK] Network {} is now empty, removing", id);
            manager.removeNetwork(this);
            return;
        }

        // BFS проверка связности
        Set<EnergyNode> visited = new HashSet<>();
        Queue<EnergyNode> queue = new LinkedList<>();

        EnergyNode start = nodes.iterator().next();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            EnergyNode current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.getPos().relative(dir);
                EnergyNode neighborNode = manager.getNode(neighborPos);

                if (neighborNode != null && nodes.contains(neighborNode) && !visited.contains(neighborNode)) {
                    visited.add(neighborNode);
                    queue.add(neighborNode);
                }
            }
        }

        // Если не все узлы достижимы - разбиваем сеть
        if (visited.size() < nodes.size()) {
            LOGGER.warn("[NETWORK] Network {} split detected! Visited: {}, Total: {}", id, visited.size(), nodes.size());
            split();
        }
    }

    /**
     * Объединяет две сети в одну
     */
    public void merge(EnergyNetwork other) {
        if (this == other || other.nodes.isEmpty()) {
            LOGGER.debug("[NETWORK] Cannot merge network {} with itself or empty network", id);
            return;
        }

        LOGGER.info("[NETWORK] Merging network {} ({} nodes) into {} ({} nodes)",
                other.id, other.nodes.size(), this.id, this.nodes.size());

        Set<EnergyNode> nodesToMove = new HashSet<>(other.nodes);

        for (EnergyNode node : nodesToMove) {
            other.nodes.remove(node);
            this.nodes.add(node);
            node.setNetwork(this);
        }

        manager.removeNetwork(other);
        LOGGER.info("[NETWORK] Merge complete. Network {} now has {} nodes", id, nodes.size());
    }

    /**
     * Разделяет сеть на несколько независимых сетей (после удаления узла)
     */
    private void split() {
        LOGGER.info("[NETWORK] Splitting network {} ({} nodes)", id, nodes.size());

        Set<EnergyNode> remaining = new HashSet<>(nodes);
        List<Set<EnergyNode>> islands = new ArrayList<>();

        // Находим изолированные группы узлов (BFS)
        while (!remaining.isEmpty()) {
            EnergyNode start = remaining.iterator().next();
            Set<EnergyNode> island = new HashSet<>();
            Queue<EnergyNode> queue = new LinkedList<>();

            queue.add(start);
            island.add(start);
            remaining.remove(start);

            while (!queue.isEmpty()) {
                EnergyNode current = queue.poll();

                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = current.getPos().relative(dir);
                    EnergyNode neighborNode = manager.getNode(neighborPos);

                    if (neighborNode != null && remaining.contains(neighborNode)) {
                        island.add(neighborNode);
                        queue.add(neighborNode);
                        remaining.remove(neighborNode);
                    }
                }
            }

            islands.add(island);
            LOGGER.debug("[NETWORK]   Found island with {} nodes", island.size());
        }

        // Очищаем текущую сеть
        for (EnergyNode node : nodes) {
            node.setNetwork(null);
        }
        nodes.clear();
        manager.removeNetwork(this);

        // Создаем новые сети для каждого "острова"
        for (Set<EnergyNode> island : islands) {
            EnergyNetwork newNet = new EnergyNetwork(manager);
            manager.addNetwork(newNet);

            for (EnergyNode node : island) {
                newNet.nodes.add(node);
                node.setNetwork(newNet);
            }

            LOGGER.info("[NETWORK] Created new network {} with {} nodes after split", newNet.id, island.size());
        }
    }

    // --- GETTERS ---

    public UUID getId() {
        return id;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof EnergyNetwork that && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
