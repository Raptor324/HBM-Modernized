package com.hbm_m.client.overlay;

import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.client.model.variant.*;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.network.ServerboundDoorModelPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * UI Меню для выбора модели и скина двери.
 * 
 * Макет:
 * ┌─────────────────────────────────────┐
 * │   Выбор модели двери                │
 * │   (large_vehicle_door)              │
 * ├─────────────────────────────────────┤
 * │   Модель:                           │
 * │   [► Старая модель]                 │
 * │   [  Новая модель]                  │
 * │                                     │
 * │   Скин (только для новой):          │
 * │   [▼ По умолчанию          ]        │
 * │   [  Чистый                ]        │
 * │   [  Ржавый                ]        │
 * │   [  Военный               ]        │
 * ├─────────────────────────────────────┤
 * │   [По умолчанию]    [Применить] [X] │
 * └─────────────────────────────────────┘
 * 
 * @author HBM-M Team
 */
@OnlyIn(Dist.CLIENT)
public class DoorModelSelectionScreen extends Screen {
    
    private static final int MENU_WIDTH = 280;
    private static final int MENU_HEIGHT = 320;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 22;
    
    // Данные двери
    private final BlockPos doorPos;
    private final String doorId;
    private DoorModelSelection currentSelection;
    private DoorModelSelection selectedSelection;
    
    // Доступные скины
    private List<DoorSkin> availableSkins;
    
    // UI
    private int leftPos;
    private int topPos;
    private Button legacyButton;
    private Button modernButton;
    private int selectedSkinIndex = 0;
    
    public DoorModelSelectionScreen(BlockPos doorPos, String doorId, DoorModelSelection currentSelection) {
        super(Component.translatable("gui.hbm_m.door_model_selection.title"));
        this.doorPos = doorPos;
        this.doorId = doorId;
        this.currentSelection = currentSelection;
        this.selectedSelection = currentSelection;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.leftPos = (this.width - MENU_WIDTH) / 2;
        this.topPos = (this.height - MENU_HEIGHT) / 2;
        
        DoorModelRegistry registry = DoorModelRegistry.getInstance();
        this.availableSkins = registry.getSkins(doorId);
        updateSkinIndex();
        
        int sectionY = topPos + 45 + 18; // Место для заголовка "Модель:"
        
        // Кнопка LEGACY
        addRenderableWidget(Button.builder(
            Component.literal((selectedSelection.isLegacy() ? "► " : "  ") + DoorModelType.LEGACY.getDisplayName()),
            btn -> selectModelType(DoorModelType.LEGACY)
        ).bounds(leftPos + (MENU_WIDTH - BUTTON_WIDTH) / 2, sectionY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        sectionY += BUTTON_HEIGHT + 4;
        
        // Кнопка MODERN
        addRenderableWidget(Button.builder(
            Component.literal((selectedSelection.isModern() ? "► " : "  ") + DoorModelType.MODERN.getDisplayName()),
            btn -> selectModelType(DoorModelType.MODERN)
        ).bounds(leftPos + (MENU_WIDTH - BUTTON_WIDTH) / 2, sectionY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        sectionY += BUTTON_HEIGHT + 15;
        
        // Скины (только для MODERN)
        if (selectedSelection.isModern() && availableSkins.size() > 1) {
            sectionY += 18; // Место для заголовка "Скин:"
            
            for (int i = 0; i < Math.min(availableSkins.size(), 5); i++) {
                final int skinIndex = i;
                DoorSkin skin = availableSkins.get(i);
                boolean isSelected = skin.equals(selectedSelection.getSkin());
                
                addRenderableWidget(Button.builder(
                    Component.literal((isSelected ? "► " : "  ") + skin.getDisplayName()),
                    btn -> selectSkin(skinIndex)
                ).bounds(leftPos + (MENU_WIDTH - BUTTON_WIDTH) / 2, sectionY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
                sectionY += BUTTON_HEIGHT + 2;
            }
        }
        
        // Кнопки действий
        int bottomY = topPos + MENU_HEIGHT - 55;
        
        addRenderableWidget(Button.builder(
            Component.translatable("gui.hbm_m.door_model_selection.set_default"),
            btn -> setAsDefault()
        ).bounds(leftPos + 15, bottomY, 90, 20).build());
        
        addRenderableWidget(Button.builder(
            Component.translatable("gui.hbm_m.door_model_selection.apply"),
            btn -> applySelection()
        ).bounds(leftPos + MENU_WIDTH - 115, bottomY, 100, 20).build());
        
        addRenderableWidget(Button.builder(
            Component.translatable("gui.hbm_m.door_model_selection.cancel"),
            btn -> onClose()
        ).bounds(leftPos + MENU_WIDTH - 115, bottomY + 25, 100, 20).build());
    }
    
    private void updateSkinIndex() {
        selectedSkinIndex = 0;
        for (int i = 0; i < availableSkins.size(); i++) {
            if (availableSkins.get(i).equals(selectedSelection.getSkin())) {
                selectedSkinIndex = i;
                break;
            }
        }
    }
    
    private void selectModelType(DoorModelType type) {
        DoorSkin skin = type.isLegacy() 
            ? DoorSkin.DEFAULT 
            : availableSkins.get(selectedSkinIndex);
        
        selectedSelection = new DoorModelSelection(type, skin);
        
        // Пересоздаём UI
        rebuildWidgets();
    }
    
    private void selectSkin(int index) {
        if (selectedSelection.isModern() && index >= 0 && index < availableSkins.size()) {
            selectedSelection = new DoorModelSelection(DoorModelType.MODERN, availableSkins.get(index));
            rebuildWidgets();
        }
    }
    
    private void setAsDefault() {
        // Сохраняем как настройку по умолчанию
        DoorModelRegistry registry = DoorModelRegistry.getInstance();
        registry.setDefaultSelection(doorId, selectedSelection);
        
        // Показываем уведомление
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                Component.translatable("message.hbm_m.door_model_default_set", 
                    selectedSelection.getDisplayName()),
                true
            );
        }
    }
    
    private void applySelection() {
        if (!selectedSelection.equals(currentSelection)) {
            // Отправляем пакет на сервер
            ServerboundDoorModelPacket.send(doorPos, selectedSelection);
        }
        onClose();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        renderMenuPanel(guiGraphics);
        renderTitle(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderInfo(guiGraphics);
    }
    
    private void renderMenuPanel(GuiGraphics guiGraphics) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        
        // Фон
        guiGraphics.fillGradient(
            leftPos, topPos, 
            leftPos + MENU_WIDTH, topPos + MENU_HEIGHT,
            0xC0101010, 0xD0101010
        );
        
        // Рамка
        int border = 0xFF606060;
        guiGraphics.fill(leftPos, topPos, leftPos + MENU_WIDTH, topPos + 1, border);
        guiGraphics.fill(leftPos, topPos + MENU_HEIGHT - 1, leftPos + MENU_WIDTH, topPos + MENU_HEIGHT, border);
        guiGraphics.fill(leftPos, topPos, leftPos + 1, topPos + MENU_HEIGHT, border);
        guiGraphics.fill(leftPos + MENU_WIDTH - 1, topPos, leftPos + MENU_WIDTH, topPos + MENU_HEIGHT, border);
    }
    
    private void renderTitle(GuiGraphics guiGraphics) {
        Component title = Component.translatable("gui.hbm_m.door_model_selection.title");
        guiGraphics.drawString(font, title, 
            leftPos + (MENU_WIDTH - font.width(title)) / 2, 
            topPos + 12, 
            0xFFFFFF
        );
        
        // ID двери
        if (doorId != null && !doorId.isEmpty()) {
            guiGraphics.drawString(font, Component.literal("(" + doorId + ")"), 
                leftPos + (MENU_WIDTH - font.width("(" + doorId + ")")) / 2, 
                topPos + 25, 
                0x808080
            );
        }
        
        // Заголовки секций
        guiGraphics.drawString(font, 
            Component.translatable("gui.hbm_m.door_model_selection.model_type"),
            leftPos + 15, topPos + 45, 0xFFFF80);
        
        if (selectedSelection.isModern() && availableSkins.size() > 1) {
            int sectionY = topPos + 45 + 18 + BUTTON_HEIGHT + 4 + BUTTON_HEIGHT + 15;
            guiGraphics.drawString(font, 
                Component.translatable("gui.hbm_m.door_model_selection.skin"),
                leftPos + 15, sectionY, 0xFFFF80);
        }
    }
    
    private void renderInfo(GuiGraphics guiGraphics) {
        Component current = Component.translatable(
            "gui.hbm_m.door_model_selection.current",
            selectedSelection.getDisplayName()
        );
        guiGraphics.drawString(font, current, leftPos + 10, topPos + MENU_HEIGHT - 25, 0xAAAAAA);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
