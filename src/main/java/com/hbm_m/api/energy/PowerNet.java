package com.hbm_m.api.energy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.hbm_m.api.network.NodeNet;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт api/hbm/energymk2/PowerNetMK2 из 1.7.10 — 1:1.
 * Технически MK3, поскольку работает поверх UNINOS-порта.
 */
public class PowerNet extends NodeNet<IEnergyReceiver, IEnergyProvider, Nodespace.PowerNode> {

    public long energyTracker = 0L;

    protected static int timeout = 3_000;

    @Override
    public void resetTrackers() {
        this.energyTracker = 0;
    }

    @Override
    public void update() {

        if (providerEntries.isEmpty()) return;
        if (receiverEntries.isEmpty()) return;

        long timestamp = System.currentTimeMillis();

        List<Pair<IEnergyProvider, Long>> providers = new ArrayList<>();
        long powerAvailable = 0;

        // sum up available power
        Iterator<Entry<IEnergyProvider, Long>> provIt = providerEntries.entrySet().iterator();
        while (provIt.hasNext()) {
            Entry<IEnergyProvider, Long> entry = provIt.next();
            if (timestamp - entry.getValue() > timeout || isBadLink(entry.getKey())) { provIt.remove(); continue; }
            long src = Math.min(entry.getKey().getEnergyStored(), entry.getKey().getProvideSpeed());
            if (src > 0) {
                providers.add(new Pair<>(entry.getKey(), src));
                powerAvailable += src;
            }
        }

        // sum up total demand, categorized by priority
        List<Pair<IEnergyReceiver, Long>>[] receivers = new ArrayList[IEnergyReceiver.Priority.values().length];
        for (int i = 0; i < receivers.length; i++) receivers[i] = new ArrayList<>();
        long[] demand = new long[IEnergyReceiver.Priority.values().length];
        long totalDemand = 0;

        Iterator<Entry<IEnergyReceiver, Long>> recIt = receiverEntries.entrySet().iterator();

        while (recIt.hasNext()) {
            Entry<IEnergyReceiver, Long> entry = recIt.next();
            if (timestamp - entry.getValue() > timeout || isBadLink(entry.getKey())) { recIt.remove(); continue; }
            long rec = Math.min(entry.getKey().getMaxEnergyStored() - entry.getKey().getEnergyStored(), entry.getKey().getReceiveSpeed());
            if (rec > 0) {
                int p = entry.getKey().getPriority().ordinal();
                receivers[p].add(new Pair<>(entry.getKey(), rec));
                demand[p] += rec;
                totalDemand += rec;
            }
        }

        long toTransfer = Math.min(powerAvailable, totalDemand);
        long energyUsed = 0;

        // add power to receivers, ordered by priority
        for (int i = IEnergyReceiver.Priority.values().length - 1; i >= 0; i--) {
            List<Pair<IEnergyReceiver, Long>> list = receivers[i];
            long priorityDemand = demand[i];

            for (Pair<IEnergyReceiver, Long> entry : list) {
                double weight = (double) entry.getSecond() / (double) (priorityDemand);
                long toSend = (long) Math.min(Math.max(toTransfer * weight, 0D), entry.getSecond());
                energyUsed += (toSend - transferPower(entry.getFirst(), toSend)); // leftovers are subtracted from the intended amount to use up
            }

            toTransfer -= energyUsed;
        }

        this.energyTracker += energyUsed;
        long leftover = energyUsed;

        // remove power from providers
        for (Pair<IEnergyProvider, Long> entry : providers) {
            double weight = (double) entry.getSecond() / (double) powerAvailable;
            long toUse = (long) Math.max(energyUsed * weight, 0D);
            usePower(entry.getFirst(), toUse);
            leftover -= toUse;
        }

        // rounding error compensation, detects surplus that hasn't been used and removes it from random providers
        int iterationsLeft = 100; // whiles without emergency brakes are a bad idea
        while (iterationsLeft > 0 && leftover > 0 && providers.size() > 0) {
            iterationsLeft--;

            Pair<IEnergyProvider, Long> selected = providers.get(RAND.nextInt(providers.size()));
            IEnergyProvider scapegoat = selected.getFirst();

            long toUse = Math.min(leftover, scapegoat.getEnergyStored());
            usePower(scapegoat, toUse);
            leftover -= toUse;
        }
    }

    /**
     * Аналог sendPowerDiode: одностороння передача энергии (диоды/пилоны), 1:1 с оригиналом.
     */
    public long sendPowerDiode(long power) {

        if (receiverEntries.isEmpty()) return power;

        long timestamp = System.currentTimeMillis();

        List<Pair<IEnergyReceiver, Long>>[] receivers = new ArrayList[IEnergyReceiver.Priority.values().length];
        for (int i = 0; i < receivers.length; i++) receivers[i] = new ArrayList<>();
        long[] demand = new long[IEnergyReceiver.Priority.values().length];
        long totalDemand = 0;

        Iterator<Entry<IEnergyReceiver, Long>> recIt = receiverEntries.entrySet().iterator();

        while (recIt.hasNext()) {
            Entry<IEnergyReceiver, Long> entry = recIt.next();
            if (timestamp - entry.getValue() > timeout) { recIt.remove(); continue; }
            long rec = Math.min(entry.getKey().getMaxEnergyStored() - entry.getKey().getEnergyStored(), entry.getKey().getReceiveSpeed());
            int p = entry.getKey().getPriority().ordinal();
            receivers[p].add(new Pair<>(entry.getKey(), rec));
            demand[p] += rec;
            totalDemand += rec;
        }

        long toTransfer = Math.min(power, totalDemand);
        long energyUsed = 0;

        for (int i = IEnergyReceiver.Priority.values().length - 1; i >= 0; i--) {
            List<Pair<IEnergyReceiver, Long>> list = receivers[i];
            long priorityDemand = demand[i];

            for (Pair<IEnergyReceiver, Long> entry : list) {
                double weight = (double) entry.getSecond() / (double) (priorityDemand);
                long toSend = (long) Math.max(toTransfer * weight, 0D);
                energyUsed += (toSend - transferPower(entry.getFirst(), toSend));
            }

            toTransfer -= energyUsed;
        }

        this.energyTracker += energyUsed;

        return power - energyUsed;
    }

    /**
     * Аналог IEnergyReceiverMK2.transferPower: возвращает "перелив" (overshoot).
     */
    private static long transferPower(IEnergyReceiver receiver, long power) {
        if (power + receiver.getEnergyStored() <= receiver.getMaxEnergyStored()) {
            receiver.setEnergyStored(power + receiver.getEnergyStored());
            return 0;
        }
        long capacity = receiver.getMaxEnergyStored() - receiver.getEnergyStored();
        long overshoot = power - capacity;
        receiver.setEnergyStored(receiver.getMaxEnergyStored());
        return overshoot;
    }

    /**
     * Аналог IEnergyProviderMK2.usePower.
     */
    private static void usePower(IEnergyProvider provider, long power) {
        provider.extractEnergy(power, false);
    }

    /**
     * Аналог NodeNet.isBadLink для наших BE-подписчиков.
     */
    public static boolean isBadLink(Object o) {
        if (o instanceof BlockEntity be && be.isRemoved()) return true;
        return false;
    }
}

