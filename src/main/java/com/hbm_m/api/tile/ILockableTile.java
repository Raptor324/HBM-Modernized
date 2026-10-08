package com.hbm_m.api.tile;

/**
 * Die Schloss-Schnittstelle von {@code TileEntityLockableBase} (1.7.10): Stifte, Sperre und
 * Knack-Faktor. Blockentities, die abschliessbar sind (Kisten, Tresor, Massenspeicher, Aktenschrank,
 * Tueren), implementieren sie und halten den Zustand in einem gemeinsamen {@link LockState};
 * {@code ItemLock} (Vorhaengeschloss) setzt sie.
 */
public interface ILockableTile {

    /** Der gemeinsame Schlosszustand (eine Basis fuer alle abschliessbaren Blockentities). */
    LockState getLockState();

    default boolean isLocked() { return getLockState().isLocked; }
    void lock();
    void unlock();
    void setPins(int pins);
    default int getPins() { return getLockState().lock; }
    void setMod(double mod);
    default double getMod() { return getLockState().lockMod; }

    /** Original TileEntityLockableBase.cheesable: das Schloss laesst sich mit key_kit nachmachen. */
    default boolean isCheesable() { return getLockState().cheesable; }
    default void setCheesable(boolean cheesable) { getLockState().cheesable = cheesable; }
}
