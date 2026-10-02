package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.HeldItemMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** {@code GUILeadBox}, {@code GUIPlasticBag}, {@code GUICasingBag} 1:1 (Texturen, Groessen, Beschriftungen). */
public class GUIHeldItem extends AbstractContainerScreen<HeldItemMenu> {

    private final ResourceLocation texture;

    public GUIHeldItem(HeldItemMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = menu.layout.ySize;
        this.texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, switch (menu.layout) {
            case LEAD_BOX -> "textures/gui/gui_containment.png";
            case PLASTIC_BAG -> "textures/gui/storage/gui_plastic_bag.png";
            case CASING_BAG -> "textures/gui/gui_casing_bag.png";
            case TOOLBOX -> "textures/gui/gui_toolbox.png";
        });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        Component inv = Component.translatable("container.inventory");
        switch (menu.layout) {
            case LEAD_BOX -> {
                Component name = menu.box.target.hasCustomHoverName() ? menu.box.target.getHoverName() : Component.translatable("container.leadBox");
                g.drawString(font, name, this.imageWidth / 2 - font.width(name) / 2, 6, 4210752, false);
                g.drawString(font, inv, 8, this.imageHeight - 96 + 2, 4210752, false);
            }
            case PLASTIC_BAG -> g.drawString(font, inv, 8, this.imageHeight - 96 + 2, 4210752, false);
            case TOOLBOX -> {
                Component name = menu.box.target.hasCustomHoverName() ? menu.box.target.getHoverName() : Component.translatable("container.toolBox");
                g.drawString(font, name, this.imageWidth / 2 - font.width(name) / 2, 37, 4210752, false);
                g.drawString(font, inv, 8, this.imageHeight - 96 + 2, 4210752, false);
            }
            case CASING_BAG -> {
                Component name = menu.box.target.hasCustomHoverName() ? menu.box.target.getHoverName() : Component.translatable("container.casingBag");
                g.drawString(font, name, this.imageWidth / 2 - font.width(name) / 2, 6, 0xffffff, false);
                g.drawString(font, inv, 8, this.imageHeight - 98, 4210752, false);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
