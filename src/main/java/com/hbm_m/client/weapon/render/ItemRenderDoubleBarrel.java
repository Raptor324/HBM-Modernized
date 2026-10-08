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

/** 1:1 {@code ItemRenderDoubleBarrel}. */
public class ItemRenderDoubleBarrel extends ItemRenderWeaponBase {

	protected ResourceLocation texture;

	public ItemRenderDoubleBarrel(ResourceLocation texture) {
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
				-1.25F * offset, -1F * offset, 2F * offset,
				0, -2 / 8D, 1);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(texture);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] shells = HbmAnimations.getRelevantTransformation("SHELLS");
		double[] shellFlip = HbmAnimations.getRelevantTransformation("SHELL_FLIP");
		double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
		double[] buckle = HbmAnimations.getRelevantTransformation("BUCKLE");
		double[] no_ammo = HbmAnimations.getRelevantTransformation("NO_AMMO");

		GunGL.translate(recoil[0] * 3, recoil[1], recoil[2]);
		GunGL.rotate(recoil[2] * 10, 1, 0, 0);

		GunGL.translate(0, 0, -4);
		GunGL.rotate(equip[0], -1, 0, 0);
		GunGL.translate(0, 0, 4);

		GunGL.translate(0, 0, -4);
		GunGL.rotate(turn[1], 0, 1, 0);
		GunGL.translate(0, 0, 4);

		GunGL.translate(0, 0, -4);
		GunGL.rotate(lift[0], -1, 0, 0);
		GunGL.translate(0, 0, 4);

		GunGL.renderPart(WeaponResources.double_barrel, "Stock");

		GunGL.pushMatrix();

		GunGL.translate(0, -0.4375, -0.875);
		GunGL.rotate(barrel[0], 1, 0, 0);
		GunGL.translate(0, 0.4375, 0.875);

		GunGL.renderPart(WeaponResources.double_barrel, "BarrelShort");
		if(!isSawedOff(stack)) GunGL.renderPart(WeaponResources.double_barrel, "Barrel");

		GunGL.pushMatrix();
		GunGL.translate(0.75, 0, -0.6875);
		GunGL.rotate(buckle[1], 0, 1, 0);
		GunGL.translate(-0.75, 0, 0.6875);
		GunGL.renderPart(WeaponResources.double_barrel, "Buckle");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(-0.3125, 0.3125, 0);
		GunGL.rotate(lever[2], 0, 0, 1);
		GunGL.translate(0.3125, -0.3125, 0);
		GunGL.renderPart(WeaponResources.double_barrel, "Lever");
		GunGL.popMatrix();

		if(no_ammo[0] == 0) {
			GunGL.pushMatrix();
			GunGL.translate(shells[0], shells[1], shells[2]);
			GunGL.translate(0, 0, -1);
			GunGL.rotate(shellFlip[0], 1, 0, 0);
			GunGL.translate(0, 0, 1);
			GunGL.renderPart(WeaponResources.double_barrel, "Shells");
			GunGL.popMatrix();
		}

		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(2, 2, 2);
		renderMuzzleFlash(gun.lastShot[0], 75, 5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 1, 3);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(isSawedOff(stack)) {
			double scale = 2D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
			GunGL.translate(-2, 0.5, 0);
		} else {
			double scale = 1.375D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
			GunGL.translate(0, 0.5, 0);
		}
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

		GunGL.bindTexture(texture);
		GunGL.renderPart(WeaponResources.double_barrel, "Stock");
		GunGL.renderPart(WeaponResources.double_barrel, "BarrelShort");
		if(!isSawedOff(stack)) GunGL.renderPart(WeaponResources.double_barrel, "Barrel");
		GunGL.renderPart(WeaponResources.double_barrel, "Buckle");
		GunGL.renderPart(WeaponResources.double_barrel, "Lever");
		GunGL.renderPart(WeaponResources.double_barrel, "Shells");

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
			GunGL.translate(0, 0, 8);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(2, 2, 2);
			renderMuzzleFlash(shot, 75, 5);
			GunGL.popMatrix();
		}
	}

	public boolean isSawedOff(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_double_barrel_sacred_dragon") || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SAWED_OFF);
	}
}
