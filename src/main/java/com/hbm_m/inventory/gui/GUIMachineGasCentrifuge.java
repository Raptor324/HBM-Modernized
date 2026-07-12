package com.hbm_m.inventory.gui;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.menu.MachineGasCentrifugeMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PseudoFluidTank;
import com.hbm_m.recipe.PseudoFluidType;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class GUIMachineGasCentrifuge extends GuiInfoScreen<MachineGasCentrifugeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_centrifuge_gas.png");

    public GUIMachineGasCentrifuge(MachineGasCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 185;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    // The supplied gui_centrifuge_gas.png does not contain separate widget sprites in the usual
    // "x >= 176" area, so dynamic bars are rendered as colored fills over the background.

    private static final int ENERGY_BAR_X = 134;
    private static final int ENERGY_BAR_Y = 18;
    private static final int ENERGY_BAR_WIDTH = 16;
    private static final int ENERGY_BAR_HEIGHT = 54;

    // Progress indicator overlays the arrow shaft above the 2x2 output grid (x 71-105, y 39-40).
    private static final int PROGRESS_X = 71;
    private static final int PROGRESS_Y = 39;
    private static final int PROGRESS_WIDTH = 34;
    private static final int PROGRESS_HEIGHT = 2;

    // Pseudo-fluid tank bars: input (raw feed accumulating toward the next enrichment cycle) on
    // the left, output (enriched product handed off to the next centrifuge in the cascade) on
    // the right of the machine.
    private static final int INPUT_TANK_X = 16;
    private static final int OUTPUT_TANK_X = 150;
    private static final int TANK_Y = 16;
    private static final int TANK_WIDTH = 12;
    private static final int TANK_HEIGHT = 54;

    private static final int INFO_ENRICHMENT_X = 4;
    private static final int INFO_OUTPUT_X = 160;
    private static final int INFO_Y = 16;
    private static final int INFO_SIZE = 16;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Energy bar (fills bottom-up)
        long power = menu.getEnergyLong();
        long maxPower = menu.getMaxEnergyLong();
        if (power > 0 && maxPower > 0) {
            int filled = (int) (power * ENERGY_BAR_HEIGHT / maxPower);
            if (filled > 0) {
                int x0 = this.leftPos + ENERGY_BAR_X;
                int y0 = this.topPos + ENERGY_BAR_Y + (ENERGY_BAR_HEIGHT - filled);
                guiGraphics.fill(x0, y0, x0 + ENERGY_BAR_WIDTH, y0 + filled, 0xFF3FCFE0);
            }
        }

        // Progress indicator (fills left-to-right)
        if (menu.isProcessing()) {
            int p = menu.getScaledProgress(PROGRESS_WIDTH);
            if (p > 0) {
                int x0 = this.leftPos + PROGRESS_X;
                int y0 = this.topPos + PROGRESS_Y;
                guiGraphics.fill(x0, y0, x0 + p, y0 + PROGRESS_HEIGHT, 0xFFFFE066);
            }
        }

        var blockEntity = menu.getBlockEntity();
        int tint = tintForRealFluid(blockEntity.getTank().getStoredFluid());
        renderPseudoTank(guiGraphics, INPUT_TANK_X, blockEntity.getInputTank(), tint);
        renderPseudoTank(guiGraphics, OUTPUT_TANK_X, blockEntity.getOutputTank(), tint);
    }

    private int tintForRealFluid(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY || fluid == ModFluids.NONE.getSource()) {
            return 0xFF808080;
        }
        return 0xFF000000 | (HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF);
    }

    private void renderPseudoTank(GuiGraphics guiGraphics, int relX, PseudoFluidTank tank, int tint) {
        if (tank.getTankType() == PseudoFluidType.NONE || tank.getFill() <= 0) return;
        int filled = (int) ((long) tank.getFill() * TANK_HEIGHT / tank.getMaxFill());
        if (filled <= 0) return;
        int x0 = this.leftPos + relX;
        int y0 = this.topPos + TANK_Y + (TANK_HEIGHT - filled);
        guiGraphics.fill(x0, y0, x0 + TANK_WIDTH, y0 + filled, tint);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                ENERGY_BAR_X, ENERGY_BAR_Y,
                ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT,
                menu.getEnergyLong(),
                menu.getMaxEnergyLong());

        var blockEntity = menu.getBlockEntity();

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                INPUT_TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT,
                mouseX, mouseY,
                tankTooltip(blockEntity.getInputTank()));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                OUTPUT_TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT,
                mouseX, mouseY,
                tankTooltip(blockEntity.getOutputTank()));

        drawInfoPanel(guiGraphics, INFO_ENRICHMENT_X, INFO_Y, PanelType.LARGE_BLUE_INFO);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                INFO_ENRICHMENT_X, INFO_Y, INFO_SIZE, INFO_SIZE,
                mouseX, mouseY,
                Component.translatable("desc.gui.gasCent.enrichment"));

        drawInfoPanel(guiGraphics, INFO_OUTPUT_X, INFO_Y, PanelType.LARGE_GREEN_INFO);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                INFO_OUTPUT_X, INFO_Y, INFO_SIZE, INFO_SIZE,
                mouseX, mouseY,
                Component.translatable("desc.gui.gasCent.output"));
    }

    private Component[] tankTooltip(PseudoFluidTank tank) {
        return new Component[] {
                tank.getTankType().getDisplayName(),
                Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")
        };
    }
}
