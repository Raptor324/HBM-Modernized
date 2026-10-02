package com.hbm_m.particle.helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-Haelfte von {@code ExplosionSmallCreator} (1.7.10, "explosionSmall"): kleine Explosion
 * mit Knall, orangefarbenen Wolken und hochgeschleudertem Blockstaub.
 */
public class ExplosionSmallCreator {

	public static final double SPEED_OF_SOUND = (17.15D) * 0.5;

	public static void composeEffect(ServerLevel world, double x, double y, double z, int cloudCount, float cloudScale, float cloudSpeedMult) {
		CompoundTag data = new CompoundTag();
		data.putString("type", "explosionSmall");
		data.putInt("cloudCount", cloudCount);
		data.putFloat("cloudScale", cloudScale);
		data.putFloat("cloudSpeedMult", cloudSpeedMult);
		data.putInt("debris", 15);
		IParticleCreator.sendPacket(world, x, y, z, 200, data);
	}
}
