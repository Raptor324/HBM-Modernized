package com.hbm_m.util;

public class LongDataPacker {

    // Берет верхние 32 бита (левую половину)
    public static int packHigh(long value) {
        return (int) (value >> 32);
    }

    // Берет нижние 32 бита (правую половину)
    public static int packLow(long value) {
        return (int) value;
    }

    // Склеивает обратно
    public static long unpack(int high, int low) {
        // ВОТ ЗДЕСЬ ИСПРАВЛЕНИЕ БАГА 4.29 МЛРД:
        // (low & 0xFFFFFFFFL) превращает int в long и стирает знак "минус",
        // чтобы Java не заполнила всё число единицами.
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }
}
