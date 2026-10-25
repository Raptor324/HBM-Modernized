package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderCongoLake}. */
public class ItemRenderCongoLake extends ItemRenderWeaponBase {

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
				-1.5F * offset, -2F * offset, 1.25F * offset,
				0, -10 / 8D, 0.25);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.congolake_tex);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);

		HbmAnimations.applyRelevantTransformation("Gun");
		GunGL.renderPart(WeaponResources.congolake, "Gun");


		GunGL.pushMatrix();
		{
			HbmAnimations.applyRelevantTransformation("Pump");
			GunGL.renderPart(WeaponResources.congolake, "Pump");
		}
		GunGL.popMatrix();


		GunGL.pushMatrix();
		{
			float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
			HbmAnimations.applyRelevantTransformation("Sight");
			GunGL.translate(0, 2.125, 3);
			GunGL.rotate(aimingProgress * -90, 1, 0, 0);
			GunGL.translate(0, -2.125, -3);
			GunGL.renderPart(WeaponResources.congolake, "Sight");
		}
		GunGL.popMatrix();


		GunGL.pushMatrix();
		{
			HbmAnimations.applyRelevantTransformation("Loop");
			GunGL.renderPart(WeaponResources.congolake, "Loop");
		}
		GunGL.popMatrix();


		GunGL.pushMatrix();
		{
			HbmAnimations.applyRelevantTransformation("GuardOuter");
			GunGL.renderPart(WeaponResources.congolake, "GuardOuter");

			GunGL.pushMatrix();
			{
				HbmAnimations.applyRelevantTransformation("GuardInner");
				GunGL.renderPart(WeaponResources.congolake, "GuardInner");
			}
			GunGL.popMatrix();
		}
		GunGL.popMatrix();


		GunGL.pushMatrix();
		{
			IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
			if(ItemGunBaseNT.getLastAnim(stack, 0) != GunAnimation.INSPECT || mag.getAmount(stack, RenderHelperA.meInventory()) > 0) { //omit when inspecting and no shell is loaded

				GunGL.bindTexture(WeaponResources.casings_tex);

				HbmAnimations.applyRelevantTransformation("Shell");

				SpentCasing casing = mag.getCasing(stack, RenderHelperA.meInventory());
				int[] colors = casing != null ? casing.getColors() : new int[] { SpentCasing.COLOR_CASE_40MM };

				int shellColor = colors[0];
				GunGL.color((shellColor >> 16 & 255) / 255F, (shellColor >> 8 & 255) / 255F, (shellColor & 255) / 255F);
				GunGL.renderPart(WeaponResources.congolake, "Shell");

				int shellForeColor = colors.length > 1 ? colors[1] : colors[0];
				GunGL.color((shellForeColor >> 16 & 255) / 255F, (shellForeColor >> 8 & 255) / 255F, (shellForeColor & 255) / 255F);
				GunGL.renderPart(WeaponResources.congolake, "ShellFore");

				GunGL.color(1F, 1F, 1F);
			}
		}
		GunGL.popMatrix();

		double smokeScale = 0.25;

		GunGL.pushMatrix();
		GunGL.translate(0, 1.75, 4.25);
		double[] transform = HbmAnimations.getRelevantTransformation("Gun");
		GunGL.rotate(-transform[5], 0, 0, 1);
		GunGL.rotate(-transform[4], 0, 1, 0);
		GunGL.rotate(-transform[3], 1, 0, 0);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1.75, 4.25);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.5, 0.5, 0.5);
		renderMuzzleFlash(gun.lastShot[0], 150, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		GunGL.translate(0, -2.5, 4);
		double scale = 2.5D;
		GunGL.scale(scale, scale, scale);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 2.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0, -1.25, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -15D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -1.25, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.congolake_tex);
		GunGL.renderAll(WeaponResources.congolake);

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
			GunGL.translate(0, 1.75, 4.25);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(0.5, 0.5, 0.5);
			renderMuzzleFlash(shot, 150, 7.5);
			GunGL.popMatrix();
		}
	}
}
