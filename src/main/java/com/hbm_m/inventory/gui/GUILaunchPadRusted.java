package com.hbm_m.inventory.gui;

import com.hbm_m.block.entity.machines.LaunchPadRustedBlockEntity;
import com.hbm_m.inventory.menu.LaunchPadRustedMenu;
import com.hbm_m.lib.RefStrings;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Новый GUI для ржавой пусковой площадки.
 *
 * Пока отображает только фон и инвентарь, без сетевых пакетов
 * и сложной логики "Release Missile" из 1.7.10. Всё, что связано
 * с управлением ракетой, будет добавлено позже.
 */
public class GUILaunchPadRusted extends GuiInfoScreen<LaunchPadRustedMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/gui_launch_pad_rusted.png");

    public GUILaunchPadRusted(LaunchPadRustedMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 236;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        LaunchPadRustedBlockEntity be = menu.getBlockEntity();
        if (be == null) {
            return;
        }

        // Простые индикаторы наличия ключа/кодов можно будет восстановить позже.
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
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
