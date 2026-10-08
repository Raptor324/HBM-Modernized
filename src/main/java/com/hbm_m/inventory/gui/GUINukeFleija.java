package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.NukeFleijaBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.NukeFleijaMenu;
import com.hbm_m.item.ModItems;
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
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/fleija_schematic.png");

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

        // Original: Teile-Overlays je Slot, kein Info-Panel, keine Bereitschaftsanzeige
        if (be != null) { // тайл может отсутствовать в реплее Flashback
            if (has(0, ModItems.FLEIJA_IGNITER.get())) guiGraphics.blit(TEXTURE, leftPos + 7, topPos + 88, 176, 0, 30, 20);
            if (has(1, ModItems.FLEIJA_IGNITER.get())) guiGraphics.blit(TEXTURE, leftPos + 139, topPos + 88, 206, 0, 30, 20);
            if (has(2, ModItems.FLEIJA_PROPELLANT.get())) guiGraphics.blit(TEXTURE, leftPos + 57, topPos + 77, 176, 62, 18, 14);
            if (has(3, ModItems.FLEIJA_PROPELLANT.get())) guiGraphics.blit(TEXTURE, leftPos + 57, topPos + 91, 176, 76, 18, 14);
            if (has(4, ModItems.FLEIJA_PROPELLANT.get())) guiGraphics.blit(TEXTURE, leftPos + 57, topPos + 105, 176, 90, 18, 14);
            if (has(5, ModItems.FLEIJA_CORE.get())) guiGraphics.blit(TEXTURE, leftPos + 85, topPos + 77, 176, 20, 18, 15);
            if (has(6, ModItems.FLEIJA_CORE.get())) guiGraphics.blit(TEXTURE, leftPos + 103, topPos + 77, 194, 20, 18, 15);
            if (has(7, ModItems.FLEIJA_CORE.get())) guiGraphics.blit(TEXTURE, leftPos + 85, topPos + 92, 176, 35, 18, 12);
            if (has(8, ModItems.FLEIJA_CORE.get())) guiGraphics.blit(TEXTURE, leftPos + 103, topPos + 92, 194, 35, 18, 12);
            if (has(9, ModItems.FLEIJA_CORE.get())) guiGraphics.blit(TEXTURE, leftPos + 85, topPos + 104, 176, 47, 18, 15);
            if (has(10, ModItems.FLEIJA_CORE.get())) guiGraphics.blit(TEXTURE, leftPos + 103, topPos + 104, 194, 47, 18, 15);
        }
    }

    private boolean has(int slot, net.minecraft.world.item.Item item) {
        return be.slots.get(slot).is(item);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
