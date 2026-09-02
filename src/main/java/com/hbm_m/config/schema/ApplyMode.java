package com.hbm_m.config.schema;

/**
 * Режим применения конфигурационного поля после изменения.
 *
 * <p>Используется GUI для предупреждений и логики сохранения:
 * <ul>
 *   <li>{@link #LIVE} — применяется сразу (перезапуск/перезагрузка ресурсов не нужны).</li>
 *   <li>{@link #REQUIRES_RESOURCE_RELOAD} — вступает в силу после перезагрузки ресурсов (F3+T).</li>
 *   <li>{@link #REQUIRES_RESTART} — вступает в силу только после перезапуска игры/модпака
 *       (например, захваченные в {@code static final} значения, worldgen/реестры).</li>
 * </ul>
 */
public enum ApplyMode {
    LIVE,
    REQUIRES_RESOURCE_RELOAD,
    /** Изменение требует перезапуска игры. В GUI помечается значком ⚠ и показом предупреждения. */
    REQUIRES_RESTART
}
