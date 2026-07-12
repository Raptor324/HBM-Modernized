package com.hbm_m.inventory.gui;

import java.util.List;
import java.util.Optional;

import com.hbm_m.inventory.menu.MachineSatLinkerMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the "Satellite ID Manager" - direct port of legacy {@code GUIMachineSatLinker}
 * (same texture/layout): slot 1 copies its frequency onto slot 2, slot 3 randomizes its own.
 */
public class GUIMachineSatLinker extends AbstractContainerScreen<MachineSatLinkerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_linker.png");

    public GUIMachineSatLinker(MachineSatLinkerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderInfoTooltip(guiGraphics, mouseX, mouseY, -16, 36,
                "The first slot copies its satellite chip's frequency onto the second slot.");
        renderInfoTooltip(guiGraphics, mouseX, mouseY, -16, 52,
                "The third slot randomizes its satellite chip's frequency.");

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderInfoTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, String text) {
        int sx = leftPos + x;
        int sy = topPos + y;
        if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
            guiGraphics.renderTooltip(font, List.of(Component.literal(text)), Optional.empty(), mouseX, mouseY);
        }
    }
}
