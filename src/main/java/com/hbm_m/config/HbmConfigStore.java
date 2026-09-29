package com.hbm_m.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.config.schema.ConfigSchema;
import com.hbm_m.config.schema.ConfigSide;
import com.hbm_m.lib.RefStrings;
import com.mojang.logging.LogUtils;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-бэкенд конфигурации без внешних зависимостей.
 *
 * <p>Порт оригинального {@code RunningConfig.readConfig/writeConfig} (1.7.10): сериализация
 * плоской карты {@code key→string}. {@link ConfigSchema} — единый источник правды о том,
 * какие ключи существуют, их границы и сторона.
 *
 * <p>Формат файла — плоский JSON-объект, по одному значению на поле схемы:
 * <pre>{@code
 * {
 *   "_comment": "HBM Modernized ...",
 *   "enableRadiation": "true",
 *   "maxRad": "100000.0",
 *   "frackingTower.maxPower": "5000000"
 * }
 * }</pre>
 *
 * <p><b>Robustness:</b>
 * <ul>
 *   <li>Неизвестные ключи при загрузке игнорируются (forward-compat со старыми/новыми версиями).</li>
 *   <li>При сохранении пишутся только известные схеме ключи.</li>
 *   <li>Повреждённый/непарсимый файл → значения по умолчанию (игра не падает); файл НЕ
 *       перезаписывается автоматически, чтобы не уничтожить данные пользователя.</li>
 *   <li>Значения-примитивы читаются толерантно: {@code true} и {@code "true"}, {@code 100} и
 *       {@code "100"} эквивалентны (ручное редактирование пользователем).</li>
 * </ul>
 */
public final class HbmConfigStore {
    private HbmConfigStore() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String COMMENT_CLIENT =
            "HBM Modernized client config. Edit manually or via in-game GUI. Values are strings per schema.";
    private static final String COMMENT_SERVER =
            "HBM Modernized server config (synced S2C). Edit manually or via in-game GUI (op only).";

    /**
     * Ключ-штамп версии мода в JSON-файле. Не совпадает с версией мода → выполняется
     * миграция (сброс полей, помеченных resetOnUpgrade), после чего штамп обновляется.
     * Отсутствие штампа (файл старой версии или свежесозданный до введения штампов)
     * также запускает миграцию — для значений на дефолте она ничего не меняет.
     */
    private static final String VERSION_KEY = "_configVersion";

    /** Текущая версия мода (по Architectury Platform; при сбое — пустая строка, миграция выполнится один раз). */
    private static String currentModVersion() {
        try {
            return Platform.getMod(RefStrings.MODID).getVersion();
        } catch (Exception e) {
            LOGGER.error("[hbm_m] Failed to resolve mod version for config migration: {}", e.toString());
            return "";
        }
    }

    /**
     * Загружает значения стороны из JSON в {@code cfg}. Если файл отсутствует —
     * создаёт его с текущими (по умолчанию) значениями. Повреждённый файл → значения по умолчанию
     * (файл НЕ перезаписывается, чтобы сохранить данные пользователя для ручного разбора).
     *
     * <p><b>Миграция:</b> если штамп {@code _configVersion} в файле не совпадает с версией мода,
     * поля, помеченные в схеме {@code resetOnUpgrade}, сбрасываются к дефолту (см.
     * {@link ConfigSchema#resetFlaggedToDefault}), после чего файл перезаписывается с актуальным
     * штампом. На повторных запусках той же версии штамп совпадает — миграция не выполняется.
     *
     * <p>После применения вызывается {@link ConfigSchema#validate} (клэмп по границам).
     */
    public static void load(ConfigSide side, ModClothConfig cfg) {
        Path file = ConfigPaths.file(side);
        if (!Files.exists(file)) {
            save(side, cfg);
            return;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            if (obj != null) {
                JsonElement versionEl = obj.get(VERSION_KEY);
                String fileVersion = versionEl != null && versionEl.isJsonPrimitive() ? versionEl.getAsString() : null;

                Map<String, String> map = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    if (e.getKey().startsWith("_desc_") || e.getKey().equals("_comment") || e.getKey().equals(VERSION_KEY)) continue;

                    JsonElement v = e.getValue();
                    if (v != null && v.isJsonPrimitive()) {
                        map.put(e.getKey(), v.getAsString());
                    }
                }
                ConfigSchema.applyAll(cfg, side, map);
                if (side == ConfigSide.CLIENT) {
                    if (!map.containsKey("occlusionCullingMode") && map.containsKey("enableOcclusionCulling")) {
                        boolean legacy = Boolean.parseBoolean(map.get("enableOcclusionCulling"));
                        cfg.occlusionCullingMode = legacy ? ModClothConfig.OcclusionCullingMode.CPU : ModClothConfig.OcclusionCullingMode.OFF;
                    }
                    cfg.enableOcclusionCulling = (cfg.occlusionCullingMode != ModClothConfig.OcclusionCullingMode.OFF);
                }

                String currentVersion = currentModVersion();
                if (!currentVersion.equals(fileVersion)) {
                    List<String> reset = ConfigSchema.resetFlaggedToDefault(cfg, side);
                    if (!reset.isEmpty()) {
                        LOGGER.info("[hbm_m] Config migration {} ({} -> {}): reset to defaults: {}",
                                side, fileVersion, currentVersion, String.join(", ", reset));
                    }
                    save(side, cfg);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[hbm_m] Failed to read config {}: {}", file, e.toString());
        }
    }

    /**
     * Сохраняет снапшот стороны в JSON. Создаёт родительский каталог при необходимости.
     * Первым полем идёт человекочитаемый {@code _comment} (игнорируется при загрузке),
     * вторым — штамп {@code _configVersion} для миграции.
     */
    public static void save(ConfigSide side, ModClothConfig cfg) {
        Path file = ConfigPaths.file(side);
        try {
            Files.createDirectories(file.getParent());

            Map<String, Object> map = ConfigSchema.snapshotForJson(cfg, side);

            Map<String, Object> withComment = new LinkedHashMap<>();
            withComment.put("_comment", side == ConfigSide.CLIENT ? COMMENT_CLIENT : COMMENT_SERVER);
            withComment.put(VERSION_KEY, currentModVersion());
            withComment.putAll(map);

            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(withComment, w);
            }
        } catch (IOException e) {
            LOGGER.error("[hbm_m] Failed to write config {}: {}", file, e.toString());
        }
    }
}
