package com.hbm_m.particle.helper;

import com.hbm_m.network.AuxParticlePacket;
import com.hbm_m.network.ModPacketHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/**
 * Creates a custom particle on the client from NBT data.
 * Server sends via {@link #sendPacket(ServerLevel, double, double, double, int, CompoundTag)}.
 */
public interface IParticleCreator {

    void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag);

    static void sendPacket(ServerLevel level, double x, double y, double z, int range, CompoundTag data) {
        PacketDistributor.TargetPoint target = new PacketDistributor.TargetPoint(x, y, z, range, level.dimension());
        ModPacketHandler.INSTANCE.send(PacketDistributor.NEAR.with(() -> target), new AuxParticlePacket(data, x, y, z));
    }
}
