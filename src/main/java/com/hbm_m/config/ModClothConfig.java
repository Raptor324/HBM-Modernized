package com.hbm_m.config;
// Конфигурация мода с использованием AutoConfig и Cloth Config.
// Включает валидацию значений после загрузки для обеспечения корректных настроек

import net.minecraft.util.Mth;
import com.hbm_m.main.MainRegistry;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

@Config(name = "hbm_m")
public class ModClothConfig implements ConfigData {
    // Общие настройки 
    @Category("general")
    @Gui.Tooltip
    public boolean enableRadiation = true;

    @Category("general")
    @Gui.Tooltip
    public boolean enableChunkRads = true;

    @Category("general")
    @Gui.Tooltip
    public boolean usePrismSystem = false;

     // Эффекты мира 
    @Category("world_effects")
    @Gui.Tooltip
    public boolean worldRadEffects = true;

    @Category("world_effects")
    @Gui.Tooltip
    public float worldRadEffectsThreshold = 500.0F;

    @Category("world_effects")
    @Gui.Tooltip
    @BoundedDiscrete(min = 1, max = 100)
    public int worldRadEffectsBlockChecks = 10;

    @Category("world_effects")
    @Gui.Tooltip
    public float worldRadEffectsMaxScaling = 4.0F;

    @Category("world_effects")
    @Gui.Tooltip
    @BoundedDiscrete(min = 1, max = 16)
    public int worldRadEffectsMaxDepth = 5;

    @Category("world_effects")
    @Gui.Tooltip
    public boolean enableRadFogEffect = true;

    @Category("world_effects")
    @Gui.Tooltip
    public float radFogThreshold = 50F;

    @Category("world_effects")
    @Gui.Tooltip
    public int radFogChance = 10;

    // Игрок 
    @Category("player")
    @Gui.Tooltip
    public float maxPlayerRad = 1000F;

    @Category("player")
    @Gui.Tooltip
    public float radDecay = 0.01F;

    @Category("player")
    @Gui.Tooltip
    public float radDamage = 0.05F;

    @Category("player")
    @Gui.Tooltip
    public float radDamageThreshold = 200F;

    @Category("player")
    @Gui.Tooltip
    public int radSickness = 300;

    @Category("player")
    @Gui.Tooltip
    public int radWater = 500;

    @Category("player")
    @Gui.Tooltip
    public int radConfusion = 700;

    @Category("player")
    @Gui.Tooltip
    public int radBlindness = 900;

    // Экранные наложения

    @Category("overlay")
    @Gui.Tooltip
    public boolean enableRadiationPixelEffect = true;

    @Category("overlay")
    @Gui.Tooltip
    public float radiationPixelEffectThreshold = 0.3f;

    @Category("overlay")
    @Gui.Tooltip
    public float radiationPixelMaxIntensityRad = 100.0f;

    @Category("overlay")
    @Gui.Tooltip
    @BoundedDiscrete(min = 1, max = 500)
    public int radiationPixelEffectMaxDots = 250;

    @Category("overlay")
    @Gui.Tooltip
    public float radiationPixelEffectGreenChance = 0.5f;

    @Category("overlay")
    @Gui.Tooltip
    public int radiationPixelMinLifetime = 5;

    @Category("overlay")
    @Gui.Tooltip
    public int radiationPixelMaxLifetime = 20;

    @Category("overlay")
    @Gui.Tooltip()
    public boolean enableObstructionHighlight = true;

    @Category("overlay")
    @Gui.Tooltip()
    @BoundedDiscrete(min = 0, max = 100)
    public int obstructionHighlightAlpha = 20;

    @Category("overlay")
    @Gui.Tooltip
    @BoundedDiscrete(min = 1, max = 10)
    public int obstructionHighlightDuration = 2;

    // Чанк 
    @Category("chunk")
    @Gui.Tooltip
    public float maxRad = 100_000F;

    @Category("chunk")
    @Gui.Tooltip
    public float radChunkDecay = 0.1F;

    @Category("chunk")
    @Gui.Tooltip
    public float radChunkSpreadFactor = 0.2F;

    @Category("chunk")
    @Gui.Tooltip
    public float radSpreadThreshold = 0.01F;

    @Category("chunk")
    @Gui.Tooltip
    public float minRadDecayAmount = 0.01F;

    @Category("chunk")
    @Gui.Tooltip
    public float radSourceInfluenceFactor = 0.08F;

    @Category("chunk")
    @Gui.Tooltip
    public float radRandomizationFactor = 1.0F;

    @Category("rendering")
    @Gui.Tooltip
    @BoundedDiscrete(min = 0, max = 20)
    public int modelUpdateDistance = 3;

    @Category("rendering")
    @Gui.Tooltip
    public boolean enableOcclusionCulling = true;

    // Отладка 
    @Category("debug")
    @Gui.Tooltip
    public boolean enableDebugRender = true;

    @Category("debug")
    @Gui.Tooltip
    public boolean debugRenderInSurvival = false;

    @Category("debug")
    @Gui.Tooltip
    public float debugRenderTextSize = 0.2F;

    @Category("debug")
    @Gui.Tooltip
    @BoundedDiscrete(min = 1, max = 20)
    public int debugRenderDistance = 4;

    @Category("debug")
    @Gui.Tooltip
    public boolean enableDebugLogging = true;

    @Override
    public void validatePostLoad() throws ValidationException {
        // Вызываем родительский метод на всякий случай
        ConfigData.super.validatePostLoad();

        // Проверяем и исправляем наше float значение
        float minThreshold = 1.0F;
        float maxThreshold = 100000.0F;

        // Используем Mth.clamp для удобства. Он ограничивает значение между min и max.
        float originalValue = this.worldRadEffectsThreshold;
        this.worldRadEffectsThreshold = Mth.clamp(originalValue, minThreshold, maxThreshold);

        // Если значение было исправлено, выводим предупреждение в лог.
        // Это очень полезно для администраторов серверов.
        if (originalValue != this.worldRadEffectsThreshold) {
            MainRegistry.LOGGER.warn("[HBM-M Config] Значение 'worldRadEffectsThreshold' было некорректным ({}). Оно было автоматически исправлено на {}.", originalValue, this.worldRadEffectsThreshold);
        }

        float minScaling = 1.0F;
        float maxScaling = 10.0F; // Ограничим 10-кратным увеличением, чтобы избежать проблем
        float originalScaling = this.worldRadEffectsMaxScaling;
        this.worldRadEffectsMaxScaling = Mth.clamp(originalScaling, minScaling, maxScaling);

        if (originalScaling != this.worldRadEffectsMaxScaling) {
            MainRegistry.LOGGER.warn("[HBM-M Config] Значение 'worldRadEffectsMaxScaling' было некорректным ({}). Оно было автоматически исправлено на {}.", originalScaling, this.worldRadEffectsMaxScaling);
        }

        originalScaling = this.radiationPixelEffectGreenChance;

        this.radiationPixelEffectGreenChance = Mth.clamp(originalScaling, 0.0F, 1.0F);
        // Здесь можно добавить валидацию для других полей, если потребуется
    }

    // Регистрация конфига (вызывать в инициализации мода) 
    public static void register() {
        AutoConfig.register(ModClothConfig.class, Toml4jConfigSerializer::new);
    }

    // Получение текущих настроек 
    public static ModClothConfig get() {
        return AutoConfig.getConfigHolder(ModClothConfig.class).getConfig();
    }
}
