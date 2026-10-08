// Port-eigene Klasse (HBM-Modernized), Stufe 2 des NTM-Next-Strahlungssystems.

package com.hbm_m.radiation.ntmnext;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Strahlungsarten. GAMMA, NEUTRON und BETA haben je ein eigenes Diffusionsfeld (eigene
 * WorldRadiationData je Dimension). ALPHA hat kein Feld: Alpha-Anteile im Quellen-Mix werden in
 * Stufe 2 verworfen und sind fuer die Kontaminationsebene (Stufe 3) reserviert.
 */
public enum RadiationType {
    GAMMA(true),
    NEUTRON(true),
    BETA(true),
    ALPHA(false);

    /** Arten mit eigenem Feld, Index = ordinal(). */
    public static final RadiationType[] FIELD_TYPES = {GAMMA, NEUTRON, BETA};

    public static final int FIELD_COUNT = FIELD_TYPES.length;

    /** Alle Arten inkl. ALPHA (Laenge der Mix-Arrays). */
    public static final int MIX_COUNT = values().length;

    public final boolean hasField;
    public final String id;

    RadiationType(boolean hasField) {
        this.hasField = hasField;
        this.id = name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static RadiationType byId(String id) {
        for (RadiationType t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
