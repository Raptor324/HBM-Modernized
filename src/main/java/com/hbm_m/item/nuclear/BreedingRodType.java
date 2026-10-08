package com.hbm_m.item.nuclear;

import java.util.Locale;

/**
 * 1:1 {@code ItemBreedingRod.BreedingRodType}. Im Port ist jede Metadatenstufe ein eigenes Item
 * ({@code rod_<typ>}, {@code rod_dual_<typ>}, {@code rod_quad_<typ>}), Reihenfolge = Original-Metadaten.
 */
public enum BreedingRodType {
    LITHIUM,
    TRITIUM,
    CO,
    CO60,
    TH232,
    THF,
    U235,
    NP237,
    U238,
    PU238,
    PU239,
    RGP,
    WASTE,

    //Required for prototype
    LEAD,
    URANIUM,

    RA226,
    AC227;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
