package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.MachineBlastFurnaceBlockEntity;
import com.hbm_m.inventory.menu.MachineBlastFurnaceMenu;
// GUI для доменной печи (обновлённая версия): вертикальный индикатор топлива+прогресса,
// датчики дутья и дымовых газов, тултип скорости.
import com.hbm_m.main.MainRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public class GUIMachineBlastFurnace extends AbstractContainerScreen<MachineBlastFurnaceMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/gui_blast_furnace.png");
             *///?} else {
                        ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/gui_blast_furnace.png");
            //?}
    private static final ResourceLocation AIRBLAST_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/fluids/airblast.png");
    private static final ResourceLocation FLUE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/fluids/flue.png");

    public GUIMachineBlastFurnace(MachineBlastFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 0;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Вертикальный столб: топливо снизу, прогресс над ним (как в оригинале)
        int fuelHeight = (int) Math.round(menu.getFuel() * 26D / MachineBlastFurnaceBlockEntity.MAX_FUEL);
        int progressHeight = (int) Math.round(menu.getProgressFraction() * (88D - fuelHeight));
        if (progressHeight > 0) {
            guiGraphics.blit(GUI_TEXTURE,
                    x + 62, y + 106 - progressHeight - fuelHeight,
                    176, 102 - progressHeight - fuelHeight, 56, progressHeight);
        }
        if (fuelHeight > 0) {
            guiGraphics.blit(GUI_TEXTURE,
                    x + 62, y + 106 - fuelHeight,
                    176, 128 - fuelHeight, 56, fuelHeight);
        }

        // Пламя
        if (menu.isProgressing()) {
            guiGraphics.blit(GUI_TEXTURE, x + 81, y + 64, 176, 0, 14, 14);
        }

        // Датчики жидкостей
        drawGauge(guiGraphics, AIRBLAST_TEXTURE, menu.getAir(), MachineBlastFurnaceBlockEntity.AIR_CAPACITY_MB, x + 34, y + 80);
        drawGauge(guiGraphics, FLUE_TEXTURE, menu.getFlue(), MachineBlastFurnaceBlockEntity.FLUE_CAPACITY_MB, x + 34, y + 26);
    }

    private static void drawGauge(GuiGraphics guiGraphics, ResourceLocation texture, int amount, int capacity, int x, int bottom) {
        int height = Math.max(0, Math.min(16, amount * 16 / capacity));
        if (height > 0) {
            guiGraphics.blit(texture, x, bottom - height, 0, 16 - height, 5, height, 16, 16);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltips(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title,
                (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                8, 128, 0x404040, false);
    }

    /** Тултипы: скорость на пламени/прогрессе, объёмы баков на датчиках. */
    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.getCarried().isEmpty()) {
            return;
        }
        if (isHoveringArea(79, 62, 18, 18, mouseX, mouseY)) {
            Component tooltip = Component.translatable("gui.hbm_m.blast_furnace.speed",
                    Math.round(menu.getSpeed() * 100D)).withStyle(ChatFormatting.YELLOW);
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        } else if (isHoveringArea(25, 71, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.translatable("fluid.hbm_m.airblast"),
                    Component.literal(menu.getAir() + "/" + MachineBlastFurnaceBlockEntity.AIR_CAPACITY_MB + " mB")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHoveringArea(25, 17, 18, 18, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.translatable("fluid.hbm_m.flue"),
                    Component.literal(menu.getFlue() + "/" + MachineBlastFurnaceBlockEntity.FLUE_CAPACITY_MB + " mB")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    private boolean isHoveringArea(int relX, int relY, int areaWidth, int areaHeight, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        return mouseX >= x + relX && mouseX < x + relX + areaWidth
                && mouseY >= y + relY && mouseY < y + relY + areaHeight;
    }
}
