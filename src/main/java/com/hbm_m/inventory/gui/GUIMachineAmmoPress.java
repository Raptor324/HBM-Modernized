package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineAmmoPressBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineAmmoPressMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur (nur der statische Rahmen, keine geratenen Icon-
 * Koordinaten daraus) - der "Press"-Blitz beim erfolgreichen Fertigen als eigenes Fuellrechteck-
 * Overlay statt der Original-Kolben-3D-Animation (siehe Klassenkommentar in
 * {@link MachineAmmoPressBlockEntity}). Die durchsuchbare Rezeptauswahl-Liste des Originals
 * entfaellt (rein kosmetisch dort - siehe selbiger Kommentar).
 */
public class GUIMachineAmmoPress extends AbstractContainerScreen<MachineAmmoPressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_ammo_press.png");

    private final MachineAmmoPressBlockEntity blockEntity;

    public GUIMachineAmmoPress(MachineAmmoPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 176;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (blockEntity != null && blockEntity.isPressing()) { // тайл может отсутствовать в реплее Flashback
            guiGraphics.fill(x + 96, y + 20, x + 116, y + 52, 0xA0FF3020);
        }
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
