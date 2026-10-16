package com.hbm_m.blockentity.machines.albion;

/**
 * Was ein {@link Particle} von seiner Quelle braucht. Im Original ruft das Teilchen direkt in
 * {@code TileEntityPASource} hinein; hier steht eine schmale Schnittstelle dazwischen, damit die
 * Teilchenklasse nichts von der Quelle wissen muss.
 */
public interface PAParticleHost {

    /** Meldet den neuen Zustand - Absturzgrund oder Erfolg. */
    void updateState(PAState state);

    /** Merkt sich den zuletzt gemessenen Impuls fuer die Anzeige. */
    void setLastSpeed(int momentum);
}
