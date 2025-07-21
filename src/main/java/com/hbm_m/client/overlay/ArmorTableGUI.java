package com.hbm_m.client.overlay;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.ArmorTableMenu;

import com.google.common.collect.ImmutableMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Map;
import javax.annotation.Nonnull;

public class ArmorTableGUI extends AbstractContainerScreen<ArmorTableMenu> {

    // Путь к текстуре вашего GUI
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_armor_modifier.png");
    private static final Map<Integer, Component> EMPTY_SLOT_TOOLTIPS;

    static {
        // Инициализируем подсказки один раз при загрузке класса
        EMPTY_SLOT_TOOLTIPS = ImmutableMap.<Integer, Component>builder()
                // Слот 0: Центральный (Желтый цвет)
                .put(0, Component.translatable("tooltip.hbm_m.armor_table.main_slot").withStyle(ChatFormatting.YELLOW))
                
                // Слоты 1-4: Модули брони (Фиолетовый цвет)
                .put(1, Component.translatable("tooltip.hbm_m.armor_table.helmet_slot").withStyle(ChatFormatting.DARK_PURPLE))
                .put(2, Component.translatable("tooltip.hbm_m.armor_table.chestplate_slot").withStyle(ChatFormatting.DARK_PURPLE))
                .put(3, Component.translatable("tooltip.hbm_m.armor_table.leggings_slot").withStyle(ChatFormatting.DARK_PURPLE))
                .put(4, Component.translatable("tooltip.hbm_m.armor_table.boots_slot").withStyle(ChatFormatting.DARK_PURPLE))
                
                // Слоты 5-8: Материалы (Фиолетовый цвет)
                // Примечание: У вас 5 названий на 4 слота. Я распределил их логически. "Сервоприводы" остались.
                .put(5, Component.translatable("tooltip.hbm_m.armor_table.battery_slot").withStyle(ChatFormatting.DARK_PURPLE)) // Аккумулятор
                .put(6, Component.translatable("tooltip.hbm_m.armor_table.special_slot").withStyle(ChatFormatting.DARK_PURPLE)) // Особое
                .put(7, Component.translatable("tooltip.hbm_m.armor_table.plating_slot").withStyle(ChatFormatting.DARK_PURPLE)) // Пластина
                .put(8, Component.translatable("tooltip.hbm_m.armor_table.casing_slot").withStyle(ChatFormatting.DARK_PURPLE))  // Обшивка
                .put(9, Component.translatable("tooltip.hbm_m.armor_table.servos_slot").withStyle(ChatFormatting.DARK_PURPLE)) // Сервоприводы
                .build();
    }
    // --- ЗОНЫ КЛИКОВ ДЛЯ НАДЕТОЙ БРОНИ ---
    private Rect2i helmetArea, chestplateArea, leggingsArea, bootsArea;

    public ArmorTableGUI(ArmorTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222; 
    }

    @Override
    protected void init() {
        super.init();
        // Инициализируем зоны кликов при открытии GUI
        int x = this.leftPos;
        int y = this.topPos;
        int displayX = x + 7;
        int displayY_start = y + 8;
        int y_offset = 18;
        this.helmetArea = new Rect2i(displayX, displayY_start, 16, 16);
        this.chestplateArea = new Rect2i(displayX, displayY_start + y_offset, 16, 16);
        this.leggingsArea = new Rect2i(displayX, displayY_start + y_offset * 2, 16, 16);
        this.bootsArea = new Rect2i(displayX, displayY_start + y_offset * 3, 16, 16);
        this.inventoryLabelY = this.imageHeight - 96;
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderEquippedArmor(guiGraphics);

        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && 
        this.hoveredSlot instanceof SlotItemHandler)
    {
        Component tooltip = EMPTY_SLOT_TOOLTIPS.get(this.hoveredSlot.getContainerSlot());
        if (tooltip != null) {
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}

    private void renderEquippedArmor(GuiGraphics guiGraphics) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        Inventory inv = this.minecraft.player.getInventory();

        // ИСПРАВЛЕНО: Рендерим в правильном порядке
        guiGraphics.renderItem(inv.getArmor(3), this.helmetArea.getX(), this.helmetArea.getY()); // Шлем
        guiGraphics.renderItem(inv.getArmor(2), this.chestplateArea.getX(), this.chestplateArea.getY()); // Нагрудник
        guiGraphics.renderItem(inv.getArmor(1), this.leggingsArea.getX(), this.leggingsArea.getY()); // Поножи
        guiGraphics.renderItem(inv.getArmor(0), this.bootsArea.getX(), this.bootsArea.getY()); // Ботинки
    }

    // --- ДОБАВЛЕНА ИНТЕРАКТИВНОСТЬ ---
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по иконкам надетой брони
        if (button == 0) { // Только левая кнопка мыши
            if (handleEquippedArmorClick(mouseX, mouseY)) {
                return true; // Сообщаем, что клик обработан
            }
        }
        // Если клик был не по нашей броне, передаем его стандартному обработчику
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleEquippedArmorClick(double mouseX, double mouseY) {
        // Индексы слотов брони в PlayerContainer: 39=шлем, 38=кираса, 37=поножи, 36=ботинки
        if (helmetArea.contains((int) mouseX, (int) mouseY)) {
            handleSlotClick(39);
            return true;
        }
        if (chestplateArea.contains((int) mouseX, (int) mouseY)) {
            handleSlotClick(38);
            return true;
        }
        if (leggingsArea.contains((int) mouseX, (int) mouseY)) {
            handleSlotClick(37);
            return true;
        }
        if (bootsArea.contains((int) mouseX, (int) mouseY)) {
            handleSlotClick(36);
            return true;
        }
        return false;
    }

    private void handleSlotClick(int slotIndex) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            // Это самый правильный способ симулировать клик по слоту.
            // Он отправляет пакет на сервер, чтобы взять предмет в курсор.
            this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, slotIndex, 0, ClickType.PICKUP, this.minecraft.player);
        }
    }
}