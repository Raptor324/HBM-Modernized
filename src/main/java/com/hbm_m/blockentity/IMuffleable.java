package com.hbm_m.blockentity;

/**
 * Original: das Feld {@code muffled} aus {@code TileEntityLoadedBase}, das {@code ItemMuffler} setzt.
 * Im Port haben die Blockentitaeten verschiedene Basisklassen, daher als Schnittstelle.
 */
public interface IMuffleable {

    boolean isMuffled();

    void setMuffled(boolean muffled);
}
