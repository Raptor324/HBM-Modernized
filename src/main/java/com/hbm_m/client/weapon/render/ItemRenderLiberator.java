package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderLiberator}. */
public class ItemRenderLiberator extends ItemRenderWeaponBase {

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
				-1.5F * offset, -1.25F * offset, 1.25F * offset,
				0, -4.625 / 8D, 0.25);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.liberator_tex);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] latch = HbmAnimations.getRelevantTransformation("LATCH");
		double[] brk = HbmAnimations.getRelevantTransformation("BREAK");
		double[] shell1 = HbmAnimations.getRelevantTransformation("SHELL1");
		double[] shell2 = HbmAnimations.getRelevantTransformation("SHELL2");
		double[] shell3 = HbmAnimations.getRelevantTransformation("SHELL3");
		double[] shell4 = HbmAnimations.getRelevantTransformation("SHELL4");

		GunGL.translate(0, -1, -3);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 1, 3);

		GunGL.translate(0, -3, -3);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 3, 3);

		GunGL.translate(recoil[0] * 2, recoil[1], recoil[2]);
		GunGL.rotate(recoil[2] * 10, 1, 0, 0);

		GunGL.renderPart(WeaponResources.liberator, "Gun");

		GunGL.pushMatrix();

		GunGL.translate(0, -0.5, 0.75);
		GunGL.rotate(brk[0], 1, 0, 0);
		GunGL.translate(0, 0.5, -0.75);
		GunGL.renderPart(WeaponResources.liberator, "Barrel");

		GunGL.pushMatrix();
		GunGL.translate(shell1[0], shell1[1], shell1[2]);
		GunGL.renderPart(WeaponResources.liberator, "Shell1");
		GunGL.popMatrix();
		GunGL.pushMatrix();
		GunGL.translate(shell2[0], shell2[1], shell2[2]);
		GunGL.renderPart(WeaponResources.liberator, "Shell2");
		GunGL.popMatrix();
		GunGL.pushMatrix();
		GunGL.translate(shell3[0], shell3[1], shell3[2]);
		GunGL.renderPart(WeaponResources.liberator, "Shell3");
		GunGL.popMatrix();
		GunGL.pushMatrix();
		GunGL.translate(shell4[0], shell4[1], shell4[2]);
		GunGL.renderPart(WeaponResources.liberator, "Shell4");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1.15625, 0.75);
		GunGL.rotate(latch[0], 1, 0, 0);
		GunGL.translate(0, -1.15625, -0.75);
		GunGL.renderPart(WeaponResources.liberator, "Latch");
		GunGL.popMatrix();
		GunGL.popMatrix();

		double smokeScale = 0.375;

		GunConfig cfg = gun.getConfig(stack, 0);

		GunGL.pushMatrix();
		GunGL.translate(0, 0.25, 7.25);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		GunGL.translate(0, 0, 0.25 / smokeScale);
		this.renderSmokeNodes(cfg.smokeNodes, 1D);
		GunGL.translate(0, 0, -0.5 / smokeScale);
		this.renderSmokeNodes(cfg.smokeNodes, 1D);
		GunGL.translate(0, 0.5 / smokeScale, 0);
		this.renderSmokeNodes(cfg.smokeNodes, 1D);
		GunGL.translate(0, 0, 0.5 / smokeScale);
		this.renderSmokeNodes(cfg.smokeNodes, 1D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0.5, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(1.5, 1.5, 1.5);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 5);
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
		GunGL.translate(-0.5, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.liberator_tex);
		GunGL.renderAll(WeaponResources.liberator);

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
			GunGL.translate(0, 0.5, 8);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(1.5, 1.5, 1.5);
			this.renderMuzzleFlash(shot, 75, 5);
			GunGL.popMatrix();
		}
	}
}
