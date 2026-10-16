package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineCoreEmitterBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineCoreEmitterMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Port von {@code GUICoreEmitter} (1.7.10). Original bot einen Watt-Regler (Textfeld 1-100%)
 * und einen An/Aus-Knopf; die modernisierte {@link MachineCoreEmitterBlockEntity} hat dafuer
 * keine Datenfelder (siehe Klassenkommentar dort - der Emitter feuert immer mit voller Rate,
 * solange Energie und Kuehlmittel vorhanden sind). Diese GUI ist daher rein informativ:
 * Energiebalken, Kuehlmittel-Tank (Cryogel) und ein Aktiv-Indikator fuer den Laserstrahl.
 */
public class GUIMachineCoreEmitter extends GuiInfoScreen<MachineCoreEmitterMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/dfc/gui_emitter.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/dfc/gui_emitter.png");
            //?}

    private final MachineCoreEmitterBlockEntity emitter;

    public GUIMachineCoreEmitter(MachineCoreEmitterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.emitter = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Energiebalken (1:1 Original-Widget-Region: x=176, bodenbuendig, 16x52).
        if (emitter != null) { // тайл может отсутствовать в реплее Flashback
            int energy = (int) (emitter.getEnergyStored() * 52L / Math.max(emitter.getMaxEnergyStored(), 1L));
            if (energy > 52) {
                energy = 52;
            }
            if (energy > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 69 - energy, 176, 52 - energy, 16, energy);
            }

            if (emitter.getBeamLength() > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 133, this.topPos + 52, 192, 0, 18, 18);
            }

            emitter.getCoolantTank().renderTank(guiGraphics, this.leftPos + 8, this.topPos + 17, 16, 52);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);

        if (emitter != null) {
            guiGraphics.drawString(this.font, "Beam: " + emitter.getBeamLength() + "m", 50, 30, 0xFF7F7F, false);
        }

        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (emitter != null) { // тайл может отсутствовать в реплее Flashback
            emitter.getCoolantTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 8, this.topPos + 17, 16, 52);

            drawElectricityInfo(guiGraphics, mouseX, mouseY,
                    26, 17, 16, 52,
                    emitter.getEnergyStored(), emitter.getMaxEnergyStored());
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
