package com.hbm_m.client.particle;

import com.hbm_m.particle.SpentCasing;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.particle.helper.ParticleCreators;
import com.hbm_m.particle.nt.ParticleEngineNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Client-Haelfte von {@link com.hbm_m.particle.helper.CasingCreator} (Original {@code CasingCreator.makeParticle},
 * Typ "casingNT"). Registrierung ueber {@link #register()} im Client-Setup.
 */
public class CasingClientCreator implements IParticleCreator {

	public static void register() {
		ParticleCreators.particleCreators().put("casingNT", new CasingClientCreator());
	}

	@Override
	public void makeParticle(ClientLevel world, Player player, RandomSource rand, double x, double y, double z, CompoundTag data) {

		String name = data.getString("name");
		SpentCasing casingConfig = SpentCasing.casingMap.get(name);
		if (casingConfig == null) return;
		double mX = data.getDouble("mX");
		double mY = data.getDouble("mY");
		double mZ = data.getDouble("mZ");
		float yaw = data.getFloat("yaw");
		float pitch = data.getFloat("pitch");
		float mPitch = data.getFloat("mPitch");
		float mYaw = data.getFloat("mYaw");
		boolean smoking = data.getBoolean("smoking");
		int smokeLife = data.getInt("smokeLife");
		double smokeLift = data.getDouble("smokeLift");
		int nodeLife = data.getInt("nodeLife");
		spawn(world, x, y, z, mX, mY, mZ, yaw, pitch, mPitch, mYaw, casingConfig, smoking, smokeLife, smokeLift, nodeLife);
	}

	/** Direkter clientseitiger Einstieg (ohne Paket), z.B. fuer rein clientseitige Effekte. */
	public static void spawn(ClientLevel world, double x, double y, double z, double mX, double mY, double mZ, float yaw, float pitch,
			float mPitch, float mYaw, SpentCasing casingConfig, boolean smoking, int smokeLife, double smokeLift, int nodeLife) {
		if (world == null) world = Minecraft.getInstance().level;
		if (world == null || casingConfig == null) return;
		ParticleSpentCasing casing = new ParticleSpentCasing(world, x, y, z, mX, mY, mZ, mPitch, mYaw, casingConfig, smoking, smokeLife, smokeLift, nodeLife);
		casing.prevRotationYaw = casing.rotationYaw = yaw;
		casing.prevRotationPitch = casing.rotationPitch = pitch;
		ParticleEngineNT.INSTANCE.add(casing);
	}
}
