package com.hbm_m.client.weapon.render;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.render.anim.HbmAnimations;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.MovingObjectPosition;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ItemRenderFolly}. */
public class ItemRenderFolly extends ItemRenderWeaponBase {

	public static long timeAiming;
	public static boolean jingle = false;
	public static boolean wasAiming = false;

	@Override
	protected float getTurnMagnitude(ItemStack stack) { return ItemGunBaseNT.getIsAiming(stack) ? 2F : 2.5F; }

	@Override
	public float getViewFOV(ItemStack stack, float fov) {
		float aimingProgress = ItemGunBaseNT.prevAimingProgress + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress) * interp;
		return  fov * (1 - aimingProgress * 0.33F);
	}

	@Override
	public void setupFirstPerson(ItemStack stack) {
		GunGL.translate(0, 0, 0.875);

		float offset = 0.8F;
		float aim = 0.75F;
		standardAimingTransform(stack,
				-2.5F * offset, -1.5F * offset, 2.75F * offset,
				-2 * aim, -1 * aim, 2.25F * offset);
	}

	@Override
	public void renderFirstPerson(ItemStack stack) {

		ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
		Player player = Minecraft.getInstance().player;
		GunGL.bindTexture(WeaponResources.folly_tex);
		double scale = 0.75D;
		GunGL.scale(scale, scale, scale);

		double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
		double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
		double[] load = HbmAnimations.getRelevantTransformation("LOAD");
		double[] shell = HbmAnimations.getRelevantTransformation("SHELL");
		double[] screw = HbmAnimations.getRelevantTransformation("SCREW");
		double[] breech = HbmAnimations.getRelevantTransformation("BREECH");

		GunGL.translate(0, 1, -4);
		GunGL.rotate(-equip[0], 1, 0, 0);
		GunGL.translate(0, -1, 4);

		GunGL.translate(0, -2, -2);
		GunGL.rotate(load[0], 1, 0, 0);
		GunGL.translate(0, 2, 2);

		GunGL.renderPart(WeaponResources.folly, "Cannon");

		GunGL.pushMatrix();
		GunGL.translate(recoil[0], recoil[1], recoil[2]);
		GunGL.renderPart(WeaponResources.folly, "Barrel");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(shell[0], shell[1], shell[2]);
		GunGL.renderPart(WeaponResources.folly, "Shell");
		GunGL.popMatrix();

		GunGL.pushMatrix();
		GunGL.translate(breech[0], breech[1], breech[2]);
		GunGL.renderPart(WeaponResources.folly, "Breech");
		GunGL.translate(0, 1, 0);
		GunGL.rotate(screw[2], 0, 0, 1);
		GunGL.translate(0, -1, 0);
		GunGL.renderPart(WeaponResources.folly, "Cog");
		GunGL.popMatrix();


		boolean isAiming = ItemGunBaseNT.prevAimingProgress >= 1F && ItemGunBaseNT.aimingProgress >= 1F;
		if(isAiming & !wasAiming) timeAiming = System.currentTimeMillis();

		if(isAiming) {

			String splash = getBootSplash();

			if(!jingle && !splash.isEmpty()) {
				player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:weapon.fire.vstar"), SoundSource.PLAYERS, 0.5F, 1F, false);
				jingle = true;
			}

			GunGL.pushMatrix();
			GunGL.pushAttrib();
			GunGL.disableLighting();
			GunGL.disableCull();
			GunGL.blendFunc(770, 771, 1, 0);
			GunGL.fullbright(true);
			int fontHeight = Minecraft.getInstance().font.lineHeight;
			float variance = 0.85F + player.getRandom().nextFloat() * 0.15F;

			if(System.currentTimeMillis() - timeAiming > 5000 && load[0] == 0) {
				IMagazine mag = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
				String msg = mag.getAmount(stack, player.getInventory()) > 0 ? "+" : "No ammo";
				GunGL.pushMatrix();
				float crosshairSize = 0.01F;
				GunGL.translate((RenderHelperA.getStringWidth(msg) / 2) * crosshairSize + 2, 1F + fontHeight * crosshairSize / 2F, -2.75F);
				GunGL.scale(crosshairSize, -crosshairSize, crosshairSize);
				GunGL.rotate(180D, 0, 1, 0);
				RenderHelperA.drawString(msg, 0, 0, RenderHelperA.colorRGB(variance, variance * 0.5F, 0F));
				GunGL.popMatrix();
			}

			GunGL.pushMatrix();
			float splashSize = 0.02F;
			GunGL.translate((RenderHelperA.getStringWidth(splash) / 2) * splashSize + 2, 1F + fontHeight * splashSize / 2F, -2.75F);
			GunGL.scale(splashSize, -splashSize, splashSize);
			GunGL.rotate(180D, 0, 1, 0);
			RenderHelperA.drawString(splash, 0, 0, RenderHelperA.colorRGB(variance, variance * 0.5F, 0F));
			GunGL.popMatrix();

			List<String> tty = getTTY();
			if(!tty.isEmpty()) {
				GunGL.pushMatrix();
				float fontSize = 0.005F;
				GunGL.translate(2.5F, 1.375F, -2.75F);
				GunGL.scale(fontSize, -fontSize, fontSize);
				GunGL.rotate(180D, 0, 1, 0);
				for(String line : tty) {
					RenderHelperA.drawString(line, 0, 0, RenderHelperA.colorRGB(variance, variance * 0.5F, 0F));
					GunGL.translate(0, (fontHeight + 2), 0);
				}
				GunGL.popMatrix();
			}

			GunGL.color(1F, 1F, 1F);

			GunGL.enableLighting();
			GunGL.enableCull();
			GunGL.popAttrib();
			GunGL.popMatrix();

			// Original: Lichtkarte wieder auf die Welthelligkeit
			GunGL.fullbright(false);
		} else {
			jingle = false;
		}

		wasAiming = isAiming;
	}

	@Override
	public void setupThirdPerson(ItemStack stack) {
		super.setupThirdPerson(stack);
		double scale = 3D;
		GunGL.scale(scale, scale, scale);
		GunGL.translate(-0.25, 0.5, 3);

	}

	@Override
	public void setupInv(ItemStack stack) {
		super.setupInv(stack);
		double scale = 1.25D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(25, 1, 0, 0);
		GunGL.rotate(45, 0, 1, 0);
		GunGL.translate(0, -0.5, 0);
	}

	@Override
	public void setupModTable(ItemStack stack) {
		double scale = -8.75D;
		GunGL.scale(scale, scale, scale);
		GunGL.rotate(90, 0, 1, 0);
		GunGL.translate(0, -1, 0);
	}

	@Override
	public void renderOther(ItemStack stack, ItemRenderType type, Object... data) {
		GunGL.enableLighting();

		GunGL.bindTexture(WeaponResources.folly_tex);
		GunGL.renderAll(WeaponResources.folly);
	}

	public static String getBootSplash() {
		long now = System.currentTimeMillis();
		if(timeAiming + 5000 < now) return "";
		if(timeAiming + 3000 > now) return "";
		int splashIndex = (int)((now - timeAiming - 3000) * 35 / 2000) - 10;
		//use the StringBuilder this, can't eat the drywall that, this used to be a free country
		char[] letters = "VStarOS".toCharArray();
		String splash = "";
		for(int i = 0; i < letters.length; i++) {
			if(i < splashIndex - 1) splash += ChatFormatting.LIGHT_PURPLE;
			if(i == splashIndex - 1) splash += ChatFormatting.AQUA;
			if(i == splashIndex) splash += ChatFormatting.WHITE;
			if(i == splashIndex + 1) splash += ChatFormatting.AQUA;
			if(i == splashIndex + 2) splash += ChatFormatting.LIGHT_PURPLE;
			if(i > splashIndex + 2) splash += ChatFormatting.BLACK;
			splash += letters[i];
		}
		return splash;
	}

	public static List<String> getTTY() {
		List<String> tty = new ArrayList<>();
		long now = System.currentTimeMillis();
		int time = (int)((now - timeAiming));
		if(time < 3000) {
			if(time > 250) tty.add(ChatFormatting.GREEN + "POST successful - Code 0");
			if(time > 500) tty.add(ChatFormatting.GREEN + "8,388,608 bytes of RAM installed");
			if(time > 500) tty.add(ChatFormatting.GREEN + "5,187,427 bytes available");
			if(time > 750) tty.add(ChatFormatting.GREEN + "Reticulating splines...");
			if(time > 1500) tty.add(ChatFormatting.GREEN + "No keyboard found!");
			if(time > 2000) tty.add(ChatFormatting.GREEN + "Booting from /dev/sda1...");
		}
		if(time > 5000) {
			Player player = RenderHelperA.me();
			MovingObjectPosition mop = RenderHelperA.getMouseOver(player, 250);
			String target = ChatFormatting.GREEN + "Target: ";
			if(mop.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) target += "N/A";
			if(mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) target += mop.blockX + "/" + mop.blockY + "/" + mop.blockZ;
			if(mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) target += mop.entityHit.getName().getString();
			tty.add(target);
			tty.add(ChatFormatting.GREEN + "Angle: " + ((int)(-player.getXRot() * 100) / 100D));
		}
		return tty;
	}
}
