package com.hbm_m.client.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.MachineChemicalPlantMenu;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * GUI для Chemical Plant — порт с 1.7.10.
 * UV-координаты и layout соответствуют оригинальному GUIMachineChemicalPlant.
 */
public class GUIMachineChemicalPlant extends AbstractContainerScreen<MachineChemicalPlantMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_chemplant.png");

    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 34;
    private static final int TANK_CAPACITY = 24_000;

    public GUIMachineChemicalPlant(MachineChemicalPlantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        long energyStored = menu.getBlockEntity().getEnergyStored();
        long maxEnergy = menu.getBlockEntity().getMaxEnergyStored();
        if (maxEnergy > 0) {
            int p = (int) (energyStored * 61L / maxEnergy);
            if (p > 61) p = 61;
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - p, 176, 61 - p, 16, p);
        }

        int progress = menu.getProgress();
        if (progress > 0) {
            int maxProgress = menu.getMaxProgress();
            if (maxProgress > 0) {
                int j = (int) Math.ceil(70.0 * progress / maxProgress);
                guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, j, 16);
            }
        }

        boolean hasRecipe = menu.getBlockEntity().getRecipe() != null;
        boolean didProcess = menu.getBlockEntity().getDidProcess();
        boolean canProcess = hasRecipe && energyStored >= 100;

        if (didProcess) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (hasRecipe) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            if (canProcess) {
                guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }

        for (int i = 0; i < 3; i++) {
            renderFluidTank(guiGraphics, menu.getBlockEntity().getInputTanks()[i],
                    this.leftPos + 8 + i * 18, this.topPos + 52);
            renderFluidTank(guiGraphics, menu.getBlockEntity().getOutputTanks()[i],
                    this.leftPos + 80 + i * 18, this.topPos + 52);
        }
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, TEXTURE);
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
        guiGraphics.drawString(this.font, name, 70 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);

        if (this.minecraft != null && this.minecraft.screen == this) {
            if (menu.getBlockEntity().getRecipe() != null) {
            } else {
                guiGraphics.renderItem(new ItemStack(ModItems.TEMPLATE_FOLDER.get()), 8, 126);
            }
        }
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

        if (isMouseOver(mouseX, mouseY, 152, 18, 16, 61)) {
            long energy = menu.getBlockEntity().getEnergyStored();
            long maxEnergy = menu.getBlockEntity().getMaxEnergyStored();
            guiGraphics.renderTooltip(this.font,
                    Component.literal(EnergyFormatter.format(energy) + " / " + EnergyFormatter.format(maxEnergy) + " HE")
                            .withStyle(ChatFormatting.GREEN),
                    mouseX, mouseY);
        }

        for (int i = 0; i < 3; i++) {
            if (isMouseOver(mouseX, mouseY, 8 + i * 18, 18, 16, 34)) {
                FluidTank tank = menu.getBlockEntity().getInputTanks()[i];
                renderTankTooltip(guiGraphics, tank, mouseX, mouseY);
                return;
            }
            if (isMouseOver(mouseX, mouseY, 80 + i * 18, 18, 16, 34)) {
                FluidTank tank = menu.getBlockEntity().getOutputTanks()[i];
                renderTankTooltip(guiGraphics, tank, mouseX, mouseY);
                return;
            }
        }

        if (isMouseOver(mouseX, mouseY, 7, 125, 18, 18)) {
            guiGraphics.renderTooltip(this.font,
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    mouseX, mouseY);
        }
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver((int) mouseX, (int) mouseY, 7, 125, 18, 18)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + w
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + h;
    }
}
