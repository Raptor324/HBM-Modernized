package com.hbm_m.particle.helper;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ExplosionCreator implements IParticleCreator {

    public static final double SPEED_OF_SOUND = 17.15D * 0.5D;

    public static void composeEffect(ServerLevel level, double x, double y, double z, int cloudCount, float cloudScale,
                    float cloudSpeedMult, float waveScale, int debrisCount, int debrisSize, int debrisRetry, float debrisVelocity,
                    float debrisHorizontalDeviation, float debrisVerticalOffset, float soundRange) {

        CompoundTag data = new CompoundTag();
        data.putString("type", "explosionLarge");
        data.putInt("cloudCount", cloudCount);
        data.putFloat("cloudScale", cloudScale);
        data.putFloat("cloudSpeedMult", cloudSpeedMult);
        data.putFloat("waveScale", waveScale);
        data.putInt("debrisCount", debrisCount);
        data.putInt("debrisSize", debrisSize);
        data.putInt("debrisRetry", debrisRetry);
        data.putFloat("debrisVelocity", debrisVelocity);
        data.putFloat("debrisHorizontalDeviation", debrisHorizontalDeviation);
        data.putFloat("debrisVerticalOffset", debrisVerticalOffset);
        data.putFloat("soundRange", soundRange);

        IParticleCreator.sendPacket(level, x, y, z, Math.max(300, (int) soundRange), data);
    }

    public static void composeEffectSmall(ServerLevel level, double x, double y, double z) {
        composeEffect(level, x, y, z, 10, 2F, 0.5F, 25F, 5, 8, 20, 0.75F, 1F, -2F, 150);
    }

    public static void composeEffectStandard(ServerLevel level, double x, double y, double z) {
        composeEffect(level, x, y, z, 15, 5F, 1F, 45F, 10, 16, 50, 1F, 3F, -2F, 200);
    }

    public static void composeEffectLarge(ServerLevel level, double x, double y, double z) {
        composeEffect(level, x, y, z, 30, 6.5F, 2F, 65F, 25, 16, 50, 1.25F, 3F, -2F, 350);
    }

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            com.hbm_m.client.particle.ExplosionClientHandler.handleClientParticle(level, player, rand, x, y, z, tag)
        );
    }
}