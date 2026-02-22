package com.hbm_m.client.overlay;

import com.hbm_m.menu.MachineFluidTankMenu;
import com.hbm_m.network.FluidTankModePacket;
import com.hbm_m.network.ModPacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GUIMachineFluidTank extends AbstractContainerScreen<MachineFluidTankMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("hbm_m", "textures/gui/storage/gui_fluid_tank.png");

    private final int tankX = 71;
    private final int tankY = 17;
    private final int tankWidth = 34;
    private final int tankHeight = 52;
    private final int tankCapacity = 256000;

    private static final int MODE_BUTTON_X = 151;
    private static final int MODE_BUTTON_Y = 34;
    private static final int MODE_BUTTON_SIZE = 18;

    public GUIMachineFluidTank(MachineFluidTankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderFluidTooltip(guiGraphics, mouseX, mouseY);
        renderModeButtonTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int mode = menu.getMode();
        guiGraphics.blit(TEXTURE, this.leftPos + MODE_BUTTON_X, this.topPos + MODE_BUTTON_Y, 176, mode * MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE);

        renderFluid(guiGraphics);
    }

    private void renderFluid(GuiGraphics guiGraphics) {
        FluidStack fluidStack = this.menu.getFluid();
        if (fluidStack.isEmpty()) return;

        int pixelHeight = (int) ((long) fluidStack.getAmount() * tankHeight / tankCapacity);
        if (pixelHeight == 0 && fluidStack.getAmount() > 0) pixelHeight = 1;
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

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderFluidTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isHovering(tankX, tankY, tankWidth, tankHeight, mouseX, mouseY)) return;

        FluidStack fluid = this.menu.getFluid();
        List<Component> lines = new ArrayList<>();

        if (!fluid.isEmpty()) {
            lines.add(fluid.getDisplayName());
            lines.add(Component.literal(fluid.getAmount() + " / " + tankCapacity + " mB"));
        } else {
            int filterId = this.menu.getFilterFluidId();
            if (filterId >= 0) {
                Fluid filterFluid = BuiltInRegistries.FLUID.byId(filterId);
                lines.add(Component.translatable("gui.hbm_m.fluid_tank.empty_filter", Component.translatable(filterFluid.getFluidType().getDescriptionId())));
            } else {
                lines.add(Component.translatable("gui.hbm_m.fluid_tank.empty"));
            }
        }

        guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
    }

    private void renderModeButtonTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, mouseX, mouseY)) {
            int mode = menu.getMode();
            String key = "gui.hbm_m.fluid_tank.mode." + mode;
            guiGraphics.renderTooltip(this.font, Component.translatable(key), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE, (int) mouseX, (int) mouseY)) {
            ModPacketHandler.INSTANCE.sendToServer(new FluidTankModePacket(menu.blockEntity.getBlockPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovering(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + w &&
                mouseY >= this.topPos + y && mouseY < this.topPos + y + h;
    }
}
