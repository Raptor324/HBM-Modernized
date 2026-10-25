package com.hbm_m.block.rail;

import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** 1:1 {@code IRailNTM} - "in retrospect, not the best name i could have chosen". */
public interface IRailNTM {

    /** Liefert den naechsten Einrastpunkt ausgehend von der Startposition. */
    Vec3NT getSnappingPos(Level world, int x, int y, int z, double trainX, double trainY, double trainZ);

    /**
     * Position auf der Schiene anhand der X/Y/Z-Bewegungsrichtung des Zuges und der Wunschgeschwindigkeit entlang der Schiene.
     * Verlaesst der Zug die Schiene in diesem Tick, ist das Ergebnis die letzte gueltige Position auf dieser Schiene.
     * Die Bewegung kommt aus der Drehung des Zuges (rueckwaerts um 180 Grad gedreht), nur die Richtung zaehlt.
     * {@link RailContext} liefert Ueberschuss und Endgierwinkel.
     */
    Vec3NT getTravelLocation(Level world, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context);

    /** Spurweite der Schiene. Passt sie nicht, entgleist der Zug. */
    TrackGauge getGauge(Level world, int x, int y, int z);

    enum TrackGauge {
        STANDARD,   // etwa 1,5 m
        NARROW      // etwa 0,75 m
    }

    /** Alles, was beim Verlassen einer Schiene gebraucht wird. */
    class RailContext {
        /** Gierwinkel, mit dem der Zug diese Schiene verlaesst */
        public float yaw;
        /** Restweg nach dem Ende dieser Schiene */
        public double overshoot;
        /** Austrittsposition dieser Schiene */
        public BlockPos pos;
        public RailContext yaw(float y) { this.yaw = y; return this; }
        public RailContext dist(double d) { this.overshoot = d; return this; }
        public RailContext pos(BlockPos d) { this.pos = d; return this; }
    }

    /** Zusatzinfos wie Prellbock-Halt und Art der Pruefung. */
    class MoveContext {
        public RailCheckType type;
        public double collisionBogieDistance;
        /** ob ein Prellbock o.ae. greift */
        public boolean collision = false;
        /** wie viel des Wegs abgeschnitten wurde */
        public double overshoot;

        public MoveContext(RailCheckType type, double collisionBogieDistance) {
            this.type = type;
            this.collisionBogieDistance = collisionBogieDistance;
        }
    }

    enum RailCheckType {
        CORE,
        FRONT,
        BACK,
        OTHER
    }

    /** Original {@code fauxpointtwelve.BlockPos(double, double, double)}: abgerundete Koordinaten. */
    static BlockPos pos(double x, double y, double z) {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}
