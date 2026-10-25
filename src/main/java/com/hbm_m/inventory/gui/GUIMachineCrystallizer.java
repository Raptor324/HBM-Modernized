package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.client.gui.FluidGuiRendering;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCrystallizerMenu;
import com.hbm_m.lib.RefStrings;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * GUI для Crystallizer - порт с 1.7.10 GUICrystallizer.
 * 1:1: Textur, Balken, Tank (35, 18-70), Titel bei 70 - Breite/2.
 */
@OnlyIn(Dist.CLIENT)
public class GUIMachineCrystallizer extends GuiInfoScreen<MachineCrystallizerMenu> {

        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_crystallizer_alt.png");

    // Окошко бака на текстуре GUI: x=35, y=18..70 → 16×52
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;

    public GUIMachineCrystallizer(MachineCrystallizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Original: getPowerScaled(52) -> drawTexturedModalRect(guiLeft + 152, guiTop + 70 - i, 176, 64 - i, 16, i)
        long energyStored = menu.getEnergyStored();
        long maxEnergy = menu.getMaxEnergyStored();
        if (maxEnergy > 0) {
            int i = (int) (energyStored * 52L / maxEnergy);
            if (i > 52) i = 52;
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 70 - i, 176, 64 - i, 16, i);
        }

        int j = menu.getProgressScaled(28);
        guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 47, 176, 0, j, 12);

        // Original: drawInfoPanel(..., 8) = kleiner blauer Stern
        drawInfoPanel(guiGraphics, 117, 22, PanelType.SMALL_BLUE_STAR);

        // Шкала жидкости. Окошко бака на текстуре GUI занимает y=18..70 (top+18 — верх,
        // top+70 — низ). renderFluidTank рисует жидкость снизу вверх: уровень растёт от
        // нижнего края окна. Поэтому передаём ВЕРХ окна — top+18.
        if (menu.getBlockEntity() != null) { // тайл может отсутствовать в реплее Flashback
            renderFluidTank(guiGraphics, menu.getBlockEntity().getTank(), this.leftPos + 35, this.topPos + 18);
        }
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
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, 70 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                152, 18, 16, 52,
                menu.getEnergyStored(), menu.getMaxEnergyStored());

        if (isPointInRect(35, 18, 16, 52, mouseX, mouseY) && menu.getBlockEntity() != null) {
            renderTankTooltip(guiGraphics, menu.getBlockEntity().getTank(), mouseX, mouseY);
        }

        Component[] upgradeText = new Component[]{
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.effectiveness"),
                Component.translatable("desc.gui.upgrade.overdrive")
        };
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 117, 22, 8, 8, this.leftPos + 200, this.topPos + 45, upgradeText);
    }
}
