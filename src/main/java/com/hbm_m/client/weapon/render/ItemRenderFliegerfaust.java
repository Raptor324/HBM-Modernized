package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.world.item.ItemStack;

/** 1:1 ItemRenderFliegerfaust port from 1.7.10. */
public class ItemRenderFliegerfaust extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-2.75F * offset, -2F * offset, -1.5F * offset,
				-0.9375, -7.75 / 8D, -2.5);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.fliegerfaust_tex);
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
		double[] drum = HbmAnimations.getRelevantTransformation("DRUM");
		double[] drumrot = HbmAnimations.getRelevantTransformation("DRUMROT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] unloading = HbmAnimations.getRelevantTransformation("UNLOADING");

		int rocketNum = unloading[0] > 0 ? gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) : 9;

		GunGL.rotate(aimingProgress * -45, 0, 0, 1);

		GunGL.translate(0, 0, 1);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 0, -1);

		GunGL.translate(0, -6, -1);
		GunGL.rotate(reload[0], 1, 0, 0);
		GunGL.translate(0, 6, 1);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.renderPart(WeaponResources.fliegerfaust, "Launcher");

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 2);
		GunGL.translate(drum[0], drum[1], drum[2]);
		GunGL.translate(0, 0, -8.5);
		GunGL.rotate(drumrot[0], 1, 0, 0);
		GunGL.translate(0, 0, 8.5);
		GunGL.renderPart(WeaponResources.fliegerfaust, "Drum");
		for (int i = 1; i <= rocketNum; i++) GunGL.renderPart(WeaponResources.fliegerfaust, "Rocket" + i);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.75, 0.75, 0.75);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		setupThirdPerson(stack, 2D, 0, 1, -2);
	}

	@Override
	public void setupInv(ItemStack stack) {
		setupInv(stack, 1.375D, 0, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		setupModTable(stack, -8.5D, 0, 0, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();
		GunGL.bindTexture(WeaponResources.fliegerfaust_tex);

		GunGL.renderPart(WeaponResources.fliegerfaust, "Launcher");

		GunGL.translate(0, 0, 2);
		GunGL.renderPart(WeaponResources.fliegerfaust, "Drum");

		renderEquippedMuzzleFlash(type, stack, data, 0, 0, 8, 0.75D, 150, 7.5);
	}
}
