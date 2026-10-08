package com.hbm_m.world.generator;

import java.util.ArrayDeque;

import net.minecraft.world.level.LevelAccessor;

/**
 * 1:1 {@code com.hbm.world.generator.TimedGenerator}: Bauauftraege in Reihenfolge. Port: Das Original arbeitete die
 * Liste ueber mehrere Ticks ab (10 ms je Tick); hier laufen sie gesammelt per {@link #runAll()} im Bau-Puffer -
 * gleiche Reihenfolge, gleiches Ergebnis.
 */
public class TimedGenerator {

    private static final ThreadLocal<ArrayDeque<ITimedJob>> operations = ThreadLocal.withInitial(ArrayDeque::new);

    public static void addOp(LevelAccessor world, ITimedJob job) {
        operations.get().add(job);
    }

    /** Arbeitet alle Auftraege ab, auch die waehrenddessen neu eingereihten. */
    public static void runAll() {
        ArrayDeque<ITimedJob> list = operations.get();
        while (!list.isEmpty()) {
            list.poll().work();
        }
    }

    /** Verwirft liegengebliebene Auftraege (nach Fehlern). */
    public static void clear() {
        operations.get().clear();
    }

    public interface ITimedJob {
        void work();
    }
}
