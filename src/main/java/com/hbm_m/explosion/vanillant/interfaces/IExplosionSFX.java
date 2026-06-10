package com.hbm_m.explosion.vanillant.interfaces;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.level.Level;

public interface IExplosionSFX {
    void doEffect(ExplosionVNT explosion, Level level, double x, double y, double z, float size);
}
