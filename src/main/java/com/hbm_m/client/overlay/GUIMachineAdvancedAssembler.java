package com.hbm_m.client.overlay;

// GUI для продвинутой сборочной машины.
// Отвечает за отрисовку прогресса, энергии, подсказок и взаимодействие с пользователем.
// Основан на AbstractContainerScreen и использует текстуры из ресурсов мода.
import com.hbm_m.menu.MachineAdvancedAssemblerMenu;
import com.hbm_m.block.entity.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;


// TODO: Нужен утилитарный класс для отрисовки подсказок и жидкостей.
// Этот функционал сейчас встроен в этот класс

public class GUIMachineAdvancedAssembler extends AbstractContainerScreen<MachineAdvancedAssemblerMenu> {

    // Текстура из старого GUI
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_assembler.png");

    private final MachineAdvancedAssemblerBlockEntity blockEntity;

    public GUIMachineAdvancedAssembler(MachineAdvancedAssemblerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockEntity = pMenu.getBlockEntity();

        this.imageWidth = 176;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
        // Здесь можно было бы добавить виджеты (кнопки), но в старом GUI их не было,
        // клик по иконке рецепта обрабатывался вручную.
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        
        // Отрисовка основной текстуры
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Отрисовка энергии (логика и координаты полностью сохранены)
        int energyStored = this.blockEntity.getEnergyStored();
        int maxEnergy = this.blockEntity.getMaxEnergyStored();
        if (maxEnergy > 0) {
            int energyBarHeight = (int) ((long)energyStored * 61 / maxEnergy);
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - energyBarHeight, 176, 61 - energyBarHeight, 16, energyBarHeight);
        }

        // Отрисовка прогресса (логика и координаты полностью сохранены)
        if (this.blockEntity.isCrafting()) {
            int progressWidth = (int) Math.ceil(70.0 * this.blockEntity.getProgress() / this.blockEntity.getMaxProgress());
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, progressWidth, 16);
        }

        // TODO: Реализуйте вашу систему рецептов. Это заглушка, имитирующая старую логику.
        boolean hasRecipe = true; // Заменить на blockEntity.hasRecipe();
        boolean canProcess = hasRecipe && energyStored >= 100; // Заменить на blockEntity.canProcess();

        // Отрисовка светодиодов (LEDs)
        if (this.blockEntity.isCrafting()) { // В старом коде это didProcess
            // Левый LED (зеленый)
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            // Правый LED (зеленый)
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (hasRecipe) {
            // Левый LED (желтый)
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            if (canProcess) {
                // Правый LED (желтый)
                guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }
        
        // Отрисовка иконки рецепта
        // TODO: Получайте иконку из вашего менеджера рецептов
        // guiGraphics.renderItem(recipe.getIcon(), this.leftPos + 8, this.topPos + 126);

        // Отрисовка "призрачных" предметов в слотах (логика перенесена)
        // TODO: Адаптируйте под вашу систему рецептов
        /*
        if (recipe != null && recipe.inputItem != null) {
            for(int i = 0; i < recipe.inputItem.length; i++) {
                Slot slot = this.menu.getSlot(i + INPUT_SLOT_START_INDEX);
                if (!slot.hasItem()) {
                    guiGraphics.renderFakeItem(recipe.inputItem[i], this.leftPos + slot.x, this.topPos + slot.y);
                }
            }
        }
        */
        
        // Отрисовка жидкостей в танках
        // TODO: Создайте утилитарный класс-рендер для жидкостей или используйте готовую библиотеку
        // renderFluidTank(guiGraphics, this.blockEntity.getInputTank(), this.leftPos + 8, this.topPos + 115, 52, 16);
        // renderFluidTank(guiGraphics, this.blockEntity.getOutputTank(), this.leftPos + 80, this.topPos + 115, 52, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        // Отрисовка заголовка (координаты сохранены)
        guiGraphics.drawString(this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 4210752, false);
        // Отрисовка надписи "Inventory" (координаты сохранены)
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(guiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        super.renderTooltip(guiGraphics, pMouseX, pMouseY);

        // Подсказка для шкалы энергии
        if (isMouseOver(pMouseX, pMouseY, 152, 18, 16, 61)) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(this.blockEntity.getEnergyStored() + " / " + this.blockEntity.getMaxEnergyStored() + " FE"),
                    pMouseX, pMouseY);
        }

        // Подсказка для танка с входной жидкостью
        if (isMouseOver(pMouseX, pMouseY, 8, 99, 52, 16)) {
            // TODO: Отобразите информацию о жидкости
            // guiGraphics.renderTooltip(this.font, Component.literal("Input Tank Info"), pMouseX, pMouseY);
        }

        // Подсказка для танка с выходной жидкостью
        if (isMouseOver(pMouseX, pMouseY, 80, 99, 52, 16)) {
            // TODO: Отобразите информацию о жидкости
            // guiGraphics.renderTooltip(this.font, Component.literal("Output Tank Info"), pMouseX, pMouseY);
        }

        // Подсказка для слота выбора рецепта
        if (isMouseOver(pMouseX, pMouseY, 7, 125, 18, 18)) {
            // TODO: Замените на логику из вашей системы рецептов
            // if (this.blockEntity.hasRecipe()) {
            //     guiGraphics.renderTooltip(this.font, recipe.print(), pMouseX, pMouseY);
            // } else {
                 guiGraphics.renderTooltip(this.font, Component.translatable("gui.hbm_m.set_recipe"), pMouseX, pMouseY);
            // }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        // Обработка клика по слоту выбора рецепта
        if (isMouseOver((int)pMouseX, (int)pMouseY, 7, 125, 18, 18)) {
            // TODO: Откройте ваш экран выбора рецептов.
            // Minecraft.getInstance().setScreen(new RecipeSelectorScreen(...));
            return true;
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    /**
     * Хелпер-метод для проверки нахождения курсора над элементом GUI.
     * @param pMouseX X координата мыши
     * @param pMouseY Y координата мыши
     * @param pX      Локальная X координата элемента
     * @param pY      Локальная Y координата элемента
     * @param pWidth  Ширина элемента
     * @param pHeight Высота элемента
     * @return true, если курсор находится над элементом
     */
    private boolean isMouseOver(int pMouseX, int pMouseY, int pX, int pY, int pWidth, int pHeight) {
        return pMouseX >= this.leftPos + pX && pMouseX < this.leftPos + pX + pWidth &&
               pMouseY >= this.topPos + pY && pMouseY < this.topPos + pY + pHeight;
    }
}