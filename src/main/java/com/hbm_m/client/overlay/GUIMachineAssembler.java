package com.hbm_m.client.overlay;

// GUI для сборочной машины. Отвечает за отрисовку прогресса, энергии и подсказок.
// Основан на AbstractContainerScreen и использует текстуры из ресурсов мода.
import javax.annotation.Nonnull;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.MachineAssemblerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineAssembler extends AbstractContainerScreen<MachineAssemblerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_assembler_old.png");

    public GUIMachineAssembler(MachineAssemblerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = this.leftPos;
        int y = this.topPos;

        // Рисуем фон
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // Отрисовка энергии
        int energyBarHeight = 52;
        int energy = this.menu.getEnergyScaled(energyBarHeight);
        guiGraphics.blit(TEXTURE, x + 116, y + 70 - energy, 176, 52 - energy, 16, energy);
        
        // Отрисовка прогресса
        if(menu.isCrafting()) {
            int progressWidth = 83;
            guiGraphics.blit(TEXTURE, x + 45, y + 82, 2, 222, menu.getProgressScaled(progressWidth), 32);
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // Переопределяем renderTooltip
    @Override
    protected void renderTooltip(@Nonnull GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        // Координаты и размеры шкалы энергии
        int energyBarX = this.leftPos + 116;
        int energyBarY = this.topPos + 18;
        int energyBarWidth = 16;
        int energyBarHeight = 52;
        
        // Проверяем, находится ли курсор над шкалой
        if (pX >= energyBarX && pX < energyBarX + energyBarWidth && pY >= energyBarY && pY < energyBarY + energyBarHeight) {
            Component text = Component.literal(this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " FE");
            pGuiGraphics.renderTooltip(this.font, text, pX, pY);
        }
    }
}