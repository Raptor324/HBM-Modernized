package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderShredder}. */
public class ItemRenderShredder extends ItemRenderWeaponBase {

	protected ResourceLocation texture;

	public ItemRenderShredder(ResourceLocation texture) {
		this.texture = texture;
	}

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
				-1.5F * offset, -1.25F * offset, 1.5F * offset,
			0, -6.25 / 8D, 0.5);
	}

	protected static String label = "[> <]";

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		Player player = Minecraft.getInstance().player;
		double scale = 0.25D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] speen = HbmAnimations.getRelevantTransformation("SPEEN");
		double[] cycle = HbmAnimations.getRelevantTransformation("CYCLE");

		GunGL.translate(0, -2, -6);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 2, 6);

		GunGL.translate(0, 0, -4);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 0, 4);

		GunGL.translate(0, 0, recoil[2]);

		boolean sexy = stack.getItem() == WeaponItems.gun("gun_autoshotgun_sexy");

		if(sexy || (gun.prevAimingProgress >= 1F && gun.aimingProgress >= 1F)) {

			GunGL.pushMatrix();
			GunGL.pushAttrib();
			GunGL.disableLighting();
			GunGL.disableCull();
			GunGL.blendFunc(770, 771, 1, 0);
			GunGL.fullbright(true);
			float f3 = 0.04F;
			GunGL.translate((RenderHelperB.getStringWidth(label) / 2) * f3, 3.25F, -1.75F);
			GunGL.scale(f3, -f3, f3);
			GunGL.rotate(180D, 0, 1, 0);
			float variance = 0.9F + player.getRandom().nextFloat() * 0.1F;
			RenderHelperB.drawString(label, 0, 0, RenderHelperB.rgb(sexy ? variance : 0F, sexy ? 0F : variance, 0F));
			GunGL.color(1F, 1F, 1F);

			GunGL.enableLighting();
			GunGL.popAttrib();
			GunGL.disableCull(); // GL_LIGHTING_BIT sichert den Cull-Zustand nicht
			GunGL.popMatrix();

			// Lichtwert zuruecksetzen: uebernimmt GunGL.popAttrib() (fullbright aus)
		}

		GunGL.bindTexture(texture);

		GunGL.renderPart(WeaponResources.shredder, "Gun");

		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);
		GunGL.translate(0, -1, -0.5);
		GunGL.rotate(speen[0], 1, 0, 0);
		GunGL.translate(0, 1, 0.5);
		GunGL.renderPart(WeaponResources.shredder, "Magazine");
		GunGL.translate(0, -1, -0.5);
		GunGL.rotate(cycle[2], 0, 0, 1);
		GunGL.translate(0, 1, 0.5);
		GunGL.renderPart(WeaponResources.shredder, "Shells");
		GunGL.popMatrix();

		double smokeScale = 0.75;

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 7.5);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 7.5);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(gun.shotRand * 90, 1, 0, 0);
		GunGL.scale(0.75, 0.75, 0.75);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 0.5, 4);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(-1.5, 0, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -7.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 0, 1.5);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(texture);
		GunGL.renderAll(WeaponResources.shredder);

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
			GunGL.translate(0, 1, 7.5);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(shotRand * 90, 1, 0, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			this.renderMuzzleFlash(shot, 75, 7.5);
			GunGL.popMatrix();
		}
	}
}
