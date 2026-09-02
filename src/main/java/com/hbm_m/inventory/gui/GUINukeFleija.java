package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.NukeFleijaBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.NukeFleijaMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Экран бомбы FLEIJA (схема размещения компонентов).
 */
public class GUINukeFleija extends GuiInfoScreen<NukeFleijaMenu> {

    private static final ResourceLocation TEXTURE =
            //? if < 1.21.1 {
            new ResourceLocation(RefStrings.MODID, "textures/gui/weapon/fleija_schematic.png");
            //?} else {
            /*ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/fleija_schematic.png");
             *///?}

    private final NukeFleijaBlockEntity be;

    public GUINukeFleija(NukeFleijaMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.be = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        // тайл может отсутствовать в реплее Flashback
        if (be != null && be.isReady()) {
            guiGraphics.blit(TEXTURE, this.leftPos + imageWidth - 42, this.topPos + 6, 176, 48, 16, 16);
        }

        this.drawInfoPanel(guiGraphics, -16, 16, PanelType.LARGE_BLUE_INFO);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
