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
        // Anti-oscillation (buffer ↔ buffer), как в оригинальной задумке Modernized:
        // Если участник сети одновременно provider+receiver и имеет LOW приоритет (буфер),
        // то его "спрос" ограничиваем до выхода на среднее по таким буферам.
        //
        // Это предотвращает сценарий "два буфера → перелей всё в один" и ping-pong.
        long[] bufferTotals = new long[PRESSURE_LEVELS];
        int[] bufferCounts  = new int[PRESSURE_LEVELS];

        for (IFluidReceiverMK2 receiver : receiverEntries.keySet()) {
            if (isBadLink(receiver)) continue;
            if (receiver.getFluidPriority() != ConnectionPriority.LOW) continue;
            if (!(receiver instanceof IFluidProviderMK2 prov)) continue;
            if (!providerEntries.containsKey(prov)) continue;

            int[] recvRange = receiver.getReceivingPressureRange(type);
            int[] provRange = prov.getProvidingPressureRange(type);
            int min = Math.max(recvRange[0], provRange[0]);
            int max = Math.min(recvRange[1], provRange[1]);
            for (int p = min; p <= max; p++) {
                long current = Math.max(0L, prov.getFluidAvailable(type, p));
                bufferTotals[p] += current;
                bufferCounts[p] += 1;
            }
        }

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

                // Ограничение спроса буфера до "среднего" — устраняет полное перекачивание туда‑сюда
                // и делает балансировку детерминированной (без хвостов от порядка).
                if (demand > 0
                        && receiver.getFluidPriority() == ConnectionPriority.LOW
                        && receiver instanceof IFluidProviderMK2 prov
                        && providerEntries.containsKey(prov)
                        && bufferCounts[p] > 0) {
                    long avg = bufferTotals[p] / (long) bufferCounts[p];
                    long current = Math.max(0L, prov.getFluidAvailable(type, p));
                    long cap = Math.max(0L, avg - current);
                    if (cap < demand) {
                        demand = cap;
                    }
                }

                int priority = receiver.getFluidPriority().ordinal();
                receivers[p][priority].add(Pair.of(receiver, demand));
                fluidDemand[p][priority] += demand;
            }
        }
    }

    // --- Transfer (parity port of FluidNetMK2.transferFluid) ---

    public void transferFluid() {
        // Передача должна быть "как в оригинале": при ветвлении в одинаковые приёмники
        // жидкость делится поровну (в рамках одного pressure+priority), а не "в первый успел — всё забрал".
        //
        // Поэтому внутри pressure+priority делаем пропорциональное распределение по demand, затем
        // добираем остаток/непринятое детерминированным вторым проходом.
        for (int p = 0; p < PRESSURE_LEVELS; p++) {
            long totalAvailable = fluidAvailable[p];
            if (totalAvailable <= 0) continue;

            List<Pair<IFluidProviderMK2, Long>> provList = providers[p];
            if (provList.isEmpty()) continue;

            // От высокого приоритета к низкому
            for (int pr = PRIORITY_LEVELS - 1; pr >= 0; pr--) {
                long priorityDemand = fluidDemand[p][pr];
                if (priorityDemand <= 0 || totalAvailable <= 0) continue;

                long toTransfer = Math.min(priorityDemand, totalAvailable);
                if (toTransfer <= 0) continue;

                List<Pair<IFluidReceiverMK2, Long>> recList = receivers[p][pr];
                if (recList.isEmpty()) continue;

                // --- 1) Пропорциональная раздача (при равных demand -> поровну) ---
                int n = recList.size();
                long[] demand = new long[n];
                long totalWant = 0L;
                for (int i = 0; i < n; i++) {
                    long d = Math.max(0L, recList.get(i).getSecond());
                    demand[i] = d;
                    totalWant += d;
                }
                if (totalWant <= 0) continue;

                long[] alloc = new long[n];
                long allocSum = 0L;
                for (int i = 0; i < n; i++) {
                    long a = (toTransfer * demand[i]) / totalWant; // floor
                    if (a < 0) a = 0;
                    alloc[i] = a;
                    allocSum += a;
                }
                long remainder = toTransfer - allocSum;
                // Дет. добор "остатка" +1 mB тем, кто ещё не получил весь спрос
                for (int i = 0; i < n && remainder > 0; i++) {
                    if (alloc[i] < demand[i]) {
                        alloc[i] += 1;
                        remainder -= 1;
                    }
                }
                // Если остаток всё ещё есть (все уже набрали demand) — просто считаем, что он не нужен.

                long acceptedThisPriority = 0L;
                long[] unmet = new long[n];
                long remaining = toTransfer;

                for (int i = 0; i < n && remaining > 0; i++) {
                    long want = Math.min(alloc[i], remaining);
                    if (want <= 0) {
                        unmet[i] = demand[i];
                        continue;
                    }
                    Pair<IFluidReceiverMK2, Long> rec = recList.get(i);
                    long leftover = rec.getFirst().transferFluid(type, p, want);
                    long accepted = want - Math.max(0L, leftover);
                    if (accepted > 0) {
                        acceptedThisPriority += accepted;
                        remaining -= accepted;
                        fluidTracker += accepted;
                    }
                    unmet[i] = Math.max(0L, demand[i] - accepted);
                }

                // --- 2) Добор непринятого/остатка: дет. round-robin до 100 итераций ---
                int safety = 100;
                while (remaining > 0 && safety-- > 0) {
                    boolean progressed = false;
                    for (int i = 0; i < n && remaining > 0; i++) {
                        long need = unmet[i];
                        if (need <= 0) continue;
                        long want = Math.min(need, remaining);
                        if (want <= 0) continue;
                        Pair<IFluidReceiverMK2, Long> rec = recList.get(i);
                        long leftover = rec.getFirst().transferFluid(type, p, want);
                        long accepted = want - Math.max(0L, leftover);
                        if (accepted <= 0) {
                            // этот ресивер сейчас не принимает — пропускаем
                            unmet[i] = 0;
                            continue;
                        }
                        progressed = true;
                        acceptedThisPriority += accepted;
                        unmet[i] = Math.max(0L, unmet[i] - accepted);
                        remaining -= accepted;
                        fluidTracker += accepted;
                    }
                    if (!progressed) break;
                }

                if (acceptedThisPriority <= 0) continue;

                // Списать ровно acceptedThisPriority с провайдеров.
                long toConsume = acceptedThisPriority;
                for (int i = 0; i < provList.size() && toConsume > 0; i++) {
                    Pair<IFluidProviderMK2, Long> prov = provList.get(i);
                    long avail = prov.getSecond();
                    if (avail <= 0) continue;
                    long use = Math.min(avail, toConsume);
                    if (use <= 0) continue;
                    prov.getFirst().useUpFluid(type, p, use);
                    toConsume -= use;
                }

                // Реально доступного стало меньше на принятый объём (даже если какой-то провайдер соврал о доступности,
                // это лишь снизит перенос на следующих шагах; "хвостов" из-за округления не будет).
                totalAvailable -= acceptedThisPriority;
                if (totalAvailable <= 0) break;
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
