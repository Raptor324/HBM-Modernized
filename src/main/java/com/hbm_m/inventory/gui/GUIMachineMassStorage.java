package com.hbm_m.inventory.gui;

import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineMassStorageBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineMassStorageMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIMassStorage} (1.7.10 Original): Fuellstandsbalken (97, 17-105) mit
 * Anzahl/Prozent, Knopf "ausgeben" (62, 72) und Ausgabe-Schalter (80, 72).
 */
public class GUIMachineMassStorage extends GuiInfoScreen<MachineMassStorageMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_mass_storage.png");

    private final MachineMassStorageBlockEntity massStorage;

    public GUIMachineMassStorage(MachineMassStorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.massStorage = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 221;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean handled = super.mouseClicked(x, y, button);
        if (massStorage == null) return handled; // тайл может отсутствовать в реплее Flashback

        if (leftPos + 62 <= x && leftPos + 62 + 14 > x && topPos + 72 < y && topPos + 72 + 14 >= y) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("provide", Screen.hasShiftDown());
            com.hbm_m.network.NBTControlPacket.sendToServer(massStorage.getBlockPos(), data);
            return true;
        }

        if (leftPos + 80 <= x && leftPos + 80 + 14 > x && topPos + 72 < y && topPos + 72 + 14 >= y) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", false);
            com.hbm_m.network.NBTControlPacket.sendToServer(massStorage.getBlockPos(), data);
            return true;
        }
        return handled;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (massStorage == null) return; // тайл может отсутствовать в реплее Flashback

        long cap = Math.max(1L, massStorage.getCapacity());
        int gauge = (int) (massStorage.getStockpile() * 88 / cap);
        guiGraphics.blit(TEXTURE, leftPos + 97, topPos + 105 - gauge, 176, 88 - gauge, 16, gauge);

        if (massStorage.output) {
            guiGraphics.blit(TEXTURE, leftPos + 80, topPos + 72, 192, 0, 14, 14);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (massStorage != null) {
            long cap = Math.max(1L, massStorage.getCapacity());
            String percent = (((int) (massStorage.getStockpile() * 1000D / (double) cap)) / 10D) + "%";
            this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 96, 16, 18, 90, mouseX, mouseY,
                    Component.literal(String.format(Locale.US, "%,d", massStorage.getStockpile()) + " / "
                            + String.format(Locale.US, "%,d", massStorage.getCapacity())),
                    Component.literal(percent));
        }
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 62, 72, 14, 14, mouseX, mouseY,
                Component.literal("Click: Provide one"), Component.literal("Shift-click: Provide stack"));
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 80, 72, 14, 14, mouseX, mouseY,
                Component.literal("Toggle output"));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
