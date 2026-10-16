package com.hbm_m.item.special;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.sound.ModSounds;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Лекарства. Порт поведения {@link com.hbm.items.food.ItemPill} (1.7.10):
 * siox и pill_herbal лечат асбестоз и угольную болезнь.
 */
public final class ModConsumables {

    private ModConsumables() {
    }

    /** SiOX: асбестоз полностью, угольная болезнь до 20% от максимума. */
    public static void useSiox(Player player, ItemStack stack) {
        if (!player.level().isClientSide()) {
            HbmLivingProps.setAsbestos(player, 0);
            HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlackLung / 5));
            stack.shrink(1);
        }
    }

    /** Herbal Paste: как SiOX, плюс −100 RAD и тяжёлые побочки. */
    public static void usePillHerbal(Player player, ItemStack stack) {
        if (!player.level().isClientSide()) {
            HbmLivingProps.setAsbestos(player, 0);
            HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlackLung / 5));
            PlayerHandler.setPlayerRads(player, Math.max(0, PlayerHandler.getPlayerRads(player) - 100F));

            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 6000, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6000, 2));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.RADAWAY_USE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.shrink(1);
        }
    }
}
