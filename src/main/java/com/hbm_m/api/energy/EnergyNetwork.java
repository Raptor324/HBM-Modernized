package com.hbm_m.api.energy; // <-- Убедись, что твой package правильный

import com.hbm_m.block.entity.machine.MachineBatteryBlockEntity;
import com.hbm_m.capability.ModCapabilities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

import java.util.*;

/**
 * ===================================================================
 * EnergyNetwork.java - ВЕРСИЯ 6.2
 * * - Приоритеты: LOW, NORMAL, HIGH
 * - Распределение: Пропорционально внутри групп по приоритету
 * - Батареи (Input/Both) и Машины конкурируют в одних группах
 * - Батареи (Output/Both) считаются генераторами
 * ===================================================================
 */
public class EnergyNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EnergyNetworkManager manager;
    private final Set<EnergyNode> nodes = new HashSet<>();
    private final UUID id = UUID.randomUUID();

    public EnergyNetwork(EnergyNetworkManager manager) {
        this.manager = manager;
    }

    /**
     * НОВАЯ ЛОГИКА TICK (v6.0) - Поддержка пропорционального распределения по приоритетам
     */
    public void tick(ServerLevel level) {
        // 1. Валидация узлов
        int sizeBefore = nodes.size();
        nodes.removeIf(node -> !node.isValid(level) || node.getNetwork() != this);

        if (nodes.size() < sizeBefore) {
            // Если узлы были удалены (стали невалидными),
            // мы ОБЯЗАНЫ проверить, не раскололась ли сеть.
            verifyConnectivity();
        }

        if (nodes.size() < 2) {
            // verifyConnectivity() мог распустить эту сеть (если она раскололась
            // на части < 2 узлов), или removeIf() удалил последний узел.
            // В любом случае, тикать больше нечего.
            return;
        }

        // 2. Сбор всех участников сети
        Set<IEnergyProvider> generators = new HashSet<>();

        // Группируем потребителей
        Map<IEnergyReceiver.Priority, Set<IEnergyReceiver>> consumersByPriority = new EnumMap<>(IEnergyReceiver.Priority.class);
        Map<IEnergyReceiver, Long> consumerDemand = new IdentityHashMap<>();
        List<BatteryInfo> batteries = new ArrayList<>();

        long totalConsumption = 0;

        for (EnergyNode node : nodes) {
            BlockEntity be = level.getBlockEntity(node.getPos());
            if (be == null) continue;

            Optional<IEnergyProvider> providerCap = be.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
            Optional<IEnergyReceiver> receiverCap = be.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();

            boolean isProvider = providerCap.isPresent();
            boolean isReceiver = receiverCap.isPresent();

            if (isProvider && isReceiver) {
                // Это батарея
                int mode = (be instanceof MachineBatteryBlockEntity batteryBE) ? batteryBE.getCurrentMode() : 0;
                BatteryInfo batteryInfo = new BatteryInfo(node.getPos(), receiverCap.get(), providerCap.get(), mode);
                batteries.add(batteryInfo); // Добавляем в общий список батарей

                // Батарея может быть и поставщиком, и потребителем
                // 1. Если она может отдавать (Output/Both) -> в список генераторов
                if (batteryInfo.canOutput() && batteryInfo.provider.canExtract()) {
                    long available = Math.min(batteryInfo.provider.getEnergyStored(), batteryInfo.provider.getProvideSpeed());
                    if (available > 0) {
                        generators.add(batteryInfo.provider); // Добавляем как IEnergyProvider
                    }
                }

                // 2. Если она может принимать (Input/Both) -> в список потребителей
                if (batteryInfo.canInput() && batteryInfo.receiver.canReceive()) {
                    long needed = Math.min(
                            batteryInfo.receiver.getMaxEnergyStored() - batteryInfo.receiver.getEnergyStored(),
                            batteryInfo.receiver.getReceiveSpeed()
                    );
                    if (needed > 0) {
                        consumerDemand.put(batteryInfo.receiver, needed);
                        consumersByPriority.computeIfAbsent(batteryInfo.receiver.getPriority(), k -> new HashSet<>()).add(batteryInfo.receiver);
                        totalConsumption += needed;
                    }
                }

            } else if (isProvider) {
                // Чистый генератор
                generators.add(providerCap.get());
            } else if (isReceiver) {
                // Чистый потребитель (машина)
                IEnergyReceiver consumer = receiverCap.get();
                if (consumer.canReceive()) {
                    long needed = Math.min(
                            consumer.getMaxEnergyStored() - consumer.getEnergyStored(),
                            consumer.getReceiveSpeed()
                    );
                    if (needed > 0) {
                        consumerDemand.put(consumer, needed);
                        consumersByPriority.computeIfAbsent(consumer.getPriority(), k -> new HashSet<>()).add(consumer);
                        totalConsumption += needed;
                    }
                }
            }
        }

        // ===== ФАЗА 1: Сбор энергии (Генераторы + Батареи в режиме Выхода) =====
        long totalGeneration = 0;
        Map<IEnergyProvider, Long> generatorCapacity = new IdentityHashMap<>();

        for (IEnergyProvider gen : generators) {
            // gen УЖЕ содержит и чистые генераторы, и батареи (Output/Both)
            if (gen.canExtract()) { // Доп. проверка
                long available = Math.min(gen.getEnergyStored(), gen.getProvideSpeed());
                if (available > 0) {
                    generatorCapacity.put(gen, available);
                    totalGeneration += available;
                }
            }
        }

        // ===== ФАЗА 2: Распределение энергии потребителям (по приоритетам) =====

        long energyToDistribute = Math.min(totalGeneration, totalConsumption);
        long remainingGeneration = totalGeneration; // Сколько всего останется

        if (energyToDistribute > 0) {
            // Идем по приоритетам от ВЫСШЕГО к НИЗШЕМУ
            // (Priority.values() по умолчанию: LOW, NORMAL, HIGH)
            for (int i = IEnergyReceiver.Priority.values().length - 1; i >= 0; i--) {
                IEnergyReceiver.Priority priority = IEnergyReceiver.Priority.values()[i];

                Set<IEnergyReceiver> currentGroup = consumersByPriority.get(priority);
                if (currentGroup == null || currentGroup.isEmpty()) continue;
                if (energyToDistribute <= 0) break; // Энергия кончилась

                // Считаем, сколько всего нужно этой группе
                long groupDemand = 0;
                for (IEnergyReceiver consumer : currentGroup) {
                    groupDemand += consumerDemand.getOrDefault(consumer, 0L);
                }
                if (groupDemand <= 0) continue;

                // Даем этой группе либо сколько она просит, либо сколько осталось
                long energyToGiveThisGroup = Math.min(energyToDistribute, groupDemand);

                // Распределяем энергию ВНУТРИ группы ПРОПОРЦИОНАЛЬНО
                long energyGiven = distributeProportionally(energyToGiveThisGroup, currentGroup, consumerDemand, generatorCapacity);

                energyToDistribute -= energyGiven;
                remainingGeneration -= energyGiven; // Уменьшаем общий остаток
            }
        }

        // ===== ФАЗА 3: Балансировка между батареями режима "BOTH" =====
        // (Остаток `remainingGeneration` уже в батареях, т.к. они были
        // и в списке `generators`, и в списке `consumersByPriority`)

        balanceBatteries(batteries);
    }

    /**
     * Распределяет энергию потребителям ОДНОГО приоритета
     * ПРОПОРЦИОНАЛЬНО их спросу.
     * @return Фактически распределенное количество энергии
     */
    private long distributeProportionally(long amount, Set<IEnergyReceiver> consumers,
                                          Map<IEnergyReceiver, Long> consumerDemand,
                                          Map<IEnergyProvider, Long> providers) {

        if (amount <= 0 || consumers.isEmpty() || providers.isEmpty()) return 0;

        // Считаем общий спрос *только* этой группы
        long totalGroupDemand = 0;
        for (IEnergyReceiver consumer : consumers) {
            totalGroupDemand += consumerDemand.getOrDefault(consumer, 0L);
        }
        if (totalGroupDemand <= 0) return 0;

        long totalEnergyGiven = 0;

        // Каждому потребителю выделяем долю пропорционально его спросу
        for (IEnergyReceiver consumer : consumers) {
            long demand = consumerDemand.getOrDefault(consumer, 0L);
            if (demand <= 0) continue;

            double share = (double) demand / totalGroupDemand;
            long energyForThis = (long) (amount * share);

            // Защита от ошибок округления, отдаем "слишком мало"
            if (energyForThis == 0 && amount > 0) {
                energyForThis = Math.min(amount, demand);
            }

            if (energyForThis > 0) {
                long accepted = consumer.receiveEnergy(energyForThis, false);
                if (accepted > 0) {
                    extractFromProviders(accepted, providers);
                    totalEnergyGiven += accepted;
                    amount -= accepted; // Уменьшаем общий пул на этот тик
                    // Обновляем карту спроса
                    consumerDemand.put(consumer, demand - accepted);
                }
            }
        }
        return totalEnergyGiven;
    }

    /**
     * Балансирует энергию между батареями в режиме "BOTH" (0)
     * (Код из v5.1, он здесь работает отлично)
     */
    private void balanceBatteries(List<BatteryInfo> batteries) {
        // 1. Группируем все 'BOTH' батареи по их приоритету
        Map<IEnergyReceiver.Priority, List<BatteryInfo>> batteriesByPriority = new EnumMap<>(IEnergyReceiver.Priority.class);
        for (BatteryInfo battery : batteries) {
            if (battery.mode == 0) { // Только режим "BOTH"
                batteriesByPriority
                        .computeIfAbsent(battery.receiver.getPriority(), k -> new ArrayList<>())
                        .add(battery);
            }
        }

        // 2. Запускаем логику балансировки для *каждой* группы отдельно
        for (List<BatteryInfo> priorityGroup : batteriesByPriority.values()) {
            balanceSinglePriorityGroup(priorityGroup);
        }
    }

    private void balanceSinglePriorityGroup(List<BatteryInfo> balancingBatteries) {
        if (balancingBatteries.size() < 2) return;

        // Вычисляем среднее заполнение *только для этой группы*
        long totalEnergy = 0;
        long totalCapacity = 0;

        for (BatteryInfo battery : balancingBatteries) {
            totalEnergy += battery.provider.getEnergyStored();
            totalCapacity += battery.provider.getMaxEnergyStored();
        }

        if (totalCapacity <= 0) return;
        double averagePercentage = (double) totalEnergy / totalCapacity;

        // 1. Находим раздающих и принимающих
        Map<BatteryInfo, Long> givers = new IdentityHashMap<>(); // Батарея -> Сколько может отдать
        Map<BatteryInfo, Long> takers = new IdentityHashMap<>(); // Батарея -> Сколько может принять

        long totalToGive = 0;
        long totalToTake = 0;
        long buffer = 100; // Буфер

        for (BatteryInfo battery : balancingBatteries) {
            long targetEnergy = (long) (battery.provider.getMaxEnergyStored() * averagePercentage);
            long currentEnergy = battery.provider.getEnergyStored();
            long diff = currentEnergy - targetEnergy;

            if (diff > buffer && battery.provider.canExtract()) { // Giver
                long canGive = Math.min(diff - buffer, battery.provider.getProvideSpeed());
                canGive = Math.min(canGive, battery.provider.getEnergyStored());
                if (canGive > 0) {
                    givers.put(battery, canGive);
                    totalToGive += canGive;
                }
            } else if (diff < -buffer && battery.receiver.canReceive()) { // Taker
                long canTake = Math.min(-diff - buffer, battery.receiver.getReceiveSpeed());
                canTake = Math.min(canTake, battery.receiver.getMaxEnergyStored() - currentEnergy);
                if (canTake > 0) {
                    takers.put(battery, canTake);
                    totalToTake += canTake;
                }
            }
        }

        if (givers.isEmpty() || takers.isEmpty() || totalToGive <= 0 || totalToTake <= 0) return;

        // 2. Находим, сколько энергии реально будет передано
        long totalToTransfer = Math.min(totalToGive, totalToTake);
        if (totalToTransfer <= 0) return;

        // 3. Распределяем `totalToTransfer` принимающим (пропорционально их "нужде")
        long transferredSoFar = 0;

        for (Map.Entry<BatteryInfo, Long> entry : takers.entrySet()) {
            if (transferredSoFar >= totalToTransfer) break;

            BatteryInfo taker = entry.getKey();
            long need = entry.getValue(); // Сколько он хочет

            double share = (double) need / totalToTake; // Его доля от общего спроса
            long energyForThis = (long) (totalToTransfer * share);
            energyForThis = Math.min(energyForThis, need); // Не больше, чем он хочет
            energyForThis = Math.min(energyForThis, totalToTransfer - transferredSoFar); // Не больше, чем осталось

            if (energyForThis > 0) {
                long accepted = taker.receiver.receiveEnergy(energyForThis, false);
                transferredSoFar += accepted;
            }
        }

        // 4. Извлекаем `transferredSoFar` (сколько РЕАЛЬНО приняли) из отдающих
        long extractedSoFar = 0;
        long totalToExtract = transferredSoFar; // Извлекаем ровно столько, сколько приняли

        for (Map.Entry<BatteryInfo, Long> entry : givers.entrySet()) {
            if (extractedSoFar >= totalToExtract) break;

            BatteryInfo giver = entry.getKey();
            long offer = entry.getValue(); // Сколько он хотел отдать

            double share = (double) offer / totalToGive; // Его доля от общего предложения
            long energyToExtract = (long) (totalToExtract * share);
            energyToExtract = Math.min(energyToExtract, offer); // Не больше, чем он предлагал
            energyToExtract = Math.min(energyToExtract, totalToExtract - extractedSoFar); // Не больше, чем осталось

            if (energyToExtract > 0) {
                long extracted = giver.provider.extractEnergy(energyToExtract, false);
                extractedSoFar += extracted;
            }
        }
    }


    /**
     * Извлекает энергию из пула провайдеров пропорционально их вкладу
     * (Код из v5.1, он здесь работает отлично)
     */
    private void extractFromProviders(long amount, Map<IEnergyProvider, Long> providers) {
        if (amount <= 0 || providers.isEmpty()) return;

        long totalCapacity = providers.values().stream().mapToLong(Long::longValue).sum();
        if (totalCapacity <= 0) {
            // Защита от бага, если емкость 0
            long amountPer = amount / providers.size();
            long remaining = amount;
            for(IEnergyProvider provider : new ArrayList<>(providers.keySet())) { // Копия, чтобы избежать ConcurrentModificationException
                if (remaining <= 0) break;
                long toExtract = Math.min(amountPer, remaining);
                if (toExtract <= 0) toExtract = remaining; // остатки

                long extracted = provider.extractEnergy(toExtract, false);
                remaining -= extracted;
                providers.computeIfPresent(provider, (p, cap) -> cap - extracted);
            }
            return;
        }

        long remaining = amount;
        List<Map.Entry<IEnergyProvider, Long>> providerList = new ArrayList<>(providers.entrySet());

        for (Map.Entry<IEnergyProvider, Long> entry : providerList) {
            if (remaining <= 0) break;

            IEnergyProvider provider = entry.getKey();
            long capacity = entry.getValue();
            if (capacity <= 0) continue;

            // Проверяем, не исчерпал ли провайдер свой пул
            if (totalCapacity <= 0) break;

            double share = (double) capacity / totalCapacity;
            long toExtract = (long) (amount * share);
            toExtract = Math.min(remaining, toExtract);
            toExtract = Math.min(capacity, toExtract); // Не больше, чем он может дать

            if (toExtract > 0) {
                long extracted = provider.extractEnergy(toExtract, false);
                remaining -= extracted;
                totalCapacity -= extracted; // Уменьшаем общую емкость пула
                entry.setValue(capacity - extracted); // Уменьшаем емкость провайдера
            }
        }

        // Если из-за ошибок округления что-то осталось, добираем
        if (remaining > 0) {
            providerList.sort(Map.Entry.<IEnergyProvider, Long>comparingByValue().reversed()); // Сначала из тех, у кого больше осталось

            for (Map.Entry<IEnergyProvider, Long> entry : providerList) {
                if (remaining <= 0) break;
                long capacity = entry.getValue();
                if (capacity <= 0) continue;

                long toExtract = Math.min(remaining, capacity);
                long extracted = entry.getKey().extractEnergy(toExtract, false);
                remaining -= extracted;
                entry.setValue(capacity - extracted);
            }
        }
    }

    /**
     * Вспомогательный класс для информации о батарее
     */
    private static class BatteryInfo {
        final BlockPos pos;
        final IEnergyReceiver receiver;
        final IEnergyProvider provider;
        final int mode; // 0=BOTH, 1=INPUT, 2=OUTPUT, 3=DISABLED

        BatteryInfo(BlockPos pos, IEnergyReceiver r, IEnergyProvider p, int mode) {
            this.pos = pos;
            this.receiver = r;
            this.provider = p;
            this.mode = mode;
        }

        boolean canInput() {
            return mode == 0 || mode == 1; // BOTH или INPUT
        }

        boolean canOutput() {
            return mode == 0 || mode == 2; // BOTH или OUTPUT
        }
    }

    // --- ЛОГИКА УПРАВЛЕНИЯ УЗЛАМИ ---
    // (Этот код из v5.1, он здесь работает отлично)

    public void addNode(EnergyNode node) {
        if (nodes.add(node)) {
            node.setNetwork(this);
            //LOGGER.debug("[NETWORK] Added node {} to network {}", node.getPos(), id);
        }
    }

    public void removeNode(EnergyNode node) {
        if (!nodes.remove(node)) return;

        node.setNetwork(null);
        //LOGGER.debug("[NETWORK] Removed node {} from network {}", node.getPos(), id);

        if (nodes.size() < 2) {
            //LOGGER.debug("[NETWORK] Network {} dissolved (size < 2).", id);
            for (EnergyNode remainingNode : nodes) {
                remainingNode.setNetwork(null);
            }
            nodes.clear();
            manager.removeNetwork(this);
        } else {
            verifyConnectivity();
        }
    }

    /**
     * Проверяет связность сети и разбивает её если нужно
     */
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
            LOGGER.warn("[NETWORK] Network {} split detected! Rebuilding...", id);

            Set<EnergyNode> lostNodes = new HashSet<>(nodes);
            lostNodes.removeAll(allReachableNodes);

            nodes.removeAll(lostNodes);
            for (EnergyNode lostNode : lostNodes) {
                lostNode.setNetwork(null);

                // [🔥 ИЗМЕНЕНО 🔥]
                // Передаем 'this' (текущую сеть) как ту, к которой нельзя присоединяться.
                manager.reAddNode(lostNode.getPos(), this);
            }

            // Если в текущей сети осталось меньше 2 узлов, распускаем ее
            if (nodes.size() < 2) {
                for (EnergyNode remainingNode : nodes) {
                    remainingNode.setNetwork(null);

                    // [🔥 ИЗМЕНЕНО 🔥]
                    manager.reAddNode(remainingNode.getPos(), this);
                }
                nodes.clear();
                manager.removeNetwork(this);
            }
        }
    }

    public void merge(EnergyNetwork other) {
        if (this == other) return;

        // Безопасное слияние (меньшая сеть вливается в большую)
        if (other.nodes.size() > this.nodes.size()) {
            other.merge(this);
            return;
        }

        for (EnergyNode node : other.nodes) {
            node.setNetwork(this);
            this.nodes.add(node);
        }

        other.nodes.clear();
        manager.removeNetwork(other);
        //LOGGER.debug("[NETWORK] Merged network {} into network {}", other.id, id);
    }

    public UUID getId() { return id; }
    @Override public boolean equals(Object o) { return this == o || (o instanceof EnergyNetwork that && id.equals(that.id)); }
    @Override public int hashCode() { return id.hashCode(); }
}