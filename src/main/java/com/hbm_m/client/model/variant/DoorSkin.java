package com.hbm_m.client.model.variant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
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
    
    public static final DoorSkin DEFAULT = new DoorSkin("default", null, null, Collections.emptyMap());
    
    private final String id;
    private final ResourceLocation texturePath; // Базовая текстура (оставлена для обратной совместимости)
    private final ResourceLocation modelPath;
    private final Map<String, ResourceLocation> textureMap; // Словарь текстур для составных моделей
    
    public DoorSkin(String id, ResourceLocation texturePath, ResourceLocation modelPath, Map<String, ResourceLocation> textureMap) {
        this.id = id;
        this.texturePath = texturePath;
        this.modelPath = modelPath;
        this.textureMap = textureMap != null ? textureMap : Collections.emptyMap();
    }
    
    public String getId() {
        return id;
    }
    
    public Component getDisplayName(String doorId) {
        String key = "door.skin." + RefStrings.MODID + "." + doorId + "." + id;
        return Component.translatable(key);
    }
    
    public ResourceLocation getTexturePath() {
        return texturePath;
    }
    
    /**
     * Получить текстуру для конкретной части модели (материала из .mtl)
     * @param materialName имя материала (например "label" или "default")
     */
    public ResourceLocation getTextureForPart(String materialName) {
        if (textureMap.containsKey(materialName)) {
            return textureMap.get(materialName);
        }
        // Если специфичная текстура не найдена, возвращаем базовую
        return texturePath;
    }
    
    public boolean isDefault() {
        return "default".equals(id);
    }
    
    public ResourceLocation getModelPath() {
        return modelPath;
    }

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

        // Парсинг словаря текстур
        Map<String, ResourceLocation> textureMap = new HashMap<>();
        if (json.has("textures")) {
            JsonObject texturesObj = json.getAsJsonObject("textures");
            for (Map.Entry<String, JsonElement> entry : texturesObj.entrySet()) {
                textureMap.put(entry.getKey(), ResourceLocation.parse(entry.getValue().getAsString()));
            }
        }
        
        return new DoorSkin(id, texturePath, modelPath, textureMap);
    }
    
    public static DoorSkin of(String id) {
        return new DoorSkin(id, null, null, Collections.emptyMap());
    }
    
    public static DoorSkin of(String id, String texturePath) {
        return new DoorSkin(id, texturePath != null ? ResourceLocation.parse(texturePath) : null, null, Collections.emptyMap());
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
