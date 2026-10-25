package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderChemthrower}. */
public class ItemRenderChemthrower extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-2.5F * offset, -2.5F * offset, 2.5F * offset,
				0, -4.375 / 8D, 1);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.chemthrower_tex);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");

		GunGL.translate(0, -2, -4);
		GunGL.rotate(equip[0], -1, 0, 0);
		GunGL.translate(0, 2, 4);

		GunGL.rotate(90, 0, 1, 0);
		GunGL.renderPart(WeaponResources.chemthrower, "Gun");
		GunGL.renderPart(WeaponResources.chemthrower, "Hose");
		GunGL.renderPart(WeaponResources.chemthrower, "Nozzle");

		GunGL.translate(0, 0.875, 1.75);
		IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		double d = (double) mag.getAmount(stack, RenderHelperA.meInventory()) / (double) mag.getCapacity(stack);
		GunGL.rotate(135 - d * 270, 1, 0, 0);
		GunGL.translate(0, -0.875, -1.75);

		GunGL.renderPart(WeaponResources.chemthrower, "Gauge");
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, -2.5, 0.5);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0.875, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.5, -0.5);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.rotate(90, 0, 1, 0);
		GunGL.bindTexture(WeaponResources.chemthrower_tex);
		GunGL.renderPart(WeaponResources.chemthrower, "Gun");
		GunGL.renderPart(WeaponResources.chemthrower, "Hose");
		GunGL.renderPart(WeaponResources.chemthrower, "Nozzle");
		GunGL.renderPart(WeaponResources.chemthrower, "Gauge");
	}
}
