package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.impl.IPAMelee;
import com.hbm_m.item.weapon.sedna.impl.IPAWeaponsProvider;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderPAMelee}. */
public class ItemRenderPAMelee extends ItemRenderWeaponBase {

	@Override public boolean isAkimbo(LivingEntity entity) { return true; }

	@Override protected float getSwayMagnitude(ItemStack stack) { return 2F; }
	@Override protected float getSwayPeriod(ItemStack stack) { return 0.5F; }

	@Override
	public void setupFirstPerson(ItemStack stack) {
		IPAMelee component = IPAWeaponsProvider.getMeleeComponentClient();
		if(component != null) component.setupFirstPerson(stack);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {
		IPAMelee component = IPAWeaponsProvider.getMeleeComponentClient();
		if(component != null) component.renderFirstPerson(stack);
	}

	@Override public void setupThirdPerson(ItemStack stack) { }
	@Override public void setupThirdPersonAkimbo(ItemStack stack) { }

	@Override
	public void setupInv(ItemStack stack) {
		GunGL.scale(1, 1, -1);
		GunGL.translate(8, 8, 0);
		double scale = 2.5D;
		GunGL.scale(scale, scale, scale);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -12.5D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -0.5, 1);
	}

	@Override
	public void renderInv(ItemStack stack) {

		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.ncrpa_arm);

		GunGL.pushMatrix();
		double scale = 0.3125D;
		GunGL.scale(scale, scale, scale);

		GunGL.rotate(135, 0, 0, 1);
		GunGL.rotate(135, 0, 1, 0);
		GunGL.translate(0, -5.5, 0);
		GunGL.translate(-3.5, 0, 0);
		GunGL.renderPart(WeaponResources.armor_ncr, "Leftarm");
		GunGL.translate(7, 1, -1);
		GunGL.renderPart(WeaponResources.armor_ncr, "RightArm");
		GunGL.popMatrix();

	}

	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		if(type == ItemRenderType.EQUIPPED) return;

		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.ncrpa_arm);

		GunGL.pushMatrix();
		double scale = 0.3125D;
		GunGL.scale(scale, scale, scale);

		GunGL.rotate(90, 1, 0, 0);
		GunGL.translate(0, -5.5, 0);
		GunGL.translate(-2, 0, 0);
		GunGL.renderPart(WeaponResources.armor_ncr, "Leftarm");
		GunGL.translate(4, 0, 0);
		GunGL.renderPart(WeaponResources.armor_ncr, "RightArm");
		GunGL.popMatrix();

	}
}
