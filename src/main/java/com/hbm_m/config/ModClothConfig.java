package com.hbm_m.config;
// Конфигурация мода с использованием AutoConfig и Cloth Config.
// Включает валидацию значений после загрузки для обеспечения корректных настроек

import com.hbm_m.main.MainRegistry;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry.BoundedDiscrete;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.minecraft.util.Mth;

@Config(name = "hbm_m")
public class ModClothConfig implements ConfigData {

    // Общие настройки 
    @Category("general")
    @Gui.Tooltip
    public boolean enableRadiation = true;

    @Category("general")
    @Gui.Tooltip
    public boolean enableChunkRads = true;

//    @Category("general")
//    @Gui.Tooltip
//    public boolean usePrismSystem = false;

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
    public float maxPlayerRad = 100F;

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
    @Gui.CollapsibleObject(startExpanded = false)
    public RadiationPixelEffectSettings radiationPixelEffect = new RadiationPixelEffectSettings();

    public static class RadiationPixelEffectSettings {
        @Gui.Tooltip
        public boolean enableRadiationPixelEffect = true;

        @Gui.Tooltip
        public float radiationPixelEffectThreshold = 0.3f;

        @Gui.Tooltip
        public float radiationPixelMaxIntensityRad = 100.0f;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 500)
        public int radiationPixelEffectMaxDots = 250;

        @Gui.Tooltip
        public float radiationPixelEffectGreenChance = 0.5f;

        @Gui.Tooltip
        public int radiationPixelMinLifetime = 5;

        @Gui.Tooltip
        public int radiationPixelMaxLifetime = 20;
    }

    @Category("overlay")
    @Gui.CollapsibleObject(startExpanded = false)
    public obstructionHighlightSettings obstructionHighlight = new obstructionHighlightSettings();

    public static class obstructionHighlightSettings {

        @Gui.Tooltip
        public boolean enableObstructionHighlight = true;

        @Gui.Tooltip
        @BoundedDiscrete(min = 0, max = 100)
        public int obstructionHighlightAlpha = 20;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 10)
        public int obstructionHighlightDuration = 2;
    }

    @Category("overlay")
    @Gui.Tooltip
    @BoundedDiscrete(min = 0, max = 500)
    public int infoToastOffsetX = 15;

    @Category("overlay")
    @Gui.Tooltip
    @BoundedDiscrete(min = 0, max = 500)
    public int infoToastOffsetY = 15;

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
    @BoundedDiscrete(min = 1, max = 20)
    public int modelStaticRenderDistance = 8;

    /**
     * Server → client pose sync for ballistic missiles (independent of client chunk loading).
     */
    @Category("rendering")
    @Gui.Tooltip
    public boolean enableMissileNetworkTrack = true;

    @Category("rendering")
    @Gui.Tooltip
    @BoundedDiscrete(min = 0, max = 500000)
    public int missileTrackMaxRangeBlocks = 0;

    @Category("rendering")
    @Gui.Tooltip
    @BoundedDiscrete(min = 1, max = 20)
    public int missileTrackInterval = 1;

    @Category("rendering")
    @Gui.Tooltip
    /**
     * CPU voxel ray-march окклюзии в {@link com.hbm_m.client.render.culling.OcclusionCullingHelper}
     * (frustum vanilla + raycast по блокам). Выключите, если модели рендерятся некорректно.
     */
    public boolean enableOcclusionCulling = false;

    /**
     * Перед заливкой instance VBO вызывать {@code glBufferData(..., NULL)} того же размера —
     * orphaning буфера, чтобы драйвер не синхронизировался с предыдущим кадром на каждом
     * {@code glBufferSubData} (типичный AZDO-приём для STREAM).
     */
    @Category("rendering")
    @Gui.Tooltip
    public boolean instanceVboOrphanBeforeUpload = true;

    /**
     * Зарезервировано: persistent mapped instance buffer (GL 4.4+ / ARB_buffer_storage).
     * Сейчас не используется — только переключатель для будущей реализации; безопасный default.
     */
    // @Category("rendering")
    // @Gui.Tooltip
    // public boolean experimentalPersistentInstanceBuffer = true;

    @Category("rendering")
    @Gui.Tooltip
    /**
     * Instanced batch для OBJ-частей. Flush только в {@code AFTER_BLOCK_ENTITIES};
     * текстуры block_lit: {@link com.hbm_m.client.render.SingleMeshVboRenderer} «РЕГРЕССИЯ-СТОП».
     * Нарушение контракта → белые модели при true, нормально при false.
     */
    public boolean useInstancedStaticRendering = true;

    /**
     * Advanced assembler: при vanilla instanced использовать {@code addInstanceGpuBones}
     * (матрица base×part на CPU, без PoseStack push/mul/pop на каждую часть).
     * Под Iris/Oculus внешний шейдер — отдельный путь; этот флаг влияет только на vanilla.
     */
    @Category("rendering")
    @Gui.Tooltip
    public boolean gpuBoneSkinning = true;

    /**
     * 2×4×2 sliced light probes (16 UV) вместо 8 угловых сэмплов для instanced/VBO block_lit.
     * Улучшает освещение на высоких моделях (башня охлаждения, фрекинг), но
     * <b>несовместимо</b> с {@link #useMultiDrawIndirect}: атлас MDI принимает только unsliced layout (30 float).
     */
    @Category("rendering")
    @Gui.Tooltip
    public boolean useSlicedLight = false;

    /**
     * Аггрегация instanced draw в один батч по общему атласу: один
     * {@code glMultiDrawElementsIndirect} на flush (при наличии GL/ARB).
     * Требует GL 4.0+ draw indirect и base instance в команде (GL 4.2+). На macOS (GL 4.1) и без
     * возможностей путь отключается. Не применяется к частям с {@link #useSlicedLight} или GPU bone skinning.
     * Отключите при проблемах с драйвером.
     */
    @Category("rendering")
    @Gui.Tooltip
    public boolean useMultiDrawIndirect = true;

    /** После каждого MDI-dispatch: одна строка INFO (число sub-draw, инстансов, атлас). */
    @Category("rendering")
    @Gui.Tooltip
    public boolean mdiDebugLogDispatch = false;

    /** Плюс по строке INFO на каждую MDI-команду (тег части, baseInstance и т.д.). */
    @Category("rendering")
    @Gui.Tooltip
    public boolean mdiVerboseSubdraws = false;

    /**
     * Max instances per {@link com.hbm_m.client.render.InstancedStaticPartRenderer}
     * (one OBJ part, e.g. ChemPlant/Base). Large machine fields need 4096+.
     */
    @Category("rendering")
    @Gui.Tooltip
    @BoundedDiscrete(min = 256, max = 16384)
    public int maxInstancedInstancesPerPart = 4096;

    @Category("rendering")
    @Gui.Tooltip
    public boolean useColladaDoorAnimations = true;

    @Category("rendering")
    @Gui.Tooltip
    public boolean useColladaZUpConversion = true;


    @Category("rendering")
    @Gui.Tooltip
    @BoundedDiscrete(min = 1, max = 32)
    public int vatsRenderDistanceChunks = 7;

    // Машины

    @Category("machines")
    @Gui.CollapsibleObject(startExpanded = false)
    public FrackingTowerSettings frackingTower = new FrackingTowerSettings();

    public static class FrackingTowerSettings {
        @Gui.Tooltip
        public long maxPower = 5_000_000L;

        @Gui.Tooltip
        public long consumption = 5_000L;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 10_000)
        public int solutionRequired = 10;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 1200)
        public int delay = 20;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 64_000)
        public int oilPerDeposit = 1000;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 64_000)
        public int gasPerDepositMin = 100;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 64_000)
        public int gasPerDepositMax = 500;

        @Gui.Tooltip
        public double drainChance = 0.02D;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 64_000)
        public int oilPerBedrockDeposit = 100;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 64_000)
        public int gasPerBedrockDepositMin = 10;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 64_000)
        public int gasPerBedrockDepositMax = 50;

        @Gui.Tooltip
        @BoundedDiscrete(min = 1, max = 256)
        public int destructionRange = 75;
    }

    // ЯДЕРНЫЕ ВЗРЫВЫ (MK5)

    @Category("explosions")
    @Gui.Tooltip
    @BoundedDiscrete(min = 1, max = 50)
    public int mk5TickTimeMs = 10;

    @Category("explosions")
    @Gui.Tooltip
    @BoundedDiscrete(min = 10, max = 400)
    public int falloutRangePercent = 100;

    @Category("explosions")
    @Gui.Tooltip
    public boolean enableCraterBiomes = true;

    @Category("explosions")
    @Gui.Tooltip
    @BoundedDiscrete(min = 10, max = 200)
    public int fatManRadius = 50;

    // Тепловизор
    @Category("rendering")
    @Gui.Tooltip
    @Gui.EnumHandler(option = Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public ThermalRenderMode thermalRenderMode = ThermalRenderMode.FULL_SHADER;

    public enum ThermalRenderMode {
        FULL_SHADER,
        ORIGINAL_FALLBACK
    }

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
    public boolean enableDebugLogging = false;

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

        originalScaling = this.radiationPixelEffect.radiationPixelEffectGreenChance;

        this.radiationPixelEffect.radiationPixelEffectGreenChance = Mth.clamp(originalScaling, 0.0F, 1.0F);
        // Здесь можно добавить валидацию для других полей, если потребуется

        // Машины
        this.frackingTower.drainChance = Mth.clamp(this.frackingTower.drainChance, 0.0D, 1.0D);
    }

    // Регистрация конфига (вызывать в инициализации мода) 
    public static void register() {
        AutoConfig.register(ModClothConfig.class, Toml4jConfigSerializer::new);
    }

    // Получение текущих настроек 
    public static ModClothConfig get() {
        return AutoConfig.getConfigHolder(ModClothConfig.class).getConfig();
    }

    /** Использовать батчинг для статических частей (frame, Base). При проблемах отключите. */
    public static boolean useInstancedBatching() {
        return get().useInstancedStaticRendering;
    }

}
