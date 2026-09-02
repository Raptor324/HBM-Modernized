package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.bomb.LargeNukeType;
import com.hbm_m.blockentity.bomb.LargeNukeBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.LargeNukeMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Общий экран больших ядерных бомб; фон — схематичная текстура конкретного типа.
 */
public class GUINukeLarge extends GuiInfoScreen<LargeNukeMenu> {

    private final LargeNukeBlockEntity be;
    private final LargeNukeType type;

    public GUINukeLarge(LargeNukeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.be = menu.be;
        this.type = menu.type;
        this.imageWidth = type.guiWidth();
        this.imageHeight = type.guiHeight();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(type.schematic(), this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        if (be != null && be.isReady()) { // тайл может отсутствовать в реплее Flashback
            // Индикатор готовности в правом верхнем углу (как у Толстяка).
            guiGraphics.blit(type.schematic(), this.leftPos + imageWidth - 42, this.topPos + 6, 176, 48, 16, 16);
        }

        this.drawInfoPanel(guiGraphics, -16, 16, PanelType.LARGE_BLUE_INFO);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, type.inventoryX(), type.inventoryY() - 11, 4210752, false);
    }
}
