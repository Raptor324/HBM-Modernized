package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderCoilgun}. */
public class ItemRenderCoilgun extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.25F * offset, -1.5F * offset, 2.5F * offset,
				0, -7.5 / 8D, 1);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		GunGL.bindTexture(WeaponResources.flaregun_tex);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);

		GunGL.rotate(-90, 0, 1, 0);

		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		GunGL.translate(-1.5 - recoil[0] * 0.5, 0, 0);
		GunGL.rotate(recoil[0] * 45, 0, 0, 1);
		GunGL.translate(1.5, 0, 0);

		double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
		GunGL.translate(-2.5, 0, 0);
		GunGL.rotate(reload[0] * -45, 0, 0, 1);
		GunGL.translate(2.5, 0, 0);

		GunGL.bindTexture(WeaponResources.coilgun_tex);
		GunGL.renderAll(WeaponResources.coilgun);
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 3D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0.25, 1.25);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 4D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.25, -0.25, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -20D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.25, 0.5);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.rotate(-90, 0, 1, 0);

		GunGL.bindTexture(WeaponResources.coilgun_tex);
		GunGL.renderAll(WeaponResources.coilgun);
	}
}
