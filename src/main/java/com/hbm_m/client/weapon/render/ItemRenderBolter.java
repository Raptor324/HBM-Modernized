package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderBolter}. */
public class ItemRenderBolter extends ItemRenderWeaponBase {

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
				-1.5F * offset, -2F * offset, 2.5F * offset,
				0, -10.5 / 8D, 1.25);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		GunGL.bindTexture(WeaponResources.bolter_tex);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);

		GunGL.rotate(180, 0, 1, 0);

		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		GunGL.rotate(recoil[0] * 5, 1, 0, 0);
		GunGL.translate(0, 0, recoil[0]);

		double[] tilt = HbmAnimations.getRelevantTransformation("TILT");
		GunGL.translate(0, tilt[0], 3);
		GunGL.rotate(tilt[0] * 35, 1, 0, 0);
		GunGL.translate(0, 0, -3);

		GunGL.renderPart(WeaponResources.bolter, "Body");

		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		GunGL.pushMatrix();
		GunGL.translate(0, 0, 5);
		GunGL.rotate(mag[0] * 60 * (mag[2] == 1 ? 2.5 : 1), -1, 0, 0);
		GunGL.translate(0, 0, -5);
		GunGL.renderPart(WeaponResources.bolter, "Mag");
		if(mag[2] != 1) GunGL.renderPart(WeaponResources.bolter, "Bullet");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.pushAttrib();
		GunGL.disableLighting();
		GunGL.disableCull();
		GunGL.blendFunc(770, 771, 1, 0);
		GunGL.fullbright(true);

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		String s = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) + "";
		float f3 = 0.04F;
		GunGL.translate(0.025F - (RenderHelperA.getStringWidth(s) / 2) * 0.04F, 2.11F, 2.91F);
		GunGL.scale(f3, -f3, f3);
		GunGL.rotate(45, 1, 0, 0);
		RenderHelperA.drawString(s, 0, 0, 0xff0000);

		GunGL.enableLighting();
		GunGL.popAttrib();
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, -0.75, 1.25);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-0.25, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -12.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.rotate(180, 0, 1, 0);

		GunGL.bindTexture(WeaponResources.bolter_tex);
		GunGL.renderAll(WeaponResources.bolter);
	}
}
