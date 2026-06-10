package com.hbm_m.particle.helper;

import com.hbm_m.particle.nt.FleijaCloudParticle;
import com.hbm_m.particle.nt.ParticleEngineNT;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Клиентское облако Fleija. Порт {@code com.hbm.particle.helper.CloudCreator} (FLEIJA).
 */
public final class CloudCreator implements IParticleCreator {

    public static void composeEffect(Level level, double x, double y, double z, int radius) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "cloud");
        tag.putInt("radius", radius);
        if (level instanceof ServerLevel serverLevel) {
            IParticleCreator.sendPacket(serverLevel, x, y, z, 1000, tag);
        }
    }

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag) {
        int radius = tag.contains("radius") ? tag.getInt("radius") : 20;
        FleijaCloudParticle particle = new FleijaCloudParticle(level, x, y, z, radius);
        ParticleEngineNT.INSTANCE.add(particle);
    }
}
