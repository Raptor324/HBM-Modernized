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

/**
 * Менеджер энергетических сетей.
 * Управляет созданием, объединением и разделением сетей.
 * Сохраняется как SavedData для каждого измерения.
 */
public class EnergyNetworkManager extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "hbm_modernized_energy_networks";
    private static final int TICK_INTERVAL = 20; // Тикать раз в секунду (20 тиков)
    private int tickCounter = 0; // ДОБАВЬ ЭТО

    private final ServerLevel level;
    private final Long2ObjectMap<EnergyNode> allNodes = new Long2ObjectOpenHashMap<>();
    private final Set<EnergyNetwork> networks = Sets.newHashSet();

    public EnergyNetworkManager(ServerLevel level, CompoundTag nbt) {
        this(level);
        load(nbt);
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
     * Тикает все сети. Вызывается из события ServerTickEvent.
     */
    public void tick() {
        tickCounter++;

        // Тикаем только каждые 20 тиков (1 раз в секунду)
        if (tickCounter < TICK_INTERVAL) {
            return;
        }

        tickCounter = 0; // Сбрасываем счетчик

        if (!networks.isEmpty()) {
            LOGGER.info("[NETWORK] Ticking {} networks with {} total nodes",
                    networks.size(), allNodes.size());
            new HashSet<>(networks).forEach(network -> network.tick(level));
        }
    }

    /**
     * Добавляет узел (провод или машину) в сеть
     */
    public void addNode(BlockPos pos) {
        if (allNodes.containsKey(pos.asLong())) {
            LOGGER.debug("[NETWORK] Node already exists at {}, removing old", pos);
            removeNode(pos);
        }

        LOGGER.info("[NETWORK] Adding node at {}", pos);
        EnergyNode newNode = new EnergyNode(pos);
        allNodes.put(pos.asLong(), newNode);

        // Ищем соседние сети
        Set<EnergyNetwork> adjacentNetworks = new HashSet<>();
        for (Direction dir : Direction.values()) {
            EnergyNode neighbor = allNodes.get(pos.relative(dir).asLong());
            if (neighbor != null && neighbor.getNetwork() != null) {
                adjacentNetworks.add(neighbor.getNetwork());
                LOGGER.debug("[NETWORK]   Found adjacent network in direction {}", dir);
            }
        }

        if (adjacentNetworks.isEmpty()) {
            // Создаем новую сеть
            LOGGER.info("[NETWORK]   Creating NEW network for node at {}", pos);
            EnergyNetwork newNetwork = new EnergyNetwork(this);
            networks.add(newNetwork);
            newNetwork.addNode(newNode);
        } else {
            // Объединяем с существующими
            LOGGER.info("[NETWORK]   Merging with {} existing network(s)", adjacentNetworks.size());
            EnergyNetwork main = adjacentNetworks.iterator().next();
            adjacentNetworks.remove(main);
            main.addNode(newNode);

            // Объединяем все соседние сети в одну
            adjacentNetworks.forEach(main::merge);
        }

        setDirty();
    }

    /**
     * Удаляет узел из сети
     */
    public void removeNode(BlockPos pos) {
        LOGGER.info("[NETWORK] Removing node at {}", pos);
        EnergyNode node = allNodes.remove(pos.asLong());
        if (node != null && node.getNetwork() != null) {
            node.getNetwork().removeNode(node);
        }
        setDirty();
    }

    /**
     * Проверяет, есть ли узел в сети
     */
    public boolean hasNode(BlockPos pos) {
        return allNodes.containsKey(pos.asLong());
    }

    /**
     * Получить узел по позиции (для использования в EnergyNetwork)
     */
    public EnergyNode getNode(BlockPos pos) {
        return allNodes.get(pos.asLong());
    }

    void addNetwork(EnergyNetwork network) {
        networks.add(network);
        LOGGER.debug("[NETWORK] Network {} registered in manager", network.getId());
    }

    void removeNetwork(EnergyNetwork network) {
        networks.remove(network);
        LOGGER.debug("[NETWORK] Network {} removed from manager", network.getId());
    }

    // --- NBT СОХРАНЕНИЕ ---
    @Override
    public CompoundTag save(CompoundTag nbt) {
        long[] nodePositions = allNodes.keySet().toLongArray();
        nbt.putLongArray("nodes", nodePositions);
        nbt.putInt("network_count", networks.size());

        LOGGER.info("[NETWORK] Saved {} nodes and {} networks", nodePositions.length, networks.size());
        return nbt;
    }

    private void load(CompoundTag nbt) {
        if (!nbt.contains("nodes")) return;

        long[] nodePositions = nbt.getLongArray("nodes");
        LOGGER.info("[NETWORK] Loading {} nodes from NBT", nodePositions.length);

        for (long posLong : nodePositions) {
            BlockPos pos = BlockPos.of(posLong);
            if (level.isLoaded(pos)) {
                addNode(pos);
            }
        }
    }
}