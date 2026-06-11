package com.hbm_m.particle.helper;

import com.hbm_m.particle.nt.MukeFlashParticle;
import com.hbm_m.particle.nt.MukeWaveParticle;
import com.hbm_m.particle.nt.ParticleEngineNT;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Порт {@code ClientProxy} / {@code effectNT} ветки {@code type == "muke"} (1.7.10).
 */
public class MukeParticleCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag) {
        boolean balefire = tag.getBoolean("balefire");

        ParticleEngineNT.INSTANCE.add(new MukeWaveParticle(level, x, y, z));
        ParticleEngineNT.INSTANCE.add(new MukeFlashParticle(level, x, y, z, balefire));

        if (player != null) {
            player.hurtDuration = 15;
            player.hurtTime = 15;
        }
    }
}
