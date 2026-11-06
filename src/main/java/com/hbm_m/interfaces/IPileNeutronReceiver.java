package com.hbm_m.interfaces;

/**
 * Port of NEXT {@code com.hbm.interfaces.IPileNeutronReceiver}: a neutron sink in the
 * CP-1 pile neutron-transport chain. Implementors so far are
 * {@link com.hbm_m.blockentity.machines.pile.BlockEntityPileNeutronDetector},
 * {@link com.hbm_m.blockentity.machines.pile.BlockEntityPileFuel} and
 * {@link com.hbm_m.blockentity.machines.pile.BlockEntityPileBreedingFuel}; the remaining
 * pile machines (source) implement it when they land (NEXT: PileNeutronHandler
 * calls {@code receiveNeutrons} on receiver tiles at the end of each neutron stream).
 */
public interface IPileNeutronReceiver {

    void receiveNeutrons(int n);
}
