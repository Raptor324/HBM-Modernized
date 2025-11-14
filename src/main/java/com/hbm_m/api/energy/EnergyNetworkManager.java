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
            LOGGER.info("[NETWORK] Loading {} nodes for dimension {}", nodePositions.length, level.dimension().location());
            for (long posLong : nodePositions) {
                BlockPos pos = BlockPos.of(posLong);
                if (level.isLoaded(pos)) {
                    allNodes.put(pos.asLong(), new EnergyNode(pos));
                }
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

        LOGGER.info("[NETWORK] Rebuilding from {} loaded nodes.", allNodes.size());

        // 2. Используем Set для отслеживания *уже* обработанных узлов
        Set<EnergyNode> processedNodes = new HashSet<>();

        // 3. Проходим по КАЖДОМУ узлу, который мы загрузили из NBT
        for (EnergyNode startNode : allNodes.values()) {

            // Если узел уже обработан (т.е. мы нашли его как соседа), пропускаем
            if (processedNodes.contains(startNode)) {
                continue;
            }

            // [🔥 ИЗМЕНЕНИЕ 🔥]
            // Мы начинаем новую сеть ТОЛЬКО с ВАЛИДНОГО узла.
            // (Н-р, 'WoodBurner' (контроллер), 'Wire' или 'Battery')
            if (startNode.isValid(level)) {

                EnergyNetwork newNetwork = new EnergyNetwork(this);
                networks.add(newNetwork);

                Queue<EnergyNode> queue = new LinkedList<>();
                queue.add(startNode);
                processedNodes.add(startNode); // Помечаем как обработанный

                while (!queue.isEmpty()) {
                    EnergyNode currentNode = queue.poll();
                    newNetwork.addNode(currentNode); // Добавляем в новую сеть

                    // Ищем соседей
                    for (Direction dir : Direction.values()) {
                        EnergyNode neighbor = allNodes.get(currentNode.getPos().relative(dir).asLong());

                        // [🔥 ГЛАВНЫЙ ФИКС 🔥]
                        // Если сосед существует в нашем списке (allNodes)
                        // и мы его еще не обработали, мы ДОЛЖНЫ его присоединить.
                        // Мы НЕ проверяем neighbor.isValid()!
                        // Невалидные части (как от мультиблоков)
                        // *должны* быть в сети, чтобы соединять валидные части.
                        if (neighbor != null && !processedNodes.contains(neighbor)) {
                            processedNodes.add(neighbor); // Помечаем
                            queue.add(neighbor); // Добавляем в очередь на поиск
                        }
                    }
                }
            } else {
                // Если startNode невалиден (н-р, это 'ghost' узел),
                // мы его тоже помечаем, чтобы не проверять его снова.
                processedNodes.add(startNode);
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
        if (allNodes.containsKey(pos.asLong())) {
            return; // Узел уже существует и находится в сети
        }

        EnergyNode newNode = new EnergyNode(pos);
        allNodes.put(pos.asLong(), newNode);

        Set<EnergyNetwork> adjacentNetworks = new HashSet<>();
        for (Direction dir : Direction.values()) {
            EnergyNode neighbor = allNodes.get(pos.relative(dir).asLong());
            if (neighbor != null && neighbor.getNetwork() != null) {
                adjacentNetworks.add(neighbor.getNetwork());
            }
        }

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

    void reAddNode(BlockPos pos) {
        // Мы не удаляем его из allNodes, он там все еще есть,
        // но он потерял свою сеть.
        EnergyNode node = allNodes.get(pos.asLong());
        if (node != null) {
            node.setNetwork(null);
        }

        // Удаляем и добавляем, чтобы сработала логика поиска соседей
        allNodes.remove(pos.asLong());
        addNode(pos);
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