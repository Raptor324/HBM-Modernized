package com.hbm_m.block.machines.pile;

import net.minecraft.util.StringRepresentable;

/**
 * 1:1-Port der neun Metadaten von {@code BlockPile} (1.7.10), hier als Zustandseigenschaft.
 *
 * <p>Die Reihenfolge entspricht den Metadatenzahlen des Originals ({@code META_DUMMY = 0} bis
 * {@code META_EDGE = 8}), damit sich der Aufbau des Meilers Zeile fuer Zeile vergleichen laesst.</p>
 */
public enum PileBlockType implements StringRepresentable {

    /** Blindblock - daraus besteht der Meiler groesstenteils. */
    DUMMY("dummy"),
    /** Der Kern; genau einer je Meiler, er traegt die Rechnung. */
    CORE("core"),
    /** Mittelstueck eines Kanals, gegen Kreuzungen. */
    CHANNEL("channel"),
    /** Anfang eines Brennstoffkanals. */
    FUEL_IN("fuel_in"),
    /** Ende eines Brennstoffkanals. */
    FUEL_OUT("fuel_out"),
    /** Anfang eines Lueftungskanals. */
    AIR_IN("air_in"),
    /** Ende eines Lueftungskanals. */
    AIR_OUT("air_out"),
    /** Steuerstabkanal - Anfang und Ende sehen gleich aus. */
    CONTROL("control"),
    /** Kante des Wuerfels; hier darf nicht gebohrt werden. */
    EDGE("edge");

    private final String name;

    PileBlockType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
