package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.BombMultiBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.BombMultiMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUIBombMulti}: Anzeige des Modultyps (beide gleich) bzw. Mischsymbol bei verschiedenen Modulen. */
public class GUIBombMulti extends GuiInfoScreen<BombMultiMenu> {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/bomb_generic.png");
    private final BombMultiBlockEntity testNuke;

    public GUIBombMulti(BombMultiMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.testNuke = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (testNuke == null) return;

        int t2 = testNuke.return2type();
        int t5 = testNuke.return5type();

        if (t2 == t5 && t2 >= 1 && t2 <= 6)
            g.blit(texture, leftPos + 124, topPos + 34, 176, (t2 - 1) * 18, 18, 18);

        if (t2 != t5)
            g.blit(texture, leftPos + 124, topPos + 34, 176, 7 * 18, 18, 18);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
