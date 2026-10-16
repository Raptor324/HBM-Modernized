package com.hbm_m.inventory.gui;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachinePressMenu;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Порт GUIMachinePress 1.7.10: текстура 176x214, слоты топлива/штампа/входа/выхода + 9 слотов хранилища,
 *  пламя-индикатор, вертикальная полоса прогресса и спидометр. */
public class GUIMachinePress extends AbstractContainerScreen<MachinePressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/processing/gui_press.png");

    // Пламя-индикатор (ориг. drawTexturedModalRect(guiLeft + 26, guiTop + 36, 0, 214, 14, 14))
    private static final int FLAME_X = 26;
    private static final int FLAME_Y = 36;
    private static final int FLAME_U = 0;
    private static final int FLAME_V = 214;
    private static final int FLAME_SIZE = 14;

    // Вертикальная полоса прогресса (ориг. drawTexturedModalRect(guiLeft + 79, guiTop + 35, 15, 214, 18, k))
    private static final int BAR_X = 79;
    private static final int BAR_Y = 35;
    private static final int BAR_U = 15;
    private static final int BAR_V = 214;
    private static final int BAR_WIDTH = 18;
    private static final int BAR_MAX_HEIGHT = 16;

    // Спидометр (ориг. GUIElements.drawSmoothGauge(guiLeft + 34, guiTop + 25, zLevel, i, 5, 2, 1, 0x7f0000))
    private static final float GAUGE_X = 34;
    private static final float GAUGE_Y = 25;

    public GUIMachinePress(MachinePressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
    }

    @Override
    protected void init() {
        super.init();
        // Оригинал: имя по центру на y=5, "Inventory" на 8, ySize - 96 + 2
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Пламя горит, пока есть топливо
        if (menu.getBurnTime() >= 20) {
            guiGraphics.blit(TEXTURE, x + FLAME_X, y + FLAME_Y, FLAME_U, FLAME_V, FLAME_SIZE, FLAME_SIZE);
        }

        // Полоса прогресса пресса
        int press = menu.getPress();
        int maxPress = menu.getMaxPress();
        if (press > 0 && maxPress > 0) {
            int barHeight = (press * BAR_MAX_HEIGHT) / maxPress;
            if (barHeight > 0) {
                guiGraphics.blit(TEXTURE, x + BAR_X, y + BAR_Y, BAR_U, BAR_V, BAR_WIDTH, barHeight);
            }
        }

        renderGaugeNeedle(guiGraphics, x, y);
    }

    private void renderGaugeNeedle(GuiGraphics guiGraphics, int x, int y) {
        int speed = menu.getSpeed();
        int maxSpeed = menu.getMaxSpeed();

        double progress = maxSpeed > 0 ? (double) speed / (double) maxSpeed : 0.0;
        progress = Mth.clamp(progress, 0.0, 1.0);

        float centerX = x + GAUGE_X;
        float centerY = y + GAUGE_Y;

        // Параметры оригинала: tipLength 5, backLength 2, backSide 1
        double tipLength = 5.0;
        double backLength = 2.0;
        double backSide = 1.0;
        int color = 0x7F0000;
        int colorOuter = 0x000000;

        // Экранные координаты Y растут вниз, поэтому базовые векторы оригинала (Y вверх) отражены по Y:
        // progress = 0 -> стрелка вправо-вверх, progress = 0.5 -> вниз, progress = 1 -> влево-вверх
        float angle = (float) Math.toRadians(45.0 + progress * 270.0);

        Vector3f tip = new Vector3f(0, (float) -tipLength, 0);
        Vector3f left = new Vector3f((float) backSide, (float) backLength, 0);
        Vector3f right = new Vector3f((float) -backSide, (float) backLength, 0);

        rotateAroundZ(tip, angle);
        rotateAroundZ(left, angle);
        rotateAroundZ(right, angle);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder buffer = com.hbm_m.platform.RenderHooks.beginTesselator(Tesselator.getInstance(), VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Внешний слой (темный контур), mult оригинала = 1.5
        double mult = 1.5;

        com.hbm_m.platform.RenderHooks.vertexColor(buffer, matrix, centerX + (float)(tip.x * mult), centerY + (float)(tip.y * mult), 0, (colorOuter >> 16) & 0xFF, (colorOuter >> 8) & 0xFF, colorOuter & 0xFF, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(buffer, matrix, centerX + (float)(left.x * mult), centerY + (float)(left.y * mult), 0, (colorOuter >> 16) & 0xFF, (colorOuter >> 8) & 0xFF, colorOuter & 0xFF, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(buffer, matrix, centerX + (float)(right.x * mult), centerY + (float)(right.y * mult), 0, (colorOuter >> 16) & 0xFF, (colorOuter >> 8) & 0xFF, colorOuter & 0xFF, 255);

        // Внутренний слой (красная стрелка)
        com.hbm_m.platform.RenderHooks.vertexColor(buffer, matrix, centerX + tip.x, centerY + tip.y, 0, (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(buffer, matrix, centerX + left.x, centerY + left.y, 0, (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(buffer, matrix, centerX + right.x, centerY + right.y, 0, (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255);

        com.hbm_m.platform.RenderHooks.drawWithShader(buffer);
        RenderSystem.disableBlend();
    }

    private void rotateAroundZ(Vector3f vec, float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        float newX = vec.x * cos - vec.y * sin;
        float newY = vec.x * sin + vec.y * cos;

        vec.x = newX;
        vec.y = newY;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Тултипы оригинала: (25,16) скорость в %, (25,34) операций топлива осталось
        if (isHovering(25, 16, 18, 18, mouseX, mouseY)) {
            int speed = menu.getSpeed();
            int maxSpeed = menu.getMaxSpeed();
            int speedPercent = maxSpeed != 0 ? (speed * 100) / maxSpeed : 0;

            guiGraphics.renderTooltip(this.font,
                    Component.literal(speedPercent + "%"),
                    mouseX, mouseY);
        }

        if (isHovering(25, 34, 18, 18, mouseX, mouseY)) {
            int burnTime = menu.getBurnTime();
            int operationsLeft = burnTime / 200;

            guiGraphics.renderTooltip(this.font,
                    Component.literal(operationsLeft + " operations left"),
                    mouseX, mouseY);
        }
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        mouseX -= guiLeft;
        mouseY -= guiTop;

        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
