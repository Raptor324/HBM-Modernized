package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.MachinePumpjackBlockEntity;
import com.hbm_m.inventory.menu.MachinePumpjackMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Upstream renders derrick and pumpjack with the same GUIMachineOilWell, so this mirrors GUIMachineDerrick. */
public class GUIMachinePumpjack extends GuiInfoScreen<MachinePumpjackMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_well.png");

    private final MachinePumpjackBlockEntity pumpjack;

    public GUIMachinePumpjack(MachinePumpjackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.pumpjack = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (pumpjack != null) { // тайл может отсутствовать в реплее Flashback
            int power = (int) (pumpjack.getEnergyStored() * 34L / Math.max(pumpjack.getMaxEnergyStored(), 1L));
            if (power > 34) power = 34;
            if (power > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 51 - power, 176, 34 - power, 16, power);
            }

            int k = pumpjack.indicator;
            if (k != 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 35, this.topPos + 17, 176 + (k - 1) * 16, 52, 16, 16);
            }

            if (pumpjack.tanks.length < 3) {
                guiGraphics.blit(TEXTURE, this.leftPos + 34, this.topPos + 36, 192, 0, 18, 34);
            }

            pumpjack.tanks[0].renderTank(guiGraphics, this.leftPos + 62, this.topPos + 17, 16, 52);
            pumpjack.tanks[1].renderTank(guiGraphics, this.leftPos + 107, this.topPos + 17, 16, 52);

            if (pumpjack.tanks.length > 2) {
                pumpjack.tanks[2].renderTank(guiGraphics, this.leftPos + 40, this.topPos + 37, 6, 32);
            }
        }

        // upgrade info panel (top-right corner icon)
        drawInfoPanel(guiGraphics, 156, 3, PanelType.SMALL_BLUE_INFO);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (pumpjack != null) { // тайл может отсутствовать в реплее Flashback
            drawElectricityInfo(guiGraphics, mouseX, mouseY,
                    8, 17, 16, 34,
                    pumpjack.getEnergyStored(), pumpjack.getMaxEnergyStored());

            pumpjack.tanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 62, this.topPos + 17, 16, 52);
            pumpjack.tanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 107, this.topPos + 17, 16, 52);

            if (pumpjack.tanks.length >= 3) {
                pumpjack.tanks[2].renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                        this.leftPos + 40, this.topPos + 37, 6, 32);
            }
        }

        // upgrade tooltip
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 156, this.topPos + 3, 8, 8,
                this.leftPos + 156, this.topPos + 3,
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.power"),
                Component.translatable("desc.gui.upgrade.afterburner"));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
