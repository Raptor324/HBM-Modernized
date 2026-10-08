package com.hbm_m.item.weapon.sedna.mods;

import com.hbm_m.item.weapon.sedna.Receiver;

import net.minecraft.world.item.ItemStack;

public class WeaponModTestDamage extends WeaponModBase {

	public WeaponModTestDamage(int id, String... slots) {
		super(id, slots);
		this.setPriority(PRIORITY_MULT_FINAL);
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {

		if(parent instanceof Receiver && key == Receiver.F_BASEDAMAGE && base instanceof Float) {
			return cast((Float) base * 1.5F, base);
		}

		return base;
	}
}
