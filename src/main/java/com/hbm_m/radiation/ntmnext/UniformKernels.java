// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (UniformDecay, UniformDecayScalar, UniformExchange, UniformExchangeScalar),
// Commit 3f9a261a. Nur die Skalar-Varianten (Java-Fallback); die Vector-API-Varianten
// (jdk.incubator.vector) sind bewusst nicht portiert.

package com.hbm_m.radiation.ntmnext;

/** Rechenkerne fuer Chunks, deren Sektionen alle "uniform" (ohne Abschirmung) sind. */
final class UniformKernels {

    static final int CHANGED = 0;
    static final int ZEROED = 1;
    static final int FOG = 2;
    static final int DESTROY = 3;

    private UniformKernels() {}

    /**
     * Zerfall {@code n} uniformer Sektionen ab {@code off}. Ergebnis-Bitmasken in {@code out}
     * (CHANGED, ZEROED, FOG, DESTROY; Bit i = Sektion off+i).
     */
    static void decay(
            double[] uniform,
            int off,
            int n,
            double retention,
            double minBound,
            double fogRad,
            double destroyRad,
            long[] out) {
        long changed = 0L, zeroed = 0L, fog = 0L, destroy = 0L;
        for (int i = 0; i < n; i++) {
            double prev = uniform[off + i];
            double next = NtmRadiationSystem.sanitize(prev * retention, minBound);
            if (next != prev) {
                uniform[off + i] = next;
                changed |= 1L << i;
            }
            if (next == 0.0D) {
                zeroed |= 1L << i;
            } else {
                if (next > fogRad) fog |= 1L << i;
                if (next >= destroyRad) destroy |= 1L << i;
            }
        }
        out[CHANGED] = changed;
        out[ZEROED] = zeroed;
        out[FOG] = fog;
        out[DESTROY] = destroy;
    }

    /** Exakter Austausch zweier uniformer Spalten (X/Z-Nachbarn). Rueckgabe: geaenderte Bits. */
    static long exchange(double[] uniform, int offA, int offB, int n, double uuE) {
        long changed = 0L;
        for (int i = 0; i < n; i++) {
            if (NtmRadiationSystem.exchangeUniExactXZ(uniform, offA + i, offB + i, uuE))
                changed |= 1L << i;
        }
        return changed;
    }
}
