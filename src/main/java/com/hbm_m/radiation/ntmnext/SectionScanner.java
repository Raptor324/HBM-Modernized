// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (RadiationSystemNT: scanResistantMask, scanSectionSources,
// scanSectionDiffusivity, harmonicByPocket, SectionMask, SectionSources), Commit 3f9a261a.
//
// Port-Abweichung: das Original liest die gepackten Palettenwoerter direkt
// (PalettedContainer.data, BitStorage.getRaw) ueber Access-Widener. 1.20.1 bietet dafuer keine
// oeffentliche API; der Port nutzt maybeHas() als schnellen Vorfilter, count() (Paletten-
// Histogramm) fuer pocketfreie Sektionen und sonst get(x,y,z) je Block. Ergebnis identisch,
// nur die Summationsreihenfolge im Diffusivitaetsmittel kann minimal abweichen.

package com.hbm_m.radiation.ntmnext;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

/** Liest Abschirmung, Quellen und Diffusivitaet einer Chunk-Sektion aus. */
final class SectionScanner {

    static final int SECTION_BLOCK_COUNT = 4096;

    private static final ThreadLocal<double[]> TL_DIFF_RECIPROCALS =
            ThreadLocal.withInitial(() -> new double[NtmRadiationSystem.MAX_POCKETS]);

    private SectionScanner() {}

    // ------------------------------------------------------------------
    // Abschirmung
    // ------------------------------------------------------------------

    /** Bitmaske der abschirmenden Bloecke, {@code null} wenn keine vorhanden. */
    static @Nullable SectionMask scanResistantMask(@Nullable LevelChunkSection section) {
        if (section == null || section.hasOnlyAir()) return null;
        if (RadiationShielding.RAD_RESISTANT_STATES.isEmpty()) return null;
        PalettedContainer<BlockState> container = section.getStates();
        if (!container.maybeHas(RadiationShielding.RAD_RESISTANT_STATES::contains)) return null;

        SectionMask result = null;
        int count = 0;
        for (int i = 0; i < SECTION_BLOCK_COUNT; i++) {
            BlockState state = container.get(i & 15, i >>> 8, (i >>> 4) & 15);
            if (!RadiationShielding.RAD_RESISTANT_STATES.contains(state)) continue;
            if (result == null) result = new SectionMask();
            result.set(i);
            count++;
        }
        if (count == SECTION_BLOCK_COUNT) return SectionMask.ALL_SET;
        return result;
    }

    // ------------------------------------------------------------------
    // Konstante Quellen
    // ------------------------------------------------------------------

    static @Nullable SectionSources scanSectionSources(
            RadiationType type,
            @Nullable LevelChunkSection section,
            @Nullable short[] pocketData,
            int pocketCount,
            int[] pocketVolumes) {
        if (RadiationSources.RAD_SOURCE_STATES.isEmpty()
                || section == null
                || section.hasOnlyAir()) return null;
        PalettedContainer<BlockState> container = section.getStates();
        if (!container.maybeHas(RadiationSources.RAD_SOURCE_STATES::containsKey)) return null;

        SectionSources out = null;
        for (int local = 0; local < SECTION_BLOCK_COUNT; local++) {
            BlockState state = container.get(local & 15, local >>> 8, (local >>> 4) & 15);
            RadiationSources.RadSource source = RadiationSources.RAD_SOURCE_STATES.get(state);
            if (source == null) continue;
            int pi = pocketData == null ? 0 : pocketData[local];
            if (pi < 0 || pi >= pocketCount) continue;
            // Stufe 2: nur der Anteil dieser Strahlungsart am Mix des Quellblocks.
            float share = SourceMixTable.mixFor(state)[type.ordinal()];
            if (!(share > 0.0F)) continue;
            if (out == null) out = new SectionSources();
            out.add(pi, source, pocketVolumes, share);
        }
        return out == null || out.size == 0 ? null : out;
    }

    // ------------------------------------------------------------------
    // Diffusivitaet (harmonisches Mittel je Pocket)
    // ------------------------------------------------------------------

    static float scanSectionDiffusivity(
            RadiationType type,
            @Nullable LevelChunkSection section,
            @Nullable short[] pocketData,
            int pocketCount,
            int[] pocketVolumes,
            @Nullable float[] out) {
        if (pocketCount == 0) return RadiationDiffusivity.NEUTRAL;
        if (RadiationDiffusivity.isTrivial() || section == null || section.hasOnlyAir()) {
            if (out != null) Arrays.fill(out, 0, pocketCount, RadiationDiffusivity.NEUTRAL);
            return RadiationDiffusivity.NEUTRAL;
        }
        PalettedContainer<BlockState> container = section.getStates();
        if (!container.maybeHas(s -> RadiationDiffusivity.of(type, s) != RadiationDiffusivity.NEUTRAL)) {
            if (out != null) Arrays.fill(out, 0, pocketCount, RadiationDiffusivity.NEUTRAL);
            return RadiationDiffusivity.NEUTRAL;
        }

        if (pocketData == null) {
            // Ganze Sektion ist eine Pocket: Histogramm statt 4096 Einzelzugriffen.
            double[] reciprocal = {0.0D};
            container.count(
                    (state, n) -> {
                        float d =
                                state == null
                                        ? RadiationDiffusivity.NEUTRAL
                                        : RadiationDiffusivity.of(type, state);
                        reciprocal[0] += n * (1.0D / d);
                    });
            float value =
                    RadiationDiffusivity.clamp((float) (SECTION_BLOCK_COUNT / reciprocal[0]));
            if (out != null) {
                Arrays.fill(out, 0, pocketCount, RadiationDiffusivity.NEUTRAL);
                out[0] = value;
            }
            return value;
        }

        double[] recip = TL_DIFF_RECIPROCALS.get();
        Arrays.fill(recip, 0, pocketCount, 0.0D);
        for (int local = 0; local < SECTION_BLOCK_COUNT; local++) {
            int pi = pocketData[local];
            if (pi < 0 || pi >= pocketCount) continue;
            BlockState state = container.get(local & 15, local >>> 8, (local >>> 4) & 15);
            recip[pi] += 1.0D / RadiationDiffusivity.of(type, state);
        }
        float first = RadiationDiffusivity.NEUTRAL;
        for (int p = 0; p < pocketCount; p++) {
            float value;
            if (!(recip[p] > 0.0D)) {
                value = RadiationDiffusivity.NEUTRAL;
            } else {
                value = RadiationDiffusivity.clamp((float) (pocketVolumes[p] / recip[p]));
            }
            if (out != null) out[p] = value;
            if (p == 0) first = value;
        }
        return first;
    }

    // ------------------------------------------------------------------
    // Hilfsstrukturen
    // ------------------------------------------------------------------

    /** 4096-Bit-Maske ueber die Bloecke einer Sektion. */
    static final class SectionMask {
        static final SectionMask ALL_SET = new SectionMask();

        static {
            Arrays.fill(ALL_SET.words, -1L);
        }

        final long[] words = new long[64];

        boolean get(int bit) {
            int w = bit >>> 6;
            return (words[w] & (1L << (bit & 63))) != 0L;
        }

        void set(int bit) {
            int w = bit >>> 6;
            words[w] |= (1L << (bit & 63));
        }

        boolean isEmpty() {
            for (long w : words) if (w != 0L) return false;
            return true;
        }
    }

    /** Zusammengefasste konstante Quellen einer Sektion, je Pocket. */
    static final class SectionSources {
        short[] pocket = new short[4];
        double[] emission = new double[4];
        double[] saturation = new double[4];
        int[] count = new int[4];
        int size;

        void add(int p, RadiationSources.RadSource source, int[] pocketVolumes, float share) {
            // Emission wird auf das Pocketvolumen normiert (Dichte statt Masse).
            double scale =
                    share * (double) SECTION_BLOCK_COUNT / (double) Math.max(1, pocketVolumes[p]);
            double e = source.emission() * scale;
            double s = source.saturation() == 0.0D ? 0.0D : source.saturation() * scale;
            add(p, e, s);
        }

        void add(int p, double e, double s) {
            for (int i = 0; i < size; i++) {
                if (pocket[i] == p && emission[i] == e && saturation[i] == s) {
                    count[i]++;
                    return;
                }
            }
            if (size == pocket.length) {
                int n = size * 2;
                pocket = Arrays.copyOf(pocket, n);
                emission = Arrays.copyOf(emission, n);
                saturation = Arrays.copyOf(saturation, n);
                count = Arrays.copyOf(count, n);
            }
            pocket[size] = (short) p;
            emission[size] = e;
            saturation[size] = s;
            count[size] = 1;
            size++;
        }

        /** Ein Zeitschritt Emission fuer Pocket {@code p} bei aktueller Dichte {@code c}. */
        double relax(int p, double c) {
            double weight = 0.0D;
            double numerator = 0.0D;
            double add = 0.0D;
            for (int i = 0; i < size; i++) {
                if (pocket[i] != p) continue;
                double e = emission[i];
                double sat = saturation[i];
                if (sat == 0.0D) {
                    add += count[i] * e;
                    continue;
                }
                if (!Emission.active(e, sat, c)) continue;
                weight += count[i] * Emission.weight(e, sat);
                numerator += count[i] * Emission.numeratorTerm(e, sat);
            }
            return Emission.relax(c, weight, numerator) + add;
        }
    }
}
