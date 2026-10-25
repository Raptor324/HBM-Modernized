package com.hbm_m.item.weapon.sedna.mods;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.item.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm_m.item.weapon.sedna.mags.MagazineLiquidEngine;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code WeaponModEngine}. Die Klasse wird erst in {@link XWeaponModInit#init()} geladen, die Fluessigkeiten sind dann registriert. */
@SuppressWarnings("rawtypes")
public class WeaponModEngine extends WeaponModBase {

	public static final MagazineLiquidEngine ENGINE_DIESEL = new MagazineLiquidEngine(0, 4_000, ModFluids.DIESEL.getSource(), ModFluids.DIESEL_CRACK.getSource(), ModFluids.LIGHTOIL.getSource());
	public static final MagazineLiquidEngine ENGINE_AVIATION = new MagazineLiquidEngine(0, 4_000, ModFluids.KEROSENE.getSource(), ModFluids.LPG.getSource());
	public static final MagazineElectricEngine ENGINE_ELECTRIC = new MagazineElectricEngine(0, 1_000_000);
	public static final MagazineLiquidEngine ENGINE_TURBO = new MagazineLiquidEngine(0, 4_000, ModFluids.KEROSENE_REFORM.getSource(), ModFluids.REFORMATE.getSource());

	protected IMagazine mag;
	protected int delay;

	public WeaponModEngine(int id) {
		super(id, "ENGINE");
		this.setPriority(PRIORITY_SET);
	}

	public WeaponModEngine mag(IMagazine mag) { this.mag = mag; return this; }
	public WeaponModEngine delay(int delay) { this.delay = delay; return this; }

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {

		if(key == Receiver.O_MAGAZINE && mag != null) return cast(mag, base);
		if(key == Receiver.I_DELAYAFTERFIRE) return cast((Integer) delay, base);

		return base;
	}

	@Override public void onInstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
	@Override public void onUninstall(ItemStack gun, ItemStack mod, int index) { XWeaponModManager.changedMagState(); }
}
