package com.hbm_m.item.special;

import com.hbm_m.platform.EffectHooks;

import com.hbm_m.effect.ModEffects;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Gemeinsame Helfer der Verbrauchsgueter: {@code VersatileConfig.applyPotionSickness/hasPotionSickness}
 * und die statischen Helfer aus {@code ItemSimpleConsumable} (1.7.10).
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
