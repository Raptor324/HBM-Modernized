package com.hbm_m.inventory.gui;

import java.awt.Color;

import com.hbm_m.blockentity.machines.MachineFelBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineFelMenu;
import com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIFEL}: 203x169, Energiesaeule rechts, Ein/Aus-Schalter, "LIVE"/"ERR."-Anzeige und der farbige
 * Strahlstrich (sichtbares Licht im Regenbogen).
 */
public class GUIMachineFel extends GuiInfoScreen<MachineFelMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_fel.png");

    private final MachineFelBlockEntity fel;

    public GUIMachineFel(MachineFelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.fel = menu.getBlockEntity();
        this.imageWidth = 203;
        this.imageHeight = 169;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        if (fel != null) drawElectricityInfo(g, mouseX, mouseY, 182, 27, 16, 113, fel.getEnergyStored(), MachineFelBlockEntity.maxPower);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (fel != null && leftPos + 142 <= x && leftPos + 142 + 29 > x && topPos + 41 < y && topPos + 41 + 17 >= y) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            com.hbm_m.network.NBTControlPacket.sendToServer(fel.getBlockPos(), data);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        String name = this.title.getString();
        g.drawString(this.font, name, 90 + this.imageWidth / 2 - this.font.width(name) / 2, 7, 0xffffff, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 98, 4210752, false);

        if (fel == null) return;
        if (fel.missingValidSilex && fel.isOn) {
            g.drawString(this.font, "ERR.", 55 + this.imageWidth / 2 - this.font.width(name) / 2, 9, 0xFF0000, false);
        } else if (fel.isOn) {
            g.drawString(this.font, "LIVE", 54 + this.imageWidth / 2 - this.font.width(name) / 2, 9, 0x00FF00, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (fel == null) return; // тайл может отсутствовать в реплее Flashback

        if (fel.isOn) g.blit(TEXTURE, leftPos + 142, topPos + 41, 203, 0, 29, 17);

        int k = (int) fel.getPowerScaled(114);
        g.blit(TEXTURE, leftPos + 182, topPos + 27 + 113 - k, 203, 17 + 113 - k, 16, k);

        int color = fel.mode != EnumWavelengths.VISIBLE ? fel.mode.guiColor
                : Color.HSBtoRGB((fel.getLevel() != null ? fel.getLevel().getGameTime() : 0) / 50.0F, 0.5F, 1F) & 16777215;

        if (fel.isBeaming() && fel.distance > 0) {
            GuiLineHelper.drawLine(g, leftPos + 113, topPos + 31.5F, leftPos + 135, topPos + 31.5F, 5F, color);
            GuiLineHelper.drawLine(g, 0, topPos + 31.5F, leftPos + 4, topPos + 31.5F, 5F, color);
        }
    }
}
