package com.hbm_m.powerarmor;

import com.hbm_m.armormod.item.ItemArmorMod;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.items.armor.IDamageHandler}: getragene Ruestung reagiert auf LivingHurtEvent. */
public interface IDamageHandler {
    void handleDamage(LivingEntity entity, ItemArmorMod.Hurt event, ItemStack stack);
}
