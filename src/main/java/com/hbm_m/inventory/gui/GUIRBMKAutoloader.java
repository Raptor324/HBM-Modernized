package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm_m.inventory.menu.RBMKAutoloaderMenu;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Port of {@code GUIRBMKAutoloader} (1.7.10 Original). */
@OnlyIn(Dist.CLIENT)
public class GUIRBMKAutoloader extends GuiInfoScreen<RBMKAutoloaderMenu> {

    private static final ResourceLocation TEXTURE =
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_autoloader.png");

    public GUIRBMKAutoloader(RBMKAutoloaderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 182;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    // Original mouseClicked click regions (GUIRBMKAutoloader L40-52): the "-" button at
    // (74..86, 36..48), the "+" button at (90..102, 36..48).
    private static final int BTN_MINUS_X = 74, BTN_PLUS_X = 90, BTN_Y = 36, BTN_SIZE = 12;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // Item icons are rendered automatically by AbstractContainerScreen using the
        // Menu's real slot positions (17 + 18*col, 18 + 18*row) - no manual icon drawing needed here.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        RBMKAutoloaderBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        // Original foreground layer: the cycle threshold as a centered green "NN%" string at y=23.
        String percent = be.cycle + "%";
        guiGraphics.drawString(font, percent, this.imageWidth / 2 - font.width(percent) / 2, 23, 0x00FF00, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        RBMKAutoloaderBlockEntity be = menu.getBlockEntity();
        // The screen's block entity is the client-side copy; the change has to go to the server,
        // exactly like the original's NBTControlPacket path.
        if (be != null && button == 0) {
            if (isPointInRect(BTN_MINUS_X, BTN_Y, BTN_SIZE, BTN_SIZE, (int) mouseX, (int) mouseY)) {
                com.hbm_m.network.RBMKAutoloaderPacket.sendCycleMinus(be.getBlockPos());
                playClickSound();
                return true;
            }
            if (isPointInRect(BTN_PLUS_X, BTN_Y, BTN_SIZE, BTN_SIZE, (int) mouseX, (int) mouseY)) {
                com.hbm_m.network.RBMKAutoloaderPacket.sendCyclePlus(be.getBlockPos());
                playClickSound();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
