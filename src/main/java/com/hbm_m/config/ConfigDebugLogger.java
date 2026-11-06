package com.hbm_m.config;

import com.hbm_m.config.schema.ConfigField;
import com.hbm_m.config.schema.ConfigSchema;
import com.hbm_m.config.schema.ConfigSide;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Логирование конфигурации на INFO: таблица текущих значений при входе в мир
 * и журнал изменений в формате «параметр: старое -> новое».
 *
 * <p><b>Безопасность:</b> серверные поля и так безусловно рассылаются каждому
 * клиенту при входе ({@code ConfigSyncS2CPacket} из {@code ModEventHandler});
 * блокировка серверной вкладки в GUI — это только право редактирования
 * ({@code ConfigEditC2SPacket} повторно проверяет {@code hasPermissions(2)}).
 * Значения в логе ничего не раскрывают тем, у кого нет прав: они и так лежат
 * в клиентском синглтоне конфига.
 *
 * <p>Точки вызова:
 * <ul>
 *   <li>{@code SERVER_STARTED} — дамп серверной стороны (dedicated: старт, SP: вход в мир);</li>
 *   <li>{@code CLIENT_PLAYER_JOIN} — дамп клиентской стороны;</li>
 *   <li>{@code ConfigSyncS2CPacket} — первый снапшот за сессию = полный дамп серверных
 *       полей, повторные = только строки изменений (op правил конфиг пока мы онлайн);</li>
 *   <li>{@code ConfigEditC2SPacket} (сервер) и {@code ConfigScreen} (клиент) — строки
 *       изменений применённых правок.</li>
 * </ul>
 */
public final class ConfigDebugLogger {
    private ConfigDebugLogger() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Последний серверный снапшот, полученный клиентом (для диффа повторных синхов). Только клиентский поток. */
    private static Map<String, String> lastSyncedServer;

    // ================================================================
    // Дампы (вход в мир)
    // ================================================================

    /** Дамп одной стороны из текущего синглтона. INFO, одним многострочным сообщением. */
    public static void dumpSide(ConfigSide side) {
        dumpValues(side, ConfigSchema.snapshot(ModClothConfig.get(), side));
    }

    /** Форматированная таблица: категории в порядке схемы, ключи выровнены в колонку. */
    private static void dumpValues(ConfigSide side, Map<String, String> values) {
        List<ConfigField> fields = ConfigSchema.bySide(side);
        if (fields.isEmpty()) return;

        int keyWidth = 0;
        for (ConfigField f : fields) keyWidth = Math.max(keyWidth, f.getKey().length());

        StringBuilder sb = new StringBuilder();
        sb.append('\n').append("==== HBM Modernized config: ").append(side).append(" ====");
        String currentCategory = null;
        for (ConfigField f : fields) {
            if (!f.getCategory().equals(currentCategory)) {
                currentCategory = f.getCategory();
                sb.append('\n').append('[').append(currentCategory).append(']');
            }
            String value = values.get(f.getKey());
            sb.append('\n')
              .append("  ")
              .append(String.format("%-" + keyWidth + "s", f.getKey()))
              .append(" = ")
              .append(value != null ? value : "?");
        }
        sb.append('\n').append("==== end config: ").append(side).append(" ====");
        LOGGER.info(sb.toString());
    }

    // ================================================================
    // Изменения
    // ================================================================

    /**
     * Логирует изменения текущих значений стороны относительно снапшота {@code before}.
     * Одна INFO-строка на изменившийся ключ: {@code Config [server] change: key: 'old' -> 'new'}.
     */
    public static void logChanges(ConfigSide side, Map<String, String> before) {
        for (Map.Entry<String, String> e : ConfigSchema.snapshot(ModClothConfig.get(), side).entrySet()) {
            String old = before.get(e.getKey());
            if (!Objects.equals(old, e.getValue())) {
                LOGGER.info("Config [{}] change: {}: '{}' -> '{}'", side, e.getKey(), old, e.getValue());
            }
        }
    }

    /**
     * Клиент: получен снапшот серверных полей (вход в мир или правка op'ом онлайн).
     * Первый за сессию — полный дамп, повторные — только изменения.
     */
    public static void onClientSyncReceived(Map<String, String> values) {
        Map<String, String> last = lastSyncedServer;
        if (last == null) {
            dumpValues(ConfigSide.SERVER, values);
        } else if (!last.equals(values)) {
            for (Map.Entry<String, String> e : values.entrySet()) {
                String old = last.get(e.getKey());
                if (!Objects.equals(old, e.getValue())) {
                    LOGGER.info("Config [{}] change (server op): {}: '{}' -> '{}'",
                            ConfigSide.SERVER, e.getKey(), old, e.getValue());
                }
            }
        }
        lastSyncedServer = values;
    }

    /** Клиент: выход из мира — следующее подключение снова даст полный дамп серверных полей. */
    public static void resetClientSession() {
        lastSyncedServer = null;
    }
}
