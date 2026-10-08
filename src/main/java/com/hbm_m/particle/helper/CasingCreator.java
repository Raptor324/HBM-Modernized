package com.hbm_m.particle.helper;

import com.hbm_m.util.Vec3NT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.particle.helper.CasingCreator} (Server-Haelfte, Typ "casingNT"). Wird wie im Original aus den
 * Orchestras serverseitig aufgerufen und schickt das Partikelpaket an alle Spieler im Umkreis von 50 Bloecken.
 * Die Client-Haelfte (Partikel erzeugen) ist {@link com.hbm_m.client.particle.CasingClientCreator}.
 * Auf einer Client-Welt wird nichts gesendet (Original: Orchestras laufen nur mit {@code !isRemote}).
 */
public class CasingCreator {

	/** Default casing without smoke */
	public static void composeEffect(Level world, LivingEntity player, double frontOffset, double heightOffset, double sideOffset, double frontMotion, double heightMotion, double sideMotion, double motionVariance, String casing) {
		composeEffect(world, player, frontOffset, heightOffset, sideOffset, frontMotion, heightMotion, sideMotion, motionVariance, 5F, 10F, casing, false, 0, 0, 0);
	}

	/** Casing without smoke */
	public static void composeEffect(Level world, LivingEntity player, double frontOffset, double heightOffset, double sideOffset, double frontMotion, double heightMotion, double sideMotion, double motionVariance, float multPitch, float multYaw, String casing) {
		composeEffect(world, player, frontOffset, heightOffset, sideOffset, frontMotion, heightMotion, sideMotion, motionVariance, multPitch, multYaw, casing, false, 0, 0, 0);
	}

	/** Default casing, but with smoke*/
	public static void composeEffect(Level world, LivingEntity player, double frontOffset, double heightOffset, double sideOffset, double frontMotion, double heightMotion, double sideMotion, double motionVariance, String casing, boolean smoking, int smokeLife, double smokeLift, int nodeLife) {
		// wie im Original: Rauch-Parameter werden hier nicht weitergereicht
		composeEffect(world, player, frontOffset, heightOffset, sideOffset, frontMotion, heightMotion, sideMotion, motionVariance, 5F, 10F, casing, false, 0, 0, 0);
	}

	public static void composeEffect(Level world, double x, double y, double z, float yaw, float pitch, double frontMotion, double heightMotion, double sideMotion, double motionVariance, float mPitch, float mYaw, String casing, boolean smoking, int smokeLife, double smokeLift, int nodeLife) {

		Vec3NT motion = Vec3NT.createVectorHelper(sideMotion, heightMotion, frontMotion);
		motion.rotateAroundX(-pitch / 180F * (float) Math.PI);
		motion.rotateAroundY(-yaw / 180F * (float) Math.PI);

		double mX = motion.xCoord + world.random.nextGaussian() * motionVariance;
		double mY = motion.yCoord + world.random.nextGaussian() * motionVariance;
		double mZ = motion.zCoord + world.random.nextGaussian() * motionVariance;

		CompoundTag data = new CompoundTag();
		data.putString("type", "casingNT");
		data.putDouble("mX", mX);
		data.putDouble("mY", mY);
		data.putDouble("mZ", mZ);
		data.putFloat("yaw", yaw);
		data.putFloat("pitch", pitch);
		data.putFloat("mPitch", mPitch);
		data.putFloat("mYaw", mYaw);
		data.putString("name", casing);
		data.putBoolean("smoking", smoking);
		data.putInt("smokeLife", smokeLife);
		data.putDouble("smokeLift", smokeLift);
		data.putInt("nodeLife", nodeLife);

		if (world instanceof ServerLevel server) IParticleCreator.sendPacket(server, x, y, z, 50, data);
	}

	public static void composeEffect(Level world, LivingEntity player, double frontOffset, double heightOffset, double sideOffset, double frontMotion, double heightMotion, double sideMotion, double motionVariance, float mPitch, float mYaw, String casing, boolean smoking, int smokeLife, double smokeLift, int nodeLife) {

		if (player.isShiftKeyDown()) heightOffset -= 0.075F;

		Vec3NT offset = Vec3NT.createVectorHelper(sideOffset, heightOffset, frontOffset);
		offset.rotateAroundX(-player.getXRot() / 180F * (float) Math.PI);
		offset.rotateAroundY(-player.getYRot() / 180F * (float) Math.PI);

		double x = player.getX() + offset.xCoord;
		double y = player.getY() + player.getEyeHeight() + offset.yCoord;
		double z = player.getZ() + offset.zCoord;

		Vec3NT motion = Vec3NT.createVectorHelper(sideMotion, heightMotion, frontMotion);
		motion.rotateAroundX(-player.getXRot() / 180F * (float) Math.PI);
		motion.rotateAroundY(-player.getYRot() / 180F * (float) Math.PI);

		double mX = player.getDeltaMovement().x + motion.xCoord + player.getRandom().nextGaussian() * motionVariance;
		double mY = player.getDeltaMovement().y + motion.yCoord + player.getRandom().nextGaussian() * motionVariance;
		double mZ = player.getDeltaMovement().z + motion.zCoord + player.getRandom().nextGaussian() * motionVariance;

		if (player instanceof Player p && p.getAbilities().flying) mY -= 0.04D;

		CompoundTag data = new CompoundTag();
		data.putString("type", "casingNT");
		data.putDouble("mX", mX);
		data.putDouble("mY", mY);
		data.putDouble("mZ", mZ);
		data.putFloat("yaw", player.getYRot());
		data.putFloat("pitch", player.getXRot());
		data.putFloat("mPitch", mPitch);
		data.putFloat("mYaw", mYaw);
		data.putString("name", casing);
		data.putBoolean("smoking", smoking);
		data.putInt("smokeLife", smokeLife);
		data.putDouble("smokeLift", smokeLift);
		data.putInt("nodeLife", nodeLife);

		if (world instanceof ServerLevel server) IParticleCreator.sendPacket(server, x, y, z, 50, data);
	}
}
