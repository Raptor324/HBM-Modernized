package com.hbm_m.item.weapon.sedna.mods;

import com.hbm_m.item.weapon.sedna.Receiver;

import net.minecraft.world.item.ItemStack;

public class WeaponModShredderSpeedup extends WeaponModBase {

	public WeaponModShredderSpeedup(int id) {
		super(id, "SPEED");
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == Receiver.I_DELAYAFTERFIRE) return cast((Integer) base / 2, base);
		if(key == Receiver.I_DELAYAFTERDRYFIRE) return cast((Integer) base / 2, base);
		return base;
	}
}
