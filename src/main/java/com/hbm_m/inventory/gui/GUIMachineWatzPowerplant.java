package com.hbm_m.inventory.gui;

import java.util.Locale;

import com.hbm_m.blockentity.machines.MachineWatzPowerplantBlockEntity;
import com.hbm_m.inventory.menu.MachineWatzPowerplantMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIWatz}. */
public class GUIMachineWatzPowerplant extends GuiInfoScreen<MachineWatzPowerplantMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_watz.png");

    private final MachineWatzPowerplantBlockEntity watz;

    public GUIMachineWatzPowerplant(MachineWatzPowerplantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.watz = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 229;
        this.inventoryLabelY = this.imageHeight - 93;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        // Tile kann in einer Flashback-Wiedergabe fehlen
        if (watz != null) {
            drawCustomInfoStat(g, x, y, 13, 100, 18, 18, x, y, Component.literal(String.format(Locale.US, "%,d", watz.heat) + " TU"));
            drawCustomInfoStat(g, x, y, 143, 71, 16, 16, x, y,
                    Component.literal(watz.isLocked ? "Unlock pellet IO configuration" : "Lock pellet IO configuration"));

            watz.tanks[0].renderTankInfo(g, font, x, y, leftPos + 142, topPos + 23, 6, 45);
            watz.tanks[1].renderTankInfo(g, font, x, y, leftPos + 148, topPos + 23, 6, 45);
            watz.tanks[2].renderTankInfo(g, font, x, y, leftPos + 154, topPos + 23, 6, 45);
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 93, 4210752, false);
        if (watz == null) return;

        float scale = 1.25F;
        String flux = String.format(Locale.US, "%,.1f", watz.fluxDisplay);
        g.pose().pushPose();
        g.pose().scale(1 / scale, 1 / scale, 1);
        g.drawString(this.font, flux, (int) (161 * scale - this.font.width(flux)), (int) (107 * scale), 0x00ff00, false);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (watz != null && leftPos + 142 <= x && leftPos + 142 + 18 > x && topPos + 70 < y && topPos + 70 + 18 >= y) {
            CompoundTag control = new CompoundTag();
            control.putBoolean("lock", true);
            NBTControlPacket.sendToServer(watz.getBlockPos(), control);
            playClickSound();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        float col = watz == null ? 1F : Mth.clamp(1 - (float) Math.log(watz.heat / 100_000D + 1) * 0.4F, 0F, 1F);
        RenderSystem.setShaderColor(1.0F, col, col, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, 131, 122);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        g.blit(TEXTURE, leftPos + 131, topPos, 131, 0, 36, 122);
        g.blit(TEXTURE, leftPos, topPos + 130, 0, 130, imageWidth, 99);
        g.blit(TEXTURE, leftPos + 126, topPos + 31, 176, 31, 9, 60);
        g.blit(TEXTURE, leftPos + 105, topPos + 96, 185, 26, 30, 26);
        g.blit(TEXTURE, leftPos + 9, topPos + 96, 184, 0, 26, 26);

        if (watz == null) return;

        if (watz.isOn) g.blit(TEXTURE, leftPos + 147, topPos + 8, 176, 0, 8, 8);
        if (watz.isLocked) g.blit(TEXTURE, leftPos + 142, topPos + 70, 210, 0, 18, 18);

        GuiGaugeNeedle.draw(g, leftPos + 22, topPos + 109, 1D - col, 5, 2, 1, 0x7F0000);

        // Original: renderTank(x, yUnten, z, 4, 43) - hier mit oberer Kante
        watz.tanks[0].renderTank(g, leftPos + 143, topPos + 69 - 43, 4, 43);
        watz.tanks[1].renderTank(g, leftPos + 149, topPos + 69 - 43, 4, 43);
        watz.tanks[2].renderTank(g, leftPos + 155, topPos + 69 - 43, 4, 43);
    }
}
