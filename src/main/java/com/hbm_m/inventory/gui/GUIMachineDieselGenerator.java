package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineDieselGeneratorMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur - Tank-/Energieanzeigen als eigene
 * Fuellrechteck-Overlays.
 *
 * <p>Der <b>An/Aus-Knopf</b> sitzt wie im Original unter der Laufanzeige: der obere Kreis meldet,
 * dass der Motor wirklich brennt, der Knopf darunter schaltet ihn.</p>
 */
public class GUIMachineDieselGenerator extends AbstractContainerScreen<MachineDieselGeneratorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_diesel.png");

    private final MachineDieselGeneratorBlockEntity blockEntity;

    public GUIMachineDieselGenerator(MachineDieselGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            var tank = blockEntity.getTank();
            int fuelH = tank.getMaxFill() > 0 ? tank.getFill() * 52 / tank.getMaxFill() : 0;
            if (fuelH > 0) guiGraphics.fill(x + 35, y + 69 + (52 - fuelH), x + 51, y + 121, 0xFF804000);

            long max = blockEntity.getMaxEnergyStored();
            long energy = blockEntity.getEnergyStored();
            int energyH = max > 0 ? (int) (energy * 52L / max) : 0;
            if (energyH > 0) guiGraphics.fill(x + 141, y + 69 + (52 - energyH), x + 157, y + 121, 0xFFFF3020);

            // Original: die Laufanzeige aus (192,0) und der gedrueckte Knopf aus (192,16).
            if (blockEntity.wasOn()) guiGraphics.blit(TEXTURE, x + 89, y + 42, 192, 0, 16, 16);
            if (blockEntity.isOn())  guiGraphics.blit(TEXTURE, x + 79, y + 61, 192, 16, 35, 14);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Original-Trefferflaeche: (89,61), 16 x 14.
        if (blockEntity != null && isHovering(89, 61, 16, 14, mouseX, mouseY)) {
            com.hbm_m.network.DieselGeneratorToggleC2SPacket.send(blockEntity.getBlockPos());
            if (minecraft != null) {
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
