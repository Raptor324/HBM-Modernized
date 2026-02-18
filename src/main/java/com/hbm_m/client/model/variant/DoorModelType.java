package com.hbm_m.client.model.variant;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * Тип модели двери.
 * LEGACY - старая модель (одна текстура)
 * MODERN - новая модель (набор текстур/скинов)
 * 
 * @author HBM-M Team
 */
public enum DoorModelType {
    
    /**
     * Старая модель двери - одна фиксированная текстура
     */
    LEGACY("legacy"),
    
    /**
     * Новая модель двери - поддерживает выбор текстуры/скина
     */
    MODERN("modern");
    
    private final String id;
    
    DoorModelType(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isLegacy() {
        return this == LEGACY;
    }
    
    public boolean isModern() {
        return this == MODERN;
    }
    
    public static DoorModelType fromId(String id) {
        if (id == null || id.isEmpty()) {
            return LEGACY;
        }
        try {
            return valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LEGACY;
        }
    }
}
