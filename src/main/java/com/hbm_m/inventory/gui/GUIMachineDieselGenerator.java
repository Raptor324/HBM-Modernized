package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineDieselGeneratorBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineDieselGeneratorMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIMachineDiesel} (176x203): Tank (35,17), Stromanzeige (141,17), Zuendknopf (89,61),
 * Laufanzeige, Info-Panel zum Verbrauch und Fehler-Panel bei ungeeignetem Treibstoff. Kein Titel (Original).
 */
public class GUIMachineDieselGenerator extends GuiInfoScreen<MachineDieselGeneratorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_diesel.png");

    private final MachineDieselGeneratorBlockEntity diesel;

    public GUIMachineDieselGenerator(MachineDieselGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.diesel = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        if (diesel == null) return; // тайл может отсутствовать в реплее Flashback

        diesel.getTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 35, topPos + 69 - 52, 16, 52);
        this.drawElectricityInfo(guiGraphics, mouseX, mouseY, 141, 69 - 52, 16, 52, diesel.getEnergyStored(), diesel.getMaxEnergyStored());

        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, -8, 36, 16, 16, leftPos, topPos + 36 + 16,
                Component.literal("Fuel consumption rate:"),
                Component.literal("  1 mB/t"),
                Component.literal("  20 mB/s"),
                Component.literal("(Consumption rate is constant)"));

        if (!diesel.hasAcceptableFuel()) {
            this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, -8, 36 + 32, 16, 16, leftPos, topPos + 36 + 16 + 32,
                    Component.literal("Error: The currently set fuel type"),
                    Component.literal("is not supported by this engine!"));
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean result = super.mouseClicked(x, y, button);

        if (diesel != null && leftPos + 89 <= x && leftPos + 89 + 16 > x && topPos + 61 < y && topPos + 61 + 14 >= y) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("turnOn", true);
            NBTControlPacket.sendToServer(diesel.getBlockPos(), data);
        }
        return result;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (diesel != null) {
            long power = diesel.getEnergyStored();
            long powerCap = diesel.getMaxEnergyStored();
            if (power > 0 && powerCap > 0) {
                int i = (int) (power * 52 / powerCap);
                guiGraphics.blit(TEXTURE, leftPos + 141, topPos + 69 - i, 176, 52 - i, 16, i);
            }

            if (diesel.isOn()) guiGraphics.blit(TEXTURE, leftPos + 79, topPos + 61, 192, 16, 35, 14);
            if (diesel.wasOn()) guiGraphics.blit(TEXTURE, leftPos + 89, topPos + 42, 192, 0, 16, 16);
        }

        this.drawInfoPanel(guiGraphics, -8, 36, PanelType.LARGE_BLUE_INFO);

        if (diesel != null && !diesel.hasAcceptableFuel())
            this.drawInfoPanel(guiGraphics, -8, 36 + 32, PanelType.LARGE_RED_EXCLAMATION);

        if (diesel != null) diesel.getTank().renderTank(guiGraphics, leftPos + 35, topPos + 69 - 52, 16, 52);
    }
}
