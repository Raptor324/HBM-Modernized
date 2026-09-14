package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineICFPressMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 1:1-Port von {@code GUIICFPress} (1.7.10), 176x180. Die beiden Brennstofftanks stehen links und
 * rechts neben den Kapselplaetzen; zwischen ihnen zeigt eine kleine Leiste an, wieviele
 * Myonenladungen noch da sind.
 */
public class GUIMachineICFPress extends AbstractContainerScreen<MachineICFPressMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_icf_press.png");

    private static final int TANK_TOP = 18;
    private static final int TANK_HEIGHT = 52;
    private static final int TANK_A_X = 26;
    private static final int TANK_B_X = 152;

    public GUIMachineICFPress(MachineICFPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 180;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTank(guiGraphics, leftPos + TANK_A_X, topPos + TANK_TOP, 16, TANK_HEIGHT);
        tanks[1].renderTank(guiGraphics, leftPos + TANK_B_X, topPos + TANK_TOP, 16, TANK_HEIGHT);

        // Myonenladungen als kleine Leiste zwischen den beiden Kapselplaetzen.
        int muon = menu.getMuon() * 32 / com.hbm_m.blockentity.machines.icf.MachineICFPressBlockEntity.MAX_MUON;
        if (muon > 0) {
            guiGraphics.fill(leftPos + 8, topPos + 44, leftPos + 8 + muon / 2, topPos + 48, 0xFF3FD8E8);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_A_X, topPos + TANK_TOP, 16, TANK_HEIGHT);
        tanks[1].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_B_X, topPos + TANK_TOP, 16, TANK_HEIGHT);

        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        if (localX >= 8 && localX < 24 && localY >= 42 && localY < 50) {
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.hbm_m.icf_press.muon", menu.getMuon(),
                            com.hbm_m.blockentity.machines.icf.MachineICFPressBlockEntity.MAX_MUON)),
                    mouseX, mouseY);
        }
    }
}
