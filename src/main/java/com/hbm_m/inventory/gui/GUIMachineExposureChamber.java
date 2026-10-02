package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineExposureChamberMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIMachineExposureChamber}. */
public class GUIMachineExposureChamber extends GuiInfoScreen<MachineExposureChamberMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_exposure_chamber.png");

    private final MachineExposureChamberBlockEntity chamber;

    public GUIMachineExposureChamber(MachineExposureChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.chamber = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);

        if (chamber != null) {
            drawElectricityInfo(g, mouseX, mouseY, 152, 18, 16, 34, chamber.getEnergyStored(), MachineExposureChamberBlockEntity.maxPower);
            drawCustomInfoStat(g, mouseX, mouseY, 26, 36, 9, 16, mouseX, mouseY,
                    Component.literal(chamber.savedParticles + " / " + MachineExposureChamberBlockEntity.maxParticles));
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int i, int j) {
        g.drawString(font, title, 70 - font.width(title) / 2, 6, 4210752, false);
        g.drawString(font, playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (chamber == null) return; // тайл может отсутствовать в реплее Flashback

        int p = chamber.progress * 42 / (chamber.processTime + 1);
        g.blit(TEXTURE, leftPos + 36, topPos + 39, 192, 0, p, 10);

        int c = chamber.savedParticles * 16 / MachineExposureChamberBlockEntity.maxParticles;
        g.blit(TEXTURE, leftPos + 26, topPos + 52 - c, 192, 26 - c, 9, c);

        int e = (int) (chamber.getEnergyStored() * 34 / MachineExposureChamberBlockEntity.maxPower);
        g.blit(TEXTURE, leftPos + 152, topPos + 52 - e, 176, 34 - e, 16, e);

        if (chamber.consumption <= chamber.getEnergyStored()) {
            g.blit(TEXTURE, leftPos + 156, topPos + 4, 176, 34, 9, 12);
        }
    }
}
