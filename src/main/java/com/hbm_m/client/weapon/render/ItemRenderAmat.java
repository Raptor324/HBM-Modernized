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

/** 1:1 {@code ItemRenderAmat}. */
public class ItemRenderAmat extends ItemRenderWeaponBase {

	public ResourceLocation texture;

	public ItemRenderAmat(ResourceLocation texture) {
		this.texture = texture;
	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * (isScoped(stack) ? 0.8F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;

		standardAimingTransform(stack,
			-1F * offset, -1F * offset, 3.25F * offset,
			0, -4.875 / 8D, 1.875);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		boolean isScoped = isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(texture);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		boolean deployed = HbmAnimations.getRelevantAnim(0) == null || HbmAnimations.getRelevantAnim(0).animation.getBus("BIPOD") == null;
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] bipod = HbmAnimations.getRelevantTransformation("BIPOD");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] boltTurn = HbmAnimations.getRelevantTransformation("BOLT_TURN");
		double[] boltPull = HbmAnimations.getRelevantTransformation("BOLT_PULL");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] scopeThrow = HbmAnimations.getRelevantTransformation("SCOPE_THROW");
		double[] scopeSpin = HbmAnimations.getRelevantTransformation("SCOPE_SPIN");

		GunGL.translate(0, 0, recoil[2]);

		GunGL.translate(0, -3, -8);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 3, 8);

		GunGL.renderPart(WeaponResources.amat, "Gun");

		if(isScoped(stack)) {
			GunGL.pushMatrix();
			GunGL.translate(scopeThrow[0], scopeThrow[1], scopeThrow[2]);
			GunGL.translate(0, 1.5, -4.5);
			GunGL.rotate(scopeSpin[0], 1, 0, 0);
			GunGL.translate(0, -1.5, 4.5);
			GunGL.renderPart(WeaponResources.amat, "Scope");
			GunGL.popMatrix();
		}

		GunGL.pushMatrix();
		GunGL.translate(0, 0.625, 0);
		GunGL.rotate(boltTurn[2], 0, 0, 1);
		GunGL.translate(0, -0.625, 0);
		GunGL.translate(0, 0, boltPull[2]);
		GunGL.renderPart(WeaponResources.amat, "Bolt");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);
		GunGL.renderPart(WeaponResources.amat, "Magazine");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0.3125, -0.625, -1);
		GunGL.rotate(deployed ? 25 : bipod[1], 0, 0, 1);
		GunGL.translate(-0.3125, 0.625, 1);
		GunGL.renderPart(WeaponResources.amat, "BipodHingeLeft");
		GunGL.translate(0.3125, -0.625, -1);
		GunGL.rotate(deployed ? 80 : bipod[0], 1, 0, 0);
		GunGL.translate(-0.3125, 0.625, 1);
		GunGL.renderPart(WeaponResources.amat, "BipodLeft");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(-0.3125, -0.625, -1);
		GunGL.rotate(deployed ? -25 : -bipod[1], 0, 0, 1);
		GunGL.translate(0.3125, 0.625, 1);
		GunGL.renderPart(WeaponResources.amat, "BipodHingeRight");
		GunGL.translate(-0.3125, -0.625, -1);
		GunGL.rotate(deployed ? 80 : bipod[0], 1, 0, 0);
		GunGL.translate(0.3125, 0.625, 1);
		GunGL.renderPart(WeaponResources.amat, "BipodRight");
		GunGL.popMatrix();

		if(isSilenced(stack)) {
			GunGL.translate(0, 0.625, -4.3125);
			GunGL.scale(1.25, 1.25, 1.25);
			GunGL.bindTexture(WeaponResources.g3_attachments);
			GunGL.renderPart(WeaponResources.g3, "Silencer");

		} else {
			GunGL.renderPart(WeaponResources.amat, "MuzzleBrake");

			double smokeScale = 0.5;

			GunGL.pushMatrix();
			GunGL.translate(0, 0.625, 12);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.scale(smokeScale, smokeScale, smokeScale);
			renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 0.5, 11);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			renderGapFlash(gun.lastShot[0]);
			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0.5, 6.75);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(isSilenced(stack)) {
		double scale = 0.8175D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
			GunGL.translate(-0.5, 0.5, -1);
		} else {
			double scale = 0.9375D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(45, 0, 1, 0);
			GunGL.translate(-0.5, 0.5, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.25, -1.5);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(texture);
		GunGL.renderPart(WeaponResources.amat, "Gun");
		GunGL.renderPart(WeaponResources.amat, "Bolt");
		GunGL.renderPart(WeaponResources.amat, "Magazine");
		GunGL.renderPart(WeaponResources.amat, "BipodLeft");
		GunGL.renderPart(WeaponResources.amat, "BipodHingeLeft");
		GunGL.renderPart(WeaponResources.amat, "BipodRight");
		GunGL.renderPart(WeaponResources.amat, "BipodHingeRight");
		if(isScoped(stack)) GunGL.renderPart(WeaponResources.amat, "Scope");

		boolean silenced = isSilenced(stack);
		if(silenced) {
			GunGL.translate(0, 0.625, -4.3125);
			GunGL.scale(1.25, 1.25, 1.25);
			GunGL.bindTexture(WeaponResources.g3_attachments);
			GunGL.renderPart(WeaponResources.g3, "Silencer");
		} else {
			GunGL.renderPart(WeaponResources.amat, "MuzzleBrake");
		}

		if(type == ItemRenderType.EQUIPPED && !silenced) {
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
			GunGL.translate(0, 0.5, 11);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			renderGapFlash(shot);
			GunGL.popMatrix();
		}
	}

	public boolean isScoped(ItemStack stack) {
		return true;
	}

	public boolean isSilenced(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_amat_penance") || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
	}
}
