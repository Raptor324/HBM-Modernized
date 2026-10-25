package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderGreasegun}. */
public class ItemRenderGreasegun extends ItemRenderWeaponBase {

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
				-1.5F * offset, -1F * offset, 1.75F * offset,
				0, -2.625 / 8D, 1.125);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(isRefurbished(stack) ? WeaponResources.greasegun_clean_tex : WeaponResources.greasegun_tex);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] stock = HbmAnimations.getRelevantTransformation("STOCK");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] flap = HbmAnimations.getRelevantTransformation("FLAP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");

		GunGL.translate(0, -3, -3);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 3, 3);

		GunGL.translate(0, -3, -3);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 3, 3);

		if(ItemGunBaseNT.aimingProgress < 1F) GunGL.rotate(turn[2], 0, 0, 1);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.renderPart(WeaponResources.greasegun, "Gun");

		GunGL.pushMatrix();
		GunGL.translate(0, 0, -4 - stock[2]);
		GunGL.renderPart(WeaponResources.greasegun, "Stock");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);
		GunGL.renderPart(WeaponResources.greasegun, "Magazine");
		if(bullet[0] != 1) GunGL.renderPart(WeaponResources.greasegun, "Bullet");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, -1.4375, -0.125);
		GunGL.rotate(handle[0], 1, 0, 0);
		GunGL.translate(0, 1.4375, 0.125);
		GunGL.renderPart(WeaponResources.greasegun, "Handle");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0.53125, 0);
		GunGL.rotate(flap[2], 0, 0, 1);
		GunGL.translate(0, -0.5125, 0);
		GunGL.renderPart(WeaponResources.greasegun, "Flap");
		GunGL.popMatrix();

		double smokeScale = 0.25;

		GunGL.pushMatrix();
		GunGL.translate(-0.25, 0, 1.5);
		GunGL.rotate(turn[2], 0, 0, -1);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 8);
		GunGL.rotate(turn[2], 0, 0, -1);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.5, 0.5, 0.5);
		renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.5, 2, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 2, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(isRefurbished(stack) ? WeaponResources.greasegun_clean_tex : WeaponResources.greasegun_tex);
		GunGL.renderAll(WeaponResources.greasegun);

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
			GunGL.translate(0, 0, 8);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(0.5, 0.5, 0.5);
			renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}

	public boolean isRefurbished(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_GREASEGUN_CLEAN);
	}
}
