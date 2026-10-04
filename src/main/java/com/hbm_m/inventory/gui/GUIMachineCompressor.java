package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCompressorBaseBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineCompressorMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUICompressor}: Druckstufenwahl (0-4 PU) per Klick, Tanks, Fortschritt, Energie. */
public class GUIMachineCompressor extends GuiInfoScreen<MachineCompressorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_compressor.png");

    private final MachineCompressorBaseBlockEntity compressor;

    public GUIMachineCompressor(MachineCompressorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.compressor = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);

        if (compressor != null) {
            compressor.tanks[0].renderTankInfo(g, font, mouseX, mouseY, leftPos + 17, topPos + 18, 16, 52);
            compressor.tanks[1].renderTankInfo(g, font, mouseX, mouseY, leftPos + 107, topPos + 18, 16, 52);
            drawElectricityInfo(g, mouseX, mouseY, 152, 18, 16, 52, compressor.getEnergyStored(), MachineCompressorBaseBlockEntity.maxPower);

            for (int j = 0; j < 5; j++) drawCustomInfoStat(g, mouseX, mouseY, 43 + j * 11, 46, 8, 14, mouseX, mouseY, Component.literal(j + " PU -> " + (j + 1) + " PU"));
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (compressor != null) {
            int x = (int) mx, y = (int) my;
            for (int j = 0; j < 5; j++) {

                if (leftPos + 43 + j * 11 <= x && leftPos + 43 + 8 + j * 11 > x && topPos + 46 < y && topPos + 46 + 14 >= y) {

                    playClickSound();
                    CompoundTag data = new CompoundTag();
                    data.putInt("compression", j);
                    com.hbm_m.network.NBTControlPacket.sendToServer(compressor.getBlockPos(), data);
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Component name = this.title;
        g.drawString(font, name, 70 - font.width(name) / 2, 6, 0xC7C1A3, false);
        g.drawString(font, playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (compressor == null) return; // тайл может отсутствовать в реплее Flashback

        if (compressor.getEnergyStored() >= compressor.powerRequirement) {
            g.blit(TEXTURE, leftPos + 156, topPos + 4, 176, 52, 9, 12);
        }

        g.blit(TEXTURE, leftPos + 43 + compressor.tanks[0].getPressure() * 11, topPos + 46, 193, 18, 8, 124);

        int i = compressor.progress * 55 / Math.max(compressor.processTime, 1);
        g.blit(TEXTURE, leftPos + 42, topPos + 26, 192, 0, i, 17);

        int j = (int) (compressor.getEnergyStored() * 52 / MachineCompressorBaseBlockEntity.maxPower);
        g.blit(TEXTURE, leftPos + 152, topPos + 70 - j, 176, 52 - j, 16, j);

        compressor.tanks[0].renderTank(g, leftPos + 17, topPos + 18, 16, 52);
        compressor.tanks[1].renderTank(g, leftPos + 107, topPos + 18, 16, 52);
    }
}
