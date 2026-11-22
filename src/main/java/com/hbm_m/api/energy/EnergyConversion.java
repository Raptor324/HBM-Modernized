// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm_m.api.energy;

import com.hbm_m.config.ModClothConfig;

/**
 * Конверсия HBM Energy (HE) ↔ Forge Energy (FE).
 * Курс настраивается в конфиге (energyRatioHe / energyRatioFe, по умолчанию 5 HE = 1 FE).
 * feQuantum — минимальный шаг FE, при котором обратная конверсия не теряет доли HE:
 * он равен feRatio / gcd(heRatio, feRatio) и используется для выравнивания запросов.
 * Carry — банковский остаток для потоков, где важен каждый юнит (мост сети).
 */
public final class EnergyConversion {

    private EnergyConversion() {}

    public static long heRatio() {
        ModClothConfig cfg = ModClothConfig.get();
        long he = cfg == null ? 5L : cfg.energyRatioHe;
        return Math.max(1L, he);
    }

    public static long feRatio() {
        ModClothConfig cfg = ModClothConfig.get();
        long fe = cfg == null ? 1L : cfg.energyRatioFe;
        return Math.max(1L, fe);
    }

    public static long feFromHe(long he) {
        return he <= 0 ? 0 : scale(he, feRatio(), heRatio());
    }

    public static long heFromFe(long fe) {
        return fe <= 0 ? 0 : scale(fe, heRatio(), feRatio());
    }

    public static long feQuantum() {
        return feQuantum(heRatio(), feRatio());
    }

    static long feQuantum(long heRatio, long feRatio) {
        return feRatio / gcd(heRatio, feRatio);
    }

    private static long gcd(long x, long y) {
        while (y != 0) {
            long t = x % y;
            x = y;
            y = t;
        }
        return x;
    }

    private static long scale(long value, long mul, long div) {
        if (value > Long.MAX_VALUE / mul) return Long.MAX_VALUE;
        return value * mul / div;
    }

    static long convert(long amount, long mul, long div, long[] carry) {
        if (amount <= 0) return 0;
        long banked = carry[0];
        if (amount > (Long.MAX_VALUE - banked) / mul) return Long.MAX_VALUE;
        long numerator = amount * mul + banked;
        carry[0] = numerator % div;
        return numerator / div;
    }

    /**
     * Накопитель дробных остатков конверсии: недокопанные доли юнита хранятся
     * до следующей операции, поэтому длинные потоки конвертируются без потерь.
     */
    public static final class Carry {

        private long heToFe;
        private long feToHe;

        public long feFromHe(long he) {
            long[] cell = {heToFe};
            long out = convert(he, feRatio(), heRatio(), cell);
            heToFe = cell[0];
            return out;
        }

        public long heFromFe(long fe) {
            long[] cell = {feToHe};
            long out = convert(fe, heRatio(), feRatio(), cell);
            feToHe = cell[0];
            return out;
        }

        public long refundFe(long fe, long maxHe) {
            if (fe <= 0 || maxHe <= 0) return 0;
            long a = heRatio();
            long b = feRatio();
            if (fe > (Long.MAX_VALUE - heToFe) / a) return 0;
            heToFe += fe * a;
            long he = Math.min(heToFe / b, maxHe);
            heToFe -= he * b;
            return he;
        }

        public long heToFeRemainder() {
            return heToFe;
        }

        public long feToHeRemainder() {
            return feToHe;
        }

        public void absorb(Carry other) {
            heToFe += other.heToFe;
            feToHe += other.feToHe;
        }

        public void restore(long heToFe, long feToHe) {
            this.heToFe = heToFe;
            this.feToHe = feToHe;
        }
    }
}
