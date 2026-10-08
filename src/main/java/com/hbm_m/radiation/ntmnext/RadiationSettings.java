// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.handler.radiation.RadiationSettings), Commit 3f9a261a.
// Original: Datapack-Registry "hbm:radiation_settings" (ein Eintrag je Dimension).
// Port: data/<dim-namespace>/ntm_radiation/settings/<dim-path>.json, geladen von
// NtmRadiationDataLoader.

package com.hbm_m.radiation.ntmnext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** Dimensionsspezifische Ueberschreibungen der globalen Strahlungsparameter. */
public record RadiationSettings(
        Optional<Double> diffusivity,
        Optional<Double> halfLifeSeconds,
        Optional<Double> fogRad,
        Optional<Double> fogChance,
        Optional<Boolean> worldRadEffects,
        Optional<Double> ambientRad,
        Optional<Boolean> diffusivityTransport) {

    public static final RadiationSettings DEFAULT =
            new RadiationSettings(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty());

    /** Vom Loader befuellt, Schluessel = Dimensions-ID. */
    static final Map<ResourceLocation, RadiationSettings> BY_DIMENSION = new HashMap<>();

    /** Aufgeloeste Schrittparameter (je Dimension einmal bei Erzeugung der Weltdaten). */
    public record Resolved(
            double diffusionDt,
            double uuE,
            double retentionDt,
            long fogProbU64,
            double fogRad,
            boolean worldRadEffects,
            double minBound,
            double ambientRad,
            double destroyRad) {}

    public Resolved resolve() {
        return resolve(NtmRadiationSystem.dT, RadiationType.GAMMA);
    }

    /**
     * Schrittparameter fuer eine Strahlungsart (Stufe 2). Diffusion und Halbwertszeit werden mit
     * den Art-Faktoren skaliert; Nebel-/Zerstoerungsschwellen gelten fuer die gewichtete Dichte
     * der Art mit Anteil {@code effect_share}; Hintergrund (ambient_rad) zaehlt als Gamma.
     */
    public Resolved resolve(double dT, RadiationType type) {
        int t = type.ordinal();
        double diffusionDt = diffusivityOrDefault() * NtmRadiationConfig.diffusivityMult[t] * dT;
        double halfLife = halfLifeSecondsOrDefault() * NtmRadiationConfig.halfLifeMult[t];
        double chance = fogChanceOrDefault();
        double weight = NtmRadiationConfig.typeWeight[t];
        double share = NtmRadiationConfig.effectShare;
        double thresholdScale = weight > 0.0D ? share / weight : Double.POSITIVE_INFINITY;
        double ambient = type == RadiationType.GAMMA ? ambientRadOrDefault() : 0.0D;
        return new Resolved(
                diffusionDt,
                Math.exp(-(diffusionDt / 128.0d)),
                Math.exp(Math.log(0.5) * (dT / halfLife)),
                (chance > 0.0D && Double.isFinite(chance))
                        ? NtmRadiationSystem.probU64(dT / chance)
                        : 0L,
                fogRadOrDefault() * thresholdScale,
                worldRadEffectsOrDefault(),
                0.0D - ambient,
                ambient,
                5.0D * thresholdScale);
    }

    public static RadiationSettings forLevel(ServerLevel level) {
        return BY_DIMENSION.getOrDefault(level.dimension().location(), DEFAULT);
    }

    public double diffusivityOrDefault() {
        return diffusivity.orElse(NtmRadiationConfig.diffusivity);
    }

    public double halfLifeSecondsOrDefault() {
        return halfLifeSeconds.orElse(NtmRadiationConfig.halfLifeSeconds);
    }

    public double fogRadOrDefault() {
        return fogRad.orElse(NtmRadiationConfig.fogRad);
    }

    public double fogChanceOrDefault() {
        return fogChance.orElse(NtmRadiationConfig.fogChance);
    }

    public boolean worldRadEffectsOrDefault() {
        return worldRadEffects.orElse(NtmRadiationConfig.worldRadEffects);
    }

    public double ambientRadOrDefault() {
        return ambientRad.orElse(0.0D);
    }
}
