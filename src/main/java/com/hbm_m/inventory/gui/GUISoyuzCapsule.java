package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.SoyuzCapsuleMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUISoyuzCapsule}: 176x186, Titel zentriert bei x=115 in Gruen (0x7daf71). */
public class GUISoyuzCapsule extends AbstractContainerScreen<SoyuzCapsuleMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_soyuz_capsule.png");

    public GUISoyuzCapsule(SoyuzCapsuleMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float pt) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, pt);
        super.render(g, mouseX, mouseY, pt);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, 115 - this.font.width(this.title) / 2, 6, 0x7daf71, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float pt, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
