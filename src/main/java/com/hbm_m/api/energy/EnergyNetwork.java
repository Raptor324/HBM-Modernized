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

    /**
     * НОВАЯ ЛОГИКА TICK (v5.1) - Исправленная логика
     */
    public void tick(ServerLevel level) {
        // 1. Валидация узлов
        nodes.removeIf(node -> !node.isValid(level) || node.getNetwork() != this);
        if (nodes.size() < 2) {
            // [ИЗМЕНЕНО] Убрал лог, чтобы не спамить в консоль
            return;
        }

        // 2. Сбор всех участников сети
        List<IEnergyProvider> generators = new ArrayList<>();
        List<IEnergyReceiver> consumers = new ArrayList<>();
        List<BatteryInfo> batteries = new ArrayList<>();

        for (EnergyNode node : nodes) {
            BlockEntity be = level.getBlockEntity(node.getPos());
            if (be == null) continue; // Узел есть, BE нет (например, выгружен) - пропускаем

            Optional<IEnergyProvider> providerCap = be.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
            Optional<IEnergyReceiver> receiverCap = be.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();

            boolean isProvider = providerCap.isPresent();
            boolean isReceiver = receiverCap.isPresent();

            if (isProvider && isReceiver) {
                // Это батарея
                int mode = (be instanceof MachineBatteryBlockEntity batteryBE) ? batteryBE.getCurrentMode() : 0;
                batteries.add(new BatteryInfo(
                        node.getPos(),
                        receiverCap.get(),
                        providerCap.get(),
                        mode
                ));
            } else if (isProvider) {
                // Чистый генератор
                generators.add(providerCap.get());
            } else if (isReceiver) {
                // Чистый потребитель (машина)
                consumers.add(receiverCap.get());
            }
        }

        // ===== ФАЗА 1: Сбор энергии (Только чистые генераторы) =====
        long totalGeneration = 0;
        Map<IEnergyProvider, Long> generatorCapacity = new IdentityHashMap<>();

        for (IEnergyProvider gen : generators) {
            if (gen.canExtract()) {
                long available = Math.min(gen.getEnergyStored(), gen.getProvideSpeed());
                if (available > 0) {
                    generatorCapacity.put(gen, available);
                    totalGeneration += available;
                }
            }
        }

        // [ИЗМЕНЕНО] Батареи в режиме отдачи больше НЕ добавляются в общий пул генерации
        // Они будут использоваться либо для покрытия дефицита, либо в балансировке.

        // ===== ФАЗА 2: Подсчет потребления (Только чистые потребители) =====
        long totalConsumption = 0;
        Map<IEnergyReceiver, Long> consumerDemand = new IdentityHashMap<>();

        // Сортируем потребителей по приоритету (высший приоритет первым)
        consumers.sort(Comparator.comparing(IEnergyReceiver::getPriority).reversed());

        for (IEnergyReceiver consumer : consumers) {
            if (consumer.canReceive()) {
                long needed = Math.min(
                        consumer.getMaxEnergyStored() - consumer.getEnergyStored(),
                        consumer.getReceiveSpeed()
                );
                if (needed > 0) {
                    consumerDemand.put(consumer, needed);
                    totalConsumption += needed;
                }
            }
        }

        // ===== ФАЗА 3: Распределение энергии потребителям (из генераторов) =====
        long energyForConsumers = Math.min(totalGeneration, totalConsumption);
        long remainingDemand = totalConsumption;
        long remainingGeneration = totalGeneration;

        if (energyForConsumers > 0) {
            // [ИЗМЕНЕНО] Передаем отсортированный список `consumers`
            long energyGiven = distributeEnergyToConsumers(energyForConsumers, consumers, consumerDemand, generatorCapacity);
            remainingGeneration -= energyGiven;
            remainingDemand -= energyGiven;
        }

        // ===== ФАЗА 4: Распределение излишков и дефицита батарей =====

        if (remainingDemand > 0) {
            // **[НОВАЯ ЛОГИКА] СЛУЧАЙ 2: ДЕФИЦИТ**
            // Потребителям все еще нужна энергия. Тянем ее из батарей (Output/Both)
            // пропорционально их *заполненности*.
            pullEnergyFromBatteriesProportionally(remainingDemand, batteries, consumers, consumerDemand);

        } else if (remainingGeneration > 0) {
            // **[ИЗМЕНЕНО] СЛУЧАЙ 1: ИЗЛИШЕК**
            // Генераторы произвели больше, чем нужно. Отдаем излишек батареям (Input/Both)
            // *поровну*, как ты и просил.
            distributeSurplusToBatteries(remainingGeneration, batteries, generatorCapacity);
        }

        // ===== ФАЗА 5: Балансировка между батареями режима "BOTH" =====
        // [ИЗМЕНЕНО] Метод полностью переписан, чтобы быть "транзакционным"
        balanceBatteries(batteries);
    }

    /**
     * [ИЗМЕНЕНО] Распределяет энергию потребителям, УВАЖАЯ приоритет
     * @return Фактически распределенное количество энергии
     */
    private long distributeEnergyToConsumers(long amount, List<IEnergyReceiver> sortedConsumers,
                                             Map<IEnergyReceiver, Long> consumerDemand,
                                             Map<IEnergyProvider, Long> providers) {
        if (amount <= 0 || sortedConsumers.isEmpty() || providers.isEmpty()) return 0;

        long energyLeftToGive = amount;
        long totalEnergyGiven = 0;

        // Идем по отсортированному списку
        for (IEnergyReceiver consumer : sortedConsumers) {
            if (energyLeftToGive <= 0) break;

            long demand = consumerDemand.getOrDefault(consumer, 0L);
            if (demand <= 0) continue;

            // Даем сколько можем (но не больше, чем нужно этому потребителю)
            long energyForThis = Math.min(energyLeftToGive, demand);
            if (energyForThis > 0) {
                long accepted = consumer.receiveEnergy(energyForThis, false);
                if (accepted > 0) {
                    // Извлекаем *только то, что было принято* из пула провайдеров
                    extractFromProviders(accepted, providers);
                    energyLeftToGive -= accepted;
                    totalEnergyGiven += accepted;
                    // Обновляем карту спроса для следующей фазы (дефицита)
                    consumerDemand.put(consumer, demand - accepted);
                }
            }
        }
        return totalEnergyGiven;
    }

    /**
     * [ИЗМЕНЕНО] Старый `distributeEnergyToBatteries` переименован
     * Распределяет ИЗЛИШЕК энергии батареям поровну (режимы Input/Both)
     */
    private void distributeSurplusToBatteries(long surplusAmount, List<BatteryInfo> batteries,
                                              Map<IEnergyProvider, Long> providers) {

        // Фильтруем батареи, которые могут принимать энергию
        List<BatteryInfo> receivingBatteries = new ArrayList<>();
        for (BatteryInfo battery : batteries) {
            if (battery.canInput() && battery.receiver.canReceive()) {
                // Дополнительно проверим, что у батареи есть место
                if (battery.receiver.getEnergyStored() < battery.receiver.getMaxEnergyStored()) {
                    receivingBatteries.add(battery);
                }
            }
        }

        if (receivingBatteries.isEmpty() || surplusAmount <= 0) return;

        // Распределяем поровну между всеми принимающими батареями
        // [ИЗМЕНЕНО] Улучшенная логика "поровну", чтобы не терять остатки
        long remainingSurplus = surplusAmount;
        int receiversCount = receivingBatteries.size();

        for (BatteryInfo battery : receivingBatteries) {
            if (remainingSurplus <= 0) break;

            long amountPerBattery = remainingSurplus / receiversCount;
            if (amountPerBattery <= 0) amountPerBattery = remainingSurplus; // Отдаем остаток последним

            receiversCount--; // Уменьшаем делитель

            long maxCanReceive = Math.min(
                    battery.receiver.getMaxEnergyStored() - battery.receiver.getEnergyStored(),
                    battery.receiver.getReceiveSpeed()
            );

            long toGive = Math.min(amountPerBattery, maxCanReceive);

            if (toGive > 0) {
                long accepted = battery.receiver.receiveEnergy(toGive, false);
                extractFromProviders(accepted, providers);
                remainingSurplus -= accepted;
            }
        }
    }

    /**
     * [НОВЫЙ МЕТОД] Покрывает дефицит, извлекая энергию из батарей (Output/Both)
     * пропорционально их *заполненности*.
     */
    private void pullEnergyFromBatteriesProportionally(long totalNeeded, List<BatteryInfo> batteries,
                                                       List<IEnergyReceiver> sortedConsumers,
                                                       Map<IEnergyReceiver, Long> consumerDemand) {

        // 1. Найти батареи-доноры и их общий "вес" (запас)
        Map<BatteryInfo, Long> donors = new IdentityHashMap<>(); // Батарея -> сколько МАКС может дать (лимит скорости)
        long totalWeight = 0; // Общий запас энергии (для пропорции)
        long totalAvailable = 0; // Общий доступный запас (с лимитом скорости)

        for (BatteryInfo battery : batteries) {
            if (battery.canOutput() && battery.provider.canExtract()) {
                long stored = battery.provider.getEnergyStored();
                long canProvide = Math.min(stored, battery.provider.getProvideSpeed());

                if (canProvide > 0) {
                    donors.put(battery, canProvide);
                    totalWeight += stored; // Вес = полный запас
                    totalAvailable += canProvide; // Доступно = запас с лимитом скорости
                }
            }
        }

        if (donors.isEmpty() || totalWeight <= 0 || totalAvailable <= 0 || totalNeeded <= 0) return;

        // 2. Определяем, сколько всего будем тянуть
        long energyToPull = Math.min(totalNeeded, totalAvailable);
        long totalPulled = 0; // Сколько реально извлекли

        // 3. Извлекаем из доноров пропорционально их весу (запасу)
        Map<IEnergyProvider, Long> batteryProviderPool = new IdentityHashMap<>();

        for (Map.Entry<BatteryInfo, Long> entry : donors.entrySet()) {
            BatteryInfo battery = entry.getKey();
            long maxCanProvide = entry.getValue(); // Лимит (скорость/запас)
            long stored = battery.provider.getEnergyStored();

            // "пропорционально... заполненности"
            double share = (double) stored / totalWeight;
            long toExtract = (long) (energyToPull * share);
            toExtract = Math.min(toExtract, maxCanProvide); // Не больше лимита

            if (toExtract > 0) {
                // НЕ извлекаем сразу, а сначала собираем в "пул"
                batteryProviderPool.put(battery.provider, toExtract);
                totalPulled += toExtract;
            }
        }

        if (totalPulled <= 0) return;

        // 4. Распределяем собранную энергию (totalPulled) потребителям по приоритету
        // Мы можем повторно использовать distributeEnergyToConsumers, т.к. он делает то, что нужно
        distributeEnergyToConsumers(totalPulled, sortedConsumers, consumerDemand, batteryProviderPool);
    }


    /**
     * [ИЗМЕНЕНО] Балансирует энергию между батареями в режиме "BOTH" (0)
     * Цель: выравнять уровень заполненности.
     * Эта версия является "транзакционной" - она не создает/теряет энергию.
     */
    private void balanceBatteries(List<BatteryInfo> batteries) {
        List<BatteryInfo> balancingBatteries = batteries.stream()
                .filter(b -> b.mode == 0) // только режим "BOTH"
                .toList();

        if (balancingBatteries.size() < 2) return;

        // Вычисляем среднее заполнение
        long totalEnergy = 0;
        long totalCapacity = 0;

        for (BatteryInfo battery : balancingBatteries) {
            totalEnergy += battery.provider.getEnergyStored();
            totalCapacity += battery.provider.getMaxEnergyStored();
        }

        if (totalCapacity <= 0) return;
        double averagePercentage = (double) totalEnergy / totalCapacity;

        // 1. Находим раздающих и принимающих, и сколько они могут отдать/принять
        Map<BatteryInfo, Long> givers = new IdentityHashMap<>(); // Батарея -> Сколько может отдать
        Map<BatteryInfo, Long> takers = new IdentityHashMap<>(); // Батарея -> Сколько может принять

        long totalToGive = 0;
        long totalToTake = 0;
        long buffer = 100; // Буфер, чтобы не гонять энергию туда-сюда

        for (BatteryInfo battery : balancingBatteries) {
            long targetEnergy = (long) (battery.provider.getMaxEnergyStored() * averagePercentage);
            long currentEnergy = battery.provider.getEnergyStored();
            long diff = currentEnergy - targetEnergy;

            if (diff > buffer && battery.provider.canExtract()) { // Giver
                long canGive = Math.min(diff - buffer, battery.provider.getProvideSpeed());
                canGive = Math.min(canGive, battery.provider.getEnergyStored()); // На всякий случай
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

        // 2. Находим, сколько энергии реально будет передано (минимальное из спроса/предложения)
        long totalToTransfer = Math.min(totalToGive, totalToTake);
        if (totalToTransfer <= 0) return;

        // 3. Распределяем `totalToTransfer` принимающим (пропорционально их "нужде")
        long transferredSoFar = 0;
        Map<IEnergyProvider, Long> providerPool = new IdentityHashMap<>(); // Виртуальный пул

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
        // (пропорционально их "желанию" отдать)
        long extractedSoFar = 0;
        long totalToExtract = transferredSoFar; // Мы извлекаем ровно столько, сколько было принято

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
     */
    private void extractFromProviders(long amount, Map<IEnergyProvider, Long> providers) {
        if (amount <= 0 || providers.isEmpty()) return;

        long totalCapacity = providers.values().stream().mapToLong(Long::longValue).sum();
        if (totalCapacity <= 0) {
            // [ИЗМЕНЕНО] Добавлена защита от бага, если емкость 0, но энергия нужна
            // Пытаемся взять поровну у всех
            long amountPer = amount / providers.size();
            long remaining = amount;
            for(IEnergyProvider provider : providers.keySet()) {
                if (remaining <= 0) break;
                long toExtract = Math.min(amountPer, remaining);
                if (toExtract <= 0) toExtract = remaining; // остатки

                long extracted = provider.extractEnergy(toExtract, false);
                remaining -= extracted;
                providers.put(provider, providers.get(provider) - extracted);
            }
            return;
        }

        long remaining = amount;

        // [ИЗМЕНЕНО] Логика чуть-чуть доработана для большей точности
        List<Map.Entry<IEnergyProvider, Long>> providerList = new ArrayList<>(providers.entrySet());

        for (Map.Entry<IEnergyProvider, Long> entry : providerList) {
            if (remaining <= 0) break;
            if (totalCapacity <= 0) break; // Все провайдеры исчерпаны

            IEnergyProvider provider = entry.getKey();
            long capacity = entry.getValue();
            if (capacity <= 0) continue;

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

        // [ИЗМЕНЕНО] Если из-за ошибок округления что-то осталось, добираем
        if (remaining > 0) {
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
    // (Без изменений, она выглядит корректной)

    public void addNode(EnergyNode node) {
        if (nodes.add(node)) {
            node.setNetwork(this);
        }
    }

    public void removeNode(EnergyNode node) {
        if (!nodes.remove(node)) return;

        node.setNetwork(null);

        if (nodes.size() < 2) {
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
            LOGGER.warn("[NETWORK] Network {} split detected! Rebuilding...", id);

            Set<EnergyNode> lostNodes = new HashSet<>(nodes);
            lostNodes.removeAll(allReachableNodes);

            nodes.removeAll(lostNodes);
            for (EnergyNode lostNode : lostNodes) {
                lostNode.setNetwork(null);
                // [ИЗМЕНЕНО] Тут была ошибка в твоей логике. Ты удалял из менеджера и добавлял.
                // Нужно просто сказать менеджеру перестроить узел, он сам найдет новую сеть.
                manager.reAddNode(lostNode.getPos());
            }

            // Если в текущей сети осталось меньше 2 узлов, распускаем ее
            if (nodes.size() < 2) {
                for (EnergyNode remainingNode : nodes) {
                    remainingNode.setNetwork(null);
                    manager.reAddNode(remainingNode.getPos());
                }
                nodes.clear();
                manager.removeNetwork(this);
            }
        }
    }

    public void merge(EnergyNetwork other) {
        if (this == other) return;

        // [ИЗМЕНЕНО] Более безопасное слияние
        if (other.nodes.size() > this.nodes.size()) {
            // Если другая сеть больше, сливаем *в нее*
            other.merge(this);
            return;
        }

        for (EnergyNode node : other.nodes) {
            node.setNetwork(this);
            this.nodes.add(node);
        }

        other.nodes.clear();
        manager.removeNetwork(other);
    }

    public UUID getId() { return id; }
    @Override public boolean equals(Object o) { return this == o || (o instanceof EnergyNetwork that && id.equals(that.id)); }
    @Override public int hashCode() {
        return id.hashCode();
    }
}