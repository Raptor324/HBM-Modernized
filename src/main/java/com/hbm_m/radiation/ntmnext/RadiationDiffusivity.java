// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.handler.radiation.RadiationDiffusivity), Commit 3f9a261a.
// Stufe 2: die Block-Diffusivitaet kommt je Strahlungsart aus RadiationShieldingTable
// (Halbwertsschichten, D = exp(-mu)); die JSON-Gruppe "rad_diffusivity" des Originals ist
// darin aufgegangen. Kanten-/Zerfallsformeln unveraendert.

package com.hbm_m.radiation.ntmnext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Blockabhaengige Diffusivitaet je Strahlungsart (1.0 = Luft). */
public final class RadiationDiffusivity {

    public static final float NEUTRAL = 1.0F;

    public static final float MIN = 0.01F, MAX = 1.50F;

    private RadiationDiffusivity() {}

    static float of(RadiationType type, BlockState state) {
        return RadiationShieldingTable.diffusivity(type.ordinal(), state);
    }

    static boolean isTrivial() {
        return !RadiationShieldingTable.isLoaded();
    }

    static boolean transportEnabled(ServerLevel level) {
        return RadiationSettings.forLevel(level).diffusivityTransport().orElse(!isTrivial());
    }

    static float clamp(float value) {
        if (!Float.isFinite(value)) return MIN;
        return Math.min(Math.max(value, MIN), MAX);
    }

    /** Effektive Kanten-Diffusivitaet zweier Zellen (Reihenschaltung, harmonisch gewichtet). */
    static double edge(double lenA, double diffA, double lenB, double diffB) {
        if (!(diffA > 0.0D) || !(diffB > 0.0D)) return 0.0D;
        double denom = (lenA / diffA) + (lenB / diffB);
        double lenSum = lenA + lenB;
        if (!(denom > 0.0D)
                || !(lenSum > 0.0D)
                || !Double.isFinite(denom)
                || !Double.isFinite(lenSum)) {
            return 0.0D;
        }
        return lenSum / denom;
    }

    static double decay(double k) {
        if (!(k > 0.0D) || !Double.isFinite(k)) return 1.0D;
        if (k >= 700.0D) return 0.0D;
        return Math.exp(-k);
    }
}
