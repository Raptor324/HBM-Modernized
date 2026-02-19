package com.hbm_m.client.model.variant;

import com.hbm_m.lib.RefStrings;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

/**
 * Выбор модели двери: тип модели + скин.
 * Сохраняется в NBT и синхронизируется между сервером и клиентом.
 * 
 * @author HBM-M Team
 */
public class DoorModelSelection {
    
    private final DoorModelType modelType;
    private final DoorSkin skin;
    
    /**
     * Выбор по умолчанию: LEGACY модель, стандартный скин
     */
    public static final DoorModelSelection DEFAULT = new DoorModelSelection(DoorModelType.LEGACY, DoorSkin.DEFAULT);
    
    public DoorModelSelection(DoorModelType modelType, DoorSkin skin) {
        this.modelType = Objects.requireNonNull(modelType, "modelType cannot be null");
        this.skin = Objects.requireNonNull(skin, "skin cannot be null");
    }
    
    // ==================== Фабричные методы ====================
    
    /**
     * Создать выбор для LEGACY модели
     */
    public static DoorModelSelection legacy() {
        return new DoorModelSelection(DoorModelType.LEGACY, DoorSkin.DEFAULT);
    }
    
    /**
     * Создать выбор для MODERN модели со скином по умолчанию
     */
    public static DoorModelSelection modern() {
        return new DoorModelSelection(DoorModelType.MODERN, DoorSkin.DEFAULT);
    }
    
    /**
     * Создать выбор для MODERN модели с указанным скином
     */
    public static DoorModelSelection modern(DoorSkin skin) {
        return new DoorModelSelection(DoorModelType.MODERN, skin);
    }
    
    /**
     * Создать выбор для MODERN модели по ID скина
     */
    public static DoorModelSelection modern(String skinId) {
        return new DoorModelSelection(DoorModelType.MODERN, DoorSkin.of(skinId));
    }
    
    // ==================== Геттеры ====================
    
    public DoorModelType getModelType() {
        return modelType;
    }
    
    public DoorSkin getSkin() {
        return skin;
    }
    
    public boolean isLegacy() {
        return modelType.isLegacy();
    }
    
    public boolean isModern() {
        return modelType.isModern();
    }
    
    // ==================== NBT ====================
    
    /**
     * Сохранить выбор в NBT
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putString("modelType", modelType.getId());
        tag.putString("skin", skin.getId());
        return tag;
    }
    
    /**
     * Создать NBT с выбором
     */
    public CompoundTag save() {
        return save(new CompoundTag());
    }
    
    /**
     * Загрузить выбор из NBT
     */
    public static DoorModelSelection load(CompoundTag tag) {
        if (!tag.contains("modelType")) {
            return DEFAULT;
        }
        
        DoorModelType type = DoorModelType.fromId(tag.getString("modelType"));
        String skinId = tag.getString("skin");
        
        // Скин загружаем по ID (полный скин будет получен из реестра)
        DoorSkin skin = skinId.isEmpty() || "default".equals(skinId) 
            ? DoorSkin.DEFAULT 
            : DoorSkin.of(skinId);
        
        return new DoorModelSelection(type, skin);
    }
    
    /**
     * Загрузить выбор из NBT с разрешением скина через реестр
     */
    public static DoorModelSelection load(CompoundTag tag, DoorModelRegistry registry, String doorId) {
        if (!tag.contains("modelType")) {
            return DEFAULT;
        }
        
        DoorModelType type = DoorModelType.fromId(tag.getString("modelType"));
        String skinId = tag.getString("skin");
        
        // Получаем полный скин из реестра
        DoorSkin skin = registry.getSkin(doorId, skinId);
        
        return new DoorModelSelection(type, skin);
    }
    
    // ==================== equals/hashCode/toString ====================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DoorModelSelection other)) return false;
        return modelType == other.modelType && skin.equals(other.skin);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(modelType, skin);
    }
    
    @Override
    public String toString() {
        return "DoorModelSelection{" + modelType.getId() + ", skin=" + skin.getId() + "}";
    }
    
    /**
     * Краткое описание для отображения — через локализацию.
     * LEGACY: door.model_type.{modid}.legacy
     * MODERN: door.skin.{modid}.{doorId}.{skinId}
     */
    public Component getDisplayName(String doorId) {
        if (modelType.isLegacy()) {
            return Component.translatable("door.model_type." + RefStrings.MODID + ".legacy");
        }
        return skin.getDisplayName(doorId);
    }
}
