package com.hbm_m.particle.helper;

import com.hbm_m.particle.nt.NukeExplosionRings;
import com.hbm_m.particle.nt.NukeTorex;
import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.util.BobMathUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class NukeTorexCreator implements IParticleCreator {

    public static void statFacStandard(Level level, double x, double y, double z, float scale) {
        statFac(level, x, y, z, scale, 0);
    }

    public static void statFacBale(Level level, double x, double y, double z, float scale) {
        statFac(level, x, y, z, scale, 1);
    }

    private static void statFac(Level level, double x, double y, double z, float scale, int type) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "nuke");
        tag.putFloat("scale", scale);
        if (type == 0 || type == 1) tag.putInt("cType", type);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            IParticleCreator.sendPacket(serverLevel, x, y, z, 1000, tag);
        }
    }

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag) {
        int type = tag.contains("cType") ? tag.getInt("cType") : 0;
        float scale = tag.getFloat("scale");
        float clampedScale = Mth.clamp((float) BobMathUtil.squirt(scale * 0.01) * 1.5F, 0.5F, 5F);
        NukeTorex torex = new NukeTorex(level, x, y, z).setScale(clampedScale, true);
        torex.setType(type);
        ParticleEngineNT.INSTANCE.add(torex);

        NukeExplosionRings rings = new NukeExplosionRings(level, x, y, z).setScale(clampedScale);
        ParticleEngineNT.INSTANCE.add(rings);
    }
}
