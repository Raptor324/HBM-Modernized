package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.menu.MachineElectrolyserMetalMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIElectrolyserMetal}: Saeure (36,18), zwei Schmelzsaeulen (58/96, 18) in Materialfarbe, Strom (186,18),
 * Fortschritt (7,71); der Knopf (8,82) wechselt zur Fluidseite.
 */
public class GUIElectrolyserMetal extends GuiInfoScreen<MachineElectrolyserMetalMenu> {

    public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_electrolyser_metal.png");
    private final MachineElectrolyserBlockEntity electrolyser;

    public GUIElectrolyserMetal(MachineElectrolyserMetalMenu menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);
        this.electrolyser = menu.getBlockEntity();

        this.imageWidth = 210;
        this.imageHeight = 204;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);

        if (electrolyser != null) {
            electrolyser.tanks[3].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 36, topPos + 18, 16, 52);

            if (electrolyser.leftStack != null) {
                this.drawCustomInfoStat(g, mouseX, mouseY, 58, 18, 34, 42, mouseX, mouseY, electrolyser.leftStack.material.getLocalizedName().copy()
                        .append(": " + Mats.formatAmount(electrolyser.leftStack.amount, Screen.hasShiftDown())).withStyle(ChatFormatting.YELLOW));
            } else {
                this.drawCustomInfoStat(g, mouseX, mouseY, 58, 18, 34, 42, mouseX, mouseY, Component.literal("Empty").withStyle(ChatFormatting.RED));
            }

            if (electrolyser.rightStack != null) {
                this.drawCustomInfoStat(g, mouseX, mouseY, 96, 18, 34, 42, mouseX, mouseY, electrolyser.rightStack.material.getLocalizedName().copy()
                        .append(": " + Mats.formatAmount(electrolyser.rightStack.amount, Screen.hasShiftDown())).withStyle(ChatFormatting.YELLOW));
            } else {
                this.drawCustomInfoStat(g, mouseX, mouseY, 96, 18, 34, 42, mouseX, mouseY, Component.literal("Empty").withStyle(ChatFormatting.RED));
            }

            this.drawElectricityInfo(g, mouseX, mouseY, 186, 18, 16, 89, electrolyser.getPower(), MachineElectrolyserBlockEntity.maxPower);
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        if (electrolyser != null && leftPos + 8 <= x && leftPos + 8 + 54 > x && topPos + 82 < y && topPos + 82 + 12 >= y) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CompoundTag data = new CompoundTag();
            data.putBoolean("sgf", true);
            NBTControlPacket.sendToServer(electrolyser.getBlockPos(), data);
            return true;
        }
        return super.mouseClicked(x, y, i);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, (this.imageWidth / 2 - this.font.width(this.title) / 2) - 16, 7, 0xffffff, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (electrolyser == null) return;

        if (electrolyser.leftStack != null) {
            int p = electrolyser.leftStack.amount * 42 / electrolyser.maxMaterial;
            int hex = electrolyser.leftStack.material.moltenColor;
            RenderSystem.setShaderColor((hex >> 16 & 255) / 255F, (hex >> 8 & 255) / 255F, (hex & 255) / 255F, 1F);
            g.blit(texture, leftPos + 58, topPos + 60 - p, 210, 131 - p, 34, p, 256, 256);
        }

        if (electrolyser.rightStack != null) {
            int p = electrolyser.rightStack.amount * 42 / electrolyser.maxMaterial;
            int hex = electrolyser.rightStack.material.moltenColor;
            RenderSystem.setShaderColor((hex >> 16 & 255) / 255F, (hex >> 8 & 255) / 255F, (hex & 255) / 255F, 1F);
            g.blit(texture, leftPos + 96, topPos + 60 - p, 210, 131 - p, 34, p, 256, 256);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int p = (int) (electrolyser.getPower() * 89 / MachineElectrolyserBlockEntity.maxPower);
        g.blit(texture, leftPos + 186, topPos + 107 - p, 210, 89 - p, 16, p, 256, 256);

        if (electrolyser.getPower() >= electrolyser.usageOre)
            g.blit(texture, leftPos + 190, topPos + 4, 226, 25, 9, 12, 256, 256);

        int o = electrolyser.progressOre * 26 / Math.max(electrolyser.processOreTime, 1);
        g.blit(texture, leftPos + 7, topPos + 71 - o, 226, 25 - o, 22, o, 256, 256);

        electrolyser.tanks[3].renderTank(g, leftPos + 36, topPos + 18, 16, 52);
    }
}
