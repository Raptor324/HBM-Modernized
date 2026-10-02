package com.hbm_m.util;

import com.hbm_m.particle.helper.IParticleCreator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** 1:1-Port von {@code com.hbm.util.ParticleUtil}: Gasflamme und Drohnen-Debuglinie. */
public final class ParticleUtil {

    private ParticleUtil() {}

    public static void spawnGasFlame(Level world, double x, double y, double z, double mX, double mY, double mZ) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "gasfire");
        data.putDouble("mX", mX);
        data.putDouble("mY", mY);
        data.putDouble("mZ", mZ);
        send(world, x, y, z, data);
    }

    public static void spawnDroneLine(Level world, double x, double y, double z, double x0, double y0, double z0, int color) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "debugdrone");
        data.putDouble("mX", x0);
        data.putDouble("mY", y0);
        data.putDouble("mZ", z0);
        data.putInt("color", color);
        send(world, x, y, z, data);
    }

    private static void send(Level world, double x, double y, double z, CompoundTag data) {
        if (world.isClientSide) {
            data.putDouble("posX", x);
            data.putDouble("posY", y);
            data.putDouble("posZ", z);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNTNow(data);
        } else if (world instanceof ServerLevel sl) {
            IParticleCreator.sendPacket(sl, x, y, z, 150, data);
        }
    }
}
