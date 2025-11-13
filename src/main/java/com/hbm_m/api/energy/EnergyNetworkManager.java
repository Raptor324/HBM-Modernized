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
        // ✅ ИЗМЕНЕНО: Загрузка теперь вызывает полную перестройку
        if (nbt.contains("nodes")) {
            long[] nodePositions = nbt.getLongArray("nodes");
            LOGGER.info("[NETWORK] Rebuilding networks from {} saved nodes for dimension {}", nodePositions.length, level.dimension().location());
            // Мы не вызываем addNode здесь, а просто запоминаем позиции.
            // Полная перестройка произойдет при первом тике мира.
            for (long posLong : nodePositions) {
                BlockPos pos = BlockPos.of(posLong);
                if (level.isLoaded(pos)) {
                    // Просто добавляем узел без создания сети
                    allNodes.put(pos.asLong(), new EnergyNode(pos));
                }
            }
            // Сама перестройка будет вызвана извне после загрузки мира
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
        // Сохраняем копию узлов, так как addNode будет изменять allNodes
        Long2ObjectMap<EnergyNode> nodesToProcess = new Long2ObjectOpenHashMap<>(allNodes);

        // Очищаем всё
        networks.clear();
        allNodes.clear();

        for (EnergyNode node : nodesToProcess.values()) {
            // Заново добавляем каждый узел, что заставит его найти соседей и создать/присоединиться к сети
            if (node.isValid(level)) {
                addNode(node.getPos());
            }
        }
        LOGGER.info("[NETWORK] Full network rebuild completed. Found {} networks.", networks.size());
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
        EnergyNode node = allNodes.remove(pos.asLong());
        if (node != null && node.getNetwork() != null) {
            node.getNetwork().removeNode(node);
        }
        setDirty();
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