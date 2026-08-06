package com.hbm_m.inventory.gui;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.machines.MachineAnnihilatorBlockEntity;
import com.hbm_m.inventory.menu.MachineAnnihilatorMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.AnnihilatorPoolC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Eigenes Layout (das Original {@code GUIMachineAnnihilator} zeigt zusaetzlich Payout-Slots/Claim-
 * Button fuer das nicht uebernommene Meilenstein-System - siehe Klassenkommentar in
 * {@link MachineAnnihilatorBlockEntity}). Verwendet die portierte Original-Panel-Textur
 * (nur der statische 176x208-Rahmen, keine geratenen Icon-Koordinaten daraus), Tankanzeige und
 * Monitor-Tooltip als eigene Overlays, sowie ein editierbares Pool-Namensfeld analog zum Original.
 */
public class GUIMachineAnnihilator extends GuiInfoScreen<MachineAnnihilatorMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_annihilator.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_annihilator.png");
            //?}

    private static final int TANK_X = 152;
    private static final int TANK_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;

    private static final int MONITOR_SLOT_X = 134;
    private static final int MONITOR_SLOT_Y = 36;

    private static final int POOL_FIELD_X = 26;
    private static final int POOL_FIELD_Y = 60;
    private static final int POOL_FIELD_WIDTH = 100;
    private static final int POOL_FIELD_HEIGHT = 12;

    private EditBox poolField;

    public GUIMachineAnnihilator(MachineAnnihilatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 208;
        this.inventoryLabelY = 116;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        MachineAnnihilatorBlockEntity be = menu.getBlockEntity();
        poolField = new EditBox(font, leftPos + POOL_FIELD_X, topPos + POOL_FIELD_Y, POOL_FIELD_WIDTH, POOL_FIELD_HEIGHT, Component.empty());
        poolField.setMaxLength(64);
        poolField.setBordered(true);
        poolField.setTextColor(0xFFFFFF);
        poolField.setValue(be.getPoolName());
        poolField.setResponder(s -> {
            if (!s.isBlank()) {
                AnnihilatorPoolC2SPacket.sendToServer(be.getBlockPos(), s);
            }
        });
        addRenderableWidget(poolField);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderTank(guiGraphics, x, y);
    }

    private void renderTank(GuiGraphics guiGraphics, int x, int y) {
        MachineAnnihilatorBlockEntity be = menu.getBlockEntity();
        be.getTank().renderTank(guiGraphics, x + TANK_X, y + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderCustomTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    private void renderCustomTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MachineAnnihilatorBlockEntity be = menu.getBlockEntity();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isPointInRect(TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            be.getTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, x + TANK_X, y + TANK_Y, TANK_WIDTH, TANK_HEIGHT);
        }

        if (isPointInRect(MONITOR_SLOT_X, MONITOR_SLOT_Y, 16, 16, mouseX, mouseY)) {
            ItemStack monitorStack = be.getInventory().getStackInSlot(MachineAnnihilatorBlockEntity.SLOT_MONITOR);
            if (!monitorStack.isEmpty()) {
                String display = be.getMonitorDisplay();
                BigInteger count = display.isBlank() ? BigInteger.ZERO : new BigInteger(display);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(monitorStack.getHoverName());
                tooltip.add(Component.translatable("gui.hbm_m.annihilator.pool", be.getPoolName()));
                tooltip.add(Component.translatable("gui.hbm_m.annihilator.destroyed", String.format("%,d", count)));
                guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }
}
