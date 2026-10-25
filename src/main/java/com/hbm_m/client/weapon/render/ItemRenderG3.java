package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderG3}. */
public class ItemRenderG3 extends ItemRenderWeaponBase {

	public ResourceLocation texture;

	public ItemRenderG3(ResourceLocation texture) {
		this.texture = texture;
	}

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		boolean isScoped = this.isScoped(stack);
		float offset = 0.8F;
		standardAimingTransform(stack,
				-1.25F * offset, -1F * offset, 2.75F * offset,
			0, isScoped ? (-5.53125 / 8D) : (-3.5625 / 8D), isScoped ? 1.46875 : 1.75);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		boolean isScoped = this.isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(getTexture(stack));
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] mag = HbmAnimations.getRelevantTransformation("MAG");
		double[] speen = HbmAnimations.getRelevantTransformation("SPEEN");
		double[] bolt = HbmAnimations.getRelevantTransformation("BOLT");
		double[] plug = HbmAnimations.getRelevantTransformation("PLUG");
		double[] handle = HbmAnimations.getRelevantTransformation("HANDLE");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");

		GunGL.translate(0, -2, -6);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.translate(0, 2, 6);

		GunGL.translate(0, 0, -4);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 0, 4);

		GunGL.translate(0, 0, recoil[2]);

		GunGL.renderPart(WeaponResources.g3, "Rifle");
		if(hasStock(stack)) GunGL.renderPart(WeaponResources.g3, "Stock");
		boolean silenced = hasSilencer(stack);
		if(!silenced) GunGL.renderPart(WeaponResources.g3, "Flash_Hider");
		GunGL.renderPart(WeaponResources.g3, "Trigger");

		GunGL.bindTexture(getTexture(stack));
		GunGL.pushMatrix();
		GunGL.translate(mag[0], mag[1], mag[2]);
		GunGL.translate(0, -1.75, -0.5);
		GunGL.rotate(speen[2], 0, 0, 1);
		GunGL.rotate(speen[1], 0, 1, 0);
		GunGL.translate(0, 1.75, 0.5);
		GunGL.renderPart(WeaponResources.g3, "Magazine");
		if(bullet[0] == 0) GunGL.renderPart(WeaponResources.g3, "Bullet");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0, bolt[2]);
		GunGL.renderPart(WeaponResources.g3, "Guide_And_Bolt");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 0.625, plug[2]);
		GunGL.rotate(handle[2], 0, 0, 1);
		GunGL.translate(0, -0.625, 0);
		GunGL.renderPart(WeaponResources.g3, "Plug");

		GunGL.translate(0, 0.625, 5.25);
		GunGL.rotate(22.5, 0, 0, 1);
		GunGL.rotate(handle[1], 0, 1, 0);
		GunGL.rotate(-22.5, 0, 0, 1);
		GunGL.translate(0, -0.625, -5.25);
		GunGL.renderPart(WeaponResources.g3, "Handle");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, -0.875, -3.5);
		GunGL.rotate(-30 * (1 - ItemGunBaseNT.getMode(stack, 0)), 1, 0, 0);
		GunGL.translate(0, 0.875, 3.5);
		GunGL.renderPart(WeaponResources.g3, "Selector");
		GunGL.popMatrix();

		if(silenced || isScoped) {
			GunGL.bindTexture(WeaponResources.g3_attachments);
			if(silenced) GunGL.renderPart(WeaponResources.g3, "Silencer");
			if(isScoped) GunGL.renderPart(WeaponResources.g3, "Scope");
		}

		if(!silenced) {
			double smokeScale = 0.75;

			GunGL.pushMatrix();
			GunGL.translate(0, 0, 13);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.scale(smokeScale, smokeScale, smokeScale);
			renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 0.5D);
			GunGL.popMatrix();

			GunGL.pushMatrix();
			GunGL.translate(0, 0, 12);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(-25 + gun.shotRand * 10, 1, 0, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			renderMuzzleFlash(gun.lastShot[0], 75, 10);
			GunGL.popMatrix();
		}
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 1D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(0, 2, 4);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		if(hasStock(stack)) {
			double scale = 0.875D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(hasSilencer(stack) ? 50 : 45, 0, 1, 0);
			GunGL.translate(hasSilencer(stack) ? 0.75 : -0.5, 0.5, 0);
		} else {
			double scale = 1.125D;
			GunGL.scale(scale, scale, scale);
			GunGL.rotate(25, 1, 0, 0);
			GunGL.rotate(hasSilencer(stack) ? 55 : 45, 0, 1, 0); //preserves proportions whilst limiting size
			GunGL.translate(2.5, 0.5, 0);
		}
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, 0.5, -0.5);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		boolean silenced = hasSilencer(stack);
		boolean isScoped = this.isScoped(stack);

		GunGL.bindTexture(getTexture(stack));
		GunGL.renderPart(WeaponResources.g3, "Rifle");
		if(hasStock(stack)) GunGL.renderPart(WeaponResources.g3, "Stock");
		GunGL.renderPart(WeaponResources.g3, "Magazine");
		if(!silenced)GunGL.renderPart(WeaponResources.g3, "Flash_Hider");
		GunGL.renderPart(WeaponResources.g3, "Guide_And_Bolt");
		GunGL.renderPart(WeaponResources.g3, "Handle");
		GunGL.renderPart(WeaponResources.g3, "Trigger");

		GunGL.pushMatrix();
		GunGL.translate(0, -0.875, -3.5);
		GunGL.rotate(-30, 1, 0, 0);
		GunGL.translate(0, 0.875, 3.5);
		GunGL.renderPart(WeaponResources.g3, "Selector");
		GunGL.popMatrix();

		if(silenced || isScoped) {
			GunGL.bindTexture(WeaponResources.g3_attachments);
			if(silenced) GunGL.renderPart(WeaponResources.g3, "Silencer");
			if(isScoped) GunGL.renderPart(WeaponResources.g3, "Scope");
		}
		//third-person muzzle flashes
		if(type == ItemRenderType.EQUIPPED && !silenced) {
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
			GunGL.translate(0, 0, 12);
			GunGL.rotate(90, 0, 1, 0);
			GunGL.rotate(-25 + shotRand * 10, 1, 0, 0);
			GunGL.scale(0.75, 0.75, 0.75);
			renderMuzzleFlash(shot, 75, 10);
			GunGL.popMatrix();
		}
	}

	public boolean hasStock(ItemStack stack) {
		return !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_STOCK);
	}

	public boolean hasSilencer(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_g3_zebra") || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
	}

	public boolean isScoped(ItemStack stack) {
		return stack.getItem() == WeaponItems.gun("gun_g3_zebra") || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}

	public ResourceLocation getTexture(ItemStack stack) {
		if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_FURNITURE_GREEN)) return WeaponResources.g3_green_tex;
		if(XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_FURNITURE_BLACK)) return WeaponResources.g3_black_tex;
		return texture;
	}
}
