package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderAtlas}. */
public class ItemRenderAtlas extends ItemRenderWeaponBase {

	public ResourceLocation texture;

	public ItemRenderAtlas(ResourceLocation texture) {
		this.texture = texture;
	}

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

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.0F * offset, -0.75F * offset, 1F * offset,
				0, -3.125 / 8D, 0.25);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(texture);
		double scale = 0.125D;
		GunGL.scale(scale, scale, scale);

		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] reloadMove = HbmAnimations.getRelevantTransformation("RELOAD_MOVE");
		double[] reloadRot = HbmAnimations.getRelevantTransformation("RELOAD_ROT");
		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");

		GunGL.translate(recoil[0], recoil[1], recoil[2]);
		GunGL.rotate(recoil[2] * 10, 1, 0, 0);

		GunGL.translate(0, 0, -7);
		GunGL.rotate(equip[0], -1, 0, 0);
		GunGL.translate(0, 0, 7);

		GunGL.pushMatrix();
		GunGL.translate(0, 1.5, 9.25);
		GunGL.rotate(-recoil[2] * 10, 1, 0, 0);
		GunGL.rotate(90, 0, 1, 0);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		GunGL.popMatrix();

		GunGL.translate(reloadMove[0], reloadMove[1], reloadMove[2]);

		GunGL.rotate(reloadRot[0], 1, 0, 0);
		GunGL.rotate(reloadRot[2], 0, 0, 1);
		GunGL.rotate(reloadRot[1], 0, 1, 0);
		GunGL.renderPart(WeaponResources.bio_revolver, "Grip");

		GunGL.pushMatrix(); /// FRONT PUSH ///
		GunGL.rotate(HbmAnimations.getRelevantTransformation("FRONT")[2], 1, 0, 0);
		GunGL.renderPart(WeaponResources.bio_revolver, "Barrel");
		GunGL.pushMatrix(); /// LATCH PUSH ///
		GunGL.translate(0, 2.3125, -0.875);
		GunGL.rotate(HbmAnimations.getRelevantTransformation("LATCH")[2], 1, 0, 0);
		GunGL.translate(0, -2.3125, 0.875);
		GunGL.renderPart(WeaponResources.bio_revolver, "Latch");
		GunGL.popMatrix(); /// LATCH POP ///

		GunGL.pushMatrix(); /// DRUM PUSH ///
		GunGL.translate(0, 1, 0);
		GunGL.rotate(HbmAnimations.getRelevantTransformation("DRUM")[2] * 60, 0, 0, 1);
		GunGL.translate(0, -1, 0);
		GunGL.translate(0, 0, HbmAnimations.getRelevantTransformation("DRUM_PUSH")[2]);
		GunGL.renderPart(WeaponResources.bio_revolver, "Drum");
		GunGL.popMatrix(); /// DRUM POP ///

		GunGL.popMatrix(); /// FRONT POP ///

		GunGL.pushMatrix(); /// HAMMER ///
		GunGL.translate(0, 0, -4.5);
		GunGL.rotate(-45 + 45 * HbmAnimations.getRelevantTransformation("HAMMER")[2], 1, 0, 0);
		GunGL.translate(0, 0, 4.5);
		GunGL.renderPart(WeaponResources.bio_revolver, "Hammer");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1.5, 9.25);
		GunGL.rotate(90, 0, 1, 0);
		renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 1, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.125D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.5, 1.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 1.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(texture);
		GunGL.renderAll(WeaponResources.bio_revolver);

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
			GunGL.translate(0, 1.5, 9.25);
			GunGL.rotate(90, 0, 1, 0);
			renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}
}
