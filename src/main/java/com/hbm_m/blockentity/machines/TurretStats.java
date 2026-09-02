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
    SENTRY(16.0D, 4.0F, 10, 200L, "gui_turret_sentry", "container.hbm_m.turret_sentry",
            PitchAxis.X, 1.25D, 0D, 0D, "sentry_pivot", List.of("sentry_body", "sentry_drum", "sentry_barrell", "sentry_barrelr"), 4.5, 3.0),
    CHEKHOV(18.0D, 5.0F, 9, 220L, "gui_turret_base", "container.hbm_m.turret_chekhov",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("chekhov_body", "chekhov_barrels"), 4.5, 3.0),
    FRIENDLY(16.0D, 4.0F, 10, 200L, "gui_turret_friendly", "container.hbm_m.turret_friendly",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage_friendly", List.of("chekhov_body", "chekhov_barrels"), 4.5, 3.0),
    JEREMY(18.0D, 5.0F, 9, 220L, "gui_turret_base", "container.hbm_m.turret_jeremy",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("jeremy_gun"), 4.5, 3.0),
    TAUON(20.0D, 6.0F, 8, 260L, "gui_turret_tau", "container.hbm_m.turret_tauon",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("tauon_cannon", "tauon_rotor"), 9.0, 6.0),
    RICHARD(20.0D, 6.0F, 8, 260L, "gui_turret_richard", "container.hbm_m.turret_richard",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("richard_launcher"), 4.5, 3.0),
    HOWARD(22.0D, 7.0F, 7, 300L, "gui_turret_howard", "container.hbm_m.turret_howard",
            PitchAxis.Z, 2.25D, 0D, 0D, "howard_carriage", List.of("howard_body", "howard_barrelstop", "howard_barrelsbottom"), 12.0, 8.0),
    MAXWELL(22.0D, 7.0F, 7, 300L, "gui_turret_maxwell", "container.hbm_m.turret_maxwell",
            PitchAxis.Z, 1.5D, 0D, 0D, "howard_carriage", List.of("maxwell_microwave"), 9.0, 6.0),
    FRITZ(24.0D, 8.0F, 6, 340L, "gui_turret_fritz", "container.hbm_m.turret_fritz",
            PitchAxis.Z, 1.5D, 0D, 0D, "chekhov_carriage", List.of("fritz_gun"), 4.5, 3.0),
    ARTY(32.0D, 12.0F, 30, 600L, "gui_turret_arty", "container.hbm_m.turret_arty",
            PitchAxis.X, 3.0D, 0D, -90D, "arty_carriage", List.of("arty_cannon", "arty_barrel"), 1.0, 0.5),
    HIMARS(40.0D, 16.0F, 40, 900L, "gui_turret_himars", "container.hbm_m.turret_himars",
            PitchAxis.X, 2.25D, 2.0D, -90D, "himars_carriage", List.of("himars_launcher", "himars_crane"), 1.0, 0.5);

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

    public ResourceLocation getGuiTexture() {
        return ResourceLocation.fromNamespaceAndPath("hbm_m", "textures/gui/weapon/" + guiTextureName + ".png");
    }

    public String getNameKey() {
        return nameKey;
    }
}
