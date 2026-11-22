package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.weapon.WeaponJointRig;
import com.hbm_m.client.render.weapon.WeaponKinematics;
import com.hbm_m.client.render.weapon.WeaponRenderRegistry;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderMAS36}. */
public class ItemRenderMAS36 extends ItemRenderWeaponBase {

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * (isScoped(stack) ? 0.66F : 0.33F));
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;

		if(isScoped(stack)) {
			standardAimingTransform(stack,
				-1.5F * offset, -1.25F * offset, 1.75F * offset,
				-0.2, -5.875 / 8D, 1.125);
		} else {
			standardAimingTransform(stack,
					-1.5F * offset, -1.25F * offset, 1.75F * offset,
					0, -4.6825 / 8D, 0.75);
		}
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		boolean isScoped = isScoped(stack);
		if(isScoped && ItemGunBaseNT.prevAimingProgress == 1 && ItemGunBaseNT.aimingProgress == 1) return;

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		GunGL.bindTexture(WeaponResources.mas36_tex);
		double scale = 0.375D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] lift = HbmAnimations.getRelevantTransformation("LIFT");
		double[] stock = HbmAnimations.getRelevantTransformation("STOCK");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] boltTurn = HbmAnimations.getRelevantTransformation("BOLT_TURN");
		double[] boltPull = HbmAnimations.getRelevantTransformation("BOLT_PULL");
		double[] bullet = HbmAnimations.getRelevantTransformation("BULLET");
		double[] showClip = HbmAnimations.getRelevantTransformation("SHOW_CLIP");
		double[] clip = HbmAnimations.getRelevantTransformation("CLIP");
		double[] bullets = HbmAnimations.getRelevantTransformation("BULLETS");
		double[] stab = HbmAnimations.getRelevantTransformation("STAB");

		GunGL.translate(0, -3, -3);
		GunGL.rotate(equip[0], 1, 0, 0);
		GunGL.rotate(lift[0], 1, 0, 0);
		GunGL.translate(0, 3, 3);

		GunGL.translate(stab[0], stab[1], stab[2]);

		GunGL.translate(0, 0, recoil[2]);

		WeaponRenderRegistry.renderPart(WeaponResources.mas36, "Gun", GunGL.pose());
		if(hasBayonet(stack)) WeaponRenderRegistry.renderPart(WeaponResources.mas36, "Bayonet", GunGL.pose());

		WeaponJointRig rig = WeaponRenderRegistry.getRig(WeaponResources.mas36);
		WeaponKinematics.renderPartWithJoint(rig, "Stock", GunGL.pose(), (float) stock[0]);
		WeaponKinematics.renderPartWithChain(rig, "Bolt", GunGL.pose(), new float[]{(float) boltTurn[2], (float) boltPull[2]});

		WeaponKinematics.scoped(GunGL.pose(), ps -> {
			WeaponKinematics.applyVectorTranslate(ps, (float) bullet[0], (float) bullet[1], (float) bullet[2]);
			WeaponRenderRegistry.renderPart(WeaponResources.mas36, "Bullet", ps);
		});

		if(isScoped) GunGL.renderPart(WeaponResources.mas36, "Scope");

		if(showClip[0] != 0) {
			GunGL.pushMatrix();
			GunGL.translate(clip[0], clip[1], clip[2]);
			GunGL.renderPart(WeaponResources.mas36, "Clip");
			GunGL.popMatrix();
			GunGL.pushMatrix();
			// GL_CLIP_PLANE0 per CPU-Beschnitt (RenderHelperB)
			if(bullets[0] == 0) RenderHelperB.enableClipPlane0();
			RenderHelperB.clipPlane0(0, 1, 0, -0.5);
			GunGL.translate(bullets[0], bullets[1], bullets[2]);
			RenderHelperB.renderPart(WeaponResources.mas36, "Bullets");
			RenderHelperB.disableClipPlane0();
			GunGL.popMatrix();
		}

		double smokeScale = 0.25;

		GunGL.pushMatrix();
		GunGL.translate(0, 1.125, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.scale(smokeScale, smokeScale, smokeScale);
		this.renderSmokeNodes(gun.getConfig(stack, 0).smokeNodes, 1D);
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(0, 1, 8);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.rotate(90 * gun.shotRand, 1, 0, 0);
		GunGL.scale(0.5, 0.5, 0.5);
		this.renderMuzzleFlash(gun.lastShot[0], 75, 7.5);
		GunGL.popMatrix();
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		setupThirdPerson(stack, 1.5D, 0, 0.5, 3);
	}

	@Override
	public void setupInv(ItemStack stack) {
		setupInv(stack, 1.5D, -0.5, 0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		setupModTable(stack, -7.5D, 0, -0.25, -2.5);
	}

	@Override
	public void renderModTable(ItemStack stack, int index) {
		GunGL.enableLighting();
		GunGL.bindTexture(WeaponResources.mas36_tex);
		GunGL.renderPart(WeaponResources.mas36, "Gun");
		GunGL.renderPart(WeaponResources.mas36, "Stock");
		GunGL.renderPart(WeaponResources.mas36, "Bolt");
		if(isScoped(stack)) GunGL.renderPart(WeaponResources.mas36, "Scope");
		if(hasBayonet(stack)) GunGL.renderPart(WeaponResources.mas36, "Bayonet");
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();
		GunGL.bindTexture(WeaponResources.mas36_tex);
		GunGL.renderPart(WeaponResources.mas36, "Gun");
		GunGL.renderPart(WeaponResources.mas36, "Stock");
		GunGL.renderPart(WeaponResources.mas36, "Bolt");
		if(isScoped(stack)) GunGL.renderPart(WeaponResources.mas36, "Scope");
		if(type != ItemRenderType.EQUIPPED) GunGL.translate(0, -1, -6);
		if(hasBayonet(stack)) GunGL.renderPart(WeaponResources.mas36, "Bayonet");
		renderEquippedMuzzleFlash(type, stack, data, 0, 1, 8, 0.5D, 75, 10);
	}

	public boolean isScoped(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE);
	}

	public boolean hasBayonet(ItemStack stack) {
		return XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_MAS_BAYONET);
	}
}
