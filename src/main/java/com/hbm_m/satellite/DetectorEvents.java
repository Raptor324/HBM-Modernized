package com.hbm_m.satellite;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 1:1-Port der statischen Ereignisliste aus {@code SatelliteDetector} (1.7.10).
 *
 * <p>Jede Detonation und jede starke Strahlungsquelle traegt sich hier ein. Der Kniff des
 * Originals ist die <b>eingebaute Ungenauigkeit</b>: ein schwaches Ereignis wird um bis zu
 * zehntausend Bloecke daneben gemeldet, ein starkes nur um fuenfhundert. Aus einer Zuendung von
 * Nachbarn laesst sich also die Richtung ablesen, aber nicht der Ort - dafuer braucht es eine
 * grosse Bombe oder mehrere Messungen.</p>
 *
 * <p>Wie bei {@link RayScanEvents} ist das hier die <b>sendende</b> Haelfte und vollstaendig; der
 * Detektorsatellit, der die Liste abfragt, gehoert zum Satellitenzweig, von dem dieser Port
 * bislang nur {@link SatelliteHorizons} kennt.</p>
 */
public final class DetectorEvents {

    private DetectorEvents() {}

    /** Original: eine Mininuke bleibt fuenfzehn Sekunden sichtbar. */
    public static final int DURATION_LOW = 15 * 20;
    /** Original: Beschleuniger und Radar nur eine halbe Sekunde. */
    public static final int DURATION_MEDIUM = 20 / 2;
    /** Original: eine ausgewachsene Atombombe eine volle Minute. */
    public static final int DURATION_HIGH = 60 * 20;

    public static final double INACCURACY_LOW = 10_000D;
    public static final double INACCURACY_MEDIUM = 2_500D;
    public static final double INACCURACY_HIGH = 500D;

    /** Original: {@code BurstIntensity}. */
    public enum BurstIntensity { LOW, MEDIUM, HIGH }

    /** Ein gemeldeter Ausbruch - im Original {@code RadiationBurst}. */
    public record RadiationBurst(ResourceKey<Level> dimension, long expiresOn,
                                 BurstIntensity intensity, int x, int z) {}

    private static final List<RadiationBurst> BURSTS = new ArrayList<>();

    /** 1:1-Port von {@code reportEvent} samt der Streuung im Konstruktor. */
    public static void reportEvent(Level level, int lifetime, BurstIntensity intensity, double x, double z) {
        if (level == null || level.isClientSide()) return;

        double inaccuracy = switch (intensity) {
            case LOW -> INACCURACY_LOW;
            case MEDIUM -> INACCURACY_MEDIUM;
            case HIGH -> INACCURACY_HIGH;
        };

        int bx = (int) (Math.floor(x) + level.getRandom().nextGaussian() * inaccuracy);
        int bz = (int) (Math.floor(z) + level.getRandom().nextGaussian() * inaccuracy);

        BURSTS.add(new RadiationBurst(level.dimension(), level.getGameTime() + lifetime,
                intensity, bx, bz));
    }

    /** 1:1-Port von {@code updateSystem}. */
    public static void updateSystem(Level level) {
        BURSTS.removeIf(b -> level.dimension() == b.dimension() && level.getGameTime() > b.expiresOn());
    }

    /** Alles, was in dieser Dimension gerade gemeldet ist. */
    public static List<RadiationBurst> bursts(ResourceKey<Level> dimension) {
        List<RadiationBurst> out = new ArrayList<>();
        for (RadiationBurst b : BURSTS) {
            if (b.dimension() == dimension) out.add(b);
        }
        return out;
    }
}
