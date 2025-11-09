package com.hbm_m.client.overlay;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachineBatteryMenu;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.UpdateBatteryC2SPacket;
import com.hbm_m.util.EnergyFormatter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI для энергохранилища MachineBattery.
 * Отображает энергию, режимы работы и приоритет.
 */
public class GUIMachineBattery extends AbstractContainerScreen<MachineBatteryMenu> {

    // ✅ ИСПРАВЛЕНО: Используем современный конструктор ResourceLocation
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/storage/gui_battery.png");

    public GUIMachineBattery(MachineBatteryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 166;
        this.imageWidth = 176;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = this.leftPos;
        int y = this.topPos;

        // Основной фон
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // ✅ Отрисовка шкалы энергии
        long energy = this.menu.getEnergy();
        long maxEnergy = this.menu.getMaxEnergy();

        if (energy > 0 && maxEnergy > 0) {
            int barHeight = (int)(52 * ((double)energy / maxEnergy));
            gui.blit(TEXTURE, x + 62, y + 69 - barHeight, 176, 52 - barHeight, 52, barHeight);
        }

        // ✅ Кнопка режима БЕЗ редстоуна
        int modeNoSignal = this.menu.getModeOnNoSignal();
        gui.blit(TEXTURE, x + 133, y + 16, 176, 52 + modeNoSignal * 18, 18, 18);

        // ✅ Кнопка режима С редстоуном
        int modeSignal = this.menu.getModeOnSignal();
        gui.blit(TEXTURE, x + 133, y + 52, 176, 52 + modeSignal * 18, 18, 18);

        // ✅ Кнопка приоритета (отображаем иконку в зависимости от уровня)
        int priorityOrdinal = this.menu.getPriorityOrdinal();
        if (priorityOrdinal > 0) { // Только если не VERY_LOW
            gui.blit(TEXTURE, x + 152, y + 35, 194, 52 + (priorityOrdinal - 1) * 16, 16, 16);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        // ✅ ИСПРАВЛЕНО: renderBackground() принимает только GuiGraphics
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);

        // ✅ Тултип для шкалы энергии
        if (isHovering(62, 17, 52, 52, mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();

            long energy = this.menu.getEnergy();
            long maxEnergy = this.menu.getMaxEnergy();
            int delta = this.menu.getEnergyDelta();

            // Форматированная энергия
            String energyStr = EnergyFormatter.formatFE(energy);
            String maxEnergyStr = EnergyFormatter.formatFE(maxEnergy);

            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr)
                    .withStyle(ChatFormatting.GREEN));

            // Дельта энергии (прирост/убыль)
            if (delta != 0) {
                String deltaStr = (delta > 0 ? "+" : "") + EnergyFormatter.formatRate(delta);
                ChatFormatting color = delta > 0 ? ChatFormatting.GREEN : ChatFormatting.RED;
                tooltip.add(Component.literal(deltaStr).withStyle(color));
            }

            gui.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }

        // ✅ Тултип для кнопки режима БЕЗ редстоуна
        if (isHovering(133, 16, 18, 18, mouseX, mouseY)) {
            int mode = this.menu.getModeOnNoSignal();
            String modeText = getModeText(mode);
            gui.renderTooltip(this.font,
                    Component.literal("No Signal: " + modeText).withStyle(ChatFormatting.YELLOW),
                    mouseX, mouseY);
        }

        // ✅ Тултип для кнопки режима С редстоуном
        if (isHovering(133, 52, 18, 18, mouseX, mouseY)) {
            int mode = this.menu.getModeOnSignal();
            String modeText = getModeText(mode);
            gui.renderTooltip(this.font,
                    Component.literal("With Signal: " + modeText).withStyle(ChatFormatting.YELLOW),
                    mouseX, mouseY);
        }

        // ✅ Тултип для кнопки приоритета
        if (isHovering(152, 35, 16, 16, mouseX, mouseY)) {
            int priorityOrdinal = this.menu.getPriorityOrdinal();
            String priorityText = getPriorityText(priorityOrdinal);
            gui.renderTooltip(this.font,
                    Component.literal("Priority: " + priorityText).withStyle(ChatFormatting.AQUA),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Левая кнопка мыши
            // ✅ Клик по кнопке режима БЕЗ редстоуна
            if (isHovering(133, 16, 18, 18, (int)mouseX, (int)mouseY)) {
                ModPacketHandler.INSTANCE.sendToServer(
                        new UpdateBatteryC2SPacket(menu.blockEntity.getBlockPos(), 0)
                );
                return true;
            }

            // ✅ Клик по кнопке режима С редстоуном
            if (isHovering(133, 52, 18, 18, (int)mouseX, (int)mouseY)) {
                ModPacketHandler.INSTANCE.sendToServer(
                        new UpdateBatteryC2SPacket(menu.blockEntity.getBlockPos(), 1)
                );
                return true;
            }

            // ✅ Клик по кнопке приоритета
            if (isHovering(152, 35, 16, 16, (int)mouseX, (int)mouseY)) {
                ModPacketHandler.INSTANCE.sendToServer(
                        new UpdateBatteryC2SPacket(menu.blockEntity.getBlockPos(), 2)
                );
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Возвращает текст режима работы
     * @param mode 0 = BOTH, 1 = INPUT, 2 = OUTPUT, 3 = DISABLED
     */
    private String getModeText(int mode) {
        return switch (mode) {
            case 0 -> "BOTH";
            case 1 -> "INPUT";
            case 2 -> "OUTPUT";
            case 3 -> "DISABLED";
            default -> "UNKNOWN";
        };
    }

    /**
     * Возвращает текст приоритета
     * @param ordinal 0 = VERY_LOW, 1 = LOW, 2 = NORMAL, 3 = HIGH, 4 = VERY_HIGH
     */
    private String getPriorityText(int ordinal) {
        return switch (ordinal) {
            case 0 -> "VERY LOW";
            case 1 -> "LOW";
            case 2 -> "NORMAL";
            case 3 -> "HIGH";
            case 4 -> "VERY HIGH";
            default -> "UNKNOWN";
        };
    }
}