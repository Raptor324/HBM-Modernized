package com.hbm_m.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

@Config(name = "hbm_m")
public class ModClothConfig implements ConfigData {
    // === Общие настройки ===
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean enableRadiation = true;
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean enableChunkRads = true;
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean worldRadEffects = true;
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean usePrismSystem = false;

    // === Игрок ===
    @ConfigEntry.Category("player")
    @ConfigEntry.Gui.Tooltip
    public float maxPlayerRad = 1000F;
    @ConfigEntry.Category("player")
    @ConfigEntry.Gui.Tooltip
    public float radDecay = 0.01F;
    @ConfigEntry.Category("player")
    @ConfigEntry.Gui.Tooltip
    public float radDamage = 0.05F;
    @ConfigEntry.Category("player")
    @ConfigEntry.Gui.Tooltip
    public float radDamageThreshold = 200F;
    @ConfigEntry.Category("player")
    @ConfigEntry.Gui.Tooltip
    public int radSickness = 300;
    @ConfigEntry.Category("player")
    @ConfigEntry.Gui.Tooltip
    public int radWater = 500;
    @ConfigEntry.Category("player")
    @ConfigEntry.Gui.Tooltip
    public int radConfusion = 700;
    @ConfigEntry.Category("player")
    @ConfigEntry.Gui.Tooltip
    public int radBlindness = 900;

    // === Чанк ===
    @ConfigEntry.Category("chunk")
    @ConfigEntry.Gui.Tooltip
    public float maxRad = 100_000F;
    @ConfigEntry.Category("chunk")
    @ConfigEntry.Gui.Tooltip
    public float fogRad = 50F;
    @ConfigEntry.Category("chunk")
    @ConfigEntry.Gui.Tooltip
    public int fogCh = 50;
    @ConfigEntry.Category("chunk")
    @ConfigEntry.Gui.Tooltip
    public float radChunkDecay = 0.1F;
    @ConfigEntry.Category("chunk")
    @ConfigEntry.Gui.Tooltip
    public float radChunkSpreadFactor = 0.2F;
    @ConfigEntry.Category("chunk")
    @ConfigEntry.Gui.Tooltip
    public float radSpreadThreshold = 0.01F;
    @ConfigEntry.Category("chunk")
    @ConfigEntry.Gui.Tooltip
    public float minRadDecayAmount = 0.01F;
    @ConfigEntry.Category("chunk")
    @ConfigEntry.Gui.Tooltip
    public float radSourceInfluenceFactor = 0.35F;
    @ConfigEntry.Category("chunk")
    @ConfigEntry.Gui.Tooltip
    public float radRandomizationFactor = 1.0F;

    // === Модификаторы ===
    @ConfigEntry.Category("modifiers")
    @ConfigEntry.Gui.Tooltip
    public float hazmatMod = 0.25F;
    @ConfigEntry.Category("modifiers")
    @ConfigEntry.Gui.Tooltip
    public float advHazmatMod = 0.1F;
    @ConfigEntry.Category("modifiers")
    @ConfigEntry.Gui.Tooltip
    public float paaHazmatMod = 0.05F;

    // === Отладка ===
    @ConfigEntry.Category("debug_render")
    @ConfigEntry.Gui.Tooltip
    public boolean enableDebugRender = true;
    @ConfigEntry.Category("debug_render")
    @ConfigEntry.Gui.Tooltip
    public float debugRenderTextSize = 0.1F;
    @ConfigEntry.Category("debug_render")
    @ConfigEntry.Gui.Tooltip
    public int debugRenderDistance = 4;
    @ConfigEntry.Category("debug_render")
    @ConfigEntry.Gui.Tooltip
    public boolean debugRenderInSurvival = false;

    // Можно добавить validatePostLoad() если нужно автокорректировать значения

    // === Регистрация конфига (вызывать в инициализации мода) ===
    public static void register() {
        AutoConfig.register(ModClothConfig.class, Toml4jConfigSerializer::new);
    }

    // === Получение текущих настроек ===
    public static ModClothConfig get() {
        return AutoConfig.getConfigHolder(ModClothConfig.class).getConfig();
    }
}
