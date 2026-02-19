package com.hbm_m.client.model.variant;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Определение скина (текстуры) для двери.
 * Применяется только к MODERN модели.
 * Отображаемое имя берётся из локализации: door.skin.{modid}.{doorId}.{skinId}
 * 
 * @author HBM-M Team
 */
public class DoorSkin {
    
    /**
     * Скин по умолчанию (для LEGACY и для MODERN без кастомного скина)
     */
    public static final DoorSkin DEFAULT = new DoorSkin("default", null, null);
    
    private final String id;
    private final ResourceLocation texturePath;
    private final ResourceLocation modelPath;
    
    public DoorSkin(String id, ResourceLocation texturePath) {
        this(id, texturePath, null);
    }
    
    public DoorSkin(String id, ResourceLocation texturePath, ResourceLocation modelPath) {
        this.id = id;
        this.texturePath = texturePath;
        this.modelPath = modelPath;
    }
    
    /**
     * ID скина для сохранения
     */
    public String getId() {
        return id;
    }
    
    /**
     * Отображаемое имя в UI — через локализацию door.skin.{modid}.{doorId}.{skinId}
     */
    public Component getDisplayName(String doorId) {
        String key = "door.skin." + RefStrings.MODID + "." + doorId + "." + id;
        return Component.translatable(key);
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
     * Путь к модели (если есть — для скинов с отдельной текстурой).
     * null = использовать базовую модель modern.
     */
    public ResourceLocation getModelPath() {
        return modelPath;
    }

    /**
     * Создаёт скин из JSON. Поле "name" игнорируется — отображение через локализацию.
     */
    public static DoorSkin fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        
        ResourceLocation texturePath = null;
        if (json.has("texture")) {
            texturePath = ResourceLocation.parse(json.get("texture").getAsString());
        }
        
        ResourceLocation modelPath = null;
        if (json.has("model")) {
            modelPath = ResourceLocation.parse(json.get("model").getAsString());
        }
        
        return new DoorSkin(id, texturePath, modelPath);
    }
    
    /**
     * Создаёт простой скин по ID (имя — через локализацию)
     */
    public static DoorSkin of(String id) {
        return new DoorSkin(id, null, null);
    }
    
    /**
     * Создаёт скин с текстурой
     */
    public static DoorSkin of(String id, String texturePath) {
        return new DoorSkin(id, texturePath != null ? ResourceLocation.parse(texturePath) : null, null);
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
        return "DoorSkin{" + id + "}";
    }
}
