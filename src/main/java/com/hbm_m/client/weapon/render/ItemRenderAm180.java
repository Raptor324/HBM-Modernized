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

/** 1:1 {@code ItemRenderAm180}. */
public class ItemRenderAm180 extends ItemRenderWeaponBase {

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
				-1F * offset, -1F * offset, 1F * offset,
				0, -4.1875 / 8D, 0.25);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.am180_tex);
		double scale = 0.1875D;
		GunGL.scale(scale, scale, scale);

		boolean silenced = this.hasSilencer(stack);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] magazine = HbmAnimations.getRelevantTransformation("MAG");
		double[] magTurn = HbmAnimations.getRelevantTransformation("MAGTURN");
		double[] magSpin = HbmAnimations.getRelevantTransformation("MAGSPIN");
		double[] bolt = HbmAnimations.getRelevantTransformation("BOLT");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");

		GunGL.translate(0, -2, -6);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 2, 6);

		GunGL.rotate(turn[2], 0, 0, 1);

		GunGL.translate(0, 0, recoil[2]);

		HbmAnimations.applyRelevantTransformation("Gun");
		GunGL.renderPart(WeaponResources.am180, "Gun");
		if(silenced) GunGL.renderPart(WeaponResources.am180, "Silencer");

		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("Trigger");
		GunGL.renderPart(WeaponResources.am180, "Trigger");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, bolt[2]);
		HbmAnimations.applyRelevantTransformation("Bolt");
		GunGL.renderPart(WeaponResources.am180, "Bolt");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(magazine[0], magazine[1], magazine[2]);

		GunGL.translate(0, 2.0625, 3.75);
		GunGL.rotate(magTurn[0], 1, 0, 0);
		GunGL.rotate(magTurn[2], 0, 0, 1);
		GunGL.translate(0, -2.0625, -3.75);

		GunGL.translate(0, 2.3125, 1.5);
		GunGL.rotate(magSpin[0], 1, 0, 0);
		GunGL.translate(0, -2.3125, -1.5);

		HbmAnimations.applyRelevantTransformation("Mag");

		GunGL.pushMatrix();
		int mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, RenderHelperA.meInventory());
		GunGL.translate(0, 0, 1.5);
		GunGL.rotate(mag / 59D * 360D, 0, -1, 0);
		GunGL.translate(0, 0, -1.5);
		GunGL.renderPart(WeaponResources.am180, "Mag");
		GunGL.popMatrix();

		GunGL.renderPart(WeaponResources.am180, "MagPlate");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1.875, silenced ? 17 : 13);
		GunGL.rotate(turn[2], 0, 0, -1);
		GunGL.rotate(90, 0, 1, 0);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.25D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1.875, silenced ? 16.75 : 12);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		double flashScale = silenced ? 0.5 : 0.75;
		GunGL.scale(flashScale, flashScale, flashScale);
		renderMuzzleFlash(gun.lastShot[0], silenced ? 75 : 50, silenced ? 5 : 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, -0.5, 3);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(1.5, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 0, -2);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.am180_tex);
		GunGL.renderPart(WeaponResources.am180, "Gun");
		if(this.hasSilencer(stack)) GunGL.renderPart(WeaponResources.am180, "Silencer");
		GunGL.renderPart(WeaponResources.am180, "Trigger");
		GunGL.renderPart(WeaponResources.am180, "Bolt");
		GunGL.renderPart(WeaponResources.am180, "Mag");
		GunGL.renderPart(WeaponResources.am180, "MagPlate");

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
			boolean silenced = this.hasSilencer(stack);

			GunGL.pushMatrix();
			GunGL.translate(0, 1.875, silenced ? 16.75 : 12);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			double flashScale = silenced ? 0.5 : 0.75;
			GunGL.scale(flashScale, flashScale, flashScale);
			renderMuzzleFlash(shot, silenced ? 75 : 50, silenced ? 5 : 7.5);
			GunGL.popMatrix();
		}
	}

	public boolean hasSilencer(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
	}
}
