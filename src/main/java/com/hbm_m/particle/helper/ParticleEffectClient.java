package com.hbm_m.particle.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only entry point for custom NT particles (called from AuxParticlePacket).
 */
public final class ParticleEffectClient {

    private ParticleEffectClient() {}

    public static void effectNT(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ClientLevel level = mc.level;
            if (level == null) return;
            Player player = mc.player;
            String type = data.getString("type");
            double x = data.getDouble("posX");
            double y = data.getDouble("posY");
            double z = data.getDouble("posZ");
            RandomSource rand = RandomSource.create();
            IParticleCreator creator = ParticleCreators.particleCreators().get(type);
            if (creator != null) {
                creator.makeParticle(level, player, rand, x, y, z, data);
            }
        });
    }
}
