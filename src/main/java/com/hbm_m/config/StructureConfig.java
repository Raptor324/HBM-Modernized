package com.hbm_m.config;

/**
 * 1:1 {@code com.hbm.config.StructureConfig} - wirksame Standardwerte des Originals, d.h. die Vorgaben aus
 * {@code loadFromConfig} (die dort die Feldwerte ueberschreiben: Leuchtturm 1, Schuessel 10, Funkhaus 25,
 * Null-Gewichte 20/35, hoechster Abstand 16 Chunks). Restport: statisch an {@code ConfigSchema} gebunden
 * (server.json, Kategorie "structures").
 */
public final class StructureConfig {

    private StructureConfig() { }

    /** Nachbehandlung aus dem Original-{@code loadFromConfig}: Minimum ueber Maximum faellt auf 8/24 zurueck. */
    public static void sanitize() {
        if (structureMinChunks > structureMaxChunks) {
            com.mojang.logging.LogUtils.getLogger().error("Fatal error config: Minimum value has been set higher than the maximum value!");
            com.mojang.logging.LogUtils.getLogger().error("Errored values will default back to 8 and 24 respectively, PLEASE REVIEW CONFIGURATION DESCRIPTION BEFORE MEDDLING WITH VALUES!");
            structureMinChunks = 8;
            structureMaxChunks = 24;
        }
    }

    /** 0 = aus, 1 = an, 2 = Weltschalter "Strukturen generieren" beachten (Vanilla tut das ohnehin). */
    public static int enableStructures = 2;

    public static int structureMinChunks = 4;
    public static int structureMaxChunks = 16;

    public static double lootAmountFactor = 1D;

    public static boolean debugStructures = false;

    public static boolean enableRuins = true;
    public static boolean enableOceanStructures = true;

    public static int ruinsASpawnWeight = 10;
    public static int ruinsBSpawnWeight = 12;
    public static int ruinsCSpawnWeight = 12;
    public static int ruinsDSpawnWeight = 12;
    public static int ruinsESpawnWeight = 12;
    public static int ruinsFSpawnWeight = 12;
    public static int ruinsGSpawnWeight = 12;
    public static int ruinsHSpawnWeight = 12;
    public static int ruinsISpawnWeight = 12;
    public static int ruinsJSpawnWeight = 12; // zusammen 120 (frueher 220)

    public static int plane1SpawnWeight = 25;
    public static int plane2SpawnWeight = 25;

    public static int desertShack1SpawnWeight = 18;
    public static int desertShack2SpawnWeight = 20;
    public static int desertShack3SpawnWeight = 22;

    public static int laboratorySpawnWeight = 20;
    public static int lighthouseSpawnWeight = 1;
    public static int oilRigSpawnWeight = 5;
    public static int broadcastingTowerSpawnWeight = 25;
    public static int waterPumpSpawnWeight = 15;
    public static int deadDishSmallSpawnWeight = 15;
    public static int beachedPatrolSpawnWeight = 15;
    public static int vertibirdSpawnWeight = 6;
    public static int vertibirdCrashedSpawnWeight = 10;

    public static int factorySpawnWeight = 40;
    public static int radioSpawnWeight = 25;
    public static int forestChemSpawnWeight = 30;
    public static int forestPostSpawnWeight = 30;
    public static int towerBaseSpawnWeight = 30;

    public static int spireSpawnWeight = 2;
    public static int craneSpawnWeight = 20;
    public static int bunkerSpawnWeight = 6;
    public static int dishSpawnWeight = 10;
    public static int featuresSpawnWeight = 50;

    public static int aircraftCarrierSpawnWeight = 3;

    // --- Null-Gewichte
    public static int plainsNullWeight = 20;
    public static int oceanNullWeight = 35;
}
