package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderTau}. */
public class ItemRenderTau extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.75F * offset, -1.75F * offset, 3.5F * offset,
				-1.75F * offset, -1.75F * offset, 3.5F * offset);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		GunGL.bindTexture(WeaponResources.tau_tex);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE");

		GunGL.translate(0, -1, -4);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 1, 4);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.translate(0, 0, -2);
		GunGL.rotate(recoil[2] * 5, 1, 0, 0);
		GunGL.translate(0, 0, 2);

		GunGL.disableCull();

		GunGL.renderPart(WeaponResources.tau, "Body");

		GunGL.pushMatrix();
		GunGL.translate(0, -0.25, 0);
		GunGL.rotate(rotate[2], 0, 0, 1);
		GunGL.translate(0, 0.25, 0);
		GunGL.renderPart(WeaponResources.tau, "Rotor");
		GunGL.popMatrix();

		GunGL.enableCull();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 1, 2);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.25, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.disableCull();
		GunGL.bindTexture(WeaponResources.tau_tex);
		GunGL.renderAll(WeaponResources.tau);
		GunGL.enableCull();
	}
}
