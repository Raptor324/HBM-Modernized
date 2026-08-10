package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.client.gui.FluidGuiRendering;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.menu.MachineGasCentrifugeMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.PseudoFluidTank;
import com.hbm_m.recipe.PseudoFluidType;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.architectury.fluid.FluidStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Direct port of the 1.7.10 {@code GUIMachineGasCent}: same 206x204 canvas, same widget/tank/
 * slot coordinates, sourcing the energy-bar and progress-arrow sprites from the actual texture
 * instead of drawing placeholder colored fills.
 */
public class GUIMachineGasCentrifuge extends GuiInfoScreen<MachineGasCentrifugeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_centrifuge_gas.png");

    public GUIMachineGasCentrifuge(MachineGasCentrifugeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 206;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    // Energy bar sprite: UV (206, 0), 16x52 (green top / blue bottom); fills bottom-up by
    // sourcing UV (206, 52 - i), 16 x i onto screen (leftPos+182, topPos+69-i).
    private static final int ENERGY_BAR_X = 182;
    private static final int ENERGY_BAR_TOP_Y = 17;
    private static final int ENERGY_BAR_WIDTH = 16;
    private static final int ENERGY_BAR_HEIGHT = 52;
    private static final int ENERGY_BAR_UV_X = 206;
    private static final int ENERGY_BAR_UV_Y = 0;

    // Progress arrow sprite: UV (206, 52), up to 36x13.
    private static final int PROGRESS_X = 70;
    private static final int PROGRESS_Y = 35;
    private static final int PROGRESS_WIDTH = 36;
    private static final int PROGRESS_HEIGHT = 13;
    private static final int PROGRESS_UV_X = 206;
    private static final int PROGRESS_UV_Y = 52;

    // Pseudo-fluid tank bars: rendered using the real piped fluid's texture (matching the
    // original, which always binds gasCent.tank's texture for both bars), sized by the
    // corresponding pseudo tank's fill.
    private static final int INPUT_TANK_X = 16;
    private static final int OUTPUT_TANK_X = 138;
    private static final int TANK_Y = 16;
    private static final int TANK_WIDTH = 12;
    private static final int TANK_HEIGHT = 52;

    private static final int INFO_X = -12;
    private static final int INFO_ENRICHMENT_Y = 16;
    private static final int INFO_OUTPUT_Y = 32;
    private static final int INFO_SIZE = 16;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Energy bar (fills bottom-up, sourced from the real gradient sprite)
        long power = menu.getEnergyLong();
        long maxPower = menu.getMaxEnergyLong();
        if (power > 0 && maxPower > 0) {
            int filled = (int) (power * ENERGY_BAR_HEIGHT / maxPower);
            if (filled > 0) {
                int x0 = this.leftPos + ENERGY_BAR_X;
                int y0 = this.topPos + ENERGY_BAR_TOP_Y + (ENERGY_BAR_HEIGHT - filled);
                guiGraphics.blit(TEXTURE, x0, y0, ENERGY_BAR_UV_X, ENERGY_BAR_UV_Y + (ENERGY_BAR_HEIGHT - filled),
                        ENERGY_BAR_WIDTH, filled);
            }
        }

        // Progress arrow (fills left-to-right, sourced from the real arrow sprite)
        if (menu.isProcessing()) {
            int p = menu.getScaledProgress(PROGRESS_WIDTH);
            if (p > 0) {
                int x0 = this.leftPos + PROGRESS_X;
                int y0 = this.topPos + PROGRESS_Y;
                guiGraphics.blit(TEXTURE, x0, y0, PROGRESS_UV_X, PROGRESS_UV_Y, p, PROGRESS_HEIGHT);
            }
        }

        var blockEntity = menu.getBlockEntity();
        Fluid realFluid = blockEntity.getTank().getStoredFluid();
        renderPseudoTank(guiGraphics, INPUT_TANK_X, blockEntity.getInputTank(), realFluid);
        renderPseudoTank(guiGraphics, OUTPUT_TANK_X, blockEntity.getOutputTank(), realFluid);
    }

    /** Renders a pseudo-fluid tank's fill level using the real piped fluid's texture/tint,
     *  matching the original's behavior of always binding {@code gasCent.tank}'s texture. */
    private void renderPseudoTank(GuiGraphics guiGraphics, int relX, PseudoFluidTank tank, Fluid realFluid) {
        if (tank.getTankType() == PseudoFluidType.NONE || tank.getFill() <= 0) return;
        if (realFluid == null || realFluid == Fluids.EMPTY || realFluid == ModFluids.NONE.getSource()) return;

        int filled = (int) ((long) tank.getFill() * TANK_HEIGHT / tank.getMaxFill());
        if (filled <= 0) return;

        int fluidColor = HbmFluidRegistry.getTintColor(realFluid) & 0xFFFFFF;
        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;

        FluidStack fStack = FluidStack.create(realFluid, tank.getFill());
        ResourceLocation fluidPng = FluidGuiRendering.guiTexturePngForFluid(realFluid, fStack);
        if (fluidPng == null) return;

        int x0 = this.leftPos + relX;
        int y0 = this.topPos + TANK_Y;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, 1.0F);
        FluidGuiRendering.renderTiledFluid(guiGraphics, fluidPng, x0, y0 + TANK_HEIGHT - filled, TANK_WIDTH, filled);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                ENERGY_BAR_X, ENERGY_BAR_TOP_Y,
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

        drawInfoPanel(guiGraphics, INFO_X, INFO_ENRICHMENT_Y, PanelType.LARGE_GREEN_INFO);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                INFO_X, INFO_ENRICHMENT_Y, INFO_SIZE, INFO_SIZE,
                mouseX, mouseY,
                Component.translatable("desc.gui.gasCent.enrichment"));

        drawInfoPanel(guiGraphics, INFO_X, INFO_OUTPUT_Y, PanelType.LARGE_BLUE_INFO);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                INFO_X, INFO_OUTPUT_Y, INFO_SIZE, INFO_SIZE,
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
