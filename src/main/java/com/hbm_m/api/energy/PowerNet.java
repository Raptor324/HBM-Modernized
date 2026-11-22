package com.hbm_m.api.energy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.hbm_m.api.network.NodeNet;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт api/hbm/energymk2/PowerNetMK2 из 1.7.10 — 1:1.
 * Технически MK3, поскольку работает поверх UNINOS-порта.
 *
 * Расширение: чужие FE-хранилища участвуют в распределении как
 * {@link ForeignEnergyEndpoint} (приём с низшим приоритетом, отдача в последнюю
 * очередь), а {@link ForeignEnergyBridge} публикует бюджеты избытка/нехватки
 * для FE-капабилити самих проводов — конверсия везде через EnergyConversion.
 */
public class PowerNet extends NodeNet<IEnergyReceiver, IEnergyProvider, Nodespace.PowerNode> {

    public long energyTracker = 0L;

    /** Мост чужой FE: бюджеты наружу/внутрь для FE-капабилити проводов сети. */
    public final ForeignEnergyBridge foreignBridge = new ForeignEnergyBridge();

    /** Чужие FE-хранилища, увиденные гранями проводников: ключ (позиция+грань) → endpoint. */
    public final HashMap<ForeignEnergyEndpoint, Long> foreignEndpoints = new HashMap<>();

    protected static int timeout = 3_000;

    @Override
    public void resetTrackers() {
        this.energyTracker = 0;
    }

    /**
     * Сканировать соседей проводника на чужие FE-хранилища и завести их в сеть.
     * Вызывается из trySubscribe/tryProvide при успешной подписке машины.
     */
    public void refreshForeign(ServerLevel level, BlockPos conductorPos) {
        long now = System.currentTimeMillis();
        if (!foreignEndpoints.isEmpty()) {
            foreignEndpoints.values().removeIf(ts -> now - ts > timeout);
            foreignEndpoints.keySet().removeIf(ep -> !ep.isLoaded());
        }
        for (Direction dir : Direction.values()) {
            BlockPos p = conductorPos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(p);
            // Наша энерго-семья подключается нативно (подписки), чужая — через endpoint.
            if (neighbor == null || neighbor instanceof IEnergyConnector || neighbor instanceof PowerConductor) continue;
            ForeignEnergyEndpoint ep = findEndpoint(p, dir.getOpposite());
            if (ep == null) {
                ep = ForeignEnergyEndpoint.probe(neighbor, dir.getOpposite());
                if (ep == null) continue;
                foreignEndpoints.put(ep, now);
                addReceiver(ep);
                addProvider(ep);
            } else {
                foreignEndpoints.put(ep, now);
            }
        }
    }

    private ForeignEnergyEndpoint findEndpoint(BlockPos pos, Direction side) {
        for (ForeignEnergyEndpoint ep : foreignEndpoints.keySet()) {
            if (ep.be().getBlockPos().equals(pos) && ep.side() == side) return ep;
        }
        return null;
    }

    @Override
    public void update() {

        // Чужие endpoints пересчитываются каждый тик распределения.
        for (ForeignEnergyEndpoint ep : foreignEndpoints.keySet()) {
            ep.refresh();
        }

        if (providerEntries.isEmpty() && receiverEntries.isEmpty()
                && foreignEndpoints.isEmpty() && foreignBridge.injected() <= 0) {
            return;
        }

        long timestamp = System.currentTimeMillis();

        List<Pair<IEnergyProvider, Long>> nativeProviders = new ArrayList<>();
        List<Pair<IEnergyProvider, Long>> foreignProviders = new ArrayList<>();
        long nativeAvailable = 0;
        long powerAvailable = 0;

        // sum up available power; чужие поставщики тратятся только после нативных
        Iterator<Entry<IEnergyProvider, Long>> provIt = providerEntries.entrySet().iterator();
        while (provIt.hasNext()) {
            Entry<IEnergyProvider, Long> entry = provIt.next();
            if (timestamp - entry.getValue() > timeout || isBadLink(entry.getKey())) { provIt.remove(); continue; }
            long src = Math.min(entry.getKey().getEnergyStored(), entry.getKey().getProvideSpeed());
            if (src > 0) {
                Pair<IEnergyProvider, Long> pair = new Pair<>(entry.getKey(), src);
                if (entry.getKey() instanceof ForeignEnergyEndpoint) {
                    foreignProviders.add(pair);
                } else {
                    nativeProviders.add(pair);
                    nativeAvailable += src;
                }
                powerAvailable += src;
            }
        }

        // Энергия, влитая чужой FE через мост, входит в распределение как чужой поставщик.
        long injected = foreignBridge.injected();
        if (injected > 0) {
            foreignProviders.add(new Pair<>(foreignBridge.injectedSource(), injected));
            powerAvailable += injected;
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
                energyUsed += (toSend - entry.getFirst().transferPower(toSend)); // leftovers are subtracted from the intended amount to use up
            }

            toTransfer -= energyUsed;
        }

        this.energyTracker += energyUsed;

        // remove power from providers: нативные покрывают transfer первыми,
        // чужие endpoint'ы/мост — только из остатка спроса
        long usedNative = Math.min(energyUsed, nativeAvailable);
        long usedForeign = energyUsed - usedNative;
        long leftover = energyUsed;

        long foreignAvailable = 0;
        for (Pair<IEnergyProvider, Long> entry : foreignProviders) foreignAvailable += entry.getSecond();

        if (usedNative > 0 && nativeAvailable > 0) {
            for (Pair<IEnergyProvider, Long> entry : nativeProviders) {
                double weight = (double) entry.getSecond() / (double) nativeAvailable;
                long toUse = (long) Math.max(usedNative * weight, 0D);
                usePower(entry.getFirst(), toUse);
                leftover -= toUse;
            }
        }

        if (usedForeign > 0 && foreignAvailable > 0) {
            for (Pair<IEnergyProvider, Long> entry : foreignProviders) {
                double weight = (double) entry.getSecond() / (double) foreignAvailable;
                long toUse = (long) Math.max(usedForeign * weight, 0D);
                usePower(entry.getFirst(), toUse);
                leftover -= toUse;
            }
        }

        // rounding error compensation, detects surplus that hasn't been used and removes it from random providers
        int iterationsLeft = 100; // whiles without emergency brakes are a bad idea
        List<Pair<IEnergyProvider, Long>> allProviders = new ArrayList<>(nativeProviders);
        allProviders.addAll(foreignProviders);
        while (iterationsLeft > 0 && leftover > 0 && allProviders.size() > 0) {
            iterationsLeft--;

            Pair<IEnergyProvider, Long> selected = allProviders.get(RAND.nextInt(allProviders.size()));
            IEnergyProvider scapegoat = selected.getFirst();

            long toUse = Math.min(leftover, scapegoat.getEnergyStored());
            usePower(scapegoat, toUse);
            leftover -= toUse;
        }

        // Сетевые обещания чужой FE (extractFe) снимаются с нативных поставщиков тем же механизмом.
        long owed = foreignBridge.drainOwed();
        iterationsLeft = 100;
        while (iterationsLeft > 0 && owed > 0 && nativeProviders.size() > 0) {
            iterationsLeft--;

            Pair<IEnergyProvider, Long> selected = nativeProviders.get(RAND.nextInt(nativeProviders.size()));
            IEnergyProvider scapegoat = selected.getFirst();

            long toUse = Math.min(owed, scapegoat.getEnergyStored());
            usePower(scapegoat, toUse);
            owed -= toUse;
        }
        foreignBridge.carryOwed(owed);

        // Публикация бюджетов моста: неиспользованный избыток и нехватка нативного спроса.
        // ~тики: монотонная метка от реального времени (окно latch 4 «тика» ≈ 200 мс).
        foreignBridge.publish(
                Math.max(0, powerAvailable - energyUsed),
                Math.max(0, totalDemand - energyUsed),
                timestamp / 50);
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
                energyUsed += (toSend - entry.getFirst().transferPower(toSend));
            }

            toTransfer -= energyUsed;
        }

        this.energyTracker += energyUsed;

        return power - energyUsed;
    }

    /**
     * Аналог NodeNet.isBadLink для наших BE-подписчиков.
     */
    public static boolean isBadLink(Object o) {
        if (o instanceof BlockEntity be && be.isRemoved()) return true;
        return false;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void joinNetworks(NodeNet other) {
        if (other instanceof PowerNet otherNet) {
            // Мост и endpoints поглощаемой сети переезжают в выжившую.
            foreignBridge.absorb(otherNet.foreignBridge);
            for (ForeignEnergyEndpoint ep : otherNet.foreignEndpoints.keySet()) {
                if (findEndpoint(ep.be().getBlockPos(), ep.side()) == null) {
                    foreignEndpoints.put(ep, System.currentTimeMillis());
                }
            }
            otherNet.foreignEndpoints.clear();
        }
        super.joinNetworks(other);
    }

    @Override
    public void destroy() {
        foreignEndpoints.clear();
        super.destroy();
    }

    /**
     * Аналог IEnergyProviderMK2.usePower.
     */
    private static void usePower(IEnergyProvider provider, long power) {
        provider.extractEnergy(power, false);
    }
}
