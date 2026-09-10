package com.hbm_m.satellite;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1:1-Port der statischen Ereignisliste aus {@code SatelliteRayScan} (1.7.10).
 *
 * <p>Jede Anlage, die etwas ausstrahlt, was sich aus dem Orbit messen laesst, traegt sich hier ein:
 * ein Lichtbogen, Neutronen aus einem Reaktor, hochenergetische Teilchen aus einem Beschleuniger,
 * Radar- oder Funkwellen. Der Eintrag verfaellt nach der angegebenen Zeit von selbst.</p>
 *
 * <p>Das ist die <b>sendende</b> Haelfte des Systems und vollstaendig. Die empfangende - der
 * Scan-Satellit {@code NB_RAY_SCANNER}, der die Liste per {@code survey}, {@code count},
 * {@code getinfo} und {@code getposition} abfragt - gehoert zum Satellitenzweig, von dem dieser
 * Port bislang nur {@link SatelliteHorizons} kennt. Bis der Scanner dazukommt, sammelt sich hier
 * still die richtige Datenlage an; es fehlt nur der Empfaenger.</p>
 */
public final class RayScanEvents {

    private RayScanEvents() {}

    /** Original: {@code MAX_SCAN_RANGE = 250}. */
    public static final int MAX_SCAN_RANGE = 250;

    // Original: die fuenf Ereignisarten aus {@code RayEvent}.
    public static final String INFO_ARC_FLASH = "ARC_FLASH";
    public static final String INFO_NUCLEAR = "NEUTRON_EMISSION";
    public static final String INFO_PARTICLE = "HIGH_ENERGY_PARTICLES";
    public static final String INFO_RADAR = "RADAR_WAVES";
    public static final String INFO_RADIO = "RADIO_WAVES";

    /** Original: {@code LinkedHashMap<DimPos, RayEvent>} - je Ort hoechstens ein Eintrag. */
    private static final Map<DimPos, RayEvent> EVENTS = new LinkedHashMap<>();

    /** Ort und Dimension eines Ereignisses - im Original {@code DimPos}. */
    public record DimPos(ResourceKey<Level> dimension, int x, int y, int z) {}

    /** Ein gemessenes Ereignis - im Original die innere Klasse {@code RayEvent}. */
    public record RayEvent(long expiresOn, String info, int x, int z) {}

    /** 1:1-Port von {@code reportEvent}. */
    public static void reportEvent(Level level, BlockPos pos, String info, int lifetime) {
        if (level == null || level.isClientSide()) return;

        EVENTS.put(new DimPos(level.dimension(), pos.getX(), pos.getY(), pos.getZ()),
                new RayEvent(level.getGameTime() + lifetime, info, pos.getX(), pos.getZ()));
    }

    private static boolean registered = false;

    /**
     * Haengt das Aufraeumen an den Servertick. Original: {@code updateSystem} wird einmal je
     * Sekunde gerufen, darum der Zaehler auf zwanzig Ticks.
     */
    public static void init() {
        if (registered) return;
        registered = true;

        dev.architectury.event.events.common.TickEvent.SERVER_POST.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                updateSystem(level);
                DetectorEvents.updateSystem(level);
            }
        });
    }

    /** 1:1-Port von {@code updateSystem} - im Original einmal je Sekunde. */
    public static void updateSystem(Level level) {
        EVENTS.entrySet().removeIf(entry ->
                level.dimension() == entry.getKey().dimension()
                        && level.getGameTime() > entry.getValue().expiresOn());
    }

    /**
     * Alles, was in dieser Dimension im Umkreis von {@value #MAX_SCAN_RANGE} Bloecken um den
     * angegebenen Punkt liegt - 1:1 die Auswahl aus {@code CMD_SURVEY}.
     */
    public static java.util.List<RayEvent> survey(ResourceKey<Level> dimension, int targetX, int targetZ) {
        java.util.List<RayEvent> results = new java.util.ArrayList<>();

        for (Map.Entry<DimPos, RayEvent> entry : EVENTS.entrySet()) {
            DimPos pos = entry.getKey();
            if (pos.dimension() != dimension) continue;

            int dX = pos.x() - targetX;
            int dZ = pos.z() - targetZ;

            if (dX * dX + dZ * dZ <= MAX_SCAN_RANGE * MAX_SCAN_RANGE) results.add(entry.getValue());
        }
        return results;
    }
}
