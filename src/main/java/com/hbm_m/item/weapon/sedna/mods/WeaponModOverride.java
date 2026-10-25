package com.hbm_m.item.weapon.sedna.mods;

import com.hbm_m.item.weapon.sedna.Receiver;

import net.minecraft.world.item.ItemStack;

public class WeaponModOverride extends WeaponModBase {

	protected final float baseDamage;

	public WeaponModOverride(int id, float baseDamage, String... slots) {
		super(id, slots);
		this.baseDamage = baseDamage;
		this.setPriority(PRIORITY_SET);
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == Receiver.F_BASEDAMAGE) return cast(baseDamage, base);
		return base;
	}
}
