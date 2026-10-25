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

/** 1:1 {@code ItemRenderPanzerschreck}. */
public class ItemRenderPanzerschreck extends ItemRenderWeaponBase {

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
				-2.75F * offset, -2F * offset, 2.5F * offset,
			-0.9375, -9.25 / 8D, 0.25);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.panzerschreck_tex);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
		double[] rocket = HbmAnimations.getRelevantTransformation("ROCKET");

		GunGL.translate(0, -1, -1);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 1, 1);

		GunGL.translate(0, -4, -3);
		GunGL.rotate(reload[0], 1, 0, 0);
		GunGL.translate(0, 4, 3);

		GunGL.renderPart(WeaponResources.panzerschreck, "Tube");
		if(hasShield(stack)) GunGL.renderPart(WeaponResources.panzerschreck, "Shield");

		GunGL.pushMatrix();
		GunGL.translate(rocket[0], rocket[1], rocket[2]);
		GunGL.renderPart(WeaponResources.panzerschreck, "Rocket");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 6.5);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.75, 0.75, 0.75);
		this.renderMuzzleFlash(gun.lastShot[0], 150, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 3D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0.5, 1);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.5, 0.5, 0);
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

		GunGL.bindTexture(WeaponResources.panzerschreck_tex);
		GunGL.renderPart(WeaponResources.panzerschreck, "Tube");
		if(hasShield(stack)) GunGL.renderPart(WeaponResources.panzerschreck, "Shield");

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
			GunGL.translate(0, 0, 6.5);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			this.renderMuzzleFlash(shot, 150, 7.5);
			GunGL.popMatrix();
		}
	}

	public boolean hasShield(ItemStack stack) {
		return !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_SHIELD);
	}
}
