package com.hbm_m.client.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.MachineCrystallizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * GUI для Crystallizer — порт с 1.7.10 GUICrystallizer.
 * UV-координаты и layout соответствуют оригиналу.
 */
public class GUIMachineCrystallizer extends GuiInfoScreen<MachineCrystallizerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_crystallizer_alt.png");

    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;
    private static final int TANK_CAPACITY = 8_000;

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

        long maxEnergy = menu.getMaxEnergyLong();
        if (maxEnergy > 0) {
            int i = (int) menu.getBlockEntity().getPowerScaled(52);
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 70 - i, 176, 64 - i, 16, i);
        }

        int j = menu.getProgressScaled(28);
        if (j > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 80, this.topPos + 47, 176, 0, j, 12);
        }

        drawInfoPanel(guiGraphics, 117, 22, PanelType.SMALL_BLUE_INFO);

        renderFluidTank(guiGraphics, menu.getBlockEntity().getTank(), this.leftPos + 35, this.topPos + 70);
    }

    private void renderFluidTank(GuiGraphics guiGraphics, FluidTank tank, int x, int y) {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) return;

        int pixelHeight = (int) ((long) fluid.getAmount() * TANK_HEIGHT / TANK_CAPACITY);
        if (pixelHeight == 0 && fluid.getAmount() > 0) pixelHeight = 1;
        if (pixelHeight > TANK_HEIGHT) pixelHeight = TANK_HEIGHT;

        IClientFluidTypeExtensions fluidProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation fluidTextureId = fluidProps.getStillTexture(fluid);
        var fluidSprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidTextureId);
        int fluidColor = fluidProps.getTintColor(fluid);

        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;
        float a = ((fluidColor >> 24) & 255) / 255.0F;
        if (a == 0) a = 1.0F;

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

        guiGraphics.blit(x, y + TANK_HEIGHT - pixelHeight, 0, TANK_WIDTH, pixelHeight, fluidSprite);

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
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
                menu.getEnergyLong(), menu.getMaxEnergyLong());

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
        FluidStack fluid = tank.getFluid();
        List<Component> tooltip = new ArrayList<>();
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("gui.hbm_m.fluid.empty"));
        } else {
            tooltip.add(fluid.getDisplayName());
            tooltip.add(Component.literal(fluid.getAmount() + " / " + TANK_CAPACITY + " mB"));
        }
        guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
    }
}
