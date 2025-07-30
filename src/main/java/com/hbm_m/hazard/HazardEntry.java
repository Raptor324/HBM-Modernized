package com.hbm_m.hazard;

// В будущем здесь могут быть и модификаторы
public class HazardEntry {
    public final HazardType type;
    public final float baseLevel;

    public HazardEntry(HazardType type, float baseLevel) {
        this.type = type;
        this.baseLevel = baseLevel;
    }
}