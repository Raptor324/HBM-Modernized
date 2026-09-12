package com.hbm_m.client.particle;

import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public class ExplosionClientHandler {

    public static void handleClientParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag) {
        float waveScale = tag.getFloat("waveScale");
        float cloudScale = tag.getFloat("cloudScale");
        float cloudSpeedMult = tag.getFloat("cloudSpeedMult");
        int cloudCount = tag.getInt("cloudCount");
        int debrisCount = tag.getInt("debrisCount");
        int debrisSize = tag.getInt("debrisSize");
        int debrisRetry = tag.getInt("debrisRetry");
        float debrisVelocity = tag.getFloat("debrisVelocity");
        float debrisHorizontalDeviation = tag.getFloat("debrisHorizontalDeviation");
        float debrisVerticalOffset = tag.getFloat("debrisVerticalOffset");
        float soundRange = tag.getFloat("soundRange");

        if (player != null) {
            float dist = (float) Math.sqrt(player.distanceToSqr(x, y, z));
            if (dist <= soundRange) {
                SoundEvent sound = dist <= soundRange * 0.4D
                        ? ModSounds.EXPLOSION_LARGE_NEAR.get()
                        : ModSounds.EXPLOSION_LARGE_FAR.get();
                SimpleSoundInstance instance = new SimpleSoundInstance(
                        sound,
                        SoundSource.PLAYERS,
                        1000F,
                        level.random.nextFloat() * 0.2F + 0.9F,
                        level.random,
                        x, y, z
                );
                Minecraft.getInstance().getSoundManager().playDelayed(instance, (int) (dist / ExplosionCreator.SPEED_OF_SOUND));
            }
        }

        level.addParticle((SimpleParticleType) ModExplosionParticles.SHOCKWAVE_RING.get(), x, y + 2, z, waveScale, 0, 0);

        SimpleParticleType cloudType = (SimpleParticleType) ModExplosionParticles.WAVE_SMOKE.get();
        for (int i = 0; i < cloudCount; i++) {
            double mX = rand.nextGaussian() * 0.5D * cloudSpeedMult;
            double mY = rand.nextDouble() * 3D * cloudSpeedMult;
            double mZ = rand.nextGaussian() * 0.5D * cloudSpeedMult;
            level.addParticle(cloudType, x, y, z, mX * cloudScale, mY * cloudScale, mZ * cloudScale);
        }

        SimpleParticleType sparkType = (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get();
        for (int c = 0; c < debrisCount; c++) {
            double oX = rand.nextGaussian() * debrisHorizontalDeviation;
            double oY = debrisVerticalOffset;
            double oZ = rand.nextGaussian() * debrisHorizontalDeviation;
            level.addParticle(sparkType, x + oX, y + oY, z + oZ, 0, debrisVelocity, 0);
        }
    }
}