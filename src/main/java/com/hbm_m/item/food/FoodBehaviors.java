package com.hbm_m.item.food;

import com.hbm_m.effect.ModEffects;
import com.hbm_m.explosion.ExplosionNukeSmall;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.special.ModConsumables;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Die {@code onFoodEaten}-Zweige der Original-Nahrungsklassen ({@code ItemLemon},
 * {@code ItemCottonCandy}, {@code ItemAppleSchrabidium}, {@code ItemAppleEuphemium},
 * {@code ItemTemFlakes}, {@code ItemSchnitzelVegan}, {@code ItemWaffle}, {@code ItemPancake},
 * {@code ItemMuchoMango}, {@code ItemPill}) als wiederverwendbare Methoden.
 */
public final class FoodBehaviors {

    private FoodBehaviors() {}

    private static void eff(Player p, MobEffect e, int ticks, int amp) {
        p.addEffect(new MobEffectInstance(e, ticks, amp));
    }

    // ------------------------------------------------------------------ ItemLemon

    /** {@code med_ipecac}/{@code med_ptsd}: Hunger 50 Stufe 49, Erbrechen samt Klang. */
    public static void ipecac(ItemStack stack, Level world, Player player) {
        eff(player, MobEffects.HUNGER, 50, 49);
        if (world instanceof ServerLevel sl) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("type", "vomit");
            nbt.putInt("entity", player.getId());
            IParticleCreator.sendPacket(sl, player.getX(), player.getY(), player.getZ(), 25, nbt);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:player.vomit"), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void loopStew(ItemStack stack, Level world, Player player) {
        eff(player, MobEffects.REGENERATION, 20 * 20, 1);
        eff(player, MobEffects.DAMAGE_RESISTANCE, 60 * 20, 2);
        eff(player, MobEffects.MOVEMENT_SPEED, 60 * 20, 1);
        eff(player, MobEffects.DAMAGE_BOOST, 20 * 20, 2);
    }

    // ------------------------------------------------------------------ ItemCottonCandy

    public static void cottonCandy(ItemStack stack, Level world, Player player) {
        if (world.isClientSide) return;
        eff(player, MobEffects.POISON, 15 * 20, 0);
        eff(player, MobEffects.WITHER, 5 * 20, 0);
        eff(player, MobEffects.WEAKNESS, 25 * 20, 2);
        eff(player, MobEffects.MOVEMENT_SPEED, 25 * 20, 2);
        eff(player, MobEffects.DAMAGE_RESISTANCE, 30 * 20, 4);
    }

    // ------------------------------------------------------------------ ItemAppleSchrabidium

    public static void appleSchrabidium(int meta, ItemStack stack, Level world, Player player) {
        if (world.isClientSide) return;
        int max = Integer.MAX_VALUE;
        switch (meta) {
            case 0 -> {
                eff(player, MobEffects.REGENERATION, 600, 4);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 6000, 0);
                eff(player, MobEffects.FIRE_RESISTANCE, 6000, 0);
            }
            case 1 -> {
                eff(player, MobEffects.REGENERATION, 1200, 4);
                eff(player, MobEffects.DAMAGE_RESISTANCE, 1200, 4);
                eff(player, MobEffects.FIRE_RESISTANCE, 1200, 0);
                eff(player, MobEffects.DAMAGE_BOOST, 1200, 4);
                eff(player, MobEffects.DIG_SPEED, 1200, 2);
                eff(player, MobEffects.MOVEMENT_SPEED, 1200, 2);
                eff(player, MobEffects.JUMP, 1200, 4);
                eff(player, MobEffects.HEALTH_BOOST, 1200, 9);
                eff(player, MobEffects.ABSORPTION, 1200, 4);
                eff(player, MobEffects.SATURATION, 1200, 9);
            }
            default -> {
                eff(player, MobEffects.REGENERATION, max, 4);
                eff(player, MobEffects.DAMAGE_RESISTANCE, max, 1);
                eff(player, MobEffects.FIRE_RESISTANCE, max, 0);
                eff(player, MobEffects.DAMAGE_BOOST, max, 9);
                eff(player, MobEffects.DIG_SPEED, max, 4);
                eff(player, MobEffects.MOVEMENT_SPEED, max, 3);
                eff(player, MobEffects.JUMP, max, 4);
                eff(player, MobEffects.HEALTH_BOOST, max, 24);
                eff(player, MobEffects.ABSORPTION, max, 14);
                eff(player, MobEffects.SATURATION, max, 99);
            }
        }
    }

    public static void appleLead(int meta, ItemStack stack, Level world, Player player) {
        if (world.isClientSide) return;
        if (meta == 0) eff(player, ModEffects.LEAD.get(), 15 * 20, 2);
        if (meta == 1) eff(player, ModEffects.LEAD.get(), 60 * 20, 4);
        if (meta == 2) player.hurt(com.hbm_m.damagesource.ModDamageSources.lead(world), 500F);
    }

    public static void appleEuphemium(ItemStack stack, Level world, Player player) {
        if (world.isClientSide) return;
        eff(player, MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 120);
        eff(player, MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0);
        eff(player, MobEffects.SATURATION, Integer.MAX_VALUE, 120);
    }

    // ------------------------------------------------------------------ Kleinkram

    public static void temFlakes(ItemStack stack, Level world, Player player) {
        player.heal(2F);
    }

    public static void schnitzelVegan(ItemStack stack, Level world, Player player) {
        if (world.isClientSide) return;
        eff(player, MobEffects.BLINDNESS, 10 * 20, 0);
        eff(player, MobEffects.CONFUSION, 30 * 20, 0);
        eff(player, MobEffects.HUNGER, 3 * 60 * 20, 4);
        eff(player, MobEffects.WITHER, 3 * 20, 0);
        player.setSecondsOnFire(5 * 20);
        Vec3 m = player.getDeltaMovement();
        player.setDeltaMovement(m.x, 2, m.z);
        player.hurtMarked = true;
    }

    public static void waffle(ItemStack stack, Level world, Player player) {
        if (!world.isClientSide) ExplosionNukeSmall.explode(world, player.getX(), player.getY() + 0.5, player.getZ(), ExplosionNukeSmall.PARAMS_MEDIUM);
    }

    public static void muchoMango(ItemStack stack, Level world, Player player) {
        if (!world.isClientSide) eff(player, MobEffects.MOVEMENT_SPEED, 200, 0);
    }

    /** {@code ItemPancake}: laedt alle Ruestungsteile, die Energie speichern, voll auf. */
    public static void pancake(ItemStack stack, Level world, Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
            ItemStack st = player.getItemBySlot(slot);
            if (st.isEmpty()) continue;
            //? if forge {
            st.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER).ifPresent(cap -> cap.receiveEnergy(Long.MAX_VALUE, false));
            //?}
        }
    }

    // ------------------------------------------------------------------ ItemPill

    public static void pill(ItemStack stack, Level world, Player player) {
        if (world.isClientSide) return;
        ModConsumables.applyPotionSickness(player, 5);
        var item = stack.getItem();

        if (item == ModItems.PILL_IODINE.get()) {
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.CONFUSION);
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
            player.removeEffect(MobEffects.HUNGER);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.WITHER);
            player.removeEffect(ModEffects.RADIATION.get());
        }
        if (item == ModItems.PLAN_C.get()) {
            for (int i = 0; i < 10; i++) {
                player.hurt(world.random.nextBoolean() ? com.hbm_m.damagesource.ModDamageSources.create(world, com.hbm_m.damagesource.ModDamageTypes.EUTHANIZED_SELF)
                        : com.hbm_m.damagesource.ModDamageSources.create(world, com.hbm_m.damagesource.ModDamageTypes.EUTHANIZED_SELF_2), 1000);
            }
        }
        if (item == ModItems.PILL_RED.get()) eff(player, ModEffects.DEATH.get(), 60 * 60 * 20, 0);
        if (item == ModItems.RADX.get()) eff(player, ModEffects.RADX.get(), 3 * 60 * 20, 0);
        if (item == ModItems.SIOX.get()) {
            HbmLivingProps.setAsbestos(player, 0);
            HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlackLung / 5));
        }
        if (item == ModItems.PILL_HERBAL.get()) {
            HbmLivingProps.setAsbestos(player, 0);
            HbmLivingProps.setBlackLung(player, Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlackLung / 5));
            HbmLivingProps.incrementRadiation(player, -100F);
            eff(player, MobEffects.CONFUSION, 10 * 20, 0);
            eff(player, MobEffects.WEAKNESS, 10 * 60 * 20, 2);
            eff(player, MobEffects.DIG_SLOWDOWN, 10 * 60 * 20, 2);
            eff(player, MobEffects.POISON, 5 * 20, 2);
            MobEffectInstance e = new MobEffectInstance(ModEffects.POTION_SICKNESS.get(), 10 * 60 * 20);
            e.setCurativeItems(new java.util.ArrayList<>());
            player.addEffect(e);
        }
        if (item == ModItems.XANAX.get()) {
            float digamma = HbmLivingProps.getDigamma(player);
            HbmLivingProps.setDigamma(player, Math.max(digamma - 0.5F, 0F));
        }
        if (item == ModItems.CHOCOLATE.get()) {
            if (world.random.nextInt(25) == 0) {
                player.hurt(com.hbm_m.damagesource.ModDamageSources.create(world, com.hbm_m.damagesource.ModDamageTypes.OVERDOSE), 1000);
            }
            eff(player, MobEffects.DIG_SPEED, 60 * 20, 3);
            eff(player, MobEffects.MOVEMENT_SPEED, 60 * 20, 3);
            eff(player, MobEffects.JUMP, 60 * 20, 3);
        }
        if (item == ModItems.FMN.get()) {
            float digamma = HbmLivingProps.getDigamma(player);
            HbmLivingProps.setDigamma(player, Math.min(digamma, 2F));
            eff(player, MobEffects.BLINDNESS, 60, 0);
        }
        if (item == ModItems.FIVE_HTP.get()) {
            HbmLivingProps.setDigamma(player, 0);
            eff(player, ModEffects.STABILITY.get(), 10 * 60 * 20, 0);
        }
    }
}
