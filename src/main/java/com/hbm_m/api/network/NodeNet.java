package com.hbm_m.api.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Абстрактная сеть (аналог NodeNet из 1.7.10).
 * R = тип получателей, P = тип поставщиков, L = тип узлов.
 */
public abstract class NodeNet<R, P, L extends GenNode<?>> {

    public static final Random RAND = new Random();

    public boolean valid = true;
    public Set<L> links = new LinkedHashSet<>();

    /** Активные получатели: ключ → timestamp последней подписки в мс. */
    public HashMap<R, Long> receiverEntries = new HashMap<>();

    /** Активные поставщики: ключ → timestamp последней подписки в мс. */
    public HashMap<P, Long> providerEntries = new HashMap<>();

    public NodeNet() {
        UniNodespace.activeNodeNets.add(this);
    }

    // --- Subscriber management ---

    public boolean isSubscribed(R receiver) { return receiverEntries.containsKey(receiver); }
    public void addReceiver(R receiver) { receiverEntries.put(receiver, System.currentTimeMillis()); }
    public void removeReceiver(R receiver) { receiverEntries.remove(receiver); }

    public boolean isProvider(P provider) { return providerEntries.containsKey(provider); }
    public void addProvider(P provider) { providerEntries.put(provider, System.currentTimeMillis()); }
    public void removeProvider(P provider) { providerEntries.remove(provider); }

    // --- Network merging ---

    /** Слить другую сеть в эту (меньшая поглощается большей). */
    public void joinNetworks(NodeNet<R, P, L> other) {
        if (other == this) return;

        List<L> oldNodes = new ArrayList<>(other.links.size());
        oldNodes.addAll(other.links);

        for (L conductor : oldNodes) forceJoinLink(conductor);
        other.links.clear();

        other.receiverEntries.keySet().forEach(this::addReceiver);
        other.providerEntries.keySet().forEach(this::addProvider);
        other.destroy();
    }

    /** Добавить узел в эту сеть, убрав из предыдущей. */
    public NodeNet<R, P, L> joinLink(L node) {
        if (node.net != null) ((NodeNet<R, P, L>) node.net).leaveLink(node);
        return forceJoinLink(node);
    }

    /** Добавить узел без удаления из старой сети. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public NodeNet<R, P, L> forceJoinLink(L node) {
        links.add(node);
        ((GenNode) node).setNet(this);
        return this;
    }

    /** Убрать узел из сети. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void leaveLink(L node) {
        ((GenNode) node).setNet(null);
        links.remove(node);
    }

    // --- Lifecycle ---

    public void invalidate() {
        this.valid = false;
        UniNodespace.activeNodeNets.remove(this);
    }

    public boolean isValid() { return valid; }

    public void resetTrackers() { }

    public abstract void update();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void destroy() {
        invalidate();
        for (GenNode<?> link : links) {
            if (link.net == this) ((GenNode) link).setNet(null);
        }
        links.clear();
        receiverEntries.clear();
        providerEntries.clear();
    }

    /**
     * Проверка плохой ссылки: подписчик/поставщик убран (BE invalidated или выгружен).
     */
    public static boolean isBadLink(Object o) {
        if (o instanceof ILoadedEntry loaded && !loaded.isLoaded()) return true;
        if (o instanceof BlockEntity be && be.isRemoved()) return true;
        return false;
    }

    /**
     * Маркер загруженности для подписчиков/провайдеров, не являющихся BlockEntity.
     * Аналог ILoadedTile из 1.7.10.
     */
    public interface ILoadedEntry {
        boolean isLoaded();
    }
}
