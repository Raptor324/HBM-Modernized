package com.hbm_m.energy; // (или com.hbm_m.api.util)

/**
 * Утилитарный класс для "упаковки" и "распаковки"
 * long-значений в два int для передачи через ContainerData.
 */
public final class LongDataPacker {

    // Приватный конструктор, т.к. это утилитарный класс
    private LongDataPacker() {}

    /**
     * Извлекает "верхние" (старшие) 32 бита из long.
     * @param value long-значение (64 бита)
     * @return int (верхние 32 бита)
     */
    public static int packHigh(long value) {
        return (int) (value >> 32);
    }

    /**
     * Извлекает "нижние" (младшие) 32 бита из long.
     * @param value long-значение (64 бита)
     * @return int (нижние 32 бита)
     */
    public static int packLow(long value) {
        return (int) value;
    }

    /**
     * Собирает long (64 бита) из двух int (32 + 32 бита).
     * @param high "Верхние" 32 бита
     * @param low  "Нижние" 32 бита
     * @return собранное long-значение
     */
    public static long unpack(int high, int low) {
        // (1. Сдвигаем 'high' влево) | (2. Маскируем 'low')
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }
}
