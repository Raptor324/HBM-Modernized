package com.hbm_m.satellite;

import net.minecraft.server.level.ServerLevel;

/**
 * 1:1 {@code SatelliteRelay}: vergibt beim Erreichen des Orbits die FOEQ-Errungenschaft an alle Spieler. Wie im
 * Original ohne super.onOrbit (kein Ziel, keine RTTY-Meldung); das Radar nutzt ihn als Relais fuer Satellitenbefehle.
 */
public class SatelliteRelay extends Satellite {

    public SatelliteRelay() { }

    @Override public String getType() { return "RX/TX"; }

    @Override
    public void onOrbit(ServerLevel world, double x, double y, double z) {
        com.hbm_m.advancement.ModAdvancements.grantAll(world, com.hbm_m.advancement.ModAdvancements.FOEQ);
    }
}
