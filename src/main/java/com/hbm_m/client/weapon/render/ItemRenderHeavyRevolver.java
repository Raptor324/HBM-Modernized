package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderHeavyRevolver}. */
public class ItemRenderHeavyRevolver extends ItemRenderWeaponBase {

	protected ResourceLocation texture;

	public ItemRenderHeavyRevolver(ResourceLocation texture) {
		this.texture = texture;
	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 1);

		boolean isScoped = this.isScoped(stack);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.0F * offset, -0.75F * offset, 1F * offset,
				0, isScoped ? (-4.75 / 8D) : (-3.875 / 8D), isScoped ? -0.25 : 0);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		boolean isScoped = this.isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		double scale = 0.125D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);


		double[] equipSpin = HbmAnimations.getRelevantTransformation("ROTATE");
		double[] spin = HbmAnimations.getRelevantTransformation("SPIN");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] reloadLift = HbmAnimations.getRelevantTransformation("RELOAD_LIFT");
		double[] reloadJolt = HbmAnimations.getRelevantTransformation("RELOAD_JOLT");
		double[] reloadTilt = HbmAnimations.getRelevantTransformation("RELAOD_TILT");
		double[] cylinderFlip = HbmAnimations.getRelevantTransformation("RELOAD_CYLINDER");
		double[] reloadBullets = HbmAnimations.getRelevantTransformation("RELOAD_BULLETS");

		GunGL.rotate(spin[0], 0, 0, 1);

		GunGL.translate(6, -3, 0);
		GunGL.rotate(equipSpin[0], 0, 0, 1);
		GunGL.translate(-6, 3, 0);

		standardAimingTransform(stack, 0, 0, recoil[2], -recoil[2], 0, 0);
		GunGL.rotate(recoil[2] * 10, 0, 0, 1);

		GunGL.pushMatrix();
		GunGL.translate(-9, 2.5, 0);
		GunGL.rotate(recoil[2] * -10, 0, 0, 1);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		GunGL.popMatrix();

		GunGL.rotate(reloadLift[0], 0, 0, 1);
		GunGL.translate(reloadJolt[0], 0, 0);
		GunGL.rotate(reloadTilt[0], 1, 0, 0);

		GunGL.bindTexture(texture);
		GunGL.renderPart(WeaponResources.lilmac, "Gun");

		GunGL.pushMatrix();
		GunGL.rotate(cylinderFlip[0], 1, 0, 0);
		GunGL.renderPart(WeaponResources.lilmac, "Pivot");
		GunGL.translate(0, 1.75, 0);
		GunGL.rotate(HbmAnimations.getRelevantTransformation("DRUM")[2] * -60, 1, 0, 0);
		GunGL.translate(0, -1.75, 0);
		GunGL.renderPart(WeaponResources.lilmac, "Cylinder");
		GunGL.translate(reloadBullets[0], reloadBullets[1], reloadBullets[2]);
		if(HbmAnimations.getRelevantTransformation("RELOAD_BULLETS_CON")[0] != 1)
		GunGL.renderPart(WeaponResources.lilmac, "Bullets");
		GunGL.renderPart(WeaponResources.lilmac, "Casings");
		GunGL.popMatrix();

		GunGL.pushMatrix(); /// HAMMER ///
		GunGL.translate(4, 1.25, 0);
		GunGL.rotate(-30 + 30 * HbmAnimations.getRelevantTransformation("HAMMER")[2], 0, 0, 1);
		GunGL.translate(-4, -1.25, 0);
		GunGL.renderPart(WeaponResources.lilmac, "Hammer");
		GunGL.popMatrix();

		if(isScoped) {
			GunGL.bindTexture(WeaponResources.lilmac_scope_tex);
			GunGL.renderPart(WeaponResources.lilmac, "Scope");
		}

		GunGL.pushMatrix();
		GunGL.translate(0.125, 2.5, 0);
		renderGapFlash(gun.lastShot[0]);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.scale(0.75, 0.75, 0.75);
		GunGL.translate(0, 1, 3);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(isScoped(stack)) {
			double scale = 1.125D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
			GunGL.translate(0, -0.5, 0);
		} else {
			double scale = 1.25D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {

		GunGL.rotate(90, 0, 1, 0);
		GunGL.enableLighting();

		GunGL.bindTexture(texture);
		GunGL.renderPart(WeaponResources.lilmac, "Gun");
		GunGL.renderPart(WeaponResources.lilmac, "Cylinder");
		GunGL.renderPart(WeaponResources.lilmac, "Bullets");
		GunGL.renderPart(WeaponResources.lilmac, "Casings");
		GunGL.renderPart(WeaponResources.lilmac, "Pivot");
		GunGL.renderPart(WeaponResources.lilmac, "Hammer");
		if(isScoped(stack)) {
			GunGL.bindTexture(WeaponResources.lilmac_scope_tex);
			GunGL.renderPart(WeaponResources.lilmac, "Scope");
		}

		if(type == ItemRenderType.EQUIPPED) {
			LivingEntity ent = (LivingEntity) data[1];
			long shot;
			if(ent == Minecraft.getInstance().player) {
				ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
				shot = gun.lastShot[0];
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			GunGL.pushMatrix();
			GunGL.translate(0.125, 2.5, 0);
			renderGapFlash(shot);
			GunGL.popMatrix();
		}
	}

	public boolean isScoped(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_heavy_revolver_lilmac") || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}
}
