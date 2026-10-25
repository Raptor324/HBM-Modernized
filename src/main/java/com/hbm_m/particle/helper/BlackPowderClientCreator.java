package com.hbm_m.particle.helper;

import com.hbm_m.particle.nt.ParticleBlackPowderSparkNT;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleRotatingNT;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Client-Haelfte von {@link BlackPowderCreator}. */
public class BlackPowderClientCreator implements IParticleCreator {

	@Override
	public void makeParticle(ClientLevel world, Player player, RandomSource rand, double x, double y, double z, CompoundTag data) {
		Vec3 heading = new Vec3(data.getDouble("hX"), data.getDouble("hY"), data.getDouble("hZ")).normalize();
		int cloudCount = data.getInt("cloudCount");
		float cloudScale = data.getFloat("cloudScale");
		float cloudSpeedMult = data.getFloat("cloudSpeedMult");
		int sparkCount = data.getInt("sparkCount");
		float sparkSpeedMult = data.getFloat("sparkSpeedMult");

		for (int i = 0; i < cloudCount; i++) {
			ParticleRotatingNT particle = ParticleRotatingNT.blackPowderSmoke(world, x, y, z, cloudScale);
			double speedMult = 0.85 + rand.nextDouble() * 0.3;
			particle.xd = heading.x * cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05;
			particle.yd = heading.y * cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05;
			particle.zd = heading.z * cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05;
			ParticleEngineNT.INSTANCE.add(particle);
		}

		for (int i = 0; i < sparkCount; i++) {
			double speedMult = 0.85 + rand.nextDouble() * 0.3;
			ParticleEngineNT.INSTANCE.add(new ParticleBlackPowderSparkNT(world, x, y, z,
					heading.x * sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02,
					heading.y * sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02,
					heading.z * sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02));
		}
	}
}
