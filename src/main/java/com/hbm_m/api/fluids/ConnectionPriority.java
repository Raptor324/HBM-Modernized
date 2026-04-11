package com.hbm_m.api.fluids;

/**
 * Приоритет подключения получателей в жидкостной сети.
 * Более высокий ordinal = более высокий приоритет (обрабатывается первым).
 */
public enum ConnectionPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST
}
