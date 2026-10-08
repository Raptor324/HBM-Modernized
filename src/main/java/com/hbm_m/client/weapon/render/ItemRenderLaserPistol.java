package com.hbm_m.client.weapon.render;

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

/** 1:1 {@code ItemRenderLaserPistol}. */
public class ItemRenderLaserPistol extends ItemRenderWeaponBase {

	public ResourceLocation texture;

	public ItemRenderLaserPistol(ResourceLocation texture) {
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
				-1.75F * offset, -2F * offset, 2.75F * offset,
				0, -10 / 8D, 1.25);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(texture);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] latch = HbmAnimations.getRelevantTransformation("LATCH");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] jolt = HbmAnimations.getRelevantTransformation("JOLT");
		double[] battery = HbmAnimations.getRelevantTransformation("BATTERY");
		double[] swirl = HbmAnimations.getRelevantTransformation("SWIRL");

		GunGL.translate(0, -1, -6);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 1, 6);

		GunGL.translate(0, 2, -2);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, -2, 2);

		GunGL.translate(0, -1, -1);
		GunGL.rotate(swirl[0], 1, 0, 0);
		GunGL.translate(0, 1, 1);

		GunGL.translate(0, 0, recoil[2]);
		GunGL.translate(jolt[0], jolt[1], jolt[2]);

		GunGL.renderPart(WeaponResources.laser_pistol, "Gun");
		if(hasCapacitors(stack)) GunGL.renderPart(WeaponResources.laser_pistol, "Capacitors");
		if(hasTape(stack)) GunGL.renderPart(WeaponResources.laser_pistol, "Tape");

		GunGL.pushMatrix();
		GunGL.translate(1.125, 0, -1.9125);
		GunGL.rotate(latch[1], 0, 1, 0);
		GunGL.translate(-1.125, 0, 1.9125);
		GunGL.renderPart(WeaponResources.laser_pistol, "Latch");
		GunGL.translate(battery[0], battery[1], battery[2]);
		GunGL.renderPart(WeaponResources.laser_pistol, "Battery");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 2, 4.75);
		GunGL.rotate(90, 0, 1, 0);
		renderLaserFlash(gun.lastShot[0], 150, 1.5D, hasEmerald(stack) ? 0x008000 : 0xff0000);
		GunGL.translate(0, 0, -0.25);
		renderLaserFlash(gun.lastShot[0], 150, 0.75D, hasEmerald(stack) ? 0x80ff00 : 0xff8000);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, -0.5, 1);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(texture);
		GunGL.renderPart(WeaponResources.laser_pistol, "Gun");
		GunGL.renderPart(WeaponResources.laser_pistol, "Latch");
		if(hasCapacitors(stack)) GunGL.renderPart(WeaponResources.laser_pistol, "Capacitors");
		if(hasTape(stack)) GunGL.renderPart(WeaponResources.laser_pistol, "Tape");

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
			GunGL.translate(0, 2, 4.75);
			GunGL.rotate(90, 0, 1, 0);
			renderLaserFlash(shot, 150, 1.5D, hasEmerald(stack) ? 0x008000 : 0xff0000);
			GunGL.translate(0, 0, -0.25);
			renderLaserFlash(shot, 150, 0.75D, hasEmerald(stack) ? 0x80ff00 : 0xff8000);
			GunGL.popMatrix();
		}
	}

	public boolean hasCapacitors(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_laser_pistol_pew_pew");
	}

	public boolean hasTape(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_laser_pistol_pew_pew");
	}

	public boolean hasEmerald(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_laser_pistol_morning_glory");
	}
}
