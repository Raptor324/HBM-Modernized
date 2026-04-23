package com.hbm_m.api.fluids;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hbm_m.api.network.NodeNet;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.level.material.Fluid;

/**
 * Жидкостная сеть — порт FluidNetMK2 из 1.7.10.
 *
 * Алгоритм передачи (per tick):
 *   1. setupFluidProviders: собрать доступные объёмы с учётом pressure/speed, убрать устаревшие.
 *   2. setupFluidReceivers: собрать спрос с учётом pressure/priority/speed, убрать устаревшие.
 *   3. transferFluid: пропорциональное распределение по приоритетам, 100-итерационный добор остатка.
 *   4. cleanUp: сброс временных массивов.
 */
public class FluidNet extends NodeNet<IFluidReceiverMK2, IFluidProviderMK2, FluidNode> {

    /** Суммарный объём, переданный за последний тик (для gauge/debug). */
    public long fluidTracker = 0L;

    protected static final int TIMEOUT = 3_000; // мс
    protected static long currentTime = 0;

    protected final Fluid type;

    // --- Временные структуры (пересоздаются каждый тик) ---

    private final int PRESSURE_LEVELS = IFluidUserMK2.HIGHEST_VALID_PRESSURE + 1;
    private final int PRIORITY_LEVELS = ConnectionPriority.values().length;

    public long[] fluidAvailable      = new long[PRESSURE_LEVELS];
    @SuppressWarnings("unchecked")
    public List<Pair<IFluidProviderMK2, Long>>[] providers =
            (List<Pair<IFluidProviderMK2, Long>>[]) new ArrayList[PRESSURE_LEVELS];

    public long[][] fluidDemand        = new long[PRESSURE_LEVELS][PRESSURE_LEVELS];
    @SuppressWarnings("unchecked")
    public List<Pair<IFluidReceiverMK2, Long>>[][] receivers =
            (List<Pair<IFluidReceiverMK2, Long>>[][]) new ArrayList[PRESSURE_LEVELS][PRIORITY_LEVELS];

    public FluidNet(Fluid type) {
        super();
        this.type = type;
        for (int p = 0; p < PRESSURE_LEVELS; p++) {
            providers[p] = new ArrayList<>();
            for (int pr = 0; pr < PRIORITY_LEVELS; pr++) {
                receivers[p][pr] = new ArrayList<>();
            }
        }
    }

    @Override
    public void resetTrackers() {
        this.fluidTracker = 0L;
    }

    @Override
    public void update() {
        if (providerEntries.isEmpty() || receiverEntries.isEmpty()) {
            return;
        }
        currentTime = System.currentTimeMillis();

        setupFluidProviders();
        setupFluidReceivers();
        transferFluid();
        cleanUp();
    }

    // --- Setup phases ---

    public void setupFluidProviders() {
        Iterator<Map.Entry<IFluidProviderMK2, Long>> it = providerEntries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<IFluidProviderMK2, Long> entry = it.next();
            if (currentTime - entry.getValue() > TIMEOUT || isBadLink(entry.getKey())) {
                it.remove();
                continue;
            }
            IFluidProviderMK2 provider = entry.getKey();
            int[] range = provider.getProvidingPressureRange(type);
            for (int p = range[0]; p <= range[1]; p++) {
                long avail = Math.min(provider.getFluidAvailable(type, p), provider.getProviderSpeed(type, p));
                providers[p].add(Pair.of(provider, avail));
                fluidAvailable[p] += avail;
            }
        }
    }

    public void setupFluidReceivers() {
        Iterator<Map.Entry<IFluidReceiverMK2, Long>> it = receiverEntries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<IFluidReceiverMK2, Long> entry = it.next();
            if (currentTime - entry.getValue() > TIMEOUT || isBadLink(entry.getKey())) {
                it.remove();
                continue;
            }
            IFluidReceiverMK2 receiver = entry.getKey();
            int[] range = receiver.getReceivingPressureRange(type);
            for (int p = range[0]; p <= range[1]; p++) {
                long demand = Math.min(receiver.getDemand(type, p), receiver.getReceiverSpeed(type, p));
                int priority = receiver.getFluidPriority().ordinal();
                receivers[p][priority].add(Pair.of(receiver, demand));
                fluidDemand[p][priority] += demand;
            }
        }
    }

    // --- Transfer (parity port of FluidNetMK2.transferFluid) ---

    public void transferFluid() {
        long[] received        = new long[PRESSURE_LEVELS];
        long[] notAccountedFor = new long[PRESSURE_LEVELS];

        for (int p = 0; p < PRESSURE_LEVELS; p++) {
            long totalAvailable = fluidAvailable[p];

            // От высокого приоритета к низкому
            for (int i = PRIORITY_LEVELS - 1; i >= 0; i--) {
                long toTransfer = Math.min(fluidDemand[p][i], totalAvailable);
                if (toTransfer <= 0) continue;

                long priorityDemand = fluidDemand[p][i];
                long receivedThisPriority = 0;

                List<Pair<IFluidReceiverMK2, Long>> recList = receivers[p][i];
                int nRec = recList.size();
                if (nRec > 0) {
                    long[] recvShares = new long[nRec];
                    long recvSum = 0;
                    for (int k = 0; k < nRec; k++) {
                        double weight = priorityDemand > 0
                                ? (double) recList.get(k).getSecond() / priorityDemand
                                : 0.0;
                        recvShares[k] = (long) Math.floor(toTransfer * weight);
                        recvSum += recvShares[k];
                    }
                    long recvRem = toTransfer - recvSum;
                    for (int k = 0; recvRem > 0 && k < nRec; k++) {
                        recvShares[k]++;
                        recvRem--;
                    }
                    for (int k = 0; k < nRec; k++) {
                        if (recvShares[k] <= 0) continue;
                        long toSend = recvShares[k];
                        toSend -= recList.get(k).getFirst().transferFluid(type, p, toSend);
                        received[p] += toSend;
                        receivedThisPriority += toSend;
                        fluidTracker += toSend;
                    }
                }

                // Списываем только объём текущего приоритета (накопленный received[p] — за все приоритеты)
                totalAvailable -= receivedThisPriority;
            }

            notAccountedFor[p] = received[p];
        }

        // Списать с провайдеров пропорционально их доле (с добором остатка от округления)
        for (int p = 0; p < PRESSURE_LEVELS; p++) {
            if (fluidAvailable[p] <= 0) continue;
            List<Pair<IFluidProviderMK2, Long>> provList = providers[p];
            int nProv = provList.size();
            if (nProv == 0) continue;
            long[] provShares = new long[nProv];
            long provSum = 0;
            for (int k = 0; k < nProv; k++) {
                double weight = (double) provList.get(k).getSecond() / fluidAvailable[p];
                provShares[k] = (long) Math.floor(received[p] * weight);
                provSum += provShares[k];
            }
            long provRem = received[p] - provSum;
            for (int k = 0; provRem > 0 && k < nProv; k++) {
                provShares[k]++;
                provRem--;
            }
            for (int k = 0; k < nProv; k++) {
                if (provShares[k] <= 0) continue;
                provList.get(k).getFirst().useUpFluid(type, p, provShares[k]);
                notAccountedFor[p] -= provShares[k];
            }
        }

        // Добрать остаток округления (до 100 итераций случайными провайдерами)
        for (int p = 0; p < PRESSURE_LEVELS; p++) {
            int iter = 100;
            while (iter > 0 && notAccountedFor[p] > 0 && !providers[p].isEmpty()) {
                iter--;
                Pair<IFluidProviderMK2, Long> sel = providers[p].get(NodeNet.RAND.nextInt(providers[p].size()));
                long toUse = Math.min(notAccountedFor[p], sel.getFirst().getFluidAvailable(type, p));
                sel.getFirst().useUpFluid(type, p, toUse);
                notAccountedFor[p] -= toUse;
            }
        }
    }

    // --- Clean up temporary arrays ---

    public void cleanUp() {
        for (int p = 0; p < PRESSURE_LEVELS; p++) {
            fluidAvailable[p] = 0;
            providers[p].clear();
            for (int pr = 0; pr < PRIORITY_LEVELS; pr++) {
                fluidDemand[p][pr] = 0;
                receivers[p][pr].clear();
            }
        }
    }

    public Fluid getType() { return type; }
}
