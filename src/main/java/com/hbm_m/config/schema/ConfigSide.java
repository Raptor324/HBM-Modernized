package com.hbm_m.config.schema;

/**
 * Логическая сторона (владелец значения) конфигурационного поля.
 *
 * <p>Повторяет оригинальное разделение 1.7.10:
 * <ul>
 *   <li>{@code ClientConfig} → {@code hbmClient.json} → {@link #CLIENT}</li>
 *   <li>{@code ServerConfig} → {@code hbmServer.json} → {@link #SERVER}</li>
 * </ul>
 *
 * <p>От стороны зависят:
 * <ul>
 *   <li>в какой JSON-файл идёт сериализация ({@code config/hbm_m/client.json} или {@code server.json});</li>
 *   <li>нужна ли синхронизация с сервера (S2C snapshot);</li>
 *   <li>на какой вкладке GUI появляется поле.</li>
 * </ul>
 */
public enum ConfigSide {
    /** Клиентская настройка (рендер, оверлеи, отладка). Не синхронизируется. */
    CLIENT,
    /** Серверная настройка (радиация, взрывы, оружие, машины). Синхронизируется S2C. */
    SERVER
}
