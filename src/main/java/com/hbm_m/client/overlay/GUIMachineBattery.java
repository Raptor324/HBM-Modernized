package com.hbm_m.client.overlay;

// GUI для энергохранилища. Показывает уровень энергии, режимы работы по красному камню и приоритет.
// Основан на AbstractContainerScreen и использует текстуры из ресурсов мода.
import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.MachineBatteryMenu;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.UpdateBatteryC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GUIMachineBattery extends AbstractContainerScreen<MachineBatteryMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_battery.png");

    public GUIMachineBattery(MachineBatteryMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        
        // Устанавливаем размеры GUI из старого кода
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Убираем рендеринг стандартного заголовка, так как у нас будет свой, динамический
        this.titleLabelX = -9999;
        this.inventoryLabelY = this.imageHeight - 94;
    }
    
    // --- ОСНОВНОЙ РЕНДЕР ---

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // Просто вызываем super.render() и renderTooltip().
        // super.render() сам позаботится о renderBackground() и renderBg().
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        // Убираем renderBackground() отсюда, оставляем только логику отрисовки вашей текстуры
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = this.leftPos;
        int y = this.topPos;

        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        renderEnergyBar(pGuiGraphics, x, y);
        renderButtons(pGuiGraphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        // --- Воссоздаем динамический заголовок из оригинала ---
        String titleText = this.title.getString() + " (" + menu.getEnergy() + " FE)";
        pGuiGraphics.drawString(this.font, titleText, (this.imageWidth - this.font.width(titleText)) / 2, 6, 4210752, false);
        
        // --- Стандартная надпись "Инвентарь" ---
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }
    
    // --- РЕНДЕР КОМПОНЕНТОВ ---

    private void renderEnergyBar(GuiGraphics graphics, int x, int y) {
        if (menu.getEnergy() > 0) {
            int totalHeight = 52;
            int barHeight = (int) (totalHeight * ((float)menu.getEnergy() / menu.getMaxEnergy()));
            // Координата Y в старом коде была (guiTop + 69 - i), адаптируем
            graphics.blit(TEXTURE, x + 62, y + 17 + (totalHeight - barHeight), 176, totalHeight - barHeight, 52, barHeight);
        }
    }
    
    // Хелпер для получения V-координаты иконки по режиму
    private int getVForMode(int mode) {
        return switch (mode) {
            case 0 -> 70;  // Игнор (Приём+Передача)
            case 1 -> 52;  // Пыль (Только приём)
            case 2 -> 88;  // Факел (Только передача)
            case 3 -> 106; // Замок (Заблокировано)
            default -> 52;
        };
    }

    private void renderButtons(GuiGraphics graphics, int x, int y) {
        // Верхняя кнопка (Нет сигнала)
        int vOnNoSignal = getVForMode(menu.getModeOnNoSignal());
        graphics.blit(TEXTURE, x + 133, y + 16, 176, vOnNoSignal, 18, 18);

        // Нижняя кнопка (Есть сигнал)
        int vOnSignal = getVForMode(menu.getModeOnSignal());
        graphics.blit(TEXTURE, x + 133, y + 52, 176, vOnSignal, 18, 18);

        // Кнопка приоритета
        int priorityV = 52 + menu.getPriorityOrdinal() * 16;
        graphics.blit(TEXTURE, x + 152, y + 35, 194, priorityV, 16, 16);
    }

    // --- ТУЛТИПЫ ---

    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        super.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
        
        // Тултип для энергии
        if (isMouseOver(pMouseX, pMouseY, 62, 17, 52, 52)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(menu.getEnergy() + " / " + menu.getMaxEnergy() + " FE"));
            
            String deltaText = (menu.getEnergyDelta() >= 0 ? "+" : "") + menu.getEnergyDelta() + " FE/t";
            ChatFormatting deltaColor = menu.getEnergyDelta() > 0 ? ChatFormatting.GREEN : (menu.getEnergyDelta() < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW);
            tooltip.add(Component.literal(deltaText).withStyle(deltaColor));
            
            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pMouseX, pMouseY);
        }
        
        // Тултип для Priority
        if (isMouseOver(pMouseX, pMouseY, 152, 35, 16, 16)) {
             List<Component> tooltip = new ArrayList<>();
             String priorityKey = "gui.hbm_m.battery.priority." + menu.getPriorityOrdinal();
             tooltip.add(Component.translatable(priorityKey));
             tooltip.add(Component.translatable("gui.hbm_m.battery.priority.recommended").withStyle(ChatFormatting.DARK_GRAY));
             tooltip.add(Component.translatable(priorityKey + ".desc").withStyle(ChatFormatting.GRAY));
             
             pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pMouseX, pMouseY);
        }

        if (isMouseOver(pMouseX, pMouseY, 133, 16, 18, 18)) {
            renderRedstoneTooltip(pGuiGraphics, pMouseX, pMouseY, menu.getModeOnNoSignal(), "no_signal");
        }
        
        // Тултип для нижней кнопки (Есть сигнал)
        if (isMouseOver(pMouseX, pMouseY, 133, 52, 18, 18)) {
            renderRedstoneTooltip(pGuiGraphics, pMouseX, pMouseY, menu.getModeOnSignal(), "with_signal");
        }
    }
    
    // --- ОБРАБОТКА КЛИКОВ ---

    private void renderRedstoneTooltip(GuiGraphics graphics, int mouseX, int mouseY, int mode, String conditionKey) {
        List<Component> tooltip = new ArrayList<>();
        
        // 1. Добавляем заголовок, описывающий УСЛОВИЕ
        tooltip.add(Component.translatable("gui.hbm_m.battery.condition." + conditionKey));
        
        String modeKey;
        switch (mode) {
            case 0 -> modeKey = "both";
            case 1 -> modeKey = "input";
            case 2 -> modeKey = "output";
            case 3 -> modeKey = "locked";
            default -> modeKey = "both";
        }

        // 2. Добавляем название и описание РЕЖИМА
        String titleKey = "gui.hbm_m.battery.mode." + modeKey;
        String descKey = "gui.hbm_m.battery.mode." + modeKey + ".desc";
        
        tooltip.add(Component.translatable(titleKey).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));

        graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
    }
    
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) { // Только левая кнопка мыши
            if (isMouseOver(pMouseX, pMouseY, 133, 16, 18, 18)) { // Кнопка входа
                playSound();
                ModPacketHandler.INSTANCE.sendToServer(new UpdateBatteryC2SPacket(this.menu.blockEntity.getBlockPos(), 0));
                return true;
            }
            if (isMouseOver(pMouseX, pMouseY, 133, 52, 18, 18)) { // Кнопка выхода
                playSound();
                ModPacketHandler.INSTANCE.sendToServer(new UpdateBatteryC2SPacket(this.menu.blockEntity.getBlockPos(), 1));
                return true;
            }
            if (isMouseOver(pMouseX, pMouseY, 152, 35, 16, 16)) { // Кнопка приоритета
                playSound();
                ModPacketHandler.INSTANCE.sendToServer(new UpdateBatteryC2SPacket(this.menu.blockEntity.getBlockPos(), 2));
                return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    // --- ХЕЛПЕРЫ ---

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return (mouseX >= this.leftPos + x && mouseX <= this.leftPos + x + sizeX &&
                mouseY >= this.topPos + y && mouseY <= this.topPos + y + sizeY);
    }

    private void playSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}