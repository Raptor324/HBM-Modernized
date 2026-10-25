package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.factory.XFactoryCatapult;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderFatMan}. */
public class ItemRenderFatMan extends ItemRenderWeaponBase {

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
				-1.5F * offset, -1.25F * offset, 0.5F * offset,
				-1F * offset, -1.25F * offset, 0F * offset);
	}

	protected static String label = "AUTO";

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.fatman_tex);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);

		boolean isLoaded = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0;

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lid = HbmAnimations.getRelevantTransformation("LID");
		double[] nuke = HbmAnimations.getRelevantTransformation("NUKE");
		double[] piston = HbmAnimations.getRelevantTransformation("PISTON");
		double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
		double[] gauge = HbmAnimations.getRelevantTransformation("GAUGE");

		GunGL.translate(0, 1, -2);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, -1, 2);

		GunGL.renderPart(WeaponResources.fatman, "Launcher");

		GunGL.pushMatrix();
		GunGL.translate(0, 0, handle[2]);
		GunGL.renderPart(WeaponResources.fatman, "Handle");

		GunGL.translate(0.4375, -0.875, 0);
		GunGL.rotate(gauge[2], 0, 0, 1);
		GunGL.translate(-0.4375, 0.875, 0);
		GunGL.renderPart(WeaponResources.fatman, "Gauge");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0.25, 0.125, 0);
		GunGL.rotate(lid[2], 0, 0, 1);
		GunGL.translate(-0.25, -0.125, 0);
		GunGL.renderPart(WeaponResources.fatman, "Lid");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, piston[2]);
		if(!isLoaded && piston[2] == 0) GunGL.translate(0, 0, 3);
		GunGL.renderPart(WeaponResources.fatman, "Piston");
		GunGL.popMatrix();

		if(isLoaded || nuke[0] != 0 || nuke[1] != 0 || nuke[2] != 0) {
			GunGL.pushMatrix();
			GunGL.translate(nuke[0], nuke[1], nuke[2]);
			renderNuke(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getType(stack, null));
			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(-0.5, 0.5, -3);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.375D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		boolean isLoaded = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0;
		GunGL.bindTexture(WeaponResources.fatman_tex);

		GunGL.renderPart(WeaponResources.fatman, "Launcher");
		GunGL.renderPart(WeaponResources.fatman, "Handle");
		GunGL.renderPart(WeaponResources.fatman, "Gauge");
		GunGL.renderPart(WeaponResources.fatman, "Lid");
		if(!isLoaded) GunGL.translate(0, 0, 3);
		GunGL.renderPart(WeaponResources.fatman, "Piston");
		if(isLoaded) renderNuke(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getType(stack, null));
	}

	public void renderNuke(Object type) {
		if(type == XFactoryCatapult.nuke_balefire) {
			renderBalefire(interp);
		} else {
			GunGL.bindTexture(WeaponResources.fatman_mininuke_tex);
			GunGL.renderPart(WeaponResources.fatman, "MiniNuke");
		}
	}

	public static void renderBalefire(float interp) {

		Minecraft mc = Minecraft.getInstance();
		GunGL.bindTexture(WeaponResources.fatman_balefire_tex);
		GunGL.renderPart(WeaponResources.fatman, "MiniNuke");
		// glintBF + disableLightmap: Glanz-RenderType in RenderHelperA (Glint-Shader ohne Lichtkarte)

		float scale = 2F;
		float r = 0F;
		float g = 0.8F;
		float b = 0.15F;
		float speed = -6;
		float glintColor = 0.76F;
		int layers = 3;

		GunGL.pushMatrix();
		float offset = (mc.player != null ? mc.player.tickCount : 0) + interp;

		for(int k = 0; k < layers; ++k) {

			float movement = offset * (0.001F + (float) k * 0.003F) * speed;

			// Original: glColor4f(r,g,b * glintColor), Texturmatrix scale/rotate/translate, GL_EQUAL, SRC_COLOR/ONE, depthMask(false)
			RenderHelperA.renderGlintPart(WeaponResources.fatman, "MiniNuke", RenderHelperA.glintBF,
					r * glintColor, g * glintColor, b * glintColor, scale, 30.0F - k * 60.0F, movement);
		}

		GunGL.color(1.0F, 1.0F, 1.0F, 1.0F);
		GunGL.popMatrix();
	}
}
