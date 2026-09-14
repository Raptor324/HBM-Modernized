package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm_m.network.VanillaExplosionPacket;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}

import java.util.List;

public class ExplosionEffectStandard implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {
        if (level.isClientSide) {
            return;
        }
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
        if (level instanceof ServerLevel serverLevel) {
            VanillaExplosionPacket.sendNear(serverLevel, x, y, z, explosion.size, explosion.compat.getToBlow());
        }
    }

    //? if forge {
    @OnlyIn(Dist.CLIENT)
    //?}
    public static void performClient(ClientLevel level, double x, double y, double z, float size, List<BlockPos> affectedBlocks) {
        if (size >= 2.0F) {
            level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0D, 0.0D, 0.0D);
        } else {
            level.addParticle(ParticleTypes.EXPLOSION, x, y, z, 1.0D, 0.0D, 0.0D);
        }

        for (BlockPos pos : affectedBlocks) {
            int pX = pos.getX();
            int pY = pos.getY();
            int pZ = pos.getZ();

            double oX = pX + level.random.nextFloat();
            double oY = pY + level.random.nextFloat();
            double oZ = pZ + level.random.nextFloat();
            double dX = oX - x;
            double dY = oY - y;
            double dZ = oZ - z;
            double delta = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
            if (delta < 1.0E-4D) {
                continue;
            }
            dX /= delta;
            dY /= delta;
            dZ /= delta;
            double mod = 0.5D / (delta / (double) size + 0.1D);
            mod *= level.random.nextFloat() * level.random.nextFloat() + 0.3F;
            dX *= mod;
            dY *= mod;
            dZ *= mod;
            level.addParticle(ParticleTypes.POOF, (oX + x) / 2.0D, (oY + y) / 2.0D, (oZ + z) / 2.0D, dX, dY, dZ);
            level.addParticle(ParticleTypes.SMOKE, oX, oY, oZ, dX, dY, dZ);
        }
    }
}
