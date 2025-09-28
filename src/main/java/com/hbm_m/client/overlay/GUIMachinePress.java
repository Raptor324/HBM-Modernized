package com.hbm_m.client.overlay;

// GUI для пресса. Отвечает за отрисовку прогресса прессования, состояния нагрева,
// индикатора топлива и подсказок. Основан на AbstractContainerScreen и использует текстуры из ресурсов мода.
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachinePressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachinePress extends AbstractContainerScreen<MachinePressMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/press/press_gui.png");
    private static final ResourceLocation PRESS_ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/press/press_arrow.png");
    private static final ResourceLocation LIGHT_ON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "textures/gui/press/light_on.png");

    // Текстуры для 13 состояний индикатора нагрева (0-12)
    private static final ResourceLocation[] HEAT_INDICATOR_TEXTURES = new ResourceLocation[13];

    static {
        for (int i = 0; i < 13; i++) {
            HEAT_INDICATOR_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID,
                    "textures/gui/press/heat_indicator_" + i + ".png");
        }
    }

    public GUIMachinePress(MachinePressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 6;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Основная текстура GUI
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderPressArrow(guiGraphics, x, y);
        renderHeatIndicator(guiGraphics, x, y);
        renderLight(guiGraphics, x, y);
    }

    private void renderPressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCrafting() && menu.isHeated()) {
            int pressPos = menu.getPressPosition(); // 0-20
            boolean isPressingDown = menu.isPressingDown();

            int arrowX = x + 80;
            int arrowY = y + 36;

            // Новая логика: рисуем стрелку с высотой в зависимости от позиции
            // pressPos 0 = стрелка наверху (не видна или минимальная)
            // pressPos 20 = стрелка максимально вниз

            if (pressPos > 0) {
                // Высота стрелки зависит от позиции (максимум 16 пикселей)
                int arrowHeight = Math.min(16, (pressPos * 16) / 20);

                // Рисуем только видимую часть стрелки сверху вниз
                guiGraphics.blit(PRESS_ARROW_TEXTURE,
                        arrowX, arrowY,  // позиция на экране
                        0, 0,           // позиция в текстуре (начинаем сверху)
                        16, arrowHeight, // размер для отрисовки
                        16, 16);        // полный размер текстуры
            }
        }
    }

    private void renderHeatIndicator(GuiGraphics guiGraphics, int x, int y) {
        // Получаем состояние нагрева
        int heatState = menu.getHeatState();

        // Убеждаемся что значение в допустимых пределах
        heatState = Math.max(0, Math.min(12, heatState));

        // Позиция индикатора
        int indicatorX = x + 25;
        int indicatorY = y + 16;

        // Рисуем текстуру
        guiGraphics.blit(HEAT_INDICATOR_TEXTURES[heatState],
                indicatorX, indicatorY, 0, 0, 18, 18, 18, 18);
    }

    private void renderLight(GuiGraphics guiGraphics, int x, int y) {
        // Рендерим индикатор топлива (как огонек в печке)
        if (menu.isBurning()) {
            int fuelTime = menu.getFuelTime();
            int maxFuelTime = menu.getMaxFuelTime();

            if (maxFuelTime > 0) {
                int lightX = x + 27;
                int lightY = y + 36;

                // Вычисляем высоту огонька в зависимости от оставшегося топлива
                int maxFlameHeight = 13; // Максимальная высота огонька
                int currentFlameHeight = (int) ((double) fuelTime / maxFuelTime * maxFlameHeight);

                if (currentFlameHeight > 0) {
                    // Рисуем огонек снизу вверх (как в ванильной печке)
                    int flameY = lightY + (maxFlameHeight - currentFlameHeight);

                    guiGraphics.blit(LIGHT_ON_TEXTURE,
                            lightX, flameY,              // позиция на экране
                            0, maxFlameHeight - currentFlameHeight, // откуда в текстуре начинаем
                            14, currentFlameHeight,      // размер для отрисовки
                            14, maxFlameHeight);         // полный размер текстуры
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Тултип для индикатора нагрева
        if (isHovering(25, 16, 18, 18, mouseX, mouseY)) {
            int heatState = menu.getHeatState();
            int heatLevel = menu.getHeatLevel();
            int maxHeatLevel = menu.getMaxHeatLevel();

            int heatPercent = maxHeatLevel != 0 ? (heatLevel * 100) / maxHeatLevel : 0;
            String status = getHeatStatusText(heatState);

            guiGraphics.renderTooltip(this.font,
                    Component.literal("Heat: " + heatPercent + "% - " + status),
                    mouseX, mouseY);
        }

        // Тултип для индикатора топлива
        if (isHovering(27, 36, 14, 13, mouseX, mouseY)) {
            int fuelTime = menu.getFuelTime();
            int maxFuelTime = menu.getMaxFuelTime();

            if (maxFuelTime > 0) {
                int fuelPercent = (fuelTime * 100) / maxFuelTime;
                int fuelTimeSeconds = fuelTime / 20; // конвертируем тики в секунды

                guiGraphics.renderTooltip(this.font,
                        Component.literal("Fuel: " + fuelPercent + "% (" + fuelTimeSeconds + "s remaining)"),
                        mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font,
                        Component.literal("No fuel"),
                        mouseX, mouseY);
            }
        }
    }

    private String getHeatStatusText(int heatState) {
        return switch (heatState) {
            case 0 -> "Cold";
            case 1 -> "Slightly warm";
            case 2 -> "Warm";
            case 3 -> "Almost ready";
            case 4 -> "Working temperature";
            case 5 -> "Stable heat";
            case 6 -> "Good heat";
            case 7 -> "High temperature";
            case 8 -> "Very hot";
            case 9 -> "Excellent heat";
            case 10 -> "High efficiency";
            case 11 -> "Maximum performance";
            case 12 -> "Peak efficiency";
            default -> "Unknown";
        };
    }
}