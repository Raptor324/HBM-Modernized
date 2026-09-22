package com.hbm_m.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 1:1-Port von {@code HbmPotion.stability} (1.7.10): reiner Markiereffekt. Er schuetzt vor
 * Digamma-Belastung ({@code ContaminationUtil.applyDigamma}, {@code ArmorUtil.checkForDigamma}).
 */
public class StabilityEffect extends HbmEffect {

    public StabilityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xD0D0D0, 2, 1);
    }
}
