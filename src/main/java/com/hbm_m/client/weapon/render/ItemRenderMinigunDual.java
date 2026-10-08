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

/** 1:1 {@code ItemRenderMinigunDual}. */
public class ItemRenderMinigunDual extends ItemRenderWeaponBase {

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

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
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {
			int index = i == -1 ? 0 : 1;
			GunGL.bindTexture(WeaponResources.minigun_dual_tex);

			GunGL.pushMatrix();
			standardAimingTransform(stack, -2.75F * offset * i, -1.75F * offset, 2.5F * offset, 0, 0, 0);

			double scale = 0.375D;
			GunGL.scale(scale, scale, scale);

			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE", index);

			GunGL.translate(0, 3, -6);
			GunGL.rotate(equip[0], 1, 0, 0);
			GunGL.translate(0, -3, 6);

			GunGL.translate(0, 0, recoil[2]);

			GunGL.renderPart(WeaponResources.minigun, index == 0 ? "GunDual" : "Gun");

			GunGL.pushMatrix();
			GunGL.rotate(rotate[2] * i, 0, 0, 1);
			GunGL.renderPart(WeaponResources.minigun, "Barrels");
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 0, 12);
			GunGL.rotate(90, 0, 1, 0);

			GunGL.rotate(gun.shotRand * 90, 1, 0, 0);
			GunGL.scale(1.5, 1.5, 1.5);
			this.renderMuzzleFlash(gun.lastShot[index], 50, 7.5);
			GunGL.popMatrix();

			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(-1, -3.5, 8);

	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(2, -3.5, 8);
	}

	@Override
	public void setupInv(ItemStack stack) {
		GunGL.scale(1, 1, -1);
		GunGL.translate(8, 8, 0);
		double scale = 0.875D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
	}

	@Override
	public void renderEquipped(ItemStack stack, Object... data) {

		GunGL.bindTexture(WeaponResources.minigun_dual_tex);
		GunGL.renderPart(WeaponResources.minigun, "Gun");
		GunGL.renderPart(WeaponResources.minigun, "Barrels");

		LivingEntity ent = (LivingEntity) data[1];
		long shot;
		double shotRand = 0;
		if(ent == Minecraft.getInstance().player) {
			ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
			shot = gun.lastShot[1];
			shotRand = gun.shotRand;
		} else {
			shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
			if(shot < 0) return;
		}

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 12.25);
		GunGL.rotate(90, 0, 1, 0);

		GunGL.translate(0, 0.5, 0);
		GunGL.rotate(shotRand * 90, 1, 0, 0);
		GunGL.scale(1.5, 1.5, 1.5);
		this.renderMuzzleFlash(shot, 50, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {

		GunGL.bindTexture(WeaponResources.minigun_dual_tex);
		GunGL.renderPart(WeaponResources.minigun, "GunDual");
		GunGL.renderPart(WeaponResources.minigun, "Barrels");

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
		GunGL.translate(0, 0, 12.25);
		GunGL.rotate(90, 0, 1, 0);

		GunGL.translate(0, 0.5, 0);
		GunGL.rotate(shotRand * 90, 1, 0, 0);
		GunGL.scale(1.5, 1.5, 1.5);
		this.renderMuzzleFlash(shot, 50, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.minigun_dual_tex);
		GunGL.renderPart(WeaponResources.minigun, index == 0 ? "GunDual" : "Gun");
		GunGL.renderPart(WeaponResources.minigun, "Barrels");
	}

	@Override
	public void renderInv(ItemStack stack) {

		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.minigun_dual_tex);

		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.renderPart(WeaponResources.minigun, "GunDual");
		GunGL.renderPart(WeaponResources.minigun, "Barrels");
		GunGL.popMatrix();

		GunGL.translate(0, 0, 8);
		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.rotate(-90, 1, 0, 0);
		GunGL.rotate(-45, 0, 1, 0);
		GunGL.renderPart(WeaponResources.minigun, "Gun");
		GunGL.renderPart(WeaponResources.minigun, "Barrels");
		GunGL.popMatrix();

	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.minigun_dual_tex);
		GunGL.renderPart(WeaponResources.minigun, "Gun");
		GunGL.renderPart(WeaponResources.minigun, "Barrels");
	}

	public boolean hasSilencer(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
	}

	public boolean isSaturnite(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_UZI_SATURN);
	}
}
