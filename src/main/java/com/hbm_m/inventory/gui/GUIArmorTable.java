package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

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
            /*new ResourceLocation(RefStrings.MODID, "textures/gui/machine/gui_armor_modifier.png");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_armor_modifier.png");
            //?}


    private static final int SIDE_PANEL_WIDTH = 22;
    private static final int SIDE_PANEL_HEIGHT = 100;
    private static final int SIDE_PANEL_U = 176;
    private static final int SIDE_PANEL_V = 96;

    private static final int SIDE_PANEL_OFFSET_X = 0;
    private static final int SIDE_PANEL_OFFSET_Y = 31;

    // Стартовые координаты для ПЕРВОГО слота (шлема) относительно левого верхнего угла основного GUI
    private static final int SLOT_START_X = -17 + 22;
    private static final int SLOT_START_Y = 36;

    public GUIArmorTable(ArmorTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        // Original: xSize = 176 + 22 (Seitenleiste fuer die Ruestungsslots links)
        this.imageWidth = 176 + 22;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();

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
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTicks);
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

        // 1. Рисуем основной фон GUI (Original: guiLeft + 22, Breite xSize - 22)
        guiGraphics.blit(TEXTURE, x + 22, y, 0, 0, this.imageWidth - 22, this.imageHeight);
        
        // 2. Рисуем нашу боковую панель
        guiGraphics.blit(TEXTURE, x + SIDE_PANEL_OFFSET_X, y + SIDE_PANEL_OFFSET_Y, SIDE_PANEL_U, SIDE_PANEL_V, SIDE_PANEL_WIDTH, SIDE_PANEL_HEIGHT);

        // 3. Вызываем отрисовку наших фоновых иконок
        this.renderSlotBackgrounds(guiGraphics);

        // 4. Рисуем индикаторы совместимости
        drawCompatibilityIndicators(guiGraphics);
    }

    /** Original: Hinweis-Symbol neben dem Ruestungsslot und je Mod-Slot gruener/roter Rahmen (passt/passt nicht). */
    private void drawCompatibilityIndicators(GuiGraphics guiGraphics) {
        ItemStack armor = this.menu.getSlot(ArmorTableMenu.MOD_SLOTS).getItem();

        if (!armor.isEmpty()) {
            if (armor.getItem() instanceof net.minecraft.world.item.ArmorItem)
                guiGraphics.blit(TEXTURE, leftPos + 41 + 22, topPos + 60, 176, 74, 22, 22);
            else
                guiGraphics.blit(TEXTURE, leftPos + 41 + 22, topPos + 60, 176, 52, 22, 22);
        } else if (System.currentTimeMillis() % 1000 < 500) {
            guiGraphics.blit(TEXTURE, leftPos + 41 + 22, topPos + 60, 176, 52, 22, 22);
        }

        for (int i = 0; i < ArmorTableMenu.MOD_SLOTS; i++) {
            Slot slot = this.menu.getSlot(i);
            ItemStack mod = slot.getItem();
            if (mod.isEmpty()) continue;
            int x = slot.x - 1, y = slot.y - 1;
            if (com.hbm_m.armormod.util.ArmorModificationHelper.isApplicable(armor, mod)) {
                guiGraphics.blit(TEXTURE, leftPos + x, topPos + y, 176, 34, 18, 18);
            } else {
                guiGraphics.blit(TEXTURE, leftPos + x, topPos + y, 176, 16, 18, 18);
            }
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = Component.translatable("container.armorTable").getString();
        guiGraphics.drawString(this.font, name, (this.imageWidth - 22) / 2 - this.font.width(name) / 2 + 22, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8 + 22, this.imageHeight - 96 + 2, 4210752, false);
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