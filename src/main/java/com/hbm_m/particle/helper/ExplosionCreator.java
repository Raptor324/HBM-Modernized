package com.hbm_m.particle.helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

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
 *
 * ВАЖНО: этот класс загружается на выделенном сервере — здесь не должно быть
 * клиентских импортов (Minecraft/SimpleSoundInstance и т.п.). Клиентская часть
 * вынесена в {@link ExplosionClientCreator}.
 */
public class ExplosionCreator {

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
}
