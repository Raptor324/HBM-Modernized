package com.hbm_m.effect;

import com.hbm_m.damagesource.ModDamageSources;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1-Port von {@code HbmPotion.lead} (1.7.10): Bleivergiftung. Alle drei Sekunden Schaden in
 * Hoehe der Stufe + 1.
 */
public class LeadEffect extends HbmEffect {

    /** Original: {@code int k = 60; return par1 % k == 0;} */
    private static final int INTERVAL = 60;

    public LeadEffect() {
        super(MobEffectCategory.HARMFUL, 0x767682, 6, 0);
    }

    @Override
    protected boolean isReady(int duration, int amplifier) {
        return duration % INTERVAL == 0;
    }

    @Override
    protected void tick(@NotNull LivingEntity entity, int amplifier) {
        entity.hurt(ModDamageSources.lead(entity.level()), amplifier + 1);
    }
}
