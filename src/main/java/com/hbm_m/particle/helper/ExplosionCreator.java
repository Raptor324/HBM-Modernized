package com.hbm_m.particle.helper;

import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Порт {@code com.hbm.particle.helper.ExplosionCreator} (1.7.10).
 *
 * Ключевая механика звука 1:1:
 *  - скорость звука: {@code 17.15 * 0.5 = 8.575} блоков/тик;
 *  - клиент выбирает near/far вариант по {@code dist <= soundRange * 0.4};
 *  - звук воспроизводится через delayed-очередь с задержкой {@code dist / speedOfSound} тиков;
 *  - громкость 1000F (звук «глобальный», слышен по всему soundRange), питч 0.9 + rand * 0.2.
 *
 * Пресеты (cloudCount, cloudScale, cloudSpeedMult, waveScale, debrisCount, debrisSize,
 * debrisRetry, debrisVelocity, debrisHorizontalDeviation, debrisVerticalOffset, soundRange):
 *  - small:    ( 10, 2.0F, 0.5F, 25F,  5, 8, 20, 0.75F, 1F, -2F, 150)
 *  - standard: ( 15, 5.0F, 1.0F, 45F, 10, 16, 50, 1.00F, 3F, -2F, 200)
 *  - large:    ( 30, 6.5F, 2.0F, 65F, 25, 16, 50, 1.25F, 3F, -2F, 350)
 */
public class ExplosionCreator implements IParticleCreator {

	/** Скорость распространения звука, блоков/тик (1.7.10: 17.15D * 0.5). */
	public static final double SPEED_OF_SOUND = 17.15D * 0.5D;

	public static void composeEffect(ServerLevel level, double x, double y, double z, int cloudCount, float cloudScale,
			float cloudSpeedMult, float waveScale, int debrisCount, int debrisSize, int debrisRetry, float debrisVelocity,
			float debrisHorizontalDeviation, float debrisVerticalOffset, float soundRange) {

		CompoundTag data = new CompoundTag();
		data.putString("type", "explosionLarge");
		data.putInt("cloudCount", cloudCount);
		data.putFloat("cloudScale", cloudScale);
		data.putFloat("cloudSpeedMult", cloudSpeedMult);
		data.putFloat("waveScale", waveScale);
		data.putInt("debrisCount", debrisCount);
		data.putInt("debrisSize", debrisSize);
		data.putInt("debrisRetry", debrisRetry);
		data.putFloat("debrisVelocity", debrisVelocity);
		data.putFloat("debrisHorizontalDeviation", debrisHorizontalDeviation);
		data.putFloat("debrisVerticalOffset", debrisVerticalOffset);
		data.putFloat("soundRange", soundRange);

		IParticleCreator.sendPacket(level, x, y, z, Math.max(300, (int) soundRange), data);
	}

	/** tier 1 generic / incendiary */
	public static void composeEffectSmall(ServerLevel level, double x, double y, double z) {
		composeEffect(level, x, y, z, 10, 2F, 0.5F, 25F, 5, 8, 20, 0.75F, 1F, -2F, 150);
	}

	/** tier 2 strong / stealth */
	public static void composeEffectStandard(ServerLevel level, double x, double y, double z) {
		composeEffect(level, x, y, z, 15, 5F, 1F, 45F, 10, 16, 50, 1F, 3F, -2F, 200);
	}

	/** tier 3 burst / inferno */
	public static void composeEffectLarge(ServerLevel level, double x, double y, double z) {
		composeEffect(level, x, y, z, 30, 6.5F, 2F, 65F, 25, 16, 50, 1.25F, 3F, -2F, 350);
	}

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
				Minecraft.getInstance().getSoundManager().playDelayed(instance, (int) (dist / SPEED_OF_SOUND));
			}
		}

		// ══ WAVE (1.7.10: ParticleMukeWave, wave.setup(waveScale, 25*waveScale/45)) ══
		level.addParticle((SimpleParticleType) ModExplosionParticles.SHOCKWAVE_RING.get(), x, y + 2, z, waveScale, 0, 0);

		// ══ SMOKE PLUME (облако) ══
		SimpleParticleType cloudType = (SimpleParticleType) ModExplosionParticles.WAVE_SMOKE.get();
		for (int i = 0; i < cloudCount; i++) {
			double mX = rand.nextGaussian() * 0.5D * cloudSpeedMult;
			double mY = rand.nextDouble() * 3D * cloudSpeedMult;
			double mZ = rand.nextGaussian() * 0.5D * cloudSpeedMult;
			level.addParticle(cloudType, x, y, z, mX * cloudScale, mY * cloudScale, mZ * cloudScale);
		}

		// ══ DEBRIS ══
		SimpleParticleType sparkType = (SimpleParticleType) ModExplosionParticles.EXPLOSION_SPARK.get();
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
