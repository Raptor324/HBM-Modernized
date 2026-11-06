package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.weapon.WeaponJointRig;
import com.hbm_m.client.render.weapon.WeaponKinematics;
import com.hbm_m.client.render.weapon.WeaponRenderRegistry;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderUzi}. */
public class ItemRenderUzi extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		return standardZoomFov(fov, 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.75F * offset, -1.5F * offset, 2.5F * offset,
				0, -4.375 / 8D, 1);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(isSaturnite(stack) ? WeaponResources.uzi_saturnite_tex : WeaponResources.uzi_tex);
		double scale = 0.25D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] stockFront = HbmAnimations.getRelevantTransformation("STOCKFRONT");
		double[] stockBack = HbmAnimations.getRelevantTransformation("STOCKBACK");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");
		double[] yeet = HbmAnimations.getRelevantTransformation("YEET");
		double[] speen = HbmAnimations.getRelevantTransformation("SPEEN");

		GunGL.translate(yeet[0], yeet[1], yeet[2]);
		GunGL.rotate(speen[0], 0, 0, 1);

		GunGL.rotateAround(equip[0], 1, 0, 0, 0, -2, -4);
		GunGL.rotateAround(lift[0], 1, 0, 0, 0, 0, -6);

		GunGL.translate(0, 0, recoil[2]);

		WeaponRenderRegistry.renderPart(WeaponResources.uzi, "Gun", GunGL.pose());

		boolean silenced = hasSilencer(stack, 0);
		if(silenced) WeaponRenderRegistry.renderPart(WeaponResources.uzi, "Silencer", GunGL.pose());

		WeaponJointRig rig = WeaponRenderRegistry.getRig(WeaponResources.uzi);

		WeaponKinematics.scoped(GunGL.pose(), ps -> {
			WeaponKinematics.applyJoint(ps, rig.getJoint("StockFront"), (float) (180 - stockFront[0]));
			WeaponRenderRegistry.renderPart(WeaponResources.uzi, "StockFront", ps);
			WeaponKinematics.applyJoint(ps, rig.getJoint("StockBack"), (float) (-200 - stockBack[0]));
			WeaponRenderRegistry.renderPart(WeaponResources.uzi, "StockBack", ps);
		});

		WeaponKinematics.renderPartWithJoint(rig, "Slide", GunGL.pose(), (float) slide[2]);

		WeaponKinematics.scoped(GunGL.pose(), ps -> {
			WeaponKinematics.applyVectorTranslate(ps, (float) mag[0], (float) mag[1], (float) mag[2]);
			WeaponRenderRegistry.renderPart(WeaponResources.uzi, "Magazine", ps);
			if(bullet[0] == 1) WeaponRenderRegistry.renderPart(WeaponResources.uzi, "Bullet", ps);
		});

		if(!silenced) {
			double smokeScale = 0.5;

			GunGL.pushMatrix();
			GunGL.translate(0, 0.75, 8.5);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.scale(smokeScale, smokeScale, smokeScale);
			this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.75D);
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 0.75, 8.5);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
			this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		setupThirdPerson(stack, 1.0D, 0, 1, 1);
	}

	@Override
	public void setupInv(ItemStack stack) {
		setupInv(stack, 1.5D, 0, 1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		setupModTable(stack, -6.25D, 0, 1, -4);
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		GunGL.enableLighting();
		GunGL.bindTexture(isSaturnite(stack) ? WeaponResources.uzi_saturnite_tex : WeaponResources.uzi_tex);
		WeaponKinematics.renderRestParts(WeaponRenderRegistry.getRig(WeaponResources.uzi), 1, GunGL.pose(),
				hasSilencer(stack, index) ? "Silencer" : null);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();
		boolean silenced = hasSilencer(stack, 0);
		if(silenced && type == ItemRenderType.INVENTORY) {
			double scale = 0.625D;
			GunGL.scale(scale, scale, scale);
			GunGL.translate(0, 0, -4);
		}
		GunGL.bindTexture(isSaturnite(stack) ? WeaponResources.uzi_saturnite_tex : WeaponResources.uzi_tex);
		WeaponKinematics.renderRestParts(WeaponRenderRegistry.getRig(WeaponResources.uzi), 1, GunGL.pose(),
				silenced ? "Silencer" : null);
		if(!silenced) {
			renderEquippedMuzzleFlash(type, stack, data, 0, 0.75, 8.5, 1.0D, 75, 7.5);
		}
	}

	public boolean hasSilencer(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
	}

	public boolean isSaturnite(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_UZI_SATURN);
	}
}
