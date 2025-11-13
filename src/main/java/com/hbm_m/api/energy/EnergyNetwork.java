package com.hbm_m.api.energy;

import com.hbm_m.block.entity.MachineBatteryBlockEntity;
import com.hbm_m.capability.ModCapabilities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.*;

public class EnergyNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EnergyNetworkManager manager;
    private final Set<EnergyNode> nodes = new HashSet<>();
    private final UUID id = UUID.randomUUID();

    public EnergyNetwork(EnergyNetworkManager manager) {
        this.manager = manager;
    }

    // ============================================================================================
    //  НОВЫЙ, УЛУЧШЕННЫЙ МЕТОД TICK (v4)
    // ============================================================================================
    public void tick(ServerLevel level) {
        // 1. Очистка и проверка
        nodes.removeIf(node -> !node.isValid(level) || node.getNetwork() != this);
        if (nodes.size() < 2) return;

        // 2. Сбор информации об участниках сети
        List<IEnergyProvider> generators = new ArrayList<>();
        List<IEnergyReceiver> consumers = new ArrayList<>();
        List<Battery> batteries = new ArrayList<>();

        for (EnergyNode node : nodes) {
            BlockEntity be = level.getBlockEntity(node.getPos());
            if (be == null) continue;

            Optional<IEnergyProvider> providerCap = be.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
            Optional<IEnergyReceiver> receiverCap = be.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();

            // Определяем, является ли узел батареей (и Provider, и Receiver)
            if (providerCap.isPresent() && receiverCap.isPresent()) {
                int mode = (be instanceof MachineBatteryBlockEntity batteryBE) ? batteryBE.getCurrentMode() : 0; // 0 = BOTH
                batteries.add(new Battery(receiverCap.get(), providerCap.get(), mode));
            } else if (providerCap.isPresent()) {
                generators.add(providerCap.get());
            } else if (receiverCap.isPresent()) {
                consumers.add(receiverCap.get());
            }
        }

        // --- ФАЗА 1: Удовлетворение потребителей (машин) ---
        long production = 0;
        Map<IEnergyProvider, Long> providerPool = new IdentityHashMap<>();

        // Собираем энергию с генераторов
        for (IEnergyProvider generator : generators) {
            if (generator.canExtract()) {
                long canProvide = Math.min(generator.getEnergyStored(), generator.getProvideSpeed());
                if (canProvide > 0) {
                    production += canProvide;
                    providerPool.put(generator, canProvide);
                }
            }
        }
        // Добавляем энергию с батарей в режиме "OUTPUT" или "BOTH"
        for (Battery battery : batteries) {
            if (battery.isOutputting() && battery.provider.canExtract()) {
                long canProvide = Math.min(battery.provider.getEnergyStored(), battery.provider.getProvideSpeed());
                if (canProvide > 0) {
                    production += canProvide;
                    providerPool.put(battery.provider, canProvide);
                }
            }
        }

        // Сортируем потребителей по приоритету
        consumers.sort(Comparator.comparing(IEnergyReceiver::getPriority).reversed());
        long totalDemand = 0;
        Map<IEnergyReceiver, Long> consumerDemand = new IdentityHashMap<>();

        for (IEnergyReceiver consumer : consumers) {
            if (consumer.canReceive()) {
                long needed = Math.min(consumer.getMaxEnergyStored() - consumer.getEnergyStored(), consumer.getReceiveSpeed());
                if (needed > 0) {
                    totalDemand += needed;
                    consumerDemand.put(consumer, needed);
                }
            }
        }

        // Распределяем энергию, если есть спрос и предложение
        if (totalDemand > 0 && production > 0) {
            long energyToDistribute = Math.min(production, totalDemand);
            distributeEnergy(energyToDistribute, providerPool, consumerDemand);
        }

        // --- ФАЗА 2: Зарядка батарей от генераторов ---
        long remainingProduction = 0;
        Map<IEnergyProvider, Long> generatorPool = new IdentityHashMap<>();
        for (IEnergyProvider generator : generators) {
            // Пересчитываем доступную энергию, так как часть могла быть потрачена
            if (generator.canExtract()) {
                long canProvide = Math.min(generator.getEnergyStored(), generator.getProvideSpeed());
                if (canProvide > 0) {
                    remainingProduction += canProvide;
                    generatorPool.put(generator, canProvide);
                }
            }
        }

        List<IEnergyReceiver> chargeableBatteries = new ArrayList<>();
        long batteryDemand = 0;
        for (Battery battery : batteries) {
            if (battery.isInputting() && battery.receiver.canReceive()) {
                chargeableBatteries.add(battery.receiver);
                batteryDemand += Math.min(battery.receiver.getMaxEnergyStored() - battery.receiver.getEnergyStored(), battery.receiver.getReceiveSpeed());
            }
        }

        if (remainingProduction > 0 && batteryDemand > 0) {
            long energyToCharge = Math.min(remainingProduction, batteryDemand);
            // Распределяем поровну, игнорируя спрос каждой отдельной батареи
            distributeEvenly(energyToCharge, generatorPool, chargeableBatteries);
        }

        // --- ФАЗА 3: Балансировка между батареями (только в режиме BOTH) ---
        List<Battery> balancingBatteries = batteries.stream().filter(b -> b.mode == 0).toList(); // mode 0 = BOTH
        if (balancingBatteries.size() > 1) {
            long totalEnergyInBalancers = 0;
            long totalCapacityOfBalancers = 0;

            for (Battery battery : balancingBatteries) {
                totalEnergyInBalancers += battery.provider.getEnergyStored();
                totalCapacityOfBalancers += battery.provider.getMaxEnergyStored();
            }

            if (totalCapacityOfBalancers > 0) {
                double averagePercentage = (double) totalEnergyInBalancers / totalCapacityOfBalancers;

                List<Battery> givers = new ArrayList<>();
                List<Battery> takers = new ArrayList<>();
                long totalToGive = 0;
                long totalToTake = 0;

                for (Battery battery : balancingBatteries) {
                    long targetEnergy = (long) (battery.provider.getMaxEnergyStored() * averagePercentage);
                    long currentEnergy = battery.provider.getEnergyStored();
                    long diff = currentEnergy - targetEnergy;

                    if (diff > 0) { // Эта батарея должна отдать
                        long canGive = Math.min(diff, battery.provider.getProvideSpeed());
                        if (canGive > 0) {
                            givers.add(battery);
                            totalToGive += canGive;
                        }
                    } else if (diff < 0) { // Эта батарея должна принять
                        long canTake = Math.min(-diff, battery.receiver.getReceiveSpeed());
                        if (canTake > 0) {
                            takers.add(battery);
                            totalToTake += canTake;
                        }
                    }
                }

                if (totalToGive > 0 && totalToTake > 0) {
                    long transferAmount = Math.min(totalToGive, totalToTake);

                    Map<IEnergyProvider, Long> balancingGiversPool = new IdentityHashMap<>();
                    givers.forEach(g -> balancingGiversPool.put(g.provider, Math.min(g.provider.getEnergyStored(), g.provider.getProvideSpeed())));

                    Map<IEnergyReceiver, Long> balancingTakersDemand = new IdentityHashMap<>();
                    takers.forEach(t -> balancingTakersDemand.put(t.receiver, Math.min(t.receiver.getMaxEnergyStored() - t.receiver.getEnergyStored(), t.receiver.getReceiveSpeed())));

                    distributeEnergy(transferAmount, balancingGiversPool, balancingTakersDemand);
                }
            }
        }
    }

    /**
     * Распределяет заданное количество энергии от провайдеров к потребителям пропорционально их спросу.
     */
    private void distributeEnergy(long amount, Map<IEnergyProvider, Long> providers, Map<IEnergyReceiver, Long> consumers) {
        if (amount <= 0 || providers.isEmpty() || consumers.isEmpty()) return;

        long totalDemand = consumers.values().stream().mapToLong(Long::longValue).sum();
        if (totalDemand <= 0) return;

        long energyToDistribute = amount;

        for (Map.Entry<IEnergyReceiver, Long> consumerEntry : consumers.entrySet()) {
            if (energyToDistribute <= 0) break;
            IEnergyReceiver consumer = consumerEntry.getKey();
            long demand = consumerEntry.getValue();

            double share = (double) demand / totalDemand;
            long energyForThisConsumer = (long) (amount * share);
            energyForThisConsumer = Math.min(energyToDistribute, energyForThisConsumer);

            if (energyForThisConsumer > 0) {
                long accepted = consumer.receiveEnergy(energyForThisConsumer, false);
                energyToDistribute -= accepted;
            }
        }

        long actuallyDistributed = amount - energyToDistribute;
        extractFromProviders(actuallyDistributed, providers);
    }

    /**
     * Распределяет заданное количество энергии от провайдеров к списку потребителей поровну.
     */
    private void distributeEvenly(long amount, Map<IEnergyProvider, Long> providers, List<IEnergyReceiver> consumers) {
        if (amount <= 0 || providers.isEmpty() || consumers.isEmpty()) return;

        long totalDistributed = 0;
        long remainingToDistribute = amount;

        // Используем Set для удаления полностью заряженных потребителей
        Set<IEnergyReceiver> activeConsumers = new HashSet<>(consumers);

        while (remainingToDistribute > 0 && !activeConsumers.isEmpty()) {
            long amountPerConsumer = Math.max(1, remainingToDistribute / activeConsumers.size());
            long acceptedInLoop = 0;

            Iterator<IEnergyReceiver> iterator = activeConsumers.iterator();
            while(iterator.hasNext()) {
                IEnergyReceiver consumer = iterator.next();
                if (remainingToDistribute <= 0) break;

                long toGive = Math.min(amountPerConsumer, remainingToDistribute);
                long accepted = consumer.receiveEnergy(toGive, false);

                acceptedInLoop += accepted;
                totalDistributed += accepted;
                remainingToDistribute -= accepted;

                if (!consumer.canReceive()) {
                    iterator.remove();
                }
            }
            // Если за целый цикл никто ничего не принял, выходим, чтобы избежать бесконечного цикла
            if (acceptedInLoop == 0) break;
        }

        if (totalDistributed > 0) {
            extractFromProviders(totalDistributed, providers);
        }
    }

    /**
     * Извлекает заданное количество энергии из пула провайдеров пропорционально их вкладу.
     */
    private void extractFromProviders(long amount, Map<IEnergyProvider, Long> providers) {
        if (amount <= 0 || providers.isEmpty()) return;

        long totalProvided = providers.values().stream().mapToLong(Long::longValue).sum();
        if (totalProvided <= 0) return;

        long energyToExtract = amount;

        for (Map.Entry<IEnergyProvider, Long> providerEntry : providers.entrySet()) {
            if (energyToExtract <= 0) break;

            double share = (double) providerEntry.getValue() / totalProvided;
            long toExtract = (long) (amount * share);
            toExtract = Math.min(energyToExtract, toExtract);

            if (toExtract > 0) {
                long extracted = providerEntry.getKey().extractEnergy(toExtract, false);
                energyToExtract -= extracted;
            }
        }
    }

    /** Вспомогательный класс для хранения информации о батареях */
    private static class Battery {
        final IEnergyReceiver receiver;
        final IEnergyProvider provider;
        final int mode; // 0=BOTH, 1=INPUT, 2=OUTPUT, 3=DISABLED

        Battery(IEnergyReceiver r, IEnergyProvider p, int mode) {
            this.receiver = r;
            this.provider = p;
            this.mode = mode;
        }

        boolean isInputting() {
            return mode == 0 || mode == 1;
        }

        boolean isOutputting() {
            return mode == 0 || mode == 2;
        }
    }


    // --- ЛОГИКА УПРАВЛЕНИЯ УЗЛАМИ (без изменений, она у тебя хорошая) ---

    public void addNode(EnergyNode node) {
        if (nodes.add(node)) {
            node.setNetwork(this);
        }
    }

    public void removeNode(EnergyNode node) {
        if (!nodes.remove(node)) return;

        node.setNetwork(null);

        if (nodes.size() < 2) {
            LOGGER.debug("[NETWORK] Network {} dissolved (size < 2).", id);
            for (EnergyNode remainingNode : nodes) {
                remainingNode.setNetwork(null);
            }
            nodes.clear();
            manager.removeNetwork(this);
        } else {
            verifyConnectivity();
        }
    }

    private void verifyConnectivity() {
        if (nodes.isEmpty()) return;
        Set<EnergyNode> allReachableNodes = new HashSet<>();
        Queue<EnergyNode> queue = new LinkedList<>();

        EnergyNode startNode = nodes.iterator().next();
        queue.add(startNode);
        allReachableNodes.add(startNode);

        while (!queue.isEmpty()) {
            EnergyNode current = queue.poll();
            for (Direction dir : Direction.values()) {
                EnergyNode neighbor = manager.getNode(current.getPos().relative(dir));
                if (neighbor != null && nodes.contains(neighbor) && allReachableNodes.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        if (allReachableNodes.size() < nodes.size()) {
            LOGGER.warn("[NETWORK] Network split detected in network {}! Initiating rebuild.", id);

            Set<EnergyNode> lostNodes = new HashSet<>(nodes);
            lostNodes.removeAll(allReachableNodes);

            nodes.removeAll(lostNodes);
            for (EnergyNode lostNode : lostNodes) {
                lostNode.setNetwork(null);
                manager.removeNode(lostNode.getPos());
                manager.addNode(lostNode.getPos());
            }
        }
    }

    public void merge(EnergyNetwork other) {
        if (this == other) return;

        for (EnergyNode node : other.nodes) {
            node.setNetwork(this);
            this.nodes.add(node);
        }

        other.nodes.clear();
        manager.removeNetwork(other);
    }

    public UUID getId() { return id; }
    @Override public boolean equals(Object o) { return this == o || (o instanceof EnergyNetwork that && id.equals(that.id)); }
    @Override public int hashCode() { return id.hashCode(); }
}