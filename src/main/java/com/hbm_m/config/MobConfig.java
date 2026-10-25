package com.hbm_m.config;

/**
 * Lesesicht auf die Glyphiden-Werte von {@code MobConfig}. Ist {@code rampantMode} an, gelten wie im Original
 * {@code loadFromConfig} die Rampant-Ueberschreibungen (alle Einzelschalter an, Spaeherschwarm-Chance 1,
 * Spaeherschwelle 0,1, Bombardier ohne Mindestruss).
 */
public final class MobConfig {

    private MobConfig() { }

    private static ModClothConfig c() { return ModClothConfig.get(); }

    /** Original 12.D00_enableDucks: Taste O laesst den Spieler eine Ente werfen (DuckKeyHandler -> DuckC2SPacket). */
    public static boolean enableDucks = true;
    /** Original 12.D01_enableMobGear: Zombies und Skelette spawnen mit Zusatzausruestung. */
    public static boolean enableMobGear = true;

    /** Original {@code trueRam()} - fuer die Rampant-Plakette im HUD. */
    public static boolean trueRam() {
        return c().rampantMode && rampantNaturalScoutSpawn() && scoutThreshold() <= 0.1D
                && rampantExtendedTargetting() && rampantDig() && rampantGlyphidGuidance();
    }

    public static boolean enableHives() { return c().enableHives; }
    public static boolean enableRaids() { return c().enableRaids; }
    public static boolean enableMaskman() { return c().enableMaskman; }
    public static int maskmanDelay() { return Math.max(1, c().maskmanDelay); }
    public static int maskmanMinRad() { return c().maskmanMinRad; }
    public static boolean maskmanUnderground() { return c().maskmanUnderground; }
    public static boolean enableElementals() { return c().enableElementals; }
    public static int elementalDelay() { return Math.max(1, c().elementalDelay); }
    public static int elementalChance() { return Math.max(1, c().elementalChance); }
    public static int elementalAmount() { return c().elementalAmount; }
    public static int raidDelay() { return Math.max(1, c().raidDelay); }
    public static int raidChance() { return Math.max(1, c().raidChance); }
    public static int raidAmount() { return c().raidAmount; }
    public static int raidDrones() { return c().raidDrones; }
    public static int raidAttackDelay() { return Math.max(1, c().raidAttackDelay); }
    public static int raidAttackReach() { return c().raidAttackReach; }
    public static int raidAttackDistance() { return c().raidAttackDistance; }
    public static int hiveSpawn() { return Math.max(1, c().hiveSpawn); }
    public static double scoutThreshold() { return c().rampantMode ? 0.1 : c().scoutThreshold; }
    public static int scoutSwarmSpawnChance() { return c().rampantMode ? 1 : c().scoutSwarmSpawnChance; }
    public static boolean waypointDebug() { return c().waypointDebug; }
    public static int largeHiveChance() { return c().largeHiveChance; }
    public static int largeHiveThreshold() { return c().largeHiveThreshold; }
    public static int swarmCooldown() { return Math.max(1, c().swarmCooldown * 20); }
    public static int baseSwarmSize() { return c().baseSwarmSize; }
    public static double swarmScalingMult() { return c().swarmScalingMult; }
    public static int sootStep() { return Math.max(1, c().sootStep); }
    public static double spawnMax() { return c().spawnMax; }
    public static double targetingThreshold() { return c().targetingThreshold; }

    public static boolean rampantNaturalScoutSpawn() { return c().rampantMode || c().rampantNaturalScoutSpawn; }
    public static double rampantScoutSpawnThresh() { return c().rampantScoutSpawnThresh; }
    public static int rampantScoutSpawnChance() { return Math.max(1, c().rampantScoutSpawnChance); }
    public static boolean scoutInitialSpawn() { return c().scoutInitialSpawn; }
    public static boolean rampantExtendedTargetting() { return c().rampantMode || c().rampantExtendedTargetting; }
    public static boolean rampantDig() { return c().rampantMode || c().rampantDig; }
    public static boolean rampantGlyphidGuidance() { return c().rampantMode || c().rampantGlyphidGuidance; }
    public static double rampantSmokeStackOverride() { return c().rampantSmokeStackOverride; }
    public static boolean enableMobWeapons() { return c().enableMobWeapons; }
    public static double mobWeaponSootReduction() { return c().mobWeaponSootReduction; }

    public static int[] glyphidChance() { return c().glyphidChance; }
    public static int[] brawlerChance() { return c().brawlerChance; }
    public static int[] bombardierChance() {
        int[] v = c().bombardierChance;
        if (c().rampantMode && v[2] == 1) return new int[] { v[0], v[1], 0 };
        return v;
    }
    public static int[] blasterChance() { return c().blasterChance; }
    public static int[] diggerChance() { return c().diggerChance; }
    public static int[] behemothChance() { return c().behemothChance; }
    public static int[] brendaChance() { return c().brendaChance; }
    public static int[] johnsonChance() { return c().johnsonChance; }
}
