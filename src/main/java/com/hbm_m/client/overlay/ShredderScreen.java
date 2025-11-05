// ShredderScreen.java
package com.hbm_m.client.overlay;

import com.hbm_m.block.entity.ShredderBlockEntity;
import com.hbm_m.menu.ShredderMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ShredderScreen extends AbstractContainerScreen<ShredderMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("hbm_m", "textures/gui/shredder_gui.png");

    // Corrected Progress Bar constants (based on typical arrow size and position)
    // Adjust these if your texture's arrow is different or positioned elsewhere.
    private static final int PROGRESS_X = 79; // Relative X position on the GUI for the arrow
    private static final int PROGRESS_Y = 40; // Relative Y position on the GUI for the arrow
    private static final int PROGRESS_WIDTH = 24; // Actual width of the arrow texture
    private static final int PROGRESS_HEIGHT = 17; // Actual height of the arrow texture
    private static final int PROGRESS_TEXTURE_X = 196; // Source X on texture for the arrow
    private static final int PROGRESS_TEXTURE_Y = 0; // Source Y on texture for the arrow

    // Coordinates and dimensions for the energy bar (assuming it's vertical and fills upwards)
    private static final int ENERGY_BAR_X = 7; // Relative X position on the GUI
    private static final int ENERGY_BAR_Y = 17; // Relative Y position on the GUI
    private static final int ENERGY_BAR_WIDTH = 16; // Width of the energy bar
    private static final int ENERGY_BAR_HEIGHT = 96; // Height of the energy bar

    // Texture coordinates for the filled part of the energy bar on shredder_gui.png
    // This assumes the gradient is located at (196, 17) with a width of 12 and height of 52 on your texture.
    private static final int ENERGY_TEXTURE_X = 196; // Source X on texture for the energy fill (start of the gradient)
    private static final int ENERGY_TEXTURE_Y = 17; // Source Y on texture for the energy fill (top of the gradient)


    public ShredderScreen(ShredderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 176;  // GUI width
        this.imageHeight = 233; // GUI height
        this.inventoryLabelY = this.imageHeight - 94; // Position of "Inventory" label
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Render GUI background
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Render progress bar
        renderProgressArrow(guiGraphics, x, y);

        // Render energy bar
        renderEnergyBar(guiGraphics, x, y);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        ShredderBlockEntity blockEntity = menu.getBlockEntity();

        if (blockEntity.getProgress() > 0) {
            int progress = blockEntity.getProgress();
            int maxProgress = blockEntity.getMaxProgress();

            int progressPixels = (int) ((progress / (float) maxProgress) * PROGRESS_WIDTH);

            if (progressPixels > 0) {
                guiGraphics.blit(TEXTURE,
                        x + PROGRESS_X, y + PROGRESS_Y,
                        PROGRESS_TEXTURE_X, PROGRESS_TEXTURE_Y,
                        progressPixels, PROGRESS_HEIGHT);
            }
        }
    }

    private void renderEnergyBar(GuiGraphics guiGraphics, int x, int y) {
        ShredderBlockEntity blockEntity = menu.getBlockEntity();

        int currentEnergy = blockEntity.getEnergy();
        int maxEnergy = blockEntity.getMaxEnergy();

        if (maxEnergy <= 0) return;

        // Вычисляем процент и пиксели
        float fillPercent = (float) currentEnergy / maxEnergy;
        int energyPixels = Math.round(fillPercent * ENERGY_BAR_HEIGHT);
        energyPixels = Math.max(0, Math.min(energyPixels, ENERGY_BAR_HEIGHT));

        if (energyPixels > 0) {
            // КЛЮЧ К РЕШЕНИЮ: смещаем обе координаты одинаково
            int yOffset = ENERGY_BAR_HEIGHT - energyPixels;

            guiGraphics.blit(
                    TEXTURE,
                    x + ENERGY_BAR_X,           // X на экране
                    y + ENERGY_BAR_Y + yOffset, // Y на экране (со смещением)
                    ENERGY_TEXTURE_X,           // X в текстуре
                    ENERGY_TEXTURE_Y + yOffset, // Y в текстуре (со смещением)
                    ENERGY_BAR_WIDTH,           // Ширина
                    energyPixels                // Высота заполнения
            );
        }
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
        int relativeX = mouseX - leftPos;
        int relativeY = mouseY - topPos;

        // Tooltip for progress bar
        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT, relativeX, relativeY)) {
            ShredderBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity.getProgress() > 0) {
                int ticks = blockEntity.getMaxProgress() - blockEntity.getProgress();
                int seconds = ticks / 20;
                Component tooltip = Component.literal("Time remaining: " + seconds + "s");
                guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }

        // Tooltip for energy bar
        if (isHovering(ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, relativeX, relativeY)) {
            ShredderBlockEntity blockEntity = menu.getBlockEntity();
            Component tooltip = Component.literal("Energy: " + blockEntity.getEnergy() + "/" + blockEntity.getMaxEnergy() + " FE");
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}