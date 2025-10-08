package com.hbm_m.client.overlay;

// GUI для дровяной печи. Показывает время горения топлива, уровень энергии,
// индикатор пламени и подсказки. Основан на AbstractContainerScreen и использует текстуры из ресурсов мода.
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachineWoodBurnerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GUIMachineWoodBurner extends AbstractContainerScreen<MachineWoodBurnerMenu> {
    // Основная текстура GUI
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/wood_burner/wood_burner_gui.png");

    // Текстуры для шкал
    private static final ResourceLocation BURN_TIME_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/wood_burner/fuel_bar.png");

    private static final ResourceLocation ENERGY_BAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/wood_burner/energy_bar.png");

    public GUIMachineWoodBurner(MachineWoodBurnerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();

        this.topPos -= 20; // поднимаем на 20 пикселей
        this.leftPos = (this.width - this.imageWidth) / 2;

        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 104;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Рендерим основную текстуру GUI
        RenderSystem.setShaderTexture(0, TEXTURE);
        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Рендерим шкалы и пламя
        renderBurnTimeBar(pGuiGraphics, x, y);
        renderEnergyBar(pGuiGraphics, x, y);
    }

    private void renderBurnTimeBar(GuiGraphics graphics, int x, int y) {
        if (menu.getBurnTime() > 0) {
            int totalHeight = 52;
            int barHeight = menu.getBurnTimeScaled(totalHeight);

            // Переключаемся на текстуру шкалы времени горения
            RenderSystem.setShaderTexture(0, BURN_TIME_BAR_TEXTURE);

            // Рендерим заполненную часть шкалы времени горения
            // Теперь берем с координат (0,0) так как это отдельный файл
            int startY = y + 18 + (totalHeight - barHeight);
            int textureStartY = totalHeight - barHeight;

            graphics.blit(BURN_TIME_BAR_TEXTURE, x + 17, startY, 0, textureStartY, 4, barHeight);
        }
    }

    private void renderEnergyBar(GuiGraphics graphics, int x, int y) {
        if (menu.getEnergy() > 0) {
            int totalHeight = 34;
            int barHeight = menu.getEnergyScaled(totalHeight);

            // Переключаемся на текстуру шкалы энергии
            RenderSystem.setShaderTexture(0, ENERGY_BAR_TEXTURE);

            // РЕНДЕРИМ СНИЗУ ВВЕРХ - берём текстуру с НУЛЯ (самый низ)
            // Позиция на экране: рисуем внизу шкалы и поднимаемся вверх
            int startY = y + 52 - barHeight;  // 52 = 18 + 34 (низ шкалы минус высота заполнения)

            graphics.blit(
                    ENERGY_BAR_TEXTURE,  // текстура
                    x + 143,              // X на экране
                    startY,               // Y на экране (верх заполненной части)
                    0,                    // X в текстуре
                    0,                    // Y в текстуре - ВСЕГДА берём с нуля (снизу)!
                    16,                   // ширина
                    barHeight             // высота (сколько взять от низа текстуры)
            );
        }
    }



    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        // Тултип для шкалы времени горения
        if (isMouseOver(pX, pY, 17, 17, 4, 52)) {
            List<Component> tooltip = new ArrayList<>();
            if (menu.isLit()) {
                int burnTimeSeconds = menu.getBurnTime() / 20; // конвертируем тики в секунды
                tooltip.add(Component.literal("Burn Time: " + burnTimeSeconds + "s")
                        .withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.literal("Not burning").withStyle(ChatFormatting.GRAY));
            }
            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
        }

        // Тултип для шкалы энергии (обновлены координаты под размер 16x34)
        if (isMouseOver(pX, pY, 143, 18, 16, 34)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(String.format("%,d / %,d FE", menu.getEnergy(), menu.getMaxEnergy()))
                    .withStyle(ChatFormatting.GREEN));

            if (menu.isLit()) {
                tooltip.add(Component.literal("+50 FE/t").withStyle(ChatFormatting.YELLOW));
            } else {
                tooltip.add(Component.literal("Not generating").withStyle(ChatFormatting.GRAY));
            }

            // Показываем процент заполнения
            int percentage = (int) ((float) menu.getEnergy() / menu.getMaxEnergy() * 100);
            tooltip.add(Component.literal(percentage + "%").withStyle(ChatFormatting.AQUA));

            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
        }

        // Тултип для области пламени
        if (isMouseOver(pX, pY, 56, 36, 14, 14)) {
            if (menu.isLit()) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("Burning fuel").withStyle(ChatFormatting.RED));
                pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY);
            }
        }
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return (mouseX >= this.leftPos + x && mouseX <= this.leftPos + x + sizeX &&
                mouseY >= this.topPos + y && mouseY <= this.topPos + y + sizeY);
    }
}