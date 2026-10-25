package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderSPAS12}. */
public class ItemRenderSPAS12 extends ItemRenderWeaponBase {

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
				-1.25F * offset, -1.75F * offset, -0.5F * offset,
				0, 0, 0);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.spas_12_tex);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(180, 0, 1, 0);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");

		GunGL.rotate(equip[0], 1, 0, 0);

		HbmAnimations.applyRelevantTransformation("MainBody");
		GunGL.renderPart(WeaponResources.spas_12, "MainBody");

		GunGL.pushMatrix();
		HbmAnimations.applyRelevantTransformation("PumpGrip");
		GunGL.renderPart(WeaponResources.spas_12, "PumpGrip");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.bindTexture(WeaponResources.casings_tex);

		HbmAnimations.applyRelevantTransformation("Shell");
		SpentCasing casing = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getCasing(stack, Minecraft.getInstance().player.getInventory());
		int color0 = SpentCasing.COLOR_CASE_BRASS;
		int color1 = SpentCasing.COLOR_CASE_BRASS;

		if(casing != null) {
			int[] colors = casing.getColors();
			color0 = colors[0];
			color1 = colors[colors.length > 1 ? 1 : 0];
		}

		int shellColor = color1;
		GunGL.color(RenderHelperB.red(shellColor) / 255F, RenderHelperB.green(shellColor) / 255F, RenderHelperB.blue(shellColor) / 255F);
		GunGL.renderPart(WeaponResources.spas_12, "Shell");

		int shellForeColor = color0;
		GunGL.color(RenderHelperB.red(shellForeColor) / 255F, RenderHelperB.green(shellForeColor) / 255F, RenderHelperB.blue(shellForeColor) / 255F);
		GunGL.renderPart(WeaponResources.spas_12, "ShellFore");

		GunGL.color(1F, 1F, 1F);

		double smokeScale = 0.25;

		GunGL.pushMatrix();
		GunGL.translate(0, 1.5, -11);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.75D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1.5, -11);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();

		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, -0.75, 0);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(4.25, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.5, -4.25);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.rotate(180, 0, 1, 0);
		GunGL.bindTexture(WeaponResources.spas_12_tex);
		GunGL.renderPart(WeaponResources.spas_12, "MainBody");
		GunGL.renderPart(WeaponResources.spas_12, "PumpGrip");

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
			GunGL.translate(0, 1.5, -11);
			GunGL.rotate(-90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			this.renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}
}
