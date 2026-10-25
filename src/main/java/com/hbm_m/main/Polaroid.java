package com.hbm_m.main;

import java.util.Random;

/**
 * Port von {@code MainRegistry.polaroidID}: beim Start ausgewuerfelte Zahl 1-18 (ohne 4 und 9),
 * die an vielen Stellen kleine Ostereier steuert - bei 11 etwa Balefire-Blitze statt Muke,
 * andere Getraenke-Tooltips und "Reda" statt "Radar". Das Polaroid-Item zeigt sie an.
 * {@code generalOverride} entspricht {@code polaroidOverride} in der Konfiguration.
 */
public final class Polaroid {

    private Polaroid() {}

    private static int polaroidID = 1;

    static {
        Random rand = new Random();
        polaroidID = rand.nextInt(18) + 1;
        while (polaroidID == 4 || polaroidID == 9) polaroidID = rand.nextInt(18) + 1;
    }

    public static int id() {
        try {
            int o = com.hbm_m.config.ModClothConfig.get().polaroidOverride;
            if (o > 0 && o < 19) return o;
        } catch (Throwable ignored) { }
        return polaroidID;
    }

    public static void set(int id) {
        polaroidID = id;
    }
}
