package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineReactorResearchMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Vereinfachtes GUI (siehe Klassenkommentar in {@link MachineReactorResearchBlockEntity}) - das
 * Original-Hintergrundbild wird uebernommen, aber die 7-Segment-Zahlenanzeigen, das dekorative
 * 3x3/2x2-Kern-Diagramm sowie das Textfeld zur manuellen Regelstab-Eingabe entfallen zugunsten
 * einfacher Text-Anzeigen (Redstone-Sperre steuert die Reaktorleistung stattdessen).
 */
public class GUIMachineReactorResearch extends GuiInfoScreen<MachineReactorResearchMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_research_reactor.png");

    private final MachineReactorResearchBlockEntity reactor;

    public GUIMachineReactorResearch(MachineReactorResearchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.reactor = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, 121 - this.font.width(name) / 2, 6, 0xE5E5E5, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
        guiGraphics.drawString(this.font, "Flux", 6, 13, 0xE5E5E5, false);
        guiGraphics.drawString(this.font, "Heat", 6, 51, 0xE5E5E5, false);
        guiGraphics.drawString(this.font, "Control", 6, 89, 0xE5E5E5, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                6, 22, 33, 16, mouseX, mouseY,
                Component.literal(String.valueOf(reactor.getTotalFlux())));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                6, 60, 33, 16, mouseX, mouseY,
                Component.literal(Math.round(reactor.getHeat() * 2.0E-5D * 980.0D + 20.0D) + " C"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                6, 98, 33, 16, mouseX, mouseY,
                Component.literal(Math.round(reactor.getRodLevel() * 100) + " %"));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
