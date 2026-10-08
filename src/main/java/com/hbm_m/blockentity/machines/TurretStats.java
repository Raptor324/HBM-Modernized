package com.hbm_m.blockentity.machines;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

/**
 * Stat- und Modell-Tabelle fuer alle Turret-Varianten aus dem Original
 * (WeaponRecipes.java / TileEntityTurretX / RenderTurretX). Feuerlogik ist fuer alle Varianten
 * dieselbe eigenstaendige Hitscan-MVP-Logik (siehe {@link TurretBaseBlockEntity}) - das Original
 * nutzt hier das noch nicht portierte Waffen-/Munitionssystem (BulletConfig). Arty/Himars feuern
 * im Original Artillerie-Granaten bzw. Raketen; hier vereinfacht auf denselben Hitscan-Mechanismus
 * mit hoeherem Schaden/Reichweite.
 * <p>
 * Modell-Teile (yawPart/pitchParts) referenzieren lose registrierte Baked-Models unter
 * {@code block/turret_parts/<key>} (siehe {@code ClientSetup#onModelRegisterAdditional}),
 * herausgeloest aus den Original-OBJs per Forge-"visibility". Barrel-Spin/Recoil (z.B. rotierende
 * Chekhov-Trommel, Sentry-Rueckstoss) sind bewusst NICHT animiert - nur Yaw/Pitch-Zielverfolgung.
 */
public enum TurretStats {
    // Reichweiten der SEDNA-Tuerme = Original getDecetorRange() (Sentry 24, Chekhov/Friendly 32, Jeremy 80,
    // Tauon 128, Richard 64, Howard 250, Fritz 48). damage/cooldownTicks werden fuer diese nicht mehr benutzt
    // (Schaden/Takt kommen aus den Original-updateFiringTick-Werten in TurretBaseBlockEntity).
    SENTRY(24.0D, 4.0F, 10, 200L, "gui_turret_sentry", "container.hbm_m.turret_sentry",
            PitchAxis.X, 1.25D, 0D, 0D, "sentry_pivot", List.of("sentry_body", "sentry_drum", "sentry_barrell", "sentry_barrelr"), 4.5, 3.0),
    CHEKHOV(32.0D, 5.0F, 9, 220L, "gui_turret_base", "container.hbm_m.turret_chekhov",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("chekhov_body", "chekhov_barrels"), 4.5, 3.0),
    FRIENDLY(32.0D, 4.0F, 10, 200L, "gui_turret_friendly", "container.hbm_m.turret_friendly",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage_friendly", List.of("chekhov_body", "chekhov_barrels"), 4.5, 3.0),
    JEREMY(80.0D, 5.0F, 9, 220L, "gui_turret_base", "container.hbm_m.turret_jeremy",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("jeremy_gun"), 4.5, 3.0),
    TAUON(128.0D, 6.0F, 8, 260L, "gui_turret_tau", "container.hbm_m.turret_tauon",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("tauon_cannon", "tauon_rotor"), 9.0, 6.0),
    RICHARD(64.0D, 6.0F, 8, 260L, "gui_turret_richard", "container.hbm_m.turret_richard",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("richard_launcher"), 4.5, 3.0),
    HOWARD(250.0D, 7.0F, 7, 300L, "gui_turret_howard", "container.hbm_m.turret_howard",
            PitchAxis.Z, 2.25D, 0D, 0D, "howard_carriage", List.of("howard_body", "howard_barrelstop", "howard_barrelsbottom"), 12.0, 8.0),
    MAXWELL(22.0D, 7.0F, 7, 300L, "gui_turret_maxwell", "container.hbm_m.turret_maxwell",
            PitchAxis.Z, 1.5D, 0D, 0D, "howard_carriage", List.of("maxwell_microwave"), 9.0, 6.0),
    FRITZ(48.0D, 8.0F, 6, 340L, "gui_turret_fritz", "container.hbm_m.turret_fritz",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("fritz_gun"), 4.5, 3.0),
    ARTY(32.0D, 12.0F, 30, 600L, "gui_turret_arty", "container.hbm_m.turret_arty",
            PitchAxis.X, 3.0D, 0D, -90D, "arty_carriage", List.of("arty_cannon", "arty_barrel"), 1.0, 0.5),
    HIMARS(40.0D, 16.0F, 40, 900L, "gui_turret_himars", "container.hbm_m.turret_himars",
            PitchAxis.X, 2.25D, 2.0D, -90D, "himars_carriage", List.of("himars_launcher", "himars_crane"), 1.0, 0.5),
    // Original TileEntityTurretSentryDamaged / TileEntityTurretHowardDamaged (Bunker-Deko, ohne GUI und Strom):
    // erben Sentry/Howard, drehen mit 3/2 Grad pro Tick, Howard sieht nur 16 Bloecke weit.
    SENTRY_DAMAGED(24.0D, 4.0F, 10, 200L, "gui_turret_sentry", "container.hbm_m.turret_sentry",
            PitchAxis.X, 1.25D, 0D, 0D, "sentry_damaged_pivot", List.of("sentry_damaged_body", "sentry_damaged_drum", "sentry_damaged_barrell", "sentry_damaged_barrelr"), 3.0, 2.0),
    HOWARD_DAMAGED(16.0D, 7.0F, 7, 300L, "gui_turret_howard", "container.hbm_m.turret_howard",
            PitchAxis.Z, 2.25D, 0D, 0D, "howard_damaged_carriage", List.of("howard_damaged_body", "howard_damaged_barrelstop", "howard_damaged_barrelsbottom"), 3.0, 2.0);

    public enum PitchAxis { X, Z }

    public final double range;
    public final float damage;
    public final int cooldownTicks;
    public final long energyPerShot;
    private final String guiTextureName;
    private final String nameKey;

    /** Achse, um die die "Pitch"-Gruppe (Kanone/Body) kippt. */
    public final PitchAxis pitchAxis;
    /** Drehpunkt der Pitch-Gruppe relativ zum Block-Ursprung. */
    public final double pivotY;
    public final double pivotZ;
    /** Zusaetzlicher Yaw-Offset in Grad (manche Modelle sind 90 Grad verdreht exportiert). */
    public final double yawExtraOffsetDeg;
    /** Loser Modell-Key (unter block/turret_parts/) fuer den Yaw-rotierenden Unterbau ("Carriage"/"Pivot"). */
    /** Original getTurretYawSpeed()/getTurretPitchSpeed(), in degrees per tick. Every turret has
     *  its own pair - the Arty and HIMARS crawl round at 1.0/0.5 while Howard whips round at
     *  12.0/8.0 - where this port turned all eleven at a flat 4 deg/tick. */
    public final double yawSpeed;
    public final double pitchSpeed;

    public final String yawPartKey;
    /** Lose Modell-Keys, die gemeinsam mit der Pitch-Gruppe gerendert werden (Body/Kanone/Laeufe usw.). */
    public final List<String> pitchPartKeys;

    TurretStats(double range, float damage, int cooldownTicks, long energyPerShot, String guiTextureName, String nameKey,
                PitchAxis pitchAxis, double pivotY, double pivotZ, double yawExtraOffsetDeg,
                String yawPartKey, List<String> pitchPartKeys,
                double yawSpeed, double pitchSpeed) {
        this.range = range;
        this.damage = damage;
        this.cooldownTicks = cooldownTicks;
        this.energyPerShot = energyPerShot;
        this.guiTextureName = guiTextureName;
        this.nameKey = nameKey;
        this.pitchAxis = pitchAxis;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;
        this.yawExtraOffsetDeg = yawExtraOffsetDeg;
        this.yawSpeed = yawSpeed;
        this.pitchSpeed = pitchSpeed;
        this.yawPartKey = yawPartKey;
        this.pitchPartKeys = pitchPartKeys;
    }

    /** Original {@code getMaxPower()} der einzelnen Tuerme (eigener Speicher je Geschuetz). */
    public long getMaxPower() {
        return switch (this) {
            case SENTRY, SENTRY_DAMAGED -> 1_000L;
            case TAUON, ARTY -> 100_000L;
            case HOWARD, HOWARD_DAMAGED -> 50_000L;
            case MAXWELL -> 10_000_000L;
            case HIMARS -> 1_000_000L;
            default -> 10_000L;
        };
    }

    /** Original {@code getConsumption()}: HE pro Tick, solange der Turm an ist (Basis 100). */
    public long getConsumption() {
        return switch (this) {
            case SENTRY, SENTRY_DAMAGED -> 5L;
            case TAUON -> 1_000L;
            case HOWARD, HOWARD_DAMAGED -> 500L;
            case MAXWELL -> 10_000L; // Original: 5G ? 10 : 10000 - blueLevel * 300
            default -> 100L;
        };
    }

    /** Original {@code getDecetorGrace()}: Mindestabstand eines Ziels (Basis 3). */
    public double getDecetorGrace() {
        return switch (this) {
            case SENTRY, SENTRY_DAMAGED, FRITZ -> 2D;
            case JEREMY -> 16D;
            case RICHARD -> 8D;
            case MAXWELL, HOWARD_DAMAGED -> 5D;
            default -> 3D;
        };
    }

    /** Original {@code getTurretDepression()}: maximale Neigung nach unten in Grad (Basis 30). */
    public double getTurretDepression() {
        return switch (this) {
            case SENTRY, SENTRY_DAMAGED -> 20D;
            case HOWARD, HOWARD_DAMAGED -> 50D;
            case JEREMY -> 45D;
            case RICHARD -> 25D;
            case TAUON, MAXWELL -> 35D;
            default -> 30D;
        };
    }

    /** Original {@code getTurretElevation()}: maximale Neigung nach oben in Grad (Basis 30). */
    public double getTurretElevation() {
        return switch (this) {
            case SENTRY, SENTRY_DAMAGED -> 20D;
            case CHEKHOV, FRIENDLY, FRITZ -> 45D;
            case HOWARD, HOWARD_DAMAGED, ARTY -> 90D;
            case RICHARD -> 25D;
            case TAUON -> 35D;
            case MAXWELL -> 40D;
            default -> 30D;
        };
    }

    /** Original {@code getAcceptableInaccuracy()} in Grad (Basis 5). */
    public double getAcceptableInaccuracy() {
        return switch (this) {
            case SENTRY, SENTRY_DAMAGED, CHEKHOV, FRIENDLY, FRITZ -> 15D;
            case MAXWELL -> 2D;
            case ARTY -> 0D;
            default -> 5D;
        };
    }

    /** Original {@code getDecetorInterval()}: Ticks zwischen zwei Zielsuchen. */
    public int getDecetorInterval() {
        return 10;
    }

    /** Original {@code hasThermalVision()}: Sentry (und die beschaedigten Tuerme) sehen keine Unsichtbaren. */
    public boolean hasThermalVision() {
        return this != SENTRY && this != SENTRY_DAMAGED && this != HOWARD_DAMAGED;
    }

    /** Beschaedigte Tuerme: immer an, brauchen keinen Strom, schiessen auf alles Lebende ausser Kreativspieler. */
    public boolean isDamaged() {
        return this == SENTRY_DAMAGED || this == HOWARD_DAMAGED;
    }

    public ResourceLocation getGuiTexture() {
        return ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/gui/weapon/" + guiTextureName + ".png");
    }

    public String getNameKey() {
        return nameKey;
    }
}
