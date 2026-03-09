package com.hbm_m.inventory.gui;

import com.hbm_m.block.entity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.inventory.menu.LaunchPadLargeMenu;
import com.hbm_m.lib.RefStrings;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Новый GUI для обычной/большой пусковой площадки.
 *
 * Основан на GuiInfoScreen, как и остальные портированные машины.
 * Ракетная логика и отрисовка моделей пока отсутствуют — только базовый
 * фон, энергия и текстовый индикатор состояния.
 */
public class GUILaunchPadLarge extends GuiInfoScreen<LaunchPadLargeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/gui_launch_pad_large.png");

    public GUILaunchPadLarge(LaunchPadLargeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 236;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        LaunchPadBaseBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return;
        }

        // Энергия — простая вертикальная шкала, 52 пикселя как в старом GUI
        long energy = be.getEnergyStored();
        long maxEnergy = be.getMaxEnergyStored();
        if (maxEnergy > 0 && energy > 0) {
            int height = (int) (energy * 52 / maxEnergy);
            if (height > 0) {
                guiGraphics.blit(TEXTURE,
                        this.leftPos + 107,
                        this.topPos + 88 - height,
                        176,
                        52 - height,
                        16,
                        height);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font,
                title,
                this.imageWidth / 2 - this.font.width(title) / 2,
                4,
                0x404040,
                false);

        guiGraphics.drawString(this.font,
                this.playerInventoryTitle,
                8,
                this.imageHeight - 96 + 2,
                0x404040,
                false);

        LaunchPadBaseBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            String text = "Not ready";
            int color = 0xFF0000;
            switch (be.getState()) {
                case LaunchPadBaseBlockEntity.STATE_LOADING -> {
                    text = "Loading...";
                    color = 0xFF8000;
                }
                case LaunchPadBaseBlockEntity.STATE_READY -> {
                    text = "Ready";
                    color = 0x00FF00;
                }
                default -> {
                }
            }

            int x = 34;
            int y = 107;
            int scaledColor = color | 0xFF000000;
            guiGraphics.drawString(this.font,
                    Component.literal(text),
                    x,
                    y,
                    scaledColor,
                    false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        LaunchPadBaseBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            // Подсказка по энергии при наведении на столбик (та же область, что и раньше)
            int relX = this.leftPos + 107;
            int relY = this.topPos + 36;
            if (isPointInRect(relX, relY, 16, 52, mouseX, mouseY)) {
                drawElectricityInfo(guiGraphics, mouseX, mouseY,
                        relX, relY, 16, 52,
                        be.getEnergyStored(), be.getMaxEnergyStored());
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
