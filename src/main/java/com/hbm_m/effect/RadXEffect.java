package com.hbm_m.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 1:1-Port von {@code HbmPotion.radx} (1.7.10): reiner Markiereffekt. Er hebt den
 * Strahlenschutz um 0.2 - ausgewertet in {@link com.hbm_m.handler.HazmatRegistry#getResistance}.
 */
public class RadXEffect extends HbmEffect {

    /** Original: {@code if(player.isPotionActive(HbmPotion.radx)) res += 0.2F;} */
    public static final float RESISTANCE_BONUS = 0.2F;

    public RadXEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xBB4B00, 5, 0);
    }
}
