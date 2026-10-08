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

/** 1:1 {@code ItemRenderUzi}. */
public class ItemRenderUzi extends ItemRenderWeaponBase {

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

		GunGL.translate(0, -2, -4);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 2, 4);

		GunGL.translate(0, 0, -6);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 0, 6);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.renderPart(WeaponResources.uzi, "Gun");

		boolean silenced = hasSilencer(stack, 0);
		if(silenced) GunGL.renderPart(WeaponResources.uzi, "Silencer");

		GunGL.pushMatrix();
		GunGL.translate(0, 0.3125D, -5.75);
		GunGL.rotate(180 - stockFront[0], 1, 0, 0);
		GunGL.translate(0, -0.3125D, 5.75);
		GunGL.renderPart(WeaponResources.uzi, "StockFront");

		GunGL.translate(0, -0.3125D, -3);
		GunGL.rotate(-200 - stockBack[0], 1, 0, 0);
		GunGL.translate(0, 0.3125D, 3);
		GunGL.renderPart(WeaponResources.uzi, "StockBack");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, slide[2]);
		GunGL.renderPart(WeaponResources.uzi, "Slide");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);
		GunGL.renderPart(WeaponResources.uzi, "Magazine");
		if(bullet[0] == 1) GunGL.renderPart(WeaponResources.uzi, "Bullet");
		GunGL.popMatrix();

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
		GunGL.translate(0, 1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 1, -4);
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		GunGL.enableLighting();

		GunGL.bindTexture(isSaturnite(stack) ? WeaponResources.uzi_saturnite_tex : WeaponResources.uzi_tex);
		GunGL.renderPart(WeaponResources.uzi, "Gun");
		GunGL.renderPart(WeaponResources.uzi, "StockBack");
		GunGL.renderPart(WeaponResources.uzi, "StockFront");
		GunGL.renderPart(WeaponResources.uzi, "Slide");
		GunGL.renderPart(WeaponResources.uzi, "Magazine");
		if(hasSilencer(stack, index)) GunGL.renderPart(WeaponResources.uzi, "Silencer");
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
		GunGL.renderPart(WeaponResources.uzi, "Gun");
		GunGL.renderPart(WeaponResources.uzi, "StockBack");
		GunGL.renderPart(WeaponResources.uzi, "StockFront");
		GunGL.renderPart(WeaponResources.uzi, "Slide");
		GunGL.renderPart(WeaponResources.uzi, "Magazine");
		if(silenced) GunGL.renderPart(WeaponResources.uzi, "Silencer");

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
			GunGL.translate(0, 0.75, 8.5);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}

	public boolean hasSilencer(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
	}

	public boolean isSaturnite(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_UZI_SATURN);
	}
}
