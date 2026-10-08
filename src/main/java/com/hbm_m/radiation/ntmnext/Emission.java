// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.handler.radiation.Emission), Commit 3f9a261a.

package com.hbm_m.radiation.ntmnext;

/**
 * Saettigende Quellen-Emission: eine Quelle mit Rate {@code emission} zieht die Dichte einer
 * Pocket gegen {@code saturation}. Mehrere Quellen werden gewichtet relaxiert.
 *
 * <p>Erweiterungspunkt (Stufe 2-4): Strahlungsarten koennen hier eigene Emissionsterme
 * bekommen, ohne den Solver anzufassen.
 */
final class Emission {

    private Emission() {}

    static boolean active(double emission, double saturation, double c) {
        if (emission == 0.0D || saturation == 0.0D) return false;
        return emission * (saturation - c) > 0.0D;
    }

    static double weight(double emission, double saturation) {
        return magnitude(emission) / magnitude(saturation);
    }

    static double numeratorTerm(double emission, double saturation) {
        return saturation < 0.0D ? -magnitude(emission) : magnitude(emission);
    }

    static double relax(double c, double weightSum, double numerator) {
        if (weightSum <= 0.0D) return c;

        if (weightSum > 1.0D) return numerator / weightSum;
        return c + (numerator - c * weightSum);
    }

    static double single(double c, double emission, double saturation) {
        if (!active(emission, saturation, c)) return c;
        return relax(c, weight(emission, saturation), numeratorTerm(emission, saturation));
    }

    private static double magnitude(double v) {
        return v < 0.0D ? -v : v;
    }
}
