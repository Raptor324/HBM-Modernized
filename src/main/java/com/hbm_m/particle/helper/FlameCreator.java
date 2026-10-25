package com.hbm_m.particle.helper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** Server-Haelfte von {@code FlameCreator} (1.7.10, "flamethrower"). */
public class FlameCreator {

	public static final int META_FIRE = 0;
	public static final int META_BALEFIRE = 1;
	public static final int META_DIGAMMA = 2;
	public static final int META_OXY = 3;
	public static final int META_BLACK = 4;

	public static void composeEffect(ServerLevel world, double x, double y, double z, int meta) {
		CompoundTag data = new CompoundTag();
		data.putString("type", "flamethrower");
		data.putInt("meta", meta);
		IParticleCreator.sendPacket(world, x, y, z, 50, data);
	}

	/** Nur auf dem Client aufrufen. */
	public static void composeEffectClient(double x, double y, double z, int meta) {
		CompoundTag data = new CompoundTag();
		data.putString("type", "flamethrower");
		data.putInt("meta", meta);
		data.putDouble("posX", x);
		data.putDouble("posY", y);
		data.putDouble("posZ", z);
		ParticleEffectClient.effectNTNow(data);
	}
}
