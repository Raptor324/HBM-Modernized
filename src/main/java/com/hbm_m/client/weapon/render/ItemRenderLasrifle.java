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

/** 1:1 {@code ItemRenderLasrifle}. */
public class ItemRenderLasrifle extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * (hasScope(stack) ? 0.75F : 0.66F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;

		if(hasScope(stack)) {
			standardAimingTransform(stack,
					-1.5F * offset, -1.5F * offset, 2.5F * offset,
				0, -7.375 / 8D, 0.75);
		} else {
			standardAimingTransform(stack,
					-1.5F * offset, -1.5F * offset, 2.5F * offset,
				0, -5.25 / 8D, 1);
		}
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		if(hasScope(stack) && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.lasrifle_tex);
		double scale = 0.3125D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");

		GunGL.translate(0, -1, -6);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 1, 6);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.renderPart(WeaponResources.lasrifle, "Gun");
		GunGL.renderPart(WeaponResources.lasrifle, "Stock");
		if(hasScope(stack)) GunGL.renderPart(WeaponResources.lasrifle, "Scope");

		GunGL.pushMatrix();
		GunGL.translate(0, -0.375, 2.375);
		GunGL.rotate(lever[0], 1, 0, 0);
		GunGL.translate(0, 0.375, -2.375);
		GunGL.renderPart(WeaponResources.lasrifle, "Lever");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);
		GunGL.renderPart(WeaponResources.lasrifle, "Battery");
		GunGL.popMatrix();

		if(!hasShotgun(stack)) GunGL.renderPart(WeaponResources.lasrifle, "Barrel");
		GunGL.bindTexture(WeaponResources.lasrifle_mods_tex);
		if(hasShotgun(stack)) GunGL.renderPart(WeaponResources.lasrifle_mods, "BarrelShotgun");
		if(hasCapacitor(stack)) GunGL.renderPart(WeaponResources.lasrifle_mods, "UnderBarrel");

		GunGL.pushMatrix();
		GunGL.translate(0, 1.5, 12);
		GunGL.rotate(90, 0, 1, 0);
		renderLaserFlash(gun.lastShot[0], 150, 1.5D, 0xff0000);
		GunGL.translate(0, 0, -0.25);
		renderLaserFlash(gun.lastShot[0], 150, 0.75D, 0xff8000);
		GunGL.popMatrix();

	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0, 4);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.03125D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0.75, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -1, -1);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.lasrifle_tex);
		GunGL.renderPart(WeaponResources.lasrifle, "Gun");
		GunGL.renderPart(WeaponResources.lasrifle, "Stock");
		if(hasScope(stack)) GunGL.renderPart(WeaponResources.lasrifle, "Scope");
		GunGL.renderPart(WeaponResources.lasrifle, "Lever");
		GunGL.renderPart(WeaponResources.lasrifle, "Battery");
		if(!hasShotgun(stack)) GunGL.renderPart(WeaponResources.lasrifle, "Barrel");
		GunGL.bindTexture(WeaponResources.lasrifle_mods_tex);
		if(hasShotgun(stack)) GunGL.renderPart(WeaponResources.lasrifle_mods, "BarrelShotgun");
		if(hasCapacitor(stack)) GunGL.renderPart(WeaponResources.lasrifle_mods, "UnderBarrel");

		if(type == ItemRenderType.EQUIPPED) {
			LivingEntity ent = (LivingEntity) data[1];
			long shot;
			if(ent == Minecraft.getInstance().player) {
				ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
				shot = gun.lastShot[0];
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			GunGL.pushMatrix();
			GunGL.translate(0, 1.5, 12);
			GunGL.rotate(90, 0, 1, 0);
			renderLaserFlash(shot, 150, 1.5D, 0xff0000);
			GunGL.translate(0, 0, -0.25);
			renderLaserFlash(shot, 150, 0.75D, 0xff8000);
			GunGL.popMatrix();
		}
	}

	public boolean hasScope(ItemStack stack) {
		return !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_LAS_AUTO);
	}

	public boolean hasShotgun(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_LAS_SHOTGUN);
	}

	public boolean hasCapacitor(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_LAS_CAPACITOR);
	}
}
