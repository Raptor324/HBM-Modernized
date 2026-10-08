// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (WorldRadiationData: runExactExchangeSweeps, sweepX/Y/Z, diffuseXZ(Wide),
// exchangeUni, exchangeFaceExact(Y), runDecayEmissionAndEffects, DiffuseX/Y/ZTask,
// DecayEmissionTask), Commit 3f9a261a. Im Original Teil von WorldRadiationData; im Port als
// eigene Klasse herausgeloest, Logik unveraendert (Java-Pfad).

package com.hbm_m.radiation.ntmnext;

import static com.hbm_m.radiation.ntmnext.NtmRadiationSystem.*;

import com.hbm_m.radiation.ntmnext.SectionScanner.SectionSources;
import com.hbm_m.radiation.ntmnext.WorldRadiationData.MultiSectionRef;
import com.hbm_m.radiation.ntmnext.WorldRadiationData.SectionRef;
import com.hbm_m.radiation.ntmnext.WorldRadiationData.SingleMaskedSectionRef;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Loeser fuer eine Dimension: pro Schritt erst Zerfall + Quellen-Emission (je Sektion), dann
 * exakte paarweise Austausch-Sweeps in X, Y und Z (Reihenfolge rotiert jeden Schritt).
 *
 * <p>Parallelisierung: Chunks liegen in 4 Paritaets-Eimern (x&1, z&1). Alle Paare eines Eimers
 * beruehren disjunkte Chunks und laufen parallel im {@link NtmRadiationSystem#RAD_POOL}.
 *
 * <p>Erweiterungspunkte (Stufe 2-4): weitere Felder (Strahlungsarten, Kontamination) koennen
 * eigene Sweep-Phasen bekommen; Wind/Regen als zusaetzlicher Advektionsschritt nach den
 * Diffusions-Sweeps.
 */
final class RadiationSolver {

    private final WorldRadiationData g;

    RadiationSolver(WorldRadiationData grid) {
        this.g = grid;
    }

    // ------------------------------------------------------------------
    // Phasen
    // ------------------------------------------------------------------

    void runExactExchangeSweeps() {
        g.rebuildPairListsIfNeeded();
        int[][] yBuckets = g.parityBucketIds;
        int[] yCounts = g.parityCounts;
        int yc0 = yCounts[0], yc1 = yCounts[1], yc2 = yCounts[2], yc3 = yCounts[3];
        int yth0 = getTaskThreshold(yc0, 64),
                yth1 = getTaskThreshold(yc1, 64),
                yth2 = getTaskThreshold(yc2, 64),
                yth3 = getTaskThreshold(yc3, 64);
        int[] xCounts = g.xPairCounts;
        int[] zCounts = g.zPairCounts;
        int xc0 = xCounts[0], xc1 = xCounts[1], xc2 = xCounts[2], xc3 = xCounts[3];
        int zc0 = zCounts[0], zc1 = zCounts[1], zc2 = zCounts[2], zc3 = zCounts[3];
        int xth0 = getTaskThreshold(xc0, 64),
                xth1 = getTaskThreshold(xc1, 64),
                xth2 = getTaskThreshold(xc2, 64),
                xth3 = getTaskThreshold(xc3, 64);
        int zth0 = getTaskThreshold(zc0, 64),
                zth1 = getTaskThreshold(zc1, 64),
                zth2 = getTaskThreshold(zc2, 64),
                zth3 = getTaskThreshold(zc3, 64);
        int[][] xa = g.xPairAByBucket, xb = g.xPairBByBucket;
        int[][] za = g.zPairAByBucket, zb = g.zPairBByBucket;

        int s = g.workEpoch;
        boolean fx = (s & 1) != 0, fz = (s & 2) != 0;
        int yPar = (s & 4) != 0 ? 1 : 0;
        int perm = s % 6;
        if (perm < 0) perm += 6;

        // Reihenfolge der Achsen wechselt jeden Schritt (Operator-Splitting ohne Vorzugsrichtung).
        switch (perm) {
            case 0 -> {
                sweepX(xa, xb, xc0, xc1, xc2, xc3, xth0, xth1, xth2, xth3, fx);
                sweepZ(za, zb, zc0, zc1, zc2, zc3, zth0, zth1, zth2, zth3, fz);
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
            }
            case 1 -> {
                sweepX(xa, xb, xc0, xc1, xc2, xc3, xth0, xth1, xth2, xth3, fx);
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
                sweepZ(za, zb, zc0, zc1, zc2, zc3, zth0, zth1, zth2, zth3, fz);
            }
            case 2 -> {
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
                sweepZ(za, zb, zc0, zc1, zc2, zc3, zth0, zth1, zth2, zth3, fz);
                sweepX(xa, xb, xc0, xc1, xc2, xc3, xth0, xth1, xth2, xth3, fx);
            }
            case 3 -> {
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
                sweepX(xa, xb, xc0, xc1, xc2, xc3, xth0, xth1, xth2, xth3, fx);
                sweepZ(za, zb, zc0, zc1, zc2, zc3, zth0, zth1, zth2, zth3, fz);
            }
            case 4 -> {
                sweepZ(za, zb, zc0, zc1, zc2, zc3, zth0, zth1, zth2, zth3, fz);
                sweepX(xa, xb, xc0, xc1, xc2, xc3, xth0, xth1, xth2, xth3, fx);
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
            }
            default -> {
                sweepZ(za, zb, zc0, zc1, zc2, zc3, zth0, zth1, zth2, zth3, fz);
                sweepY(yBuckets, yc0, yc1, yc2, yc3, yth0, yth1, yth2, yth3, yPar);
                sweepX(xa, xb, xc0, xc1, xc2, xc3, xth0, xth1, xth2, xth3, fx);
            }
        }
    }

    void runDecayEmissionAndEffects() {
        int[][] b = g.parityBucketIds;
        int[] c = g.parityCounts;
        int c0 = c[0], c1 = c[1], c2 = c[2], c3 = c[3];
        int th0 = getTaskThreshold(c0, 64),
                th1 = getTaskThreshold(c1, 64),
                th2 = getTaskThreshold(c2, 64),
                th3 = getTaskThreshold(c3, 64);
        var t0 = new DecayEmissionTask(b[0], 0, c0, th0).fork();
        var t1 = new DecayEmissionTask(b[1], 0, c1, th1).fork();
        var t2 = new DecayEmissionTask(b[2], 0, c2, th2).fork();
        new DecayEmissionTask(b[3], 0, c3, th3).invoke();
        t0.join();
        t1.join();
        t2.join();
    }

    void sweepX(
            int[][] aPairs,
            int[][] bPairs,
            int c0,
            int c1,
            int c2,
            int c3,
            int th0,
            int th1,
            int th2,
            int th3,
            boolean flip) {
        var t0 = new DiffuseXTask(aPairs[0], bPairs[0], 0, c0, th0);
        var t1 = new DiffuseXTask(aPairs[1], bPairs[1], 0, c1, th1);
        var t2 = new DiffuseXTask(aPairs[2], bPairs[2], 0, c2, th2);
        var t3 = new DiffuseXTask(aPairs[3], bPairs[3], 0, c3, th3);
        if (flip) {
            ForkJoinTask.invokeAll(t1, t3);
            ForkJoinTask.invokeAll(t0, t2);
        } else {
            ForkJoinTask.invokeAll(t0, t2);
            ForkJoinTask.invokeAll(t1, t3);
        }
    }

    void sweepZ(
            int[][] aPairs,
            int[][] bPairs,
            int c0,
            int c1,
            int c2,
            int c3,
            int th0,
            int th1,
            int th2,
            int th3,
            boolean flip) {
        var t0 = new DiffuseZTask(aPairs[0], bPairs[0], 0, c0, th0);
        var t1 = new DiffuseZTask(aPairs[1], bPairs[1], 0, c1, th1);
        var t2 = new DiffuseZTask(aPairs[2], bPairs[2], 0, c2, th2);
        var t3 = new DiffuseZTask(aPairs[3], bPairs[3], 0, c3, th3);
        if (flip) {
            ForkJoinTask.invokeAll(t2, t3);
            ForkJoinTask.invokeAll(t0, t1);
        } else {
            ForkJoinTask.invokeAll(t0, t1);
            ForkJoinTask.invokeAll(t2, t3);
        }
    }

    void sweepY(
            int[][] b,
            int c0,
            int c1,
            int c2,
            int c3,
            int th0,
            int th1,
            int th2,
            int th3,
            int startParity) {
        for (int p = 0; p < 2; p++) {
            int parity = startParity ^ p;
            var t0 = new DiffuseYTask(b[0], 0, c0, parity, th0).fork();
            var t1 = new DiffuseYTask(b[1], 0, c1, parity, th1).fork();
            var t2 = new DiffuseYTask(b[2], 0, c2, parity, th2).fork();
            new DiffuseYTask(b[3], 0, c3, parity, th3).invoke();
            t0.join();
            t1.join();
            t2.join();
        }
    }

    // ------------------------------------------------------------------
    // Austauschkerne
    // ------------------------------------------------------------------

    void diffuseXZ(int aId, int bId, int faceA, int faceB) {
        long kindsA = g.chunkKinds[aId];
        long kindsB = g.chunkKinds[bId];
        long actA = g.chunkActiveDirty[aId];
        long actB = g.chunkActiveDirty[bId];
        int N = g.sectionsPerChunk;
        int offA = aId * N;
        int offB = bId * N;
        double[] uniform = g.uniformRads;
        boolean d = false;

        if (kindsA == g.allUniKinds && kindsB == g.allUniKinds && !g.diffusivityTransport) {
            long changed = UniformKernels.exchange(uniform, offA, offB, N, g.uuE);
            if (changed != 0L) {
                g.chunkActiveDirty[aId] |= changed | CHUNK_DIRTY_MASK;
                g.chunkActiveDirty[bId] |= changed | CHUNK_DIRTY_MASK;
            }
            return;
        }

        for (int sy = 0; sy < N; sy++) {
            long actMask = 1L << sy;
            if (((actA | actB) & actMask) == 0L) continue;

            int idxA = offA + sy;
            int idxB = offB + sy;
            int shift = sy << 1;
            int kA = (int) ((kindsA >>> shift) & 3);
            int kB = (int) ((kindsB >>> shift) & 3);

            if (kA == KIND_UNI && kB == KIND_UNI) {
                if (exchangeUni(uniform, idxA, idxB, faceA == 5 ? AXIS_X : AXIS_Z)) {
                    g.chunkActiveDirty[aId] |= actMask;
                    g.chunkActiveDirty[bId] |= actMask;
                    d = true;
                }
            } else {
                d |= exchangeFaceExact(idxA, kA, faceA, idxB, kB, faceB);
            }
        }
        if (d) {
            g.chunkActiveDirty[aId] |= CHUNK_DIRTY_MASK;
            g.chunkActiveDirty[bId] |= CHUNK_DIRTY_MASK;
        }
    }

    static long uniformKinds(int count) {
        return count == 32 ? 0x5555555555555555L : 0x5555555555555555L & ((1L << (count * 2)) - 1L);
    }

    /** Variante fuer Dimensionen mit mehr als 32 Sektionen je Chunk. */
    void diffuseXZWide(int aId, int bId, int faceA, int faceB) {
        int wordsPerChunk = g.wordsPerChunk;
        int sectionsPerChunk = g.sectionsPerChunk;
        long[] chunkActiveDirty = g.chunkActiveDirty;
        int metaA = aId * wordsPerChunk;
        int metaB = bId * wordsPerChunk;
        int baseA = aId * sectionsPerChunk;
        int baseB = bId * sectionsPerChunk;
        boolean dirty = false;
        for (int word = 0; word < wordsPerChunk; word++) {
            int aWord = metaA + word;
            int bWord = metaB + word;
            long active = (chunkActiveDirty[aWord] | chunkActiveDirty[bWord]) & 0xffffffffL;
            if (active == 0L) continue;
            int first = word << 5;
            int count = Math.min(32, sectionsPerChunk - first);
            long kindsA = g.chunkKinds[aWord];
            long kindsB = g.chunkKinds[bWord];
            long allUniform = uniformKinds(count);
            if (kindsA == allUniform && kindsB == allUniform && !g.diffusivityTransport) {
                long changed =
                        UniformKernels.exchange(
                                g.uniformRads, baseA + first, baseB + first, count, g.uuE);
                chunkActiveDirty[aWord] |= changed;
                chunkActiveDirty[bWord] |= changed;
                dirty |= changed != 0L;
                continue;
            }
            for (long m = active; m != 0L; m &= m - 1L) {
                int lane = Long.numberOfTrailingZeros(m);
                int slot = first + lane;
                int kindA = (int) ((kindsA >>> (lane * 2)) & 3);
                int kindB = (int) ((kindsB >>> (lane * 2)) & 3);
                if (kindA == KIND_UNI && kindB == KIND_UNI) {
                    if (exchangeUni(g.uniformRads, baseA + slot, baseB + slot, faceA == 5 ? AXIS_X : AXIS_Z)) {
                        chunkActiveDirty[aWord] |= 1L << lane;
                        chunkActiveDirty[bWord] |= 1L << lane;
                        dirty = true;
                    }
                } else {
                    dirty |=
                            exchangeFaceExact(
                                    baseA + slot, kindA, faceA, baseB + slot, kindB, faceB);
                }
            }
        }
        if (dirty) {
            chunkActiveDirty[metaA] |= CHUNK_DIRTY_MASK;
            chunkActiveDirty[metaB] |= CHUNK_DIRTY_MASK;
        }
    }

    static final int AXIS_X = 0, AXIS_Y = 1, AXIS_Z = 2;

    /** Austausch zweier uniformer Sektionen entlang einer Achse (Stufe 3: Achsen-Diffusivitaet). */
    boolean exchangeUni(double[] uni, int idxA, int idxB, int axis) {
        if (!g.diffusivityTransport) return exchangeUniExactXZ(uni, idxA, idxB, g.uuE);
        return exchangeUniExactDiffusive(
                uni,
                idxA,
                idxB,
                g.uuE,
                g.diffusionDt,
                g.uniformDiffusivity[idxA * 3 + axis],
                g.uniformDiffusivity[idxB * 3 + axis]);
    }

    boolean exchangeFaceExactY(int idxA, int kA, int idxB, int kB) {
        if (kA == KIND_NONE || kB == KIND_NONE) return false;
        SectionRef[] complexSecs = g.complexSecs;
        if (kA == KIND_UNI) {
            SectionRef b = complexSecs[idxB];
            return b.exchangeWithUniform(idxA, 0, 1);
        }
        if (kB == KIND_UNI) {
            SectionRef a = complexSecs[idxA];
            return a.exchangeWithUniform(idxB, 1, 0);
        }
        SectionRef a = complexSecs[idxA];
        SectionRef b = complexSecs[idxB];
        if (kB == KIND_SINGLE) return a.exchangeWithSingle((SingleMaskedSectionRef) b, 1, 0);
        return a.exchangeWithMulti((MultiSectionRef) b, 1, 0);
    }

    boolean exchangeFaceExact(int idxA, int kA, int faceA, int idxB, int kB, int faceB) {
        if (kA == KIND_NONE || kB == KIND_NONE) return false;
        SectionRef[] complexSecs = g.complexSecs;
        if (kA == KIND_UNI) {
            SectionRef secB = complexSecs[idxB];
            return secB.exchangeWithUniform(idxA, faceB, faceA);
        } else if (kB == KIND_UNI) {
            SectionRef secA = complexSecs[idxA];
            return secA.exchangeWithUniform(idxB, faceA, faceB);
        } else {
            SectionRef secA = complexSecs[idxA];
            SectionRef secB = complexSecs[idxB];
            if (kB == KIND_SINGLE)
                return secA.exchangeWithSingle((SingleMaskedSectionRef) secB, faceA, faceB);
            return secA.exchangeWithMulti((MultiSectionRef) secB, faceA, faceB);
        }
    }

    boolean hasActivePair(int[] pairA, int[] pairB, int lo, int hi) {
        long[] chunkActiveDirty = g.chunkActiveDirty;
        for (int i = lo; i < hi; i++) {
            if (((chunkActiveDirty[pairA[i]] | chunkActiveDirty[pairB[i]]) & g.activeMaskAll) != 0L)
                return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Fork/Join-Tasks
    // ------------------------------------------------------------------

    final class DiffuseXTask extends RecursiveAction {
        final int[] pairA;
        final int[] pairB;
        final int lo, hi, threshold;

        DiffuseXTask(int[] pairA, int[] pairB, int lo, int hi, int threshold) {
            this.pairA = pairA;
            this.pairB = pairB;
            this.lo = lo;
            this.hi = hi;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int n = hi - lo;
            if (n <= threshold) {
                if (g.wordsPerChunk > 1) {
                    for (int i = lo; i < hi; i++) diffuseXZWide(pairA[i], pairB[i], 5, 4);
                } else {
                    if (g.sectionsPerChunk >= 16 && !hasActivePair(pairA, pairB, lo, hi)) return;
                    for (int i = lo; i < hi; i++) diffuseXZ(pairA[i], pairB[i], 5, 4);
                }
                return;
            }
            int mid = (lo + hi) >>> 1;
            var left = new DiffuseXTask(pairA, pairB, lo, mid, threshold).fork();
            new DiffuseXTask(pairA, pairB, mid, hi, threshold).compute();
            left.join();
        }
    }

    final class DiffuseZTask extends RecursiveAction {
        final int[] pairA;
        final int[] pairB;
        final int lo, hi, threshold;

        DiffuseZTask(int[] pairA, int[] pairB, int lo, int hi, int threshold) {
            this.pairA = pairA;
            this.pairB = pairB;
            this.lo = lo;
            this.hi = hi;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int n = hi - lo;
            if (n <= threshold) {
                if (g.wordsPerChunk > 1) {
                    for (int i = lo; i < hi; i++) diffuseXZWide(pairA[i], pairB[i], 3, 2);
                } else {
                    if (g.sectionsPerChunk >= 16 && !hasActivePair(pairA, pairB, lo, hi)) return;
                    for (int i = lo; i < hi; i++) diffuseXZ(pairA[i], pairB[i], 3, 2);
                }
                return;
            }
            int mid = (lo + hi) >>> 1;
            var left = new DiffuseZTask(pairA, pairB, lo, mid, threshold).fork();
            new DiffuseZTask(pairA, pairB, mid, hi, threshold).compute();
            left.join();
        }
    }

    final class DiffuseYTask extends RecursiveAction {
        final int[] chunks;
        final int lo, hi, parity, threshold;

        DiffuseYTask(int[] chunks, int lo, int hi, int parity, int threshold) {
            this.chunks = chunks;
            this.lo = lo;
            this.hi = hi;
            this.parity = parity;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int n = hi - lo;
            if (n <= threshold) {
                if (g.wordsPerChunk == 1) work(lo, hi);
                else workWide(lo, hi);
                return;
            }
            int mid = (lo + hi) >>> 1;
            var left = new DiffuseYTask(chunks, lo, mid, parity, threshold).fork();
            new DiffuseYTask(chunks, mid, hi, parity, threshold).compute();
            left.join();
        }

        void work(int start, int end) {
            int N = g.sectionsPerChunk;
            long[] chunkKinds = g.chunkKinds;
            long[] chunkActiveDirty = g.chunkActiveDirty;
            for (int i = start; i < end; i++) {
                int id = chunks[i];
                int off = id * N;
                long kinds = chunkKinds[id];
                long active = chunkActiveDirty[id];
                double[] u = g.uniformRads;
                boolean d = false;

                if (kinds == g.allUniKinds) {
                    for (int sy = parity; sy < N - 1; sy += 2) {
                        long actMask = 3L << sy;
                        if ((active & actMask) == 0L) continue;
                        int idx = off + sy;
                        int idxN = idx + 1;
                        if (exchangeUni(u, idx, idxN, AXIS_Y)) {
                            chunkActiveDirty[id] |= actMask;
                            d = true;
                        }
                    }
                    if (d) chunkActiveDirty[id] |= CHUNK_DIRTY_MASK;
                    continue;
                }

                for (int sy = parity; sy < N - 1; sy += 2) {
                    long actMask = 3L << sy;
                    if ((active & actMask) == 0L) continue;

                    int idx = off + sy;
                    int idxN = idx + 1;
                    int kk = (int) ((kinds >>> (sy << 1)) & 0xF);
                    int k = kk & 3;
                    int kN = (kk >>> 2) & 3;

                    if (k == KIND_UNI && kN == KIND_UNI) {
                        if (exchangeUni(u, idx, idxN, AXIS_Y)) {
                            chunkActiveDirty[id] |= actMask;
                            d = true;
                        }
                    } else {
                        d |= exchangeFaceExactY(idx, k, idxN, kN);
                    }
                }
                if (d) chunkActiveDirty[id] |= CHUNK_DIRTY_MASK;
            }
        }

        void workWide(int start, int end) {
            int n = g.sectionsPerChunk;
            int wordsPerChunk = g.wordsPerChunk;
            long[] chunkKinds = g.chunkKinds;
            long[] chunkActiveDirty = g.chunkActiveDirty;
            for (int i = start; i < end; i++) {
                int id = chunks[i];
                int base = id * n;
                int metadataBase = id * wordsPerChunk;
                boolean dirty = false;
                for (int word = 0; word < wordsPerChunk; word++) {
                    int at = metadataBase + word;
                    int first = word << 5;
                    long active = chunkActiveDirty[at] & 0xffffffffL;
                    long kinds = chunkKinds[at];
                    int count = Math.min(32, n - first);
                    int off = base + first;
                    double[] u = g.uniformRads;
                    if (kinds == uniformKinds(count)) {
                        for (int lane = parity; lane < count - 1; lane += 2) {
                            long pairMask = 3L << lane;
                            if ((active & pairMask) == 0L) continue;
                            if (exchangeUni(u, off + lane, off + lane + 1, AXIS_Y)) {
                                chunkActiveDirty[at] |= pairMask;
                                dirty = true;
                            }
                        }
                    } else {
                        for (int lane = parity; lane < count - 1; lane += 2) {
                            long pairMask = 3L << lane;
                            if ((active & pairMask) == 0L) continue;
                            int pairKinds = (int) ((kinds >>> (lane * 2)) & 15);
                            int kind = pairKinds & 3;
                            int upperKind = pairKinds >>> 2;
                            if (kind == KIND_UNI && upperKind == KIND_UNI) {
                                if (exchangeUni(u, off + lane, off + lane + 1, AXIS_Y)) {
                                    chunkActiveDirty[at] |= pairMask;
                                    dirty = true;
                                }
                            } else {
                                dirty |=
                                        exchangeFaceExactY(
                                                off + lane, kind, off + lane + 1, upperKind);
                            }
                        }
                    }
                    // Wortgrenze: Sektion 31 mit Sektion 32 koppeln.
                    if (parity == 1
                            && word + 1 < wordsPerChunk
                            && ((active >>> 31) | (chunkActiveDirty[at + 1] & 1L)) != 0L) {
                        int kind = (int) (kinds >>> 62);
                        int upperKind = (int) (chunkKinds[at + 1] & 3);
                        if (kind == KIND_UNI && upperKind == KIND_UNI) {
                            if (exchangeUni(u, off + 31, off + 32, AXIS_Y)) {
                                chunkActiveDirty[at] |= 1L << 31;
                                chunkActiveDirty[at + 1] |= 1L;
                                dirty = true;
                            }
                        } else {
                            dirty |= exchangeFaceExactY(off + 31, kind, off + 32, upperKind);
                        }
                    }
                }
                if (dirty) chunkActiveDirty[metadataBase] |= CHUNK_DIRTY_MASK;
            }
        }
    }

    /** Zerfall (Halbwertszeit) + konstante Quellen + Nebel/Zerstoerungs-Kandidaten. */
    final class DecayEmissionTask extends RecursiveAction {
        final int[] chunks;
        final int lo, hi, threshold;
        final long[] decayMasks = new long[4];

        DecayEmissionTask(int[] chunks, int lo, int hi, int threshold) {
            this.chunks = chunks;
            this.lo = lo;
            this.hi = hi;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            int n = hi - lo;
            if (n <= threshold) {
                if (g.wordsPerChunk == 1) work(lo, hi);
                else workWide(lo, hi);
                return;
            }
            int mid = (lo + hi) >>> 1;
            var left = new DecayEmissionTask(chunks, lo, mid, threshold);
            var right = new DecayEmissionTask(chunks, mid, hi, threshold);
            left.fork();
            right.compute();
            left.join();
        }

        void work(int start, int end) {
            int N = g.sectionsPerChunk;
            int minSectionY = g.minSectionY;
            double retentionDt = g.retentionDt;
            double fogRad = g.fogRad;
            long[] chunkKinds = g.chunkKinds;
            long[] chunkActiveDirty = g.chunkActiveDirty;
            long[] chunkSourceMask = g.chunkSourceMask;
            for (int i = start; i < end; i++) {
                int id = chunks[i];
                LevelChunk chunk = g.mcChunks[id];
                if (chunk == null) continue;
                boolean dirty = g.isChunkDirty(id);
                long baseSck = SectionKeys.sectionToLong(g.cks[id], minSectionY);
                int secBase = id * N;
                long kinds = chunkKinds[id];
                long active = chunkActiveDirty[id];

                if (kinds == g.allUniKinds && chunkSourceMask[id] == 0L) {
                    long[] masks = decayMasks;
                    UniformKernels.decay(
                            g.uniformRads, secBase, N, retentionDt, g.minBound, fogRad, g.destroyRad, masks);
                    if (masks[0] != 0L) dirty = true;
                    chunkActiveDirty[id] &= ~(masks[1] & active);
                    for (long m = masks[2] & active; m != 0L; m &= m - 1L) {
                        int sy = Long.numberOfTrailingZeros(m);
                        g.maybeQueueFog(SectionKeys.setSectionY(baseSck, minSectionY + sy), 0, sy);
                    }
                    for (long m = masks[3] & active; m != 0L; m &= m - 1L) {
                        int sy = Long.numberOfTrailingZeros(m);
                        g.maybeQueueDestroy(
                                SectionKeys.setSectionY(baseSck, minSectionY + sy), 0, sy);
                    }
                    g.clearChunkDirty(id);
                    if (dirty) chunkActiveDirty[id] |= CHUNK_DIRTY_MASK;
                    continue;
                }

                long visit = active | chunkSourceMask[id];
                for (int sy = 0; sy < N; sy++) {
                    if ((visit & (1L << sy)) == 0L) continue;

                    int secIdx = secBase + sy;
                    SectionSources src = g.sectionSources[secIdx];
                    int kind = (int) ((kinds >>> (sy << 1)) & 3);

                    long sck = SectionKeys.setSectionY(baseSck, minSectionY + sy);

                    if (kind < KIND_MULTI) {
                        double prev = g.uniformRads[secIdx];
                        double next =
                                g.sanitize((src == null ? prev : src.relax(0, prev)) * retentionDt);
                        if (next != prev) {
                            g.uniformRads[secIdx] = next;
                            dirty = true;
                        }
                        if (next == 0.0D && src == null) {
                            chunkActiveDirty[id] &= ~(1L << sy);
                        } else if (next != 0.0D) {
                            if (next > fogRad) g.maybeQueueFog(sck, 0, sy);
                            if (next >= g.destroyRad) g.maybeQueueDestroy(sck, 0, sy);
                        }
                    } else {
                        MultiSectionRef multi = (MultiSectionRef) g.complexSecs[secIdx];
                        int pCount = multi.pocketCount & 0xFFFF;
                        boolean anyAlive = false;

                        for (int p = 0; p < pCount; p++) {
                            int dataIdx = p << 1;
                            double prev = multi.data[dataIdx];
                            if (prev == 0.0D && src == null) continue;

                            double next =
                                    g.sanitize(
                                            (src == null ? prev : src.relax(p, prev))
                                                    * retentionDt);
                            if (next != prev) {
                                multi.data[dataIdx] = next;
                                dirty = true;
                            }

                            if (next != 0.0D) {
                                anyAlive = true;
                                if (next > fogRad) g.maybeQueueFog(sck, p, sy);
                                if (next >= g.destroyRad) g.maybeQueueDestroy(sck, p, sy);
                            }
                        }

                        if (!anyAlive && src == null) {
                            chunkActiveDirty[id] &= ~(1L << sy);
                        }
                    }
                }
                chunkActiveDirty[id] &= ~CHUNK_DIRTY_MASK;
                if (dirty) chunkActiveDirty[id] |= CHUNK_DIRTY_MASK;
            }
        }

        void workWide(int start, int end) {
            int minSectionY = g.minSectionY;
            int wordsPerChunk = g.wordsPerChunk;
            double retentionDt = g.retentionDt;
            double fogRad = g.fogRad;
            long[] chunkKinds = g.chunkKinds;
            long[] chunkActiveDirty = g.chunkActiveDirty;
            long[] chunkSourceMask = g.chunkSourceMask;
            for (int i = start; i < end; i++) {
                int id = chunks[i];
                LevelChunk chunk = g.mcChunks[id];
                if (chunk == null) continue;
                boolean dirty = g.isChunkDirty(id);
                long baseSck = SectionKeys.sectionToLong(g.cks[id], minSectionY);
                int metadataBase = id * wordsPerChunk;
                for (int word = 0; word < wordsPerChunk; word++) {
                    int first = word << 5;
                    int N = Math.min(32, g.sectionsPerChunk - first);
                    int secBase = id * g.sectionsPerChunk + first;
                    int at = metadataBase + word;
                    long kinds = chunkKinds[at];
                    long active = chunkActiveDirty[at] & 0xffffffffL;

                    if (kinds == uniformKinds(N) && chunkSourceMask[at] == 0L) {
                        long[] masks = decayMasks;
                        UniformKernels.decay(
                                g.uniformRads,
                                secBase,
                                N,
                                retentionDt,
                                g.minBound,
                                fogRad,
                                g.destroyRad,
                                masks);
                        if (masks[0] != 0L) dirty = true;
                        chunkActiveDirty[at] &= ~(masks[1] & active);
                        for (long m = masks[2] & active; m != 0L; m &= m - 1L) {
                            int sy = Long.numberOfTrailingZeros(m);
                            g.maybeQueueFog(
                                    SectionKeys.setSectionY(baseSck, minSectionY + first + sy),
                                    0,
                                    first + sy);
                        }
                        for (long m = masks[3] & active; m != 0L; m &= m - 1L) {
                            int sy = Long.numberOfTrailingZeros(m);
                            g.maybeQueueDestroy(
                                    SectionKeys.setSectionY(baseSck, minSectionY + first + sy),
                                    0,
                                    first + sy);
                        }
                        continue;
                    }

                    long visit = active | chunkSourceMask[at];
                    for (int sy = 0; sy < N; sy++) {
                        if ((visit & (1L << sy)) == 0L) continue;

                        int secIdx = secBase + sy;
                        SectionSources src = g.sectionSources[secIdx];
                        int kind = (int) ((kinds >>> (sy << 1)) & 3);

                        long sck = SectionKeys.setSectionY(baseSck, minSectionY + first + sy);

                        if (kind < KIND_MULTI) {
                            double prev = g.uniformRads[secIdx];
                            double next =
                                    g.sanitize(
                                            (src == null ? prev : src.relax(0, prev))
                                                    * retentionDt);
                            if (next != prev) {
                                g.uniformRads[secIdx] = next;
                                dirty = true;
                            }
                            if (next == 0.0D && src == null) {
                                chunkActiveDirty[at] &= ~(1L << sy);
                            } else if (next != 0.0D) {
                                if (next > fogRad) g.maybeQueueFog(sck, 0, first + sy);
                                if (next >= g.destroyRad) g.maybeQueueDestroy(sck, 0, first + sy);
                            }
                        } else {
                            MultiSectionRef multi = (MultiSectionRef) g.complexSecs[secIdx];
                            int pCount = multi.pocketCount & 0xFFFF;
                            boolean anyAlive = false;

                            for (int p = 0; p < pCount; p++) {
                                int dataIdx = p << 1;
                                double prev = multi.data[dataIdx];
                                if (prev == 0.0D && src == null) continue;

                                double next =
                                        g.sanitize(
                                                (src == null ? prev : src.relax(p, prev))
                                                        * retentionDt);
                                if (next != prev) {
                                    multi.data[dataIdx] = next;
                                    dirty = true;
                                }

                                if (next != 0.0D) {
                                    anyAlive = true;
                                    if (next > fogRad) g.maybeQueueFog(sck, p, first + sy);
                                    if (next >= g.destroyRad) g.maybeQueueDestroy(sck, p, first + sy);
                                }
                            }

                            if (!anyAlive && src == null) {
                                chunkActiveDirty[at] &= ~(1L << sy);
                            }
                        }
                    }
                }
                if (dirty) chunkActiveDirty[metadataBase] |= CHUNK_DIRTY_MASK;
            }
        }
    }
}
