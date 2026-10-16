package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.rbmk.RBMKOutgasserBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.RBMKOutgasserMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Port of {@code GUIRBMKOutgasser} (1.7.10 Original). */
public class GUIRBMKOutgasser extends GuiInfoScreen<RBMKOutgasserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_rbmk_outgasser.png");

    /** Modernized Outgasser has no rod-max constant on the block entity; 1500 mirrors {@code RBMKColumnBlockEntity#maxHeat()}. */
    private static final double MAX_HEAT = 1500.0;

    private final RBMKOutgasserBlockEntity be;

    public GUIRBMKOutgasser(RBMKOutgasserMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // тайл может отсутствовать в реплее Flashback
        if (be == null) return;

        // Progress bar (original: rod.progress/rod.duration). The modernized outgasser has no
        // gradual processing timer -- it consumes the rod's Xenon poison instantly whenever flux
        // is available. Repurposed to show the rod's current Xenon poison level (0-100%).
        ItemStack rod = be.rodSlot;
        double poisonPct = (!rod.isEmpty() && rod.getItem() instanceof RBMKRodItem) ? RBMKRodItem.getPoison(rod) : 0;
        int progress = (int) (poisonPct * 13 / 100.0);
        if (progress > 0) {
            g.blit(TEXTURE, this.leftPos + 82, this.topPos + 50, 176, 0, progress, 6);
        }

        // Fluid/gas bar (original: rod.gas fluid tank). The modernized outgasser has no fluid
        // tank -- its closest analog is the column's accumulated heat. Repurposed as a heat gauge.
        int gas = (int) (Math.min(be.heat, MAX_HEAT) * 42 / MAX_HEAT);
        if (gas > 0) {
            g.blit(TEXTURE, this.leftPos + 115, this.topPos + 66 - gas, 188, 42 - gas, 10, gas);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Component name = this.title;
        g.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        // тайл может отсутствовать в реплее Flashback
        if (be != null) {
        ItemStack rod = be.rodSlot;
        double poisonPct = (!rod.isEmpty() && rod.getItem() instanceof RBMKRodItem) ? RBMKRodItem.getPoison(rod) : 0;

        drawCustomInfoStat(g, mouseX, mouseY,
                82, 50, 13, 6,
                mouseX, mouseY,
                Component.literal(String.format("Xenon: %.2f%%", poisonPct)));

        drawCustomInfoStat(g, mouseX, mouseY,
                115, 24, 10, 42,
                mouseX, mouseY,
                Component.literal(String.format("Heat: %.1f / %.0f", be.heat, MAX_HEAT)));

        drawCustomInfoStat(g, mouseX, mouseY,
                48, 45, 16, 16,
                mouseX, mouseY,
                Component.literal(String.format("Flux buffer: %.2f", be.fluxBuffer)));
        }

        this.renderTooltip(g, mouseX, mouseY);
    }
}
