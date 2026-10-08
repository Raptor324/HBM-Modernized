package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderDrill}. */
public class ItemRenderDrill extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.25F * offset, -1.75F * offset, 1.75F * offset,
				-1F * offset, -1.75F * offset, 1.25F * offset);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.drill_tex);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		double gauge = (double) mag.getAmount(stack, null) / (double) mag.getCapacity(stack);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] deploy = HbmAnimations.getRelevantTransformation("DEPLOY");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] spin = HbmAnimations.getRelevantTransformation("SPIN");

		/*float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		GunGL.rotate(15 * (1 - aimingProgress), 0, 1, 0);
		GunGL.rotate(-10 * (1 - aimingProgress), 1, 0, 0);*/

		GunGL.rotate(15 * (1 - deploy[0] * 0.5), 0, 1, 0);
		GunGL.rotate(-10 * (1 - deploy[0] * 0.5), 1, 0, 0);

		GunGL.translate(0, 2, -6);
		GunGL.rotate(equip[0] * -45, 0, 1, 0);
		GunGL.rotate(equip[0] * -20, 1, 0, 0);
		GunGL.translate(0, -2, 6);

		GunGL.rotate(lift[0], 1, 0, 0);

		GunGL.translate(0, 0, deploy[0]);

		GunGL.renderPart(WeaponResources.drill, "Base");

		GunGL.pushMatrix();
		GunGL.translate(1, 2.0625, -1.75);
		GunGL.rotate(45, 1, 0, 0);
		GunGL.rotate(-135 + gauge * 270, 0, 0, 1);
		GunGL.rotate(-45, 1, 0, 0);
		GunGL.translate(-1, -2.0625, 1.75);
		GunGL.renderPart(WeaponResources.drill, "Gauge");
		GunGL.popMatrix();

		double rot = spin[0];
		double rot2 = rot * 5;

		GunGL.pushMatrix();
		GunGL.translate(0, Math.sin(rot2 * Math.PI / 180) * 0.125 - 0.125, 0);
		GunGL.renderPart(WeaponResources.drill, "Piston1");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, Math.sin(rot2 * Math.PI / 180 + Math.PI * 2D / 3D) * 0.125 - 0.125, 0);
		GunGL.renderPart(WeaponResources.drill, "Piston2");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, Math.sin(rot2 * Math.PI / 180 + Math.PI * 4D / 3D) * 0.125 - 0.125, 0);
		GunGL.renderPart(WeaponResources.drill, "Piston3");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.rotate(rot, 0, 0, -1);
		GunGL.renderPart(WeaponResources.drill, "DrillBack");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.rotate(rot, 0, 0, 1);
		GunGL.renderPart(WeaponResources.drill, "DrillFront");
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(1, -2, 6);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.5, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 0, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.drill_tex);
		GunGL.renderAll(WeaponResources.drill);
	}
}
