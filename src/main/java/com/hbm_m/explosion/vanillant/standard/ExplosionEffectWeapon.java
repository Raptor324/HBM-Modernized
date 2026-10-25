package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm_m.particle.helper.ExplosionSmallCreator;

import net.minecraft.world.level.Level;

/** 1:1 {@code ExplosionEffectWeapon}: kleine Waffenexplosion mit Rauchwolken. */
public class ExplosionEffectWeapon implements IExplosionSFX {

    int cloudCount;
    float cloudScale;
    float cloudSpeedMult;

    public ExplosionEffectWeapon(int cloudCount, float cloudScale, float cloudSpeedMult) {
        this.cloudCount = cloudCount;
        this.cloudScale = cloudScale;
        this.cloudSpeedMult = cloudSpeedMult;
    }

    @Override
    public void doEffect(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {
        if (!(world instanceof net.minecraft.server.level.ServerLevel server)) return;
        ExplosionSmallCreator.composeEffect(server, x, y, z, cloudCount, cloudScale, cloudSpeedMult);
    }
}
