package com.hbm_m.item.special;

import com.hbm_m.effect.ModEffects;
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
            applyPotionSickness(player, 5);
            HbmLivingProps.setAsbestos(player, 0);
            HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlackLung / 5));
            stack.shrink(1);
        }
    }

    /** Herbal Paste: как SiOX, плюс −100 RAD и тяжёлые побочки. */
    public static void usePillHerbal(Player player, ItemStack stack) {
        if (!player.level().isClientSide()) {
            applyPotionSickness(player, 5);
            HbmLivingProps.setAsbestos(player, 0);
            HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlackLung / 5));
            PlayerHandler.setPlayerRads(player, Math.max(0, PlayerHandler.getPlayerRads(player) - 100F));

            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 6000, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6000, 2));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));

            // Original: eigene Uebelkeit von 10 Minuten, die die kurze von oben ueberschreibt.
            player.addEffect(new MobEffectInstance(ModEffects.POTION_SICKNESS.get(), 10 * 60 * 20, 0));

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.RADAWAY_USE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.shrink(1);
        }
    }

    /**
     * Original: {@code ItemPill} bei {@code pill_iodine} - raeumt die gaengigen Vergiftungen samt
     * Strahlungseffekt ab.
     */
    public static void usePillIodine(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;

        applyPotionSickness(player, 5);

        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(ModEffects.RADIATION.get());

        stack.shrink(1);
    }

    /** Original: {@code ItemPill} bei {@code pill_red} - eine Stunde Tod. */
    public static void usePillRed(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;

        applyPotionSickness(player, 5);
        player.addEffect(new MobEffectInstance(ModEffects.DEATH.get(), 60 * 60 * 20, 0));
        stack.shrink(1);
    }

    /** Original: {@code ItemPill} bei {@code radx} - drei Minuten Strahlenschutz. */
    public static void useRadX(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;

        applyPotionSickness(player, 5);
        player.addEffect(new MobEffectInstance(ModEffects.RADX.get(), 3 * 60 * 20, 0));
        stack.shrink(1);
    }

    /** Original: {@code ItemPill} bei {@code xanax} - senkt die Digamma-Belastung um 0.5. */
    public static void useXanax(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;

        applyPotionSickness(player, 5);
        // Der Port kennt kein setDigamma; der Abzug wird auf den Bestand begrenzt.
        HbmLivingProps.incrementDigamma(player, -Math.min(0.5F, HbmLivingProps.getDigamma(player)));
        stack.shrink(1);
    }

    /**
     * Original: {@code VersatileConfig.applyPotionSickness} - jede Pille setzt eine kurze
     * Sperre, die eine sofortige zweite Einnahme wirkungslos macht.
     */
    public static void applyPotionSickness(Player player, int seconds) {
        player.addEffect(new MobEffectInstance(ModEffects.POTION_SICKNESS.get(), seconds * 20, 0));
    }

    /** Original: {@code VersatileConfig.isPotionSick}. */
    public static boolean isPotionSick(Player player) {
        return player.hasEffect(ModEffects.POTION_SICKNESS.get());
    }
}
