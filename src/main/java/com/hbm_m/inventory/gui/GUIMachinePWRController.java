package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.PWRControllerMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.PWRControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Simple, self-drawn layout on top of the original's {@code gui_pwr.png} texture (already
 * present in this port's assets), following the same approach as
 * {@code GUIMachineWatzPowerplant}: no pixel-perfect reproduction of the original
 * {@code GUIPWR} overlay coordinates, tanks rendered with the generic
 * {@code FluidTank#renderTank} helper. The original's draggable control-rod slider is replaced
 * with +/-5% buttons for simplicity.
 */
public class GUIMachinePWRController extends GuiInfoScreen<PWRControllerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_pwr.png");

    private static final int TANK_COOLANT_X = 132;
    private static final int TANK_HOT_X = 152;
    private static final int TANK_Y = 8;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 108;

    private static final int HEAT_BAR_X = 8;
    private static final int HEAT_BAR_Y = 8;
    private static final int HEAT_BAR_WIDTH = 108;
    private static final int HEAT_BAR_HEIGHT = 12;

    private static final int ROD_MINUS_X = 8;
    private static final int ROD_PLUS_X = 96;
    private static final int ROD_BUTTON_Y = 40;
    private static final int ROD_BUTTON_SIZE = 18;

    private final PWRControllerBlockEntity pwr;

    public GUIMachinePWRController(PWRControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.pwr = menu.getBlockEntity();
        this.imageWidth = 226;
        this.imageHeight = 230;
        this.inventoryLabelY = 137;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // тайл может отсутствовать в реплее Flashback
        if (pwr == null) return;

        pwr.getCoolantTank().renderTank(guiGraphics, this.leftPos + TANK_COOLANT_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        pwr.getCoolantHotTank().renderTank(guiGraphics, this.leftPos + TANK_HOT_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);

        int heatScaled = pwr.getGaugeScaled(HEAT_BAR_WIDTH, 2);
        int heatColor = pwr.coreHeat > pwr.coreHeatCapacity / 2 ? 0xFFCC3333 : 0xFFCC8833;
        guiGraphics.fill(this.leftPos + HEAT_BAR_X, this.topPos + HEAT_BAR_Y,
                this.leftPos + HEAT_BAR_X + heatScaled, this.topPos + HEAT_BAR_Y + HEAT_BAR_HEIGHT, heatColor);

        guiGraphics.fill(this.leftPos + ROD_MINUS_X, this.topPos + ROD_BUTTON_Y,
                this.leftPos + ROD_MINUS_X + ROD_BUTTON_SIZE, this.topPos + ROD_BUTTON_Y + ROD_BUTTON_SIZE, 0xFF884433);
        guiGraphics.fill(this.leftPos + ROD_PLUS_X, this.topPos + ROD_BUTTON_Y,
                this.leftPos + ROD_PLUS_X + ROD_BUTTON_SIZE, this.topPos + ROD_BUTTON_Y + ROD_BUTTON_SIZE, 0xFF338844);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // тайл может отсутствовать в реплее Flashback
        if (pwr != null) {
        if (isPointInRect(TANK_COOLANT_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            pwr.getCoolantTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + TANK_COOLANT_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        }
        if (isPointInRect(TANK_HOT_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            pwr.getCoolantHotTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + TANK_HOT_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        }

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                HEAT_BAR_X, HEAT_BAR_Y, HEAT_BAR_WIDTH, HEAT_BAR_HEIGHT,
                this.leftPos + HEAT_BAR_X, this.topPos + HEAT_BAR_Y,
                Component.literal("Core heat: " + pwr.coreHeat + " / " + pwr.coreHeatCapacity),
                Component.literal("Flux: " + Math.round(pwr.flux)));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                0, 60, imageWidth, 12,
                this.leftPos + 4, this.topPos + 60,
                Component.literal("Control rods: " + Math.round(pwr.rodLevel) + "% -> " + Math.round(pwr.rodTarget) + "%"),
                Component.literal(pwr.typeLoaded != null ? ("Loaded: " + pwr.typeLoaded + " x" + pwr.amountLoaded) : "No fuel loaded"));

        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        // тайл может отсутствовать в реплее Flashback
        if (pwr == null) return super.mouseClicked(x, y, button);
        if (isWithin(ROD_MINUS_X, ROD_BUTTON_Y, ROD_BUTTON_SIZE, ROD_BUTTON_SIZE, x, y)) {
            PWRControlPacket.sendToServer(pwr.getBlockPos(), Math.max(0D, pwr.rodTarget - 5D));
            playClickSound();
            return true;
        }
        if (isWithin(ROD_PLUS_X, ROD_BUTTON_Y, ROD_BUTTON_SIZE, ROD_BUTTON_SIZE, x, y)) {
            PWRControlPacket.sendToServer(pwr.getBlockPos(), Math.min(100D, pwr.rodTarget + 5D));
            playClickSound();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private boolean isWithin(int relX, int relY, int w, int h, double mouseX, double mouseY) {
        double x = this.leftPos + relX;
        double y = this.topPos + relY;
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
