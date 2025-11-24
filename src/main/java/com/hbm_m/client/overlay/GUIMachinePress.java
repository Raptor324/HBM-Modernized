package com.hbm_m.client.overlay;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachinePressMenu;
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

public class GUIMachinePress extends AbstractContainerScreen<MachinePressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/gui_press.png");

    private static final int LIGHT_U = 176;
    private static final int LIGHT_V = 0;
    private static final int LIGHT_WIDTH = 14;
    private static final int LIGHT_HEIGHT = 14;

    private static final int ARROW_U = 194;
    private static final int ARROW_V = 0;
    private static final int ARROW_WIDTH = 19;
    private static final int ARROW_MAX_HEIGHT = 17;

    private static final int GAUGE_WIDTH = 18;
    private static final int GAUGE_HEIGHT = 18;

    public GUIMachinePress(MachinePressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 6;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        renderLight(guiGraphics, x, y);
        renderGaugeNeedle(guiGraphics, x, y);
        renderPressArrow(guiGraphics, x, y);
    }

    private void renderLight(GuiGraphics guiGraphics, int x, int y) {
        int burnTime = menu.getBurnTime();
        
        if (burnTime >= 20) {
            guiGraphics.blit(TEXTURE,
                    x + 27, y + 36,
                    LIGHT_U, LIGHT_V,
                    LIGHT_WIDTH, LIGHT_HEIGHT);
        }
    }

    private void renderGaugeNeedle(GuiGraphics guiGraphics, int x, int y) {
        int speed = menu.getSpeed();
        int maxSpeed = menu.getMaxSpeed();
        
        double progress = maxSpeed > 0 ? (double) speed / (double) maxSpeed : 0.0;
        progress = Mth.clamp(progress, 0.0, 1.0);

        float centerX = x + 25 + GAUGE_WIDTH / 2.0f;
        float centerY = y + 16 + GAUGE_HEIGHT / 2.0f;

        // Уменьшенные параметры стрелки
        double tipLength = 5.5;
        double backLength = 1.5;
        double backSide = 0.8;
        int color = 0x7F0000;
        int colorOuter = 0x000000;

        // ИСПРАВЛЕНО: начальная точка 45°, конечная 315° (диапазон 270° против часовой)
        // progress = 0 -> angle = 45° (холодный, стрелка вверху справа)
        // progress = 1 -> angle = 315° (горячий, стрелка справа, прошла 270° ПРОТИВ часовой)
        float angle = (float) Math.toRadians(45.0 + progress * 270.0);

        Vector3f tip = new Vector3f(0, (float)tipLength, 0);
        Vector3f left = new Vector3f((float)backSide, (float)(-backLength), 0);
        Vector3f right = new Vector3f((float)(-backSide), (float)(-backLength), 0);

        rotateAroundZ(tip, angle);
        rotateAroundZ(left, angle);
        rotateAroundZ(right, angle);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // Внешний слой (темный контур)
        double mult = 1.3;
        
        buffer.vertex(matrix, centerX + (float)(tip.x * mult), centerY + (float)(tip.y * mult), 0)
              .color((colorOuter >> 16) & 0xFF, (colorOuter >> 8) & 0xFF, colorOuter & 0xFF, 255)
              .endVertex();
        buffer.vertex(matrix, centerX + (float)(left.x * mult), centerY + (float)(left.y * mult), 0)
              .color((colorOuter >> 16) & 0xFF, (colorOuter >> 8) & 0xFF, colorOuter & 0xFF, 255)
              .endVertex();
        buffer.vertex(matrix, centerX + (float)(right.x * mult), centerY + (float)(right.y * mult), 0)
              .color((colorOuter >> 16) & 0xFF, (colorOuter >> 8) & 0xFF, colorOuter & 0xFF, 255)
              .endVertex();

        // Внутренний слой (красная стрелка)
        buffer.vertex(matrix, centerX + tip.x, centerY + tip.y, 0)
              .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255)
              .endVertex();
        buffer.vertex(matrix, centerX + left.x, centerY + left.y, 0)
              .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255)
              .endVertex();
        buffer.vertex(matrix, centerX + right.x, centerY + right.y, 0)
              .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255)
              .endVertex();

        BufferUploader.drawWithShader(buffer.end());
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

    private void renderPressArrow(GuiGraphics guiGraphics, int x, int y) {
        int press = menu.getPress();
        int maxPress = menu.getMaxPress();
        
        if (press > 0 && maxPress > 0) {
            int arrowHeight = (press * ARROW_MAX_HEIGHT) / maxPress;
            
            if (arrowHeight > 0) {
                guiGraphics.blit(TEXTURE,
                        x + 79, y + 35,
                        ARROW_U, ARROW_V,
                        ARROW_WIDTH, arrowHeight,
                        256, 256);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

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
            
            if (burnTime > 0) {
                int operationsLeft = burnTime / 200;
                guiGraphics.renderTooltip(this.font,
                        Component.literal(operationsLeft + " operations left"),
                        mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font,
                        Component.literal("No fuel"),
                        mouseX, mouseY);
            }
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
