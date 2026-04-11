package com.hbm_m.api.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * Универсальный узел сети (аналог GenNode из 1.7.10).
 * Хранит одну или несколько позиций блока и список точек соединения (DirPos).
 * Узел "живёт" в UniNodespace независимо от загрузки чанка — реализует ту же логику
 * "soul of a tile entity" что и оригинальный UNINOS.
 */
public class GenNode<N extends NodeNet<?, ?, ?>> {

    /** Текущая сеть, к которой принадлежит узел. null — не принадлежит ни одной. */
    public N net;

    /** Позиции, по которым этот узел зарегистрирован в UniNodeWorld (обычно одна). */
    public final List<BlockPos> positions;

    /** Точки соединения: позиция соседнего узла + направление ОТ ТЕКУЩЕГО к нему. */
    public NodeDirPos[] connections;

    /** Провайдер, создавший этот тип сети. */
    public final INetworkProvider<N> networkProvider;

    /** Флаг: узел изменился и нужно перепроверить соединения в следующий updateNodespace. */
    public boolean recentlyChanged = true;

    /** Флаг: узел уничтожен. */
    public boolean expired = false;

    @SuppressWarnings("unchecked")
    public GenNode(INetworkProvider<N> provider, BlockPos... positions) {
        this.networkProvider = provider;
        this.positions = new ArrayList<>(Arrays.asList(positions));
        this.connections = new NodeDirPos[0];
    }

    /** Задать точки соединения (используется при создании). */
    public GenNode<N> setConnections(NodeDirPos... conns) {
        this.connections = conns;
        return this;
    }

    /** Добавить дополнительную точку соединения (pipeline/anchor). */
    public void addConnection(NodeDirPos conn) {
        NodeDirPos[] expanded = new NodeDirPos[connections.length + 1];
        System.arraycopy(connections, 0, expanded, 0, connections.length);
        expanded[connections.length] = conn;
        this.connections = expanded;
    }

    /** true если узел принадлежит живой сети. */
    public boolean hasValidNet() {
        return net != null && net.isValid();
    }

    public void setNet(N net) {
        this.net = net;
    }

    public boolean isExpired() {
        return expired;
    }
}
