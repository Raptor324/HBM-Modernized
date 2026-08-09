package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.blockentity.network.MachineDroneCrateBlockEntity;
import com.hbm_m.client.gui.FluidGuiRendering;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineDroneCrateMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.architectury.fluid.FluidStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class GUIMachineDroneCrate extends GuiInfoScreen<MachineDroneCrateMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_crate_drone.png");

    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 34;

    private final MachineDroneCrateBlockEntity crate;

    public GUIMachineDroneCrate(MachineDroneCrateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.crate = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 185;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        guiGraphics.blit(TEXTURE, this.leftPos + 151, this.topPos + 16, 194, crate.isItemType() ? 0 : 18, 18, 18);
        guiGraphics.blit(TEXTURE, this.leftPos + 151, this.topPos + 52, 176, crate.isSendingMode() ? 18 : 0, 18, 18);

        renderFluidTank(guiGraphics, crate.getFluidTank(), this.leftPos + 125, this.topPos + 17);
    }

    private void renderFluidTank(GuiGraphics guiGraphics, FluidTank tank, int x, int y) {
        Fluid fluid = tank.getStoredFluid();
        int amountMb = tank.getFluidAmountMb();
        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY || amountMb <= 0) return;

        int capacity = tank.getMaxFill();
        int pixelHeight = capacity > 0 ? (int) ((long) amountMb * TANK_HEIGHT / capacity) : 0;
        if (pixelHeight == 0 && amountMb > 0) pixelHeight = 1;
        if (pixelHeight > TANK_HEIGHT) pixelHeight = TANK_HEIGHT;

        int fluidColor = HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF;
        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;

        ResourceLocation png = FluidGuiRendering.guiTexturePngForFluid(fluid, FluidStack.create(fluid, (long) amountMb));
        if (png == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, 1.0F);

        FluidGuiRendering.renderTiledFluid(guiGraphics, png, x, y + TANK_HEIGHT - pixelHeight, TANK_WIDTH, pixelHeight);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderTankTooltip(GuiGraphics guiGraphics, FluidTank tank, int mouseX, int mouseY) {
        Fluid fluid = tank.getStoredFluid();
        int amountMb = tank.getFluidAmountMb();
        List<Component> tooltip = new ArrayList<>();
        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY || amountMb <= 0) {
            tooltip.add(Component.translatable("gui.hbm_m.fluid.empty"));
        } else {
            tooltip.add(Component.literal(HbmFluidRegistry.getFluidName(fluid)));
            tooltip.add(Component.literal(amountMb + " / " + tank.getMaxFill() + " mB"));
        }
        guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
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

        if (isHovering(151, 16, 18, 18, mouseX, mouseY)) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 151, 16, 18, 18, mouseX, mouseY,
                    Component.literal("Mode: " + (crate.isItemType() ? "Item" : "Fluid")));
        }
        if (isHovering(151, 52, 18, 18, mouseX, mouseY)) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 151, 52, 18, 18, mouseX, mouseY,
                    Component.literal(crate.isSendingMode() ? "Sending" : "Receiving"));
        }
        if (isHovering(125, 17, 16, 34, mouseX, mouseY)) {
            renderTankTooltip(guiGraphics, crate.getFluidTank(), mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(151, 16, 18, 18, (int) mouseX, (int) mouseY)) {
            crate.toggleItemType();
            return true;
        }
        if (isHovering(151, 52, 18, 18, (int) mouseX, (int) mouseY)) {
            crate.toggleSendingMode();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovering(int x, int y, int w, int h, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }
}
