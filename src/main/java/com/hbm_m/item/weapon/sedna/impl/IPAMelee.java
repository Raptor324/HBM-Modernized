package com.hbm_m.item.weapon.sedna.impl;

import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;

import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.items.armor.IPAMelee}: Nahkampf-Komponente einer Power-Armor (Brustteil liefert sie ueber
 * {@link IPAWeaponsProvider}). Liegt im Waffenpaket, weil die Power-Armor-Seite im Port sie noch nicht hat.
 */
public interface IPAMelee {

    public void setupFirstPerson(ItemStack stack);
    public void renderFirstPerson(ItemStack stack);

    public BusAnimation playAnim(ItemStack stack, GunAnimation type);
    public void orchestra(ItemStack stack, LambdaContext ctx);

    public void clickPrimary(ItemStack stack, LambdaContext ctx);
    public void clickSecondary(ItemStack stack, LambdaContext ctx);
}
