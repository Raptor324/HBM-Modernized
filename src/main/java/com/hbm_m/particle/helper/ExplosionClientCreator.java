package com.hbm_m.particle.helper;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.hbm_m.particle.nt.MukeWaveParticle;
import com.hbm_m.particle.nt.ParticleDebrisNT;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleRocketFlameNT;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Клиентская часть {@link ExplosionCreator}: звук near/far + волна/облако/обломки.
 * Загружается только на клиенте (через ParticleEffectClient), поэтому здесь
 * допустимы клиентские классы.
 */
@OnlyIn(Dist.CLIENT)
public class ExplosionClientCreator implements IParticleCreator {

	@Override
	public void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag) {

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

		// ══ WAVE (1.7.10: ParticleMukeWave, wave.setup(waveScale, 25*waveScale/45)) — FFA merge loss restore ══
		ParticleEngineNT.INSTANCE.add(new MukeWaveParticle(level, x, y + 2, z).setup(waveScale, (int) (25F * waveScale / 45)));

		// ══ SMOKE PLUME (облако: ParticleRocketFlameNT — FFA merge loss restore) ══
		for (int i = 0; i < cloudCount; i++) {
			ParticleRocketFlameNT fx = new ParticleRocketFlameNT(level, x, y, z).setScale(cloudScale);
			fx.xd = rand.nextGaussian() * 0.5 * cloudSpeedMult;
			fx.yd = rand.nextDouble() * 3 * cloudSpeedMult;
			fx.zd = rand.nextGaussian() * 0.5 * cloudSpeedMult;
			fx.setMaxAge(70 + rand.nextInt(20));
			fx.noClip = true;
			ParticleEngineNT.INSTANCE.add(fx);
		}

		// ══ DEBRIS (ParticleDebrisNT с реальными блоками вокруг эпицентра — FFA merge loss restore;
		// "WorldInAJar"-сэмплирование: центральный куб 2x2x2 + случайные осколки по слоям) ══
		for (int c = 0; c < debrisCount; c++) {
			double oX = rand.nextGaussian() * debrisHorizontalDeviation;
			double oY = debrisVerticalOffset;
			double oZ = rand.nextGaussian() * debrisHorizontalDeviation;
			int cX = (int) Math.floor(x + oX + 0.5);
			int cY = (int) Math.floor(y + oY + 0.5);
			int cZ = (int) Math.floor(z + oZ + 0.5);

			// Оригинал: Vec3(velocity,0,0).zRot(-45..-70°).yRot(0..2π) — дуга ВВЕРХ.
			Vec3 motion = new Vec3(debrisVelocity, 0, 0)
					.zRot((float) -Math.toRadians(45 + rand.nextFloat() * 25))
					.yRot((float) (rand.nextDouble() * Math.PI * 2));
			ParticleDebrisNT particle = new ParticleDebrisNT(level, x, y, z, motion.x, motion.y, motion.z, debrisSize);

			if (debrisSize > 0) {
				int middle = debrisSize / 2 - 1;
				for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) for (int k = 0; k < 2; k++)
					particle.setBlock(middle + i, middle + j, middle + k, level.getBlockState(new net.minecraft.core.BlockPos(cX + i, cY + j, cZ + k)));

				for (int layer = 2; layer <= (debrisSize / 2); layer++) {
					for (int i = 0; i < debrisRetry; i++) {
						int jx = -layer + rand.nextInt(layer * 2 + 1);
						int jy = -layer + rand.nextInt(layer * 2 + 1);
						int jz = -layer + rand.nextInt(layer * 2 + 1);
						if (!particle.isAir(middle + jx + 1, middle + jy, middle + jz) || !particle.isAir(middle + jx - 1, middle + jy, middle + jz)
								|| !particle.isAir(middle + jx, middle + jy + 1, middle + jz) || !particle.isAir(middle + jx, middle + jy - 1, middle + jz)
								|| !particle.isAir(middle + jx, middle + jy, middle + jz + 1) || !particle.isAir(middle + jx, middle + jy, middle + jz - 1)) {
							particle.setBlock(middle + jx, middle + jy, middle + jz, level.getBlockState(new net.minecraft.core.BlockPos(cX + jx, cY + jy, cZ + jz)));
						}
					}
				}
			}
			ParticleEngineNT.INSTANCE.add(particle);
		}
	}
}
