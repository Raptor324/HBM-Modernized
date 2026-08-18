package com.hbm_m.inventory.gui;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.client.gui.FluidGuiRendering;
import com.hbm_m.inventory.menu.MachineSolarBoilerMenu;
import com.hbm_m.lib.RefStrings;
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
 * Solar Boiler GUI: same slot/tank layout as {@link GUIMachineIndustrialBoiler} (water in/out,
 * steam in/out, two tank bars) but no energy bar - purely solar driven. The leftmost decorative
 * icon column is repurposed as a sunlight/mirror-count info hover instead of an energy readout.
 */
public class GUIMachineSolarBoiler extends GuiInfoScreen<MachineSolarBoilerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_solar_boiler.png");

    public GUIMachineSolarBoiler(MachineSolarBoilerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    private static final int WATER_TANK_X = 61;
    private static final int STEAM_TANK_X = 133;
    private static final int TANK_Y = 16;
    private static final int TANK_WIDTH = 17;
    private static final int TANK_HEIGHT = 52;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, TEXTURE);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        var be = menu.getBlockEntity();
        renderTank(guiGraphics, WATER_TANK_X, be.getTank(com.hbm_m.blockentity.machines.MachineSolarBoilerBlockEntity.TANK_WATER).getStoredFluid(), menu.getWaterAmount(), menu.getWaterCapacity());
        renderTank(guiGraphics, STEAM_TANK_X, be.getTank(com.hbm_m.blockentity.machines.MachineSolarBoilerBlockEntity.TANK_STEAM).getStoredFluid(), menu.getSteamAmount(), menu.getSteamCapacity());
    }

    private void renderTank(GuiGraphics guiGraphics, int relX, Fluid fluid, int fill, int capacity) {
        if (fill <= 0 || capacity <= 0) return;
        if (fluid == null || fluid == Fluids.EMPTY) return;

        int filled = (int) ((long) fill * TANK_HEIGHT / capacity);
        if (filled <= 0) return;
        if (filled > TANK_HEIGHT) filled = TANK_HEIGHT;

        int fluidColor = HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF;
        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;

        FluidStack fStack = FluidStack.create(fluid, fill);
        ResourceLocation fluidPng = FluidGuiRendering.guiTexturePngForFluid(fluid, fStack);
        if (fluidPng == null) return;

        int x0 = this.leftPos + relX;
        int y0 = this.topPos + TANK_Y;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, 1.0F);
        FluidGuiRendering.renderTiledFluid(guiGraphics, fluidPng, x0, y0 + TANK_HEIGHT - filled, TANK_WIDTH, filled);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                WATER_TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT,
                mouseX, mouseY,
                Component.translatable("gui.hbm_m.industrial_boiler.water"),
                Component.literal(menu.getWaterAmount() + " / " + menu.getWaterCapacity() + " mB"));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                STEAM_TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT,
                mouseX, mouseY,
                Component.translatable("gui.hbm_m.industrial_boiler.steam"),
                Component.literal(menu.getSteamAmount() + " / " + menu.getSteamCapacity() + " mB"));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                8, TANK_Y, 16, TANK_HEIGHT,
                mouseX, mouseY,
                Component.translatable("gui.hbm_m.solar_boiler.sunlight"),
                Component.literal(menu.getSolarBrightness() + " / 15"),
                Component.translatable("gui.hbm_m.solar_boiler.mirrors", menu.getBlockEntity().getActiveMirrorCount()));
    }
}
