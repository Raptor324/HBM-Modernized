package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.BombMultiBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.BombMultiMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Экран многоцелевой бомбы (плоский фон, 4 заряда + 2 модификатора).
 */
public class GUIBombMulti extends GuiInfoScreen<BombMultiMenu> {

    private final BombMultiBlockEntity be;

    public GUIBombMulti(BombMultiMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.be = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (be != null && be.isReady()) { // тайл может отсутствовать в реплее Flashback
            guiGraphics.drawCenteredString(this.font,
                    Component.translatable("gui.hbm_m.bomb_multi.ready").withStyle(net.minecraft.ChatFormatting.DARK_RED),
                    this.leftPos + imageWidth / 2, this.topPos + 76, 0);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + imageWidth, this.topPos + imageHeight, 0xFFC6C6C6);
        guiGraphics.renderOutline(this.leftPos, this.topPos, imageWidth, imageHeight, 0xFF000000);
        int[][] pos = {{44, 18}, {116, 18}, {44, 54}, {116, 54}, {71, 27}, {89, 45}};
        for (int[] p : pos) {
            int sx = this.leftPos + p[0] - 1;
            int sy = this.topPos + p[1] - 1;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
            guiGraphics.renderOutline(sx, sy, 18, 18, 0xFF373737);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
