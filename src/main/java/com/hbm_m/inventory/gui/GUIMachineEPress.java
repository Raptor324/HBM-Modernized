package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineEPressMenu;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIMachineEPress} (1.7.10): 176x186, Energiebalken rechts (152, 18-52),
 * Stempelanzeige links (18, 33).
 */
public class GUIMachineEPress extends GuiInfoScreen<MachineEPressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_electric_press.png");

    public GUIMachineEPress(MachineEPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.drawElectricityInfo(guiGraphics, mouseX, mouseY, 152, 52 - 34, 16, 34,
                menu.getEnergyStored(), menu.getMaxEnergyStored());
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 89 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        long maxPower = menu.getMaxEnergyStored();
        int i = maxPower > 0 ? (int) ((long) menu.getEnergyStored() * 34 / maxPower) : 0;
        guiGraphics.blit(TEXTURE, leftPos + 152, topPos + 52 - i, 176, 34 - i, 16, i);

        int maxPress = menu.getMaxPress();
        int k = maxPress > 0 ? menu.getPress() * 16 / maxPress : 0;
        guiGraphics.blit(TEXTURE, leftPos + 18, topPos + 33, 192, 0, 18, k);
    }
}
