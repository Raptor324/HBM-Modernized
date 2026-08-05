package com.hbm_m.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.config.schema.ConfigSchema;
import com.hbm_m.config.schema.ConfigSide;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
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
     * Загружает значения стороны из JSON в {@code cfg}. Если файл отсутствует —
     * создаёт его с текущими (по умолчанию) значениями. Повреждённый файл → значения по умолчанию.
     *
     * <p>После применения вызывается {@link ConfigSchema#validate} (клэмп по границам).
     */
    public static void load(ConfigSide side, ModClothConfig cfg) {
        Path file = ConfigPaths.file(side);
        if (!Files.exists(file)) {
            // Первый запуск: создаём файл с дефолтами, чтобы пользователь видел все ключи.
            save(side, cfg);
            return;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            if (obj != null) {
                Map<String, String> map = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    JsonElement v = e.getValue();
                    // Только примитивы (строка/число/булев) — объекты/массивы пропускаем.
                    if (v != null && v.isJsonPrimitive()) {
                        map.put(e.getKey(), v.getAsString());
                    }
                }
                ConfigSchema.applyAll(cfg, side, map);
            }
        } catch (Exception e) {
            // Не падаем из-за конфига: оставляем дефолты, файл не трогаем.
            LOGGER.error("[hbm_m] Не удалось прочитать конфиг {}: {}", file, e.toString());
        }
    }

    /**
     * Сохраняет снапшот стороны в JSON. Создаёт родительский каталог при необходимости.
     * Первым полем идёт человекочитаемый {@code _comment} (игнорируется при загрузке).
     */
    public static void save(ConfigSide side, ModClothConfig cfg) {
        Path file = ConfigPaths.file(side);
        try {
            Files.createDirectories(file.getParent());
            Map<String, String> map = ConfigSchema.snapshot(cfg, side);
            Map<String, String> withComment = new LinkedHashMap<>();
            withComment.put("_comment", side == ConfigSide.CLIENT ? COMMENT_CLIENT : COMMENT_SERVER);
            withComment.putAll(map);
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(withComment, w);
            }
        } catch (IOException e) {
            LOGGER.error("[hbm_m] Не удалось записать конфиг {}: {}", file, e.toString());
        }
    }
}
