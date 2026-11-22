package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.weapon.WeaponJointRig;
import com.hbm_m.client.render.weapon.WeaponKinematics;
import com.hbm_m.client.render.weapon.WeaponRenderRegistry;
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

		WeaponRenderRegistry.renderPart(WeaponResources.henry, "Gun", GunGL.pose());

		WeaponJointRig rig = WeaponRenderRegistry.getRig(WeaponResources.henry);
		WeaponKinematics.renderPartWithJoint(rig, "Sight", GunGL.pose(), (float) sight[0]);
		WeaponKinematics.renderPartWithJoint(rig, "Hammer", GunGL.pose(), (float) (-30 + hammer[0]));
		WeaponKinematics.renderPartWithJoint(rig, "Lever", GunGL.pose(), (float) lever[0]);
		WeaponKinematics.renderPartWithJoint(rig, "Front", GunGL.pose(), (float) twist[2]);

		WeaponKinematics.scoped(GunGL.pose(), ps -> {
			WeaponKinematics.applyVectorTranslate(ps, (float) bullet[0], (float) bullet[1], (float) (bullet[2] - 1));
			WeaponRenderRegistry.renderPart(WeaponResources.henry, "Bullet", ps);
		});

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		renderMuzzleFlash(gun.lastShot[0], 75, 5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		setupThirdPerson(stack, 1.75D, 0, 0.25, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		setupInv(stack, 1.5D, -0.5, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		setupModTable(stack, -7.5D, 0, 0, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();
		GunGL.bindTexture(texture);
		GunGL.renderAll(WeaponResources.henry);
		renderEquippedMuzzleFlash(type, stack, data, 0, 1, 8, 1.0D, 75, 5);
	}
}
