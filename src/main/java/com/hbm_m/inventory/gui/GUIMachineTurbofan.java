package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.inventory.menu.MachineTurbofanMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIMachineTurbofan} (1.7.10-Original): imageWidth/imageHeight (176x203) und
 * Label-Positionen 1:1 uebernommen. Original nutzt {@code GUIElements}-Fluid-Gauges fuer den
 * Kerosin-Tank und drawTexturedModalRect-UVs (176+16, 52-i) fuer den Energie-Balken; die Original-
 * Textur existiert 1:1 unter {@code textures/gui/generators/gui_turbofan.png}, daher wird der
 * Energie-Balken exakt wie im Original per Textur-UV geblittet. Der Kerosin-Tankfuellstand
 * verwendet - analog zum bereits portierten Combustion Engine - ein Fuellrechteck statt der
 * komplexen Original-Gauge-Klasse (kein 1:1-Aequivalent in diesem Repo). Afterburner-Anzeige und
 * Blut-Gauge des Originals entfallen (siehe {@link MachineTurbofanBlockEntity}-Klassenkommentar:
 * kein Upgrade-System, kein Blut-Fluid an diesem Block).
 */
public class GUIMachineTurbofan extends AbstractContainerScreen<MachineTurbofanMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_turbofan.png");

    private static final int TANK_X = 35;
    private static final int TANK_Y = 17;
    private static final int TANK_W = 34;
    private static final int TANK_H = 52;
    private static final int KEROSENE_COLOR = 0xFFFFA5D2;

    private static final int POWER_X = 143;
    private static final int POWER_Y = 17;
    private static final int POWER_H = 52;

    private final MachineTurbofanBlockEntity turbofan;

    public GUIMachineTurbofan(MachineTurbofanMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.turbofan = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Energie-Balken: 1:1-Original-UV (drawTexturedModalRect(guiLeft+143, guiTop+69-i, 176+16, 52-i, 16, i))
        long maxPower = turbofan.getMaxEnergyStored();
        int power = maxPower > 0 ? (int) (turbofan.getEnergyStored() * POWER_H / maxPower) : 0;
        if (power > POWER_H) power = POWER_H;
        if (power > 0) {
            guiGraphics.blit(TEXTURE, x + POWER_X, y + POWER_Y + (POWER_H - power), 192, POWER_H - power, 16, power);
        }

        // Kerosin-Tank: Fuellrechteck-Fallback (Original nutzt GUIElements.Gauge, hier nicht 1:1 verfuegbar)
        var tank = turbofan.getTank();
        int fuelH = tank.getMaxFill() > 0 ? tank.getFill() * TANK_H / tank.getMaxFill() : 0;
        if (fuelH > 0) {
            guiGraphics.fill(x + TANK_X, y + TANK_Y + (TANK_H - fuelH), x + TANK_X + TANK_W, y + TANK_Y + TANK_H, KEROSENE_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 43 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (isPointInRect(POWER_X, POWER_Y, 16, POWER_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.hbm_m.energy",
                    EnergyFormatter.format(turbofan.getEnergyStored()), EnergyFormatter.format(turbofan.getMaxEnergyStored())), mouseX, mouseY);
        }
        if (isPointInRect(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            var tank = turbofan.getTank();
            guiGraphics.renderTooltip(font, Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB"), mouseX, mouseY);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private boolean isPointInRect(int relX, int relY, int width, int height, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        return x >= relX && x < relX + width && y >= relY && y < relY + height;
    }
}
