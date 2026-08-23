package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineWatzPowerplantBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineWatzPowerplantMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.WatzControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Simple, self-drawn layout on top of the original's {@code gui_watz.png}/{@code fluids/watz.png}
 * textures (already present in this port's assets). Pixel-perfect reproduction of the original
 * {@code GUIWatz} overlay coordinates was out of scope; tanks are rendered with the generic
 * {@code FluidTank#renderTank} helper (same approach as {@code GUIMachineArcFurnace}) instead of
 * hand-placed gauge sprites.
 */
public class GUIMachineWatzPowerplant extends GuiInfoScreen<MachineWatzPowerplantMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_watz.png");

    private static final int TANK_COOLANT_X = 132;
    private static final int TANK_HOT_X = 152;
    private static final int TANK_WASTE_X = 172;
    private static final int TANK_Y = 8;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 108;

    private static final int BUTTON_X = 132;
    private static final int BUTTON_Y = 122;
    private static final int BUTTON_SIZE = 18;

    private final MachineWatzPowerplantBlockEntity watz;

    public GUIMachineWatzPowerplant(MachineWatzPowerplantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.watz = menu.getBlockEntity();
        this.imageWidth = 226;
        this.imageHeight = 230;
        this.inventoryLabelY = 137;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Тайл может отсутствовать в реплее Flashback
        if (watz == null) return;

        watz.coolantTank.renderTank(guiGraphics, this.leftPos + TANK_COOLANT_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        watz.coolantHotTank.renderTank(guiGraphics, this.leftPos + TANK_HOT_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        watz.wasteTank.renderTank(guiGraphics, this.leftPos + TANK_WASTE_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);

        int color = watz.isOn ? 0xFF33CC33 : 0xFFCC3333;
        guiGraphics.fill(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y,
                this.leftPos + BUTTON_X + BUTTON_SIZE, this.topPos + BUTTON_Y + BUTTON_SIZE, color);
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

        // Тайл может отсутствовать в реплее Flashback
        if (watz == null) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }

        if (isPointInRect(TANK_COOLANT_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            watz.coolantTank.renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + TANK_COOLANT_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        }
        if (isPointInRect(TANK_HOT_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            watz.coolantHotTank.renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + TANK_HOT_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        }
        if (isPointInRect(TANK_WASTE_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            watz.wasteTank.renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + TANK_WASTE_X, this.topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        }

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                BUTTON_X, BUTTON_Y, BUTTON_SIZE, BUTTON_SIZE,
                this.leftPos + BUTTON_X, this.topPos + BUTTON_Y,
                Component.literal(watz.isOn ? "Reactor: ON" : "Reactor: OFF"),
                Component.literal(watz.redstonePowered ? "(forced on by redstone)" : "(click to toggle)"));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                0, 152, imageWidth, 12,
                this.leftPos + 4, this.topPos + 152,
                Component.literal("Heat: " + Math.round(watz.heat) + " / " + Math.round(MachineWatzPowerplantBlockEntity.MAX_SAFE_HEAT)),
                Component.literal("Flux: " + Math.round(watz.fluxLastBase + watz.fluxLastReaction)));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        // тайл может отсутствовать в реплее Flashback
        if (watz == null) return super.mouseClicked(x, y, button);
        if (this.leftPos + BUTTON_X <= x && this.leftPos + BUTTON_X + BUTTON_SIZE > x
                && this.topPos + BUTTON_Y <= y && this.topPos + BUTTON_Y + BUTTON_SIZE > y) {
            WatzControlPacket.sendToServer(watz.getBlockPos(), 0);
            playClickSound();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
}
