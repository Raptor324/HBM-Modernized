package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderAberrator}. */
public class ItemRenderAberrator extends ItemRenderWeaponBase {

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

		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.0F * offset, -1.25F * offset, 1.25F * offset,
				0, -5.25 / 8D, 0.125);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.aberrator_tex);
		double scale = 0.25D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] rise = HbmAnimations.getRelevantTransformation("RISE");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] slide = HbmAnimations.getRelevantTransformation("SLIDE");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] hammer = HbmAnimations.getRelevantTransformation("HAMMER");
		double[] roll = HbmAnimations.getRelevantTransformation("ROLL");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] magroll = HbmAnimations.getRelevantTransformation("MAGROLL");
		double[] sight = HbmAnimations.getRelevantTransformation("SIGHT");

		GunGL.translate(0, rise[1], 0);

		GunGL.translate(0, 1, -2.25);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, -1, 2.25);

		GunGL.translate(0, -1, -4);
		GunGL.rotate(recoil[0], 1, 0, 0);
		GunGL.translate(0, 1, 4);

		GunGL.translate(0, 1, 0);
		GunGL.rotate(roll[2], 0, 0, 1);
		GunGL.translate(0, -1, 0);

		GunGL.renderPart(WeaponResources.aberrator, "Gun");

		GunGL.pushMatrix();
		GunGL.translate(0, 2.4375, -1.9375);
		GunGL.rotate(sight[0], 1, 0, 0);
		GunGL.translate(0, -2.4375, 1.9375);
		GunGL.renderPart(WeaponResources.aberrator, "Sight");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);

		GunGL.translate(0, 1, 0);
		GunGL.rotate(magroll[2], 0, 0, 1);
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
		GunGL.rotate(roll[2], 0, 0, -1);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 2, 4);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.75, 0.75, 0.75);
		renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 2, -1.5);
		GunGL.scale(0.5, 0.5, 0.5);
		renderFireball(gun.lastShot[0]);
		GunGL.popMatrix();

		// Original: Items.golden_sword-Icon aus dem Item-Atlas
		TextureAtlasSprite icon = RenderHelperA.bindItemIcon("minecraft:item/golden_sword");

		GunGL.disableCull();
		GunGL.disableLighting();
		float minU = icon.getU0();
		float maxU = icon.getU1();
		float minV = icon.getV0();
		float maxV = icon.getV1();
		GunGL.translate(0, 2, 4.5);
		GunGL.rotate(roll[2], 0, 0, -1);
		GunGL.rotate(recoil[0], -1, 0, 0);
		GunGL.rotate(equip[0], -1, 0, 0);
		GunGL.rotate(System.currentTimeMillis() / 50D % 360D, 0, 0, 1);

		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		aimingProgress = Math.min(1F, aimingProgress * 2);

		GunGL.Tess tess = GunGL.tess();
		GunGL.pushMatrix();
		int amount = 16;
		for(int i = 0; i < amount; i++) {
			GunGL.pushMatrix();
			GunGL.translate(0, -1.5 - aimingProgress, 0);
			GunGL.rotate(90 * aimingProgress, 1, 0, 0);
			GunGL.rotate(-45, 0, 0, 1);
			tess.startDrawingQuads();
			tess.setNormal(0F, 1F, 0F);
			tess.addVertexWithUV(-0.5, -0.5F, -0.5, maxU, maxV);
			tess.addVertexWithUV(0.5F, -0.5F, -0.5, minU, maxV);
			tess.addVertexWithUV(0.5F, 0.5F, -0.5, minU, minV);
			tess.addVertexWithUV(-0.5, 0.5F, -0.5, maxU, minV);
			tess.draw();
			GunGL.popMatrix();
			GunGL.rotate(360D / amount, 0, 0, 1);
		}
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, -1, 4);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.5,-1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -12.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0,-1, 0.5);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {

		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.aberrator_tex);
		GunGL.renderPart(WeaponResources.aberrator, "Gun");
		GunGL.renderPart(WeaponResources.aberrator, "Hammer");
		GunGL.renderPart(WeaponResources.aberrator, "Magazine");
		GunGL.renderPart(WeaponResources.aberrator, "Slide");
		GunGL.renderPart(WeaponResources.aberrator, "Sight");

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
