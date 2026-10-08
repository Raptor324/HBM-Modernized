package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderHangman}. */
public class ItemRenderHangman extends ItemRenderWeaponBase {

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
				-1.5F * offset, -0.875F * offset, 1.75F * offset,
				0, -1.5 / 8D, 1.25);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.hangman_tex);
		float offset = 0.8F;

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] roll = HbmAnimations.getRelevantTransformation("ROLL");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] smack = HbmAnimations.getRelevantTransformation("SMACK");
		double[] lid = HbmAnimations.getRelevantTransformation("LID");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] bullets = HbmAnimations.getRelevantTransformation("BULLETS");

		GunGL.translate(1.5F * offset, 0, -1);
		GunGL.rotate(turn[1], 0, 1, 0);
		GunGL.translate(-1.5F * offset, 0, 1);

		GunGL.rotate(roll[2], 0, 0, 1);
		GunGL.translate(smack[0], smack[1], smack[2]);

		double scale = 0.125D;
		GunGL.scale(scale, scale, scale);

		GunGL.translate(0, -4, -10);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 4, 10);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.renderPart(WeaponResources.hangman, "Rifle");
		GunGL.renderPart(WeaponResources.hangman, "Internals");

		GunGL.pushMatrix();
		//i give the fuck up
		GunGL.translate(-2.1875, -1.75, 0);
		GunGL.rotate(lid[2], 0, 0, 1);
		GunGL.translate(2.1875, 1.75, 0);
		GunGL.renderPart(WeaponResources.hangman, "Lid");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);
		GunGL.renderPart(WeaponResources.hangman, "Magazine");
		if(bullets[0] == 0) GunGL.renderPart(WeaponResources.hangman, "Bullets");
		GunGL.popMatrix();

		double smokeScale = 1.5;

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 29);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 29);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(2, 2, 2);
		renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 4.25, 11);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.5, 2.5, 0);
	}

	@Override
	public void setupEntity(ItemStack stack) {
		double scale = 0.0625D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -2.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.hangman_tex);
		GunGL.renderAll(WeaponResources.hangman);

		if(type == ItemRenderType.EQUIPPED) {
			LivingEntity ent = (LivingEntity) data[1];
			long shot;
			double shotRand = 0;
			if(ent == Minecraft.getInstance().player) {
				ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
				shot = gun.lastShot[0];
				shotRand = gun.shotRand;
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			GunGL.pushMatrix();
			GunGL.translate(0, 0, 29);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(2, 2, 2);
			renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}
}
