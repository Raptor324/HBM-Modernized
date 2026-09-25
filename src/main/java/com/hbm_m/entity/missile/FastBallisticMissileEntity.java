package com.hbm_m.entity.missile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Basis fuer Raketen, die deutlich schneller fliegen als die Vorlage aus 1.7.10 vorsah.
 *
 * <p>Die Standardrakete der Mod steigert ihren Zeitfaktor {@code velocity} bis 4 und fliegt eine
 * Bahn, deren Gipfelhoehe gleich der Schussweite ist - bei einem Schuss ueber 3.000 Bloecke also
 * 3.000 Bloecke hoch, weit oberhalb der Bauhoehe. Diese Klasse loest beides:
 *
 * <ul>
 *   <li><b>Gedrueckte Bahn.</b> {@link #maxApogee()} deckelt die Gipfelhoehe. Der Einschlagpunkt
 *       bleibt erhalten, weil {@code MissileBaseEntity} die Bahn fuer jede Gipfelhoehe neu
 *       aufloest ({@code decelY = 2/A}, {@code accelXZ = len/A²}).</li>
 *   <li><b>Echtes Tempo.</b> {@link #cruiseGroundSpeed()} ist eine Bodengeschwindigkeit in
 *       Bloecken pro Tick. Umgerechnet wird sie ueber {@code speedScale = A / len}, denn
 *       {@code velocity} ist ein reiner Zeitfaktor und keine Geschwindigkeit.</li>
 * </ul>
 *
 * <p>Der Preis dafuer ist die Schrittweite: bei zig Bloecken pro Tick laeuft die Euler-Integration
 * des Ticks aus dem Ruder, deshalb deckelt {@link #maxStepDistance()} auf einen Block je
 * Teilschritt. Die Chunk-Vorausladung in {@code MissileBaseEntity} haengt ebenfalls daran.
 */
public abstract class FastBallisticMissileEntity extends MissileBaseEntity {

    /** Bodengeschwindigkeit in Bloecken/Tick - die Groesse, die das Profil tatsaechlich steuert. */
    private double groundSpeed = 0.0D;

    /**
     * Umrechnung Bodengeschwindigkeit -> Zeitfaktor, also {@code A / len}. Wird beim Start
     * gesetzt, sobald Schussweite und Gipfelhoehe feststehen.
     */
    private double speedScale = 1.0D;

    protected FastBallisticMissileEntity(EntityType<? extends FastBallisticMissileEntity> type, Level level) {
        super(type, level);
    }

    // -- Profil der Unterklasse ------------------------------------------------

    /** Reisegeschwindigkeit in Bloecken pro Tick. 1 Block/Tick = 20 m/s. */
    protected abstract double cruiseGroundSpeed();

    /** Obergrenze der Gipfelhoehe in Bloecken. Kuerzere Schuesse behalten ihre flachere Bahn. */
    protected abstract double maxApogee();

    /** Ticks vom Start bis zur Reisegeschwindigkeit. */
    protected int boostTicks() {
        return 60;
    }

    /** Anteil der Reisegeschwindigkeit, auf den im Sinkflug abgebremst wird. */
    protected double terminalFraction() {
        return 0.55D;
    }

    /** Bremsrate im Sinkflug, als Anteil der Reisegeschwindigkeit pro Tick. */
    protected double brakePerTick() {
        return 0.02D;
    }

    // -- Bahn ------------------------------------------------------------------

    @Override
    protected double apogeeFor(double len) {
        return Math.min(len, maxApogee());
    }

    @Override
    protected void onTrajectorySolved(double len, double apogee) {
        this.speedScale = apogee / Math.max(1.0D, len);
        this.groundSpeed = 0.0D;
    }

    // -- Tempoprofil -----------------------------------------------------------

    @Override
    protected double maxVelocity() {
        return cruiseGroundSpeed() * this.speedScale;
    }

    /**
     * Startphase quadratisch, damit die ersten Sekunden sichtbar langsam sind und man die Rakete
     * die Rampe verlassen sieht. Im Steigflug wird auf Reisegeschwindigkeit hochgezogen, im
     * Sinkflug wieder abgebremst.
     */
    @Override
    protected void updateVelocity() {
        double cruise = cruiseGroundSpeed();

        if (this.getDeltaMovement().y > 0.0D) {
            double t = Math.min(1.0D, this.tickCount / (double) boostTicks());
            this.groundSpeed = Math.max(this.groundSpeed, cruise * t * t);
        } else {
            double terminal = cruise * terminalFraction();
            if (this.groundSpeed > terminal) {
                this.groundSpeed = Math.max(terminal, this.groundSpeed - cruise * brakePerTick());
            }
        }

        this.velocity = this.groundSpeed * this.speedScale;
    }

    /**
     * Ein Integrationsschritt darf hoechstens einen Block weit fuehren. Bei zig Bloecken pro Tick
     * wuerde ein einziger Euler-Schritt die Bahn sichtbar verfaelschen: bei 105 gemessen 16 Bloecke
     * Fehlschuss auf 3.000 Bloecke Entfernung und ueber 170 auf 10.000. Mit dieser Deckelung
     * bleibt der Fehler unter einem Block, bei etwa 20-35 Teilschritten pro Tick - genau die
     * Aufloesung, mit der die Kollisionspruefung die Strecke ohnehin schon abtastet.
     */
    @Override
    protected double maxStepDistance() {
        return 1.0D;
    }

    /** Aktuelle Bodengeschwindigkeit in Bloecken/Tick - fuer Anzeigen und Tests. */
    public double getGroundSpeed() {
        return this.groundSpeed;
    }

    /** Aktuelle Machzahl, gerundet auf eine Nachkommastelle. */
    public double getMachNumber() {
        // 1 Block/Tick = 20 m/s, Schallgeschwindigkeit 343 m/s.
        return Math.round(this.groundSpeed * 20.0D / 343.0D * 10.0D) / 10.0D;
    }

    /** Umrechnung Bodengeschwindigkeit -> Zeitfaktor, fuer abtrennende Gefechtskoepfe. */
    protected double speedScale() {
        return this.speedScale;
    }

    // -- Persistenz ------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("GroundSpeed", this.groundSpeed);
        tag.putDouble("SpeedScale", this.speedScale);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.groundSpeed = tag.getDouble("GroundSpeed");
        this.speedScale = tag.contains("SpeedScale") ? tag.getDouble("SpeedScale") : 1.0D;
    }
}
