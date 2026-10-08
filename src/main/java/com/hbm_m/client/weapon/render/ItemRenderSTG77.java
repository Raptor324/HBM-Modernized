package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderSTG77}. */
public class ItemRenderSTG77 extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 0.5F : -0.25F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.5F * offset, -1F * offset, 2.5F * offset,
			0, -5.75 / 8D, 2);
	}

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.66F);
	}

	@Override
	protected float getBaseFOV(ItemStack stack) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return 70F - aimingProgress * 65;
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		if(ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.stg77_tex);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] bolt = HbmAnimations.getRelevantTransformation("BOLT");
		double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
		double[] safety = HbmAnimations.getRelevantTransformation("SAFETY");

		double[] inspectGun = HbmAnimations.getRelevantTransformation("INSPECT_GUN");
		double[] inspectBarrel = HbmAnimations.getRelevantTransformation("INSPECT_BARREL");
		double[] inspectMove = HbmAnimations.getRelevantTransformation("INSPECT_MOVE");
		double[] inspectLever = HbmAnimations.getRelevantTransformation("INSPECT_LEVER");

		GunGL.translate(0, -1, -4);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 1, 4);

		GunGL.translate(0, 0, -4);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 0, 4);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.pushMatrix();

		//GunGL.rotate(-70, 0, 0, 1);
		//GunGL.rotate(15, 1, 0, 0);
		GunGL.rotate(inspectGun[2], 0, 0, 1);
		GunGL.rotate(inspectGun[0], 1, 0, 0);

		HbmAnimations.applyRelevantTransformation("Gun");
		GunGL.renderPart(WeaponResources.stg77, "Gun");

		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("Magazine");
		GunGL.renderPart(WeaponResources.stg77, "Magazine");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.rotate(inspectLever[2], 0, 0, 1);
		HbmAnimations.applyRelevantTransformation("Lever");
		GunGL.renderPart(WeaponResources.stg77, "Lever");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, bolt[2]);
		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("Breech");
		GunGL.renderPart(WeaponResources.stg77, "Breech");
		GunGL.popMatrix();
		GunGL.translate(0.125, 0, 0);
		GunGL.rotate(handle[2], 0, 0, 1);
		GunGL.translate(-0.125, 0, 0);
		HbmAnimations.applyRelevantTransformation("Handle");
		GunGL.renderPart(WeaponResources.stg77, "Handle");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(safety[0], 0, 0);
		HbmAnimations.applyRelevantTransformation("Safety");
		GunGL.renderPart(WeaponResources.stg77, "Safety");
		GunGL.popMatrix();

		GunGL.popMatrix();

		GunGL.pushMatrix();
		//GunGL.translate(2, 0.75, 0);
		//GunGL.rotate(15, 1, 0, 0);
		//GunGL.rotate(0, 0, 0, 1);

		GunGL.translate(inspectMove[0], inspectMove[1], inspectMove[2]);
		GunGL.rotate(inspectBarrel[0], 1, 0, 0);
		GunGL.rotate(inspectBarrel[2], 0, 0, 1);
		HbmAnimations.applyRelevantTransformation("Gun");
		HbmAnimations.applyRelevantTransformation("Barrel");
		GunGL.renderPart(WeaponResources.stg77, "Barrel");
		GunGL.popMatrix();

		double smokeScale = 0.75;

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 7.5);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(0.25, 0.25, 0.25);
		GunGL.rotate(-5 + gun.shotRand * 10, 1, 0, 0);
		this.renderGapFlash(gun.lastShot[0]);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 1, 2);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.375D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.5, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.stg77_tex);
		GunGL.renderPart(WeaponResources.stg77, "Gun");
		GunGL.renderPart(WeaponResources.stg77, "Barrel");
		GunGL.renderPart(WeaponResources.stg77, "Lever");
		GunGL.renderPart(WeaponResources.stg77, "Magazine");
		GunGL.renderPart(WeaponResources.stg77, "Safety");
		GunGL.renderPart(WeaponResources.stg77, "Handle");
		GunGL.renderPart(WeaponResources.stg77, "Breech");

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
			GunGL.translate(0, 0, 7.5);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.scale(0.25, 0.25, 0.25);
			GunGL.rotate(-5 + shotRand * 10, 1, 0, 0);
			this.renderGapFlash(shot);
			GunGL.popMatrix();
		}
	}
}
