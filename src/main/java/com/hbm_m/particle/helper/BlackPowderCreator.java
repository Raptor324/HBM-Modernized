package com.hbm_m.particle.helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** Server-Haelfte von {@code BlackPowderCreator} (1.7.10, "blackPowder") - Muendungsdampf und Funken. */
public class BlackPowderCreator {

	public static void composeEffect(ServerLevel world, double x, double y, double z, double headingX, double headingY, double headingZ,
			int cloudCount, float cloudScale, float cloudSpeedMult, int sparkCount, float sparkSpeedMult) {
		CompoundTag data = new CompoundTag();
		data.putString("type", "blackPowder");
		data.putInt("cloudCount", cloudCount);
		data.putFloat("cloudScale", cloudScale);
		data.putFloat("cloudSpeedMult", cloudSpeedMult);
		data.putInt("sparkCount", sparkCount);
		data.putFloat("sparkSpeedMult", sparkSpeedMult);
		data.putDouble("hX", headingX);
		data.putDouble("hY", headingY);
		data.putDouble("hZ", headingZ);
		IParticleCreator.sendPacket(world, x, y, z, 200, data);
	}
}
