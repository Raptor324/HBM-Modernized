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

    public long[][] fluidDemand        = new long[PRESSURE_LEVELS][PRIORITY_LEVELS];
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

        // --- Infinite bypass mode ---
        // Если в сети есть участник, который одновременно provider+receiver и помечен как "infinite",
        // то балансировка должна быть обойдена: за один тик заполняем/осушаем ВСЮ сеть.
        // При этом требуем, чтобы участник реально мог отдавать/принимать сейчас:
        // - для source: available > 0 (если машина не может отдавать в сеть — трогаем только её саму)
        // - для sink: demand > 0
        boolean infiniteSource = false;
        for (IFluidProviderMK2 p : providerEntries.keySet()) {
            if (!receiverEntries.containsKey(p)) continue; // интересуют только трансиверы (например цистерна)
            if (!p.isInfiniteNetworkSource(type)) continue;
            if (p.getFluidAvailable(type, 0) <= 0) continue;
            infiniteSource = true;
            break;
        }
        if (infiniteSource) {
            forceFillAllReceivers();
            cleanUp();
            return;
        }

        boolean infiniteSink = false;
        for (IFluidReceiverMK2 r : receiverEntries.keySet()) {
            if (!(r instanceof IFluidProviderMK2)) continue;
            if (!r.isInfiniteNetworkSink(type)) continue;
            if (r.getDemand(type, 0) <= 0) continue;
            infiniteSink = true;
            break;
        }
        if (infiniteSink) {
            forceDrainAllProviders();
            cleanUp();
            return;
        }

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
        // Совпадает с 1.7.10 FluidNetMK2.setupFluidReceivers(): полный необрезанный спрос ресивера.
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
                long required = Math.min(receiver.getDemand(type, p), receiver.getReceiverSpeed(type, p));
                int priority = receiver.getFluidPriority().ordinal();
                receivers[p][priority].add(Pair.of(receiver, required));
                fluidDemand[p][priority] += required;
            }
        }
    }

    // --- Transfer (parity port of FluidNetMK2.transferFluid) ---

    public void transferFluid() {
        // Порт FluidNetMK2.transferFluid (1.7.10): ресиверы — долями от общего объёма;
        // списание с провайдеров — строго пропорционально доле в суммарном available + добор случайными итерациями.
        long[] received           = new long[PRESSURE_LEVELS];
        long[] notAccountedFor    = new long[PRESSURE_LEVELS];

        for (int p = 0; p < PRESSURE_LEVELS; p++) {
            long totalAvailable = fluidAvailable[p];

            for (int i = PRIORITY_LEVELS - 1; i >= 0; i--) {
                long toTransfer = Math.min(fluidDemand[p][i], totalAvailable);
                if (toTransfer <= 0) continue;

                long priorityDemand = fluidDemand[p][i];

                for (Pair<IFluidReceiverMK2, Long> entry : receivers[p][i]) {
                    double weight = (double) entry.getSecond() / (double) priorityDemand;
                    long toSend   = (long) Math.max(toTransfer * weight, 0D);
                    toSend -= entry.getFirst().transferFluid(type, p, toSend);
                    received[p] += toSend;
                    fluidTracker += toSend;
                }

                totalAvailable -= received[p];
            }

            notAccountedFor[p] = received[p];
        }

        for (int p = 0; p < PRESSURE_LEVELS; p++) {
            if (fluidAvailable[p] <= 0) continue;

            for (Pair<IFluidProviderMK2, Long> entry : providers[p]) {
                double weight = (double) entry.getSecond() / (double) fluidAvailable[p];
                long toUse      = (long) Math.max(received[p] * weight, 0D);
                entry.getFirst().useUpFluid(type, p, toUse);
                notAccountedFor[p] -= toUse;
            }
        }

        for (int p = 0; p < PRESSURE_LEVELS; p++) {
            int iterationsLeft = 100;
            while (iterationsLeft > 0 && notAccountedFor[p] > 0 && !providers[p].isEmpty()) {
                iterationsLeft--;
                Pair<IFluidProviderMK2, Long> selected = providers[p].get(RAND.nextInt(providers[p].size()));
                long toUse = Math.min(notAccountedFor[p], selected.getFirst().getFluidAvailable(type, p));
                selected.getFirst().useUpFluid(type, p, toUse);
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

    /**
     * "Мгновенная" заливка: за один тик заполнить всех получателей, которые вообще могут принять жидкость.
     * Используется для бесконечных бочек/источников, чтобы не зависеть от балансировщика/анти-осцилляции.
     *
     * Важно: намеренно игнорирует provider/receiver speed и приоритеты — работает как "infinite source".
     */
    public void forceFillAllReceivers() {
        if (receiverEntries.isEmpty()) return;
        for (IFluidReceiverMK2 receiver : new ArrayList<>(receiverEntries.keySet())) {
            if (isBadLink(receiver)) continue;
            int[] range = receiver.getReceivingPressureRange(type);
            for (int p = range[0]; p <= range[1]; p++) {
                long demand = receiver.getDemand(type, p);
                if (demand <= 0) continue;
                receiver.transferFluid(type, p, demand);
            }
        }
    }

    /**
     * "Мгновенный" слив: за один тик опустошить всех провайдеров, которые вообще могут отдавать жидкость.
     * Используется для бесконечных бочек-утилизаторов (void sink), чтобы не зависеть от балансировщика.
     *
     * Важно: намеренно игнорирует speed и приоритеты — работает как "void".
     */
    public void forceDrainAllProviders() {
        if (providerEntries.isEmpty()) return;
        for (IFluidProviderMK2 provider : new ArrayList<>(providerEntries.keySet())) {
            if (isBadLink(provider)) continue;
            int[] range = provider.getProvidingPressureRange(type);
            for (int p = range[0]; p <= range[1]; p++) {
                long avail = provider.getFluidAvailable(type, p);
                if (avail <= 0) continue;
                provider.useUpFluid(type, p, avail);
            }
        }
    }

    public Fluid getType() { return type; }
}
