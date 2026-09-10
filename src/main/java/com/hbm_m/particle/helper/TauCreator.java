package com.hbm_m.particle.helper;

import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleSparkNT;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * 1:1-Port des {@code "tau"}-Zweigs aus {@code ClientProxy.effectNT}: ein Schwall Funken und ein
 * Hadronenring darueber - der Lichtbogen des Schweissgeraets.
 */
public class TauCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
                             double x, double y, double z, CompoundTag data) {

        boolean small = data.getBoolean("small");
        int count = data.getByte("count");

        for (int i = 0; i < count; i++) {
            ParticleSparkNT spark = new ParticleSparkNT(level, x, y, z,
                    rand.nextGaussian() * 0.05D, 0.05D, rand.nextGaussian() * 0.05D);
            ParticleEngineNT.INSTANCE.add(spark.makeSmall(small));
        }

        level.addParticle(ModParticleTypes.HADRON.get(), x, y, z, small ? 1D : 0D, 0D, 0D);
    }
}
