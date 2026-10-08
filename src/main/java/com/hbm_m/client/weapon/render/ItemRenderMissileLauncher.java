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

/** 1:1 {@code ItemRenderMissileLauncher}. */
public class ItemRenderMissileLauncher extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

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
		Player player = Minecraft.getInstance().player;
		GunGL.bindTexture(WeaponResources.missile_launcher_tex);
		double scale = 0.5D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] barrel = HbmAnimations.getRelevantTransformation("BARREL");
		double[] open = HbmAnimations.getRelevantTransformation("OPEN");
		double[] missile = HbmAnimations.getRelevantTransformation("MISSILE");

		GunGL.translate(0, -2, -2);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 2, 2);

		GunGL.renderPart(WeaponResources.missile_launcher, "Launcher");

		GunGL.pushMatrix();

		GunGL.translate(0, 0.25, 1.6875);
		GunGL.rotate(open[0], 1, 0, 0);
		GunGL.translate(0, -0.25, -1.6875);

		GunGL.renderPart(WeaponResources.missile_launcher, "Front");

		GunGL.pushMatrix();
		GunGL.translate(0, 0, barrel[2]);
		GunGL.renderPart(WeaponResources.missile_launcher, "Barrel");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(missile[0], missile[1], missile[2]);
		GunGL.renderPart(WeaponResources.missile_launcher, "Missile");
		GunGL.popMatrix();

		GunGL.popMatrix();

		if(gun.prevAimingProgress >= 1F && gun.aimingProgress >= 1F) {

			GunGL.pushMatrix();
			GunGL.pushAttrib();
			GunGL.disableLighting();
			GunGL.disableCull();
			GunGL.blendFunc(770, 771, 1, 0);
			GunGL.fullbright(true);
			float f3 = 0.04F;
			GunGL.translate(0.9375F, 2.25F, -0.5625F + (RenderHelperB.getStringWidth(label) / 2) * f3);
			GunGL.scale(f3, -f3, f3);
			GunGL.rotate(90D, 0, 1, 0);
			float variance = 0.7F + player.getRandom().nextFloat() * 0.3F;
			RenderHelperB.drawString(label, 0, 0, RenderHelperB.rgb(variance, 0F, 0F));
			GunGL.color(1F, 1F, 1F);

			GunGL.enableLighting();
			GunGL.popAttrib();
			GunGL.disableCull(); // GL_LIGHTING_BIT sichert den Cull-Zustand nicht
			GunGL.popMatrix();

			// Lichtwert zuruecksetzen: uebernimmt GunGL.popAttrib() (fullbright aus)
		}

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 6.75);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(gun.shotRand * 90, 1, 0, 0);
		GunGL.scale(0.75, 0.75, 0.75);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 2.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, -0.5, -2);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -10D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.missile_launcher_tex);
		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();

		GunGL.renderPart(WeaponResources.missile_launcher, "Launcher");
		GunGL.renderPart(WeaponResources.missile_launcher, "Barrel");
		GunGL.renderPart(WeaponResources.missile_launcher, "Front");
		if(gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null) > 0) GunGL.renderPart(WeaponResources.missile_launcher, "Missile");

		if(type == ItemRenderType.EQUIPPED) {
			LivingEntity ent = (LivingEntity) data[1];
			long shot;
			double shotRand = 0;
			if(ent == Minecraft.getInstance().player) {
				shot = gun.lastShot[0];
				shotRand = gun.shotRand;
			} else {
				shot = ItemRenderWeaponBase.flashMap.getOrDefault(ent, (long) -1);
				if(shot < 0) return;
			}

			GunGL.pushMatrix();
			GunGL.translate(0, 1, 6.75);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(shotRand * 90, 1, 0, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			this.renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}
}
