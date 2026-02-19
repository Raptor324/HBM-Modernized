package com.hbm_m.config;

import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.model.variant.DoorModelType;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.main.MainRegistry;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация моделей дверей по умолчанию.
 * Настройки сохраняются в config/hbm_m_door_models.toml
 * 
 * @author HBM-M Team
 */
@Config(name = MainRegistry.MOD_ID + "_door_models")
public class DoorModelConfig implements ConfigData {
    
    /**
     * Глобальный выбор по умолчанию для всех дверей
     */
    @ConfigEntry.Gui.Tooltip
    public String globalDefaultType = "legacy";
    
    public String globalDefaultSkin = "default";
    
    /**
     * Настройки для конкретных типов дверей
     * Формат: "door_id=type:skin"
     * Например: "large_vehicle_door=modern:clean"
     */
    @ConfigEntry.Gui.Tooltip
    public Map<String, String> doorDefaults = new HashMap<>();
    
    /**
     * Получить выбор модели по умолчанию для указанной двери
     */
    public DoorModelSelection getDefaultForDoor(String doorId) {
        // Сначала проверяем настройки для конкретной двери
        String doorSetting = doorDefaults.get(doorId);
        if (doorSetting != null && !doorSetting.isEmpty()) {
            DoorModelSelection selection = parseSelection(doorSetting);
            if (selection != null) {
                return selection;
            }
        }
        
        // Затем глобальные настройки
        return new DoorModelSelection(
            DoorModelType.fromId(globalDefaultType),
            DoorSkin.of(globalDefaultSkin)
        );
    }
    
    /**
     * Установить выбор по умолчанию для двери
     */
    public void setDefaultForDoor(String doorId, DoorModelSelection selection) {
        String value = selection.getModelType().getId() + ":" + selection.getSkin().getId();
        doorDefaults.put(doorId, value);
    }
    
    /**
     * Парсит строку настройки формата "type:skin"
     */
    private DoorModelSelection parseSelection(String value) {
        try {
            String[] parts = value.split(":");
            DoorModelType type = DoorModelType.fromId(parts[0]);
            String skinId = parts.length > 1 ? parts[1] : "default";
            return new DoorModelSelection(
                type,
                DoorSkin.of(skinId)
            );
        } catch (Exception e) {
            MainRegistry.LOGGER.warn("Invalid door model config value: {}", value);
            return null;
        }
    }
    
    /**
     * Сбросить настройки для двери (использовать глобальные)
     */
    public void resetDefaultForDoor(String doorId) {
        doorDefaults.remove(doorId);
    }
    
    /**
     * Сбросить все настройки
     */
    public void reset() {
        globalDefaultType = "legacy";
        globalDefaultSkin = "default";
        doorDefaults.clear();
    }
}