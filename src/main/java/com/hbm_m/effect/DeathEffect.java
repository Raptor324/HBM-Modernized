package com.hbm_m.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * 1:1-Port von {@code HbmPotion.death} (1.7.10): reiner Markiereffekt, der ueber den Tod hinaus
 * bestehen bleibt ({@code PermaSyncHandler}).
 *
 * <p>Das Original registriert ihn mit {@code isBad = false} - trotz des Namens gilt er als
 * nuetzlicher Effekt. Das ist hier uebernommen.</p>
 */
public class DeathEffect extends HbmEffect {

    public DeathEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x111111, 4, 1);
    }
}
