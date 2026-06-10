package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.client.gui.FluidGuiRendering;
import com.hbm_m.inventory.menu.MachineCrystallizerMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import dev.architectury.fluid.FluidStack;
import net.minecraft.world.level.material.Fluid;

/**
 * GUI для Crystallizer - порт с 1.7.10 GUICrystallizer.
 * UV-координаты и layout соответствуют оригиналу.
 */
public class GUIMachineCrystallizer extends GuiInfoScreen<MachineCrystallizerMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(
            RefStrings.MODID, "textures/gui/processing/gui_crystallizer_alt.png");
    *///?} else {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_crystallizer_alt.png");
    //?}


    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;
    private static final int TANK_CAPACITY = 8_000;

    // Область подписи ожидаемой жидкости в нижней центральной части GUI.
    private static final int EXPECTED_FLUID_LABEL_CENTER_X = 98;
    private static final int EXPECTED_FLUID_LABEL_Y = 82;
    private static final int EXPECTED_FLUID_LABEL_MAX_WIDTH = 74;

    public GUIMachineCrystallizer(MachineCrystallizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Шкала энергии — берём из ContainerData (синхрон ванилью).
        long energyStored = menu.getEnergyStored();
        long maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy > 0) {
            int i = (int) (energyStored * 52L / maxEnergy);
            if (i > 52) i = 52;
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 70 - i, 176, 64 - i, 16, i);
        }

        int j = menu.getProgressScaled(28);
        if (j > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 47, 176, 0, j, 12);
        }

        drawInfoPanel(guiGraphics, 117, 22, PanelType.SMALL_BLUE_INFO);

        // Шкала жидкости. Окошко бака на текстуре GUI занимает y=18..70 (top+18 — верх,
        // top+70 — низ). renderFluidTank рисует жидкость снизу вверх: уровень растёт от
        // нижнего края окна. Поэтому передаём ВЕРХ окна — top+18.
        renderFluidTank(guiGraphics, menu.getBlockEntity().getTank(), this.leftPos + 35, this.topPos + 18);
    }

    private void renderFluidTank(GuiGraphics guiGraphics, FluidTank tank, int x, int y) {
        Fluid fluid = tank.getStoredFluid();
        int amountMb = tank.getFluidAmountMb();
        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY || amountMb <= 0) return;

        int pixelHeight = (int) ((long) amountMb * TANK_HEIGHT / TANK_CAPACITY);
        if (pixelHeight == 0 && amountMb > 0) pixelHeight = 1;
        if (pixelHeight > TANK_HEIGHT) pixelHeight = TANK_HEIGHT;

        int fluidColor = HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF;
        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;

        ResourceLocation png = FluidGuiRendering.guiTexturePngForFluid(fluid, FluidStack.create(fluid, (long) amountMb));
        if (png == null) return;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, 1.0F);

        FluidGuiRendering.renderTiledFluid(guiGraphics, png, x, y + TANK_HEIGHT - pixelHeight, TANK_WIDTH, pixelHeight);

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }


    private Component getExpectedFluidLabel() {
        Fluid expected = menu.getBlockEntity().getTank().getTankType();

        if (expected == null
                || expected == net.minecraft.world.level.material.Fluids.EMPTY
                || expected == ModFluids.NONE.getSource()) {
            return Component.literal("Вставьте идентификатор");
        }

        String fluidName = HbmFluidRegistry.getFluidName(expected);
        return Component.translatable("fluid." + RefStrings.MODID + "." + fluidName);
    }

    private void drawCenteredFittedString(GuiGraphics guiGraphics, Component text, int centerX, int y, int maxWidth, int color) {
        int textWidth = this.font.width(text);
        if (textWidth <= 0) return;

        float scale = textWidth > maxWidth ? (float) maxWidth / (float) textWidth : 1.0F;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);

        int drawX = Math.round((centerX - textWidth * scale / 2.0F) / scale);
        int drawY = Math.round(y / scale);
        guiGraphics.drawString(this.font, text, drawX, drawY, color, false);

        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        drawCenteredFittedString(guiGraphics, getExpectedFluidLabel(),
                EXPECTED_FLUID_LABEL_CENTER_X, EXPECTED_FLUID_LABEL_Y,
                EXPECTED_FLUID_LABEL_MAX_WIDTH, 0x404040);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                152, 18, 16, 52,
                menu.getEnergyStored(), menu.getMaxEnergyStored());

        if (isPointInRect(35, 18, 16, 52, mouseX, mouseY)) {
            renderTankTooltip(guiGraphics, menu.getBlockEntity().getTank(), mouseX, mouseY);
        }

        Component[] upgradeText = new Component[]{
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.effectiveness"),
                Component.translatable("desc.gui.upgrade.overdrive")
        };
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 117, 22, 8, 8, mouseX, mouseY, upgradeText);
    }

    private void renderTankTooltip(GuiGraphics guiGraphics, FluidTank tank, int mouseX, int mouseY) {
        Fluid fluid = tank.getStoredFluid();
        int amountMb = tank.getFluidAmountMb();
        List<Component> tooltip = new ArrayList<>();
        if (fluid == net.minecraft.world.level.material.Fluids.EMPTY || amountMb <= 0) {
            tooltip.add(Component.translatable("gui.hbm_m.fluid.empty"));
        } else {
            tooltip.add(Component.literal(HbmFluidRegistry.getFluidName(fluid)));
            tooltip.add(Component.literal(amountMb + " / " + TANK_CAPACITY + " mB"));
        }
        guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
    }
}