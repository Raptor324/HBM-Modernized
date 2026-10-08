package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderStinger}. */
public class ItemRenderStinger extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.5F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-3.75F * offset, -9F * offset, -3.5F * offset,
				-2.625F * offset, -6.5, -8.5F);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		if(ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.stinger_tex);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
		double[] rocket = HbmAnimations.getRelevantTransformation("ROCKET");

		GunGL.translate(0, -1, -1);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 1, 1);

		GunGL.translate(0, -4, -3);
		GunGL.rotate(reload[0], 1, 0, 0);
		GunGL.translate(0, 4, 3);

		GunGL.pushMatrix();
		GunGL.rotate(180, 0, 1, 0);
		GunGL.renderAll(WeaponResources.stinger);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.bindTexture(WeaponResources.panzerschreck_tex);
		GunGL.translate(rocket[0], rocket[1] + 3.5, rocket[2] - 3);
		GunGL.renderPart(WeaponResources.panzerschreck, "Rocket");

		GunGL.pushMatrix();
		GunGL.pushAttrib();
		GunGL.disableLighting();
		GunGL.disableCull();
		GunGL.blendFunc(770, 771, 1, 0);
		GunGL.fullbright(true);

		String label = "Not accurate";
		float f3 = 0.04F;
		GunGL.translate(0.025F, -0.5F, (RenderHelperB.getStringWidth(label) / 2) * f3 - 3);
		GunGL.scale(f3, -f3, f3);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(45, -1, 0, 0);
		RenderHelperB.drawString(label, 0, 0, 0xff0000);

		GunGL.enableLighting();
		GunGL.popAttrib();
		GunGL.disableCull(); // GL_LIGHTING_BIT sichert den Cull-Zustand nicht
		GunGL.popMatrix();

		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, 6.5);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.75, 0.75, 0.75);
		this.renderMuzzleFlash(gun.lastShot[0], 150, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, -2.5, -3.5);
		GunGL.rotate(180, 0, 1, 0);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.0625D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(225, 0, 1, 0);
		GunGL.translate(0.25, -2.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(-90, 0, 1, 0);
		GunGL.translate(0, -4, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.enableCull();
		GunGL.bindTexture(WeaponResources.stinger_tex);
		GunGL.renderAll(WeaponResources.stinger);

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
			GunGL.translate(0, 3.5, -10.3795);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			this.renderMuzzleFlash(shot, 150, 10);
			GunGL.popMatrix();
		}
	}
}
