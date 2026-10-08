package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.GunItemRenderer;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.inventory.menu.WeaponTableMenu;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.inventory.gui.GUIWeaponTable}. Der Klassenname muss so bleiben:
 * {@code GunClientHooks.isWeaponTableOpen()} prueft ihn (Tooltip "akzeptiert" in {@code ItemGunBaseNT}).
 * Vorschau der Waffe per {@link ItemRenderWeaponBase#setupModTable}/{@link ItemRenderWeaponBase#renderModTable},
 * mit gedrueckter linker Maustaste im Vorschaufeld drehbar; Knopf links unten wechselt die Konfiguration.
 */
public class GUIWeaponTable extends AbstractContainerScreen<WeaponTableMenu> {

	public static ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_weapon_modifier.png");

	public double yaw = 20;
	public double pitch = -10;

	public GUIWeaponTable(WeaponTableMenu menu, Inventory player, Component title) {
		super(menu, player, title);

		this.imageWidth = 176;
		this.imageHeight = 240;
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int x, int y, float interp) {
		GuiCompat.renderBackground(this, guiGraphics, x, y, interp);
		super.render(guiGraphics, x, y, interp);

		if (leftPos + 8 <= x && leftPos + 8 + 160 > x && topPos + 18 < y && topPos + 18 + 79 >= y) {
			if (GLFW.glfwGetMouseButton(this.minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
				double distX = (leftPos + 8 + 80) - x;
				double distY = (topPos + 18 + 39.5) - y;
				yaw = distX / 80D * -180D;
				pitch = distY / 39.5D * 90D;
			}
		}

		this.renderTooltip(guiGraphics, x, y);
	}

	@Override
	public boolean mouseClicked(double x, double y, int key) {
		boolean handled = super.mouseClicked(x, y, key);

		if (leftPos + 26 <= x && leftPos + 26 + 7 > x && topPos + 111 < y && topPos + 111 + 10 >= y) {
			WeaponTableMenu container = this.menu;
			ItemStack gun = container.gun.getItem(0);
			if (!gun.isEmpty() && gun.getItem() instanceof ItemGunBaseNT) {
				int configs = ((ItemGunBaseNT) gun.getItem()).getConfigCount();
				if (configs > 1) {
					container.index++;
					container.index %= configs;
					// Original handleMouseClick(null, 0, index, 999_999) -> Vanilla-Knopfpaket
					this.minecraft.gameMode.handleInventoryButtonClick(container.containerId, container.index);
					return true;
				}
			}
		}

		return handled;
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mX, int mY) {

		Component name = Component.translatable("container.weaponsTable");
		guiGraphics.drawString(this.font, name, (this.imageWidth) / 2 - this.font.width(name) / 2, 6, 0xffffff, false);
		guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics guiGraphics, float inter, int mX, int mY) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

		WeaponTableMenu container = this.menu;
		ItemStack gun = container.gun.getItem(0);

		if (!gun.isEmpty() && gun.getItem() instanceof ItemGunBaseNT) {
			guiGraphics.blit(texture, leftPos + 35, topPos + 112, 176 + 6 * container.index, 0, 6, 8);

			ItemRenderWeaponBase renderGun = GunItemRenderer.get(gun);

			if (renderGun != null) {
				PoseStack pose = guiGraphics.pose();
				pose.pushPose();
				pose.translate(leftPos + 88, topPos + 57, 100);

				// Original: RenderHelper.enableStandardItemLighting() (Lichtrichtung um 180 X gedreht)
				Lighting.setupFor3DItems();

				pose.mulPose(Axis.YP.rotationDegrees((float) yaw));
				pose.mulPose(Axis.XP.rotationDegrees((float) pitch));

				ItemRenderWeaponBase.interp = this.minecraft.getFrameTime();
				GunGL.begin(pose, guiGraphics.bufferSource(), LightTexture.FULL_BRIGHT);
				GunGL.pushMatrix();
				renderGun.setupModTable(gun);
				renderGun.renderModTable(gun, container.index);
				GunGL.popMatrix();
				GunGL.end();
				guiGraphics.flush();

				pose.popPose();
			}
		}
	}
}
