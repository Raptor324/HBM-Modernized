package com.hbm_m.util.explosions.nuclear;

import com.hbm_m.network.AuxParticlePacket;
import com.hbm_m.network.ModPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public final class ExplosionPacketHelper {
    private ExplosionPacketHelper() {}

    public static void sendLargeExplosionPacket(ServerLevel level, double x, double y, double z) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "explosionLarge");
        data.putFloat("scale", 1.0F);

        int range = 350;
        ModPacketHandler.sendToPlayersNear(
            level, x, y, z, range,
            ModPacketHandler.AUX_PARTICLE,
            new AuxParticlePacket(data, x, y, z)
        );
    }
}