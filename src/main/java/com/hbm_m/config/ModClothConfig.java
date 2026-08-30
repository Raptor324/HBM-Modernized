package com.hbm_m.config;
// POJO-холдер конфигурации мода. Все поля публичные со значениями по умолчанию.
//
// Загрузка/сохранение — через HbmConfigStore (JSON, без зависимостей).
// Метаданные полей (сторона, границы, категория, режим применения) — в ConfigSchema.
// Валидация границ — ConfigSchema.validate() (замена validatePostLoad из AutoConfig).
//
// Раньше класс использовал AutoConfig + Cloth Config + Toml4j; теперь это чистый POJO,
// что позволяет убрать обязательную зависимость cloth-config, сохранив ~120 call-сайтов
// вида ModClothConfig.get().field без изменений.

import com.hbm_m.config.schema.ConfigSchema;
import com.hbm_m.config.schema.ConfigSide;

public class ModClothConfig {

    // ════════════════════════════════════════════════════════════════
    // Общие настройки
    // ════════════════════════════════════════════════════════════════
    public boolean enableRadiation = true;
    public boolean enableChunkRads = true;

    /** MOTD при входе в мир и уведомление о новой версии на Modrinth (ориг. GeneralConfig.enableMOTD). */
    public boolean enableMOTD = true;

    // ════════════════════════════════════════════════════════════════
    // Эффекты мира
    // ════════════════════════════════════════════════════════════════
    /** Частицы радиоактивного тумана в чанках (порог/шанс — ChunkRadiationHandlerSimple, как fogRad/fogCh в 1.7.10). */
    public boolean enableRadFogEffect = true;

    /** Как RadiationConfig.worldRadEffects (1.7.10). Пороги/частота — константы в ChunkRadiationHandlerSimple. */
    public boolean worldRadEffects = true;

    /** Следы блока taint под сущностями с эффектом порчи (ориг. ServerConfig.TAINT_TRAILS, по умолчанию выкл.). */
    public boolean taintTrails = false;

    /** Включает смену биома при ядерном взрыве (ориг. WorldConfig.enableCraterBiomes). */
    public boolean enableCraterBiomes = true;
    /** RAD/s для игрока в inner_crater биоме (1.7.10 WorldConfig.craterBiomeInnerRad). */
    public float craterBiomeInnerRad = 25F;
    /** RAD/s для игрока в crater биоме (1.7.10 WorldConfig.craterBiomeRad). */
    public float craterBiomeRad = 5F;
    /** RAD/s для игрока в outer_crater биоме (1.7.10 WorldConfig.craterBiomeOuterRad). */
    public float craterBiomeOuterRad = 0.5F;
    /** Множитель RAD/s в crater биомах в воде/под дождём (1.7.10 WorldConfig.craterBiomeWaterMult). */
    public float craterBiomeWaterMult = 5F;
    /** Фоновая радиация в Незере, RAD/s (1.7.10 RadiationConfig.hellRad, floor поверх чанковой). 0 = выключено. */
    public float netherAmbientRad = 0.1F;
    /** Множитель адского фона в Базальтовых дельтах (0.1 × 10 = 1.0 RAD/s по умолчанию). */
    public float basaltDeltasRadMult = 10F;

    // ════════════════════════════════════════════════════════════════
    // Оружие / падение предметов
    // ════════════════════════════════════════════════════════════════
    /** Спавн сингулярностей/чёрных дыр при падении предмета (WeaponConfig.dropSing). */
    public boolean dropSingularity = true;

    /** Взрыв антиматерии при падении ячейки/пеллета (WeaponConfig.dropCell). */
    public boolean dropCell = true;

    // ════════════════════════════════════════════════════════════════
    // Игрок (радиация)
    // ════════════════════════════════════════════════════════════════
    public float maxPlayerRad = 1000F;
    public float radDecay = 0.01F;
    public float radDamage = 0.05F;
    public float radDamageThreshold = 200F;
    public int radSickness = 200;
    public int radWater = 500;
    public int radConfusion = 700;
    public int radBlindness = 900;

    // ════════════════════════════════════════════════════════════════
    // Экранные наложения (overlay) — клиент
    // ════════════════════════════════════════════════════════════════
    public RadiationPixelEffectSettings radiationPixelEffect = new RadiationPixelEffectSettings();

    public static class RadiationPixelEffectSettings {
        public boolean enableRadiationPixelEffect = true;
        public float radiationPixelEffectThreshold = 0.3f;
        public float radiationPixelMaxIntensityRad = 100.0f;
        public int radiationPixelEffectMaxDots = 250;
        public float radiationPixelEffectGreenChance = 0.5f;
        public int radiationPixelMinLifetime = 5;
        public int radiationPixelMaxLifetime = 20;
    }

    public obstructionHighlightSettings obstructionHighlight = new obstructionHighlightSettings();

    public static class obstructionHighlightSettings {
        public boolean enableObstructionHighlight = true;
        public int obstructionHighlightAlpha = 20;
        public int obstructionHighlightDuration = 2;
    }

    public int infoToastOffsetX = 15;
    public int infoToastOffsetY = 15;

    // ════════════════════════════════════════════════════════════════
    // Чанк-радиация
    // ════════════════════════════════════════════════════════════════
    public float maxRad = 100_000F;
    public float radChunkDecay = 0.1F;
    public float radChunkSpreadFactor = 0.2F;
    public float radSpreadThreshold = 0.01F;
    public float minRadDecayAmount = 0.01F;
    /** GIT ChunkRadiationHandlerSimple has no ambient randomization. */
    public float radRandomizationFactor = 0.0F;

    // ════════════════════════════════════════════════════════════════
    // Рендеринг — клиент
    // ════════════════════════════════════════════════════════════════
    public int modelUpdateDistance = 3;
    public int modelStaticRenderDistance = 8;

    /** Server → client pose sync for ballistic missiles (independent of client chunk loading). */
    public boolean enableMissileNetworkTrack = true;
    public int missileTrackMaxRangeBlocks = 0;
    public int missileTrackInterval = 1;

    /**
     * DH-мост: рендер дальних ракет/гриба внутри DH FBO через DhApiBeforeApplyShaderRenderEvent.
     * Выключите для A/B-теста артефактов (просвечивание пещер, порядок листвы и т.п.):
     * если артефакты остаются без моста — их источник сам DH, а не HBM.
     */
    public boolean enableDhRenderBridge = true;

    /** CPU voxel ray-march окклюзии в OcclusionCullingHelper (frustum vanilla + raycast по блокам). Выключите, если модели рендерятся некорректно. */
    public boolean enableOcclusionCulling = false;

    /** Перед заливкой instance VBO вызывать glBufferData(..., NULL) того же размера — orphaning буфера. */
    public boolean instanceVboOrphanBeforeUpload = true;

    /** Instanced batch для OBJ-частей. Flush только в AFTER_BLOCK_ENTITIES. Нарушение контракта → белые модели при true. */
    public boolean useInstancedStaticRendering = true;

    /** Advanced assembler: при vanilla instanced использовать addInstanceGpuBones (матрица base×part на CPU). */
    public boolean gpuBoneSkinning = false;

    /** 2×4×2 sliced light probes (16 UV). Несовместимо с useMultiDrawIndirect. */
    public boolean useSlicedLight = false;

    /** Аггрегация instanced draw в один батч по общему атласу: один glMultiDrawElementsIndirect на flush. */
    public boolean useMultiDrawIndirect = true;

    /** После каждого MDI-dispatch: одна строка INFO (число sub-draw, инстансов, атлас). */
    public boolean mdiDebugLogDispatch = false;

    /** Плюс по строке INFO на каждую MDI-команду (тег части, baseInstance и т.д.). */
    public boolean mdiVerboseSubdraws = false;

    /** Max instances per InstancedStaticPartRenderer (one OBJ part). Large machine fields need 4096+. */
    public int maxInstancedInstancesPerPart = 4096;

    public int vatsRenderDistanceChunks = 7;

    // ════════════════════════════════════════════════════════════════
    // Машины
    // ════════════════════════════════════════════════════════════════
    public MachineRadarSettings machineRadar = new MachineRadarSettings();

    public static class MachineRadarSettings {
        public boolean generateChunks = false;
    }

    public FrackingTowerSettings frackingTower = new FrackingTowerSettings();

    public static class FrackingTowerSettings {
        public long maxPower = 5_000_000L;
        public long consumption = 5_000L;
        public int solutionRequired = 10;
        public int delay = 20;
        public int oilPerDeposit = 1000;
        public int gasPerDepositMin = 100;
        public int gasPerDepositMax = 500;
        public double drainChance = 0.02D;
        public int oilPerBedrockDeposit = 100;
        public int gasPerBedrockDepositMin = 10;
        public int gasPerBedrockDepositMax = 50;
        public int destructionRange = 75;
    }

    // ════════════════════════════════════════════════════════════════
    // РАДИУСЫ ЯДЕРНЫХ УСТРОЙСТВ — порт BombConfig (1.7.10, категория "nukes", ключи 3.00–3.13)
    // Имена оригинальных ключей сохранены в комментариях для переноса конфигов.
    // ════════════════════════════════════════════════════════════════
    /** Радиус взрыва «Gadget». Ориг. ключ: 3.00_gadgetRadius = 150 */
    public int gadgetRadius = 150;
    /** Радиус взрыва «Little Boy». Ориг. ключ: 3.01_boyRadius = 120 */
    public int boyRadius = 120;
    /** Радиус взрыва «Fat Man». Ориг. ключ: 3.02_manRadius = 175 */
    public int manRadius = 175;
    /** Радиус взрыва «Ivy Mike». Ориг. ключ: 3.03_mikeRadius = 250 */
    public int mikeRadius = 250;
    /** Радиус взрыва «Царь-бомба». Ориг. ключ: 3.04_tsarRadius = 500 */
    public int tsarRadius = 500;
    /** Радиус взрыва «Prototype». Ориг. ключ: 3.05_prototypeRadius = 150 */
    public int prototypeRadius = 150;
    /** Радиус взрыва «FleiJa». Ориг. ключ: 3.06_fleijaRadius = 50 */
    public int fleijaRadius = 50;
    /** Радиус взрыва ЭТЗ-заряда (Solinium). Ориг. ключ: 3.07_soliniumRadius = 150 */
    public int soliniumRadius = 150;
    /** Радиус взрыва N2-мины. Ориг. ключ: 3.08_n2Radius = 200 */
    public int n2Radius = 200;
    /** Радиус ядерной боеголовки баллистической ракеты. Ориг. ключ: 3.09_missileRadius = 100 */
    public int missileRadius = 100;
    /** Радиус РГЧ (cluster MIRV warhead). Ориг. ключ: 3.10_mirvRadius = 100 */
    public int mirvRadius = 100;
    /** Радиус мини-ядерки (Fat Man launcher). Ориг. ключ: 3.11_fatmanRadius = 35 */
    public int fatmanRadius = 35;
    /** Радиус взрыва «Nuka» гранаты. Ориг. ключ: 3.12_nukaRadius = 25 */
    public int nukaRadius = 25;
    /** Радиус взрыва антишрабидиевого снаряда. Ориг. ключ: 3.13_aSchrabRadius = 20 */
    public int aSchrabRadius = 20;

    // ════════════════════════════════════════════════════════════════
    // ПАРАМЕТРЫ ВЗРЫВНОГО ДВИЖКА — ориг. категория "explosions", ключи 6.00–6.06
    // ════════════════════════════════════════════════════════════════
    /** Минимальное время миллисекунд на тик для MK5 chunk processing. Ориг. ключ: 6.02_mk5BlastTime = 50 */
    public int mk5TickTimeMs = 50;
    /** Базовая скорость MK3/Tom blast (блоков/тик). Ориг. ключ: 6.01_blastSpeed = 1024 */
    public int blastSpeed = 1024;
    /** Радиус области радиоактивных осадков, в % от базового радиуса. Ориг. ключ: 6.03_falloutRange = 100 */
    public int falloutRangePercent = 100;
    /** Сколько тиков ждать перед следующим расчётом fallout chunk. Ориг. ключ: 6.04_falloutDelay = 4 */
    public int falloutDelay = 4;
    /** Включить принудительную прогрузку чанков взрывом (chunk ticket). Ориг. ключ: 6.05_enableChunkLoading = true */
    public boolean enableChunkLoading = true;
    /** Алгоритм взрыва: 0 = Legacy (однопоточный), 1/2 = многопоточный движок с той же энергомоделью Legacy (идентичны). Ориг. ключ: 6.06_explosionAlgorithm (в 1.7.10 дефолт 2, но фактически всегда работал Legacy). */
    public int explosionAlgorithm = 0;
    /** Лимит жизни невыгруженного взрыва в секундах; 0 = без лимита (ориг. BombConfig.limitExplosionLifespan, ключ 6.00). */
    public int limitExplosionLifespan = 0;
    /** Сохранять состояние взрыва MK5 в NBT — после перезахода взрыв продолжается с места остановки (ориг. BombConfig.enableNukeNBTSaving). */
    public boolean enableNukeNBTSaving = true;

    // ════════════════════════════════════════════════════════════════
    // Тепловизор
    // ════════════════════════════════════════════════════════════════
    public ThermalRenderMode thermalRenderMode = ThermalRenderMode.FULL_SHADER;

    public enum ThermalRenderMode {
        FULL_SHADER,
        ORIGINAL_FALLBACK
    }

    // ════════════════════════════════════════════════════════════════
    // Отладка
    // ════════════════════════════════════════════════════════════════
    public boolean enableDebugRender = true;
    public boolean debugRenderInSurvival = false;
    public float debugRenderTextSize = 0.2F;
    public int debugRenderDistance = 4;
    public boolean enableDebugLogging = false;

    // ════════════════════════════════════════════════════════════════
    // Singleton + загрузка/сохранение (замена AutoConfig)
    // ════════════════════════════════════════════════════════════════

    /** Текущий экземпляр конфига (никогда не null). */
    private static volatile ModClothConfig INSTANCE = new ModClothConfig();

    /** Текущие настройки (никогда не возвращает null). ~120 call-сайтов читают поля отсюда. */
    public static ModClothConfig get() {
        return INSTANCE;
    }

    /**
     * Загружает конфиг из JSON-файлов (client.json + server.json) и заменяет синглтон.
     * Заменяет {@code AutoConfig.register(...)}. Вызывать один раз на старте
     * (статический блок MainRegistry) — ДО загрузки классов, захватывающих значения
     * в static final (см. {@code ChunkRadiationHandlerSimple.MAX_RAD}).
     *
     * <p>Если файлы отсутствуют — создаются со значениями по умолчанию. Повреждённый
     * файл → значения по умолчанию (без падения).
     */
    public static void load() {
        ModClothConfig cfg = new ModClothConfig();
        HbmConfigStore.load(ConfigSide.CLIENT, cfg);
        HbmConfigStore.load(ConfigSide.SERVER, cfg);
        ConfigSchema.validate(cfg);
        INSTANCE = cfg;
    }

    /** Перезагружает только клиентские настройки из client.json. */
    public static void reloadClient() {
        HbmConfigStore.load(ConfigSide.CLIENT, INSTANCE);
    }

    /** Перезагружает только серверные настройки из server.json. */
    public static void reloadServer() {
        HbmConfigStore.load(ConfigSide.SERVER, INSTANCE);
    }

    /** Сохраняет клиентские настройки в {@code config/hbm_m/client.json}. */
    public static void saveClient() {
        HbmConfigStore.save(ConfigSide.CLIENT, INSTANCE);
    }

    /** Сохраняет серверные настройки в {@code config/hbm_m/server.json}. */
    public static void saveServer() {
        HbmConfigStore.save(ConfigSide.SERVER, INSTANCE);
    }

    /** Использовать батчинг для статических частей (frame, Base). При проблемах отключите. */
    public static boolean useInstancedBatching() {
        return INSTANCE.useInstancedStaticRendering;
    }
}
