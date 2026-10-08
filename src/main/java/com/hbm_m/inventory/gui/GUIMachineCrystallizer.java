package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.inventory.menu.MachineCrystallizerMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI для Crystallizer - порт с 1.7.10 GUICrystallizer.
 * 1:1: Textur, Balken, Tank (35, 18-70), Titel bei 70 - Breite/2.
 */
public class GUIMachineCrystallizer extends GuiInfoScreen<MachineCrystallizerMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(
            RefStrings.MODID, "textures/gui/processing/gui_crystallizer_alt.png");
    *///?} else {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_crystallizer_alt.png");
    //?}

    public GUIMachineCrystallizer(MachineCrystallizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Original: getPowerScaled(52) -> drawTexturedModalRect(guiLeft + 152, guiTop + 70 - i, 176, 64 - i, 16, i)
        long energyStored = menu.getEnergyStored();
        long maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy > 0) {
            int i = (int) (energyStored * 52L / maxEnergy);
            if (i > 52) i = 52;
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 70 - i, 176, 64 - i, 16, i);
        }

        int j = menu.getProgressScaled(28);
        guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 47, 176, 0, j, 12);

        // Original: drawInfoPanel(..., 8) = kleiner blauer Stern
        drawInfoPanel(guiGraphics, 117, 22, PanelType.SMALL_BLUE_STAR);

        // Original: tank.renderTank(guiLeft + 35, guiTop + 70, zLevel, 16, 52) - Port-renderTank erwartet die Oberkante
        if (menu.getBlockEntity() != null) { // тайл может отсутствовать в реплее Flashback
            menu.getBlockEntity().getTank().renderTank(guiGraphics, this.leftPos + 35, this.topPos + 70 - 52, 16, 52);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 70 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                152, 18, 16, 52,
                menu.getEnergyStored(), menu.getMaxEnergyStored());

        if (menu.getBlockEntity() != null) {
            menu.getBlockEntity().getTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 35, this.topPos + 18, 16, 52);
        }

        Component[] upgradeText = new Component[]{
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.effectiveness"),
                Component.translatable("desc.gui.upgrade.overdrive")
        };
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 117, 22, 8, 8, this.leftPos + 200, this.topPos + 45, upgradeText);
    }
}
