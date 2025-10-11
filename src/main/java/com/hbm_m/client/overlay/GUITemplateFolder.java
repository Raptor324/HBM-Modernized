package com.hbm_m.client.overlay;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.GiveTemplateC2SPacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.util.TemplateCraftingCosts;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

public class GUITemplateFolder extends Screen {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_planner.png");
    private final int imageWidth = 176;
    private final int imageHeight = 229;
    private int leftPos;
    private int topPos;

    private EditBox searchBox;
    private int currentPage = 0;
    private final List<ItemStack> allRecipes = new ArrayList<>();
    private List<ItemStack> filteredRecipes = new ArrayList<>();

    public GUITemplateFolder(ItemStack folderStack) {
        super(Component.translatable(folderStack.getDescriptionId()));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // Заполняем список рецептов здесь, а не в конструкторе
        if (this.allRecipes.isEmpty()) {
            if (this.minecraft != null && this.minecraft.level != null) {
                // Добавляем штампы пресса В НАЧАЛО списка

                // Каменные штампы
                allRecipes.add(new ItemStack(ModItems.STAMP_STONE_PLATE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_STONE_WIRE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_STONE_CIRCUIT.get()));

                // Железные штампы
                allRecipes.add(new ItemStack(ModItems.STAMP_IRON_PLATE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_IRON_WIRE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_IRON_CIRCUIT.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_IRON_9.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_IRON_44.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_IRON_50.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_IRON_357.get()));

                // Стальные штампы
                allRecipes.add(new ItemStack(ModItems.STAMP_STEEL_PLATE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_STEEL_WIRE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_STEEL_CIRCUIT.get()));

                // Титановые штампы
                allRecipes.add(new ItemStack(ModItems.STAMP_TITANIUM_PLATE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_TITANIUM_WIRE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_TITANIUM_CIRCUIT.get()));

                // Обсидиановые штампы
                allRecipes.add(new ItemStack(ModItems.STAMP_OBSIDIAN_PLATE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_OBSIDIAN_WIRE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_OBSIDIAN_CIRCUIT.get()));

                // Desh штампы
                allRecipes.add(new ItemStack(ModItems.STAMP_DESH_PLATE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_DESH_WIRE.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_DESH_CIRCUIT.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_DESH_9.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_DESH_44.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_DESH_50.get()));
                allRecipes.add(new ItemStack(ModItems.STAMP_DESH_357.get()));

                // Затем добавляем рецепты сборочной машины
                List<AssemblerRecipe> recipes = this.minecraft.level.getRecipeManager()
                        .getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);
                for (AssemblerRecipe recipe : recipes) {
                    allRecipes.add(recipe.getResultItem(null));
                }
            }
            this.filteredRecipes.clear();
            this.filteredRecipes.addAll(allRecipes);
        }

        // Поле поиска
        this.searchBox = new EditBox(this.font, this.leftPos + 62, this.topPos + 213, 48, 12, Component.empty());
        this.searchBox.setMaxLength(50);
        this.searchBox.setResponder(this::onSearch);
        this.searchBox.setBordered(false);

        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setFocused(true);
        this.addRenderableWidget(this.searchBox);

        updateNavButtons();
    }

    private void updateNavButtons() {
        this.clearWidgets();
        this.addRenderableWidget(this.searchBox);

        // Кнопка "Назад"
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(this.leftPos + 7, this.topPos + 107, 18, 18)
                .build()).active = currentPage > 0;

        // Кнопка "Вперед"
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(this.leftPos + 151, this.topPos + 107, 18, 18)
                .build()).active = currentPage < getPageCount() -1;
    }

    private void changePage(int delta) {
        this.currentPage += delta;
        updateNavButtons();
    }

    private void onSearch(String query) {
        filteredRecipes.clear();
        if (query.isEmpty()) {
            filteredRecipes.addAll(allRecipes);
        } else {
            String lowerQuery = query.toLowerCase(Locale.ROOT);
            for (ItemStack stack : allRecipes) {
                if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    filteredRecipes.add(stack);
                }
            }
        }
        this.currentPage = 0;
        updateNavButtons();
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.searchBox.isFocused()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 45, this.topPos + 211, 176, 54, 72, 12);
        }

        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);

        renderRecipes(guiGraphics, pMouseX, pMouseY);

        String pageText = (currentPage + 1) + "/" + getPageCount();
        guiGraphics.drawString(this.font, pageText, this.leftPos + this.imageWidth / 2 - this.font.width(pageText) / 2, this.topPos + 10, 0xFFFFFF);
    }

    private void renderRecipes(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int recipesPerPage = 35;
        int startIndex = currentPage * recipesPerPage;

        for (int i = 0; i < recipesPerPage; i++) {
            int recipeIndex = startIndex + i;
            if (recipeIndex >= filteredRecipes.size()) break;

            int row = i / 5;
            int col = i % 5;
            int x = this.leftPos + 25 + (col * 27);
            int y = this.topPos + 26 + (row * 27);

            ItemStack outputStack = filteredRecipes.get(recipeIndex);

            // Отрисовка фона кнопки
            boolean hovered = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
            guiGraphics.blit(TEXTURE, x, y, hovered ? 194 : 176, 0, 18, 18);

            // Отрисовка иконки
            guiGraphics.renderFakeItem(outputStack, x + 1, y + 1);

            // Отрисовка тултипа
            if (hovered) {
                // Создаем расширенный тултип для штампов
                if (isStamp(outputStack)) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(outputStack.getHoverName());

                    // Добавляем информацию о необходимости плоского штампа
                    if (this.minecraft != null && this.minecraft.player != null && !this.minecraft.player.isCreative()) {
                        NonNullList<Ingredient> cost =
                                TemplateCraftingCosts.getCostForStamp(outputStack);
                        if (cost != null && !cost.isEmpty()) {
                            // Получаем первый ингредиент (плоский штамп)
                            for (net.minecraft.world.item.crafting.Ingredient ingredient : cost) {
                                ItemStack[] items = ingredient.getItems();
                                if (items.length > 0) {
                                    tooltip.add(Component.translatable("tooltip.hbm_m.requires")
                                            .append(": ")
                                            .append(items[0].getHoverName())
                                            .withStyle(style -> style.withColor(0xAAAAAA)));
                                    break;
                                }
                            }
                        }
                    }

                    guiGraphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
                } else {
                    guiGraphics.renderTooltip(this.font, outputStack, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        // Клик по рецепту
        int recipesPerPage = 35;
        int startIndex = currentPage * recipesPerPage;
        for (int i = 0; i < recipesPerPage; i++) {
            int recipeIndex = startIndex + i;
            if (recipeIndex >= filteredRecipes.size()) break;

            int row = i / 5;
            int col = i % 5;
            int x = this.leftPos + 25 + (col * 27);
            int y = this.topPos + 26 + (row * 27);

            if (pMouseX >= x && pMouseX < x + 18 && pMouseY >= y && pMouseY < y + 18) {
                ModPacketHandler.INSTANCE.sendToServer(new GiveTemplateC2SPacket(filteredRecipes.get(recipeIndex)));
                this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (pDelta > 0 && currentPage > 0) {
            changePage(-1);
        } else if (pDelta < 0 && currentPage < getPageCount() - 1) {
            changePage(1);
        }
        return true;
    }

    private int getPageCount() {
        return Math.max(1, (int) Math.ceil(filteredRecipes.size() / 35.0));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Проверяет, является ли предмет штампом пресса
     */
    private boolean isStamp(ItemStack stack) {
        return TemplateCraftingCosts.isStamp(stack);
    }
}