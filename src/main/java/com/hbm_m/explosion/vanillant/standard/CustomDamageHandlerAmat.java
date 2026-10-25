package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 1:1 {@code CustomDamageHandlerAmat}: Strahlung je nach Abstand und Groesse (Kreativ-Kontamination). */
public class CustomDamageHandlerAmat implements ICustomDamageHandler {

    protected float radiation;

    public CustomDamageHandlerAmat(float radiation) {
        this.radiation = radiation;
    }

    @Override
    public void handleAttack(ExplosionVNT explosion, Entity entity, double distanceScaled) {
        if (entity instanceof LivingEntity living)
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, (float) (radiation * (1D - distanceScaled) * explosion.size));
    }
}
