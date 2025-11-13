package com.hbm_m.client.overlay;

import com.hbm_m.network.DetonateAllPacket;
import com.hbm_m.network.ModNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import com.hbm_m.item.MultiDetonatorItem;
import com.hbm_m.item.MultiDetonatorItem.PointData;
import net.minecraft.ChatFormatting;

/**
 * GUI для мульти-детонатора
 * Показывает 6 кнопок для выбора точки, координаты и поле для имени
 * При нажатии "Detonate All" активирует все точки и выводит отчет
 */
public class MultiDetonatorScreen extends Screen {

    private static final int POINTS_COUNT = 4;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COORDS_TEXT_WIDTH = 100;
    private static final int NAME_INPUT_WIDTH = 120;
    private static final int NAME_INPUT_HEIGHT = 18;
    private static final int SPACING = 10;

    private ItemStack detonatorStack;
    private MultiDetonatorItem detonatorItem;
    private int selectedPoint = 0;

    private EditBox[] nameInputs = new EditBox[POINTS_COUNT];
    private Button[] pointButtons = new Button[POINTS_COUNT];
    private Button detonateAllButton;

    public MultiDetonatorScreen(ItemStack stack) {
        super(Component.literal("Multi-Detonator"));
        this.detonatorStack = stack;
        this.detonatorItem = (MultiDetonatorItem) stack.getItem();
        this.selectedPoint = detonatorItem.getActivePoint(stack);
    }

    @Override
    protected void init() {
        super.init();

        this.clearWidgets();

        int centerX = this.width / 2;
        int startY = 30;
        int yOffset = 0;

        // Создаем кнопки для каждой точки
        for (int i = 0; i < POINTS_COUNT; i++) {
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
            if (pointData != null) {
                nameInput.setValue(pointData.name);
            } else {
                nameInput.setValue("Point " + (i + 1));
            }

            nameInput.setMaxLength(16);

            final int finalI = i;
            nameInput.setResponder(name -> {
                if (!name.isEmpty()) {
                    detonatorItem.setPointName(detonatorStack, finalI, name);
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
                .pos(centerX - 55, this.height - 60)
                .size(100, 20)
                .build();

        this.addRenderableWidget(detonateAllButton);


    }

    private void selectPoint(int pointIndex) {
        selectedPoint = pointIndex;
        detonatorItem.setActivePoint(detonatorStack, pointIndex);
        this.init();
    }

    private void clearPoint(int pointIndex) {
        detonatorItem.clearPoint(detonatorStack, pointIndex);
        this.init();
    }

    /**
     * Детонировать все точки и вывести отчет в чат
     */
    /**
     * Детонировать все точки и вывести отчет в чат
     */
    private void detonateAllPoints() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack stack = detonatorStack;
        if (stack.isEmpty() || !(stack.getItem() instanceof MultiDetonatorItem)) return;

        // Отправка пакета на сервер (пример)
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

        guiGraphics.drawString(this.font, "Multi-Detonator", centerX - 57, 10, 0xFFFFFF, false);

        for (int i = 0; i < POINTS_COUNT; i++) {
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