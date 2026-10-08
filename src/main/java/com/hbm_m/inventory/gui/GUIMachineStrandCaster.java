package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineStrandCasterBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineStrandCasterMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIMachineStrandCaster}: Schmelzsaeule (17,93) in der Farbe des Materials, Wasser- (82,14) und Dampftank
 * (82,65) je 16x24.
 */
public class GUIMachineStrandCaster extends GuiInfoScreen<MachineStrandCasterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_strand_caster.png");

    private final MachineStrandCasterBlockEntity caster;

    public GUIMachineStrandCaster(MachineStrandCasterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.caster = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 214;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        if (caster != null) {
            Component info = caster.type == null
                    ? Component.literal("Empty").withStyle(ChatFormatting.RED)
                    : caster.type.getLocalizedName().copy().append(": " + caster.formatAmount(Screen.hasShiftDown())).withStyle(ChatFormatting.YELLOW);
            drawCustomInfoStat(g, x, y, 16, 17, 36, 81, x, y, info);
            caster.water.renderTankInfo(g, this.font, x, y, leftPos + 82, topPos + 14, 16, 24);
            caster.steam.renderTankInfo(g, this.font, x, y, leftPos + 82, topPos + 65, 16, 24);
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 4, 0xffffff, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (caster == null) return;

        if (caster.amount != 0 && caster.type != null) {
            int targetHeight = Math.min(caster.amount * 79 / caster.getCapacity(), 92);

            int hex = caster.type.moltenColor;
            RenderSystem.setShaderColor((hex >> 16 & 255) / 255F, (hex >> 8 & 255) / 255F, (hex & 255) / 255F, 1F);
            g.blit(TEXTURE, leftPos + 17, topPos + 93 - targetHeight, 176, 89 - targetHeight, 34, targetHeight);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1F, 1F, 1F, 0.3F);
            g.blit(TEXTURE, leftPos + 17, topPos + 93 - targetHeight, 176, 89 - targetHeight, 34, targetHeight);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }

        caster.water.renderTank(g, leftPos + 82, topPos + 14, 16, 24);
        caster.steam.renderTank(g, leftPos + 82, topPos + 65, 16, 24);
    }
}
