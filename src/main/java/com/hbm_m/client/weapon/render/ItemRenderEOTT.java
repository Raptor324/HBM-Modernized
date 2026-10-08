package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderEOTT}. */
public class ItemRenderEOTT extends ItemRenderWeaponBase {

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
		GunGL.translate(0, 0, 1);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		float offset = 0.8F;

		for(int i = -1; i <= 1; i += 2) {
			GunGL.bindTexture(WeaponResources.eott_tex);
			int index = i == -1 ? 0 : 1;

			GunGL.pushMatrix();
			standardAimingTransform(stack,
					-1.0F * offset * i, -1.25F * offset, 1.25F * offset,
					0, -5.25 / 8D, 0.125);

			double scale = 0.25D;
			GunGL.scale(scale, scale, scale);

			double[] equip = HbmAnimations.getRelevantTransformation("EQUIP", index);
			double[] rise = HbmAnimations.getRelevantTransformation("RISE", index);
			double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL", index);
			double[] slide = HbmAnimations.getRelevantTransformation("SLIDE", index);
			double[] bullet = HbmAnimations.getRelevantTransformation("BULLET", index);
			double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER", index);
			double[] roll = HbmAnimations.getRelevantTransformation("ROLL", index);
			double[] mag = HbmAnimations.getRelevantTransformation("MAG", index);
			double[] magroll = HbmAnimations.getRelevantTransformation("MAGROLL", index);
			double[] sight = HbmAnimations.getRelevantTransformation("SIGHT", index);

			GunGL.translate(0, rise[1], 0);

			GunGL.translate(0, 1, -2.25);
			GunGL.rotate(equip[0], 1, 0, 0);
			GunGL.translate(0, -1, 2.25);

			GunGL.translate(0, -1, -4);
			GunGL.rotate(recoil[0], 1, 0, 0);
			GunGL.translate(0, 1, 4);

			GunGL.translate(0, 1, 0);
			GunGL.rotate(roll[2] * i, 0, 0, 1);
			GunGL.translate(0, -1, 0);

			GunGL.renderPart(WeaponResources.aberrator, "Gun");

			GunGL.pushMatrix();
			GunGL.translate(0, 2.4375, -1.9375);
			GunGL.rotate(sight[0], 1, 0, 0);
			GunGL.translate(0, -2.4375, 1.9375);
			GunGL.renderPart(WeaponResources.aberrator, "Sight");
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(mag[0] * i, mag[1], mag[2]);

			GunGL.translate(0, 1, 0);
			GunGL.rotate(magroll[2] * i, 0, 0, 1);
			GunGL.translate(0, -1, 0);

			GunGL.renderPart(WeaponResources.aberrator, "Magazine");
			GunGL.translate(bullet[0], bullet[1], bullet[2]);
			GunGL.renderPart(WeaponResources.aberrator, "Bullet");
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 0, slide[2]);
			GunGL.renderPart(WeaponResources.aberrator, "Slide");
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 1.25, -3.625);
			GunGL.rotate(-45 + hammer[0], 1, 0, 0);
			GunGL.translate(0, -1.25, 3.625);
			GunGL.renderPart(WeaponResources.aberrator, "Hammer");
			GunGL.popMatrix();

			double smokeScale = 0.5;

			GunGL.pushMatrix();
			GunGL.translate(0, 2, 4);
			GunGL.rotate(recoil[0], -1, 0, 0);
			GunGL.rotate(roll[2] * i, 0, 0, -1);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.scale(smokeScale, smokeScale, smokeScale);
			renderSmokeNodes(gun.getConfig(stack, index).smokeNodes, 0.5D);
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 2, 4);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			renderMuzzleFlash(gun.lastShot[index], 75, 7.5);
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 2, -1.5);
			GunGL.scale(0.5, 0.5, 0.5);
			renderFireball(gun.lastShot[index]);
			GunGL.popMatrix();

			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, -1, 4);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupThirdPersonAkimbo(ItemStack stack) {
		super.setupThirdPersonAkimbo(stack);
		GunGL.translate(0, -1, 4);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);

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
		GunGL.translate(0,-1, 0.5);
	}

	@Override
	public void renderInv(ItemStack stack) {

		GunGL.enableLighting();
		GunGL.translate(0, 1, 0);

		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-1, 0, 0);
		GunGL.bindTexture(WeaponResources.eott_tex);
		GunGL.renderPart(WeaponResources.aberrator, "Gun");
		GunGL.renderPart(WeaponResources.aberrator, "Hammer");
		GunGL.renderPart(WeaponResources.aberrator, "Magazine");
		GunGL.renderPart(WeaponResources.aberrator, "Slide");
		GunGL.renderPart(WeaponResources.aberrator, "Sight");
		GunGL.popMatrix();

		GunGL.translate(0, 0, 5);
		GunGL.pushMatrix();
		GunGL.rotate(225, 0, 0, 1);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.rotate(-90, 1, 0, 0);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(-45, 0, 1, 0);
		GunGL.translate(1, 0, 0);
		GunGL.bindTexture(WeaponResources.eott_tex);
		GunGL.renderPart(WeaponResources.aberrator, "Gun");
		GunGL.renderPart(WeaponResources.aberrator, "Hammer");
		GunGL.renderPart(WeaponResources.aberrator, "Magazine");
		GunGL.renderPart(WeaponResources.aberrator, "Slide");
		GunGL.renderPart(WeaponResources.aberrator, "Sight");
		GunGL.popMatrix();
	}

	@Override
	public void renderEquipped(ItemStack stack, Object... data) {

		GunGL.bindTexture(WeaponResources.eott_tex);
		GunGL.renderPart(WeaponResources.aberrator, "Gun");
		GunGL.renderPart(WeaponResources.aberrator, "Hammer");
		GunGL.renderPart(WeaponResources.aberrator, "Magazine");
		GunGL.renderPart(WeaponResources.aberrator, "Slide");
		GunGL.renderPart(WeaponResources.aberrator, "Sight");

		LivingEntity ent = (LivingEntity) data[1];
		long shot;
		double shotRand = 0;
		if(ent == Minecraft.getInstance().player) {
			ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
			shot = gun.lastShot[1];
			shotRand = gun.shotRand;
		} else {
			shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
			if(shot < 0) return;
		}

		GunGL.pushMatrix();
		GunGL.translate(0, 2, 4);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * shotRand, 1, 0, 0);
		GunGL.scale(0.75, 0.75, 0.75);
		renderMuzzleFlash(shot, 75, 7.5);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 2, -1.5);
		GunGL.scale(0.5, 0.5, 0.5);
		renderFireball(shot);
		GunGL.popMatrix();
	}

	@Override
	public void renderEquippedAkimbo(ItemStack stack, LivingEntity ent) {

		GunGL.bindTexture(WeaponResources.eott_tex);
		GunGL.renderPart(WeaponResources.aberrator, "Gun");
		GunGL.renderPart(WeaponResources.aberrator, "Hammer");
		GunGL.renderPart(WeaponResources.aberrator, "Magazine");
		GunGL.renderPart(WeaponResources.aberrator, "Slide");
		GunGL.renderPart(WeaponResources.aberrator, "Sight");

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
		GunGL.translate(0, 2, 4);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * shotRand, 1, 0, 0);
		GunGL.scale(0.75, 0.75, 0.75);
		renderMuzzleFlash(shot, 75, 7.5);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 2, -1.5);
		GunGL.scale(0.5, 0.5, 0.5);
		renderFireball(shot);
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
		GunGL.bindTexture(WeaponResources.eott_tex);

		GunGL.translate(-1, 1, 0);
		GunGL.renderPart(WeaponResources.aberrator, "Gun");
		GunGL.renderPart(WeaponResources.aberrator, "Hammer");
		GunGL.renderPart(WeaponResources.aberrator, "Magazine");
		GunGL.renderPart(WeaponResources.aberrator, "Slide");
		GunGL.renderPart(WeaponResources.aberrator, "Sight");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(1, 1, 0);
		GunGL.renderPart(WeaponResources.aberrator, "Gun");
		GunGL.renderPart(WeaponResources.aberrator, "Hammer");
		GunGL.renderPart(WeaponResources.aberrator, "Magazine");
		GunGL.renderPart(WeaponResources.aberrator, "Slide");
		GunGL.renderPart(WeaponResources.aberrator, "Sight");
		GunGL.popMatrix();
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {

		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.eott_tex);
		GunGL.renderPart(WeaponResources.aberrator, "Gun");
		GunGL.renderPart(WeaponResources.aberrator, "Hammer");
		GunGL.renderPart(WeaponResources.aberrator, "Magazine");
		GunGL.renderPart(WeaponResources.aberrator, "Slide");
		GunGL.renderPart(WeaponResources.aberrator, "Sight");
	}

	public static void renderFireball(long lastShot) {
		GunGL.Tess tess = GunGL.tess();

		int flash = 150;

		if(System.currentTimeMillis() - lastShot < flash) {
			GunGL.enableBlend();
			GunGL.blendFunc(GunGL.GL_SRC_ALPHA, GunGL.GL_ONE);
			GunGL.disableCull();
			GunGL.pushMatrix();

			double fire = (System.currentTimeMillis() - lastShot) / (double) flash;
			double height = 5 * fire;
			double length = 10 * fire;
			double offset = 1 * fire;
			double lengthOffset = -1.125;
			GunGL.bindTexture(flash_plume);
			tess.startDrawingQuads();
			tess.setBrightness(240);
			tess.setNormal(0F, 1F, 0F);
			tess.setColorRGBA_F(1F, 1F, 1F, 1F);

			tess.addVertexWithUV(height, -offset, 0, 0, 1);
			tess.addVertexWithUV(-height, -offset, 0, 1, 1);
			tess.addVertexWithUV(-height, -offset + length, -lengthOffset, 1, 0);
			tess.addVertexWithUV(height, -offset + length, -lengthOffset, 0 ,0);

			tess.addVertexWithUV(height, -offset, 0, 0, 1);
			tess.addVertexWithUV(-height, -offset, 0, 1, 1);
			tess.addVertexWithUV(-height, -offset + length, lengthOffset, 1, 0);
			tess.addVertexWithUV(height, -offset + length, lengthOffset, 0 ,0);

			tess.draw();
			GunGL.popMatrix();
			GunGL.disableBlend();
			GunGL.enableCull();
		}
	}
}
