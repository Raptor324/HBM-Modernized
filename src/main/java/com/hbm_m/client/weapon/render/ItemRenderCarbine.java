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

/** 1:1 {@code ItemRenderCarbine}. */
public class ItemRenderCarbine extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;

		if(this.isScoped(stack)) {
			standardAimingTransform(stack,
					-1.5F * offset, -1.5F * offset, 0.875F * offset,
					0, -8 / 8D, 0.25);
		} else {
			standardAimingTransform(stack,
					-1.5F * offset, -1.5F * offset, 0.875F * offset,
					0, -6.25 / 8D, 0.25);
		}
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		boolean isScoped = isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.carbine_tex);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] rel = HbmAnimations.getRelevantTransformation("REL");
		double[] stab = HbmAnimations.getRelevantTransformation("STAB");

		GunGL.translate(0, -1, -2);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 1, 2);

		GunGL.translate(0, 0, -2);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 0, 2);

		GunGL.translate(stab[0], stab[1], stab[2]);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.renderPart(WeaponResources.carbine, "Gun");

		GunGL.pushMatrix();
		GunGL.translate(0, 0, slide[2]);
		GunGL.renderPart(WeaponResources.carbine, "Slide");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);
		GunGL.renderPart(WeaponResources.carbine, "Magazine");
		GunGL.translate(rel[0], rel[1], rel[2]);
		if(bullet[0] != 1) GunGL.renderPart(WeaponResources.carbine, "Bullet");
		GunGL.popMatrix();

		if(!isScoped(stack)) {
			GunGL.renderPart(WeaponResources.carbine, "IronSight");
		} else {
			GunGL.bindTexture(WeaponResources.carbine_scope_tex);
			GunGL.renderPart(WeaponResources.carbine, "Scope");
		}

		if(hasBayonet(stack)) {
			GunGL.bindTexture(WeaponResources.carbine_bayonet_tex);
			GunGL.renderPart(WeaponResources.carbine, "Bayonet");
		}

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 8);
		GunGL.rotate(90, 0, 1, 0);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.25D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.5, 0.5, 0.5);
		renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.375D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0, 2);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(hasBayonet(stack)) {
			double scale = 1.1875D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
			GunGL.translate(1.5, 0, 0);
		} else {
			double scale = 1.375D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
			GunGL.translate(-0.5, 0, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 0, -1.75);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.carbine_tex);
		GunGL.renderPart(WeaponResources.carbine, "Gun");
		GunGL.renderPart(WeaponResources.carbine, "Slide");
		GunGL.renderPart(WeaponResources.carbine, "Magazine");
		if(!isScoped(stack)) {
			GunGL.renderPart(WeaponResources.carbine, "IronSight");
		} else {
			GunGL.bindTexture(WeaponResources.carbine_scope_tex);
			GunGL.renderPart(WeaponResources.carbine, "Scope");
		}
		if(hasBayonet(stack)) {
			GunGL.bindTexture(WeaponResources.carbine_bayonet_tex);
			GunGL.renderPart(WeaponResources.carbine, "Bayonet");
		}

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
			GunGL.translate(0, 1, 8);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(0.5, 0.5, 0.5);
			renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}

	public boolean isScoped(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}

	public boolean hasBayonet(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_CARBINE_BAYONET);
	}
}
