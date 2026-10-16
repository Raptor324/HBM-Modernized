package com.hbm_m.effect;

import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1-Port von {@code HbmPotion.radiation} (1.7.10): verstrahlt den Traeger jeden Tick um
 * {@code (Stufe + 1) * 0.05} RAD.
 */
public class RadiationEffect extends HbmEffect {

    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x84C128, 1, 0);
    }

    @Override
    protected boolean isReady(int duration, int amplifier) {
        return true;
    }

    @Override
    protected void tick(@NotNull LivingEntity entity, int amplifier) {
        ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE,
                (amplifier + 1F) * 0.05F);
    }
}
