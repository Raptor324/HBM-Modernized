package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCoreReceiverBlockEntity;
import com.hbm_m.client.GuiCompat;
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
 * 1:1-Port von {@code GUICoreReceiver} (1.7.10), 176x166.
 *
 * <p>Links der Cryogeltank bei (8, 17), daneben die beiden Zahlen, auf die es ankommt: was in
 * diesem Tick als Strahl ankam, und was der Empfaenger daraus macht - das Fuenftausendfache. An
 * diesen zwei Zeilen liest man ab, ob sich eine Anlage rechnet.</p>
 *
 * <p>Der Tankfuellstand wird als farbiges Rechteck gezeichnet, weil die Originaltextur dafuer
 * keine eigenen Bildkoordinaten mitbringt.
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

        // Original: receiver.tank.renderTank(guiLeft + 8, guiTop + 69, ..., 16, 52) - Port-renderTank erwartet die Oberkante
        if (receiver != null) { // тайл может отсутствовать в реплее Flashback
            receiver.getCoolantTank().renderTank(guiGraphics, this.leftPos + 8, this.topPos + 69 - 52, 16, 52);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);

        if (receiver != null) {
            // 1:1 aus dem Original: Eingang in Spk, Ausgang im Fuenftausendfachen als HE.
            guiGraphics.drawString(this.font, "Input:", 40, 25, 0xFF7F7F, false);
            guiGraphics.drawString(this.font,
                    EnergyFormatter.format(receiver.getJoules()) + "Spk", 50, 35, 0xFF7F7F, false);
            guiGraphics.drawString(this.font, "Output:", 40, 45, 0xFF7F7F, false);
            guiGraphics.drawString(this.font,
                    EnergyFormatter.format(receiver.getJoules() * MachineCoreReceiverBlockEntity.HE_PER_SPK) + "HE",
                    50, 55, 0xFF7F7F, false);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (receiver != null) { // тайл может отсутствовать в реплее Flashback
            receiver.getCoolantTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 8, topPos + 17, 16, 52);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
