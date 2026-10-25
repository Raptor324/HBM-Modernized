// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.config.RadiationConfig + Gruppe "radiation" aus
// com.hbm.data.RadiationData), Commit 3f9a261a.
//
// Im Original kommen die Werte aus dem Datapack-Registry "hbm:radiation_config". Im Port liest
// NtmRadiationDataLoader beim Serverstart data/<ns>/ntm_radiation/config.json und setzt die
// Felder hier. Die Hauptschalter des Ports (enableRadiation/enableChunkRads/worldRadEffects/
// enableDebugLogging aus ModClothConfig) werden zusaetzlich beachtet.

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.config.ModClothConfig;

/** Globale Parameter des NTM-Next-Strahlungssystems (Vorgaben = Original). */
public final class NtmRadiationConfig {

    /** Ab dieser Dichte (RAD) kann radioaktiver Nebel entstehen. */
    static double fogRad = 100D;
    /** Mittlere Sekunden zwischen Nebelpartikeln pro aktiver Pocket (>= 1). */
    static double fogChance = 20D;
    /** Weltzerstoerung (Gras -> Oedland, Blaetter -> tote Blaetter) aktiv. */
    static boolean worldRadEffects = true;
    /** Datapack-Schalter fuer das gesamte Chunk-Strahlungsfeld. */
    static boolean enableChunkRads = true;
    /** Diffusionskoeffizient (Bloecke^2/s), > 0. */
    static double diffusivity = 10D;
    /** Halbwertszeit des Feldes in Sekunden, > 0. */
    static double halfLifeSeconds = 120D;
    /** Simulationsschritt alle n Server-Ticks (Original: RadiationConfig.radTickRate). */
    static int radTickRate = 1;

    // ------------------------------------------------------------------
    // Stufe 2: Strahlungsarten (Index = RadiationType.ordinal(), nur Feldarten)
    // ------------------------------------------------------------------

    /** Gewicht je Art in der RAD/s-Summe fuer Spieler/Geigerzaehler (Standard 1 = HBM-Skala). */
    static final double[] typeWeight = {1.0D, 1.0D, 1.0D};
    /**
     * Faktor auf die Halbwertszeit je Art. Neutronen gibt es nur, solange die Quelle aktiv ist
     * (Standard 0.1 -> 12 s), Gamma/Beta aus Fallout bleiben laenger (1.0 -> 120 s).
     */
    static final double[] halfLifeMult = {1.0D, 0.1D, 1.0D};
    /** Faktor auf den Diffusionskoeffizienten je Art. */
    static final double[] diffusivityMult = {1.0D, 1.0D, 1.0D};
    /** Faktor auf alle Halbwertsschichten der Abschirmtabelle je Art (>1 = schwaechere Abschirmung). */
    static final double[] hvlScale = {1.0D, 1.0D, 1.0D};
    /** Standard-Mix (gamma, neutron, beta, alpha) fuer Quellen ohne eigenen Eintrag. */
    static final float[] defaultMix = {0.6F, 0.1F, 0.3F, 0.0F};
    /**
     * Anteil der Schwellen (Nebel 100 RAD, Zerstoerung 5 RAD), den eine einzelne Art (gewichtet)
     * erreichen muss. 0.5 = eine Art mit halber Schwelle genuegt (Summe ~ wie Stufe 1).
     */
    static double effectShare = 0.5D;

    // Nahfeld (Abstandsquadratgesetz + Beer-Lambert entlang des Strahls)
    static boolean nearEnabled = true;
    /** Reichweite in Bloecken; am Rand faellt der Beitrag auf 0. */
    static int nearRadius = 8;
    /** Verstaerkung: Beitrag = gain * Quellrate * T / d^2 (abzgl. Randwert). */
    static double nearGain = 8.0D;
    /** Hoechstens so viele staerkste Quellen je Abfrage. */
    static int nearMaxEmitters = 32;
    /** Cache-Dauer je Abfrageposition in Ticks. */
    static int nearCacheTicks = 10;

    /**
     * Stufe 3: Laengenskala der Wandwirkung im Feld. 16 = eine einzelne Schicht in einer Sektion
     * daempft den Durchfluss genau um 2^(-1/HVL); kleiner = Waende wirken schwaecher.
     */
    static double wallScale = 16.0D;

    // ------------------------------------------------------------------
    // Stufe 3: Kontamination (Index = ContaminationGroup.ordinal())
    // ------------------------------------------------------------------

    static boolean contaminationEnabled = true;
    /** Halbwertszeiten in Sekunden: SHORT 1 Spieltag, MEDIUM 14 Spieltage, LONG ~3 Spieljahre, EXOTIC 100 Tage. */
    static final double[] contamHalfLife = {1200.0D, 16800.0D, 1.0e8D, 120000.0D};
    /** Emission ins Feld je Einheit Kontamination und Sekunde. */
    static final double[] contamEmission = {0.003D, 0.003D, 0.003D, 0.006D};
    /** Arten-Mix (gamma, neutron, beta, alpha) je Gruppe; Alpha geht nicht ins Feld. */
    static final float[][] contamMix = {
        {0.4F, 0.0F, 0.6F, 0.0F},
        {0.5F, 0.0F, 0.5F, 0.0F},
        {0.3F, 0.0F, 0.1F, 0.6F},
        {1.0F, 0.0F, 0.0F, 0.0F}
    };
    /** Anteil einmaliger Emissionen, der als Kontamination liegen bleibt (Rest direkt ins Feld). */
    static double oneOffContaminationFraction = 0.5D;
    /** Aufteilung von Fallout/Luft-Emissionen auf SHORT/MEDIUM/LONG/EXOTIC. */
    static final float[] falloutSplit = {0.70F, 0.25F, 0.05F, 0.0F};
    /** Dekontamination: abgebaute Kontamination je Einheit decrementRad. */
    static double deconFactor = 1.0D;
    /** Kontaminationsschritt alle n Ticks (Emission wird entsprechend hochgerechnet). */
    static int contamTickInterval = 20;

    // ------------------------------------------------------------------
    // Stufe 4: Wetter (Wind, Regen, Wasser) fuer die Kontamination
    // ------------------------------------------------------------------

    /** Anteil der Kontamination aus Luft-Emissionen, der zunaechst luftgetragen ist. */
    static double airborneFraction = 1.0D;
    /** Absinkrate luftgetragener Kontamination (1/s); 1/120 = im Mittel 2 min in der Luft. */
    static double settleRate = 1.0D / 120.0D;

    static boolean windEnabled = true;
    /** Mittlere Windgeschwindigkeit in Bloecken/s. */
    static double windBaseSpeed = 1.5D;
    /** Schwankung der Geschwindigkeit (+/- Bloecke/s). */
    static double windSpeedVariation = 1.0D;
    /** Zeitskala der Richtungsdrift in Sekunden (1200 = ca. ein Spieltag). */
    static double windDirectionPeriod = 1200.0D;
    /** Boeen-Staerke (Bloecke/s), 0 = aus. */
    static double gustStrength = 0.5D;
    static double gustPeriod = 30.0D;
    /** Hoechstens dieser Anteil einer Sektion wandert pro Schritt weiter (Stabilitaet). */
    static double windMaxFraction = 0.5D;

    static boolean rainEnabled = true;
    /** Zusaetzliche Auswaschrate bei Regen (1/s). */
    static double washoutRate = 0.05D;
    /** Auswaschrate bei Schnee (1/s). */
    static double snowWashoutRate = 0.02D;
    /** Faktor bei Gewitter. */
    static double thunderMult = 1.5D;
    /** Abfluss in Senken bei Regen. */
    static boolean runoffEnabled = true;
    /** Anteil Bodenkontamination je Sekunde, der zur tiefsten Nachbar-Oberflaeche fliesst. */
    static double runoffRate = 0.0005D;
    /** Mindest-Hoehenunterschied (Bloecke) fuer Abfluss. */
    static int runoffMinDrop = 2;

    static boolean waterEnabled = true;
    /** Wassertiefe (Bloecke), ab der eine Spalte als Gewaesser gilt. */
    static int waterMinDepth = 2;
    /** Austauschrate zwischen benachbarten Gewaesser-Sektionen (1/s, Richtung Ausgleich). */
    static double waterMixRate = 0.01D;
    /** Verduennung im Ozean-Biom: Halbwertszeit in Sekunden (2400 = 2 Spieltage). */
    static double oceanDilutionHalfLife = 2400.0D;

    private NtmRadiationConfig() {}

    static void resetDefaults() {
        fogRad = 100D;
        fogChance = 20D;
        worldRadEffects = true;
        enableChunkRads = true;
        diffusivity = 10D;
        halfLifeSeconds = 120D;
        radTickRate = 1;
        typeWeight[0] = 1.0D;
        typeWeight[1] = 1.0D;
        typeWeight[2] = 1.0D;
        halfLifeMult[0] = 1.0D;
        halfLifeMult[1] = 0.1D;
        halfLifeMult[2] = 1.0D;
        diffusivityMult[0] = 1.0D;
        diffusivityMult[1] = 1.0D;
        diffusivityMult[2] = 1.0D;
        hvlScale[0] = 1.0D;
        hvlScale[1] = 1.0D;
        hvlScale[2] = 1.0D;
        defaultMix[0] = 0.6F;
        defaultMix[1] = 0.1F;
        defaultMix[2] = 0.3F;
        defaultMix[3] = 0.0F;
        effectShare = 0.5D;
        nearEnabled = true;
        nearRadius = 8;
        nearGain = 8.0D;
        nearMaxEmitters = 32;
        nearCacheTicks = 10;
        wallScale = 16.0D;
        contaminationEnabled = true;
        double[] hl = {1200.0D, 16800.0D, 1.0e8D, 120000.0D};
        System.arraycopy(hl, 0, contamHalfLife, 0, 4);
        double[] em = {0.003D, 0.003D, 0.003D, 0.006D};
        System.arraycopy(em, 0, contamEmission, 0, 4);
        float[][] mix = {
            {0.4F, 0.0F, 0.6F, 0.0F},
            {0.5F, 0.0F, 0.5F, 0.0F},
            {0.3F, 0.0F, 0.1F, 0.6F},
            {1.0F, 0.0F, 0.0F, 0.0F}
        };
        for (int i = 0; i < 4; i++) System.arraycopy(mix[i], 0, contamMix[i], 0, 4);
        oneOffContaminationFraction = 0.5D;
        float[] split = {0.70F, 0.25F, 0.05F, 0.0F};
        System.arraycopy(split, 0, falloutSplit, 0, 4);
        deconFactor = 1.0D;
        contamTickInterval = 20;
        airborneFraction = 1.0D;
        settleRate = 1.0D / 120.0D;
        windEnabled = true;
        windBaseSpeed = 1.5D;
        windSpeedVariation = 1.0D;
        windDirectionPeriod = 1200.0D;
        gustStrength = 0.5D;
        gustPeriod = 30.0D;
        windMaxFraction = 0.5D;
        rainEnabled = true;
        washoutRate = 0.05D;
        snowWashoutRate = 0.02D;
        thunderMult = 1.5D;
        runoffEnabled = true;
        runoffRate = 0.0005D;
        runoffMinDrop = 2;
        waterEnabled = true;
        waterMinDepth = 2;
        waterMixRate = 0.01D;
        oceanDilutionHalfLife = 2400.0D;
    }

    public static double typeWeight(RadiationType type) {
        return type.hasField ? typeWeight[type.ordinal()] : 0.0D;
    }

    /** Entspricht RadiationData.ENABLE_CHUNK_RADS, zusaetzlich die Port-Hauptschalter. */
    static boolean chunkRadsEnabled() {
        ModClothConfig c = ModClothConfig.get();
        return enableChunkRads && c.enableRadiation && c.enableChunkRads;
    }

    /** Entspricht RadiationData.WORLD_RAD_EFFECTS, zusaetzlich Port-Schalter worldRadEffects. */
    static boolean worldEffectsEnabled() {
        return worldRadEffects && ModClothConfig.get().worldRadEffects;
    }

    /** Entspricht RadiationConfig.enableDebugMode (Profiling-Ausgaben im Log). */
    static boolean debug() {
        return ModClothConfig.get().enableDebugLogging;
    }

    public static double fogRad() {
        return fogRad;
    }

    public static double diffusivity() {
        return diffusivity;
    }

    public static double halfLifeSeconds() {
        return halfLifeSeconds;
    }

    public static int radTickRate() {
        return radTickRate;
    }
}
