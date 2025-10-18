package com.hbm_m.client.overlay;

import com.hbm_m.menu.AnvilMenu;
import com.hbm_m.recipe.AnvilRecipe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AnvilScreen extends AbstractContainerScreen<AnvilMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("hbm_m", "textures/gui/anvil_gui.png");
    private EditBox searchBox;
    private int selectedRecipeIndex = -1;

    public AnvilScreen(AnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();

        // Поле поиска (слот 9)
        int searchX = this.leftPos + 9;
        int searchY = this.topPos + 109;
        searchBox = new EditBox(this.font, searchX, searchY, 86, 14, Component.literal("Search"));
        searchBox.setMaxLength(50);
        searchBox.setBordered(true);
        searchBox.setVisible(true);
        searchBox.setTextColor(0xFFFFFF);
        this.addRenderableWidget(searchBox);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Рендерим список рецептов (2 ряда по 5)
        renderRecipeList(guiGraphics);

        // Рендерим панель с требуемыми предметами (синее поле)
        renderRequiredItems(guiGraphics);
    }

    private void renderRecipeList(GuiGraphics guiGraphics) {
        List<AnvilRecipe> visibleRecipes = menu.getVisibleRecipes();

        int startX = this.leftPos + 60;
        int startY = this.topPos + 82;
        int slotSize = 18;

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                if (index < visibleRecipes.size()) {
                    AnvilRecipe recipe = visibleRecipes.get(index);
                    ItemStack output = recipe.getOutput();

                    int x = startX + col * slotSize;
                    int y = startY + row * slotSize;

                    guiGraphics.renderItem(output, x, y);
                    guiGraphics.renderItemDecorations(this.font, output, x, y);
                }
            }
        }
    }

    private void renderRequiredItems(GuiGraphics guiGraphics) {
        if (selectedRecipeIndex >= 0) {
            List<AnvilRecipe> visibleRecipes = menu.getVisibleRecipes();
            if (selectedRecipeIndex < visibleRecipes.size()) {
                AnvilRecipe recipe = visibleRecipes.get(selectedRecipeIndex);

                // Синяя панель (поле 5)
                int panelX = this.leftPos + 150;
                int panelY = this.topPos + 20;

                // Inputs
                guiGraphics.drawString(this.font, "Inputs:", panelX + 5, panelY + 5, 0xFFFFFF);

                int yOffset = 20;
                guiGraphics.renderItem(recipe.getInputA(), panelX + 10, panelY + yOffset);
                guiGraphics.renderItem(recipe.getInputB(), panelX + 30, panelY + yOffset);

                // Required items
                yOffset += 25;
                for (ItemStack required : recipe.getRequiredItems()) {
                    guiGraphics.renderItem(required, panelX + 10, panelY + yOffset);
                    yOffset += 20;
                }

                // Outputs
                yOffset += 10;
                guiGraphics.drawString(this.font, "Outputs:", panelX + 5, panelY + yOffset, 0xFFFFFF);
                yOffset += 15;
                guiGraphics.renderItem(recipe.getOutput(), panelX + 10, panelY + yOffset);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверка клика по стрелкам навигации (7 и 8)
        int leftArrowX = this.leftPos + 1;
        int leftArrowY = this.topPos + 92;
        int rightArrowX = this.leftPos + 90;
        int rightArrowY = this.topPos + 92;

        if (isHovering(leftArrowX, leftArrowY, 10, 36, mouseX, mouseY)) {
            menu.scrollLeft();
            return true;
        }

        if (isHovering(rightArrowX, rightArrowY, 10, 36, mouseX, mouseY)) {
            menu.scrollRight();
            return true;
        }

        // Проверка клика по рецептам
        if (handleRecipeClick(mouseX, mouseY)) {
            return true;
        }

        // Проверка клика по кнопке крафта (6)
        int craftButtonX = this.leftPos + 235;
        int craftButtonY = this.topPos + 92;

        if (isHovering(craftButtonX, craftButtonY, 16, 16, mouseX, mouseY)) {
            if (selectedRecipeIndex >= 0) {
                List<AnvilRecipe> visibleRecipes = menu.getVisibleRecipes();
                if (selectedRecipeIndex < visibleRecipes.size()) {
                    AnvilRecipe recipe = visibleRecipes.get(selectedRecipeIndex);
                    boolean craftAll = hasShiftDown();
                    menu.craftItem(recipe, craftAll);
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleRecipeClick(double mouseX, double mouseY) {
        int startX = this.leftPos + 60;
        int startY = this.topPos + 82;
        int slotSize = 18;

        List<AnvilRecipe> visibleRecipes = menu.getVisibleRecipes();

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                if (index < visibleRecipes.size()) {
                    int x = startX + col * slotSize;
                    int y = startY + row * slotSize;

                    if (isHovering(x, y, 16, 16, mouseX, mouseY)) {
                        selectedRecipeIndex = index;
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Обработка Enter в поле поиска
        if (keyCode == 257 && searchBox.isFocused()) { // 257 = Enter
            menu.setSearchQuery(searchBox.getValue());
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}