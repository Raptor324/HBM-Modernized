package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.NukeN2BlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.NukeN2Menu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Экран мины N2 (схема размещения зарядов).
 */
public class GUINukeN2 extends GuiInfoScreen<NukeN2Menu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/n2_schematic.png");

    private final NukeN2BlockEntity be;

    public GUINukeN2(NukeN2Menu menu, Inventory playerInventory, Component title) {
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

        // Original: Fuellsaeule je eingelegter N2-Ladung (6 Pixel pro Ladung), kein Info-Panel
        if (be != null) { // тайл может отсутствовать в реплее Flashback
            int count = 0;
            for (int i = 0; i < 12; i++) {
                if (be.slots.get(i).is(ModItems.N2_CHARGE.get())) count++;
            }
            if (count > 0) {
                guiGraphics.blit(TEXTURE, leftPos + 35, topPos + 120 - 6 * count, 176, 0, 34, 6 * count);
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
