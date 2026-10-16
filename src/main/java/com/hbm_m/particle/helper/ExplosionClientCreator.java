package com.hbm_m.particle.helper;

import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Клиентская часть {@link ExplosionCreator}: звук near/far + волна/облако/обломки.
 * Загружается только на клиенте (через ParticleEffectClient), поэтому здесь
 * допустимы клиентские классы.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class ExplosionClientCreator implements IParticleCreator {

	@Override
	public void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag) {

		float waveScale = tag.getFloat("waveScale");
		float cloudScale = tag.getFloat("cloudScale");
		float cloudSpeedMult = tag.getFloat("cloudSpeedMult");
		int cloudCount = tag.getInt("cloudCount");
		int debrisCount = tag.getInt("debrisCount");
		float debrisVelocity = tag.getFloat("debrisVelocity");
		float debrisHorizontalDeviation = tag.getFloat("debrisHorizontalDeviation");
		float debrisVerticalOffset = tag.getFloat("debrisVerticalOffset");
		float soundRange = tag.getFloat("soundRange");

		// ══ SOUND: near/far + отложенное воспроизведение по скорости звука (1:1 с 1.7.10) ══
		if (player != null) {
			float dist = (float) Math.sqrt(player.distanceToSqr(x, y, z));
			if (dist <= soundRange) {
				SoundEvent sound = dist <= soundRange * 0.4D
						? ModSounds.EXPLOSION_LARGE_NEAR.get()
						: ModSounds.EXPLOSION_LARGE_FAR.get();
				SimpleSoundInstance instance = new SimpleSoundInstance(
						sound,
						net.minecraft.sounds.SoundSource.PLAYERS,
						1000F,                                   // громкость (как в оригинале)
						level.random.nextFloat() * 0.2F + 0.9F,  // питч: 0.9 + rand * 0.2
						level.random,
						x, y, z
				);
				Minecraft.getInstance().getSoundManager().playDelayed(instance, (int) (dist / ExplosionCreator.SPEED_OF_SOUND));
			}
		}

		// ══ WAVE (1.7.10: ParticleMukeWave, wave.setup(waveScale, 25*waveScale/45)) ══
		level.addParticle((SimpleParticleType) com.hbm_m.particle.ModExplosionParticles.SHOCKWAVE_RING.get(), x, y + 2, z, waveScale, 0, 0);

		// ══ SMOKE PLUME (облако) ══
		SimpleParticleType cloudType = (SimpleParticleType) com.hbm_m.particle.ModExplosionParticles.WAVE_SMOKE.get();
		for (int i = 0; i < cloudCount; i++) {
			double mX = rand.nextGaussian() * 0.5D * cloudSpeedMult;
			double mY = rand.nextDouble() * 3D * cloudSpeedMult;
			double mZ = rand.nextGaussian() * 0.5D * cloudSpeedMult;
			level.addParticle(cloudType, x, y, z, mX * cloudScale, mY * cloudScale, mZ * cloudScale);
		}

		// ══ DEBRIS ══
		SimpleParticleType sparkType = (SimpleParticleType) com.hbm_m.particle.ModExplosionParticles.EXPLOSION_SPARK.get();
		for (int c = 0; c < debrisCount; c++) {
			double oX = rand.nextGaussian() * debrisHorizontalDeviation;
			double oY = debrisVerticalOffset;
			double oZ = rand.nextGaussian() * debrisHorizontalDeviation;
			double angle = -Math.toRadians(45 + rand.nextFloat() * 25);
			double yaw = rand.nextDouble() * Math.PI * 2;
			double vx = debrisVelocity * Math.cos(angle) * Math.cos(yaw);
			double vy = debrisVelocity * Math.sin(angle);
			double vz = debrisVelocity * Math.cos(angle) * Math.sin(yaw);
			level.addParticle(sparkType, x + oX, y + oY, z + oZ, vx, vy, vz);
		}
	}
}
