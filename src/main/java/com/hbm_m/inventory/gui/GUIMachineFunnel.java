package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineFunnelBlockEntity;
import com.hbm_m.inventory.menu.MachineFunnelMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.FunnelModeC2SPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIFunnel} (1.7.10): Modusschalter als Textur-Sprite bei (159, 73),
 * {@code 176, mode * 10}, Klick schaltet weiter (Original: NBTControlPacket "toggle").
 */
public class GUIMachineFunnel extends GuiInfoScreen<MachineFunnelMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_funnel.png");

    private final MachineFunnelBlockEntity blockEntity;

    public GUIMachineFunnel(MachineFunnelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 168;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean handled = super.mouseClicked(x, y, button);
        // Original checkClick(x, y, 159, 73, 10, 10)
        if (blockEntity != null && leftPos + 159 <= x && leftPos + 159 + 10 > x && topPos + 73 < y && topPos + 73 + 10 >= y) {
            playClickSound();
            FunnelModeC2SPacket.sendToServer(blockEntity.getBlockPos());
            return true;
        }
        return handled;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            guiGraphics.blit(TEXTURE, leftPos + 159, topPos + 73, 176, blockEntity.getMode() * 10, 10, 10);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(font, name, imageWidth / 2 - font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (blockEntity != null) {
            int mode = blockEntity.getMode();
            this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 159, 73, 10, 10, mouseX, mouseY,
                    Component.literal("Mode: " + (mode == 1 ? "3x3 only" : mode == 2 ? "2x2 only" : "3x3 then 2x2")));
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
