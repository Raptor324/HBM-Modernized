package com.hbm_m.item.weapon.sedna.mods;

import java.util.function.BiConsumer;

import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;

import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unchecked")
public class WeaponModPolymerFurniture extends WeaponModBase {

	public WeaponModPolymerFurniture(int id) {
		super(id, "FURNITURE");
	}

	@Override
	public <T> T eval(T base, ItemStack gun, String key, Object parent) {
		if(key == Receiver.CON_ONRECOIL) return (T) LAMBDA_RECOIL_G3;
		return base;
	}

	public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_G3 = (stack, ctx) -> {
		if (ctx.entity == null) return;
		ItemGunBaseNT.setupRecoil((float) (ctx.entity.getRandom().nextGaussian() * 0.125), (float) (ctx.entity.getRandom().nextGaussian() * 0.125));
	};

}
