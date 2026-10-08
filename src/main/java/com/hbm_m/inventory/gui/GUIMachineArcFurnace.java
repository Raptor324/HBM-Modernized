package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.machines.MachineArcFurnaceBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.menu.MachineArcFurnaceMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.platform.GlStateManager;
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
 * 1:1 {@code GUIMachineArcFurnaceLarge}: Modusschalter (151,17), Betriebslampe (7,17), Strom (8,36) und Fortschritt
 * (17,36) als Saeulen, Schmelzsaeule (152,36) mit gestapelten Materialfarben.
 */
public class GUIMachineArcFurnace extends GuiInfoScreen<MachineArcFurnaceMenu> {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_arc_furnace.png");
    private final MachineArcFurnaceBlockEntity arc;

    public GUIMachineArcFurnace(MachineArcFurnaceMenu menu, Inventory invPlayer, Component title) {
        super(menu, invPlayer, title);
        this.arc = menu.getBlockEntity();

        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        if (arc != null) {
            drawStackInfo(g, arc.liquids, x, y, 152, 36);
            this.drawElectricityInfo(g, x, y, 8, 36, 7, 70, arc.getPower(), MachineArcFurnaceBlockEntity.maxPower);
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    public boolean mouseClicked(double x, double y, int k) {
        if (arc != null && isPointInRect(151, 17, 18, 18, (int) x, (int) y)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CompoundTag data = new CompoundTag();
            data.putBoolean("liquid", true);
            NBTControlPacket.sendToServer(arc.getBlockPos(), data);
            return true;
        }
        return super.mouseClicked(x, y, k);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0xffffff, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (arc == null) return;

        if (arc.liquidMode) g.blit(texture, leftPos + 151, topPos + 17, 190, 18, 18, 18);
        if (arc.isProgressing) g.blit(texture, leftPos + 7, topPos + 17, 190, 0, 18, 18);

        int p = (int) (arc.getPower() * 70 / MachineArcFurnaceBlockEntity.maxPower);
        g.blit(texture, leftPos + 8, topPos + 106 - p, 176, 70 - p, 7, p);

        int o = (int) (arc.progress * 70);
        g.blit(texture, leftPos + 17, topPos + 106 - o, 183, 70 - o, 7, o);

        drawStack(g, arc.liquids, MachineArcFurnaceBlockEntity.maxLiquid, 152, 106);
    }

    protected void drawStackInfo(GuiGraphics g, List<MaterialStack> stack, int mouseX, int mouseY, int x, int y) {
        List<Component> list = new ArrayList<>();
        if (stack.isEmpty()) list.add(Component.literal("Empty").withStyle(ChatFormatting.RED));
        for (MaterialStack sta : stack) list.add(sta.material.getLocalizedName().copy().append(": " + Mats.formatAmount(sta.amount, Screen.hasShiftDown())).withStyle(ChatFormatting.YELLOW));
        this.drawCustomInfoStat(g, mouseX, mouseY, x, y, 16, 70, mouseX, mouseY, list.toArray(new Component[0]));
    }

    protected void drawStack(GuiGraphics g, List<MaterialStack> stack, int capacity, int x, int y) {

        if (stack.isEmpty()) return;

        int lastHeight = 0;
        int lastQuant = 0;

        for (MaterialStack sta : stack) {

            int targetHeight = (lastQuant + sta.amount) * 70 / capacity;

            if (lastHeight == targetHeight) continue; //skip draw calls that would be 0 pixels high

            int hex = sta.material.moltenColor;
            RenderSystem.setShaderColor((hex >> 16 & 255) / 255F, (hex >> 8 & 255) / 255F, (hex & 255) / 255F, 1F);
            g.blit(texture, leftPos + x, topPos + y - targetHeight, 208, 70 - targetHeight, 16, targetHeight - lastHeight);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1F, 1F, 1F, 0.3F);
            g.blit(texture, leftPos + x, topPos + y - targetHeight, 208, 70 - targetHeight, 16, targetHeight - lastHeight);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();

            lastQuant += sta.amount;
            lastHeight = targetHeight;
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }
}
