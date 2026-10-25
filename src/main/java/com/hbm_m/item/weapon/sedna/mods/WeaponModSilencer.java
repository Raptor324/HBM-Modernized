package com.hbm_m.item.weapon.sedna.mods;

import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.WeaponItems;

import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unchecked")
public class WeaponModSilencer extends WeaponModBase {

	/** NTMSounds.GUN_AMAT_SILENCER / GUN_RIFLE_SILENCER */
	public static final String GUN_AMAT_SILENCER = "hbm:weapon.silencerShoot";
	public static final String GUN_RIFLE_SILENCER = "hbm:weapon.fire.silenced";

	public WeaponModSilencer(int id) {
		super(id, "SILENCER");
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {

		if(key == Receiver.S_FIRESOUND) {
			if(gun.getItem() == WeaponItems.gun("gun_amat")) return (T) GUN_AMAT_SILENCER;
			return (T) GUN_RIFLE_SILENCER;
		}

		return base;
	}
}
