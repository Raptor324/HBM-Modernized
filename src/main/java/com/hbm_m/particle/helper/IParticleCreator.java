package com.hbm_m.particle.helper;

import com.hbm_m.network.AuxParticlePacket;
import com.hbm_m.network.ModPacketHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Creates a custom particle on the client from NBT data.
 * Server sends via {@link #sendPacket(ServerLevel, double, double, double, int, CompoundTag)}.
 */
public interface IParticleCreator {

    void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag);

    static void sendPacket(ServerLevel level, double x, double y, double z, int range, CompoundTag data) {
        ModPacketHandler.sendToPlayersNear(level, x, y, z, range, ModPacketHandler.AUX_PARTICLE,
            new AuxParticlePacket(data, x, y, z));
    }
}
