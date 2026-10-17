package com.hbm_m.api.space;

import net.minecraft.world.level.Level;

/**
 * Meldung einer nuklearen Detonation an Space-Addons (Original {@code CelestialNukeShockHandler.trigger(world, x, z,
 * blastRadius)}: Planeten-Erschuetterung am Himmel). Nur serverseitig.
 *
 * @param level       Welt der Detonation (Server)
 * @param x           Zentrum X
 * @param y           Zentrum Y
 * @param z           Zentrum Z
 * @param blastRadius Sprengradius in Bloecken (Original {@code params.blastRadius} bzw. {@code EntityNukeExplosionMK5.length})
 * @param kind        Quelle der Detonation
 */
public record NuclearDetonation(Level level, double x, double y, double z, float blastRadius, Kind kind) {

    /** Quelle der Meldung. */
    public enum Kind {
        /** {@code ExplosionNukeSmall} mit {@code miniNuke = true} (Mini-Nukes, Fat-Man-Granaten, Tiny Tot ...). */
        MINI_NUKE,
        /** Grosse MK5-Explosion. HBM meldet sie (noch) nicht selbst - das Addon erkennt sie ueber das Entity-Join-Event. */
        MK5
    }

    /** Listener fuer {@link SpaceApi#onNuclearDetonation(Listener)} - reine Java-Schnittstelle, loaderneutral. */
    @FunctionalInterface
    public interface Listener {
        void onDetonation(NuclearDetonation detonation);
    }
}
