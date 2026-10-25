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

/** 1:1 {@code ItemRenderHenry}. */
public class ItemRenderHenry extends ItemRenderWeaponBase {

	public ResourceLocation texture;

	public ItemRenderHenry(ResourceLocation texture) {
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
				-1.25F * offset, -1F * offset, 1.75F * offset,
				0, -5 / 8D, 1);

		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		double r = -2.5 * aimingProgress;
		GunGL.rotate(r, 1, 0, 0);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(texture);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] sight = HbmAnimations.getRelevantTransformation("SIGHT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] lever = HbmAnimations.getRelevantTransformation("LEVER");
		double[] turn = HbmAnimations.getRelevantTransformation("TURN");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] twist = HbmAnimations.getRelevantTransformation("TWIST");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] yeet = HbmAnimations.getRelevantTransformation("YEET");
		double[] roll = HbmAnimations.getRelevantTransformation("ROLL");

		GunGL.translate(recoil[0] * 2, recoil[1], recoil[2]);
		GunGL.rotate(recoil[2] * 5, 1, 0, 0);
		GunGL.rotate(turn[2], 0, 0, 1);

		GunGL.translate(yeet[0], yeet[1], yeet[2]);

		GunGL.translate(0, 1, 0);
		GunGL.rotate(roll[2], 0, 0, 1);
		GunGL.translate(0, -1, 0);

		GunGL.translate(0, -4, 4);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 4, -4);

		GunGL.translate(0, 2, -4);
		GunGL.rotate(equip[0], -1, 0, 0);
		GunGL.translate(0, -2, 4);

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 8);
		GunGL.rotate(turn[2], 0, 0, -1);
		GunGL.rotate(90, 0, 1, 0);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.25D);
		GunGL.popMatrix();

		GunGL.renderPart(WeaponResources.henry, "Gun");

		GunGL.pushMatrix();
		GunGL.translate(0, 1.25, -0.1875);
		GunGL.rotate(sight[0], 1, 0, 0);
		GunGL.translate(0, -1.25, 0.1875);
		GunGL.renderPart(WeaponResources.henry, "Sight");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0.625, -3);
		GunGL.rotate(-30 + hammer[0], 1, 0, 0);
		GunGL.translate(0, -0.625, 3);
		GunGL.renderPart(WeaponResources.henry, "Hammer");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0.25, -2.3125);
		GunGL.rotate(lever[0], 1, 0, 0);
		GunGL.translate(0, -0.25, 2.3125);
		GunGL.renderPart(WeaponResources.henry, "Lever");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 0);
		GunGL.rotate(twist[2], 0, 0, 1);
		GunGL.translate(0, -1, 0);
		GunGL.renderPart(WeaponResources.henry, "Front");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(bullet[0], bullet[1], bullet[2] - 1);
		GunGL.renderPart(WeaponResources.henry, "Bullet");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		renderMuzzleFlash(gun.lastShot[0], 75, 5);
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
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.5, 0.5, 0);
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

		GunGL.bindTexture(texture);
		GunGL.renderAll(WeaponResources.henry);

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
			renderMuzzleFlash(shot, 75, 5);
			GunGL.popMatrix();
		}
	}
}
