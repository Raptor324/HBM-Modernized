package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderQuadro}. */
public class ItemRenderQuadro extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		standardAimingTransform(stack,
				-2.5F * offset, -3.5F * offset, 2.5F * offset,
				-1.5F * offset, -3F * offset, 2.5F * offset);
	}

	protected static String label = ">> <<";

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		Player player = Minecraft.getInstance().player;
		double scale = 1.75D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] reloadPush = HbmAnimations.getRelevantTransformation("RELOAD_PUSH");
		double[] reloadRotate = HbmAnimations.getRelevantTransformation("RELOAD_ROTATE");

		GunGL.translate(0, -1, -1);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 1, 1);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.translate(0, -1, -1);
		GunGL.rotate(reloadRotate[2], 1, 0, 0);
		GunGL.translate(0, 1, 1);

		GunGL.bindTexture(WeaponResources.quadro_tex);
		GunGL.renderPart(WeaponResources.quadro, "Launcher");

		GunGL.pushMatrix();

		GunGL.translate(0, -1, 0);
		GunGL.translate(0, 3, 0);
		GunGL.rotate(reloadPush[1] * 30, 1, 0, 0);
		GunGL.translate(0, -3, 0);
		GunGL.translate(0, 0, reloadPush[0] * 3);
		GunGL.bindTexture(WeaponResources.quadro_rocket_tex);
		GunGL.renderPart(WeaponResources.quadro, "Rockets");
		GunGL.popMatrix();

		if(gun.prevAimingProgress >= 1F && gun.aimingProgress >= 1F) {

			GunGL.pushMatrix();
			GunGL.pushAttrib();
			GunGL.disableLighting();
			GunGL.disableCull();
			GunGL.blendFunc(770, 771, 1, 0);
			GunGL.fullbright(true);
			float f3 = 0.04F;
			GunGL.translate(-0.375F, 2.25F, 0.875F);
			GunGL.rotate(180D + (System.currentTimeMillis() / 2) % 360D, 0, -1, 0);
			GunGL.translate(-(RenderHelperB.getStringWidth(label) / 2) * f3, 0, 0);
			GunGL.scale(f3, -f3, f3);
			RenderHelperB.drawString(label, 0, 0, RenderHelperB.rgb(0F, 1F, 1F));
			GunGL.color(1F, 1F, 1F);

			GunGL.enableLighting();
			GunGL.popAttrib();
			GunGL.disableCull(); // GL_LIGHTING_BIT sichert den Cull-Zustand nicht
			GunGL.popMatrix();

			// Lichtwert zuruecksetzen: uebernimmt GunGL.popAttrib() (fullbright aus)
		}

		GunGL.pushMatrix();
		GunGL.translate(-1, 0.75, 6.5);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.75, 0.75, 0.75);
		this.renderMuzzleFlash(gun.lastShot[0], 150, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 7.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, -0.5, -0.25);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 4.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0, -1, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -30D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -1.125, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.quadro_tex);
		GunGL.renderPart(WeaponResources.quadro, "Launcher");

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

			GunGL.pushMatrix(); //TODO: adjust in third person, flash is too far forward/ large
			GunGL.translate(0, 0.75, 2);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(90 * shotRand, 1, 0, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			this.renderMuzzleFlash(shot, 150, 7.5);
			GunGL.popMatrix();
		}
	}
}
