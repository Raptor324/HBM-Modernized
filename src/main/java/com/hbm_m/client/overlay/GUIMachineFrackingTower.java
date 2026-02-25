package com.hbm_m.client.overlay;

import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.block.entity.custom.machines.MachineHydraulicFrackiningTowerBlockEntity;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachineFrackingTowerMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI Screen для Fracking Tower (Гидроразрывная вышка).
 * Порт из версии 1.7.10 (GUIMachineOilWell).
 * 
 * UV координаты текстуры (gui_well.png):
 * - Основной GUI: (0, 0) размером 176x166
 * - Индикатор энергии: UV (176, 0) размером 16x34
 * - Индикатор состояния: UV (176, 52) размером 16x16 для каждого состояния
 * - Заглушка для 2 танков: UV (192, 0) размером 18x34
 */
public class GUIMachineFrackingTower extends AbstractContainerScreen<MachineFrackingTowerMenu> {

    //=====================================================================================//
    // КОНСТАНТЫ ТЕКСТУРЫ
    //=====================================================================================//

    /** Путь к текстуре GUI */
    private static final ResourceLocation GUI_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/machine/gui_well.png");

    //=====================================================================================//
    // UV КООРДИНАТЫ (из оригинала)
    //=====================================================================================//

    // --- Энергия ---
    /** Позиция индикатора энергии на экране */
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 17;
    /** Размер индикатора энергии */
    private static final int ENERGY_WIDTH = 16;
    private static final int ENERGY_HEIGHT = 34;
    /** UV координаты энергии в текстуре (за пределами основного GUI) */
    private static final int ENERGY_UV_X = 176;
    private static final int ENERGY_UV_Y = 0;

    // --- Индикатор состояния ---
    /** Позиция индикатора состояния */
    private static final int INDICATOR_X = 35;
    private static final int INDICATOR_Y = 17;
    /** Размер индикатора */
    private static final int INDICATOR_SIZE = 16;
    /** UV Y координата индикаторов */
    private static final int INDICATOR_UV_Y = 52;

    // --- Танки ---
    /** Танк нефти (позиция нижнего левого угла) */
    private static final int OIL_TANK_X = 62;
    private static final int OIL_TANK_Y = 69;
    private static final int OIL_TANK_WIDTH = 16;
    private static final int OIL_TANK_HEIGHT = 52;

    /** Танк газа */
    private static final int GAS_TANK_X = 107;
    private static final int GAS_TANK_Y = 69;
    private static final int GAS_TANK_WIDTH = 16;
    private static final int GAS_TANK_HEIGHT = 52;

    /** Танк FrackSol */
    private static final int FRACKSOL_TANK_X = 40;
    private static final int FRACKSOL_TANK_Y = 69;
    private static final int FRACKSOL_TANK_WIDTH = 6;
    private static final int FRACKSOL_TANK_HEIGHT = 32;

    // --- Info Panel ---
    private static final int INFO_X = 156;
    private static final int INFO_Y = 3;
    private static final int INFO_SIZE = 8;
    private static final int INFO_UV_X = 232;
    private static final int INFO_UV_Y = 0;

    //=====================================================================================//
    // ПОЛЯ
    //=====================================================================================//

    private final MachineHydraulicFrackiningTowerBlockEntity blockEntity;

    //=====================================================================================//
    // КОНСТРУКТОР
    //=====================================================================================//

    public GUIMachineFrackingTower(MachineFrackingTowerMenu container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.blockEntity = container.getBlockEntity();
        
        // Размеры GUI (как в оригинале)
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    //=====================================================================================//
    // РЕНДЕРИНГ
    //=====================================================================================//

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Рендер основного фона GUI
        graphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Рендер индикатора энергии
        renderEnergyBar(graphics);

        // Рендер индикатора состояния
        renderIndicator(graphics);

        // Рендер танков жидкостей
        renderFluidTanks(graphics);

        // Рендер info panel
        renderInfoPanel(graphics);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // Название машины
        Component name = this.blockEntity != null 
                ? this.blockEntity.getDisplayName() 
                : Component.translatable("container.frackingTower");
        
        graphics.drawString(this.font, name, 
                this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        
        // Название инвентаря игрока
        graphics.drawString(this.font, 
                Component.translatable("container.inventory"), 
                8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Сначала рендерим фон
        this.renderBackground(graphics);
        
        // Затем основной GUI
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // И tooltips
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        if (this.blockEntity == null) return;

        // Tooltip для танков
        renderTankTooltip(graphics, mouseX, mouseY, 
                leftPos + OIL_TANK_X, topPos + OIL_TANK_Y - OIL_TANK_HEIGHT, 
                OIL_TANK_WIDTH, OIL_TANK_HEIGHT, 
                blockEntity.getOilTank(), "fluid.hbm_m.crude_oil");

        renderTankTooltip(graphics, mouseX, mouseY, 
                leftPos + GAS_TANK_X, topPos + GAS_TANK_Y - GAS_TANK_HEIGHT, 
                GAS_TANK_WIDTH, GAS_TANK_HEIGHT, 
                blockEntity.getGasTank(), "fluid.hbm_m.gas");

        renderTankTooltip(graphics, mouseX, mouseY, 
                leftPos + FRACKSOL_TANK_X, topPos + FRACKSOL_TANK_Y - FRACKSOL_TANK_HEIGHT, 
                FRACKSOL_TANK_WIDTH, FRACKSOL_TANK_HEIGHT, 
                blockEntity.getFracksolTank(), "fluid.hbm_m.fracksol");

        // Tooltip для энергии
        renderEnergyTooltip(graphics, mouseX, mouseY);

        // Tooltip для info panel
        renderInfoPanelTooltip(graphics, mouseX, mouseY);
    }

    //=====================================================================================//
    // РЕНДЕР КОМПОНЕНТОВ
    //=====================================================================================//

    /**
     * Рендер индикатора энергии.
     * UV: (176, 0) размером 16x34, заполняется снизу вверх.
     */
    private void renderEnergyBar(GuiGraphics graphics) {
        if (this.blockEntity == null) return;

        long energy = this.blockEntity.getEnergyStored();
        long maxEnergy = this.blockEntity.getMaxEnergyStored();
        
        // Рассчитываем высоту заполнения (как в оригинале: 34 * power / maxPower)
        int fillHeight = (int) (ENERGY_HEIGHT * energy / maxEnergy);
        
        if (fillHeight > 0) {
            // Рендер заполненной части (снизу вверх)
            // В оригинале: drawTexturedModalRect(guiLeft + 8, guiTop + 51 - i, 176, 34 - i, 16, i);
            // UV берётся из (176, 34 - fillHeight) с размером (16, fillHeight)
            int renderY = topPos + ENERGY_Y + ENERGY_HEIGHT - fillHeight;
            int uvY = ENERGY_UV_Y + ENERGY_HEIGHT - fillHeight;
            
            graphics.blit(GUI_TEXTURE, 
                    leftPos + ENERGY_X, renderY, 
                    ENERGY_UV_X, uvY, 
                    ENERGY_WIDTH, fillHeight);
        }
    }

    /**
     * Рендер индикатора состояния.
     * UV: (176 + (k-1)*16, 52) размером 16x16 для каждого состояния.
     * k=1,2,3 - разные состояния.
     */
    private void renderIndicator(GuiGraphics graphics) {
        if (this.blockEntity == null) return;

        int indicator = this.blockEntity.getIndicator();
        
        if (indicator > 0 && indicator <= 3) {
            // В оригинале: drawTexturedModalRect(guiLeft + 35, guiTop + 17, 176 + (k - 1) * 16, 52, 16, 16);
            int uvX = 176 + (indicator - 1) * INDICATOR_SIZE;
            
            graphics.blit(GUI_TEXTURE, 
                    leftPos + INDICATOR_X, topPos + INDICATOR_Y, 
                    uvX, INDICATOR_UV_Y, 
                    INDICATOR_SIZE, INDICATOR_SIZE);
        }
    }

    /**
     * Рендер танков жидкостей.
     * Все танки рендерятся снизу вверх (как в оригинале).
     */
    private void renderFluidTanks(GuiGraphics graphics) {
        if (this.blockEntity == null) return;

        // Танк нефти
        renderFluidTank(graphics, 
                leftPos + OIL_TANK_X, topPos + OIL_TANK_Y, 
                OIL_TANK_WIDTH, OIL_TANK_HEIGHT, 
                blockEntity.getOilTank());

        // Танк газа
        renderFluidTank(graphics, 
                leftPos + GAS_TANK_X, topPos + GAS_TANK_Y, 
                GAS_TANK_WIDTH, GAS_TANK_HEIGHT, 
                blockEntity.getGasTank());

        // Танк FrackSol
        renderFluidTank(graphics, 
                leftPos + FRACKSOL_TANK_X, topPos + FRACKSOL_TANK_Y, 
                FRACKSOL_TANK_WIDTH, FRACKSOL_TANK_HEIGHT, 
                blockEntity.getFracksolTank());
    }

    /**
     * Рендер отдельного танка жидкости.
     * Жидкость рендерится снизу вверх.
     */
    private void renderFluidTank(GuiGraphics graphics, int x, int y, int width, int height, IFluidHandler tank) {
        FluidStack fluid = tank.getFluidInTank(0);
        if (fluid.isEmpty()) return;

        int amount = fluid.getAmount();
        int capacity = tank.getTankCapacity(0);
        int fillHeight = (int) (height * (float) amount / capacity);

        if (fillHeight <= 0) return;

        // Рендер жидкости
        Fluid fluidType = fluid.getFluid();
        renderFluid(graphics, x, y, width, fillHeight, fluidType, amount, capacity);
    }

    /**
     * Рендер текстуры жидкости.
     */
    private void renderFluid(GuiGraphics graphics, int x, int y, int width, int fillHeight, 
                             Fluid fluid, int amount, int capacity) {
        // Получаем tint цвет из ModFluids
        int tintColor = ModFluids.getTintColor(getFluidName(fluid));
        
        // Получаем текстуру жидкости
        ResourceLocation texture = getFluidTexture(fluid);
        
        // Устанавливаем цвет
        float r = ((tintColor >> 16) & 0xFF) / 255.0f;
        float g = ((tintColor >> 8) & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;
        
        graphics.setColor(r, g, b, 1.0f);
        
        // Рендер текстуры жидкости (тайлами)
        int tiles = (fillHeight + 16) / 16;
        for (int tile = 0; tile < tiles; tile++) {
            int tileHeight = Math.min(16, fillHeight - tile * 16);
            int tileY = y - tile * 16 - tileHeight;
            
            graphics.blit(texture, x, tileY, width, tileHeight, 
                    0, 16 - tileHeight, 16, tileHeight, 16, 16);
        }
        
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Получение текстуры для жидкости.
     */
    private ResourceLocation getFluidTexture(Fluid fluid) {
        String fluidName = getFluidName(fluid);
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "block/fluids/" + fluidName);
    }

    /**
     * Получение имени жидкости для текстуры.
     */
    private String getFluidName(Fluid fluid) {
        // Маппинг на имена из ModFluids
        String path = fluid.toString().toLowerCase();
        if (path.contains("crude_oil") || path.contains("oil")) return "crude_oil";
        if (path.contains("gas")) return "gas";
        if (path.contains("fracksol")) return "fracksol";
        return "none";
    }

    /**
     * Рендер info panel.
     */
    private void renderInfoPanel(GuiGraphics graphics) {
        // Info panel icon
        graphics.blit(GUI_TEXTURE, 
                leftPos + INFO_X, topPos + INFO_Y, 
                INFO_UV_X, INFO_UV_Y, INFO_SIZE, INFO_SIZE);
    }

    //=====================================================================================//
    // TOOLTIPS
    //=====================================================================================//

    /**
     * Tooltip для танка жидкости.
     */
    private void renderTankTooltip(GuiGraphics graphics, int mouseX, int mouseY, 
                                   int tankX, int tankY, int tankWidth, int tankHeight,
                                   IFluidHandler tank, String fluidNameKey) {
        if (isMouseOver(mouseX, mouseY, tankX, tankY, tankWidth, tankHeight)) {
            FluidStack fluid = tank.getFluidInTank(0);
            int amount = fluid.getAmount();
            int capacity = tank.getTankCapacity(0);
            
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(fluidNameKey));
            tooltip.add(Component.literal(String.format("%,d / %,d mB", amount, capacity)));
            
            // Используем renderComponentTooltip для List<Component>
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Tooltip для индикатора энергии.
     */
    private void renderEnergyTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.blockEntity == null) return;
        
        if (isMouseOver(mouseX, mouseY, 
                leftPos + ENERGY_X, topPos + ENERGY_Y, 
                ENERGY_WIDTH, ENERGY_HEIGHT)) {
            
            long energy = blockEntity.getEnergyStored();
            long maxEnergy = blockEntity.getMaxEnergyStored();
            long delta = blockEntity.getEnergyDelta();
            
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("gui.hbm_m.energy"));
            tooltip.add(Component.literal(String.format("%,d / %,d HE", energy, maxEnergy)));
            
            if (delta != 0) {
                String deltaStr = delta > 0 ? "+" + delta : String.valueOf(delta);
                tooltip.add(Component.literal(deltaStr + " HE/t"));
            }
            
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Tooltip для info panel.
     */
    private void renderInfoPanelTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY, 
                leftPos + INFO_X, topPos + INFO_Y, INFO_SIZE, INFO_SIZE)) {
            
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("desc.gui.upgrade"));
            tooltip.add(Component.translatable("desc.gui.upgrade.speed"));
            tooltip.add(Component.translatable("desc.gui.upgrade.power"));
            tooltip.add(Component.translatable("desc.gui.upgrade.afterburner"));
            
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    //=====================================================================================//
    // УТИЛИТЫ
    //=====================================================================================//

    /**
     * Проверка наведения мыши на область.
     */
    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}