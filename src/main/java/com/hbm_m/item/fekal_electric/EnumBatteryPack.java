// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm_m.item.fekal_electric;

/**
 * Тиры больших батарей-паков (бэкпорт оригинального EnumBatteryPack).
 * Батареи держат заряд 15 минут под номинальной нагрузкой, конденсаторы — 30 секунд.
 * Хранят энергию в NBT предмета (ModBatteryItem), поэтому номинал — только константы.
 */
public enum EnumBatteryPack {
    BATTERY_REDSTONE("battery_redstone", 100L, false),
    BATTERY_LEAD("battery_lead", 1_000L, false),
    BATTERY_LITHIUM("battery_lithium", 10_000L, false),
    BATTERY_SODIUM("battery_sodium", 50_000L, false),
    BATTERY_SCHRABIDIUM("battery_schrabidium", 250_000L, false),
    BATTERY_QUANTUM("battery_quantum", 1_000_000L, 60 * 1200L),

    CAPACITOR_COPPER("capacitor_copper", 1_000L, true),
    CAPACITOR_GOLD("capacitor_gold", 10_000L, true),
    CAPACITOR_NIOBIUM("capacitor_niobium", 100_000L, true),
    CAPACITOR_TANTALUM("capacitor_tantalum", 500_000L, true),
    CAPACITOR_BISMUTH("capacitor_bismuth", 2_500_000L, true),
    CAPACITOR_SPARK("capacitor_spark", 10_000_000L, true);

    public static final int TICKS_PER_SECOND = 20;
    public static final int TICKS_PER_MINUTE = 20 * 60;

    public static final EnumBatteryPack[] VALUES = values();

    /** Базовое имя текстуры: textures/block/machine/<tex>.png — совпадает с рендером сокета. */
    public final String tex;
    public final long capacity;
    public final long chargeRate;
    public final long dischargeRate;
    public final String id = name().toLowerCase(java.util.Locale.ROOT);

    EnumBatteryPack(String tex, long dischargeRate, boolean capacitor) {
        this(
                tex,
                capacitor
                        ? dischargeRate * TICKS_PER_SECOND * 30
                        : dischargeRate * TICKS_PER_MINUTE * 15,
                capacitor ? dischargeRate : dischargeRate * 10,
                dischargeRate);
    }

    EnumBatteryPack(String tex, long dischargeRate, long duration) {
        this(tex, dischargeRate * duration, dischargeRate * 10, dischargeRate);
    }

    EnumBatteryPack(String tex, long capacity, long chargeRate, long dischargeRate) {
        this.tex = tex;
        this.capacity = capacity;
        this.chargeRate = chargeRate;
        this.dischargeRate = dischargeRate;
    }

    public boolean isCapacitor() {
        return this.ordinal() > BATTERY_QUANTUM.ordinal();
    }
}
