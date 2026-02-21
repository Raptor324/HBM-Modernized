package com.hbm_m.client.overlay;

import com.hbm_m.menu.MachineChemicalPlantMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

public class GUIMachineChemicalPlant extends AbstractContainerScreen<MachineChemicalPlantMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("hbm_m", "textures/gui/gui_chemplant.png");

    // Main GUI dimensions (full texture)
    private static final int MACHINE_WIDTH = 176;
    private static final int MACHINE_HEIGHT = 222;
    
    // Fluid tank display area (right side of GUI)
    private final int tankX = 158;
    private final int tankY = 17;
    private final int tankWidth = 16;
    private final int tankHeight = 132;
    private final int tankCapacity = 8000;

    public GUIMachineChemicalPlant(MachineChemicalPlantMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // Full GUI dimensions including inventory
        this.imageWidth = 176;
        this.imageHeight = 240;
        this.inventoryLabelY = 148;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderFluidTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw full GUI background (machine + inventory in one texture)
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Draw fluid in tank
        renderFluid(guiGraphics);

        // Draw progress (if needed)
        renderProgress(guiGraphics);
    }

    private void renderFluid(GuiGraphics guiGraphics) {
        FluidStack fluidStack = this.menu.getFluid();
        if (fluidStack.isEmpty()) return;

        int fluidAmount = this.menu.getFluidAmount();
        int pixelHeight = (int) ((long) fluidAmount * tankHeight / tankCapacity);
        if (pixelHeight == 0 && fluidAmount > 0) pixelHeight = 1;
        if (pixelHeight > tankHeight) pixelHeight = tankHeight;

        IClientFluidTypeExtensions fluidProps = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation fluidTextureId = fluidProps.getStillTexture(fluidStack);

        TextureAtlasSprite fluidSprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidTextureId);
        int fluidColor = fluidProps.getTintColor(fluidStack);

        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;
        float a = ((fluidColor >> 24) & 255) / 255.0F;

        RenderSystem.setShaderColor(r, g, b, a);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

        int x = this.leftPos + tankX;
        int y = this.topPos + tankY + tankHeight - pixelHeight;

        guiGraphics.blit(x, y, 0, tankWidth, pixelHeight, fluidSprite);

        // Reset color
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderProgress(GuiGraphics guiGraphics) {
        float progress = this.menu.getProgressScaled();
        if (progress <= 0) return;

        // Progress arrow rendering - adjust position based on texture
        int arrowX = 71;
        int arrowY = 35;
        int arrowWidth = 22;
        int arrowHeight = 15;
        int scaledWidth = (int) (arrowWidth * progress);
        
        // Draw progress overlay from texture
        guiGraphics.blit(TEXTURE, 
            this.leftPos + arrowX, 
            this.topPos + arrowY, 
            176, 0, 
            scaledWidth, arrowHeight);
    }

    private void renderFluidTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + tankX;
        int y = this.topPos + tankY;

        if (mouseX >= x && mouseX < x + tankWidth && mouseY >= y && mouseY < y + tankHeight) {
            FluidStack fluidStack = this.menu.getFluid();
            int amount = this.menu.getFluidAmount();

            if (!fluidStack.isEmpty()) {
                List<Component> tooltip = List.of(
                    fluidStack.getDisplayName(),
                    Component.literal("§7" + amount + " / " + tankCapacity + " mB")
                );
                guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            } else {
                List<Component> tooltip = List.of(
                    Component.literal("§7Empty"),
                    Component.literal("§70 / " + tankCapacity + " mB")
                );
                guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw title
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        // Draw player inventory label
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}
