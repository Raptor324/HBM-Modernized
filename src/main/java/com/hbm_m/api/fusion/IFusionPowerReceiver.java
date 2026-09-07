package com.hbm_m.api.fusion;

/**
 * 1:1-Port von {@code com.hbm.tileentity.machine.fusion.IFusionPowerReceiver} (1.7.10).
 * Wird von allen Geraeten implementiert, die am Plasma-Netz des Fusionstorus haengen.
 */
public interface IFusionPowerReceiver {

    /**
     * true, wenn dieses Geraet die geteilte Plasma-Ausgangsleistung des Torus abnimmt,
     * false, wenn es nur den (nicht geteilten) Neutronenfluss verwendet.
     */
    boolean receivesFusionPower();

    /**
     * @param fusionPower  Ausgang pro Port, bereits um die Anzahl angeschlossener Abnehmer korrigiert
     *                     (d.h. wenn sich Boiler die Leistung teilen).
     * @param neutronPower Fester Wert aus dem Rezept.
     */
    void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b);
}
