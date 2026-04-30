package com.hbm_m.inventory.gui;

import com.hbm_m.block.entity.machines.MachineSubstationBlockEntity;
import com.hbm_m.inventory.menu.MachineSubstationMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineSubstation extends GuiInfoScreen<MachineSubstationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_turbinegas.png");

    private final MachineSubstationBlockEntity substation;

    public GUIMachineSubstation(MachineSubstationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.substation = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int power = (int) (substation.getEnergyStored() * 52L / Math.max(substation.getMaxEnergyStored(), 1L));
        if (power > 52) {
            power = 52;
        }
        if (power > 0) {
            guiGraphics.fill(this.leftPos + 152, this.topPos + 70 - power, this.leftPos + 158, this.topPos + 70, 0xFFFFCC33);
        }

        int progress = substation.getProgressScaled(33);
        if (progress > 0) {
            guiGraphics.fill(this.leftPos + 72, this.topPos + 37, this.leftPos + 72 + progress, this.topPos + 51, 0xFFA9A9A9);
        }

        if (substation.isActive()) {
            drawInfoPanel(guiGraphics, 78, 58, PanelType.SMALL_GREEN_INFO);
        } else {
            drawInfoPanel(guiGraphics, 78, 58, PanelType.SMALL_RED_EXCLAMATION);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                152, 18, 6, 52,
                substation.getEnergyStored(), substation.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                this.leftPos + 78, this.topPos + 58, 8, 8,
                this.leftPos + 78, this.topPos + 58,
                Component.literal("Load:"),
                Component.literal("   " + substation.getProgress() + " / " + substation.getMaxProgress()));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
