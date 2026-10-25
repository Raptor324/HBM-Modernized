// Port-eigene Klasse (HBM-Modernized), Stufe 3 des NTM-Next-Strahlungssystems.

package com.hbm_m.radiation.ntmnext;

import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Isotopengruppen der Kontamination (radioaktives Material am Ort). Halbwertszeiten und Arten-Mix
 * stehen in {@link NtmRadiationConfig} (config.json, Block "contamination").
 * <ul>
 *   <li>SHORT: I-131-artig, Standard 1 Spieltag; frischer Fallout ist anfangs sehr heiss.</li>
 *   <li>MEDIUM: Cs-137/Sr-90-artig, Standard 14 Spieltage.</li>
 *   <li>LONG: Pu/U/Am-artig, praktisch dauerhaft; Hauptquelle der Alpha-Gefahr.</li>
 *   <li>EXOTIC: Schrabidium/Balefire, reines Gamma, doppelte Emission, 100 Spieltage.</li>
 * </ul>
 */
public enum ContaminationGroup {
    SHORT,
    MEDIUM,
    LONG,
    EXOTIC;

    public static final int COUNT = values().length;

    public final String id = name().toLowerCase(Locale.ROOT);

    @Nullable
    public static ContaminationGroup byId(String id) {
        for (ContaminationGroup g : values()) if (g.id.equals(id)) return g;
        return null;
    }
}
