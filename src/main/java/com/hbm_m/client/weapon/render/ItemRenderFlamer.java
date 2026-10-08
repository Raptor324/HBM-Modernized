package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderFlamer}. */
public class ItemRenderFlamer extends ItemRenderWeaponBase {

	public ResourceLocation texture;

	public ItemRenderFlamer(ResourceLocation texture) {
		this.texture = texture;
	}

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
				-1.5F * offset, -1.5F * offset, 2.75F * offset,
				0, -4.625 / 8D, 0.25);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(texture);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE");

		GunGL.translate(0, 2, -6);
		GunGL.rotate(-equip[0], 1, 0, 0);
		GunGL.translate(0, -2, 6);

		GunGL.translate(0, 1, 0);
		GunGL.rotate(rotate[2], 0, 0, 1);
		GunGL.translate(0, -1, 0);

		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("Gun");
		GunGL.renderPart(WeaponResources.flamethrower, "Gun");
		if(hasShield(stack)) GunGL.renderPart(WeaponResources.flamethrower, "HeatShield");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("Tank");
		GunGL.renderPart(WeaponResources.flamethrower, "Tank");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("Gauge");
		GunGL.translate(1.25, 1.25, 0);
		IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
		GunGL.rotate(-135 + (mag.getAmount(stack, RenderHelperA.meInventory()) * 270D / mag.getCapacity(stack)), 0, 0, 1);
		GunGL.translate(-1.25, -1.25, 0);
		GunGL.renderPart(WeaponResources.flamethrower, "Gauge");
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, -3, 4);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-1, 1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 0, 1);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(texture);
		GunGL.renderPart(WeaponResources.flamethrower, "Gun");
		GunGL.renderPart(WeaponResources.flamethrower, "Tank");
		GunGL.renderPart(WeaponResources.flamethrower, "Gauge");
		if(hasShield(stack)) GunGL.renderPart(WeaponResources.flamethrower, "HeatShield");
	}

	public boolean hasShield(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_flamer_daybreaker");
	}
}
