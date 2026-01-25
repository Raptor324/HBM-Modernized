package com.hbm_m.client.overlay;

import com.hbm_m.main.MainRegistry; // Твой Main класс
import com.hbm_m.menu.MachineFluidTankMenu;
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

public class GUIMachineFluidTank extends AbstractContainerScreen<MachineFluidTankMenu> {

    // Путь к текстуре фона. Убедись, что файл лежит в assets/hbm_m/textures/gui/
    private static final ResourceLocation TEXTURE = new ResourceLocation("hbm_m", "textures/gui/storage/gui_fluid_tank.png");

    // Координаты и размеры шкалы жидкости
    private final int tankX = 71;
    private final int tankY = 17;
    private final int tankWidth = 34;
    private final int tankHeight = 52;
    private final int tankCapacity = 256000; // 256 ведер

    public GUIMachineFluidTank(MachineFluidTankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Рисуем затемненный фон мира
        renderBackground(guiGraphics);

        // 2. Рисуем фон GUI и жидкость
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 3. Рисуем тултипы предметов (стандартный метод)
        renderTooltip(guiGraphics, mouseX, mouseY);

        // 4. Рисуем тултип жидкости, если навели мышку на шкалу
        renderFluidTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Отрисовка основной текстуры GUI
        // x, y, u, v, width, height
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Отрисовка жидкости ВНУТРИ шкалы
        renderFluid(guiGraphics);
    }

    private void renderFluid(GuiGraphics guiGraphics) {
        FluidStack fluidStack = this.menu.getFluid();
        if (fluidStack.isEmpty()) return;

        // Расчет высоты (как у тебя и было)
        int pixelHeight = (int) ((long) fluidStack.getAmount() * tankHeight / tankCapacity);
        if (pixelHeight == 0 && fluidStack.getAmount() > 0) pixelHeight = 1;
        if (pixelHeight > tankHeight) pixelHeight = tankHeight;

        // Получаем свойства жидкости
        IClientFluidTypeExtensions fluidProps = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation fluidTextureId = fluidProps.getStillTexture(fluidStack);

        // Получаем спрайт из Атласа Блоков
        TextureAtlasSprite fluidSprite = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidTextureId);
        int fluidColor = fluidProps.getTintColor(fluidStack);

        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;
        float a = ((fluidColor >> 24) & 255) / 255.0F;

        // Устанавливаем цвет
        RenderSystem.setShaderColor(r, g, b, a);

        // === ВАЖНОЕ ИСПРАВЛЕНИЕ ===
        // Мы говорим движку: "Перестань смотреть на gui_fluid_tank.png, смотри на общую карту блоков"
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        // ===========================

        int x = this.leftPos + tankX;
        int y = this.topPos + tankY + tankHeight - pixelHeight;

        // Рисуем спрайт
        guiGraphics.blit(
                x, y,
                0,
                tankWidth, pixelHeight,
                fluidSprite
        );

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderFluidTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isHovering(tankX, tankY, tankWidth, tankHeight, mouseX, mouseY)) {
            FluidStack fluid = this.menu.getFluid();
            if (!fluid.isEmpty()) {
                // Формируем список строк для тултипа
                guiGraphics.renderTooltip(this.font, List.of(
                        fluid.getDisplayName(), // Название (Нефть)
                        Component.literal(fluid.getAmount() + " / " + tankCapacity + " mB") // Кол-во
                ), Optional.empty(), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font, Component.literal("Empty"), mouseX, mouseY);
            }
        }
    }
}