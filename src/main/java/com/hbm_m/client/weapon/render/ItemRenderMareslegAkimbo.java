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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderMareslegAkimbo}. */
public class ItemRenderMareslegAkimbo extends ItemRenderWeaponBase {

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {

			GunGL.bindTexture(WeaponResources.maresleg_tex);
			GunGL.pushMatrix();

			int index = i == -1 ? 0 : 1;

			standardAimingTransform(stack, -1.5F * offset * i, -1F * offset, 2F * offset, 0, -3.875 / 8D, 1);

			double scale = 0.375D;
			GunGL.scale(scale, scale, scale);

			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] lever = HbmAnimations.getRelevantTransformation("LEVER", index);
			double[] turn = HbmAnimations.getRelevantTransformation("TURN", index);
			double[] flip = HbmAnimations.getRelevantTransformation("FLIP", index);
			double[] lift = HbmAnimations.getRelevantTransformation("LIFT", index);
			double[] shell = HbmAnimations.getRelevantTransformation("SHELL", index);
			double[] flag = HbmAnimations.getRelevantTransformation("FLAG", index);

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
			GunGL.translate(0, 1, 3.75);
			GunGL.rotate(turn[2], 0, 0, -1);
			GunGL.rotate(flip[0], 1, 0, 0);
			GunGL.rotate(90, 0, 1, 0);
			this.renderSmokeNodes(gun.getConfig(stack, index).smokeNodes, 0.25D);
			GunGL.popMatrix();

			WeaponRenderRegistry.renderPart(WeaponResources.maresleg, "Gun", GunGL.pose());

			WeaponJointRig rig = WeaponRenderRegistry.getRig(WeaponResources.maresleg);
			WeaponKinematics.renderPartWithJoint(rig, "Lever", GunGL.pose(), (float) lever[0]);
			WeaponKinematics.renderPartWithVector(rig, "Shell", GunGL.pose(), (float) shell[0], (float) (shell[1] - 0.75), (float) shell[2]);

			if(flag[0] != 0) {
				WeaponKinematics.renderPartWithVector(rig, "Shell", GunGL.pose(), 0f, -0.5f, 0f);
			}

			GunGL.pushMatrix();
			GunGL.translate(0, 1, 3.75);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
			this.renderMuzzleFlash(gun.lastShot[index], 75, 5);
			GunGL.popMatrix();

			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0.25, 3);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPersonAkimbo(stack);
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0.25, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		GunGL.scale(1, 1, -1);
		GunGL.translate(8, 8, 0);
		double scale = 2.5D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -12.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.5, 1);
	}

	@Override
	public void renderInv(ItemStack stack) {

		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.maresleg_tex);

		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-1, 0, 0);
		GunGL.renderPart(WeaponResources.maresleg, "Gun");
		GunGL.renderPart(WeaponResources.maresleg, "Lever");
		GunGL.popMatrix();

		GunGL.translate(0, 0, 5);
		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.rotate(-90, 1, 0, 0);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(-45, 0, 1, 0);
		GunGL.translate(1, 0, 0);
		GunGL.renderPart(WeaponResources.maresleg, "Gun");
		GunGL.renderPart(WeaponResources.maresleg, "Lever");
		GunGL.popMatrix();

	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		renderOther(stack, ItemRenderType.INVENTORY);
	}

	@Override
	public void renderEntity(ItemStack stack) {
		GunGL.enableLighting();

		GunGL.pushMatrix();
		GunGL.bindTexture(WeaponResources.maresleg_tex);

		GunGL.translate(-1, 1, 0);
		GunGL.renderPart(WeaponResources.maresleg, "Gun");
		GunGL.renderPart(WeaponResources.maresleg, "Lever");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(1, 1, 0);
		GunGL.renderPart(WeaponResources.maresleg, "Gun");
		GunGL.renderPart(WeaponResources.maresleg, "Lever");
		GunGL.popMatrix();

	}

	@Override
	public void renderEquipped(ItemStack stack, Object... data) {
		renderOther(stack, ItemRenderType.EQUIPPED);
		if(data.length > 1 && data[1] instanceof LivingEntity ent) {
			renderEquippedMuzzleFlash(stack, ent, 1, 0, 1, 3.75, 1.0D, 75, 5);
		}
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {
		renderOther(stack, ItemRenderType.EQUIPPED);
		renderEquippedMuzzleFlash(stack, ent, 0, 0, 1, 3.75, 1.0D, 75, 5);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {

		GunGL.bindTexture(WeaponResources.maresleg_tex);
		GunGL.renderPart(WeaponResources.maresleg, "Gun");
		GunGL.renderPart(WeaponResources.maresleg, "Lever");
	}
}
