package com.hbm_m.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 1:1-Port von {@code HbmPotion.mutation} (1.7.10): reiner Markiereffekt ohne eigenen Tick.
 * Er macht den Traeger strahlungsimmun - ausgewertet in
 * {@link com.hbm_m.util.ContaminationUtil#isRadImmune}.
 */
public class MutationEffect extends HbmEffect {

    public MutationEffect() {
        // Original: registerPotion(..., isBad = false, ...)
        super(MobEffectCategory.BENEFICIAL, 0x800080, 2, 0);
    }
}
