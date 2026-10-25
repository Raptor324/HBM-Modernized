package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderLAG}. */
public class ItemRenderLAG extends ItemRenderWeaponBase {

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
				-1.5F * offset, -1F * offset, 1.5F * offset,
				0, -3.375 / 8D, 0.5);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.mike_hawk_tex);
		double scale = 0.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		//double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] addTrans = HbmAnimations.getRelevantTransformation("ADD_TRANS");
		double[] addRot = HbmAnimations.getRelevantTransformation("ADD_ROT");
		//Animation anim = HbmAnimations.getRelevantAnim(0);

		GunGL.translate(4, -4, 0);
		GunGL.rotate(-equip[0], 0, 0, 1);
		GunGL.translate(-4, 4, 0);

		GunGL.translate(addTrans[0], addTrans[1], addTrans[2]);
		GunGL.rotate(addRot[2], 0, 0, 1);
		GunGL.rotate(addRot[1], 0, 1, 0);

		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("Grip");
		GunGL.renderPart(WeaponResources.mike_hawk, "Grip");

		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("Slide");

		/*if(anim != null) {
			BusAnimationSequence slideSeq = anim.animation.getBus("Hammer");
			if(slideSeq != null) GunGL.translate(0, 0.75, 0);
		}*/

		GunGL.renderPart(WeaponResources.mike_hawk, "Slide");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(3.125, 0.125, 0);
		GunGL.rotate(-25, 0, 0, 1);
		GunGL.translate(-3.125, -0.125, 0);
		HbmAnimations.applyRelevantTransformation("Hammer");
		GunGL.renderPart(WeaponResources.mike_hawk, "Hammer");
		GunGL.popMatrix();

		if(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0) {
			GunGL.pushMatrix();
			HbmAnimations.applyRelevantTransformation("Bullet");
			GunGL.renderPart(WeaponResources.mike_hawk, "Bullet");
			GunGL.popMatrix();
		}

		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("Magazine");
		GunGL.renderPart(WeaponResources.mike_hawk, "Magazine");
		GunGL.popMatrix();

		double smokeScale = 0.5;

		GunGL.pushMatrix();
		GunGL.translate(-10.25, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(-10.25, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();

		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, 1, 1);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(2.5, 1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 0.5, -2);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();
		GunGL.rotate(90, 0, 1, 0);

		GunGL.bindTexture(WeaponResources.mike_hawk_tex);
		GunGL.renderPart(WeaponResources.mike_hawk, "Grip");
		GunGL.renderPart(WeaponResources.mike_hawk, "Slide");
		GunGL.renderPart(WeaponResources.mike_hawk, "Hammer");

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
			GunGL.translate(-10.25, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}
}
