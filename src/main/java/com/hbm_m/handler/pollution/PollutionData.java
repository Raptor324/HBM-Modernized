package com.hbm_m.handler.pollution;

import java.util.Locale;

import com.hbm_m.inventory.fluid.trait.PollutionType;

import net.minecraft.nbt.CompoundTag;

/**
 * 1:1-Port von {@code PollutionHandler.PollutionData} (1.7.10): die Verschmutzungswerte einer
 * Rasterzelle, ein Wert je {@link PollutionType}.
 */
public class PollutionData {

    public final float[] pollution = new float[PollutionType.values().length];

    /** Original: die NBT-Schluessel sind die kleingeschriebenen Enum-Namen. */
    private static String key(PollutionType type) {
        return type.name().toLowerCase(Locale.US);
    }

    public static PollutionData fromNBT(CompoundTag nbt) {
        PollutionData data = new PollutionData();
        for (PollutionType type : PollutionType.values()) {
            data.pollution[type.ordinal()] = nbt.getFloat(key(type));
        }
        return data;
    }

    public void toNBT(CompoundTag nbt) {
        for (PollutionType type : PollutionType.values()) {
            nbt.putFloat(key(type), pollution[type.ordinal()]);
        }
    }

    public float get(PollutionType type) {
        return pollution[type.ordinal()];
    }
}
