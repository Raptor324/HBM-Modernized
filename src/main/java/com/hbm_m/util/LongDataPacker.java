package com.hbm_m.util;

/**
 * Утилитарный класс для упаковки long в два int и обратно.
 * Это необходимо для синхронизации long-значений через ванильный ContainerData,
 * который поддерживает только 16-битные значения, но Forge расширяет его до 32-битных (int).
 */
public final class LongDataPacker {

    /**
     * Приватный конструктор, чтобы никто не мог создать экземпляр этого класса.
     */
    private LongDataPacker() {}

    /**
     * Получает старшие 32 бита из long и возвращает их как int.
     * @param value long-значение для упаковки.
     * @return Старшие 32 бита.
     */
    public static int packHigh(long value) {
        return (int) (value >> 32);
    }

    /**
     * Получает младшие 32 бита из long и возвращает их как int.
     * @param value long-значение для упаковки.
     * @return Младшие 32 бита.
     */
    public static int packLow(long value) {
        return (int) (value & 0xFFFFFFFFL);
    }

    /**
     * Собирает long из двух int (старших и младших битов).
     * @param high Старшие 32 бита.
     * @param low Младшие 32 бита.
     * @return Собранное long-значение.
     */
    public static long unpack(int high, int low) {
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }
}
