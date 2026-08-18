package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.rbmk.RBMKRodBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.RBMKRodMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Port of {@code GUIRBMKRod} (1.7.10 Original). */
public class GUIRBMKRod extends GuiInfoScreen<RBMKRodMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/reactors/gui_rbmk_element.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_rbmk_element.png");
            //?}

    private final RBMKRodBlockEntity be;

    public GUIRBMKRod(RBMKRodMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        ItemStack rod = be.fuelSlot;
        if (!rod.isEmpty() && rod.getItem() instanceof RBMKRodItem) {
            // Depletion bar background + fill
            g.blit(TEXTURE, leftPos + 34, topPos + 21, 176, 0, 18, 67);
            double depletion = 1D - RBMKRodItem.getEnrichment(rod);
            int d = (int) (depletion * 67);
            if (d > 0) g.blit(TEXTURE, leftPos + 34, topPos + 21, 194, 0, 18, d);

            // Xenon bar (fills bottom-up)
            double xenon = RBMKRodItem.getPoisonLevel(rod);
            int x = (int) (xenon * 58);
            if (x > 0) g.blit(TEXTURE, leftPos + 126, topPos + 82 - x, 212, 58 - x, 14, x);
        }

        if (!be.coldEnoughForAutoloader()) drawInfoPanel(g, -16, 20, PanelType.LARGE_YELLOW_EXCLAMATION);
        if (!be.coldEnoughForManual())     drawInfoPanel(g, -16, 36, PanelType.LARGE_RED_EXCLAMATION);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);

        if (!be.coldEnoughForAutoloader())
            drawCustomInfoStat(g, mouseX, mouseY, -16, 20, 16, 16, leftPos - 8, topPos + 36,
                    Component.literal("Fuel skin temperature has exceeded 1,000°C,"),
                    Component.literal("autoloaders can no longer cycle fuel!"));
        if (!be.coldEnoughForManual())
            drawCustomInfoStat(g, mouseX, mouseY, -16, 36, 16, 16, leftPos - 8, topPos + 52,
                    Component.literal("Fuel skin temperature has exceeded 200°C,"),
                    Component.literal("fuel can no longer be removed by hand!"));

        this.renderTooltip(g, mouseX, mouseY);
    }
}
