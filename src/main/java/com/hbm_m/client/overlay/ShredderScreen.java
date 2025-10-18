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

    // Координаты и размеры прогресс-бара
    private static final int PROGRESS_X = 60;
    private static final int PROGRESS_Y = 120;
    private static final int PROGRESS_WIDTH = 330;
    private static final int PROGRESS_HEIGHT = 130;
    private static final int PROGRESS_TEXTURE_X = 196;
    private static final int PROGRESS_TEXTURE_Y = 0;

    public ShredderScreen(ShredderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 176;  // ширина GUI
        this.imageHeight = 233; // высота GUI
        this.inventoryLabelY = this.imageHeight - 94; // Положение надписи "Инвентарь"
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

        // Отрисовка фона GUI
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Отрисовка прогресс-бара
        renderProgressArrow(guiGraphics, x, y);
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

        // Тултип прогресса
        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT, relativeX, relativeY)) {
            ShredderBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity.getProgress() > 0) {
                int ticks = blockEntity.getMaxProgress() - blockEntity.getProgress();
                int seconds = ticks / 20;
                Component tooltip = Component.literal("Time remaining: " + seconds + "s");
                guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }

        // Удалены жестко закодированные тултипы для слотов, так как они не соответствуют новому расположению
        // и Minecraft автоматически обрабатывает тултипы для предметов в слотах.
        // Если необходимы специальные тултипы для пустых слотов, их следует реализовать более универсально.
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}