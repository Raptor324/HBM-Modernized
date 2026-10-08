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

/** 1:1 {@code ItemRenderMaresleg}. */
public class ItemRenderMaresleg extends ItemRenderWeaponBase {

	public ResourceLocation texture;

	public ItemRenderMaresleg(ResourceLocation texture) {
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
				0, -3.875 / 8D, 1);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(texture);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		boolean shortened = getShort(stack);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] flip = HbmAnimations.getRelevantTransformation("FLIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] shell = HbmAnimations.getRelevantTransformation("SHELL");
		double[] flag = HbmAnimations.getRelevantTransformation("FLAG");

		GunGL.translate(recoil[0] * 2, recoil[1], recoil[2]);
		GunGL.rotate(recoil[2] * 5, 1, 0, 0);
		GunGL.rotate(turn[2], 0, 0, 1);

		GunGL.translate(0, 0, -4);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 0, 4);

		GunGL.translate(0, 0, -4);
		GunGL.rotate(equip[0], -1, 0, 0);
		GunGL.translate(0, 0, 4);

		GunGL.translate(0, 0, -2);
		GunGL.rotate(flip[0], -1, 0, 0);
		GunGL.translate(0, 0, 2);

		GunGL.pushMatrix();
		GunGL.translate(0, 1, shortened ? 3.75 : 8);
		GunGL.rotate(turn[2], 0, 0, -1);
		GunGL.rotate(flip[0], 1, 0, 0);
		GunGL.rotate(90, 0, 1, 0);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.25D);
		GunGL.popMatrix();

		GunGL.renderPart(WeaponResources.maresleg, "Gun");
		if(!shortened) {
			GunGL.renderPart(WeaponResources.maresleg, "Stock");
			GunGL.renderPart(WeaponResources.maresleg, "Barrel");
		}

		GunGL.pushMatrix();
		GunGL.translate(0, 0.125, -2.875);
		GunGL.rotate(lever[0], 1, 0, 0);
		GunGL.translate(0, -0.125, 2.875);
		GunGL.renderPart(WeaponResources.maresleg, "Lever");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(shell[0], shell[1] - 0.75, shell[2]);
		GunGL.renderPart(WeaponResources.maresleg, "Shell");
		GunGL.popMatrix();

		if(flag[0] != 0) {
			GunGL.pushMatrix();
			GunGL.translate(0, -0.5, 0);
			GunGL.renderPart(WeaponResources.maresleg, "Shell");
			GunGL.popMatrix();
		}

		GunGL.pushMatrix();
		GunGL.translate(0, 1, shortened ? 3.75 : 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0.25, 3);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);

		if(getShort(stack)) {
			double scale = 2.5D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
			GunGL.translate(-1, 0, 0);
		} else {
			double scale = 1.4375D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
			GunGL.translate(-0.5, 0.5, 0);
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
		GunGL.renderPart(WeaponResources.maresleg, "Gun");
		GunGL.renderPart(WeaponResources.maresleg, "Lever");
		boolean shortened = getShort(stack);
		if(!shortened) {
			GunGL.renderPart(WeaponResources.maresleg, "Stock");
			GunGL.renderPart(WeaponResources.maresleg, "Barrel");
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
			GunGL.translate(0, 1, shortened ? 3.75 : 8);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 5);
			GunGL.popMatrix();
		}
	}

	public boolean getShort(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_maresleg_broken") || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SAWED_OFF);
	}
}
