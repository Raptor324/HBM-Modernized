package com.hbm_m.satellite;

/** 1:1 {@code SatelliteScanner} (Tiefenscan): die Arbeit macht die Neutrino-Linse ({@code ItemModLens}). */
public class SatelliteScanner extends Satellite {

    public SatelliteScanner() { }

    @Override public String getType() { return "DEPTH_SCANNER"; }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> getInfo(net.minecraft.server.level.ServerLevel world) {
        return java.util.List.of(
                net.minecraft.network.chat.Component.translatable(com.hbm_m.item.ModItems.SATELLITE_SCANNER.get().getDescriptionId())
        );
    }
}
