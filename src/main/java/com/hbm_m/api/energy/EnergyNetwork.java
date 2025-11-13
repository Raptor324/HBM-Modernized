package com.hbm_m.api.energy;

import com.hbm_m.capability.ModCapabilities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class EnergyNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EnergyNetworkManager manager;
    private final Set<EnergyNode> nodes = new HashSet<>();
    private final UUID id = UUID.randomUUID();

    public EnergyNetwork(EnergyNetworkManager manager) {
        this.manager = manager;
        //LOGGER.debug("[NETWORK] Created network {}", id);
    }

    // ✅ Метод tick остается таким же, как в предыдущем ответе (v3, исправленный)
    public void tick(ServerLevel level) {
        nodes.removeIf(node -> !node.isValid(level) || node.getNetwork() != this);
        if (nodes.size() < 2) return;
        List<IEnergyReceiver> consumers = new ArrayList<>();
        List<BatteryInfo> batteries = new ArrayList<>();
        List<IEnergyProvider> generators = new ArrayList<>();
        for (EnergyNode node : nodes) {
            BlockEntity be = level.getBlockEntity(node.getPos());
            if (be == null) continue;
            Optional<IEnergyProvider> providerCap = be.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
            Optional<IEnergyReceiver> receiverCap = be.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();
            if (providerCap.isPresent() && receiverCap.isPresent()) {
                batteries.add(new BatteryInfo(receiverCap.get(), providerCap.get()));
            } else if (providerCap.isPresent()) {
                generators.add(providerCap.get());
            } else if (receiverCap.isPresent()) {
                consumers.add(receiverCap.get());
            }
        }
        long energyPool = 0;
        Map<IEnergyProvider, Long> extractedFrom = new IdentityHashMap<>();
        for (IEnergyProvider generator : generators) {
            if (generator.canExtract()) {
                long canExtract = Math.min(generator.getEnergyStored(), generator.getProvideSpeed());
                if (canExtract > 0) {
                    long extracted = generator.extractEnergy(canExtract, false);
                    energyPool += extracted;
                    extractedFrom.put(generator, extracted);
                }
            }
        }
        for (BatteryInfo battery : batteries) {
            if (battery.provider.canExtract()) {
                long canExtract = Math.min(battery.provider.getEnergyStored(), battery.provider.getProvideSpeed());
                if (canExtract > 0) {
                    long extracted = battery.provider.extractEnergy(canExtract, false);
                    energyPool += extracted;
                    extractedFrom.put(battery.provider, extractedFrom.getOrDefault(battery.provider, 0L) + extracted);
                }
            }
        }
        if (energyPool == 0) return;
        consumers.sort(Comparator.comparing(IEnergyReceiver::getPriority).reversed());
        for (IEnergyReceiver consumer : consumers) {
            if (energyPool <= 0) break;
            if (consumer.canReceive()) {
                long needed = Math.min(consumer.getMaxEnergyStored() - consumer.getEnergyStored(), consumer.getReceiveSpeed());
                long toGive = Math.min(energyPool, needed);
                if (toGive > 0) {
                    long received = consumer.receiveEnergy(toGive, false);
                    energyPool -= received;
                }
            }
        }
        List<BatteryInfo> chargeableBatteries = batteries.stream().filter(b -> b.receiver.canReceive()).sorted(Comparator.comparingDouble(BatteryInfo::getStoredPercentage)).collect(Collectors.toList());
        if (energyPool > 0 && !chargeableBatteries.isEmpty()) {
            while(energyPool > 0){
                long totalReceivedInLoop = 0;
                long amountPerBattery = Math.max(1, energyPool / chargeableBatteries.size());
                for(BatteryInfo battery : chargeableBatteries) {
                    if(energyPool <= 0) break;
                    long toGive = Math.min(amountPerBattery, energyPool);
                    long received = battery.receiver.receiveEnergy(toGive, false);
                    energyPool -= received;
                    totalReceivedInLoop += received;
                }
                if(totalReceivedInLoop == 0) break;
            }
        }
        if (energyPool > 0) returnToSources(extractedFrom, energyPool);
    }

    // ... вспомогательные классы и методы для tick ...
    private void returnToSources(Map<IEnergyProvider, Long> contributions, long remaining) {
        if (remaining <= 0 || contributions.isEmpty()) return;
        long totalContributed = contributions.values().stream().mapToLong(Long::longValue).sum();
        if (totalContributed == 0) return;
        for (Map.Entry<IEnergyProvider, Long> entry : contributions.entrySet()) {
            if (remaining <= 0) break;
            IEnergyProvider source = entry.getKey();
            if (source instanceof IEnergyReceiver receiver && receiver.canReceive()) {
                double share = (double) entry.getValue() / totalContributed;
                long toReturn = (long) (remaining * share);
                if (toReturn > 0) {
                    long returned = receiver.receiveEnergy(toReturn, false);
                    remaining -= returned;
                }
            }
        }
    }
    private static class BatteryInfo {
        final IEnergyReceiver receiver; final IEnergyProvider provider;
        BatteryInfo(IEnergyReceiver r, IEnergyProvider p) { this.receiver = r; this.provider = p; }
        double getStoredPercentage() { long max = provider.getMaxEnergyStored(); return max == 0 ? 0 : (double) provider.getEnergyStored() / max; }
    }


    // --- НОВАЯ, БОЛЕЕ НАДЁЖНАЯ ЛОГИКА УПРАВЛЕНИЯ УЗЛАМИ ---

    public void addNode(EnergyNode node) {
        if (nodes.add(node)) {
            node.setNetwork(this);
        }
    }

    public void removeNode(EnergyNode node) {
        if (!nodes.remove(node)) return; // Если узла и не было, выходим

        node.setNetwork(null); // Очищаем ссылку

        if (nodes.size() < 2) {
            // Если осталось 0 или 1 узел, сеть больше не нужна
            LOGGER.debug("[NETWORK] Network {} dissolved (size < 2).", id);
            for (EnergyNode remainingNode : nodes) {
                remainingNode.setNetwork(null); // Очищаем ссылку у последнего узла
            }
            nodes.clear();
            manager.removeNetwork(this);
        } else {
            // ✅ ИЗМЕНЕНО: Проверяем связность сети после удаления узла
            verifyConnectivity();
        }
    }

    private void verifyConnectivity() {
        Set<EnergyNode> allReachableNodes = new HashSet<>();
        Queue<EnergyNode> queue = new LinkedList<>();

        // Начинаем поиск с произвольного узла
        EnergyNode startNode = nodes.iterator().next();
        queue.add(startNode);
        allReachableNodes.add(startNode);

        while (!queue.isEmpty()) {
            EnergyNode current = queue.poll();
            for (Direction dir : Direction.values()) {
                // Ищем соседа в глобальном менеджере, но проверяем, что он принадлежит НАШЕЙ сети
                EnergyNode neighbor = manager.getNode(current.getPos().relative(dir));
                if (neighbor != null && nodes.contains(neighbor) && allReachableNodes.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        // Если количество достижимых узлов меньше, чем всего узлов в сети, значит, произошло разделение
        if (allReachableNodes.size() < nodes.size()) {
            LOGGER.warn("[NETWORK] Network split detected in network {}! Initiating rebuild.", id);

            // ✅ НОВАЯ ЛОГИКА SPLIT:
            // 1. Собираем все узлы, которые "откололись"
            Set<EnergyNode> lostNodes = new HashSet<>(nodes);
            lostNodes.removeAll(allReachableNodes);

            // 2. Удаляем их из текущей сети
            nodes.removeAll(lostNodes);
            for (EnergyNode lostNode : lostNodes) {
                lostNode.setNetwork(null);
                // 3. Заставляем менеджер полностью пересоздать для них сеть
                manager.removeNode(lostNode.getPos()); // Удаляем из менеджера
                manager.addNode(lostNode.getPos());    // И тут же добавляем заново
            }
        }
    }

    public void merge(EnergyNetwork other) {
        if (this == other) return;

        // Перемещаем все узлы из другой сети в эту
        for (EnergyNode node : other.nodes) {
            node.setNetwork(this);
            this.nodes.add(node);
        }

        // Очищаем и удаляем старую сеть
        other.nodes.clear();
        manager.removeNetwork(other);
    }

    // Старый метод split() больше не нужен, его логика теперь в verifyConnectivity()

    // --- Геттеры ---
    public UUID getId() { return id; }
    @Override public boolean equals(Object o) { return this == o || (o instanceof EnergyNetwork that && id.equals(that.id)); }
    @Override public int hashCode() { return id.hashCode(); }
}