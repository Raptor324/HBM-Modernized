package com.hbm_m.item.special;

import com.hbm_m.effect.ModEffects;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.platform.EffectHooks;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.sound.ModSounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Gemeinsame Helfer der Verbrauchsgueter und Medikamente:
 * {@code VersatileConfig.applyPotionSickness/hasPotionSickness},
 * {@code ItemSimpleConsumable} (1.7.10) und {@link com.hbm.items.food.ItemPill}.
 */
public final class ModConsumables {

    private ModConsumables() {
    }

    /** Original {@code ItemSimpleConsumable.giveSoundAndDecrement}. */
    public static void giveSoundAndDecrement(ItemStack stack, LivingEntity entity, SoundEvent sound, ItemStack container) {
        stack.shrink(1);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        tryAddItem(entity, container);
    }

    /** Original {@code ItemSimpleConsumable.addPotionEffect}: laufende Dauer wird verlaengert. */
    public static void addPotionEffect(LivingEntity entity, MobEffect effect, int duration, int level) {
        //? if < 1.21.1 {
        MobEffectInstance active = entity.getEffect(effect);
        //?} else {
        /*MobEffectInstance active = entity.getEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
        *///?}
        if (active == null) {
            //? if < 1.21.1 {
            entity.addEffect(new MobEffectInstance(effect, duration, level));
            //?} else {
            /*entity.addEffect(new MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), duration, level));
            *///?}
        } else {
            int d = active.getDuration() + duration;
            //? if < 1.21.1 {
            entity.addEffect(new MobEffectInstance(effect, d, level));
            //?} else {
            /*entity.addEffect(new MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), d, level));
            *///?}
        }
    }

    /** Original {@code ItemSimpleConsumable.tryAddItem}. */
    public static void tryAddItem(LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player player) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
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
            EffectHooks.addEffect(player, ModEffects.POTION_SICKNESS, 10 * 60 * 20, 0);

            ModSounds.RADAWAY_USE.ifPresent(sound ->
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            sound, SoundSource.PLAYERS, 1.0F, 1.0F));
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
        EffectHooks.removeEffect(player, ModEffects.RADIATION);

        stack.shrink(1);
    }

    /** Original: {@code ItemPill} bei {@code pill_red} - eine Stunde Tod. */
    public static void usePillRed(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;

        applyPotionSickness(player, 5);
        EffectHooks.addEffect(player, ModEffects.DEATH, 60 * 60 * 20, 0);
        stack.shrink(1);
    }

    /** Original: {@code ItemPill} bei {@code radx} - drei Minuten Strahlenschutz. */
    public static void useRadX(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;

        applyPotionSickness(player, 5);
        EffectHooks.addEffect(player, ModEffects.RADX, 3 * 60 * 20, 0);
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
     * Original: {@code VersatileConfig.applyPotionSickness} - nur wenn {@code potionSickness} an ist
     * (Voreinstellung im Original: aus), TERRARIA verzwoelffacht die Dauer. Milch heilt es nicht.
     */
    public static void applyPotionSickness(LivingEntity entity, int duration) {
        int mode = com.hbm_m.config.ModClothConfig.get().potionSickness;
        if (mode == 0) return;
        if (mode == 2) duration *= 12;

        MobEffectInstance eff = new MobEffectInstance(EffectHooks.of(ModEffects.POTION_SICKNESS), duration * 20);
        //? if forge {
        eff.setCurativeItems(new java.util.ArrayList<>());
        //?} elif neoforge {
        /*eff.getCures().clear();
        *///?}
        entity.addEffect(eff);
    }

    /** Original: {@code VersatileConfig.hasPotionSickness}. */
    public static boolean isPotionSick(LivingEntity entity) {
        return entity.hasEffect(EffectHooks.of(ModEffects.POTION_SICKNESS));
    }
}
