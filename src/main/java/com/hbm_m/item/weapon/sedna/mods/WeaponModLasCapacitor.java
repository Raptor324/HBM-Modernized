package com.hbm_m.item.weapon.sedna.mods;

import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.mags.MagazineFullReload;

import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unchecked")
public class WeaponModLasCapacitor extends WeaponModBase {

	public WeaponModLasCapacitor(int id) {
		super(id, "UNDERBARREL");
		this.setPriority(PRIORITY_MULTIPLICATIVE);
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == Receiver.F_BASEDAMAGE) return cast((Float) base * 1.05F, base);
		if(key == Receiver.O_MAGAZINE) {
			MagazineFullReload original = (MagazineFullReload) base;
			WeaponModStackMag.DUMMY_FULL.acceptedBullets = original.acceptedBullets;
			WeaponModStackMag.DUMMY_FULL.capacity = original.capacity * 3 / 2;
			WeaponModStackMag.DUMMY_FULL.index = original.index;
			return (T) WeaponModStackMag.DUMMY_FULL;
		}
		return base;
	}
}
