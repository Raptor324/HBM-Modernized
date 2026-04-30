package com.hbm_m.inventory.gui;

import com.hbm_m.block.entity.machines.MachineZirnoxBlockEntity;
import com.hbm_m.inventory.menu.MachineZirnoxMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineZirnox extends GuiInfoScreen<MachineZirnoxMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_zirnox.png");

    private final MachineZirnoxBlockEntity zirnox;

    public GUIMachineZirnox(MachineZirnoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.zirnox = menu.getBlockEntity();
        this.imageWidth = 203;
        this.imageHeight = 256;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int s = zirnox.getGaugeScaled(6, 0);
        guiGraphics.blit(TEXTURE, this.leftPos + 160, this.topPos + 108, 238, 0 + 12 * s, 18, 12);

        int c = zirnox.getGaugeScaled(6, 1);
        guiGraphics.blit(TEXTURE, this.leftPos + 142, this.topPos + 108, 238, 0 + 12 * c, 18, 12);

        int w = zirnox.getGaugeScaled(6, 2);
        guiGraphics.blit(TEXTURE, this.leftPos + 178, this.topPos + 108, 238, 0 + 12 * w, 18, 12);

        int h = zirnox.getGaugeScaled(12, 3);
        guiGraphics.blit(TEXTURE, this.leftPos + 160, this.topPos + 33, 220, 0 + 18 * h, 18, 17);

        int p = zirnox.getGaugeScaled(12, 4);
        guiGraphics.blit(TEXTURE, this.leftPos + 178, this.topPos + 33, 220, 0 + 18 * p, 18, 17);

        if (zirnox.isOn) {
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 7 + 36 * x, this.topPos + 15 + 36 * y, 238, 238, 18, 18);
                }
            }
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 25 + 36 * x, this.topPos + 33 + 36 * y, 238, 238, 18, 18);
                }
            }
            guiGraphics.blit(TEXTURE, this.leftPos + 142, this.topPos + 15, 220, 238, 18, 18);
        }

        this.drawInfoPanel(guiGraphics, -16, 36, PanelType.LARGE_BLUE_INFO);
        this.drawInfoPanel(guiGraphics, -16, 52, PanelType.LARGE_BLUE_INFO);

        if (zirnox.water <= 0) {
            this.drawInfoPanel(guiGraphics, -16, 68, PanelType.LARGE_RED_EXCLAMATION);
        }

        if (zirnox.carbonDioxide < 4000) {
            this.drawInfoPanel(guiGraphics, -16, 84, PanelType.LARGE_RED_EXCLAMATION);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 160, this.topPos + 33, 18, 17,
                this.leftPos + 180, this.topPos + 33,
                Component.literal("Temperature:"),
                Component.literal("   " + Math.round((zirnox.heat) * 0.00001 * 780 + 20) + " C"));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 178, this.topPos + 33, 18, 17,
                this.leftPos + 200, this.topPos + 33,
                Component.literal("Pressure:"),
                Component.literal("   " + Math.round((zirnox.pressure) * 0.00001 * 30) + " bar"));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 160, this.topPos + 108, 18, 12,
                this.leftPos + 180, this.topPos + 108,
                Component.literal("Steam: " + zirnox.steam + "/" + MachineZirnoxBlockEntity.TANK_MAX));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 142, this.topPos + 108, 18, 12,
                this.leftPos + 160, this.topPos + 108,
                Component.literal("CO2: " + zirnox.carbonDioxide + "/" + MachineZirnoxBlockEntity.TANK_MAX));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 178, this.topPos + 108, 18, 12,
                this.leftPos + 200, this.topPos + 108,
                Component.literal("Water: " + zirnox.water + "/" + MachineZirnoxBlockEntity.TANK_MAX));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (this.leftPos + 144 <= x && this.leftPos + 158 > x && this.topPos + 35 < y && this.topPos + 49 >= y) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (this.leftPos + 151 <= x && this.leftPos + 187 > x && this.topPos + 51 < y && this.topPos + 87 >= y) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        return super.mouseClicked(x, y, button);
    }
}
