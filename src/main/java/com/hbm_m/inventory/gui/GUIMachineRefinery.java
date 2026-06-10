package com.hbm_m.inventory.gui;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.block.entity.machines.MachineRefineryBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineRefineryMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

public class GUIMachineRefinery extends GuiInfoScreen<MachineRefineryMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_refinery.png");

    private static final int TEX_WIDTH = 350;
    private static final int TEX_HEIGHT = 256;

    private final MachineRefineryBlockEntity refinery;

    public GUIMachineRefinery(MachineRefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.refinery = menu.getBlockEntity();
        this.imageWidth = 210;
        this.imageHeight = 231;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_WIDTH, TEX_HEIGHT);

        renderPowerBar(guiGraphics);
        renderInputTankFill(guiGraphics);
        renderPipeOverlays(guiGraphics);
        renderOutputTanks(guiGraphics);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name,
                this.imageWidth / 2 - 17 - this.font.width(name) / 2,
                6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle,
                8, this.imageHeight - 96 + 4, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        FluidTank[] tanks = refinery.getTanks();
        tanks[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 30, this.topPos + 27, 21, 104);
        tanks[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 86, this.topPos + 42, 16, 52);
        tanks[2].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 106, this.topPos + 42, 16, 52);
        tanks[3].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 126, this.topPos + 42, 16, 52);
        tanks[4].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 146, this.topPos + 42, 16, 52);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                186, 18, 16, 52,
                refinery.getEnergyStored(), refinery.getMaxEnergyStored());
    }

    private void renderPowerBar(GuiGraphics guiGraphics) {
        long maxPower = Math.max(refinery.getMaxEnergyStored(), 1L);
        int power = (int) (refinery.getEnergyStored() * 50L / maxPower);
        if (power <= 0) {
            return;
        }
        if (power > 50) {
            power = 50;
        }
        guiGraphics.blit(TEXTURE,
                this.leftPos + 186, this.topPos + 69 - power,
                210, 52 - power,
                16, power,
                TEX_WIDTH, TEX_HEIGHT);
    }

    private void renderInputTankFill(GuiGraphics guiGraphics) {
        FluidTank inputTank = refinery.getTank(MachineRefineryBlockEntity.TANK_INPUT);
        if (inputTank.getFill() <= 0 || inputTank.getMaxFill() <= 0) {
            return;
        }

        int targetHeight = inputTank.getFill() * 101 / inputTank.getMaxFill();
        if (targetHeight <= 0) {
            return;
        }

        int color = HbmFluidRegistry.getTintColor(inputTank.getConfiguredFluid()) & 0xFFFFFF;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, 1.0F);
        guiGraphics.blit(TEXTURE,
                this.leftPos + 33, this.topPos + 130 - targetHeight,
                226, 101 - targetHeight,
                16, targetHeight,
                TEX_WIDTH, TEX_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderPipeOverlays(GuiGraphics guiGraphics) {
        if (!refinery.hasDisplayRecipe()) {
            renderNeutralPipes(guiGraphics);
            return;
        }

        Fluid[] outputFluids = refinery.getDisplayPipeFluids();
        renderTintedPipe(guiGraphics, outputFluids[0], 52, 63, 247, 1, 33, 48);
        renderTintedPipe(guiGraphics, outputFluids[1], 52, 32, 247, 50, 66, 52);
        renderTintedPipe(guiGraphics, outputFluids[2], 52, 24, 247, 145, 86, 35);
        renderTintedPipe(guiGraphics, outputFluids[3], 36, 16, 211, 119, 122, 25);
    }

    private void renderNeutralPipes(GuiGraphics guiGraphics) {
        guiGraphics.blit(TEXTURE, this.leftPos + 52, this.topPos + 63, 247, 1, 33, 48, TEX_WIDTH, TEX_HEIGHT);
        guiGraphics.blit(TEXTURE, this.leftPos + 52, this.topPos + 32, 247, 50, 66, 52, TEX_WIDTH, TEX_HEIGHT);
        guiGraphics.blit(TEXTURE, this.leftPos + 52, this.topPos + 24, 247, 145, 86, 35, TEX_WIDTH, TEX_HEIGHT);
        guiGraphics.blit(TEXTURE, this.leftPos + 36, this.topPos + 16, 211, 119, 122, 25, TEX_WIDTH, TEX_HEIGHT);
    }

    private void renderTintedPipe(GuiGraphics guiGraphics, Fluid fluid,
                                  int x, int y, int u, int v, int width, int height) {
        int color = HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, u, v, width, height, TEX_WIDTH, TEX_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderOutputTanks(GuiGraphics guiGraphics) {
        refinery.getTank(MachineRefineryBlockEntity.TANK_HEAVY).renderTank(guiGraphics, this.leftPos + 86, this.topPos + 42, 16, 52);
        refinery.getTank(MachineRefineryBlockEntity.TANK_NAPHTHA).renderTank(guiGraphics, this.leftPos + 106, this.topPos + 42, 16, 52);
        refinery.getTank(MachineRefineryBlockEntity.TANK_LIGHT).renderTank(guiGraphics, this.leftPos + 126, this.topPos + 42, 16, 52);
        refinery.getTank(MachineRefineryBlockEntity.TANK_PETROLEUM).renderTank(guiGraphics, this.leftPos + 146, this.topPos + 42, 16, 52);
    }
}