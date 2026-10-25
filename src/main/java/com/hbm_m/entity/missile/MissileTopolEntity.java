package com.hbm_m.entity.missile;

import api.hbm_m.entity.IRadarDetectable;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.explosion.NuclearExplosionAPI;
import com.hbm_m.explosion.NuclearExplosionConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * RT-2PM2 Topol-M (NATO SS-27 Sickle B) - die interkontinentale Feststoffrakete des gleichnamigen
 * Werfers.
 *
 * <p><b>Reale Vorlage:</b> 22,5 m lang, 1,9 m Durchmesser, 47 t Startmasse, drei Feststoffstufen,
 * Reichweite 11.000 km, <b>ein</b> Gefechtskopf mit rund 800 kt. Der Mehrfachkopf gehoert zur
 * Weiterentwicklung RS-24 Jars - dieselbe Rakete, anderer Bug.
 *
 * <p><b>Was uebernommen ist:</b> die Abmessungen (der Rumpf ist als Rotationskoerper aus dem
 * Laengsschnitt gebaut, 22,5 x 1,9 Bloecke) und der Feststoffantrieb, der sich hier in einer
 * langen Startphase ausdrueckt: {@link #BOOST_TICKS} sind sechs Sekunden bis Reisetempo, gegenueber
 * drei bei der 9M723. Nicht uebernommen ist die Gipfelhoehe - eine echte Topol-M steigt auf ueber
 * 1.000 km, die Welt endet bei y=320.
 */
public class MissileTopolEntity extends FastBallisticMissileEntity {

    /** 1.600 m/s bei 1 Block = 1 m und 20 Ticks/s, also Mach 4,7. */
    public static final double CRUISE_GROUND_SPEED = 80.0D;

    /** Gipfelhoehe, knapp unter der Bauhoehe von 320. */
    public static final double MAX_APOGEE = 300.0D;

    /** Startphase: die drei Feststoffstufen brauchen sechs Sekunden auf Reisetempo. */
    private static final int BOOST_TICKS = 120;

    /** Der Wiedereintrittskoerper bremst kaum ab. */
    private static final double TERMINAL_FRACTION = 0.85D;

    /**
     * Sprengkraft als Vielfaches des konfigurierten Raketenradius, hergeleitet aus dem Vorbild.
     *
     * <p>Bei 1 Block = 1 m: ein Bodenzuender von 1 Mt hinterlaesst in trockenem Boden einen
     * Krater von rund 366 m Durchmesser, also 183 m Radius. Kraterradius waechst mit
     * Y^(1/3,4), fuer 800 kt sind das 183 * 0,8^0,294 = <b>rund 170 m</b>. Der Standardwert von
     * {@code missileRadius} ist 100, also 1,7.
     *
     * <p>Aufgerundet auf 2,3 (230 Bloecke), weil der sichtbare Trichter deutlich kleiner
     * ausfaellt als die Strahlenreichweite - die aeusseren Strahlen raeumen nur noch Oberflaeche.
     *
     * <p>Der Rest der realen Wirkung passt nicht in die Welt: schwere Zerstoerung reicht bei
     * 800 kt ueber 2,4 km, das waeren 2.400 Bloecke.
     */
    private static final double YIELD_FACTOR = 2.3D;

    /**
     * Groesse der Pilzwolke. Die Ableitung aus der Sprengkraft waechst nur mit der Wurzel und
     * kaeme hier auf 1,96 - kaum mehr als das Doppelte einer Fat Man. Deshalb der Hoechstwert
     * der Wolkendarstellung. Auch der ist noch stark untertrieben: eine echte 800-kt-Wolke
     * steigt auf ueber 18 km, die Welt endet bei 320.
     */
    private static final float MUSHROOM_SCALE = 5.0F;

    /**
     * Standzeit der Wolke. Ohne Vorgabe waeren es 45 Sekunden mal Groesse, bei voller Groesse
     * also dreieinhalb Minuten - die Wolke stand laenger als die halbe Partie.
     */
    private static final int MUSHROOM_LIFE_TICKS = 900;

    /**
     * Reichweite des Niederschlags als Vielfaches des Kraterradius. Vorgabe im Mod ist 2,5; der
     * Krater soll hier die Hauptwirkung sein, nicht die Verseuchung drumherum.
     */
    private static final float FALLOUT_FACTOR = 0.8F;

    public MissileTopolEntity(EntityType<? extends MissileTopolEntity> type, Level level) {
        super(type, level);
        this.health = 120;
    }

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
    protected void onMissileImpact(BlockPos pos) {
        if (level().isClientSide) {
            return;
        }
        int radius = (int) Math.max(1, ModClothConfig.get().missileRadius * YIELD_FACTOR);
        NuclearExplosionConfig cfg = NuclearExplosionConfig.builder(radius)
                .fallout(true)
                .radiation(true)
                .mushroomType(0)
                .mushroomScale(MUSHROOM_SCALE)
                .mushroomLifeTicks(MUSHROOM_LIFE_TICKS)
                .falloutFactor(FALLOUT_FACTOR)
                .build();
        NuclearExplosionAPI.start(level(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, cfg);
    }

    @Override
    public IRadarDetectable.RadarTargetType getTargetType() {
        return IRadarDetectable.RadarTargetType.MISSILE_TIER4;
    }

    @Override
    protected float getContrailScale() {
        return 1.5F;
    }
}
