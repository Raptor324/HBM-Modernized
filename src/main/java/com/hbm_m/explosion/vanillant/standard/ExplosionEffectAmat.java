package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** 1:1 {@code ExplosionEffectAmat}: Knall (klein: random.explode, gross: Muke) + "amat"-Blitz. */
public class ExplosionEffectAmat implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {
        if (size < 15)
            world.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.4F + (world.random.nextFloat() - world.random.nextFloat()) * 0.2F) * 0.7F);
        else
            world.playSound(null, x, y, z, HbmSoundsNT.get("hbm:weapon.mukeExplosion"), SoundSource.BLOCKS, 15.0F, 1.0F);

        if (world instanceof ServerLevel server) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "amat");
            data.putFloat("scale", size);
            IParticleCreator.sendPacket(server, x, y, z, 200, data);
        }
    }
}
