package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.menu.ArmorTableMenu;
import com.hbm_m.interfaces.IHasTooltip;
import com.hbm_m.interfaces.IMixinSlot;
import com.hbm_m.lib.RefStrings;

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

public class GUIArmorTable extends AbstractContainerScreen<ArmorTableMenu> {

    private static final ResourceLocation TEXTURE =
            //? if fabric && < 1.21.1 {
            new ResourceLocation(RefStrings.MODID, "textures/gui/machine/gui_armor_modifier.png");
            //?} else {
                        /*ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_armor_modifier.png");
            *///?}


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

        // ВАЖНО: Slot#index — это индекс *в контейнере-источнике* (у брони это 36..39),
        // а нам нужно двигать слоты по их индексу *в меню* (46..49).
        repositionArmorSidePanelSlots();
    }

    private void repositionArmorSidePanelSlots() {
        int[] armorMenuSlotIndices = new int[] {
                ArmorTableMenu.SLOT_ARMOR_SIDE_HELMET,
                ArmorTableMenu.SLOT_ARMOR_SIDE_CHEST,
                ArmorTableMenu.SLOT_ARMOR_SIDE_LEGS,
                ArmorTableMenu.SLOT_ARMOR_SIDE_BOOTS
        };

        for (int i = 0; i < armorMenuSlotIndices.length; i++) {
            Slot slot = this.menu.getSlot(armorMenuSlotIndices[i]);
            int newX = SLOT_START_X;
            int newY = SLOT_START_Y + (i * 18);
            ((IMixinSlot) slot).setPos(newX, newY);
        }
    }

    /**
     * Главный метод рендеринга очень простой. Мы вызываем ванильный код,
     * который сам корректно отрисует фон, слоты, предметы, подсветку и прочее,
     * потому что теперь он знает ПРАВИЛЬНЫЕ координаты слотов.
     */
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        // Отдельно вызываем рендер наших кастомных подсказок, чтобы они были поверх всего.
        // Переопределяем рендер подсказок, чтобы они были поверх всего.
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    private void renderSlotBackgrounds(GuiGraphics guiGraphics) {
        int[] armorMenuSlotIndices = new int[] {
                ArmorTableMenu.SLOT_ARMOR_SIDE_HELMET,
                ArmorTableMenu.SLOT_ARMOR_SIDE_CHEST,
                ArmorTableMenu.SLOT_ARMOR_SIDE_LEGS,
                ArmorTableMenu.SLOT_ARMOR_SIDE_BOOTS
        };

        for (int menuSlotIndex : armorMenuSlotIndices) {
            Slot slot = this.menu.getSlot(menuSlotIndex);
            if (!slot.hasItem()) {
                ResourceLocation spriteLocation = getArmorSlotBackground(menuSlotIndex);
                if (spriteLocation == null) continue;

                TextureAtlasSprite sprite = Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(spriteLocation);

                guiGraphics.blit(this.leftPos + slot.x, this.topPos + slot.y, 0, 16, 16, sprite);
            }
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
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
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
    private ResourceLocation getArmorSlotBackground(int menuSlotIndex) {
        if (menuSlotIndex == ArmorTableMenu.SLOT_ARMOR_SIDE_HELMET) return InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
        if (menuSlotIndex == ArmorTableMenu.SLOT_ARMOR_SIDE_CHEST) return InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
        if (menuSlotIndex == ArmorTableMenu.SLOT_ARMOR_SIDE_LEGS) return InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
        if (menuSlotIndex == ArmorTableMenu.SLOT_ARMOR_SIDE_BOOTS) return InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
        return null;
    }
}