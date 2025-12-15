package com.hbm_m.client.overlay;

import java.util.ArrayList;
import java.util.List;

// GUI для сборочной машины. Отвечает за отрисовку прогресса, энергии и подсказок.
// Основан на GuiInfoScreen и использует текстуры из ресурсов мода.
import javax.annotation.Nonnull;

import com.hbm_m.item.custom.industrial.ItemAssemblyTemplate;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.MachineAssemblerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GUIMachineAssembler extends GuiInfoScreen<MachineAssemblerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_assembler.png");
    
    // Константа для корректного индекса слота шаблона
    private static final int TEMPLATE_SLOT_GUI_INDEX = 36 + 4; // 36 слотов игрока + 4 слота машины

    public GUIMachineAssembler(MachineAssemblerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;

        // Рисуем фон
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // Отрисовка энергии
        int energyBarHeight = 52;
        int energy = this.menu.getEnergyScaled(energyBarHeight);
        guiGraphics.blit(TEXTURE, x + 116, y + 70 - energy, 176, 52 - energy, 16, energy);

        // Отрисовка прогресса
        if(menu.isCrafting()) {
            int progressWidth = 83;
            guiGraphics.blit(TEXTURE, x + 45, y + 82, 2, 222, menu.getProgressScaled(progressWidth), 32);
        }

        // Информационные панели (ВАЖНО: привязываем текстуру заново)
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawInfoPanels(guiGraphics);
        renderGhostItems(guiGraphics);
    }

    /**
     * Отрисовывает информационные панели (иконки за пределами GUI)
     */
    private void drawInfoPanels(GuiGraphics guiGraphics) {
        drawInfoPanel(guiGraphics, -16, 16, PanelType.LARGE_GRAY_STAR);

        ItemStack templateStack = menu.getSlot(TEMPLATE_SLOT_GUI_INDEX).getItem();
        if (templateStack.isEmpty() || !(templateStack.getItem() instanceof ItemAssemblyTemplate)) {
            drawInfoPanel(guiGraphics, -16, 36, PanelType.LARGE_RED_EXCLAMATION);
        }
    }

    private void renderGhostItems(GuiGraphics guiGraphics) {
        NonNullList<ItemStack> ghostItems = this.menu.getBlockEntity().getGhostItems();
        
        if (ghostItems.isEmpty()) {
            return;
        }
        
        // Группируем одинаковые предметы и суммируем их количество
        java.util.Map<ItemStack, Integer> groupedItems = new java.util.LinkedHashMap<>();
        for (ItemStack stack : ghostItems) {
            if (stack.isEmpty()) {
                continue;
            }
            
            // Ищем уже существующий предмет в группе
            ItemStack found = null;
            for (ItemStack key : groupedItems.keySet()) {
                if (ItemStack.isSameItemSameTags(key, stack)) {
                    found = key;
                    break;
                }
            }
            
            if (found != null) {
                // Увеличиваем количество
                groupedItems.put(found, groupedItems.get(found) + stack.getCount());
            } else {
                // Добавляем новый предмет
                ItemStack copy = stack.copy();
                copy.setCount(1); // Нормализуем количество для ключа
                groupedItems.put(copy, stack.getCount());
            }
        }
        
        // Слоты 6-17 (handler) соответствуют слотам 42-53 в menu (36 слотов игрока + 6 машины)
        int inputSlotsStart = 36 + 6; // 42
        int inputSlotsCount = 12;
        
        // Отрисовываем сгруппированные предметы
        int slotOffset = 0;
        for (java.util.Map.Entry<ItemStack, Integer> entry : groupedItems.entrySet()) {
            if (slotOffset >= inputSlotsCount) {
                break; // Превышен лимит слотов
            }
            
            ItemStack ghostStack = entry.getKey().copy();
            ghostStack.setCount(entry.getValue()); // Устанавливаем суммарное количество
            
            // Получаем слот
            int slotIndex = inputSlotsStart + slotOffset;
            if (slotIndex >= this.menu.slots.size()) break;
            
            net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);
            
            // Отрисовываем призрак только если слот пуст
            if (!slot.hasItem()) {
                // Отрисовка призрачного предмета с полупрозрачностью
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 100); // z-level 100
                
                // Полупрозрачность
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
                
                int x = this.leftPos + slot.x;
                int y = this.topPos + slot.y;
                
                guiGraphics.renderItem(ghostStack, x, y);
                
                // ОТРИСОВКА КОЛИЧЕСТВА (если > 1)
                if (ghostStack.getCount() > 1) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Полная непрозрачность для текста
                    guiGraphics.renderItemDecorations(this.font, ghostStack, x, y);
                }
                
                // Восстанавливаем цвет
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
                
                guiGraphics.pose().popPose();
            }
            
            slotOffset++;
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(@Nonnull GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        // Подсказка для энергии
        renderEnergyTooltip(pGuiGraphics, pX, pY);
        
        // Подсказки для информационных панелей
        renderInfoPanelTooltips(pGuiGraphics, pX, pY);
    }

    /**
     * Отрисовывает подсказку для шкалы энергии
     */
    private void renderEnergyTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        int energyBarX = this.leftPos + 116;
        int energyBarY = this.topPos + 18;
        int energyBarWidth = 16;
        int energyBarHeight = 52;

        if (pX >= energyBarX && pX < energyBarX + energyBarWidth &&
                pY >= energyBarY && pY < energyBarY + energyBarHeight) {

            List<Component> tooltip = new ArrayList<>();

        long energy = this.menu.getEnergyLong();
        long maxEnergy = this.menu.getMaxEnergyLong();

            String energyStr = EnergyFormatter.format(energy);
            String maxEnergyStr = EnergyFormatter.format(maxEnergy);

            tooltip.add(Component.literal(energyStr + " / " + maxEnergyStr + " HE"));

            pGuiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pX, pY);
        }
    }

    /**
     * Отрисовывает подсказки для информационных панелей
     */
    private void renderInfoPanelTooltips(GuiGraphics pGuiGraphics, int pX, int pY) {
        // Панель шаблона
        if (isPointInRect(-16, 16, 16, 16, pX, pY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("desc.gui.template"));
            pGuiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pX, pY);
        }

        // Панель предупреждения (только если нет шаблона)
        ItemStack templateStack = menu.getSlot(TEMPLATE_SLOT_GUI_INDEX).getItem();
        if (templateStack.isEmpty() || !(templateStack.getItem() instanceof ItemAssemblyTemplate)) {
            if (isPointInRect(-16, 36, 16, 16, pX, pY)) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("desc.gui.assembler.warning").withStyle(ChatFormatting.RED));
                pGuiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), pX, pY);
            }
        }
    }

}