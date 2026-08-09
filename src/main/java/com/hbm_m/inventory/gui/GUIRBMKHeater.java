package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.rbmk.RBMKHeaterBlockEntity;
import com.hbm_m.inventory.menu.RBMKHeaterMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1-Port von {@code GUIRBMKHeater} (1.7.10-Original, {@code ContainerRBMKHeater}):
 * xSize/ySize (176x186) sowie die Original-Koordinaten fuer Kuehlmittel- (feed) und
 * Dampf- (steam) Fuellstand wurden 1:1 uebernommen. Das Original rendert die Tanks ueber
 * die generische {@code FluidTank.renderTank}/{@code renderTankInfo}-Methode (fluidtyp-abhaengige
 * Textur, keine statische Balken-Grafik aus dem GUI-Sheet) - die modernisierte
 * {@link com.hbm_m.inventory.fluid.tank.FluidTank} bringt dafuer bereits 1:1 passende
 * {@code renderTank(GuiGraphics, ...)}/{@code renderTankInfo(...)}-Ueberladungen mit, die hier
 * direkt genutzt werden. Die zwei kleinen 10x10-Icons (Pfeile/Ventile) bei (72,72) und (130,72)
 * kommen weiterhin statisch aus dem GUI-Sheet.
 *
 * <p>Das Original besitzt einen einzelnen Item-Slot (Fluid-Identifier zum Setzen des
 * Kuehlmitteltyps); die modernisierte {@link RBMKHeaterBlockEntity} hat kein Item-Inventar
 * (nur Fluid-Tanks), daher entfaellt dieser Slot analog zu {@code GUIRBMKBoiler} - siehe
 * {@link RBMKHeaterMenu}.</p>
 */
public class GUIRBMKHeater extends GuiInfoScreen<RBMKHeaterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_rbmk_heater.png");

    // Kuehlmittel- (feed/input) Fuellstandsanzeige (Original: guiLeft+68, guiTop+24, 16x58 Hover-Bereich)
    private static final int FEED_X = 68, FEED_Y = 24, FEED_W = 16, FEED_H = 58;

    // Dampf- (steam/output) Fuellstandsanzeige (Original: guiLeft+126, guiTop+24, 16x58 Hover-Bereich)
    private static final int STEAM_X = 126, STEAM_Y = 24, STEAM_W = 16, STEAM_H = 58;

    private final RBMKHeaterBlockEntity be;

    public GUIRBMKHeater(RBMKHeaterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = leftPos, y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Original: rod.feed.renderTank(guiLeft+68, guiTop+82, zLevel, 14, 58);
        be.inputTank.renderTank(g, x + 68, y + 82, 14, 58);
        // Original: rod.steam.renderTank(guiLeft+126, guiTop+82, zLevel, 14, 58);
        be.outputTank.renderTank(g, x + 126, y + 82, 14, 58);

        // Original: drawTexturedModalRect(guiLeft+72, guiTop+72, 176, 0, 10, 10);
        g.blit(TEXTURE, x + 72, y + 72, 176, 0, 10, 10);
        // Original: drawTexturedModalRect(guiLeft+130, guiTop+72, 186, 0, 10, 10);
        g.blit(TEXTURE, x + 130, y + 72, 186, 0, 10, 10);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);

        be.inputTank.renderTankInfo(g, font, mx, my, leftPos + FEED_X, topPos + FEED_Y, FEED_W, FEED_H);
        be.outputTank.renderTankInfo(g, font, mx, my, leftPos + STEAM_X, topPos + STEAM_Y, STEAM_W, STEAM_H);

        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}
