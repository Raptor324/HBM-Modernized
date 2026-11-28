package com.hbm_m.satellite;

/** 1:1 {@code SatelliteLunarMiner} (Mond-Bergbauschiff), Beutepool {@link SatellitePools#POOL_SAT_LUNAR}. */
public class SatelliteLunarMiner extends SatelliteMiner {

    @Override public String getType() { return "LUNAR_MINER"; }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> getInfo(net.minecraft.server.level.ServerLevel world) {
        int pct = (int) Math.round(this.progress * 100);
        return java.util.List.of(
                net.minecraft.network.chat.Component.translatable(com.hbm_m.item.ModItems.SATELLITE_MINER_LUNAR.get().getDescriptionId()),
                net.minecraft.network.chat.Component.translatable("satellite.minerprogress", pct + "%")
        );
    }
}
