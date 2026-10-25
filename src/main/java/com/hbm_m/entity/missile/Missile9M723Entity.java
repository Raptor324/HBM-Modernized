package com.hbm_m.entity.missile;

import api.hbm_m.entity.IRadarDetectable;
import com.hbm_m.explosion.MissileWarheadEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 9M723 - die quasiballistische Rakete des Iskander-M (9K720, NATO SS-26 Stone).
 *
 * <p><b>Reale Vorlage:</b> 7,3 m lang, 0,92 m Durchmesser, 3.750 kg Startmasse, Gefechtskopf
 * 480-700 kg, Reichweite 500 km, Hoechstgeschwindigkeit 2.100 m/s (Mach 6-7).
 *
 * <p><b>Was davon uebernommen wurde und was nicht.</b> Zwei Werte sind bewusst kleiner als beim
 * Vorbild. Die Reisegeschwindigkeit liegt bei {@link #CRUISE_GROUND_SPEED} = 70 Bloecken pro Tick,
 * also 1.400 m/s oder Mach 4,1 - originalgetreu waeren 105 (2.100 m/s), aber dann ist der Flug
 * kaum noch zu sehen. Und die Gipfelhoehe: eine echte 9M723 steigt auf rund 50 km, waehrend die
 * Welt bei y=320 endet - die Bahn wird deshalb auf {@link #MAX_APOGEE} gedrueckt.
 *
 * <p><b>Warum das die Treffgenauigkeit nicht anruehrt.</b> {@code MissileBaseEntity} loest die
 * Bahn ueber {@code decelY = 2/A} und {@code accelXZ = len/A²}; beide haengen von der Gipfelhoehe
 * ab, und der Einschlagpunkt bleibt fuer jedes A der Zielpunkt. Und weil {@code velocity} dort ein
 * reiner Zeitfaktor ist (die Position rueckt um {@code motion * velocity} vor, und {@code motion}
 * wird ebenfalls damit integriert), aendert auch ein beliebiges Tempoprofil nur, wie schnell die
 * Bahn abgeflogen wird - nicht, wo sie endet.
 *
 * <p>Das gilt allerdings nur fuer die stetige Bahn. Der Tick integriert sie in Euler-Schritten,
 * und bei siebzig bis hundert Bloecken pro Schritt ist die Bahn innerhalb eines Schrittes viel zu
 * stark gekruemmt:
 * gemessen 16 Bloecke Fehlschuss auf 3.000 Bloecke, ueber 170 auf 10.000. Deshalb deckelt
 * {@code FastBallisticMissileEntity#maxStepDistance()} die Schrittweite auf einen Block.
 */
public abstract class Missile9M723Entity extends FastBallisticMissileEntity {

    /** 1.400 m/s bei 1 Block = 1 m und 20 Ticks/s. Entspricht Mach 4,1 auf Meereshoehe. */
    public static final double CRUISE_GROUND_SPEED = 70.0D;

    /**
     * Gipfelhoehe in Bloecken. Bewusst unter der Bauhoehe von 320, damit die Rakete auf ihrer
     * ganzen Bahn sichtbar bleibt und nicht ueber der Welt verschwindet.
     */
    public static final double MAX_APOGEE = 280.0D;

    /** Startphase: erst nach dieser Zeit ist die Reisegeschwindigkeit erreicht (3 Sekunden). */
    private static final int BOOST_TICKS = 60;

    /** Endanflug: die Rakete faellt auf diesen Bruchteil der Reisegeschwindigkeit zurueck. */
    private static final double TERMINAL_FRACTION = 0.55D;

    /** Wie schnell im Sinkflug abgebremst wird, als Bruchteil der Reisegeschwindigkeit pro Tick. */
    private static final double BRAKE_PER_TICK = 0.02D;

    protected Missile9M723Entity(EntityType<? extends Missile9M723Entity> type, Level level) {
        super(type, level);
        this.health = 80;
    }

    // -- Tempoprofil, den Rest erledigt FastBallisticMissileEntity ---------------

    @Override
    protected double cruiseGroundSpeed() {
        return CRUISE_GROUND_SPEED;
    }

    @Override
    protected double maxApogee() {
        return MAX_APOGEE;
    }

    @Override
    protected int boostTicks() {
        return BOOST_TICKS;
    }

    @Override
    protected double terminalFraction() {
        return TERMINAL_FRACTION;
    }

    @Override
    protected double brakePerTick() {
        return BRAKE_PER_TICK;
    }

    @Override
    public IRadarDetectable.RadarTargetType getTargetType() {
        return IRadarDetectable.RadarTargetType.MISSILE_TIER3;
    }

    @Override
    protected float getContrailScale() {
        return 0.85F;
    }

    // -- Gefechtskoepfe --------------------------------------------------------

    /** 9M723 mit Splittergefechtskopf - die Standardausfuehrung, ~480 kg. */
    public static class HighExplosive extends Missile9M723Entity {
        public HighExplosive(EntityType<? extends HighExplosive> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadTier3Large(this, server, pos, false);
            }
        }
    }

    /** 9M723 mit Bunkerbrechergefechtskopf. */
    public static class Buster extends Missile9M723Entity {
        public Buster(EntityType<? extends Buster> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                MissileWarheadEffects.warheadBusterTier2(this, server, pos);
            }
        }
    }
}
