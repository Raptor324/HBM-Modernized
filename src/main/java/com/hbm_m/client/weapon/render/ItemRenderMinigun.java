package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.weapon.WeaponJointRig;
import com.hbm_m.client.render.weapon.WeaponKinematics;
import com.hbm_m.client.render.weapon.WeaponRenderRegistry;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderMinigun}. */
public class ItemRenderMinigun extends ItemRenderWeaponBase {

	protected ResourceLocation texture;

	public ItemRenderMinigun(ResourceLocation texture) {
		this.texture = texture;
	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		return standardZoomFov(fov, 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.75F * offset, -1.75F * offset, 3.5F * offset,
			0, -6.25 / 8D, 1);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(texture);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] rotate = HbmAnimations.getRelevantTransformation("ROTATE");

		GunGL.rotateAround(equip[0], 1, 0, 0, 0, 3, -6);

		GunGL.translate(0, 0, recoil[2]);

		WeaponRenderRegistry.renderPart(WeaponResources.minigun, "Gun", GunGL.pose());
		WeaponRenderRegistry.renderPart(WeaponResources.minigun, "Grip", GunGL.pose());

		WeaponJointRig rig = WeaponRenderRegistry.getRig(WeaponResources.minigun);
		WeaponKinematics.renderPartWithJoint(rig, "Barrels", GunGL.pose(), (float) rotate[2]);

		double smokeScale = 0.5;

		GunGL.pushMatrix();
		GunGL.translate(-2, 1.25, -3.5);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 12);
		GunGL.rotate(90, 0, 1, 0);

		if(stack.getItem() == WeaponItems.gun("gun_minigun_lacunae")) {
			renderLaserFlash(gun.lastShot[0], 50, 1D, 0xff00ff);
			GunGL.translate(0, 0, -0.25);
			renderLaserFlash(gun.lastShot[0], 50, 0.5D, 0xff0080);
		} else {
			GunGL.translate(0, 0.5, 0);
			GunGL.rotate(gun.shotRand * 90, 1, 0, 0);
			GunGL.scale(1.5, 1.5, 1.5);
			this.renderMuzzleFlash(gun.lastShot[0], 50, 7.5);
		}
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		setupThirdPerson(stack, 1.75D, 1, -3.5, 8);
	}

	@Override
	public void setupInv(ItemStack stack) {
		setupInv(stack, 0.875D, -0.25, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		setupModTable(stack, -6.25D, 0, 0, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(texture);
		GunGL.renderPart(WeaponResources.minigun, "Gun");
		GunGL.renderPart(WeaponResources.minigun, "Grip");
		GunGL.renderPart(WeaponResources.minigun, "Barrels");

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
			GunGL.translate(0, 0, 12.25);
			GunGL.rotate(90, 0, 1, 0);

			if(stack.getItem() == WeaponItems.gun("gun_minigun_lacunae")) {
				renderLaserFlash(shot, 50, 1D, 0xff00ff);
				GunGL.translate(0, 0, -0.25);
				renderLaserFlash(shot, 50, 0.5D, 0xff0080);
			} else {
				GunGL.translate(0, 0.5, 0);
				GunGL.rotate(shotRand * 90, 1, 0, 0);
				GunGL.scale(1.5, 1.5, 1.5);
				this.renderMuzzleFlash(shot, 50, 7.55);
			}
			GunGL.popMatrix();
		}
	}
}
