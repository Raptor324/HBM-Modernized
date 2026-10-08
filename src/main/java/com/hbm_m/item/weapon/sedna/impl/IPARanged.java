package com.hbm_m.item.weapon.sedna.impl;

import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.items.armor.IPARanged}: Fernkampf-Komponente einer Power-Armor (siehe {@link IPAWeaponsProvider}). */
public interface IPARanged {

    public void clickPrimary(ItemStack stack, LambdaContext ctx);
    public void clickSecondary(ItemStack stack, LambdaContext ctx);
}
