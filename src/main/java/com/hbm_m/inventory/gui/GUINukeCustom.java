package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.NukeCustomBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.explosion.CustomNukeExplosion;
import com.hbm_m.inventory.menu.NukeCustomMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Экран кастомной бомбы: сетка 9x3 + расчётный состав заряда.
 */
public class GUINukeCustom extends GuiInfoScreen<NukeCustomMenu> {

    private final NukeCustomBlockEntity be;

    public GUINukeCustom(NukeCustomMenu menu, Inventory playerInventory, Component title) {
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

        // Сводка по выходам под сеткой слотов.
        CustomNukeExplosion.Yields y = CustomNukeExplosion.computeYields(be.slots);
        int lineY = 76;
        guiGraphics.drawString(this.font, summary(y), this.leftPos + 8, this.topPos + lineY, 0xFF555555, false);
    }

    private Component summary(CustomNukeExplosion.Yields y) {
        if (y.euph() > 0) return Component.translatable("gui.hbm_m.nuke_custom.euph").withStyle(ChatFormatting.LIGHT_PURPLE);
        if (y.schrab() > 0) return Component.translatable("gui.hbm_m.nuke_custom.schrab", (int) Math.min(y.schrab(), 250)).withStyle(ChatFormatting.AQUA);
        if (y.hydro() > 0) return Component.translatable("gui.hbm_m.nuke_custom.hydro", (int) Math.min(y.hydro(), 350)).withStyle(ChatFormatting.BLUE);
        if (y.nuke() > 0) return Component.translatable("gui.hbm_m.nuke_custom.nuke", (int) Math.min(y.nuke(), 200)).withStyle(ChatFormatting.GREEN);
        if (y.tnt() >= 75) return Component.translatable("gui.hbm_m.nuke_custom.tnt_big", (int) Math.min(y.tnt(), 150)).withStyle(ChatFormatting.YELLOW);
        if (y.tnt() > 0) return Component.translatable("gui.hbm_m.nuke_custom.tnt_small").withStyle(ChatFormatting.GRAY);
        return Component.translatable("gui.hbm_m.nuke_custom.empty").withStyle(ChatFormatting.DARK_GRAY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + imageWidth, this.topPos + imageHeight, 0xFFC6C6C6);
        guiGraphics.renderOutline(this.leftPos, this.topPos, imageWidth, imageHeight, 0xFF000000);
        for (int slot = 0; slot < NukeCustomBlockEntity.SLOTS; slot++) {
            int sx = this.leftPos + 8 + (slot % 9) * 18 - 1;
            int sy = this.topPos + 18 + (slot / 9) * 18 - 1;
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
