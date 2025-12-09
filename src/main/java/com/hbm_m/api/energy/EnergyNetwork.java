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
     * ИСПРАВЛЕННАЯ ЛОГИКА TICK (v6.3) - С гистерезисом для батарей
     * Решает проблему "мигания" интерфейса и бесконечных циклов зарядки/разрядки.
     */
    public void tick(ServerLevel level) {
        // 1. Валидация узлов
        int sizeBefore = nodes.size();
        nodes.removeIf(node -> !node.isValid(level) || node.getNetwork() != this);

        if (nodes.size() < sizeBefore) {
            verifyConnectivity();
        }

        if (nodes.size() < 2) {
            return;
        }

        // ====================================================================
        // ШАГ 1: Сбор и классификация участников
        // ====================================================================

        // Чистые генераторы (и батареи в режиме OUTPUT)
        List<IEnergyProvider> pureGenerators = new ArrayList<>();
        // Чистые потребители (машины и батареи в режиме INPUT)
        List<IEnergyReceiver> pureConsumers = new ArrayList<>();
        // Батареи в режиме BOTH (неопределившиеся)
        List<BatteryInfo> ambivalentBatteries = new ArrayList<>();

        List<BatteryInfo> allBatteries = new ArrayList<>(); // Для фазы балансировки

        long pureGenerationCap = 0;   // Сколько могут дать чистые генераторы
        long pureConsumerDemand = 0;  // Сколько хотят чистые потребители (машины)

        for (EnergyNode node : nodes) {
            if (!level.isLoaded(node.getPos())) continue;
            BlockEntity be = level.getBlockEntity(node.getPos());
            if (be == null) continue;

            Optional<IEnergyProvider> providerCap = be.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
            Optional<IEnergyReceiver> receiverCap = be.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();

            boolean isProvider = providerCap.isPresent();
            boolean isReceiver = receiverCap.isPresent();

            if (isProvider && isReceiver) {
                // Это БАТАРЕЯ
                int mode = (be instanceof MachineBatteryBlockEntity batteryBE) ? batteryBE.getCurrentMode() : 0;
                BatteryInfo info = new BatteryInfo(node.getPos(), receiverCap.get(), providerCap.get(), mode);
                allBatteries.add(info);

                if (mode == 2) {
                    // OUTPUT ONLY -> Считаем генератором
                    if (info.provider.canExtract()) {
                        pureGenerators.add(info.provider);
                        pureGenerationCap += Math.min(info.provider.getEnergyStored(), info.provider.getProvideSpeed());
                    }
                } else if (mode == 1) {
                    // INPUT ONLY -> Считаем потребителем
                    if (info.receiver.canReceive()) {
                        pureConsumers.add(info.receiver);
                        long needed = Math.min(info.receiver.getMaxEnergyStored() - info.receiver.getEnergyStored(), info.receiver.getReceiveSpeed());
                        pureConsumerDemand += needed;
                    }
                } else if (mode == 0) {
                    // BOTH -> Решим судьбу ниже
                    ambivalentBatteries.add(info);
                }
            } else if (isProvider) {
                // Обычный ГЕНЕРАТОР
                IEnergyProvider p = providerCap.get();
                if (p.canExtract()) {
                    pureGenerators.add(p);
                    pureGenerationCap += Math.min(p.getEnergyStored(), p.getProvideSpeed());
                }
            } else if (isReceiver) {
                // Обычная МАШИНА
                IEnergyReceiver r = receiverCap.get();
                if (r.canReceive()) {
                    pureConsumers.add(r);
                    long needed = Math.min(r.getMaxEnergyStored() - r.getEnergyStored(), r.getReceiveSpeed());
                    pureConsumerDemand += needed;
                }
            }
        }

        // ====================================================================
        // ШАГ 2: Определение режима сети (Surplus vs Deficit)
        // ====================================================================

        // Списки для финального распределения
        Set<IEnergyProvider> activeProviders = new HashSet<>(pureGenerators);

        // Карты для распределения (группировка по приоритетам)
        Map<IEnergyReceiver.Priority, Set<IEnergyReceiver>> activeConsumersByPriority = new EnumMap<>(IEnergyReceiver.Priority.class);
        Map<IEnergyReceiver, Long> activeConsumerDemand = new IdentityHashMap<>();

        long totalActiveDemand = 0;

        // Добавляем "чистых" потребителей (Машины) в список на раздачу
        for (IEnergyReceiver r : pureConsumers) {
            long needed = Math.min(r.getMaxEnergyStored() - r.getEnergyStored(), r.getReceiveSpeed());
            if (needed > 0) {
                activeConsumerDemand.put(r, needed);
                activeConsumersByPriority.computeIfAbsent(r.getPriority(), k -> new HashSet<>()).add(r);
                totalActiveDemand += needed;
            }
        }

        // ГЛАВНАЯ ЛОГИКА: Решаем, что делают BOTH батареи
        boolean isDeficit = pureGenerationCap < pureConsumerDemand;

        for (BatteryInfo bat : ambivalentBatteries) {
            if (isDeficit) {
                // ДЕФИЦИТ: Батареи (Both) помогают генераторам (РАЗРЯДКА)
                // Они НЕ добавляются в consumers, поэтому не будут потреблять
                if (bat.provider.canExtract()) {
                    activeProviders.add(bat.provider);
                }
            } else {
                // ПРОФИЦИТ: Батареи (Both) заряжаются от излишков (ЗАРЯДКА)
                // Они НЕ добавляются в providers, поэтому не будут сливать энергию
                if (bat.receiver.canReceive()) {
                    long needed = Math.min(bat.receiver.getMaxEnergyStored() - bat.receiver.getEnergyStored(), bat.receiver.getReceiveSpeed());
                    if (needed > 0) {
                        activeConsumerDemand.put(bat.receiver, needed);
                        activeConsumersByPriority.computeIfAbsent(bat.receiver.getPriority(), k -> new HashSet<>()).add(bat.receiver);
                        totalActiveDemand += needed;
                    }
                }
            }
        }

        // ====================================================================
        // ШАГ 3: Физическое распределение энергии
        // ====================================================================

        // 3.1 Считаем реальную доступную энергию от выбранных провайдеров
        long totalEnergyAvailable = 0;
        Map<IEnergyProvider, Long> providerCapacities = new IdentityHashMap<>();

        for (IEnergyProvider p : activeProviders) {
            long canProvide = Math.min(p.getEnergyStored(), p.getProvideSpeed());
            if (canProvide > 0) {
                providerCapacities.put(p, canProvide);
                totalEnergyAvailable += canProvide;
            }
        }

        long energyToDistribute = Math.min(totalEnergyAvailable, totalActiveDemand);

        if (energyToDistribute > 0) {
            // Распределяем по приоритетам (HIGH -> NORMAL -> LOW)
            for (int i = IEnergyReceiver.Priority.values().length - 1; i >= 0; i--) {
                IEnergyReceiver.Priority priority = IEnergyReceiver.Priority.values()[i];

                Set<IEnergyReceiver> group = activeConsumersByPriority.get(priority);
                if (group == null || group.isEmpty()) continue;
                if (energyToDistribute <= 0) break;

                long groupDemand = 0;
                for (IEnergyReceiver r : group) groupDemand += activeConsumerDemand.getOrDefault(r, 0L);

                long energyForGroup = Math.min(energyToDistribute, groupDemand);

                // Используем твой метод пропорционального распределения
                long used = distributeProportionally(energyForGroup, group, activeConsumerDemand, providerCapacities);

                energyToDistribute -= used;
            }
        }

        // ====================================================================
        // ШАГ 4: Балансировка батарей (между собой)
        // ====================================================================

        // Балансировка запускается всегда, чтобы выровнять заряд между батареями
        // (например, если одна батарея зарядилась быстрее другой в фазе 3)
        balanceBatteries(allBatteries);
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