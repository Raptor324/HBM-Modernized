package com.hbm_m.satellite;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

/** 1:1 {@code SatelliteResonator} (Xenium-Resonator): teleportiert den Spieler zum Ziel (y &lt; 0 = Oberflaeche). */
public class SatelliteResonator extends Satellite {

    public SatelliteResonator() { }

    @Override public String getType() { return "XEN_RELAY"; }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> getInfo(ServerLevel world) {
        return java.util.List.of(
                net.minecraft.network.chat.Component.translatable(com.hbm_m.item.ModItems.SATELLITE_XENIUM_RESONATOR.get().getDescriptionId())
        );
    }

    private long lastUse = 0L;

    @Override
    public void onCoordAction(ServerLevel world, Player player, int x, int y, int z) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!world.getWorldBorder().isWithinBounds(x, z)) return;
        long now = world.getGameTime();
        if (now - lastUse < 20L) return;
        lastUse = now;

        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.stopRiding();
        world.getChunk(x >> 4, z >> 4);
        if (y < 0) y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        y = net.minecraft.util.Mth.clamp(y, world.getMinBuildHeight(), world.getMaxBuildHeight());
        sp.connection.teleport(x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
