package com.hbm_m.client.model.variant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm_m.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Реестр моделей и скинов дверей.
 * Загружает конфигурацию из JSON файлов и предоставляет доступ к моделям и скинам.
 * 
 * @author HBM-M Team
 */
@OnlyIn(Dist.CLIENT)
public class DoorModelRegistry implements ResourceManagerReloadListener {
    
    private static final DoorModelRegistry INSTANCE = new DoorModelRegistry();
    
    // Путь к конфигам - рядом с моделью
    // Пример: models/block/doors/large_vehicle_door_config.json
    private static final String CONFIG_PATH = "models/block/doors/";
    private static final String CONFIG_SUFFIX = "_config.json";
    
    // Хранилище конфигураций дверей: doorId -> DoorConfig
    private final Map<String, DoorConfig> doorConfigs = new LinkedHashMap<>();
    
    // Настройки по умолчанию: doorId -> DoorModelSelection
    private final Map<String, DoorModelSelection> defaultSelections = new LinkedHashMap<>();
    
    private boolean initialized = false;
    
    private DoorModelRegistry() {}
    
    public static DoorModelRegistry getInstance() {
        return INSTANCE;
    }
    
    // ==================== Регистрация ====================
    
    /**
     * Регистрирует конфигурацию двери
     */
    public void register(String doorId, DoorConfig config) {
        doorConfigs.put(doorId, config);
        MainRegistry.LOGGER.info("Registered door config: {} with {} skins", 
            doorId, config.getSkins().size());
    }
    
    /**
     * Устанавливает выбор по умолчанию для двери
     */
    public void setDefaultSelection(String doorId, DoorModelSelection selection) {
        defaultSelections.put(doorId, selection);
    }
    
    /**
     * Быстрая регистрация двери с минимальной конфигурацией
     */
    public void registerSimple(String doorId, 
                               ResourceLocation legacyModel,
                               ResourceLocation modernModel,
                               List<DoorSkin> modernSkins) {
        DoorConfig config = new DoorConfig(doorId, legacyModel, modernModel, modernSkins);
        register(doorId, config);
    }
    
    // ==================== Получение данных ====================
    
    /**
     * Проверяет, зарегистрирована ли дверь
     */
    public boolean isRegistered(String doorId) {
        return doorConfigs.containsKey(doorId);
    }
    
    /**
     * Получает конфигурацию двери
     */
    public DoorConfig getConfig(String doorId) {
        return doorConfigs.get(doorId);
    }
    
    /**
     * Получает путь к модели для указанного типа
     */
    public ResourceLocation getModelPath(String doorId, DoorModelType type) {
        DoorConfig config = doorConfigs.get(doorId);
        if (config == null) return null;
        
        return type.isLegacy() ? config.getLegacyModel() : config.getModernModel();
    }
    
    /**
     * Получает путь к модели с учётом скина (для MODERN — скин может иметь свою модель).
     * Важно: скин из NBT (DoorSkin.of) имеет modelPath=null — разрешаем через реестр по skinId.
     */
    public ResourceLocation getModelPath(String doorId, DoorModelSelection selection) {
        DoorConfig config = doorConfigs.get(doorId);
        if (config == null) return null;
        
        if (selection.getModelType().isLegacy()) {
            return config.getLegacyModel();
        }
        // MODERN: проверяем modelPath скина (может быть null при загрузке из NBT)
        ResourceLocation skinModel = selection.getSkin().getModelPath();
        if (skinModel != null) {
            return skinModel;
        }
        // Скин загружен из NBT без modelPath — разрешаем через реестр по skinId
        if (!selection.getSkin().isDefault()) {
            DoorSkin resolvedSkin = config.getSkinById(selection.getSkin().getId());
            if (resolvedSkin != null && resolvedSkin.getModelPath() != null) {
                return resolvedSkin.getModelPath();
            }
        }
        return config.getModernModel();
    }
    
    /**
     * Получает список скинов для двери
     */
    public List<DoorSkin> getSkins(String doorId) {
        DoorConfig config = doorConfigs.get(doorId);
        if (config == null) {
            return Collections.singletonList(DoorSkin.DEFAULT);
        }
        return config.getSkins();
    }
    
    /**
     * Получает скин по ID
     */
    public DoorSkin getSkin(String doorId, String skinId) {
        if (skinId == null || skinId.isEmpty() || "default".equals(skinId)) {
            return DoorSkin.DEFAULT;
        }
        
        DoorConfig config = doorConfigs.get(doorId);
        if (config == null) {
            return DoorSkin.DEFAULT;
        }
        
        return config.getSkinById(skinId);
    }
    
    /**
     * Получает выбор модели по умолчанию для двери.
     * ПРИОРИТЕТ:
     * 1. defaultSelections (установленные через API)
     * 2. JSON конфиг (default секция)
     * 3. LEGACY по умолчанию
     */
    public DoorModelSelection getDefaultSelection(String doorId) {
        DoorModelSelection custom = defaultSelections.get(doorId);
        if (custom != null) {
            return custom;
        }
        
        DoorConfig config = doorConfigs.get(doorId);
        if (config != null && config.getDefaultSelection() != null) {
            return config.getDefaultSelection();
        }
        
        return DoorModelSelection.DEFAULT;
    }
    
    /**
     * Получает все зарегистрированные ID дверей
     */
    public Set<String> getRegisteredDoorIds() {
        return Collections.unmodifiableSet(doorConfigs.keySet());
    }
    
    // ==================== Перезагрузка ресурсов ====================
    
    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        MainRegistry.LOGGER.info("Reloading door model configurations...");
        
        // Сохраняем пользовательские настройки по умолчанию
        Map<String, DoorModelSelection> savedDefaults = new LinkedHashMap<>(defaultSelections);
        
        doorConfigs.clear();
        
        // Загружаем конфигурации из ресурсов
        loadAllConfigs(resourceManager);
        
        // Восстанавливаем пользовательские настройки
        defaultSelections.putAll(savedDefaults);
        
        initialized = true;
        MainRegistry.LOGGER.info("Door model registry reloaded. {} door types registered", doorConfigs.size());
    }
    
    private void loadAllConfigs(ResourceManager resourceManager) {
        String[] knownDoorTypes = {
            "large_vehicle_door", "round_airlock_door", "transition_seal", "fire_door",
            "sliding_blast_door", "sliding_seal_door", "secure_access_door",
            "qe_sliding_door", "qe_containment_door", "water_door", "silo_hatch", "silo_hatch_large",
            "vault_door"
        };
        
        for (String namespace : resourceManager.getNamespaces()) {
            for (String doorType : knownDoorTypes) {
                // Путь к конфигу рядом с моделью
                // models/block/doors/large_vehicle_door_config.json
                ResourceLocation configPath = ResourceLocation.fromNamespaceAndPath(
                    namespace, 
                    CONFIG_PATH + doorType + CONFIG_SUFFIX
                );
                
                try {
                    Optional<Resource> resource = resourceManager.getResource(configPath);
                    if (resource.isPresent()) {
                        loadConfigFromResource(resource.get(), configPath);
                    }
                } catch (Exception ignored) {
                    // Конфиг не обязателен - дверь будет использовать LEGACY по умолчанию
                }
            }
        }
    }
    
    private void loadConfigFromResource(Resource resource, ResourceLocation configPath) {
        try (BufferedReader reader = resource.openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            DoorConfig config = DoorConfig.fromJson(json);
            doorConfigs.put(config.getDoorId(), config);
            MainRegistry.LOGGER.debug("Loaded door config: {} from {}", config.getDoorId(), configPath);
        } catch (IOException e) {
            MainRegistry.LOGGER.error("Failed to load door config: {}", configPath, e);
        }
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void clearCache() {
        // Очистка кэшей при необходимости
    }
    
    // ==================== Внутренний класс конфигурации ====================
    
    /**
     * Конфигурация одной двери
     */
    public static class DoorConfig {
        private final String doorId;
        private final ResourceLocation legacyModel;
        private final ResourceLocation modernModel;
        private final List<DoorSkin> skins;
        private final Map<String, DoorSkin> skinMap;
        private DoorModelSelection defaultSelection;
        
        public DoorConfig(String doorId, 
                         ResourceLocation legacyModel, 
                         ResourceLocation modernModel,
                         List<DoorSkin> skins) {
            this.doorId = doorId;
            this.legacyModel = legacyModel;
            this.modernModel = modernModel;
            this.skins = new ArrayList<>(skins);
            
            // Индекс для быстрого поиска
            this.skinMap = new LinkedHashMap<>();
            for (DoorSkin skin : skins) {
                skinMap.put(skin.getId(), skin);
            }
            
            // По умолчанию LEGACY
            this.defaultSelection = DoorModelSelection.DEFAULT;
        }
        
        public String getDoorId() { return doorId; }
        public ResourceLocation getLegacyModel() { return legacyModel; }
        public ResourceLocation getModernModel() { return modernModel; }
        public List<DoorSkin> getSkins() { return Collections.unmodifiableList(skins); }
        public DoorModelSelection getDefaultSelection() { return defaultSelection; }
        
        public void setDefaultSelection(DoorModelSelection selection) {
            this.defaultSelection = selection;
        }
        
        public DoorSkin getSkinById(String id) {
            return skinMap.getOrDefault(id, DoorSkin.DEFAULT);
        }
        
        /**
         * Парсинг из JSON
         */
        public static DoorConfig fromJson(JsonObject json) {
            String doorId = json.get("door_id").getAsString();
            
            // Модели
            ResourceLocation legacyModel = null;
            if (json.has("legacy_model")) {
                legacyModel = ResourceLocation.parse(json.get("legacy_model").getAsString());
            }
            
            ResourceLocation modernModel = null;
            if (json.has("modern_model")) {
                JsonObject modern = json.getAsJsonObject("modern_model");
                if (modern.has("model")) {
                    modernModel = ResourceLocation.parse(modern.get("model").getAsString());
                }
            }
            
            // Скины
            List<DoorSkin> skins = new ArrayList<>();
            skins.add(DoorSkin.DEFAULT); // Всегда есть default
            
            if (json.has("modern_model")) {
                JsonObject modern = json.getAsJsonObject("modern_model");
                if (modern.has("skins")) {
                    JsonArray skinsArray = modern.getAsJsonArray("skins");
                    for (JsonElement elem : skinsArray) {
                        DoorSkin skin = DoorSkin.fromJson(elem.getAsJsonObject());
                        if (!skin.isDefault()) {
                            skins.add(skin);
                        }
                    }
                }
            }
            
            DoorConfig config = new DoorConfig(doorId, legacyModel, modernModel, skins);
            
            // Выбор по умолчанию из конфига
            if (json.has("default")) {
                JsonObject def = json.getAsJsonObject("default");
                DoorModelType type = DoorModelType.fromId(def.get("type").getAsString());
                String skinId = def.has("skin") ? def.get("skin").getAsString() : "default";
                DoorSkin skin = config.getSkinById(skinId);
                config.setDefaultSelection(new DoorModelSelection(type, skin));
            }
            
            return config;
        }
    }
}
