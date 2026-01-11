package com.hbm_m.util;
// Добавь эти импорты в начало файла EnergyFormatter.java
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class EnergyFormatter {

    private static final String[] SUFFIXES = {"", "k", "M", "G", "T", "P", "E"};
    private static final long[] THRESHOLDS = {
            1L,
            1_000L,
            1_000_000L,
            1_000_000_000L,
            1_000_000_000_000L,
            1_000_000_000_000_000L,
            1_000_000_000_000_000_000L
    };

    // Создаем форматировщики один раз для производительности.
    // Используем Locale.US, чтобы разделителем всегда была точка (.), а не запятая (,).
    private static final DecimalFormat df0 = new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat df1 = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat df2 = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));

    /**
     * Форматирует число энергии с приставками СИ
     * @param energy количество энергии
     * @return отформатированная строка (например: "500k")
     */
    public static String format(long energy) {
        if (energy < 0) {
            return "-" + format(-energy);
        }

        if (energy < 1_000) {
            return df2.format(energy); // Показываем с точностью до сотых для значений < 1000
        }

        // Находим подходящую приставку
        int suffixIndex = 0;
        for (int i = 1; i < THRESHOLDS.length; i++) {
            if (energy >= THRESHOLDS[i]) {
                suffixIndex = i;
            } else {
                break;
            }
        }

        double value = (double) energy / THRESHOLDS[suffixIndex];

        // Всегда показываем 2 десятичных знака для точности до десятков
        String formatted = df2.format(value);

        return formatted + SUFFIXES[suffixIndex];
    }

    // Остальные методы (formatWithUnit, formatFE, formatRate) остаются без изменений

    public static String formatWithUnit(long energy, String unit) {
        return format(energy) + " " + unit;
    }

    public static String formatFE(long energy) {
        return formatWithUnit(energy, "HE");
    }

    public static String formatRate(long rate) {
        return formatWithUnit(rate, "HE/t");
    }
}