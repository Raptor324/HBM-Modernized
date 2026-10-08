package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.albion.PAQuadrupoleBlockEntity;
import com.hbm_m.inventory.menu.PAQuadrupoleMenu;
import com.hbm_m.item.machine.ItemPACoil;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code GUIPAQuadrupole}: Lampen Kuehlung (75, 64), Spule (85, 64), Energie (65, 64) und Spulenbild (65, 30). */
public class GUIPAQuadrupole extends GUIPABase<PAQuadrupoleMenu> {

    public GUIPAQuadrupole(PAQuadrupoleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, "gui_quadrupole.png", 26, 116, 118);
    }

    @Override
    protected void renderIndicators(GuiGraphics g, int mouseX, int mouseY) {
        if (heatOk()) g.blit(texture, leftPos + 75, topPos + 64, 176, 8, 8, 8);
        ItemStack coil = be().getInventory().getStackInSlot(PAQuadrupoleBlockEntity.SLOT_COIL);
        if (!coil.isEmpty() && coil.getItem() instanceof ItemPACoil) {
            g.blit(texture, leftPos + 85, topPos + 64, 176, 8, 8, 8);
            ItemPACoil.CoilType type = ItemPACoil.typeOf(coil);
            if (type == ItemPACoil.CoilType.GOLD) g.blit(texture, leftPos + 65, topPos + 30, 200, 0, 28, 28);
            if (type == ItemPACoil.CoilType.NIOBIUM) g.blit(texture, leftPos + 65, topPos + 30, 228, 0, 28, 28);
            if (type == ItemPACoil.CoilType.BSCCO) g.blit(texture, leftPos + 65, topPos + 30, 200, 28, 28, 28);
            if (type == ItemPACoil.CoilType.CHLOROPHYTE) g.blit(texture, leftPos + 65, topPos + 30, 228, 28, 28, 28);
        }
        if (be().getEnergyStored() >= PAQuadrupoleBlockEntity.getUsage()) g.blit(texture, leftPos + 65, topPos + 64, 176, 8, 8, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        drawTitle(g, -9, 6, 0xffffff);
        super.renderLabels(g, mouseX, mouseY);
    }
}
