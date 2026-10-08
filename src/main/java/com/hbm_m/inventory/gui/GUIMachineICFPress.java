package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.MachineICFPressMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


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
    private static final int TANK_A_X = 44;
    private static final int TANK_B_X = 152;

    public GUIMachineICFPress(MachineICFPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 179;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTank(guiGraphics, leftPos + TANK_A_X, topPos + TANK_TOP, 16, TANK_HEIGHT);
        tanks[1].renderTank(guiGraphics, leftPos + TANK_B_X, topPos + TANK_TOP, 16, TANK_HEIGHT);

        // Original: int m = muon * 52 / maxMuon; drawTexturedModalRect(guiLeft + 28, guiTop + 70 - m, 176, 52 - m, 4, m)
        int m = menu.getMuon() * 52 / com.hbm_m.blockentity.machines.icf.MachineICFPressBlockEntity.MAX_MUON;
        guiGraphics.blit(TEXTURE, leftPos + 28, topPos + 70 - m, 176, 52 - m, 4, m);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        var tanks = menu.getBlockEntity().getTanks();
        tanks[0].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_A_X, topPos + TANK_TOP, 16, TANK_HEIGHT);
        tanks[1].renderTankInfo(guiGraphics, font, mouseX, mouseY, leftPos + TANK_B_X, topPos + TANK_TOP, 16, TANK_HEIGHT);

        // Original: Hinweise auf den leeren Feststoff-Slots (Index 4 und 5)
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
            int idx = this.menu.slots.indexOf(this.hoveredSlot);
            if (idx == 4) guiGraphics.renderTooltip(font, Component.literal("Item input: Top/Bottom").withStyle(net.minecraft.ChatFormatting.YELLOW), mouseX, mouseY);
            if (idx == 5) guiGraphics.renderTooltip(font, Component.literal("Item input: Sides").withStyle(net.minecraft.ChatFormatting.YELLOW), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Original drawGuiContainerForegroundLayer
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 88 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
