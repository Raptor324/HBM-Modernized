package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderStarFAkimbo}. */
public class ItemRenderStarFAkimbo extends ItemRenderWeaponBase {

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {
			int index = i == -1 ? 0 : 1;
			GunGL.bindTexture(WeaponResources.star_f_elite_tex);

			GunGL.pushMatrix();
			standardAimingTransform(stack, -2F * offset * i, -1.75F * offset, 2.5F * offset, 0, -7.625 / 8D, 1);

			double scale = 0.25D;
			GunGL.scale(scale, scale, scale);

			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER", index);
			double[] tilt = HbmAnimations.getRelevantTransformation("TILT", index);
			double[] turn = HbmAnimations.getRelevantTransformation("TURN", index);
			double[] mag = HbmAnimations.getRelevantTransformation("MAG", index);
			double[] bullet = HbmAnimations.getRelevantTransformation("BULLET", index);
			double[] slide = HbmAnimations.getRelevantTransformation("SLIDE", index);

			GunGL.translate(0, -2, -8);
			GunGL.rotate(equip[0], 1, 0, 0);
			GunGL.translate(0, 2, 8);

			GunGL.translate(0, 1, -3);
			GunGL.rotate(turn[2] * i, 0, 0, 1);
			GunGL.rotate(tilt[0], 1, 0, 0);
			GunGL.translate(0, -1, 3);

			GunGL.translate(0, 0, recoil[2]);

			GunGL.renderPart(WeaponResources.star_f, "Gun");

			GunGL.pushMatrix();
			GunGL.translate(0, 1.75, -4.25);
			GunGL.rotate(60 * (hammer[0] - 1), 1, 0, 0);
			GunGL.translate(0, -1.75, 4.25);
			GunGL.renderPart(WeaponResources.star_f, "Hammer");
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 0, slide[2] * 2.3125);
			GunGL.renderPart(WeaponResources.star_f, "Slide");
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(mag[0], mag[1], mag[2]);
			GunGL.renderPart(WeaponResources.star_f, "Mag");
			GunGL.translate(bullet[0], bullet[1], bullet[2]);
			GunGL.renderPart(WeaponResources.star_f, "Bullet");
			GunGL.popMatrix();

			if(hasSilencer(stack, index)) {
				GunGL.pushMatrix();
				GunGL.translate(0, 2.375, -0.25);
				GunGL.bindTexture(WeaponResources.uzi_tex);
				GunGL.renderPart(WeaponResources.uzi, "Silencer");
				GunGL.popMatrix();

			} else {
				double smokeScale = 0.5;

				GunGL.pushMatrix();
				GunGL.translate(0, 3, 6.125);
				GunGL.rotate(90, 0, 1, 0);
				GunGL.scale(smokeScale, smokeScale, smokeScale);
				this.renderSmokeNodes(gun.getConfig(stack, index).smokeNodes, 0.75D);
				GunGL.popMatrix();

				renderMuzzleFlash(gun.shotRand, gun.lastShot[index]);
			}

			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, -0.25, 1.75);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPersonAkimbo(stack);
		GunGL.translate(0, -0.25, 1.75);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupInv(ItemStack stack) {
		GunGL.scale(1, 1, -1);
		GunGL.translate(8, 8, 0);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -6.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.25, -5);
	}

	@Override
	public void renderEquipped(ItemStack stack, Object... data) {
		renderStandardGun(stack, 1);
		if(!hasSilencer(stack, 1)) renderThirdPersonFlash((LivingEntity) data[1], stack, 1);
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {
		renderStandardGun(stack, 0);
		if(!hasSilencer(stack, 0)) renderThirdPersonFlash(ent, stack, 0);
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.star_f_elite_tex);
		GunGL.renderPart(WeaponResources.star_f, "Gun");
		GunGL.renderPart(WeaponResources.star_f, "Slide");
		GunGL.renderPart(WeaponResources.star_f, "Mag");
		GunGL.renderPart(WeaponResources.star_f, "Hammer");
		if(hasSilencer(stack, index)) {
			GunGL.translate(0, 2.375, -0.25);
			GunGL.bindTexture(WeaponResources.uzi_tex);
			GunGL.renderPart(WeaponResources.uzi, "Silencer");
		}
	}

	@Override
	public void renderEntity(ItemStack stack) {
		GunGL.enableLighting();

		boolean anySilenced = hasSilencer(stack, 0) || hasSilencer(stack, 1);

		if(anySilenced) {
			GunGL.scale(0.75, 0.75, 0.75);
		}

		GunGL.pushMatrix();
		GunGL.translate(-1, 1, 0);
		renderStandardGun(stack, 1);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(1, 1, 0);
		renderStandardGun(stack, 0);
		GunGL.popMatrix();

	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		boolean anySilenced = hasSilencer(stack, 0) || hasSilencer(stack, 1);

		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0.5, 0, 0);
		if(anySilenced) {
			double scale = 0.625D;
			GunGL.scale(scale, scale, scale);
			GunGL.translate(0, 0, -4);
		}
		renderStandardGun(stack, 1);
		GunGL.popMatrix();

		GunGL.translate(0, 0, 5);

		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.rotate(-90, 1, 0, 0);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(-45, 0, 1, 0);
		GunGL.translate(-0.5, 0, 0);
		if(anySilenced) {
			double scale = 0.625D;
			GunGL.scale(scale, scale, scale);
			GunGL.translate(0, 0, -4);
		}
		renderStandardGun(stack, 0);
		GunGL.popMatrix();

	}

	public boolean hasSilencer(ItemStack stack, int cfg) {
		return XWeaponModManager.hasUpgrade(stack, cfg, XWeaponModManager.ID_SILENCER);
	}

	public void renderStandardGun(ItemStack stack, int index) {

		GunGL.bindTexture(WeaponResources.star_f_elite_tex);
		GunGL.renderPart(WeaponResources.star_f, "Gun");
		GunGL.renderPart(WeaponResources.star_f, "Slide");
		GunGL.renderPart(WeaponResources.star_f, "Mag");
		GunGL.renderPart(WeaponResources.star_f, "Hammer");
		boolean silenced = hasSilencer(stack, index);
		if(silenced) {
			GunGL.translate(0, 2.375, -0.25);
			GunGL.bindTexture(WeaponResources.uzi_tex);
			GunGL.renderPart(WeaponResources.uzi, "Silencer");
		}
	}

	public void renderThirdPersonFlash(Entity ent, ItemStack stack, int config) {

		long shot;
		double shotRand = 0;
		if(ent == Minecraft.getInstance().player) {
			ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
			shot = gun.lastShot[config];
			shotRand = gun.shotRand;
		} else {
			shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
			if(shot < 0) return;
		}

		renderMuzzleFlash(shotRand, shot);
	}

	public void renderMuzzleFlash(double shotRand, long shot) {

		GunGL.pushMatrix();
		GunGL.translate(0, 3, 6.125);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * shotRand, 1, 0, 0);
		this.renderMuzzleFlash(shot, 75, 7.5);
		GunGL.popMatrix();
	}
}
