package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** 1:1 {@code ExplosionEffectTiny}: explosionTiny-Klang + eine grosse Vanilla-Explosionspartikel. */
public class ExplosionEffectTiny implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {
        if (world.isClientSide) return;
        world.playSound(null, x, y, z, HbmSoundsNT.get("hbm:weapon.explosionTiny"), SoundSource.BLOCKS, 15.0F, 1.0F);

        CompoundTag data = new CompoundTag();
        data.putString("type", "vanillaExt");
        data.putString("mode", "largeexplode");
        data.putFloat("size", 1.5F);
        data.putByte("count", (byte) 1);
        if (world instanceof ServerLevel server) IParticleCreator.sendPacket(server, x, y, z, 100, data);
    }
}
