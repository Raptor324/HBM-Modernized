package com.hbm_m.api.energy;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

public class EnergyNetworkManager extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "hbm_modernized_energy_networks";

    private final ServerLevel level;
    private final Long2ObjectMap<EnergyNode> allNodes = new Long2ObjectOpenHashMap<>();
    private final Set<EnergyNetwork> networks = Sets.newHashSet();

    public EnergyNetworkManager(ServerLevel level, CompoundTag nbt) {
        this(level);
        if (nbt.contains("nodes")) {
            long[] nodePositions = nbt.getLongArray("nodes");
            // [🔥 ФИКС] Эта строка из твоего лога, она правильная
            LOGGER.info("[NETWORK] Loading {} nodes for dimension {}", nodePositions.length, level.dimension().location());

            for (long posLong : nodePositions) {
                BlockPos pos = BlockPos.of(posLong);

                // [🔥 ФИКС] Просто добавляем. БЕЗ isValid().
                // Мы "разгребём" мусор в rebuildAllNetworks(), когда мир будет готов.
                allNodes.put(pos.asLong(), new EnergyNode(pos));
            }
        }
    }

    public EnergyNetworkManager(ServerLevel level) {
        this.level = level;
    }

    public static EnergyNetworkManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                (nbt) -> new EnergyNetworkManager(level, nbt),
                () -> new EnergyNetworkManager(level),
                DATA_NAME
        );
    }

    /**
     * ✅ НОВЫЙ МЕТОД: Полностью перестраивает все сети.
     * Вызывается при загрузке мира, чтобы исправить любые сломанные состояния.
     */


    public void rebuildAllNetworks() {
        LOGGER.info("[NETWORK] Starting full network rebuild for dimension {}...", level.dimension().location());

        // 1. Очищаем старые сети и сбрасываем узлы
        networks.clear();
        for (EnergyNode node : allNodes.values()) {
            node.setNetwork(null);
        }

        // [🔥 НОВЫЙ ШАГ ОЧИСТКИ]
        // Теперь, когда мир ТОЧНО загружен, чистим 'allNodes'
        // от всего мусора (невалидных узлов), который загрузил конструктор.
        int totalNodes = allNodes.size();
        allNodes.values().removeIf(node -> !node.isValid(level));
        int validNodes = allNodes.size();
        LOGGER.info("[NETWORK] Pruned node list: {} total -> {} valid.", totalNodes, validNodes);


        // 2. Используем Set для отслеживания *уже* обработанных узлов
        Set<EnergyNode> processedNodes = new HashSet<>();

        // 3. Проходим по *очищенному* allNodes
        for (EnergyNode startNode : allNodes.values()) {

            if (processedNodes.contains(startNode)) {
                continue;
            }

            // [🔥 ФИКС] Нам больше не нужен startNode.isValid()
            // потому что мы уже очистили 'allNodes'.

            EnergyNetwork newNetwork = new EnergyNetwork(this);
            networks.add(newNetwork);

            Queue<EnergyNode> queue = new LinkedList<>();
            queue.add(startNode);
            processedNodes.add(startNode);

            while (!queue.isEmpty()) {
                EnergyNode currentNode = queue.poll();
                newNetwork.addNode(currentNode); // Добавляем в новую сеть

                // Ищем соседей
                for (Direction dir : Direction.values()) {
                    EnergyNode neighbor = allNodes.get(currentNode.getPos().relative(dir).asLong());

                    // [🔥 ФИКС] Нам больше не нужен neighbor.isValid()
                    // потому что 'allNodes' уже чист.
                    if (neighbor != null && !processedNodes.contains(neighbor)) {
                        processedNodes.add(neighbor); // Помечаем
                        queue.add(neighbor); // Добавляем в очередь на поиск
                    }
                }
            }
        }

        LOGGER.info("[NETWORK] Rebuild completed. Found {} networks.", networks.size());
        setDirty();
    }


    public void tick() {
        // Копируем, чтобы избежать ConcurrentModificationException
        new HashSet<>(networks).forEach(network -> network.tick(level));
    }

    public void addNode(BlockPos pos) {
        addNode(pos, null);
    }

    private void addNode(BlockPos pos, @Nullable EnergyNetwork networkToAvoid) {
        long posLong = pos.asLong();

        // 1. Защита от дубликатов и проверка существования
        if (allNodes.containsKey(posLong)) {
            EnergyNode existingNode = allNodes.get(posLong);
            if (existingNode != null && existingNode.getNetwork() != null) {
                return; // Узел уже есть и он в порядке
            }
        }

        // 2. Создаем и проверяем валидность
        EnergyNode newNode = new EnergyNode(pos);
        // Если чанк загружен, но блока нет (или он не подходит), удаляем и выходим
        if (!newNode.isValid(level)) {
            allNodes.remove(posLong);
            return;
        }

        // 3. Сохраняем узел
        allNodes.put(posLong, newNode);

        // 4. Ищем соседей (с функцией АВТО-ПОЧИНКИ)
        Set<EnergyNetwork> adjacentNetworks = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            long neighborLong = neighborPos.asLong();

            EnergyNode neighbor = allNodes.get(neighborLong);

            // [АВТО-ПОЧИНКА]
            // Если в памяти менеджера соседа НЕТ, но чанк загружен...
            if (neighbor == null && level.isLoaded(neighborPos)) {
                // Проверяем, есть ли там реальный TileEntity с энергией
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(neighborPos);
                if (be != null) {
                    boolean isEnergyBlock = be.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_PROVIDER).isPresent() ||
                            be.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER).isPresent() ||
                            be.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_CONNECTOR).isPresent();

                    if (isEnergyBlock) {
                        // Мы нашли "потерянный" провод! Добавляем его принудительно.
                        // Это рекурсивно вызовет addNode для провода и починит сеть дальше.
                        addNode(neighborPos);
                        neighbor = allNodes.get(neighborLong); // Теперь он точно есть
                    }
                }
            }

            // Стандартная логика соединения
            if (neighbor != null && neighbor.getNetwork() != null) {
                if (neighbor.getNetwork() != networkToAvoid) {
                    adjacentNetworks.add(neighbor.getNetwork());
                }
            }
        }

        // 5. Слияние или создание сети
        if (adjacentNetworks.isEmpty()) {
            EnergyNetwork newNetwork = new EnergyNetwork(this);
            networks.add(newNetwork);
            newNetwork.addNode(newNode);
        } else {
            Iterator<EnergyNetwork> it = adjacentNetworks.iterator();
            EnergyNetwork main = it.next();
            main.addNode(newNode);
            while (it.hasNext()) {
                main.merge(it.next());
            }
        }
        setDirty();
    }

    public void removeNode(BlockPos pos) {
        long posLong = pos.asLong();
        EnergyNode node = allNodes.remove(posLong); // <--- Удаляем из глобальной карты

        if (node == null) {
            // LOGGER.debug("[NETWORK] Node {} was not in the manager", pos);
            return;
        }

        EnergyNetwork network = node.getNetwork();
        if (network != null) {
            network.removeNode(node); // <--- Говорим сети, что узел удален
            LOGGER.debug("[NETWORK] Removed node {} from network {}", pos, network.getId());
        }

        setDirty();
    }

    void reAddNode(BlockPos pos, @Nullable EnergyNetwork networkToAvoid) {
        // Мы не удаляем его из allNodes, он там все еще есть,
        // но он потерял свою сеть.
        EnergyNode node = allNodes.get(pos.asLong());
        if (node != null) {
            node.setNetwork(null);
        }

        // Удаляем и добавляем, чтобы сработала логика поиска соседей
        allNodes.remove(pos.asLong());

        // [🔥 ИЗМЕНЕНО 🔥]
        addNode(pos, networkToAvoid); // Передаем "запрещенную" сеть
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        // Сохраняем только позиции узлов
        long[] nodePositions = allNodes.keySet().toLongArray();
        nbt.putLongArray("nodes", nodePositions);
        return nbt;
    }

    // Остальные методы (hasNode, getNode, addNetwork, removeNetwork) без изменений
    public boolean hasNode(BlockPos pos) { return allNodes.containsKey(pos.asLong()); }
    public EnergyNode getNode(BlockPos pos) { return allNodes.get(pos.asLong()); }
    void addNetwork(EnergyNetwork network) { networks.add(network); }
    void removeNetwork(EnergyNetwork network) { networks.remove(network); }
}