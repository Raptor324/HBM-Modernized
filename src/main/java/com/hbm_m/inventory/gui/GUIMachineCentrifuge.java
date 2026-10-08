package com.hbm_m.inventory.gui;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineCentrifugeMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIMachineCentrifuge} (1.7.10): 182x189, Energiebalken links (8, 18-55),
 * Fortschritt in vier 36 Pixel hohen Saeulen (Gesamtskala 145), Upgrade-Info rechts oben.
 */
public class GUIMachineCentrifuge extends GuiInfoScreen<MachineCentrifugeMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_centrifuge.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_centrifuge.png");
            //?}

    public GUIMachineCentrifuge(MachineCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 182;
        this.imageHeight = 189;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);

        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 18, 16, 37,
                menu.getEnergyLong(), menu.getMaxEnergyLong());

        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 160, 16, 8, 8, mouseX, mouseY,
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.power"),
                Component.translatable("desc.gui.upgrade.overdrive"));

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 + 36 / 2 - this.font.width(name) / 2, 6, 0xffffff, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 11, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        long power = menu.getEnergyLong();
        long maxPower = menu.getMaxEnergyLong();
        if (power > 0 && maxPower > 0) {
            int i1 = (int) (power * 37 / maxPower);
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 55 - i1, 182, 37 - i1, 16, i1);
        }

        if (menu.isProcessing()) {
            int p = menu.getScaledProgress(145);
            for (int i = 0; i < 4; i++) {
                int h = Math.min(p, 36);
                guiGraphics.blit(TEXTURE, this.leftPos + 72 + i * 20, this.topPos + 57 - h, 182, 73 - h, 12, h);
                p -= h;
                if (p <= 0) break;
            }
        }

        this.drawInfoPanel(guiGraphics, 160, 16, PanelType.SMALL_BLUE_STAR);
    }
}
