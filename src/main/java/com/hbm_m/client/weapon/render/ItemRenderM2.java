package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderM2}. */
public class ItemRenderM2 extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

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
				-1.5F * offset, -2.5F * offset, 1.75F * offset,
				0, -12.5 / 8D, 1.75);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.greasegun_tex);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");

		GunGL.translate(0, 1, -2.25);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, -1, 2.25);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.pushMatrix();
		GunGL.rotate(180, 0, 1, 0);
		GunGL.bindTexture(WeaponResources.m2_tex);
		GunGL.renderAll(WeaponResources.m2);
		GunGL.popMatrix();

		double smokeScale = 0.5;

		GunGL.pushMatrix();
		GunGL.translate(0, 1.625, 5);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.375D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1.625, 5);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.5, 0.5, 0.5);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		setupThirdPerson(stack, 5.0D, 0.5, -2, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		setupInv(stack, 2.625D, 0.5, -1.25, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		setupModTable(stack, -15D, 0, -1.5, -0.5);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();
		GunGL.pushMatrix();
		GunGL.rotate(180, 0, 1, 0);
		GunGL.bindTexture(WeaponResources.m2_tex);
		GunGL.renderAll(WeaponResources.m2);
		GunGL.popMatrix();
		renderEquippedMuzzleFlash(type, stack, data, 0, 1.625, 5, 0.5D, 75, 7.5);
	}
}
