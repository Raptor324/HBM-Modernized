package com.hbm_m.particle.helper;

import com.hbm_m.particle.nt.MukeWaveParticle;
import com.hbm_m.particle.nt.ParticleDebrisNT;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleRocketFlameNT;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Client-Haelfte von {@link ExplosionCreator} ("explosionLarge") - 1:1-Port von
 * {@code ExplosionCreator.makeParticle} (1.7.10): verzoegerter Knall nach Schallgeschwindigkeit,
 * Stosswellenring, aufsteigende Feuerwolken ({@code ParticleRocketFlame}) und fliegende Brocken aus
 * den echten Bloecken rund um den Einschlag ({@code ParticleDebris} + {@code WorldInAJar}).
 */
public class ExplosionClientCreator implements IParticleCreator {

	@Override
	public void makeParticle(ClientLevel world, Player player, RandomSource rand, double x, double y, double z, CompoundTag data) {

		int cloudCount = data.getInt("cloudCount");
		float cloudScale = data.getFloat("cloudScale");
		float cloudSpeedMult = data.getFloat("cloudSpeedMult");
		float waveScale = data.getFloat("waveScale");
		int debrisCount = data.getInt("debrisCount");
		int debrisSize = data.getInt("debrisSize");
		int debrisRetry = data.getInt("debrisRetry");
		float debrisVelocity = data.getFloat("debrisVelocity");
		float debrisHorizontalDeviation = data.getFloat("debrisHorizontalDeviation");
		float debrisVerticalOffset = data.getFloat("debrisVerticalOffset");
		float soundRange = data.getFloat("soundRange");

		float dist = (float) Math.sqrt(player.distanceToSqr(x, y, z));
		if (dist <= soundRange) {
			SoundEvent sound = dist <= soundRange * 0.4D ? ModSounds.EXPLOSION_LARGE_NEAR.get() : ModSounds.EXPLOSION_LARGE_FAR.get();
			SimpleSoundInstance instance = new SimpleSoundInstance(sound, net.minecraft.sounds.SoundSource.PLAYERS,
					1000F, 0.9F + rand.nextFloat() * 0.2F, rand, x, y, z);
			Minecraft.getInstance().getSoundManager().playDelayed(instance, (int) (dist / ExplosionCreator.SPEED_OF_SOUND));
		}

		ParticleEngineNT.INSTANCE.add(new MukeWaveParticle(world, x, y + 2, z).setup(waveScale, (int) (25F * waveScale / 45)));

		for (int i = 0; i < cloudCount; i++) {
			ParticleRocketFlameNT fx = new ParticleRocketFlameNT(world, x, y, z).setScale(cloudScale);
			fx.xd = rand.nextGaussian() * 0.5 * cloudSpeedMult;
			fx.yd = rand.nextDouble() * 3 * cloudSpeedMult;
			fx.zd = rand.nextGaussian() * 0.5 * cloudSpeedMult;
			fx.setMaxAge(70 + rand.nextInt(20));
			fx.noClip = true;
			ParticleEngineNT.INSTANCE.add(fx);
		}

		for (int c = 0; c < debrisCount; c++) {
			double oX = rand.nextGaussian() * debrisHorizontalDeviation;
			double oY = debrisVerticalOffset;
			double oZ = rand.nextGaussian() * debrisHorizontalDeviation;
			int cX = (int) Math.floor(x + oX + 0.5);
			int cY = (int) Math.floor(y + oY + 0.5);
			int cZ = (int) Math.floor(z + oZ + 0.5);

			Vec3 motion = new Vec3(debrisVelocity, 0, 0)
					.zRot((float) -Math.toRadians(45 + rand.nextFloat() * 25))
					.yRot((float) (rand.nextDouble() * Math.PI * 2));
			ParticleDebrisNT particle = new ParticleDebrisNT(world, x, y, z, motion.x, motion.y, motion.z, debrisSize);

			if (debrisSize > 0) {
				int middle = debrisSize / 2 - 1;
				for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) for (int k = 0; k < 2; k++)
					particle.setBlock(middle + i, middle + j, middle + k, world.getBlockState(new BlockPos(cX + i, cY + j, cZ + k)));

				for (int layer = 2; layer <= (debrisSize / 2); layer++) {
					for (int i = 0; i < debrisRetry; i++) {
						int jx = -layer + rand.nextInt(layer * 2 + 1);
						int jy = -layer + rand.nextInt(layer * 2 + 1);
						int jz = -layer + rand.nextInt(layer * 2 + 1);
						if (!particle.isAir(middle + jx + 1, middle + jy, middle + jz) || !particle.isAir(middle + jx - 1, middle + jy, middle + jz)
								|| !particle.isAir(middle + jx, middle + jy + 1, middle + jz) || !particle.isAir(middle + jx, middle + jy - 1, middle + jz)
								|| !particle.isAir(middle + jx, middle + jy, middle + jz + 1) || !particle.isAir(middle + jx, middle + jy, middle + jz - 1)) {
							particle.setBlock(middle + jx, middle + jy, middle + jz, world.getBlockState(new BlockPos(cX + jx, cY + jy, cZ + jz)));
						}
					}
				}
			}
			ParticleEngineNT.INSTANCE.add(particle);
		}
	}
}
