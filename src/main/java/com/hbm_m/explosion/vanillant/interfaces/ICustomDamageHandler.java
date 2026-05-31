package com.hbm_m.explosion.vanillant.interfaces;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.entity.Entity;

public interface ICustomDamageHandler {
    void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled);
}