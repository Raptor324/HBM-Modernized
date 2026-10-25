package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.implementations.TrinketRenderer;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderTeslaCannon}. */
public class ItemRenderTeslaCannon extends ItemRenderWeaponBase {

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
				-1.75F * offset, -0.5F * offset, 1.75F * offset,
				-1.3125F * offset, 0F * offset, -0.5F * offset);
	}

	protected static String label = "AUTO";

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.tesla_cannon_tex);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");
		double[] count = HbmAnimations.getRelevantTransformation("COUNT");
		double[] yomi = HbmAnimations.getRelevantTransformation("YOMI");
		double[] squeeze = HbmAnimations.getRelevantTransformation("SQUEEZE");

		GunGL.translate(0, -2, -2);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 2, 2);

		GunGL.translate(0, 0, recoil[2]);
		GunGL.rotate(recoil[2] * 2, 1, 0, 0);

		int amount = Math.max((int) count[0], gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, Minecraft.getInstance().player.getInventory()));

		GunGL.renderPart(WeaponResources.tesla_cannon, "Gun");
		GunGL.renderPart(WeaponResources.tesla_cannon, "Extension");

		double cogAngle = cycle[2];

		GunGL.pushMatrix();
		GunGL.translate(0, -1.625, 0);
		GunGL.rotate(cogAngle, 0, 0, 1);
		GunGL.translate(0, 1.625, 0);
		GunGL.renderPart(WeaponResources.tesla_cannon, "Cog");
		GunGL.popMatrix();

		GunGL.pushMatrix();

		GunGL.translate(0, -1.625, 0);
		GunGL.rotate(cogAngle, 0, 0, 1);
		GunGL.translate(0, 1.625, 0);

		for(int i = 0; i < Math.min(amount, 8); i++) {
			GunGL.renderPart(WeaponResources.tesla_cannon, "Capacitor");

			if(i < 4) {
				GunGL.translate(0, -1.625, 0);
				GunGL.rotate(-22.5, 0, 0, 1);
				GunGL.translate(0, 1.625, 0);
			} else {
				if(i == 4) {
					GunGL.translate(0, -1.625, 0);
					GunGL.rotate(-cogAngle, 0, 0, 1);
					GunGL.translate(0, 1.625, 0);
					GunGL.translate(-cogAngle * 0.5 / 22.5, 0, 0);
				}
				GunGL.translate(0.5, 0, 0);
			}
		}
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(yomi[0], yomi[1], yomi[2]);
		GunGL.rotate(135, 0, 1, 0);
		GunGL.scale(squeeze[0], squeeze[1], squeeze[2]);
		GunGL.bindTexture(TrinketRenderer.YOMI_TEX);
		GunGL.renderAll(TrinketRenderer.YOMI);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 1.5, 1);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.tesla_cannon_tex);

		GunGL.renderPart(WeaponResources.tesla_cannon, "Gun");
		GunGL.renderPart(WeaponResources.tesla_cannon, "Extension");
		GunGL.renderPart(WeaponResources.tesla_cannon, "Cog");

		GunGL.pushMatrix();
		for(int i = 0; i < 10; i++) {
			GunGL.renderPart(WeaponResources.tesla_cannon, "Capacitor");

			if(i < 4) {
				GunGL.translate(0, -1.625, 0);
				GunGL.rotate(-22.5, 0, 0, 1);
				GunGL.translate(0, 1.625, 0);
			} else {
				GunGL.translate(0.5, 0, 0);
			}
		}
		GunGL.popMatrix();
	}
}
