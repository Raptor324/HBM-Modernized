package com.hbm_m.inventory.gui;

import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineCombinationOvenBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineCombinationOvenMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIFurnaceCombo}: Fortschrittsbalken (45,37) und Hitzebalken (45,46) mit TU-Tooltips, Tank 16x52 bei
 * (118,18).
 */
public class GUIMachineCombinationOven extends GuiInfoScreen<MachineCombinationOvenMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_furnace_combination.png");

    private final MachineCombinationOvenBlockEntity furnace;

    public GUIMachineCombinationOven(MachineCombinationOvenMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.furnace = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (furnace != null) {
            furnace.getTank().renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 118, topPos + 18, 16, 52);

            drawCustomInfoStat(g, mouseX, mouseY, 44, 36, 39, 7, mouseX, mouseY, Component.literal(
                    String.format(Locale.US, "%,d", furnace.getProgress()) + " / " + String.format(Locale.US, "%,d", MachineCombinationOvenBlockEntity.processTime) + "TU"));
            drawCustomInfoStat(g, mouseX, mouseY, 44, 45, 39, 7, mouseX, mouseY, Component.literal(
                    String.format(Locale.US, "%,d", furnace.getHeat()) + " / " + String.format(Locale.US, "%,d", MachineCombinationOvenBlockEntity.maxHeat) + "TU"));
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (furnace == null) return;

        int p = furnace.getProgress() * 38 / MachineCombinationOvenBlockEntity.processTime;
        g.blit(TEXTURE, leftPos + 45, topPos + 37, 176, 0, p, 5);

        int h = furnace.getHeat() * 37 / MachineCombinationOvenBlockEntity.maxHeat;
        g.blit(TEXTURE, leftPos + 45, topPos + 46, 176, 5, h, 5);

        furnace.getTank().renderTank(g, leftPos + 118, topPos + 18, 16, 52);
    }
}
