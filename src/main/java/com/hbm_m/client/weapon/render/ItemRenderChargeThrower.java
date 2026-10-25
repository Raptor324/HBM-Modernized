package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.factory.XFactoryTool;
import com.hbm_m.item.weapon.sedna.mags.MagazineFullReload;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderChargeThrower}. */
public class ItemRenderChargeThrower extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		float zoom = 0.5F;

		if(isScoped(stack)) standardAimingTransform(stack,
				-1.5F * offset, -1.25F * offset, 3.5F * offset,
				-0.15625, -6.5 / 8D, 1.6875);
		else standardAimingTransform(stack,
				-1.5F * offset, -1.25F * offset, 3.5F * offset,
				-1.5F * zoom, -1.25F * zoom, 3.5F * zoom);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		boolean usingScope = this.isScoped(stack) && ItemGunBaseNT.aimingProgress == 1 && ItemGunBaseNT.prevAimingProgress == 1;
		MagazineFullReload mag = (MagazineFullReload) gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

		if(usingScope) {
			double scale = 3.5D;
			GunGL.scale(scale, scale, scale);
			GunGL.translate(-0.5, -1.5, -4);
		} else {
			double scale = 0.5D;
			GunGL.scale(scale, scale, scale);
		}

		boolean reloading = HbmAnimations.getRelevantAnim(0) != null && HbmAnimations.getRelevantAnim(0).animation.getBus("AMMO") != null;
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] raise = HbmAnimations.getRelevantTransformation("RAISE");
		double[] ammo = HbmAnimations.getRelevantTransformation("AMMO");
		double[] twist = HbmAnimations.getRelevantTransformation("TWIST");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] roll = HbmAnimations.getRelevantTransformation("ROLL");

		GunGL.translate(0, 0, -7);
		GunGL.rotate(equip[0], -1, 0, 0);
		GunGL.translate(0, 0, 7);

		GunGL.translate(0, -7, 4);
		GunGL.rotate(raise[0], 1, 0, 0);
		GunGL.translate(0, 7, -4);

		GunGL.translate(recoil[0], recoil[1], recoil[2]);

		GunGL.translate(0, 0, -2);
		GunGL.rotate(turn[1], 0, 1, 0);
		GunGL.translate(0, 0, 2);
		GunGL.translate(0, -1, 0);
		GunGL.rotate(roll[2], 0, 0, 1);
		GunGL.translate(0, 1, 0);

		GunGL.bindTexture(WeaponResources.charge_thrower_tex);
		GunGL.renderPart(WeaponResources.charge_thrower, "Gun");
		if(isScoped(stack) && !usingScope) GunGL.renderPart(WeaponResources.charge_thrower, "Scope");

		if(mag.getAmount(stack, null) > 0 || reloading) {

			GunGL.translate(ammo[0], ammo[1], ammo[2]);
			GunGL.rotate(twist[2], 0, 0, 1);

			if(mag.getType(stack, null) == XFactoryTool.ct_hook) {
				GunGL.bindTexture(WeaponResources.charge_thrower_hook_tex);
				GunGL.renderPart(WeaponResources.charge_thrower, "Hook");
			}
			if(mag.getType(stack, null) == XFactoryTool.ct_mortar) {
				GunGL.bindTexture(WeaponResources.charge_thrower_mortar_tex);
				GunGL.renderPart(WeaponResources.charge_thrower, "Mortar");
			}
			if(mag.getType(stack, null) == XFactoryTool.ct_mortar_charge) {
				GunGL.bindTexture(WeaponResources.charge_thrower_mortar_tex);
				GunGL.renderPart(WeaponResources.charge_thrower, "Mortar");
				GunGL.renderPart(WeaponResources.charge_thrower, "Oomph");
			}
			//GunGL.bindTexture(WeaponResources.charge_thrower_rocket_tex);
			//GunGL.renderPart(WeaponResources.charge_thrower, "Rocket");
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0.75, 1, 4);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0, 0, -0.625);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 0, -1);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.charge_thrower_tex);
		GunGL.renderPart(WeaponResources.charge_thrower, "Gun");
		if(isScoped(stack)) GunGL.renderPart(WeaponResources.charge_thrower, "Scope");

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		MagazineFullReload mag = (MagazineFullReload) gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

		if(mag.getAmount(stack, null) > 0) {

			if(mag.getType(stack, null) == XFactoryTool.ct_hook) {
				GunGL.bindTexture(WeaponResources.charge_thrower_hook_tex);
				GunGL.renderPart(WeaponResources.charge_thrower, "Hook");
			}
			if(mag.getType(stack, null) == XFactoryTool.ct_mortar) {
				GunGL.bindTexture(WeaponResources.charge_thrower_mortar_tex);
				GunGL.renderPart(WeaponResources.charge_thrower, "Mortar");
			}
			if(mag.getType(stack, null) == XFactoryTool.ct_mortar_charge) {
				GunGL.bindTexture(WeaponResources.charge_thrower_mortar_tex);
				GunGL.renderPart(WeaponResources.charge_thrower, "Mortar");
				GunGL.renderPart(WeaponResources.charge_thrower, "Oomph");
			}
		}
	}

	public boolean isScoped(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}
}
