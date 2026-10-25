package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderNI4NI}. */
public class ItemRenderNI4NI extends ItemRenderWeaponBase {

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
				-1.0F * offset, -1F * offset, 1F * offset,
				0, -5 / 8D, 0.125);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		int[] color = RenderHelperB.ni4niGetColors(stack);
		int dark = 0xffffff;
		int light = 0xffffff;
		int grip = 0xffffff;
		if(color != null) {
			GunGL.bindTexture(WeaponResources.n_i_4_n_i_greyscale_tex);
			dark = color[0];
			light = color[1];
			grip = color[2];
		} else {
			GunGL.bindTexture(WeaponResources.n_i_4_n_i_tex);
		}

		double scale = 0.3125D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] drum = HbmAnimations.getRelevantTransformation("DRUM");

		GunGL.translate(0, 0, -2.25);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 0, 2.25);

		GunGL.translate(0, -1, -6);
		GunGL.rotate(recoil[0], 1, 0, 0);
		GunGL.translate(0, 1, 6);

		GunGL.pushMatrix();

		GunGL.color(RenderHelperB.fr(dark), RenderHelperB.fg(dark), RenderHelperB.fb(dark));
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "FrameDark");

		GunGL.color(RenderHelperB.fr(grip), RenderHelperB.fg(grip), RenderHelperB.fb(grip));
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Grip");

		GunGL.color(RenderHelperB.fr(light), RenderHelperB.fg(light), RenderHelperB.fb(light));
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "FrameLight");

		GunGL.pushMatrix();
		GunGL.translate(0, 1.1875D, 0);
		GunGL.rotate(drum[2], 0, 0, 1);
		GunGL.translate(0, -1.1875D, 0);
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Cylinder");
		GunGL.fullbright(true);
		GunGL.color(1F, 1F, 1F);
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "CylinderHighlights");
		GunGL.fullbright(false);
		GunGL.popMatrix();

		GunGL.fullbright(true);
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Barrel");

		GunGL.disableTexture2D();
		int coinCount = RenderHelperB.ni4niGetCoinCount(stack);
		if(coinCount > 3) { GunGL.color(coinCount > 7 ? 1F : 0F, 1F, 0F); GunGL.renderPart(WeaponResources.n_i_4_n_i, "Coin1"); }
		if(coinCount > 2) { GunGL.color(coinCount > 6 ? 1F : 0F, 1F, 0F); GunGL.renderPart(WeaponResources.n_i_4_n_i, "Coin2"); }
		if(coinCount > 1) { GunGL.color(coinCount > 5 ? 1F : 0F, 1F, 0F); GunGL.renderPart(WeaponResources.n_i_4_n_i, "Coin3"); }
		if(coinCount > 0) { GunGL.color(coinCount > 4 ? 1F : 0F, 1F, 0F); GunGL.renderPart(WeaponResources.n_i_4_n_i, "Coin4"); }

		GunGL.enableTexture2D();

		GunGL.fullbright(false);

		GunGL.pushMatrix();
		GunGL.translate(0, 0.75, 4);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.125, 0.125, 0.125);
		this.renderLaserFlash(gun.lastShot[0], 75, 7.5, 0xFFFFFF);
		GunGL.popMatrix();

		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, 0.25, 3);
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
		GunGL.translate(0, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -15D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.5, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {

		GunGL.enableLighting();

		int[] color = RenderHelperB.ni4niGetColors(stack);
		int dark = 0xffffff;
		int light = 0xffffff;
		int grip = 0xffffff;
		if(color != null) {
			GunGL.bindTexture(WeaponResources.n_i_4_n_i_greyscale_tex);
			dark = color[0];
			light = color[1];
			grip = color[2];
		} else {
			GunGL.bindTexture(WeaponResources.n_i_4_n_i_tex);
		}

		GunGL.color(RenderHelperB.fr(light), RenderHelperB.fg(light), RenderHelperB.fb(light));
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "FrameLight");
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Cylinder");
		GunGL.color(RenderHelperB.fr(grip), RenderHelperB.fg(grip), RenderHelperB.fb(grip));
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Grip");
		GunGL.color(RenderHelperB.fr(dark), RenderHelperB.fg(dark), RenderHelperB.fb(dark));
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "FrameDark");
		GunGL.color(1F, 1F, 1F);
		GunGL.fullbright(true);
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "CylinderHighlights");
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Barrel");
		GunGL.disableTexture2D();
		GunGL.color(0F, 1F, 0F);
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Coin1");
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Coin2");
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Coin3");
		GunGL.renderPart(WeaponResources.n_i_4_n_i, "Coin4");
		GunGL.color(1F, 1F, 1F);
		GunGL.enableTexture2D();
		GunGL.fullbright(false);

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
			GunGL.translate(0, 0.75, 4);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(0.125, 0.125, 0.125);
			this.renderLaserFlash(shot, 75, 7.5, 0xFFFFFF);
			GunGL.popMatrix();
		}
	}
}
