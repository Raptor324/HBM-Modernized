package com.hbm_m.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1-Port von {@code HbmPotion.phosphorus} (1.7.10): haelt den Traeger dauerhaft in Brand.
 * Das Original ruft jeden Tick {@code entity.setFire(1)}.
 */
public class PhosphorusEffect extends HbmEffect {

    public PhosphorusEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFF00, 1, 1);
    }

    @Override
    protected boolean isReady(int duration, int amplifier) {
        return true;
    }

    @Override
    protected void tick(@NotNull LivingEntity entity, int amplifier) {
        entity.setSecondsOnFire(1);
    }
}
