package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCoreReceiverBlockEntity;
import com.hbm_m.inventory.menu.MachineCoreReceiverMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Port von {@code GUICoreReceiver} (1.7.10 Original). Verwendet die bereits portierte Textur
 * {@code textures/gui/dfc/gui_receiver.png} (176x166, wie im Original).
 * <p>
 * Vereinfachung ggue. Original: das Original zeigte "Input: X Spk" (pro-Tick Laserenergie) und
 * "Output: X*5000 HE" - dieses zweigeteilte SPK/HE-System existiert im modernisierten Energienetz
 * nicht mehr (siehe {@link MachineCoreReceiverBlockEntity}). Stattdessen wird der aktuelle
 * Energiespeicherstand (Energy: X / Y) sowie der Cryogel-Kuehlmittelstand angezeigt. Der Tankfuellstand
 * wird - da die Originaltextur keine eigenen UV-Koordinaten fuer eine gefuellte Tankgrafik bereitstellt -
 * als einfaches farbiges Rechteck ueber der Tankaussparung gezeichnet (an derselben Position wie im
 * Original: links 8, oben 69, 16 breit, 52 hoch).
 */
public class GUIMachineCoreReceiver extends GuiInfoScreen<MachineCoreReceiverMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/dfc/gui_receiver.png");

    private final MachineCoreReceiverBlockEntity receiver;

    public GUIMachineCoreReceiver(MachineCoreReceiverMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.receiver = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Original: receiver.tank.renderTank(guiLeft + 8, guiTop + 69, ..., 16, 52) - kein UV-Bereich
        // fuer die gefuellte Tankgrafik in der Original-Textur vorhanden, daher als Fuellstands-
        // Rechteck (Cryogel = hellblau) nachgebildet, bottom-up wie im Original.
        int capacity = receiver.getCoolantTank().getMaxFill();
        int fill = receiver.getCoolantTank().getFill();
        if (capacity > 0 && fill > 0) {
            int tankHeight = 52;
            int filled = Math.min(tankHeight, fill * tankHeight / capacity);
            if (filled > 0) {
                int x0 = this.leftPos + 8;
                int y1 = this.topPos + 69 + tankHeight;
                int y0 = y1 - filled;
                guiGraphics.fill(x0, y0, x0 + 16, y1, 0xFF7FE0FF);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);

        guiGraphics.drawString(this.font, "Energy:", 40, 25, 0xFF7F7F, false);
        guiGraphics.drawString(this.font,
                EnergyFormatter.format(receiver.getEnergyStored()) + " / " + EnergyFormatter.format(receiver.getMaxEnergyStored()),
                40, 35, 0xFF7F7F, false);

        guiGraphics.drawString(this.font, "Coolant:", 40, 47, 0xFF7F7F, false);
        guiGraphics.drawString(this.font,
                receiver.getCoolantTank().getFill() + " / " + receiver.getCoolantTank().getMaxFill() + "mB",
                40, 57, 0xFF7F7F, false);

        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                40, 25, 100, 20,
                receiver.getEnergyStored(), receiver.getMaxEnergyStored());

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
