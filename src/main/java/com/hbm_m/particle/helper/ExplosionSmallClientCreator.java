package com.hbm_m.particle.helper;

import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleRotatingNT;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Client-Haelfte von {@link ExplosionSmallCreator} - 1:1 {@code ExplosionSmallCreator.makeParticle}. */
public class ExplosionSmallClientCreator implements IParticleCreator {

	@Override
	public void makeParticle(ClientLevel world, Player player, RandomSource rand, double x, double y, double z, CompoundTag data) {
		int cloudCount = data.getInt("cloudCount");
		float cloudScale = data.getFloat("cloudScale");
		float cloudSpeedMult = data.getFloat("cloudSpeedMult");
		int debris = data.getInt("debris");

		float dist = (float) Math.sqrt(player.distanceToSqr(x, y, z));
		float soundRange = 200F;
		if (dist <= soundRange) {
			// Original: ein Klangereignis mit Varianten; im Port liegen die Varianten einzeln vor.
			SoundEvent sound;
			if (dist <= soundRange * 0.4) {
				int v = rand.nextInt(3);
				sound = (v == 0 ? ModSounds.EXPLOSION_SMALL_NEAR1 : v == 1 ? ModSounds.EXPLOSION_SMALL_NEAR2 : ModSounds.EXPLOSION_SMALL_NEAR3).get();
			} else {
				sound = (rand.nextBoolean() ? ModSounds.EXPLOSION_SMALL_FAR1 : ModSounds.EXPLOSION_SMALL_FAR2).get();
			}
			SimpleSoundInstance instance = new SimpleSoundInstance(sound, net.minecraft.sounds.SoundSource.PLAYERS,
					100F, 0.9F + rand.nextFloat() * 0.2F, rand, x, y, z);
			Minecraft.getInstance().getSoundManager().playDelayed(instance, (int) (dist / ExplosionSmallCreator.SPEED_OF_SOUND));
		}

		for (int i = 0; i < cloudCount; i++) {
			ParticleEngineNT.INSTANCE.add(ParticleRotatingNT.explosionSmall(world, x, y, z, cloudScale, cloudSpeedMult));
		}

		BlockState b = null;
		for (Direction dir : Direction.values()) {
			b = world.getBlockState(new BlockPos((int) Math.floor(x) + dir.getStepX(), (int) Math.floor(y) + dir.getStepY(), (int) Math.floor(z) + dir.getStepZ()));
			if (!b.isAir()) break;
		}

		if (b != null && !b.isAir()) for (int i = 0; i < debris; i++) {
			double mx = world.random.nextGaussian() * 0.2, my = 0.5F + world.random.nextDouble() * 0.7, mz = world.random.nextGaussian() * 0.2;
			Particle fx = ParticleEffectClient.add(new BlockParticleOption(ParticleTypes.BLOCK, b), x, y + 0.1, z, mx, my, mz);
			if (fx != null) {
				fx.setParticleSpeed(mx, my, mz);
				fx.scale(2);
				fx.setLifetime(50 + rand.nextInt(20));
			}
		}
	}
}
