package com.hbm_m.inventory.gui;

import java.util.List;
import java.util.Locale;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.FireboxBaseMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Порт {@code GUIFirebox} (1.7.10): общая для гритбокса и Heating Oven отрисовка —
 * шкала тепла (69px от (176,0)) на (81,28), шкала горения (70px от (176,5)) на
 * (81,37), иконка пламени 18×18 от (176,10) на (25,26) при горении.
 */
public abstract class GUIFireboxBase<M extends FireboxBaseMenu<?>> extends GuiInfoScreen<M> {

    private static final int HEAT_X = 81, HEAT_Y = 28, HEAT_W = 69, HEAT_H = 5;
    private static final int BURN_X = 81, BURN_Y = 37, BURN_W = 70, BURN_H = 5;
    private static final int FLAME_X = 25, FLAME_Y = 26, FLAME_W = 18, FLAME_H = 18;
    private static final int GAUGE_U = 176;

    protected final M menu;
    private final ResourceLocation texture;
    private final boolean brightTitle;

    protected GUIFireboxBase(M menu, Inventory playerInventory, Component title,
                             String texturePath, boolean brightTitle) {
        super(menu, playerInventory, title);
        this.menu = menu;
        this.texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, texturePath);
        this.brightTitle = brightTitle;
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Шкала тепла
        int heatW = menu.getHeatScaled(HEAT_W);
        if (heatW > 0) {
            guiGraphics.blit(texture, this.leftPos + HEAT_X, this.topPos + HEAT_Y, GAUGE_U, 0, heatW, HEAT_H);
        }

        // Шкала горения
        int burnW = menu.getBurnTimeScaled(BURN_W);
        if (burnW > 0) {
            guiGraphics.blit(texture, this.leftPos + BURN_X, this.topPos + BURN_Y, GAUGE_U, 5, burnW, BURN_H);
        }

        // Пламя при горении
        if (menu.isBurning()) {
            guiGraphics.blit(texture, this.leftPos + FLAME_X, this.topPos + FLAME_Y, GAUGE_U, 10, FLAME_W, FLAME_H);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        // Тепло: "%d / %dTU"
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                HEAT_X - 1, HEAT_Y - 1, HEAT_W + 2, HEAT_H + 2, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d / %,dTU",
                        menu.getHeatEnergy(), menu.getMaxHeatEnergy())));

        // Горение: "TU/t, s"
        if (menu.getMaxBurnTime() > 0) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                    BURN_X - 1, BURN_Y - 1, BURN_W + 2, BURN_H + 2, mouseX, mouseY,
                    Component.literal(String.format(Locale.US, "%,d TU/t, %ds",
                            menu.getBurnHeat(), menu.getBurnSeconds())));
        }

        // Тултип модификаторов топлива над ПУСТЫМИ слотами (порт getModule().getDesc())
        int fuelSlot = hoveredFuelSlot(mouseX, mouseY);
        if (!menu.getFuelDescription().isEmpty() && fuelSlot >= 0
                && menu.blockEntity.getInventory().getStackInSlot(fuelSlot).isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, menu.getFuelDescription(), mouseX, mouseY);
        }
    }

    private int hoveredFuelSlot(double mouseX, double mouseY) {
        for (int i = 0; i < FireboxBaseMenu.MACHINE_SLOT_COUNT; i++) {
            double sx = this.leftPos + 44 + i * 18;
            double sy = this.topPos + 27;
            if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY,
                brightTitle ? 0xFFFFFF : 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                8, this.inventoryLabelY, 0x404040, false);
    }
}
