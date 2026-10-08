package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderFlaregun}. */
public class ItemRenderFlaregun extends ItemRenderWeaponBase {

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
				-1.25F * offset, -1.5F * offset, 2F * offset,
				0, -5.5 / 8D, 0.5);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.flaregun_tex);
		double scale = 0.125D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] open = HbmAnimations.getRelevantTransformation("OPEN");
		double[] shell = HbmAnimations.getRelevantTransformation("SHELL");
		double[] flip = HbmAnimations.getRelevantTransformation("FLIP");

		GunGL.translate(recoil[0], recoil[1], recoil[2]);
		GunGL.rotate(recoil[2] * 10, 1, 0, 0);
		GunGL.rotate(flip[0], 1, 0, 0);

		GunGL.translate(0, 0, -8);
		GunGL.rotate(equip[0], -1, 0, 0);
		GunGL.translate(0, 0, 8);

		GunGL.renderPart(WeaponResources.flaregun, "Gun");

		GunGL.pushMatrix();
		GunGL.translate(0, 1.8125, -4);
		GunGL.rotate(hammer[0] - 15, 1, 0, 0);
		GunGL.translate(0, -1.8125, 4);
		GunGL.renderPart(WeaponResources.flaregun, "Hammer");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 2.156, 1.78);
		GunGL.rotate(open[0], 1, 0, 0);
		GunGL.translate(0, -2.156, -1.78);
		GunGL.renderPart(WeaponResources.flaregun, "Barrel");
		GunGL.translate(shell[0], shell[1], shell[2]);
		GunGL.renderPart(WeaponResources.flaregun, "Flare");
		GunGL.popMatrix();

		double smokeScale = 0.5;

		GunGL.pushMatrix();
		GunGL.translate(0, 4, 9);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 2.5D);
		GunGL.translate(0, 0, 0.1);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 2D);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0.25, 3);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.5, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
	}

	@Override
	public void setupEntity(ItemStack stack) {
		super.setupEntity(stack);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.flaregun_tex);
		GunGL.renderAll(WeaponResources.flaregun);
	}
}
