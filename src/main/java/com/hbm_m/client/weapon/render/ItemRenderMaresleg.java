package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.weapon.WeaponJointRig;
import com.hbm_m.client.render.weapon.WeaponKinematics;
import com.hbm_m.client.render.weapon.WeaponRenderRegistry;
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

		WeaponRenderRegistry.renderPart(WeaponResources.maresleg, "Gun", GunGL.pose());
		if(!shortened) {
			WeaponRenderRegistry.renderPart(WeaponResources.maresleg, "Stock", GunGL.pose());
			WeaponRenderRegistry.renderPart(WeaponResources.maresleg, "Barrel", GunGL.pose());
		}

		WeaponJointRig rig = WeaponRenderRegistry.getRig(WeaponResources.maresleg);
		WeaponKinematics.renderPartWithJoint(rig, "Lever", GunGL.pose(), (float) lever[0]);
		WeaponKinematics.renderPartWithVector(rig, "Shell", GunGL.pose(), (float) shell[0], (float) (shell[1] - 0.75), (float) shell[2]);

		if(flag[0] != 0) {
			WeaponKinematics.renderPartWithVector(rig, "Shell", GunGL.pose(), 0f, -0.5f, 0f);
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
		setupThirdPerson(stack, 1.75D, 0, 0.25, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		if(getShort(stack)) {
			setupInv(stack, 2.5D, -1, 0, 0);
		} else {
			setupInv(stack, 1.4375D, -0.5, 0.5, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		setupModTable(stack, -8.75D, 0, 0, 0);
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
		renderEquippedMuzzleFlash(type, stack, data, 0, 1, shortened ? 3.75 : 8, 1.0D, 75, 5);
	}

	public boolean getShort(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_maresleg_broken") || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SAWED_OFF);
	}
}
