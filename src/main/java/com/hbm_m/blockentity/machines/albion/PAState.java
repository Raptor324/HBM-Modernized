package com.hbm_m.blockentity.machines.albion;

/**
 * 1:1-Port von {@code TileEntityPASource.PAState} (1.7.10): der Zustand des Strahls, wie ihn die
 * Quelle anzeigt. Die Farbe ist dieselbe wie im Original.
 */
public enum PAState {

    /** Kein Teilchen unterwegs. */
    IDLE(0x8080ff),
    /** Laeuft ohne Beanstandung. */
    RUNNING(0xffff00),
    /** Rezept fertig. */
    SUCCESS(0x00ff00),
    /** Angehalten, weil der Strahl in ungeladene Bereiche gelaufen ist. */
    PAUSE_UNLOADED(0x808080),
    /** Absturz durch zu grosse Streuung. */
    CRASH_DEFOCUS(0xff0000),
    /** Absturz, weil der Strahl die Strecke verlassen hat. */
    CRASH_DERAIL(0xff0000),
    /** Absturz, weil ein Bauteil von der falschen Seite getroffen wurde. */
    CRASH_CANNOT_ENTER(0xff0000),
    /** Absturz mangels Kuehlung. */
    CRASH_NOCOOL(0xff0000),
    /** Absturz mangels Energie. */
    CRASH_NOPOWER(0xff0000),
    /** Absturz, weil in Quadrupol oder Dipol keine Spule steckt. */
    CRASH_NOCOIL(0xff0000),
    /** Absturz, weil der Impuls die Spule ueberfordert. */
    CRASH_OVERSPEED(0xff0000),
    /** Absturz, weil das Rezept mehr Impuls verlangt. */
    CRASH_UNDERSPEED(0xff0000),
    /** Absturz, weil kein Rezept passt. */
    CRASH_NORECIPE(0xff0000);

    public final int color;

    PAState(int color) {
        this.color = color;
    }

    public boolean isCrash() {
        return name().startsWith("CRASH_");
    }
}
