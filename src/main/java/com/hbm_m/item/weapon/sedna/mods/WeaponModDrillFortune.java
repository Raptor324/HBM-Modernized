package com.hbm_m.item.weapon.sedna.mods;

import com.hbm_m.util.EnchantmentUtil;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

public class WeaponModDrillFortune extends WeaponModBase {

	int addFortune = 0;

	public WeaponModDrillFortune(int id, String slot, int fortune) {
		super(id, slot);
		this.setPriority(PRIORITY_ADDITIVE);
		this.addFortune = fortune;
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		return base;
	}

	@Override
	public void onInstall(ItemStack gun, ItemStack mod, int index) {
		int fortuneLvl = EnchantmentUtil.getEnchantmentLevel(gun, Enchantments.BLOCK_FORTUNE);
		fortuneLvl += this.addFortune;
		EnchantmentUtil.removeEnchantment(gun, Enchantments.BLOCK_FORTUNE);
		EnchantmentUtil.addEnchantment(gun, Enchantments.BLOCK_FORTUNE, fortuneLvl);
	}

	@Override
	public void onUninstall(ItemStack gun, ItemStack mod, int index) {
		int fortuneLvl = EnchantmentUtil.getEnchantmentLevel(gun, Enchantments.BLOCK_FORTUNE);
		fortuneLvl -= this.addFortune;
		EnchantmentUtil.removeEnchantment(gun, Enchantments.BLOCK_FORTUNE);
		if(fortuneLvl > 0) EnchantmentUtil.addEnchantment(gun, Enchantments.BLOCK_FORTUNE, fortuneLvl);
	}
}
