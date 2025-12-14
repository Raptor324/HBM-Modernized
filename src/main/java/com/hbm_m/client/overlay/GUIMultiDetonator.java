package com.hbm_m.client.overlay;

import com.hbm_m.network.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem.PointData;
import net.minecraft.ChatFormatting;

/**
 * GUI для мульти-детонатора
 * Показывает 4 кнопки для выбора точки, координаты и поле для имени
 * При нажатии "Detonate All" активирует все точки и выводит отчет
 */
public class GUIMultiDetonator extends Screen {

    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COORDS_TEXT_WIDTH = 100;
    private static final int NAME_INPUT_WIDTH = 120;
    private static final int NAME_INPUT_HEIGHT = 18;
    private static final int SPACING = 10;

    private ItemStack detonatorStack;
    private MultiDetonatorItem detonatorItem;
    private int selectedPoint = 0; // Текущий выбор в GUI
    private EditBox[] nameInputs;
    private Button[] pointButtons;
    private Button detonateAllButton;
    private int pointsCount;

    public GUIMultiDetonator(ItemStack stack) {
        super(Component.literal("Multi-Detonator"));
        this.detonatorStack = stack;
        this.detonatorItem = (MultiDetonatorItem) stack.getItem();

        // ВАЖНО: Загружаем активную точку из NBT - это гарантирует синхронизацию
        this.selectedPoint = detonatorItem.getActivePoint(stack);
        this.pointsCount = detonatorItem.getMaxPoints();
        this.nameInputs = new EditBox[pointsCount];
        this.pointButtons = new Button[pointsCount];
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int centerX = this.width / 2;
        int startY = 30;
        int yOffset = 0;

        // Создаем кнопки для каждой точки
        for (int i = 0; i < pointsCount; i++) {
            final int pointIndex = i;
            int buttonY = startY + yOffset;

            // Кнопка выбора точки
            Button pointButton = Button.builder(
                            Component.literal("Point " + (i + 1)),
                            btn -> selectPoint(pointIndex)
                    )
                    .pos(centerX - BUTTON_WIDTH / 2, buttonY)
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();

            // Подсвечиваем активную точку
            if (i == selectedPoint) {
                pointButton.active = false;
            }

            this.addRenderableWidget(pointButton);
            pointButtons[i] = pointButton;

            // Поле ввода для имени
            EditBox nameInput = new EditBox(this.font, centerX - NAME_INPUT_WIDTH / 2,
                    buttonY + BUTTON_HEIGHT + 5, NAME_INPUT_WIDTH, NAME_INPUT_HEIGHT,
                    Component.literal("Name"));

            PointData pointData = detonatorItem.getPointData(detonatorStack, i);
            String currentName = "";
            int currentX = 0, currentY = 0, currentZ = 0;
            boolean hasTarget = false;

            if (pointData != null && !pointData.name.isEmpty()) {
                currentName = pointData.name;
                currentX = pointData.x;
                currentY = pointData.y;
                currentZ = pointData.z;
                hasTarget = pointData.hasTarget;
            } else {
                currentName = "Point " + (i + 1);
            }

            nameInput.setValue(currentName);
            nameInput.setMaxLength(16);

            final int finalI = i;
            final String finalCurrentName = currentName;
            final int finalX = currentX;
            final int finalY = currentY;
            final int finalZ = currentZ;
            final boolean finalHasTarget = hasTarget;

// ⭐ КРИТИЧНО: На каждое изменение имени отправляем ПОЛНЫЕ данные на сервер
            nameInput.setResponder(name -> {
                if (!name.isEmpty()) {
                    // Сохраняем локально на клиенте
                    detonatorItem.setPointName(detonatorStack, finalI, name);

                    // ⭐ ГЛАВНОЕ: Отправляем пакет на сервер с ПОЛНЫМИ данными
                    ModNetwork.CHANNEL.sendToServer(
                            new SyncPointPacket(finalI, name, finalX, finalY, finalZ, finalHasTarget)
                    );
                }
            });

            this.addRenderableWidget(nameInput);
            nameInputs[i] = nameInput;

            // Кнопка очистки точки
            Button clearButton = Button.builder(
                            Component.literal("Clear"),
                            btn -> clearPoint(pointIndex)
                    )
                    .pos(centerX + NAME_INPUT_WIDTH / 2 + 5, buttonY + BUTTON_HEIGHT + 5)
                    .size(50, NAME_INPUT_HEIGHT)
                    .build();

            this.addRenderableWidget(clearButton);

            yOffset += BUTTON_HEIGHT + NAME_INPUT_HEIGHT + SPACING;
        }

        // Кнопка "Detonate All" (детонировать все точки)
        detonateAllButton = Button.builder(
                        Component.literal("Detonate All"),
                        btn -> detonateAllPoints()
                )
                .pos(centerX - 55, this.height - 51)
                .size(110, 20)
                .build();

        this.addRenderableWidget(detonateAllButton);
    }

    private void selectPoint(int pointIndex) {
        selectedPoint = pointIndex;

        // Сохраняем на клиенте
        detonatorItem.setActivePoint(detonatorStack, pointIndex);

        // ⭐ КРИТИЧНО: Отправляем пакет на сервер для синхронизации
        ModNetwork.CHANNEL.sendToServer(new SetActivePointPacket(pointIndex));

        this.init();
    }

    private void clearPoint(int pointIndex) {
        // Очищаем на клиенте (только координаты, имя сохраняется)
        detonatorItem.clearPoint(detonatorStack, pointIndex);

        // ⭐ КРИТИЧНО: Отправляем пакет на сервер для синхронизации
        ModNetwork.CHANNEL.sendToServer(new ClearPointPacket(pointIndex));

        // Обновляем GUI без изменения текущего состояния
        this.init();
    }

    /**
     * Детонировать все точки и вывести отчет в чат
     * ✓ Исправлено: Не вызывает ModNetwork.registerChannels() на клиенте
     * ✓ Исправлено: Проверяет что канал инициализирован перед отправкой
     */
    private void detonateAllPoints() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack stack = detonatorStack;
        if (stack.isEmpty() || !(stack.getItem() instanceof MultiDetonatorItem)) return;

        // Проверяем что канал инициализирован
        if (ModNetwork.CHANNEL == null) {
            player.displayClientMessage(
                    Component.literal("Сетевой канал не инициализирован!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        // Отправка пакета на сервер
        ModNetwork.CHANNEL.sendToServer(new DetonateAllPacket());
        this.minecraft.setScreen(null); // закрыть GUI
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int startY = 30;
        int yOffset = 0;

        guiGraphics.drawString(this.font, "Multi-Detonator", centerX - 37, 10, 0xFFFFFF, false);

        for (int i = 0; i < pointsCount; i++) {
            int textY = startY + yOffset + 2;

            PointData pointData = detonatorItem.getPointData(detonatorStack, i);

            String coordText = "----";
            int textColor = 0xFF0000; // Красный - нет координат

            if (pointData != null && pointData.hasTarget) {
                coordText = String.format("X:%d Y:%d Z:%d", pointData.x, pointData.y, pointData.z);
                textColor = 0x00AA00; // Зеленый - координаты установлены
            }

            guiGraphics.drawString(this.font, coordText, centerX + BUTTON_WIDTH / 2 + 20, textY, textColor, false);

            yOffset += BUTTON_HEIGHT + NAME_INPUT_HEIGHT + SPACING;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox input : nameInputs) {
            if (input.isFocused() && input.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (EditBox input : nameInputs) {
            if (input.isFocused() && input.charTyped(codePoint, modifiers)) {
                return true;
            }
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void tick() {
        super.tick();

        for (EditBox input : nameInputs) {
            input.tick();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}