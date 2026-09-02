package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.rbmk.RBMKOutgasserBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.RBMKOutgasserMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 port of {@code GUIRBMKOutgasser}: a flux progress bar at (66,58) and the gas tank gauge at
 * (143,23).
 *
 * <p>Both readouts used to show something else entirely - the progress bar was repurposed as a
 * xenon-poison meter and the tank gauge as a heat gauge, because the block entity had neither a
 * progress value nor a tank. It has both now.</p>
 */
public class GUIRBMKOutgasser extends GuiInfoScreen<RBMKOutgasserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_rbmk_outgasser.png");

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

        int progress = be.duration > 0 ? (int) (be.progress * 45 / be.duration) : 0;
        progress = Math.max(0, Math.min(progress, 45));
        if (progress > 0) {
            g.blit(TEXTURE, this.leftPos + 66, this.topPos + 58, 190, 0, progress, 6);
        }

        int gas = be.gasTank.getMaxFill() > 0
                ? (int) ((long) be.gasTank.getFill() * 58 / be.gasTank.getMaxFill()) : 0;
        gas = Math.max(0, Math.min(gas, 58));
        if (gas > 0) {
            g.blit(TEXTURE, this.leftPos + 143, this.topPos + 82 - gas, 176, 58 - gas, 14, gas);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Component name = this.title;
        g.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
        g.drawString(this.font, "Flux", 21, 34, 0x404040, false);

        String fluxNumbers = formatNumber((float) be.progress) + "/" + formatNumber(be.duration);
        g.drawString(this.font, fluxNumbers, 123 - this.font.width(fluxNumbers), 34, 0x46EA00, false);
    }

    /** CE's own SI-ish formatter, kept identical so the readout lines up the same way. */
    private static String formatNumber(float number) {
        if (number < 1000F)             return String.format("%5.1f ", number);
        if (number < 1000000F)          return String.format("%5.1fk", number / 1000F);
        if (number < 1000000000F)       return String.format("%5.1fM", number / 1000000F);
        if (number < 1000000000000F)    return String.format("%5.1fG", number / 1000000000F);
        if (number < 1000000000000000F) return String.format("%5.1fT", number / 1000000000000F);
        return "";
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        drawCustomInfoStat(g, mouseX, mouseY, 66, 58, 45, 6, mouseX, mouseY,
                Component.literal(String.format("Flux: %.1f / %d", be.progress, be.duration)));

        drawCustomInfoStat(g, mouseX, mouseY, 143, 23, 14, 58, mouseX, mouseY,
                Component.literal(be.gasTank.getFill() + " / " + be.gasTank.getMaxFill() + " mB"));

        this.renderTooltip(g, mouseX, mouseY);
    }
}
