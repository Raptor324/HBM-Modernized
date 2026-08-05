package com.hbm_m.config;

import com.hbm_m.config.schema.ConfigSide;
import com.hbm_m.platform.PlatformHooks;

import java.nio.file.Path;

/**
 * Пути к JSON-файлам конфигурации мода (замена TOML-сериализатора AutoConfig).
 *
 * <p>Структура повторяет глобальный каталог оригинального {@code RunningConfig} 1.7.10
 * ({@code configHbmDir}) — конфиг не world-local, а общий:
 * <ul>
 *   <li>{@code <game>/config/hbm_m/client.json} — клиентские настройки (рендер, оверлеи, отладка).</li>
 *   <li>{@code <game>/config/hbm_m/server.json} — серверные настройки (радиация, взрывы, оружие, ...).</li>
 * </ul>
 *
 * <p>Глобальный (а не world-local) серверный конфг выбран намеренно: он общий для всех миров,
 * как {@code ServerConfig → hbmServer.json} в 1.7.10.
 */
public final class ConfigPaths {
    private ConfigPaths() {}

    /** Подкаталог мода внутри config: {@code config/hbm_m}. */
    public static Path configRoot() {
        return PlatformHooks.getConfigDir().resolve("hbm_m");
    }

    /** Файл для стороны: {@code client.json} (CLIENT) или {@code server.json} (SERVER). */
    public static Path file(ConfigSide side) {
        String name = side == ConfigSide.CLIENT ? "client.json" : "server.json";
        return configRoot().resolve(name);
    }
}
