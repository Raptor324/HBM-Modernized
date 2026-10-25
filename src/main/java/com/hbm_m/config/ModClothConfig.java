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

import com.hbm_m.client.render.culling.GpuCullingCapability;
import com.hbm_m.config.schema.ConfigSchema;
import com.hbm_m.config.schema.ConfigSide;

public class ModClothConfig {

    /**
     * Курс конверсии HBM Energy (HE) ↔ Forge Energy (FE): по умолчанию 5 HE = 1 FE.
     * Применяется на всех границах с чужими FE-предметами/машинами и мостом сети.
     */
    public int energyRatioHe = 5;
    public int energyRatioFe = 1;

    // Общие настройки
    /**
     * The RBMK simulation dials that vanilla game rules cannot carry. In the original every dial
     * is a world game rule; the boolean and integer ones are registered as such in
     * {@link com.hbm_m.handler.rbmk.RBMKGameRules}, but game rules have no floating-point type,
     * so the rest live here. Defaults are the original's, so leaving this untouched behaves
     * exactly like an unmodified world.
     */
    public RBMKDialSettings rbmkDials = new RBMKDialSettings();

    public static class RBMKDialSettings {
        /** dialPassiveCooling */
        public double passiveCooling = 2.5;
        /** dialPassiveCoolingInner */
        public double passiveCoolingInner = 0.1;
        /** dialColumnHeatFlow */
        public double columnHeatFlow = 0.2;
        /** dialDiffusionMod */
        public double fuelDiffusionMod = 1.0;
        /** dialHeatProvision */
        public double heatProvision = 0.2;
        /** dialBoilerHeatConsumption */
        public double boilerHeatConsumption = 0.1;
        /** dialControlSpeed */
        public double controlSpeedMod = 1.0;
        /** dialReactivityMod */
        public double reactivityMod = 1.0;
        /** dialOutgasserSpeedMod */
        public double outgasserMod = 1.0;
        /** dialControlSurgeMod */
        public double surgeMod = 1.0;
        /** dialReasimBoilerSpeed */
        public double reasimBoilerSpeed = 0.05;
        /** dialModeratorEfficiency */
        public double moderatorEfficiency = 1.0;
        /** dialAbsorberEfficiency */
        public double absorberEfficiency = 1.0;
        /** dialReflectorEfficiency */
        public double reflectorEfficiency = 1.0;
        /** dialAbsorberHeatConversion */
        public double absorberHeatConversion = 0.05;
    }

    // ════════════════════════════════════════════════════════════════
    // Original ServerConfig (1.7.10, JSON-Konfiguration hbmServer.json)
    // ════════════════════════════════════════════════════════════════
    /** Original: ServerConfig.DAMAGE_COMPATIBILITY_MODE. */
    public boolean damageCompatibilityMode = false;
    /** Original: ServerConfig.MINE_AP_DAMAGE / HE / SHRAP / NUKE / NAVAL. */
    public float mineApDamage = 10F;
    public float mineHeDamage = 35F;
    public float mineShrapDamage = 7.5F;
    public float mineNukeDamage = 100F;
    public float mineNavalDamage = 60F;
    /** Original: ServerConfig.CRATE_OPEN_HELD. */
    public boolean crateOpenHeld = true;
    /** Original: ServerConfig.CRATE_KEEP_CONTENTS. */
    public boolean crateKeepContents = true;
    /** Original: ServerConfig.ITEM_HAZARD_DROP_TICKRATE. */
    public int itemHazardDropTickrate = 2;
    /** Original: ServerConfig.ENABLE_MKU - Ansteckung (contagion) durch das MKU. */
    public boolean enableMKU = true;
    /** Original: PotionConfig.potionSickness ("OFF" = 0 Standard, "NORMAL" = 1, "TERRARIA" = 2 -> Dauer x12). */
    public int potionSickness = 0;
    /** Original: ServerConfig.STRUCTURE_DEBUG. */
    public boolean structureDebug = false;
    /** Original: ServerConfig.AUTOCAL_MAX_CLOCK. */
    public int autocalMaxClock = 20;
    /** Original: GeneralConfig.enable528NetherBurn - Spieler brennen im Nether (nur wirksam mit enable528, Vorgabe wie Original an). */
    public boolean enable528NetherBurn = true;
    /** Original: GeneralConfig.enableMachineGravity - grosse Maschinen brauchen ein Fundament (checkTilt). */
    public boolean enableMachineGravity = false;
    /** Original: GeneralConfig.enable528MachineGravity - dasselbe im 528-Modus (nur wirksam mit enable528). */
    public boolean enable528MachineGravity = true;
    /** Original: GeneralConfig.enableMycelium - Toedliches Myzel breitet sich aus. */
    public boolean enableMycelium = false;
    /** Original: RadiationConfig.cleanupDeadDirt - tote Erde/Myzel werden sofort wieder zu Erde. */
    public boolean cleanupDeadDirt = false;
    /** Original: GeneralConfig.enable528ColtanDeposit - Coltan-Lagerstaette um einen Seed-Punkt. */
    public boolean enable528ColtanDeposit = true;
    /** Original: GeneralConfig.enable528ColtanSpawn - Coltan zusaetzlich als zufaellige Ader ueberall. */
    public boolean enable528ColtanSpawn = false;
    /** Original: GeneralConfig.coltanRate - Adern pro Chunk bei enable528ColtanSpawn. */
    public int coltanRate = 2;
    /** Original ToolConfig (CATEGORY_TOOLS 11.xx), Standardwerte aus loadFromConfig. */
    public int toolRecursionDepth = 1000;
    public boolean toolRecursiveStone = false;
    public boolean toolRecursiveNetherrack = false;
    public boolean toolAbilityHammer = true;
    public boolean toolAbilityVein = true;
    public boolean toolAbilityLuck = true;
    public boolean toolAbilitySilk = true;
    public boolean toolAbilityFurnace = true;
    public boolean toolAbilityShredder = true;
    public boolean toolAbilityCentrifuge = true;
    public boolean toolAbilityCrystallizer = true;
    public boolean toolAbilityMercury = true;
    public boolean toolAbilityExplosion = true;
    /** Original ClientConfig.TOOL_HUD_INDICATOR_X / _Y. */
    public int toolHudIndicatorX = 0;
    public int toolHudIndicatorY = 0;
    /** Original ClientConfig.RENDER_REBAR_SIMPLE (nur drei Staebe, wirkt nach Neuladen der Ressourcen) / RENDER_REBAR_LIMIT. */
    public boolean renderRebarSimple = false;
    /** Original ClientConfig.RENDER_REEDS: Schilf bis zum Gewaessergrund zeichnen. */
    public boolean renderReeds = true;
    /** Original ClientConfig GUN_ANIMS_LEGACY / GUN_MODEL_FOV / GUN_VISUAL_RECOIL / GUN_ANIMATION_SPEED. */
    public boolean gunAnimsLegacy = false;
    public boolean gunModelFov = false;
    public boolean gunVisualRecoil = true;
    public float gunAnimationSpeed = 1.0F;
    /** Original GeneralConfig 1.20_enableGuns: neue Waffen koennen abgefeuert werden. */
    public boolean enableGuns = true;
    public int renderRebarLimit = 250;
    /** Original: generalOverride (1-18 erzwingt die Polaroid-Zahl, 0 = zufaellig). */
    public int polaroidOverride = 0;

    /** Original WorldConfig (Kategorie METEOR), Werte wie nach dem Laden der Originalkonfig. */
    public boolean enableMeteorStrikes = true;
    public boolean enableMeteorShowers = true;
    public boolean enableMeteorTails = true;
    public boolean enableSpecialMeteors = true;
    public int meteorStrikeChance = 20 * 60 * 60 * 5;
    public int meteorShowerChance = 20 * 60 * 15;
    public int meteorShowerDuration = 20 * 60 * 30;

    public boolean enableRadiation = true;
    public boolean enableChunkRads = true;

    /**
     * Welt-Strahlungssystem: RAPTOR = bisheriges Chunk-System (Standard), ADVANCED = Port des
     * NTM-Next-Feldes (com.hbm_m.radiation.ntmnext). Wird beim Serverstart uebernommen.
     */
    public RadiationSystemMode radiationSystem = RadiationSystemMode.RAPTOR;

    public enum RadiationSystemMode {
        RAPTOR,
        ADVANCED
    }

    // ════════════════════════════════════════════════════════════════
    // Verschmutzung (1.7.10 RadiationConfig / MobConfig)
    // ════════════════════════════════════════════════════════════════
    /** Original: RadiationConfig.enablePollution - schaltet das gesamte Verschmutzungsraster. */
    public boolean enablePollution = true;
    /** Original: MobConfig.pollutionMult - Faktor auf jeden Eintrag ins Raster. */
    public double pollutionMult = 1.0D;
    /** Original: RadiationConfig.buffMobThreshold - ab so viel Russ spawnen verstaerkte Monster. */
    public float buffMobThreshold = 50F;
    /** Original: RadiationConfig.enablePoison - Giftwirkung auf Kreaturen in der Zelle. */
    public boolean enablePoison = true;
    /** Original: RadiationConfig.enableLeadPoisoning - Bleivergiftung durch Schwermetall in der Luft. */
    public boolean enableLeadPoisoning = true;
    /** Original: RadiationConfig.enableLeadFromBlocks - Bleivergiftung beim Abbauen in belasteten Zellen. */
    public boolean enableLeadFromBlocks = true;

    // ════════════════════════════════════════════════════════════════
    // Glyphiden (1.7.10 MobConfig, Werte wie nach dem Laden der Originalkonfig)
    // ════════════════════════════════════════════════════════════════
    /** Original GeneralConfig.enableVirus (Konfigvorgabe false): Dunkelkristall breitet sich aus. */
    public boolean enableVirus = false;
    /** Original GeneralConfig.enableCataclysm: bei jedem Tod stuerzt ein Satellit ab. */
    public boolean enableCataclysm = false;
    /** Original MobConfig 12.M*: Maskman (Verzoegerung in Weltsekunden). */
    public boolean enableMaskman = true;
    public int maskmanDelay = 20 * 60;
    public int maskmanMinRad = 50;
    public boolean maskmanUnderground = true;
    /** Original MobConfig 12.E*: Strahlungs-Elementare nach Kernschmelzen. */
    public boolean enableElementals = true;
    public int elementalDelay = 30 * 60 * 60;
    public int elementalChance = 2;
    public int elementalAmount = 10;
    public int elementalDistance = 32;
    /** Original MobConfig.enableRaids und 12.F*: FBI-Razzien. */
    public boolean enableRaids = false;
    public int raidDelay = 30 * 60 * 60;
    public int raidChance = 3;
    public int raidAmount = 15;
    public int raidDrones = 5;
    public int raidAttackDelay = 40;
    public int raidAttackReach = 2;
    public int raidAttackDistance = 32;
    public boolean enableHives = true;
    public int hiveSpawn = 256;
    /** Original WorldConfig 4.21_capsuleSpawn: Landekapsel am Strand in jedem n-ten Chunk (0 = aus). */
    public int capsuleStructure = 100;
    /** Original GeneralConfig 1.14_enableBomberShortMode: Bomber erscheinen naeher am Ziel. */
    public boolean enableBomberShortMode = false;
    public float scoutThreshold = 1F;
    public int scoutSwarmSpawnChance = 3;
    public boolean waypointDebug = false;
    public int largeHiveChance = 5;
    public int largeHiveThreshold = 20;
    public int swarmCooldown = 120;
    public int baseSwarmSize = 5;
    public float swarmScalingMult = 1.2F;
    public int sootStep = 50;
    public float spawnMax = 50F;
    public boolean enableInfestation = true;
    public float baseInfestChance = 5F;
    public float targetingThreshold = 1F;
    public boolean rampantMode = false;
    public boolean rampantNaturalScoutSpawn = false;
    public float rampantScoutSpawnThresh = 13F;
    public int rampantScoutSpawnChance = 1400;
    public boolean scoutInitialSpawn = false;
    public boolean rampantExtendedTargetting = false;
    public boolean rampantDig = false;
    /** Original MobConfig 12.D02/12.D03: Skelette mit Schusswaffen bei hohem Russ. */
    public boolean enableMobWeapons = true;
    public double mobWeaponSootReduction = 0D;
    public boolean rampantGlyphidGuidance = false;
    public float rampantSmokeStackOverride = 0.4F;
    /** Basischance, Russmodifikator, Mindestruss je Art (nicht in der Konfigdatei, Werte der Originalkonfig). */
    public int[] glyphidChance = {50, -45, 0};
    public int[] brawlerChance = {10, 30, 1};
    public int[] bombardierChance = {20, -15, 1};
    public int[] blasterChance = {-5, 40, 5};
    public int[] diggerChance = {-15, 25, 5};
    public int[] behemothChance = {-30, 45, 10};
    public int[] brendaChance = {-50, 60, 20};
    public int[] johnsonChance = {-50, 60, 50};

    /** MOTD при входе в мир и уведомление о новой версии на Modrinth (ориг. GeneralConfig.enableMOTD). */
    public boolean enableMOTD = true;

    /** Логирование использования детонатора, взрывов, пусков ракет (ориг. GeneralConfig.enableExtendedLogging). */
    public boolean enableExtendedLogging = false;

    /**
     * Центральный тумблер 528-режима (NEXT ContentConfig {@code mode_528.enabled} →
     * NEXT {@code BalanceConfig.enable528}); зеркалируется в
     * {@link GeneralConfig#enable528} через {@code GeneralConfig.syncFrom}.
     */
    public boolean enable528 = false;

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
    /** Original WeaponConfig.dropCrys: Xen-Kristalle verschieben beim Fallenlassen Bloecke. */
    public boolean dropCrys = true;
    /** Original WeaponConfig.dropDead: Totmann-Sprengsatz explodiert beim Fallenlassen. */
    public boolean dropDead = true;

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

    public enum OcclusionCullingMode {
        GPU, CPU, OFF
    }

    public OcclusionCullingMode occlusionCullingMode = OcclusionCullingMode.GPU;

    /**
     * Legacy boolean preserved for backwards compatibility with existing client.json and external callers.
     * @deprecated Use {@link #occlusionCullingMode} and {@link #getEffectiveOcclusionCullingMode()} instead.
     */
    @Deprecated
    public boolean enableOcclusionCulling = false;

    /**
     * Resolves the effective occlusion culling mode based on user setting and hardware capabilities.
     * If configured to GPU but hardware does not support OpenGL 4.3 compute shaders, automatically
     * falls back to OFF. Manual CPU selection is always honored.
     */
    public OcclusionCullingMode getEffectiveOcclusionCullingMode() {
        if (occlusionCullingMode == OcclusionCullingMode.GPU) {
            return GpuCullingCapability.isSupported() ? OcclusionCullingMode.GPU : OcclusionCullingMode.OFF;
        }
        return occlusionCullingMode;
    }

    /**
     * Инстансинг, MDI и GPU-bone skinning включаются автоматически (переключателей больше нет).
     * Резервный тумблер: принудительно рисовать все OBJ-станки ванильным immediate-путём
     * (putBulkData) — если наш VBO-рендер сломался (белые/невидимые модели, артефакты).
     */
    public boolean forceVanillaImmediatePath = false;

    /** После каждого MDI-dispatch: одна строка INFO (число sub-draw, инстансов, атлас). */
    public boolean mdiDebugLogDispatch = false;

    /** Плюс по строке INFO на каждую MDI-команду (тег части, baseInstance и т.д.). */
    public boolean mdiVerboseSubdraws = false;

    /**
     * Переиспользование чистых MDI-рендереров между кадрами: рендерер, не изменивший
     * ни одной записи инстансов (skip-write в addInstance), не копируется в снапшот
     * и не проходит span-дифф — координатор переиспользует прошлокадровые партицию,
     * fade-кэши и окна аплоада. Kill-switch на случай регрессии картинки.
     */
    public boolean mdiCleanFrameReuse = true;

    /**
     * Fast-path dirty-skip: BlockEntity с {@code RenderDirtyTracker} (базовые машины)
     * не пересобираются каждый кадр — матрицы/свет/сравнение записей выполняются
     * только для грязных машин (onLoad/смена state/серверный update-пакет) и
     * периодического TTL-освежения света/fade (~0.75-1.1 с, размазано по позициям).
     * Требует GPU-куллинга (фрустум/окклюзию берёт на себя compute-куллер).
     * Kill-switch: {@code -Dhbm.dirtySkip=false}.
     */
    public boolean nucleusRenderDirtySkip = true;

    /**
     * Кэш дельт анимаций: аниматор части исполняется один раз на кадр, второй
     * проход (main/shadow) берёт готовую дельту. Kill-switch регрессии картинки.
     * Аварийный JVM-флаг: {@code -Dhbm.animCache=false} (вынуждает OFF).
     */
    public boolean nucleusAnimCache = true;

    /**
     * Параметрическая GPU-анимация (attrib 15 AnimParams): вращение/сдвиг деталей
     * считается в вершинном шейдере, CPU пишет только параметры сустава —
     * работающая машина льёт 4 флоата на инстанс, стоящая — ноль. Выключить при
     * артефактах анимации (детали на месте/улетели). Флаг: {@code -Dhbm.parametricAnim=false}.
     */
    public boolean nucleusParametricAnim = true;

    /**
     * Dither-fade (screen-door) вместо альфа-блендинга дистанционного фейда:
     * фрагментный шейдер отсекает пиксели по статическому экранному хешу,
     * блендинг и back-to-front сортировка фейд-хвоста не нужны вовсе —
     * фейд-фаза схлопывается в непрозрачную пачку (ноль CPU-сортировки и
     * CPU-партиционирования). Цена: проявление «зерном» в переходной полосе.
     */
    public boolean nucleusDitherFade = false;

    /**
     * Глобальный теневой батч под Iris: все части теневого прохода уходят одной
     * пачкой инстансных дроуков вместо per-BE отрисовки. Флаг: {@code -Dhbm.shadowInstancing=false}.
     */
    public boolean nucleusShadowInstancing = true;

    /**
     * Шеринг скинов дверей: один VBO на геометрию, скин — per-instance uvRect.
     * Выключить при неправильных текстурах дверей (переключит в per-skin VBO).
     * Флаг: {@code -Dhbm.doorSkinSharing=false}.
     */
    public boolean nucleusDoorSkinSharing = true;

    /**
     * Покадровое скрытие частей дверей при открывании (куллинг в корпус): у blast door
     * сегменты створки исчезают по мере входа в корпус (порт RenderBlastDoor).
     * Выключите, если части двери пропадают раньше времени — все двери будут
     * рисоваться целиком всю анимацию.
     * Флаг: {@code -Dhbm.doorPartCulling=false}.
     */
    public boolean doorPartCulling = true;

    /**
     * Графические эффекты - вложенный объект группы «Графические эффекты»
     * (аккордеон во вкладке Rendering). Имя поля = parentObject полей схемы
     * graphicEffects.* (ConfigField.resolveTarget резолвит его рефлексией).
     */
    public GraphicEffectsSettings graphicEffects = new GraphicEffectsSettings();

    /** Поля группы «Графические эффекты»; ключи схемы - graphicEffects.<имя поля>. */
    public static class GraphicEffectsSettings {
        /**
         * Позиционный свет ядерного взрыва (RawScreenSpaceLight: экранный пасс
         * поверх картинки шейдерпака, инъекция в Iris/Oculus finalizeLevelRendering).
         * Выключите для A/B-проверки артефактов или аварийного отключения; ванильный
         * рендер без пака не затрагивается. Флаг: {@code -Dhbm.screenSpaceLight=false}.
         */
        public boolean screenSpaceLightFill = true;

        /**
         * Интенсивность позиционного света ядерного взрыва (аддитив после тонемапа
         * пака). Ориентиры: 1.5 едва заметно в ночном мире, 50 - белая сатурация
         * всего радиуса на всё время жизни гриба.
         */
        public float nukeLightIntensity = 4.0F;

        /**
         * Модификатор РАДИУСА ореола (независимо от яркости): итоговый радиус
         * источника = базовый (растёт с масштабом гриба, 384..3840 блоков) *
         * это значение. Меньше 1 - компактный ореол, больше 1 - зарево на
         * весь горизонт.
         */
        public float nukeLightRadiusScale = 1.0F;

        /**
         * Кинематографические эффекты ядерного света (RawScreenSpaceLight):
         * альбедо-освещение поверхностей (текстура сцены сохраняет рельеф),
         * спектральный цвет вспышки (белое ядро → янтарь → багровое тление),
         * Mie-рассеяние ореола, экранные godrays за рельефом, преломляющая
         * ударная волна с хроматической аберрацией. 0 - классический
         * аддитивный свет (round-8), 1 - штатная сила, до 2 - утрированно.
         */
        public float nukeCinematics = 1.0F;

        /** Режим рендера тепловизора. */
        public ThermalRenderMode thermalRenderMode = ThermalRenderMode.FULL_SHADER;
    }

    /**
     * Одноразовый дебаг-дамп клипа дверей в лог ('[HBM-M][DoorClipDebug]'):
     * почему резолвер вернул/не вернул плоскости и какие спрайты/UV реально
     * приходят в клип-тир. Для диагностики артефактов скрытия створок.
     */
    public boolean doorClipDebug = false;


    /**
     * Байпас диспетчера BE: машины фабрики собираются плоским обходом до
     * Sodium/ванильного обхода BlockEntity. Флаг: {@code -Dhbm.dispatcherBypass=false}.
     */
    public boolean nucleusDispatcherBypass = true;

    /**
     * Tier 1 GPU-bake под Iris: compute-шейдер запекает инстансы в вершинный буфер
     * (один draw на part). Требует OpenGL 4.3; без поддержки автоматически
     * деградирует в Tier 2/3. Флаг: {@code -Dhbm.gpuBake=false}.
     */
    public boolean nucleusGpuBake = true;

    /**
     * Окклюзионная фаза bake-cull (Tier 1 под Iris) по Hi-Z пирамиде глубины
     * (переиспользует клиентский HiZDepthPyramid). Строго опционально: при false
     * cull остаётся frustum-only (прежнее поведение); при недоступной глубине
     * — тихий откат на frustum-only (на кадр), ошибка GL отключает фичу до
     * перезапуска с одним логом.
     */
    public boolean nucleusBakeHiZ = false;

    /**
     * Compute-скаттер массовых аплоадов инстансов (один dispatch вместо N
     * копий). Требует OpenGL 4.3 и не-Intel GPU; иначе DMA-путь.
     * Флаг: {@code -Dhbm.gpuScatter=false}.
     */
    public boolean nucleusGpuScatter = true;

    /**
     * Прямой glBufferSubData вместо staging-кольца при аплоаде спанов (A/B для
     * UMA-встроек, где GPU-GPU копия = лишний memcpy). Флаг: {@code -Dhbm.stagingDirect=true}.
     */
    public boolean nucleusStagingDirect = false;

    /**
     * Порог общего числа MDI-инстансов, выше которого GPU-culler включает
     * окклюзию (Hi-Z rebuild фиксированно дорог; гистерезис 2:1).
     * Флаг: {@code -Dhbm.gpuCull.minInstances=N}.
     */
    public int nucleusGpuCullMinInstances = 4096;

    /**
     * Минимум изменившихся span'ов в окне, при котором аплоад уходит в
     * compute-скаттер вместо последовательных копий.
     * Флаг: {@code -Dhbm.gpuScatter.minSpans=N}.
     */
    public int nucleusGpuScatterMinSpans = 16;

    /**
     * Общий бюджет VRAM для per-renderer bake-кэшей Nucleus (Tier 1: output + record +
     * control SSBO; heap-снимок записей учитывается по факту). Кэш, не влезающий в
     * бюджет после LRU-выселения, остаётся на immediate-пути — бюджет не может
     * трэшиться. Значение &lt;= 0 отключает кэширование; эффективный бюджет клэмпится
     * в 16..1024 МБ. Флаг: {@code -Dhbm.bakeCacheMB=N}.
     */
    public int nucleusBakeCacheMB = 192;

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
    /** Алгоритм взрыва: 0 = Legacy (однопоточный; работает ТОЛЬКО по уже загруженным чанкам, принудительной загрузки нет), 1 = Threaded DDA (асинхронная загрузка чанков, рекомендуется), 2 = Threaded DDA с накоплением урона. Ориг. ключ: 6.06_explosionAlgorithm (в оригинале дефолт 2). */
    public int explosionAlgorithm = 1;
    /** Лимит жизни невыгруженного взрыва в секундах; 0 = без лимита (ориг. BombConfig.limitExplosionLifespan, ключ 6.00). */
    public int limitExplosionLifespan = 0;
    /** Кап одновременно загружаемых взрывом чанков; 0 = авто (max(64, ядра*8)). */
    public int blastChunksInFlight = 0;
    /** Максимум потоков ForkJoinPool взрывов; <=0 = ядра + значение (по умолчанию -1). */
    public int bombMaxThreads = -1;
    /** Сохранять состояние взрыва MK5 в NBT — после перезахода взрыв продолжается с места остановки (ориг. BombConfig.enableNukeNBTSaving). */
    public boolean enableNukeNBTSaving = true;

    // ════════════════════════════════════════════════════════════════
    // Тепловизор (переключатель - в graphicEffects.thermalRenderMode)
    // ════════════════════════════════════════════════════════════════

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

    /**
     * GL debug callback (KHR_debug): HIGH/MEDIUM сообщения драйвера в лог.
     * Вступает в силу после перезапуска. Флаг: {@code -Dhbm.glDebug=0} или env HBM_GL_DEBUG=0.
     */
    public boolean glDebugOutput = true;

    /** Периодический лог статистики кэша анимаций Nucleus (раз в 5 с, при включённом). */
    public boolean debugAnimCacheLog = false;

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
        return !INSTANCE.forceVanillaImmediatePath;
    }
}
