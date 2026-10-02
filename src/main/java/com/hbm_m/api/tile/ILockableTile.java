package com.hbm_m.api.tile;

/**
 * Die Schloss-Schnittstelle von {@code TileEntityLockableBase} (1.7.10): Stifte, Sperre und
 * Knack-Faktor. Blockentities, die abschliessbar sind (Kisten, Tresore, Tueren), implementieren sie;
 * {@code ItemLock} (Vorhaengeschloss) setzt sie.
 */
public interface ILockableTile {
    boolean isLocked();
    void lock();
    void unlock();
    void setPins(int pins);
    int getPins();
    void setMod(double mod);
    double getMod();

    /** Original TileEntityLockableBase.cheesable: das Schloss laesst sich mit key_kit nachmachen. */
    default boolean isCheesable() { return true; }
}
