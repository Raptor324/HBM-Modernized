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

/** 1:1 {@code ItemRenderStarF}. */
public class ItemRenderStarF extends ItemRenderWeaponBase {

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
				-1.75F * offset, -1.75F * offset, 2.5F * offset,
				0, -7.625 / 8D, 1);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.star_f_tex);
		double scale = 0.25D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] tilt = HbmAnimations.getRelevantTransformation("TILT");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");

		GunGL.translate(0, -2, -8);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 2, 8);

		GunGL.translate(0, 1, -3);
		GunGL.rotate(turn[2], 0, 0, 1);
		GunGL.rotate(tilt[0], 1, 0, 0);
		GunGL.translate(0, -1, 3);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.renderPart(WeaponResources.star_f, "Gun");

		GunGL.pushMatrix();
		GunGL.translate(0, 1.75, -4.25);
		GunGL.rotate(60 * (hammer[0] - 1), 1, 0, 0);
		GunGL.translate(0, -1.75, 4.25);
		GunGL.renderPart(WeaponResources.star_f, "Hammer");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, slide[2] * 2.3125);
		GunGL.renderPart(WeaponResources.star_f, "Slide");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);
		GunGL.renderPart(WeaponResources.star_f, "Mag");
		GunGL.translate(bullet[0], bullet[1], bullet[2]);
		GunGL.renderPart(WeaponResources.star_f, "Bullet");
		GunGL.popMatrix();

		if(hasSilencer(stack)) {
			GunGL.pushMatrix();
			GunGL.translate(0, 2.375, -0.25);
			GunGL.bindTexture(WeaponResources.uzi_tex);
			GunGL.renderPart(WeaponResources.uzi, "Silencer");
			GunGL.popMatrix();

		} else {
			double smokeScale = 0.5;

			GunGL.pushMatrix();
			GunGL.translate(0, 3, 6.125);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.scale(smokeScale, smokeScale, smokeScale);
			this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.75D);
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 3, 6.125);
			GunGL.scale(0.75, 0.75, 0.75);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
			this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, -0.25, 1.75);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-1, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.25, -5);
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		GunGL.enableLighting();

		renderStandardGun(stack);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		boolean silenced = hasSilencer(stack);

		if(silenced && type == ItemRenderType.INVENTORY) {
			double scale = 0.625D;
			GunGL.scale(scale, scale, scale);
			GunGL.translate(0, 0, -6);
		}

		renderStandardGun(stack);

		if(type == ItemRenderType.EQUIPPED && !silenced) {
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
			GunGL.translate(0, 3, 6.25);
			GunGL.scale(0.75, 0.75, 0.75);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}

	public boolean hasSilencer(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
	}

	public void renderStandardGun(ItemStack stack) {

		GunGL.bindTexture(WeaponResources.star_f_tex);
		GunGL.renderPart(WeaponResources.star_f, "Gun");
		GunGL.renderPart(WeaponResources.star_f, "Slide");
		GunGL.renderPart(WeaponResources.star_f, "Mag");
		GunGL.renderPart(WeaponResources.star_f, "Hammer");
		boolean silenced = hasSilencer(stack);
		if(silenced) {
			GunGL.translate(0, 2.375, -0.25);
			GunGL.bindTexture(WeaponResources.uzi_tex);
			GunGL.renderPart(WeaponResources.uzi, "Silencer");
		}
	}
}
