package com.hbm_m.client.overlay;

// GUI для верстака модификации брони. Имеет боковую панель с подсказками,
// перемещает слоты брони в боковую панель, рисует иконки слотов брони,
// рисует индикаторы совместимости для слотов модов и показывает подсказки для модов.
// Основан на AbstractContainerScreen и использует миксин для изменения координат слотов.
// Подсказки для пустых слотов реализованы через интерфейс IHasTooltip.
import com.hbm_m.block.custom.machines.armormod.menu.ArmorTableMenu;
import com.hbm_m.block.custom.machines.armormod.menu.IHasTooltip;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.util.IMixinSlot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GUIArmorTable extends AbstractContainerScreen<ArmorTableMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_armor_modifier.png");

    private static final int SIDE_PANEL_WIDTH = 22;
    private static final int SIDE_PANEL_HEIGHT = 80;
    private static final int SIDE_PANEL_U = 176;
    private static final int SIDE_PANEL_V = 96;

    private static final int SIDE_PANEL_OFFSET_X = -22;
    private static final int SIDE_PANEL_OFFSET_Y = 31;

    // Стартовые координаты для ПЕРВОГО слота (шлема) относительно левого верхнего угла основного GUI
    private static final int SLOT_START_X = -17;
    private static final int SLOT_START_Y = 36;

    public GUIArmorTable(ArmorTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 96;

        for (Slot slot : this.menu.slots) {
            if (isArmorSlot(slot)) {
                int armorIndex = slot.index - 46;
                // ИСПОЛЬЗУЕМ КОНСТАНТЫ ДЛЯ РАСЧЕТА ПОЗИЦИИ
                int newX = SLOT_START_X;
                int newY = SLOT_START_Y + (armorIndex * 18);

                ((IMixinSlot) slot).setPos(newX, newY);
            }
        }
    }

    /**
     * Главный метод рендеринга очень простой. Мы вызываем ванильный код,
     * который сам корректно отрисует фон, слоты, предметы, подсветку и прочее,
     * потому что теперь он знает ПРАВИЛЬНЫЕ координаты слотов.
     */
    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        // Отдельно вызываем рендер наших кастомных подсказок, чтобы они были поверх всего.
        // Переопределяем рендер подсказок, чтобы они были поверх всего.
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    private void renderSlotBackgrounds(GuiGraphics guiGraphics) {
        // Проходимся по всем слотам в контейнере
        for (final Slot slot : this.menu.slots) {
            // Нас интересуют только слоты брони и только если они пустые
            if (isArmorSlot(slot) && !slot.hasItem()) {
                // Получаем нужную иконку для этого слота
                ResourceLocation spriteLocation = getArmorSlotBackground(slot.index);
                if (spriteLocation != null) {
                    // Получаем саму текстуру (спрайт) из атласа
                    TextureAtlasSprite sprite = Minecraft.getInstance()
                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(spriteLocation);
                    
                    // Рисуем спрайт в координатах слота
                    // slot.x и slot.y уже правильные благодаря нашему миксину
                    guiGraphics.blit(this.leftPos + slot.x, this.topPos + slot.y, 0, 16, 16, sprite);
                }
            }
        }
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 1. Рисуем основной фон GUI
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        
        // 2. Рисуем нашу боковую панель
        guiGraphics.blit(TEXTURE, x + SIDE_PANEL_OFFSET_X, y + SIDE_PANEL_OFFSET_Y, SIDE_PANEL_U, SIDE_PANEL_V, SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT);

        // 3. Вызываем отрисовку наших фоновых иконок
        this.renderSlotBackgrounds(guiGraphics);

        // 4. Рисуем индикаторы совместимости
        drawCompatibilityIndicators(guiGraphics);
    }

    private void drawCompatibilityIndicators(GuiGraphics guiGraphics) {
        // Предмет, который игрок держит в руке
        ItemStack carried = this.menu.getCarried();
        if (carried.isEmpty()) {
            return;
        }

        for (int i = 1; i <= 9; i++) { // Проходим по слотам модов (индексы 1-9)
            Slot slot = this.menu.getSlot(i);
            
            // Проверяем, можно ли поместить предмет в этот слот
            if (slot.mayPlace(carried)) {
                // Рисуем зеленый полупрозрачный квадрат (валидно)
                guiGraphics.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0x8000FF00);
            } else {
                // Рисуем красный полупрозрачный квадрат (невалидно)
                guiGraphics.fill(this.leftPos + slot.x, this.topPos + slot.y, this.leftPos + slot.x + 16, this.topPos + slot.y + 16, 0x80FF0000);
            }
        }
    }

    @Override
    protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Сначала вызываем стандартную логику, чтобы не сломать подсказки для предметов
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        
        // Если мы навели на пустой слот, который умеет давать подсказку...
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot instanceof IHasTooltip slotWithTooltip) {
            // ...то мы просто просим у него эту подсказку и отрисовываем ее.
            guiGraphics.renderTooltip(this.font, slotWithTooltip.getEmptyTooltip(), mouseX, mouseY);
        }
    }

    // Вспомогательный метод для идентификации слотов брони
    // Он все еще нужен для логики в init()
    
    @Nullable
    private ResourceLocation getArmorSlotBackground(int slotIndex) {
        int armorIndex = slotIndex - 46; // 0=head, 1=chest, 2=legs, 3=feet
        return switch (armorIndex) {
            case 0 -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
            case 1 -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
            case 2 -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
            case 3 -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
            default -> null;
        };
    }

    private boolean isArmorSlot(Slot slot) {
        int armorSlotStartIndex = 46;
        return slot.index >= armorSlotStartIndex && slot.index < armorSlotStartIndex + 4;
    }
}