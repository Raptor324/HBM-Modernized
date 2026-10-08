package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.inventory.menu.MachinePressMenu;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIMachinePress} (1.7.10): 176x214, Brennlicht, Stempelpfeil, Drehzahl-Zeiger
 * ({@code GUIElements.drawSmoothGauge}) und die neun Ablageslots unter der Presse.
 */
public class GUIMachinePress extends GuiInfoScreen<MachinePressMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/processing/gui_press.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/processing/gui_press.png");
            //?}

    public GUIMachinePress(MachinePressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);

        int maxSpeed = menu.getMaxSpeed();
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 25, 16, 18, 18, mouseX, mouseY,
                Component.literal((maxSpeed != 0 ? menu.getSpeed() * 100 / maxSpeed : 0) + "%"));
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 25, 34, 18, 18, mouseX, mouseY,
                Component.literal((menu.getBurnTime() / 200) + " operations left"));

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 5, 0xffffff, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (menu.getBurnTime() >= 20) {
            guiGraphics.blit(TEXTURE, leftPos + 26, topPos + 36, 0, 214, 14, 14);
        }

        int maxPress = menu.getMaxPress();
        int k = maxPress > 0 ? menu.getPress() * 16 / maxPress : 0;
        guiGraphics.blit(TEXTURE, leftPos + 79, topPos + 35, 15, 214, 18, k);

        int maxSpeed = menu.getMaxSpeed();
        double i = maxSpeed > 0 ? (double) menu.getSpeed() / (double) maxSpeed : 0D;
        GuiGaugeNeedle.draw(guiGraphics, leftPos + 34, topPos + 25, i, 5, 2, 1, 0x7f0000);
    }
}
