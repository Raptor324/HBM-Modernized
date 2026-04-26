package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.client.gui.FluidGuiRendering;
import com.hbm_m.inventory.fluid.trait.FluidTraitManager;
import com.hbm_m.inventory.menu.MachineFluidTankMenu;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.FluidTankModePacket;
import com.hbm_m.network.ModPacketHandler;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraftforge.fluids.FluidStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

public class GUIMachineFluidTank extends AbstractContainerScreen<MachineFluidTankMenu> {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(MainRegistry.MOD_ID, "textures/gui/storage/gui_tank.png");
    *///?} else {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/storage/gui_tank.png");
    //?}


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

        Fluid fluid = fluidStack.getFluid();
        
        // Получаем tint цвет - сначала пробуем HbmFluidRegistry для кастомных жидкостей
        int fluidColor = getFluidTintColor(fluid);
        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;
        float a = ((fluidColor >> 24) & 255) / 255.0F;
        if (a == 0) a = 1.0F; // Если alpha не задана, используем 1.0

        ResourceLocation fluidPng = FluidGuiRendering.guiTexturePngForFluid(fluid, dev.architectury.hooks.fluid.forge.FluidStackHooksForge.fromForge(fluidStack));
        if (fluidPng == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a);

        int x = this.leftPos + tankX;
        int y = this.topPos + tankY + tankHeight - pixelHeight;

        FluidGuiRendering.renderTiledFluid(guiGraphics, fluidPng, x, y, tankWidth, pixelHeight);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /**
     * Получает tint цвет для жидкости.
     * Сначала проверяет HbmFluidRegistry для кастомных жидкостей HBM,
     * затем использует стандартный метод Forge.
     */
    private int getFluidTintColor(Fluid fluid) {
        // Для кастомных жидкостей HBM используем HbmFluidRegistry
        String fluidName = HbmFluidRegistry.getFluidName(fluid);
        if (fluidName != null && !fluidName.equals("none") && !fluidName.equals("empty")) {
            int hbmColor = HbmFluidRegistry.getTintColor(fluid);
            if (hbmColor != 0xFFFFFF) {
                return hbmColor;
            }
        }
        
        // Для ванильных и других жидкостей используем Forge API
        IClientFluidTypeExtensions fluidProps = IClientFluidTypeExtensions.of(fluid);
        return fluidProps.getTintColor();
    }

    private void renderFluidTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isHovering(tankX, tankY, tankWidth, tankHeight, mouseX, mouseY)) return;

        FluidStack fluid = this.menu.getFluid();
        List<Component> lines = new ArrayList<>();
        boolean shift = Screen.hasShiftDown();
        int pressure = this.menu.getPressure();

        if (!fluid.isEmpty()) {
            lines.add(fluid.getDisplayName());
            lines.add(Component.literal(fluid.getAmount() + " / " + tankCapacity + " mB"));
            appendPressureLines(lines, pressure);
            FluidTraitManager.appendFluidTypeTooltip(fluid.getFluid(), shift, lines);
        } else {
            // При 0 mB getFluid() пустой, но тип цистерны (как текстура в мире) — data[1], не filterFluid[6]
            Fluid tankType = this.menu.getTankTypeFluid();
            Component lockedTypeName = null;
            if (tankType == ModFluids.NONE.getSource()) {
                lockedTypeName = Component.translatable("fluid.hbm_m.none");
            } else if (tankType != null && tankType != Fluids.EMPTY) {
                lockedTypeName = Component.translatable(tankType.getFluidType().getDescriptionId());
            }
            if (lockedTypeName != null) {
                lines.add(Component.translatable("gui.hbm_m.fluid_tank.empty_locked", lockedTypeName));
                lines.add(Component.literal("0 / " + tankCapacity + " mB"));
                appendPressureLines(lines, pressure);
                if (tankType != null && tankType != Fluids.EMPTY && tankType != ModFluids.NONE.getSource()) {
                    FluidTraitManager.appendFluidTypeTooltip(tankType, shift, lines);
                }
            } else {
                int filterId = this.menu.getFilterFluidId();
                if (filterId >= 0) {
                    Fluid filterFluid = BuiltInRegistries.FLUID.byId(filterId);
                    if (filterFluid != null && filterFluid != Fluids.EMPTY) {
                        lines.add(Component.translatable("gui.hbm_m.fluid_tank.empty_filter",
                                Component.translatable(filterFluid.getFluidType().getDescriptionId())));
                        appendPressureLines(lines, pressure);
                        FluidTraitManager.appendFluidTypeTooltip(filterFluid, shift, lines);
                    } else {
                        lines.add(Component.translatable("gui.hbm_m.fluid_tank.empty"));
                        appendPressureLines(lines, pressure);
                    }
                } else {
                    lines.add(Component.translatable("gui.hbm_m.fluid_tank.empty"));
                    appendPressureLines(lines, pressure);
                }
            }
        }

        guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
    }

    private static void appendPressureLines(List<Component> lines, int pressure) {
        if (pressure == 0) return;
        lines.add(Component.translatable("gui.hbm_m.fluid_tank.pressure", pressure).withStyle(ChatFormatting.RED));
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        lines.add(Component.translatable("gui.hbm_m.fluid_tank.pressurized")
                .withStyle(blink ? ChatFormatting.RED : ChatFormatting.DARK_RED));
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
