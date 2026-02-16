package com.hbm_m.client.model.variant;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * Определение скина (текстуры) для двери.
 * Применяется только к MODERN модели.
 * 
 * @author HBM-M Team
 */
public class DoorSkin {
    
    /**
     * Скин по умолчанию (для LEGACY и для MODERN без кастомного скина)
     */
    public static final DoorSkin DEFAULT = new DoorSkin("default", "Default", null);
    
    private final String id;
    private final String displayName;
    private final ResourceLocation texturePath;
    
    public DoorSkin(String id, String displayName, ResourceLocation texturePath) {
        this.id = id;
        this.displayName = displayName;
        this.texturePath = texturePath;
    }
    
    /**
     * ID скина для сохранения
     */
    public String getId() {
        return id;
    }
    
    /**
     * Отображаемое имя в UI
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Путь к текстуре (может быть null - используется текстура модели по умолчанию)
     */
    public ResourceLocation getTexturePath() {
        return texturePath;
    }
    
    /**
     * Является ли это скином по умолчанию
     */
    public boolean isDefault() {
        return "default".equals(id);
    }
    
    /**
     * Создаёт скин из JSON
     */
    public static DoorSkin fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String displayName = json.has("name") ? json.get("name").getAsString() : id;
        
        ResourceLocation texturePath = null;
        if (json.has("texture")) {
            texturePath = ResourceLocation.parse(json.get("texture").getAsString());
        }
        
        return new DoorSkin(id, displayName, texturePath);
    }
    
    /**
     * Создаёт простой скин по ID
     */
    public static DoorSkin of(String id, String displayName) {
        return new DoorSkin(id, displayName, null);
    }
    
    /**
     * Создаёт скин с текстурой
     */
    public static DoorSkin of(String id, String displayName, String texturePath) {
        return new DoorSkin(id, displayName, ResourceLocation.parse(texturePath));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DoorSkin other)) return false;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "DoorSkin{" + id + ", display=" + displayName + "}";
    }
}
