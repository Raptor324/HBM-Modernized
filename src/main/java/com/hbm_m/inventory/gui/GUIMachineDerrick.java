package com.hbm_m.inventory.gui;

import com.hbm_m.block.entity.machines.MachineDerrickBlockEntity;
import com.hbm_m.inventory.menu.MachineDerrickMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineDerrick extends GuiInfoScreen<MachineDerrickMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_well.png");

    private final MachineDerrickBlockEntity derrick;

    public GUIMachineDerrick(MachineDerrickMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.derrick = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // energy bar (fills upward, 34px height, bar sits at x=8, bottom at y=51)
        int power = (int) (derrick.getEnergyStored() * 34L / Math.max(derrick.getMaxEnergyStored(), 1L));
        if (power > 34) power = 34;
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 51 - power, 176, 34 - power, 16, power);
        }


        // indicator icon (0 = none, 1 = active/ok, 2 = blocked)
        int k = derrick.indicator;
        if (k != 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 35, this.topPos + 17, 176 + (k - 1) * 16, 52, 16, 16);
        }

        // cover the small tank area if tanks.length < 3
        if (derrick.tanks.length < 3) {
            guiGraphics.blit(TEXTURE, this.leftPos + 34, this.topPos + 36, 192, 0, 18, 34);
        }

        // fluid tanks (y = top of tank area)
        derrick.tanks[0].renderTank(guiGraphics, this.leftPos + 62, this.topPos + 17, 16, 52);
        derrick.tanks[1].renderTank(guiGraphics, this.leftPos + 107, this.topPos + 17, 16, 52);

        if (derrick.tanks.length > 2) {
            derrick.tanks[2].renderTank(guiGraphics, this.leftPos + 40, this.topPos + 37, 6, 32);
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
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // energy tooltip (bar spans x=8..24, y=17..51)
        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                8, 17, 16, 34,
                derrick.getEnergyStored(), derrick.getMaxEnergyStored());


        // tank tooltips
        derrick.tanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 62, this.topPos + 17, 16, 52);
        derrick.tanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + 107, this.topPos + 17, 16, 52);

        if (derrick.tanks.length >= 3) {
            derrick.tanks[2].renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                    this.leftPos + 40, this.topPos + 37, 6, 32);
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
