package com.hbm_m.client.overlay;

// Используем импорты из твоих файлов
import com.hbm_m.lib.RefStrings; // Убедись, что RefStrings.MODID существует
import com.hbm_m.menu.MachineBatteryMenu;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.UpdateBatteryC2SPacket;
import com.hbm_m.util.EnergyFormatter;
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

/**
 * [ФИКС] GUI для энергохранилища.
 * Объединяет старый дизайн (тултипы, координаты)
 * с новым бэкендом (ContainerData из MachineBatteryMenu).
 */
public class GUIMachineBattery extends AbstractContainerScreen<MachineBatteryMenu> {

    // Используем современный .fromNamespaceAndPath()
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_battery.png");

    public GUIMachineBattery(MachineBatteryMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);

        // Размеры из старого GUI
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Убираем рендеринг стандартного заголовка
        this.titleLabelX = -9999;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    // --- ОСНОВНОЙ РЕНДЕР ---

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // Используем renderBackground() здесь, как в новом GUI
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = this.leftPos;
        int y = this.topPos;

        pGuiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Вызываем методы рендера из старого GUI
        renderEnergyBar(pGuiGraphics, x, y);
        renderButtons(pGuiGraphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        // --- Динамический заголовок из старого GUI ---

        // Геттеры menu.getEnergy() и т.д. совпа
        String formattedEnergy = EnergyFormatter.formatFE(menu.getEnergy()); // Используем getEnergy() из нового меню
        String titleText = this.title.getString() + " (" + formattedEnergy + ")";

        pGuiGraphics.drawString(this.font, titleText, (this.imageWidth - this.font.width(titleText)) / 2, 6, 4210752, false);

        // --- Стандартная надпись "Инвентарь" ---
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    // --- РЕНДЕР КОМПОНЕНТОВ (из GUIMachineBattery old.java) ---

    private void renderEnergyBar(GuiGraphics graphics, int x, int y) {
        // Используем getEnergy() и getMaxEnergy() из нового меню
        long energy = menu.getEnergy();
        long maxEnergy = menu.getMaxEnergy();

        if (energy > 0 && maxEnergy > 0) { // Добавил maxEnergy > 0 для защиты от деления на ноль
            int totalHeight = 52;
            int barHeight = (int) (totalHeight * ((double)energy / maxEnergy)); // Используем double для точности
            if (barHeight > totalHeight) barHeight = totalHeight;

            // Координаты (x, y, u, v, w, h) из старого GUI
            graphics.blit(TEXTURE, x + 62, y + 17 + (totalHeight - barHeight), 176, totalHeight - barHeight, 52, barHeight);
        }
    }

    // [🔥 ИЗМЕНЕННЫЙ ХЕЛПЕР 🔥]
    private int getVForMode(int mode) {
        return switch (mode) {
            case 0 -> 70;  // BOTH (Теперь V=70)
            case 1 -> 52;  // INPUT (Теперь V=52)
            case 2 -> 88;  // OUTPUT
            case 3 -> 106; // DISABLED
            default -> 70; // По умолчанию BOTH
        };
    }

    // [🔥 ИЗМЕНЕННЫЙ МЕТОД 🔥]
    private void renderButtons(GuiGraphics graphics, int x, int y) {
        // Верхняя кнопка (Нет сигнала)
        int modeNoSignal = menu.getModeOnNoSignal();
        int vOnNoSignal = getVForMode(modeNoSignal); // <-- Используем хелпер
        graphics.blit(TEXTURE, x + 133, y + 16, 176, vOnNoSignal, 18, 18);

        // Нижняя кнопка (Есть сигнал)
        int modeSignal = menu.getModeOnSignal();
        int vOnSignal = getVForMode(modeSignal); // <-- Используем хелпер
        graphics.blit(TEXTURE, x + 133, y + 52, 176, vOnSignal, 18, 18);

        // Кнопка приоритета
        int priorityOrdinal = menu.getPriorityOrdinal();
        int priorityV = 52 + priorityOrdinal * 16;
        graphics.blit(TEXTURE, x + 152, y + 35, 194, priorityV, 16, 16);
    }

    // --- ТУЛТИПЫ (из GUIMachineBattery old.java) ---

    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        super.renderTooltip(pGuiGraphics, pMouseX, pMouseY);

        // Тултип для энергии (полностью из старого GUI)
        if (isMouseOver(pMouseX, pMouseY, 62, 17, 52, 52)) {
            List<Component> tooltip = new ArrayList<>();

            long energy = menu.getEnergy();
            long maxEnergy = menu.getMaxEnergy();
            long delta = menu.getEnergyDelta();

            String energyStr = EnergyFormatter.format(energy);
            String maxEnergyStr = EnergyFormatter.format(maxEnergy);
            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE"));

            String deltaText = (delta >= 0 ? "+" : "") + EnergyFormatter.formatRate(delta);
            ChatFormatting deltaColor = delta > 0 ? ChatFormatting.GREEN : (delta < 0 ? ChatFormatting.RED : ChatFormatting.YELLOW);
            tooltip.add(Component.literal(deltaText).withStyle(deltaColor));

            // Расчет дельты в секунду (HE/t * 20)
            long deltaPerSecond = delta * 20;
            String deltaPerSecondText = (deltaPerSecond >= 0 ? "+" : "") + EnergyFormatter.formatWithUnit(deltaPerSecond, "HE/s");
            tooltip.add(Component.literal(deltaPerSecondText).withStyle(deltaColor));

            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pMouseX, pMouseY);
        }

        // Тултип для Priority (полностью из старого GUI)
        if (isMouseOver(pMouseX, pMouseY, 152, 35, 16, 16)) {
            List<Component> tooltip = new ArrayList<>();
            // Мы убрали VERY_LOW/VERY_HIGH, поэтому ординалы 0, 1, 2 (LOW, NORMAL, HIGH)
            int priorityOrdinal = menu.getPriorityOrdinal();
            String priorityKey = "gui.hbm_m.battery.priority." + priorityOrdinal;

            tooltip.add(Component.translatable(priorityKey));
            tooltip.add(Component.translatable("gui.hbm_m.battery.priority.recommended").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(priorityKey + ".desc").withStyle(ChatFormatting.GRAY));

            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pMouseX, pMouseY);
        }

        // Тултип для кнопки БЕЗ сигнала (из старого GUI)
        if (isMouseOver(pMouseX, pMouseY, 133, 16, 18, 18)) {
            renderRedstoneTooltip(pGuiGraphics, pMouseX, pMouseY, menu.getModeOnNoSignal(), "no_signal");
        }

        // Тултип для кнопки С сигналом (из старого GUI)
        if (isMouseOver(pMouseX, pMouseY, 133, 52, 18, 18)) {
            renderRedstoneTooltip(pGuiGraphics, pMouseX, pMouseY, menu.getModeOnSignal(), "with_signal");
        }
    }

    // --- ОБРАБОТКА КЛИКОВ (из GUIMachineBattery old.java) ---

    // Хелпер для тултипов редстоуна
    private void renderRedstoneTooltip(GuiGraphics graphics, int mouseX, int mouseY, int mode, String conditionKey) {
        List<Component> tooltip = new ArrayList<>();

        tooltip.add(Component.translatable("gui.hbm_m.battery.condition." + conditionKey));

        String modeKey = switch (mode) {
            case 0 -> "both";
            case 1 -> "input";
            case 2 -> "output";
            case 3 -> "locked";
            default -> "both";
        };

        String titleKey = "gui.hbm_m.battery.mode." + modeKey;
        String descKey = "gui.hbm_m.battery.mode." + modeKey + ".desc";

        tooltip.add(Component.translatable(titleKey).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));

        graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0) { // Только левая кнопка мыши
            // Кнопка "Без сигнала"
            if (isMouseOver(pMouseX, pMouseY, 133, 16, 18, 18)) {
                playSound();
                ModPacketHandler.INSTANCE.sendToServer(new UpdateBatteryC2SPacket(this.menu.blockEntity.getBlockPos(), 0));
                return true;
            }
            // Кнопка "С сигналом"
            if (isMouseOver(pMouseX, pMouseY, 133, 52, 18, 18)) {
                playSound();
                ModPacketHandler.INSTANCE.sendToServer(new UpdateBatteryC2SPacket(this.menu.blockEntity.getBlockPos(), 1));
                return true;
            }
            // Кнопка "Приоритет"
            if (isMouseOver(pMouseX, pMouseY, 152, 35, 16, 16)) {
                playSound();
                ModPacketHandler.INSTANCE.sendToServer(new UpdateBatteryC2SPacket(this.menu.blockEntity.getBlockPos(), 2));
                return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    // --- ХЕЛПЕРЫ (из GUIMachineBattery old.java) ---

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int sizeX, int sizeY) {
        return (mouseX >= this.leftPos + x && mouseX <= this.leftPos + x + sizeX &&
                mouseY >= this.topPos + y && mouseY <= this.topPos + y + sizeY);
    }

    private void playSound() {
        if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}