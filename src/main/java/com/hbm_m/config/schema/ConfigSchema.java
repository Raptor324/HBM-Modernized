package com.hbm_m.config.schema;

import com.hbm_m.config.ModClothConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Реестр всех конфигурационных полей мода (единый источник правды).
 *
 * <p>Заменяет аннотации Cloth Config ({@code @Category/@Gui/@BoundedDiscrete})
 * и является основой для:
 * <ul>
 *   <li>сериализации (client.json / server.json),</li>
 *   <li>сетевой синхронизации (какие поля шлются S2C и принимаются C2S),</li>
 *   <li>построения GUI (какие виджеты, границы, категории, вкладки),</li>
 *   <li>пометок requiresRestart.</li>
 *   <li>Всегда следить, чтобы имя поля в POJO-классе буква в букву совпадало со строковым ключом при регистрации в ConfigSchema!</li>
 * </ul>
 *
 * <p>Порядок регистрации определяет порядок категорий/полей в GUI и в файле.
 */
public final class ConfigSchema {

    private static final LinkedHashMap<String, ConfigField> FIELDS = new LinkedHashMap<>();
    /** Standardwerte statisch gebundener Felder, festgehalten bei der Registrierung (vor jedem Laden). Muss vor dem static-Block stehen. */
    private static final Map<String, String> STATIC_DEFAULTS = new LinkedHashMap<>();
    private static volatile ModClothConfig defaults;

    static {
        register();
    }

    private ConfigSchema() {}

    // ================================================================
    // Регистрация всех полей ModClothConfig
    // ================================================================

    private static void register() {
        // ── SERVER: общие ───────────────────────────────────────────
        reg(ConfigField.bool("enableRadiation", ConfigSide.SERVER, ApplyMode.LIVE, "general").withComment("Enables / disables global radiation system"));
        reg(ConfigField.bool("enableCataclysm", ConfigSide.SERVER, ApplyMode.LIVE, "general").withComment("Causes satellites to fall whenever a mob dies"));
        reg(ConfigField.bool("enableVirus", ConfigSide.SERVER, ApplyMode.LIVE, "general").withComment("Allows virus blocks to spread"));
        reg(ConfigField.bool("enableMaskman", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether mask man should spawn"));
        reg(ConfigField.integer("maskmanDelay", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("How many world seconds need to pass for mask man to spawn, if the requirements are met"));
        reg(ConfigField.integer("maskmanMinRad", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0, Integer.MAX_VALUE).withComment("The amount of radiation needed for mask man to spawn"));
        reg(ConfigField.bool("maskmanUnderground", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether players need to be underground for mask man to spawn"));
        reg(ConfigField.bool("enableElementals", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether there should be radiation elementals"));
        reg(ConfigField.integer("elementalDelay", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("How many world ticks need to pass for a check to be performed"));
        reg(ConfigField.integer("elementalChance", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("1:x chance to spawn elementals, must be at least 1"));
        reg(ConfigField.integer("elementalAmount", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0, 1000).withComment("How many elementals are spawned each raid"));
        reg(ConfigField.integer("elementalDistance", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0, 1000).withComment("How far away elementals will spawn from the targeted player"));
        reg(ConfigField.bool("enableRaids", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether there should be FBI raids"));
        reg(ConfigField.integer("raidDelay", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("How many world ticks need to pass for a check to be performed"));
        reg(ConfigField.integer("raidChance", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("1:x chance to spawn a raid, must be at least 1"));
        reg(ConfigField.integer("raidAmount", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0, 1000).withComment("How many FBI agents are spawned each raid"));
        reg(ConfigField.integer("raidAttackDelay", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("Time between individual attempts to break machines"));
        reg(ConfigField.integer("raidAttackReach", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0, 100).withComment("How far away machines can be broken"));
        reg(ConfigField.integer("raidAttackDistance", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0, 1000).withComment("How far away agents will spawn from the targeted player"));
        reg(ConfigField.integer("raidDrones", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0, 1000).withComment("How many quadcopter drones are spawned each raid"));
        reg(ConfigField.bool("enableHives", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether glyphid hives should spawn"));
        reg(ConfigField.integer("hiveSpawn", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("The average amount of chunks per hive"));
        reg(ConfigField.bool("enableBomberShortMode", ConfigSide.SERVER, ApplyMode.LIVE, "general").withComment("Has bomber planes spawn in closer to the target for use with smaller render distances"));
        reg(ConfigField.integer("capsuleStructure", ConfigSide.SERVER, ApplyMode.LIVE, "general", 0, Integer.MAX_VALUE).withComment("Spawn landing capsule on every nTH chunk"));
        reg(ConfigField.floatNum("scoutThreshold", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0F, 100000F).withComment("Minimum amount of soot for scouts to spawn"));
        reg(ConfigField.floatNum("spawnMax", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0F, 100000F).withComment("Maximum amount of glyphids being able to exist at once through natural spawning"));
        reg(ConfigField.floatNum("targetingThreshold", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0F, 100000F).withComment("Minimum amount of soot required for glyphids' extended targeting range to activate"));
        reg(ConfigField.integer("scoutSwarmSpawnChance", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("How likely are scouts to spawn in swarms, 1 in x chance format"));
        reg(ConfigField.integer("largeHiveChance", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("The chance for a large hive to spawn, formula: 1/x"));
        reg(ConfigField.integer("largeHiveThreshold", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0, Integer.MAX_VALUE).withComment("The soot threshold for a large hive to spawn"));
        reg(ConfigField.bool("waypointDebug", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Allows glyphid waypoints to be seen, mainly used for debugging, also useful as an aid against them"));
        reg(ConfigField.bool("enableInfestation", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether structures infested with glyphids should spawn"));
        reg(ConfigField.floatNum("baseInfestChance", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0F, 100F).withComment("The chance for infested structures to spawn"));
        reg(ConfigField.integer("baseSwarmSize", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0, 1000).withComment("The basic, soot-less swarm size"));
        reg(ConfigField.floatNum("swarmScalingMult", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0F, 1000F).withComment("By how much should swarm size scale by per soot amount determined below"));
        reg(ConfigField.integer("sootStep", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("The soot amount the above multiplier applies to the swarm size"));
        reg(ConfigField.integer("swarmCooldown", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("How often do glyphid swarms spawn, in seconds"));
        reg(ConfigField.bool("rampantMode", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("The main rampant mode toggle, enables all other features associated with it"));
        reg(ConfigField.bool("rampantNaturalScoutSpawn", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether scouts should spawn natually in highly polluted chunks"));
        reg(ConfigField.floatNum("rampantScoutSpawnThresh", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0F, 100000F).withComment("How much soot is needed for scouts to naturally spawn"));
        reg(ConfigField.integer("rampantScoutSpawnChance", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 1, Integer.MAX_VALUE).withComment("How often scouts naturally spawn per mob population, 1/x format, the bigger the number, the more uncommon the scouts"));
        reg(ConfigField.bool("rampantExtendedTargetting", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether Glyphids should have the extended targetting always enabled"));
        reg(ConfigField.bool("rampantDig", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether Glyphids should be able to dig to waypoints"));
        reg(ConfigField.bool("rampantGlyphidGuidance", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether Glyphids should always expand toward a player's spawnpoint"));
        reg(ConfigField.floatNum("rampantSmokeStackOverride", ConfigSide.SERVER, ApplyMode.LIVE, "mobs", 0F, 10F).withComment("How much should the smokestack multiply soot by when on rampant mode"));
        reg(ConfigField.bool("scoutInitialSpawn", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether glyphid scouts should be able to spawn on the first swarm of a hive, causes glyphids to expand significantly faster"));
        reg(ConfigField.bool("enableMobWeapons", ConfigSide.SERVER, ApplyMode.LIVE, "mobs").withComment("Whether skeletons should have bows replaced with guns when spawning at higher soot levels"));
        reg(ConfigField.builder("mobWeaponSootReduction", ConfigField.FieldType.DOUBLE).side(ConfigSide.SERVER).applyMode(ApplyMode.LIVE).category("mobs").range(-100000D, 100000D).build().withComment("Reduces the amount of soot needed for skeleton guns to appear"));
        reg(ConfigField.bool("enableMeteorStrikes", ConfigSide.SERVER, ApplyMode.LIVE, "meteor").withComment("Toggles the spawning of meteors"));
        reg(ConfigField.bool("enableMycelium", ConfigSide.SERVER, ApplyMode.LIVE, "general").withComment("Allows glowing mycelium to spread"));
        reg(ConfigField.bool("cleanupDeadDirt", ConfigSide.SERVER, ApplyMode.LIVE, "world_effects").withComment("Whether dead grass and mycelium should decay into dirt"));
        reg(ConfigField.bool("enable528ColtanDeposit", ConfigSide.SERVER, ApplyMode.LIVE, "528").withComment("Enables the coltan deposit. A large amount of coltan will spawn around a single random location in the world."));
        reg(ConfigField.bool("enableMachineGravity", ConfigSide.SERVER, ApplyMode.LIVE, "general").withComment("Requires large large machines to have a proper foundation, or else they tilt and break. Independent of 528 mode."));
        reg(ConfigField.bool("enable528MachineGravity", ConfigSide.SERVER, ApplyMode.LIVE, "528").withComment("Requires most large machines to have a proper foundation, or else they tilt and break."));
        reg(ConfigField.bool("enable528ColtanSpawn", ConfigSide.SERVER, ApplyMode.LIVE, "528").withComment("Enables coltan ore as a random spawn in the world. Unlike the deposit option, coltan will not just spawn in one central location."));
        reg(ConfigField.integer("coltanRate", ConfigSide.SERVER, ApplyMode.LIVE, "528", 0, Integer.MAX_VALUE).withComment("Determines how many coltan ore veins are to be expected in a chunk. These values do not affect the frequency in deposits, and only apply if random coltan spawning is enabled."));
        reg(ConfigField.bool("enableMeteorShowers", ConfigSide.SERVER, ApplyMode.LIVE, "meteor").withComment("Toggles meteor showers, which start with a 1% chance for every spawned meteor"));
        reg(ConfigField.bool("enableMeteorTails", ConfigSide.SERVER, ApplyMode.LIVE, "meteor").withComment("Toggles the particle effect created by falling meteors"));
        reg(ConfigField.bool("enableSpecialMeteors", ConfigSide.SERVER, ApplyMode.LIVE, "meteor").withComment("Toggles rare, special meteor types with different impact effects"));
        reg(ConfigField.integer("meteorStrikeChance", ConfigSide.SERVER, ApplyMode.LIVE, "meteor", 1, Integer.MAX_VALUE).withComment("The probability of a meteor spawning (an average of once every nTH ticks)"));
        reg(ConfigField.integer("meteorShowerChance", ConfigSide.SERVER, ApplyMode.LIVE, "meteor", 1, Integer.MAX_VALUE).withComment("The probability of a meteor spawning during meteor shower (an average of once every nTH ticks)"));
        reg(ConfigField.integer("meteorShowerDuration", ConfigSide.SERVER, ApplyMode.LIVE, "meteor", 0, Integer.MAX_VALUE).withComment("Max duration of meteor shower in ticks"));
        reg(ConfigField.bool("renderRebarSimple", ConfigSide.CLIENT, ApplyMode.LIVE, "general").withComment("Renders rebar with only three bars (needs a resource reload)"));
        reg(ConfigField.bool("renderReeds", ConfigSide.CLIENT, ApplyMode.LIVE, "general").withComment("Renders reeds all the way down to the ground (needs a chunk reload)"));
        reg(ConfigField.bool("gunAnimsLegacy", ConfigSide.CLIENT, ApplyMode.LIVE, "general").withComment("Uses the legacy gun animations"));
        reg(ConfigField.bool("gunModelFov", ConfigSide.CLIENT, ApplyMode.LIVE, "general").withComment("Gun models use the FOV setting instead of a fixed 70"));
        reg(ConfigField.bool("gunVisualRecoil", ConfigSide.CLIENT, ApplyMode.LIVE, "general").withComment("Guns shake the camera when fired"));
        reg(ConfigField.floatNum("gunAnimationSpeed", ConfigSide.CLIENT, ApplyMode.LIVE, "general", 0.01F, 10F).withComment("Multiplier for gun animation speed"));
        reg(ConfigField.bool("enableGuns", ConfigSide.SERVER, ApplyMode.LIVE, "general").withComment("Prevents new system guns to be fired"));
        reg(ConfigField.integer("renderRebarLimit", ConfigSide.CLIENT, ApplyMode.LIVE, "general", 0, 100000).withComment("How many rebar blocks may render their concrete fill per frame"));
        reg(ConfigField.bool("enableChunkRads", ConfigSide.SERVER, ApplyMode.LIVE, "general"));
        // Wird nur beim Serverstart gelesen (RadiationSystemSelector) -> Welt neu laden.
        reg(ConfigField.enumField("radiationSystem", ConfigSide.SERVER, ApplyMode.REQUIRES_RESTART, "general"));
        reg(ConfigField.bool("enableMOTD", ConfigSide.CLIENT, ApplyMode.LIVE, "general"));

        // ── SERVER: эффекты мира ────────────────────────────────────
        reg(ConfigField.bool("enableRadFogEffect", ConfigSide.SERVER, ApplyMode.LIVE, "world_effects"));
        reg(ConfigField.bool("worldRadEffects", ConfigSide.SERVER, ApplyMode.LIVE, "world_effects"));
        reg(ConfigField.bool("taintTrails", ConfigSide.SERVER, ApplyMode.LIVE, "world_effects"));

        // ── SERVER: кратерные биомы (ориг. WorldConfig, категория CATEGORY_BIOMES) ──
        reg(ConfigField.bool("enableCraterBiomes", ConfigSide.SERVER, ApplyMode.LIVE, "world_effects").withComment("Enables the biome change caused by nuclear explosions"));
        reg(ConfigField.floatNum("craterBiomeInnerRad", ConfigSide.SERVER, ApplyMode.LIVE, "world_effects", 0F, 10_000F));
        reg(ConfigField.floatNum("craterBiomeRad", ConfigSide.SERVER, ApplyMode.LIVE, "world_effects", 0F, 10_000F));
        reg(ConfigField.floatNum("craterBiomeOuterRad", ConfigSide.SERVER, ApplyMode.LIVE, "world_effects", 0F, 10_000F));
        reg(ConfigField.floatNum("craterBiomeWaterMult", ConfigSide.SERVER, ApplyMode.LIVE, "world_effects", 0F, 100F));

        // ── SERVER: оружие / падение предметов ──────────────────────
        reg(ConfigField.bool("dropSingularity", ConfigSide.SERVER, ApplyMode.LIVE, "weapons"));
        reg(ConfigField.bool("dropCell", ConfigSide.SERVER, ApplyMode.LIVE, "weapons"));

        // ── SERVER: игрок (радиация) ────────────────────────────────
        reg(ConfigField.floatNum("maxPlayerRad", ConfigSide.SERVER, ApplyMode.LIVE, "player", 1F, 100_000F));
        reg(ConfigField.floatNum("radDecay", ConfigSide.SERVER, ApplyMode.LIVE, "player", 0F, 1_000F));
        reg(ConfigField.floatNum("radDamage", ConfigSide.SERVER, ApplyMode.LIVE, "player", 0F, 1_000F));
        reg(ConfigField.floatNum("radDamageThreshold", ConfigSide.SERVER, ApplyMode.LIVE, "player", 0F, 100_000F));
        reg(ConfigField.integer("radSickness", ConfigSide.SERVER, ApplyMode.LIVE, "player", 0, 100_000));
        reg(ConfigField.integer("radWater", ConfigSide.SERVER, ApplyMode.LIVE, "player", 0, 100_000));
        reg(ConfigField.integer("radConfusion", ConfigSide.SERVER, ApplyMode.LIVE, "player", 0, 100_000));
        reg(ConfigField.integer("radBlindness", ConfigSide.SERVER, ApplyMode.LIVE, "player", 0, 100_000));

        // ── SERVER: чанк-радиация ───────────────────────────────────
        // maxRad захватывается в static final (см. ChunkRadiationHandlerSimple / ChunkRadiation) → REQUIRES_RESTART
        reg(ConfigField.floatNum("maxRad", ConfigSide.SERVER, ApplyMode.REQUIRES_RESTART, "chunk", 1F, 10_000_000F));
        reg(ConfigField.floatNum("radChunkDecay", ConfigSide.SERVER, ApplyMode.LIVE, "chunk", 0F, 10_000F));
        reg(ConfigField.floatNum("radChunkSpreadFactor", ConfigSide.SERVER, ApplyMode.LIVE, "chunk", 0F, 100F));
        reg(ConfigField.floatNum("radSpreadThreshold", ConfigSide.SERVER, ApplyMode.LIVE, "chunk", 0F, 1_000F));
        reg(ConfigField.floatNum("minRadDecayAmount", ConfigSide.SERVER, ApplyMode.LIVE, "chunk", 0F, 1_000F));
        reg(ConfigField.floatNum("radRandomizationFactor", ConfigSide.SERVER, ApplyMode.LIVE, "chunk", 0F, 1F));

        // ── SERVER: машины ──────────────────────────────────────────
        reg(ConfigField.boolNested("machineRadar.generateChunks", ConfigSide.SERVER, ApplyMode.REQUIRES_RESTART, "machines", "machineRadar"));
        reg(ConfigField.longNested("frackingTower.maxPower", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 0L, 100_000_000_000L));
        reg(ConfigField.longNested("frackingTower.consumption", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 0L, 100_000_000L));
        reg(ConfigField.intNested("frackingTower.solutionRequired", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 1, 10_000));
        reg(ConfigField.intNested("frackingTower.delay", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 1, 1_200));
        reg(ConfigField.intNested("frackingTower.oilPerDeposit", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 1, 64_000));
        reg(ConfigField.intNested("frackingTower.gasPerDepositMin", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 1, 64_000));
        reg(ConfigField.intNested("frackingTower.gasPerDepositMax", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 1, 64_000));
        reg(ConfigField.doubleNested("frackingTower.drainChance", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 0D, 1D));
        reg(ConfigField.intNested("frackingTower.oilPerBedrockDeposit", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 1, 64_000));
        reg(ConfigField.intNested("frackingTower.gasPerBedrockDepositMin", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 1, 64_000));
        reg(ConfigField.intNested("frackingTower.gasPerBedrockDepositMax", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 1, 64_000));
        reg(ConfigField.intNested("frackingTower.destructionRange", ConfigSide.SERVER, ApplyMode.LIVE, "machines", "frackingTower", 1, 256));

        // ── SERVER: радиусы ядерных устройств ───────────────────────
        reg(ConfigField.integer("gadgetRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("boyRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("manRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("mikeRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("tsarRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("prototypeRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("fleijaRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("soliniumRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("n2Radius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("missileRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("mirvRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("fatmanRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("nukaRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));
        reg(ConfigField.integer("aSchrabRadius", ConfigSide.SERVER, ApplyMode.LIVE, "nukes", 10, 1500));

        // ── SERVER: двигатель взрывов ───────────────────────────────
        reg(ConfigField.integer("mk5TickTimeMs", ConfigSide.SERVER, ApplyMode.LIVE, "explosions", 0, 1000));
        reg(ConfigField.integer("blastSpeed", ConfigSide.SERVER, ApplyMode.LIVE, "explosions", 1, 8192));
        reg(ConfigField.integer("falloutRangePercent", ConfigSide.SERVER, ApplyMode.LIVE, "explosions", 0, 500));
        reg(ConfigField.integer("falloutDelay", ConfigSide.SERVER, ApplyMode.LIVE, "explosions", 0, 100));
        reg(ConfigField.bool("enableChunkLoading", ConfigSide.SERVER, ApplyMode.LIVE, "explosions"));
        reg(ConfigField.integer("explosionAlgorithm", ConfigSide.SERVER, ApplyMode.LIVE, "explosions", 0, 2));
        reg(ConfigField.integer("limitExplosionLifespan", ConfigSide.SERVER, ApplyMode.LIVE, "explosions", 0, 3600));
        reg(ConfigField.bool("enableNukeNBTSaving", ConfigSide.SERVER, ApplyMode.LIVE, "explosions"));

        // ── SERVER: сетевая трассировка ракет ───────────────────────
        reg(ConfigField.bool("enableMissileNetworkTrack", ConfigSide.SERVER, ApplyMode.LIVE, "missile_track"));
        reg(ConfigField.integer("missileTrackMaxRangeBlocks", ConfigSide.SERVER, ApplyMode.LIVE, "missile_track", 0, 500_000));
        reg(ConfigField.integer("missileTrackInterval", ConfigSide.SERVER, ApplyMode.LIVE, "missile_track", 1, 20));

        // ── SERVER: отладка (читается обеими сторонами → синхронизируется) ──
        reg(ConfigField.bool("enableDebugRender", ConfigSide.SERVER, ApplyMode.LIVE, "debug"));
        reg(ConfigField.bool("debugRenderInSurvival", ConfigSide.SERVER, ApplyMode.LIVE, "debug"));
        reg(ConfigField.bool("enableDebugLogging", ConfigSide.SERVER, ApplyMode.LIVE, "debug"));

        // ── CLIENT: рендеринг ───────────────────────────────────────
        reg(ConfigField.integer("modelUpdateDistance", ConfigSide.CLIENT, ApplyMode.LIVE, "rendering", 0, 20));
        reg(ConfigField.integer("modelStaticRenderDistance", ConfigSide.CLIENT, ApplyMode.LIVE, "rendering", 1, 20));
        reg(ConfigField.bool("enableOcclusionCulling", ConfigSide.CLIENT, ApplyMode.LIVE, "rendering"));
        reg(ConfigField.bool("instanceVboOrphanBeforeUpload", ConfigSide.CLIENT, ApplyMode.LIVE, "rendering"));
        // Инстансинг/MDI/GPU-bone skinning всегда включены; forceVanillaImmediatePath — резервный
        // ручной перевод всех OBJ-станков на ванильный immediate-путь (putBulkData).
        reg(ConfigField.bool("forceVanillaImmediatePath", ConfigSide.CLIENT, ApplyMode.LIVE, "rendering"));
        reg(ConfigField.bool("mdiDebugLogDispatch", ConfigSide.CLIENT, ApplyMode.LIVE, "rendering"));
        reg(ConfigField.bool("mdiVerboseSubdraws", ConfigSide.CLIENT, ApplyMode.LIVE, "rendering"));
        // Размер буферов инстансинга фиксируется при создании рендерера → reload ресурсов
        reg(ConfigField.integer("maxInstancedInstancesPerPart", ConfigSide.CLIENT, ApplyMode.REQUIRES_RESOURCE_RELOAD, "rendering", 256, 16384));
        reg(ConfigField.integer("vatsRenderDistanceChunks", ConfigSide.CLIENT, ApplyMode.LIVE, "rendering", 1, 32));
        reg(ConfigField.enumField("thermalRenderMode", ConfigSide.CLIENT, ApplyMode.LIVE, "rendering"));

        // ── CLIENT: оверлеи ─────────────────────────────────────────
        reg(ConfigField.boolNested("radiationPixelEffect.enableRadiationPixelEffect", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "radiationPixelEffect"));
        reg(ConfigField.floatNested("radiationPixelEffect.radiationPixelEffectThreshold", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "radiationPixelEffect", 0F, 1F));
        reg(ConfigField.floatNested("radiationPixelEffect.radiationPixelMaxIntensityRad", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "radiationPixelEffect", 0F, 100_000F));
        reg(ConfigField.intNested("radiationPixelEffect.radiationPixelEffectMaxDots", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "radiationPixelEffect", 1, 500));
        reg(ConfigField.floatNested("radiationPixelEffect.radiationPixelEffectGreenChance", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "radiationPixelEffect", 0F, 1F));
        reg(ConfigField.intNested("radiationPixelEffect.radiationPixelMinLifetime", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "radiationPixelEffect", 1, 200));
        reg(ConfigField.intNested("radiationPixelEffect.radiationPixelMaxLifetime", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "radiationPixelEffect", 1, 1000));

        reg(ConfigField.boolNested("obstructionHighlight.enableObstructionHighlight", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "obstructionHighlight"));
        reg(ConfigField.intNested("obstructionHighlight.obstructionHighlightAlpha", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "obstructionHighlight", 0, 100));
        reg(ConfigField.intNested("obstructionHighlight.obstructionHighlightDuration", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", "obstructionHighlight", 1, 10));

        reg(ConfigField.integer("infoToastOffsetX", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", 0, 500));
        reg(ConfigField.integer("infoToastOffsetY", ConfigSide.CLIENT, ApplyMode.LIVE, "overlay", 0, 500));

        // ── CLIENT: отладка (читается только клиентом) ──────────────
        reg(ConfigField.floatNum("debugRenderTextSize", ConfigSide.CLIENT, ApplyMode.LIVE, "debug", 0.05F, 5F));
        reg(ConfigField.integer("debugRenderDistance", ConfigSide.CLIENT, ApplyMode.LIVE, "debug", 1, 20));

        // ── Restport: Original-Optionen, die bisher nur als POJO-Feld existierten ──
        // ToolConfig (11.xx), Lesestellen IToolAreaAbility / IToolHarvestAbility
        reg(ConfigField.integer("toolRecursionDepth", ConfigSide.SERVER, ApplyMode.LIVE, "tools", 0, Integer.MAX_VALUE).withComment("Limits veinminer's recursive function. Usually not an issue, unless you're using bukkit which is especially sensitive for some reason. [Original: 11.00_recursionDepth]"));
        reg(ConfigField.bool("toolRecursiveStone", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Determines whether veinminer can break stone [Original: 11.01_recursionStone]"));
        reg(ConfigField.bool("toolRecursiveNetherrack", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Determines whether veinminer can break netherrack [Original: 11.02_recursionNetherrack]"));
        reg(ConfigField.bool("toolAbilityHammer", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allows AoE ability [Original: 11.03_hammerAbility]"));
        reg(ConfigField.bool("toolAbilityVein", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allows veinminer ability [Original: 11.04_abilityVein]"));
        reg(ConfigField.bool("toolAbilityLuck", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allow luck (fortune) ability [Original: 11.05_abilityLuck]"));
        reg(ConfigField.bool("toolAbilitySilk", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allow silk touch ability [Original: 11.06_abilitySilk]"));
        reg(ConfigField.bool("toolAbilityFurnace", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allow auto-smelter ability [Original: 11.07_abilityFurnace]"));
        reg(ConfigField.bool("toolAbilityShredder", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allow auto-shredder ability [Original: 11.08_abilityShredder]"));
        reg(ConfigField.bool("toolAbilityCentrifuge", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allow auto-centrifuge ability [Original: 11.09_abilityCentrifuge]"));
        reg(ConfigField.bool("toolAbilityCrystallizer", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allow auto-crystallizer ability [Original: 11.10_abilityCrystallizer]"));
        reg(ConfigField.bool("toolAbilityMercury", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allow mercury touch ability (digging redstone gives mercury) [Original: 11.11_abilityMercury]"));
        reg(ConfigField.bool("toolAbilityExplosion", ConfigSide.SERVER, ApplyMode.LIVE, "tools").withComment("Allow explosion ability [Original: 11.12_abilityExplosion]"));
        reg(ConfigField.integer("toolHudIndicatorX", ConfigSide.CLIENT, ApplyMode.LIVE, "hud", -10000, 10000).withComment("Horizontal offset of the tool ability HUD indicator [Original: TOOL_HUD_INDICATOR_X]"));
        reg(ConfigField.integer("toolHudIndicatorY", ConfigSide.CLIENT, ApplyMode.LIVE, "hud", -10000, 10000).withComment("Vertical offset of the tool ability HUD indicator [Original: TOOL_HUD_INDICATOR_Y]"));
        // ServerConfig (hbmServer.json)
        reg(ConfigField.bool("damageCompatibilityMode", ConfigSide.SERVER, ApplyMode.LIVE, "server").withComment("[Original: DAMAGE_COMPATIBILITY_MODE]"));
        reg(ConfigField.floatNum("mineApDamage", ConfigSide.SERVER, ApplyMode.LIVE, "server", 0F, 100000F).withComment("[Original: MINE_AP_DAMAGE]"));
        reg(ConfigField.floatNum("mineHeDamage", ConfigSide.SERVER, ApplyMode.LIVE, "server", 0F, 100000F).withComment("[Original: MINE_HE_DAMAGE]"));
        reg(ConfigField.floatNum("mineShrapDamage", ConfigSide.SERVER, ApplyMode.LIVE, "server", 0F, 100000F).withComment("[Original: MINE_SHRAP_DAMAGE]"));
        reg(ConfigField.floatNum("mineNukeDamage", ConfigSide.SERVER, ApplyMode.LIVE, "server", 0F, 100000F).withComment("[Original: MINE_NUKE_DAMAGE]"));
        reg(ConfigField.floatNum("mineNavalDamage", ConfigSide.SERVER, ApplyMode.LIVE, "server", 0F, 100000F).withComment("[Original: MINE_NAVAL_DAMAGE]"));
        reg(ConfigField.bool("crateOpenHeld", ConfigSide.SERVER, ApplyMode.LIVE, "server").withComment("[Original: CRATE_OPEN_HELD]"));
        reg(ConfigField.bool("crateKeepContents", ConfigSide.SERVER, ApplyMode.LIVE, "server").withComment("[Original: CRATE_KEEP_CONTENTS]"));
        reg(ConfigField.integer("itemHazardDropTickrate", ConfigSide.SERVER, ApplyMode.LIVE, "server", 1, Integer.MAX_VALUE).withComment("[Original: ITEM_HAZARD_DROP_TICKRATE]"));
        reg(ConfigField.bool("enableMKU", ConfigSide.SERVER, ApplyMode.LIVE, "server").withComment("[Original: ENABLE_MKU]"));
        reg(ConfigField.bool("structureDebug", ConfigSide.SERVER, ApplyMode.LIVE, "server").withComment("[Original: STRUCTURE_DEBUG]"));
        reg(ConfigField.integer("autocalMaxClock", ConfigSide.SERVER, ApplyMode.LIVE, "server", 1, Integer.MAX_VALUE).withComment("[Original: AUTOCAL_MAX_CLOCK]"));
        reg(ConfigField.integer("potionSickness", ConfigSide.SERVER, ApplyMode.LIVE, "server", 0, 2).withComment("0 = OFF, 1 = NORMAL, 2 = TERRARIA (duration x12) [Original: 8.S0_potionSickness]"));
        // GeneralConfig / WeaponConfig / RadiationConfig / MobConfig
        reg(ConfigField.bool("enable528NetherBurn", ConfigSide.SERVER, ApplyMode.LIVE, "528").withComment("Whether players burn in the nether [Original: X528_enable528NetherBurn]"));
        reg(ConfigField.bool("enableExtendedLogging", ConfigSide.SERVER, ApplyMode.LIVE, "general").withComment("Logs uses of the detonator, nuclear explosions, missile launches, grenades, etc. [Original: 1.18_enableExtendedLogging]"));
        reg(ConfigField.integer("polaroidOverride", ConfigSide.SERVER, ApplyMode.LIVE, "general", 0, 18).withComment("1-18 forces the polaroid number, 0 = random [Original: generalOverride]"));
        reg(ConfigField.bool("dropCrys", ConfigSide.SERVER, ApplyMode.LIVE, "weapons").withComment("Whether xen crystals should move blocks when dropped [Original: 10.04_dropCrys]"));
        reg(ConfigField.bool("dropDead", ConfigSide.SERVER, ApplyMode.LIVE, "weapons").withComment("Whether dead man's explosives should explode when dropped [Original: 10.05_dropDead]"));
        reg(ConfigField.floatNum("netherAmbientRad", ConfigSide.SERVER, ApplyMode.LIVE, "radiation", 0F, 100000F).withComment("RAD/s in the nether [Original: AMBIENT_00_nether]"));
        reg(ConfigField.floatNum("basaltDeltasRadMult", ConfigSide.SERVER, ApplyMode.LIVE, "radiation", 0F, 1000F).withComment("Port: multiplier of the nether background in basalt deltas"));
        reg(ConfigField.bool("enablePollution", ConfigSide.SERVER, ApplyMode.LIVE, "pollution").withComment("If disabled, none of the polltuion related things will work [Original: POL_00_enablePollution]"));
        reg(ConfigField.bool("enableLeadFromBlocks", ConfigSide.SERVER, ApplyMode.LIVE, "pollution").withComment("Whether breaking blocks in heavy metal polluted areas will poison the player [Original: POL_01_enableLeadFromBlocks]"));
        reg(ConfigField.bool("enableLeadPoisoning", ConfigSide.SERVER, ApplyMode.LIVE, "pollution").withComment("Whether being in a heavy metal polluted area will poison the player [Original: POL_02_enableLeadPoisoning]"));
        reg(ConfigField.bool("enablePoison", ConfigSide.SERVER, ApplyMode.LIVE, "pollution").withComment("Whether being in a poisoned area will affect the player [Original: POL_04_enablePoison]"));
        reg(ConfigField.floatNum("buffMobThreshold", ConfigSide.SERVER, ApplyMode.LIVE, "pollution", 0F, 100000F).withComment("The amount of soot required to buff naturally spawning mobs [Original: POL_05_buffMobThreshold]"));
        reg(ConfigField.builder("pollutionMult", ConfigField.FieldType.DOUBLE).side(ConfigSide.SERVER).applyMode(ApplyMode.LIVE).category("pollution").range(0D, 1000D).build().withComment("A multiplier for soot emitted, whether you want to increase or decrease it [Original: 12.R08_pollutionMult]"));

        // ── Restport: statisch gebundene Original-*Config-Klassen (World/Structure/General/Radiation/...) ──
        RestportConfigFields.addTo(ConfigSchema::reg);
    }

    private static void reg(ConfigField f) {
        Objects.requireNonNull(f, "ConfigField");
        ConfigField prev = FIELDS.put(f.getKey(), f);
        if (prev != null) {
            throw new IllegalStateException("Дубликат ключа конфига: " + f.getKey());
        }
        if (f.isStatic()) STATIC_DEFAULTS.put(f.getKey(), f.getAsString(null));
    }

    // ================================================================
    // Lookup / итерация
    // ================================================================

    /** Поле по ключу или null. */
    public static ConfigField get(String key) {
        return FIELDS.get(key);
    }

    /** Все поля в порядке регистрации. */
    public static Collection<ConfigField> all() {
        return Collections.unmodifiableCollection(FIELDS.values());
    }

    /** Все поля заданной стороны в порядке регистрации. */
    public static List<ConfigField> bySide(ConfigSide side) {
        List<ConfigField> out = new ArrayList<>();
        for (ConfigField f : FIELDS.values()) {
            if (f.getSide() == side) out.add(f);
        }
        return out;
    }

    /** Категории заданной стороны в порядке регистрации (без дубликатов). */
    public static List<String> categories(ConfigSide side) {
        Set<String> seen = new LinkedHashSet<>();
        for (ConfigField f : FIELDS.values()) {
            if (f.getSide() == side) seen.add(f.getCategory());
        }
        return new ArrayList<>(seen);
    }

    /** Поля категории заданной стороны в порядке регистрации. */
    public static List<ConfigField> byCategory(ConfigSide side, String category) {
        List<ConfigField> out = new ArrayList<>();
        for (ConfigField f : FIELDS.values()) {
            if (f.getSide() == side && f.getCategory().equals(category)) out.add(f);
        }
        return out;
    }

    // ================================================================
    // Снапшоты (для файла/сети)
    // ================================================================

    /** 
     * Снапшот всех значений стороны как key→String. 
     * Используется сетевыми пакетами и GUI. Не содержит описаний.
     */
    public static Map<String, String> snapshot(ModClothConfig cfg, ConfigSide side) {
        Map<String, String> out = new LinkedHashMap<>();
        for (ConfigField f : FIELDS.values()) {
            if (f.getSide() == side) out.put(f.getKey(), f.getAsString(cfg));
        }
        return out;
    }

    /** 
     * Снапшот как key→Object (для сериализации GSON в файл). 
     * Избавляет от кавычек для чисел/true/false и добавляет _desc_ ключи.
     */
    public static Map<String, Object> snapshotForJson(ModClothConfig cfg, ConfigSide side) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (ConfigField f : FIELDS.values()) {
            if (f.getSide() == side) {
                // Формируем строчку с описанием, дефолтным значением и границами
                StringBuilder desc = new StringBuilder();
                if (f.getComment() != null) desc.append(f.getComment()).append(" ");
                
                if (f.getMin() != null && f.getMax() != null) {
                    desc.append("[Range: ").append(f.getMin()).append(" ~ ").append(f.getMax()).append("] ");
                }
                desc.append("[Default: ").append(defaultAsString(f)).append("]");
                if (f.requiresRestart()) desc.append(" [REQUIRES RESTART]");

                // Записываем фейковый ключ-комментарий
                out.put("_desc_" + f.getKey(), desc.toString().trim());
                
                // Записываем само значение (без кавычек)
                out.put(f.getKey(), f.getForSerialization(cfg));
            }
        }
        return out;
    }

    /** Применяет пары key→value из карты. Неизвестные ключи игнорируются. */
    public static void applyAll(ModClothConfig cfg, ConfigSide side, Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) {
            ConfigField f = FIELDS.get(e.getKey());
            if (f == null || f.getSide() != side) continue;
            try {
                f.setFromString(cfg, e.getValue());
            } catch (IllegalArgumentException ex) {
                // Пропускаем некорректные значения (защита от повреждённого JSON/пакета)
            }
        }
        validate(cfg);
    }

    /** Применяет одно значение по ключу (проверяет side). Возвращает true при успехе. */
    public static boolean applyField(ModClothConfig cfg, String key, String value) {
        ConfigField f = FIELDS.get(key);
        if (f == null) return false;
        try {
            f.setFromString(cfg, value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    // ================================================================
    // Валидация (перенос validatePostLoad)
    // ================================================================

    /**
     * Клэмпит все числовые поля по границам схемы. Заменяет
     * {@code validatePostLoad()} из AutoConfig. Вызывается после загрузки
     * и после применения пакетов.
     */
    public static void validate(ModClothConfig cfg) {
        for (ConfigField f : FIELDS.values()) {
            if (f.getMin() == null || f.getMax() == null) continue;
            try {
                Object v = f.get(cfg);
                if (v instanceof Number) {
                    // set() сам клэмпит по границам схемы
                    f.set(cfg, v);
                }
            } catch (IllegalStateException ignored) {
                // Поле недоступно — пропускаем
            }
        }
        // Restport: Nachbehandlung des Original-loadFromConfig (Strukturabstand min > max)
        com.hbm_m.config.StructureConfig.sanitize();
    }

    // ================================================================
    // Значения по умолчанию (для "Сбросить" и генерации файла)
    // ================================================================

    /** Ленивый экземпляр со значениями по умолчанию. */
    public static ModClothConfig defaults() {
        if (defaults == null) {
            synchronized (ConfigSchema.class) {
                if (defaults == null) defaults = new ModClothConfig();
            }
        }
        return defaults;
    }

    /** Строковое значение по умолчанию для поля. */
    public static String defaultAsString(ConfigField f) {
        if (f.isStatic()) return STATIC_DEFAULTS.get(f.getKey());
        return f.getAsString(defaults());
    }

    // ================================================================
    // Lang-ключи
    // ================================================================

    /** Ключ локализации названия поля: config.hbm_m.field.<key> */
    public static String labelKey(ConfigField f) {
        return "config.hbm_m.field." + f.getKey();
    }

    /** Ключ локализации тултипа поля: config.hbm_m.field.<key>.tooltip */
    public static String tooltipKey(ConfigField f) {
        return labelKey(f) + ".tooltip";
    }

    /** Ключ локализации категории: config.hbm_m.category.<category> */
    public static String categoryKey(String category) {
        return "config.hbm_m.category." + category;
    }
}