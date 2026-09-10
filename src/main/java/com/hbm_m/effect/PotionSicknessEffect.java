package com.hbm_m.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 1:1-Port von {@code HbmPotion.potionsickness} (1.7.10): reiner Markiereffekt. Solange er
 * anliegt, wirkt eine erneute Einnahme derselben Pille nicht ({@code VersatileConfig}).
 */
public class PotionSicknessEffect extends HbmEffect {

    public PotionSicknessEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xff8080, 3, 1);
    }
}
