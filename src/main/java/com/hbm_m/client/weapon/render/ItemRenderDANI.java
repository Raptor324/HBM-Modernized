package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderDANI}. */
public class ItemRenderDANI extends ItemRenderWeaponBase {

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

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
			GunGL.bindTexture(index == 0 ? WeaponResources.dani_celestial_tex : WeaponResources.dani_lunar_tex);

			GunGL.pushMatrix();

			standardAimingTransform(stack,
					-1.5F * offset * i, -0.75F * offset, 1F * offset,
					0, -3.125 / 8D, 0.25);

			double scale = 0.125D;
			GunGL.scale(scale, scale, scale);

			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] reloadMove = HbmAnimations.getRelevantTransformation("RELOAD_MOVE", index);
			double[] reloadRot = HbmAnimations.getRelevantTransformation("RELOAD_ROT", index);
			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);

			GunGL.translate(recoil[0], recoil[1], recoil[2]);
			GunGL.rotate(recoil[2] * 10, 1, 0, 0);

			GunGL.translate(0, -2, -2);
			GunGL.rotate(equip[0], -1, 0, 0);
			GunGL.translate(0, 2, 2);

			GunGL.pushMatrix();
			GunGL.translate(0, 1.5, 9.25);
			GunGL.rotate(-recoil[2] * 10, 1, 0, 0);
			GunGL.rotate(90, 0, 1, 0);
			renderSmokeNodes(gun.getConfig(stack, index).smokeNodes, 0.5D);
			GunGL.popMatrix();

			GunGL.translate(reloadMove[0], reloadMove[1], reloadMove[2]);

			GunGL.rotate(reloadRot[0], 1, 0, 0);
			GunGL.rotate(reloadRot[2] * i, 0, 0, 1);
			GunGL.rotate(reloadRot[1] * i, 0, 1, 0);
			GunGL.renderPart(WeaponResources.bio_revolver, "Grip");

			GunGL.pushMatrix(); /// FRONT PUSH ///
			GunGL.rotate(HbmAnimations.getRelevantTransformation("FRONT", index)[2], 1, 0, 0);
			GunGL.renderPart(WeaponResources.bio_revolver, "Barrel");
			GunGL.pushMatrix(); /// LATCH PUSH ///
			GunGL.translate(0, 2.3125, -0.875);
			GunGL.rotate(HbmAnimations.getRelevantTransformation("LATCH", index)[2], 1, 0, 0);
			GunGL.translate(0, -2.3125, 0.875);
			GunGL.renderPart(WeaponResources.bio_revolver, "Latch");
			GunGL.popMatrix(); /// LATCH POP ///

			GunGL.pushMatrix(); /// DRUM PUSH ///
			GunGL.translate(0, 1, 0);
			GunGL.rotate(HbmAnimations.getRelevantTransformation("DRUM", index)[2] * 60, 0, 0, 1);
			GunGL.translate(0, -1, 0);
			GunGL.translate(0, 0, HbmAnimations.getRelevantTransformation("DRUM_PUSH", index)[2]);
			GunGL.renderPart(WeaponResources.bio_revolver, "Drum");
			GunGL.popMatrix(); /// DRUM POP ///

			GunGL.popMatrix(); /// FRONT POP ///

			GunGL.pushMatrix(); /// HAMMER ///
			GunGL.translate(0, 0, -4.5);
			GunGL.rotate(-45 + 45 * HbmAnimations.getRelevantTransformation("HAMMER", index)[2], 1, 0, 0);
			GunGL.translate(0, 0, 4.5);
			GunGL.renderPart(WeaponResources.bio_revolver, "Hammer");
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 1.5, 9.25);
			GunGL.rotate(90, 0, 1, 0);
			renderMuzzleFlash(gun.lastShot[index], 75, 7.5);
			GunGL.popMatrix();

			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, 1, 3);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPersonAkimbo(stack);
		GunGL.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		GunGL.scale(1, 1, -1);
		GunGL.translate(8, 6, 0);
		double scale = 1.125D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 1.5, 0);
	}

	@Override
	public void renderInv(ItemStack stack) {

		GunGL.enableLighting();

		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(2, 0, 0);
		GunGL.bindTexture(WeaponResources.dani_celestial_tex);
		GunGL.renderAll(WeaponResources.bio_revolver);
		GunGL.popMatrix();

		GunGL.translate(0, 0, 5);
		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.rotate(-90, 1, 0, 0);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(-45, 0, 1, 0);
		GunGL.translate(-2, 0, 0);
		GunGL.bindTexture(WeaponResources.dani_lunar_tex);
		GunGL.renderAll(WeaponResources.bio_revolver);
		GunGL.popMatrix();
	}

	@Override
	public void renderEquipped(ItemStack stack, Object... data) {

		GunGL.bindTexture(WeaponResources.dani_lunar_tex);
		GunGL.renderAll(WeaponResources.bio_revolver);

		//Stopgap: at the moment, the flashMap is gun agnostic and does not care about index.
		LivingEntity ent = (LivingEntity) data[1]; //Entity is second obj. passed
		long shot;
		if(ent == Minecraft.getInstance().player) {
			ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
			shot = gun.lastShot[1];
		} else {
			shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
			if(shot < 0) return;
		}

		GunGL.pushMatrix();
		GunGL.translate(0, 1.5, 9.25);
		GunGL.rotate(90, 0, 1, 0);
		renderMuzzleFlash(shot, 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {

		GunGL.bindTexture(WeaponResources.dani_celestial_tex);
		GunGL.renderAll(WeaponResources.bio_revolver);

		//EntityPlayer is first & only object passed
		long shot;
		if(ent == Minecraft.getInstance().player) {
			ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
			shot = gun.lastShot[0];
		} else {
			shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
			if(shot < 0) return;
		}

		GunGL.pushMatrix();
		GunGL.translate(0, 1.5, 9.25);
		GunGL.rotate(90, 0, 1, 0);
		renderMuzzleFlash(shot, 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		GunGL.enableLighting();

		GunGL.bindTexture(index == 1 ? WeaponResources.dani_celestial_tex : WeaponResources.dani_lunar_tex);
		GunGL.renderAll(WeaponResources.bio_revolver);
	}

	@Override
	public void renderEntity(ItemStack stack) {
		GunGL.enableLighting();

		GunGL.pushMatrix();

		GunGL.translate(-2, 1, 0);
		GunGL.bindTexture(WeaponResources.dani_lunar_tex);
		GunGL.renderAll(WeaponResources.bio_revolver);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(2, 1, 0);
		GunGL.bindTexture(WeaponResources.dani_celestial_tex);
		GunGL.renderAll(WeaponResources.bio_revolver);
		GunGL.popMatrix();
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {

		GunGL.bindTexture(WeaponResources.dani_celestial_tex);
		GunGL.renderAll(WeaponResources.bio_revolver);
	}
}
