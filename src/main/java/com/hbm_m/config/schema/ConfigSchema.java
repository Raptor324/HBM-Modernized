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
        reg(ConfigField.bool("enableChunkRads", ConfigSide.SERVER, ApplyMode.LIVE, "general"));
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
    }

    private static void reg(ConfigField f) {
        Objects.requireNonNull(f, "ConfigField");
        ConfigField prev = FIELDS.put(f.getKey(), f);
        if (prev != null) {
            throw new IllegalStateException("Дубликат ключа конфига: " + f.getKey());
        }
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