package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderPepperbox}. */
public class ItemRenderPepperbox extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 1.5);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.25F * offset, -0.75F * offset, 1F * offset,
				0, -2.5 / 8D, 0.5);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		double scale = 0.25D;
		GunGL.scale(scale, scale, scale);

		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] cylinder = HbmAnimations.getRelevantTransformation("ROTATE");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] trigger = HbmAnimations.getRelevantTransformation("TRIGGER");
		double[] translate = HbmAnimations.getRelevantTransformation("TRANSLATE");
		double[] loader = HbmAnimations.getRelevantTransformation("LOADER");
		double[] shot = HbmAnimations.getRelevantTransformation("SHOT");

		GunGL.translate(translate[0], translate[1], translate[2]);

		GunGL.translate(0, 0, -5);
		GunGL.rotate(recoil[0], -1, 0, 0);
		GunGL.translate(0, 0, 5);

		GunGL.pushMatrix();
		GunGL.translate(0, 0.5, 7);
		GunGL.rotate(90, 0, 1, 0);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		GunGL.popMatrix();

		GunGL.bindTexture(WeaponResources.pepperbox_tex);

		if(loader[0] != 0 || loader[1] != 0 || loader[2] != 0) {
			GunGL.pushMatrix();
			GunGL.translate(loader[0], loader[1], loader[2]);
			GunGL.renderPart(WeaponResources.pepperbox, "Speedloader");
			if(shot[0] != 0) GunGL.renderPart(WeaponResources.pepperbox, "Shot");
			GunGL.popMatrix();
		}

		GunGL.renderPart(WeaponResources.pepperbox, "Grip");

		GunGL.pushMatrix();
		GunGL.rotate(cylinder[0], 0, 0, 1);
		GunGL.renderPart(WeaponResources.pepperbox, "Cylinder");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0.375, -1.875);
		GunGL.rotate(hammer[0], 1, 0, 0);
		GunGL.translate(0, -0.375, 1.875);
		GunGL.renderPart(WeaponResources.pepperbox, "Hammer");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, -trigger[0] * 0.5);
		GunGL.renderPart(WeaponResources.pepperbox, "Trigger");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0.5, 7);
		GunGL.scale(0.5, 0.5, 0.5);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0]);
		GunGL.rotate(45, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0]);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, 1, 3);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0.5, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {

		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.pepperbox_tex);
		GunGL.renderPart(WeaponResources.pepperbox, "Grip");
		GunGL.renderPart(WeaponResources.pepperbox, "Cylinder");
		GunGL.renderPart(WeaponResources.pepperbox, "Hammer");
		GunGL.renderPart(WeaponResources.pepperbox, "Trigger");

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
			GunGL.translate(0, 0.5, 7);
			GunGL.scale(0.5, 0.5, 0.5);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot);
			GunGL.rotate(45, 1, 0, 0);
			this.renderMuzzleFlash(shot);
			GunGL.popMatrix();
		}
	}
}
