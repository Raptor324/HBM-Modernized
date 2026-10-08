package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.CartCrateMenu;
import com.hbm_m.inventory.menu.CartDestroyerMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 1:1 {@code GUICrateSteel} (fuer die Kisten-Lore) und {@code GUICartDestroyer}. */
public final class GUICartScreens {

    private GUICartScreens() { }

    public static class Crate extends AbstractContainerScreen<CartCrateMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_crate_steel.png");

        public Crate(CartCrateMenu menu, Inventory inv, Component title) {
            super(menu, inv, title);
            this.imageWidth = 176;
            this.imageHeight = 222;
        }

        @Override
        public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float pt) {
            GuiCompat.renderBackground(this, g, mouseX, mouseY, pt);
            super.render(g, mouseX, mouseY, pt);
            this.renderTooltip(g, mouseX, mouseY);
        }

        @Override
        protected void renderBg(@NotNull GuiGraphics g, float pt, int mouseX, int mouseY) {
            g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }

        @Override
        protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
            g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
            g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
        }
    }

    public static class Destroyer extends AbstractContainerScreen<CartDestroyerMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/cart/gui_destroyer.png");

        public Destroyer(CartDestroyerMenu menu, Inventory inv, Component title) {
            super(menu, inv, title);
            this.imageWidth = 176;
            this.imageHeight = 166;
        }

        @Override
        public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float pt) {
            GuiCompat.renderBackground(this, g, mouseX, mouseY, pt);
            super.render(g, mouseX, mouseY, pt);
            this.renderTooltip(g, mouseX, mouseY);
        }

        @Override
        protected void renderBg(@NotNull GuiGraphics g, float pt, int mouseX, int mouseY) {
            g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

            // Original: Lauflicht-Animation des Vernichters (8 Bilder pro Sekunde)
            int time = (int) (System.currentTimeMillis() % 1000);
            int index = time / 128;

            if (index == 1 || index == 7) g.blit(TEXTURE, leftPos + 66, topPos + 35, 0, 166, 44, 16);
            if (index == 2 || index == 6) g.blit(TEXTURE, leftPos + 66, topPos + 35, 0, 182, 44, 16);
            if (index == 3 || index == 5) g.blit(TEXTURE, leftPos + 66, topPos + 35, 0, 198, 44, 16);
            if (index == 4) g.blit(TEXTURE, leftPos + 66, topPos + 35, 0, 214, 44, 16);
        }

        @Override
        protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
            g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
            g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 4, 4210752, false);
        }
    }
}
