// Port-eigene Klasse (HBM-Modernized), Stufe 2 des NTM-Next-Strahlungssystems.

package com.hbm_m.radiation.ntmnext;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Strahlungsarten-Mix je Quellblock (Anteile gamma, neutron, beta, alpha; Summe wird normiert).
 *
 * <p>Die bestehenden Quellen des Ports rufen {@code incrementRad(level, x, y, z, rad)} an ihrer
 * eigenen Blockposition auf (BlockHazard, RBMK-Staebe, Reaktoren ...). Der Mix wird deshalb aus
 * dem Block an der Emissionsposition abgeleitet ({@code data/<ns>/ntm_radiation/source_mix/}),
 * ohne dass die Aufrufer geaendert werden muessen. Unbekannte Bloecke und Emissionen in Luft
 * (Explosionen, Fallout-Regen) nutzen {@link NtmRadiationConfig#defaultMix}.
 */
public final class SourceMixTable {

    private static final Reference2ObjectOpenHashMap<Block, float[]> BY_BLOCK =
            new Reference2ObjectOpenHashMap<>();
    private static final Reference2IntOpenHashMap<Block> PRIORITY = new Reference2IntOpenHashMap<>();
    /** Stufe 3: Aufteilung auf Kontaminationsgruppen je Block (sonst Fallout-Aufteilung). */
    private static final Reference2ObjectOpenHashMap<Block, float[]> CONTAMINATION =
            new Reference2ObjectOpenHashMap<>();

    private SourceMixTable() {}

    static void clear() {
        BY_BLOCK.clear();
        PRIORITY.clear();
        CONTAMINATION.clear();
    }

    static void put(Block block, float[] mix, int priority) {
        if (PRIORITY.containsKey(block) && PRIORITY.getInt(block) > priority) return;
        BY_BLOCK.put(block, normalize(mix));
        PRIORITY.put(block, priority);
    }

    /** Normiert auf Summe 1 (bei Summe 0: Standard-Mix). */
    static float[] normalize(float[] mix) {
        float sum = 0.0F;
        for (float v : mix) sum += Math.max(0.0F, v);
        if (!(sum > 0.0F)) return NtmRadiationConfig.defaultMix.clone();
        float[] out = new float[RadiationType.MIX_COUNT];
        for (int i = 0; i < out.length && i < mix.length; i++) out[i] = Math.max(0.0F, mix[i]) / sum;
        return out;
    }

    /** Mix fuer den Block an der Emissionsposition. Nicht veraendern. */
    public static float[] mixFor(BlockState state) {
        float[] mix = BY_BLOCK.get(state.getBlock());
        return mix != null ? mix : NtmRadiationConfig.defaultMix;
    }

    /** Mix aus genau einer Art (fuer die typisierte API). */
    public static float[] single(RadiationType type) {
        float[] mix = new float[RadiationType.MIX_COUNT];
        mix[type.ordinal()] = 1.0F;
        return mix;
    }

    static void putContamination(Block block, float[] split) {
        float sum = 0.0F;
        for (float v : split) sum += Math.max(0.0F, v);
        if (!(sum > 0.0F)) return;
        float[] out = new float[ContaminationGroup.COUNT];
        for (int i = 0; i < out.length && i < split.length; i++) out[i] = Math.max(0.0F, split[i]) / sum;
        CONTAMINATION.put(block, out);
    }

    /** Hat der Block einen eigenen Mix-Eintrag (= bekannter Quellblock)? */
    static boolean hasEntry(BlockState state) {
        return BY_BLOCK.containsKey(state.getBlock());
    }

    /** Kontaminationsgruppen-Aufteilung fuer Emission an diesem Block. Nicht veraendern. */
    static float[] contaminationSplitFor(BlockState state) {
        float[] split = CONTAMINATION.get(state.getBlock());
        return split != null ? split : NtmRadiationConfig.falloutSplit;
    }

    static int size() {
        return BY_BLOCK.size();
    }
}
