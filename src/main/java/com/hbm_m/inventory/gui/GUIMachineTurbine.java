package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineTurbineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineTurbineMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class GUIMachineTurbine extends GuiInfoScreen<MachineTurbineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_turbine.png");

    private final MachineTurbineBlockEntity turbine;

    public GUIMachineTurbine(MachineTurbineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.turbine = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 168;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        Fluid inputFluid = turbine.getTanks()[0].getTankType();
        if (isSameFluid(inputFluid, ModFluids.STEAM.getSource())) {
            guiGraphics.blit(TEXTURE, this.leftPos + 99, this.topPos + 18, 183, 0, 14, 14);
        } else if (isSameFluid(inputFluid, ModFluids.HOTSTEAM.getSource())) {
            guiGraphics.blit(TEXTURE, this.leftPos + 99, this.topPos + 18, 183, 14, 14, 14);
        } else if (isSameFluid(inputFluid, ModFluids.SUPERHOTSTEAM.getSource())) {
            guiGraphics.blit(TEXTURE, this.leftPos + 99, this.topPos + 18, 183, 28, 14, 14);
        } else if (isSameFluid(inputFluid, ModFluids.ULTRAHOTSTEAM.getSource())) {
            guiGraphics.blit(TEXTURE, this.leftPos + 99, this.topPos + 18, 183, 42, 14, 14);
        }

        int power = turbine.getPowerScaled(34);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 123, this.topPos + 69 - power, 176, 34 - power, 7, power);
        }

        if (isSameFluid(turbine.getTanks()[1].getTankType(), ModFluids.NONE.getSource())) {
            drawInfoPanel(guiGraphics, -16, 68, PanelType.LARGE_RED_EXCLAMATION);
        }

        turbine.getTanks()[0].renderTank(guiGraphics, this.leftPos + 62, this.topPos + 69, 16, 52);
        turbine.getTanks()[1].renderTank(guiGraphics, this.leftPos + 134, this.topPos + 69, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        FluidTank[] tanks = turbine.getTanks();
        tanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 62, this.topPos + 17, 16, 52);
        tanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 134, this.topPos + 17, 16, 52);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                123, 35, 7, 34,
                turbine.getEnergyStored(), turbine.getMaxEnergyStored());

        if (isSameFluid(turbine.getTanks()[1].getTankType(), ModFluids.NONE.getSource())) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                    -16, 68, 16, 16,
                    mouseX, mouseY,
                    Component.literal("Error: Invalid fluid!"));
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private boolean isSameFluid(Fluid a, Fluid b) {
        return a == b || (a != null && b != null && a.isSame(b));
    }
}
