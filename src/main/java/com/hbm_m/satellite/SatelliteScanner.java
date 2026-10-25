package com.hbm_m.satellite;

/** 1:1 {@code SatelliteScanner} (Tiefenscan): die Arbeit macht die Neutrino-Linse ({@code ItemModLens}). */
public class SatelliteScanner extends Satellite {

    public SatelliteScanner() { }

    @Override public String getType() { return "DEPTH_SCANNER"; }
}
