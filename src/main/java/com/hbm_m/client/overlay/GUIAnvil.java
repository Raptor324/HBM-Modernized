package com.hbm_m.client.overlay;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.AnvilMenu;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GUIAnvil extends AbstractContainerScreen<AnvilMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/anvil_gui.png");
    private List<AnvilRecipe> displayedRecipes = new ArrayList<>();
    private int recipeScrollIndex = 0;
    private AnvilRecipe selectedRecipe = null;
    private EditBox searchBox;
    private Button craftButton;
    private Button leftButton;
    private Button rightButton;

    public GUIAnvil(AnvilMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageWidth = 256;
        this.imageHeight = 166;
        displayedRecipes = AnvilRecipeManager.getAllRecipes();
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Поле поиска
        searchBox = new EditBox(this.font, x + 20, y + 65, 100, 16, Component.literal("Search"));
        searchBox.setMaxLength(50);
        searchBox.setBordered(true);
        searchBox.setVisible(true);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        // Кнопка крафта
        craftButton = Button.builder(Component.literal("Craft"), button -> {
            boolean shiftPressed = hasShiftDown();
            menu.tryCraft(minecraft.player, shiftPressed);
        })
        .bounds(x + 50, y + 45, 50, 20)
        .build();
        this.addRenderableWidget(craftButton);

        // Кнопка влево
        leftButton = Button.builder(Component.literal("<"), button -> {
            if (recipeScrollIndex > 0) {
                recipeScrollIndex -= 10;
                updateSelectedRecipe();
            }
        })
        .bounds(x + 20, y + 30, 20, 20)
        .build();
        this.addRenderableWidget(leftButton);

        // Кнопка вправо
        rightButton = Button.builder(Component.literal(">"), button -> {
            if (recipeScrollIndex + 10 < displayedRecipes.size()) {
                recipeScrollIndex += 10;
                updateSelectedRecipe();
            }
        })
        .bounds(x + 100, y + 30, 20, 20)
        .build();
        this.addRenderableWidget(rightButton);

        updateSelectedRecipe();
    }

    // ИСПРАВЛЕНО: добавлен метод поиска с фильтрацией по имени предмета
    private void onSearchChanged(String query) {
        if (query == null || query.trim().isEmpty()) {
            displayedRecipes = AnvilRecipeManager.getAllRecipes();
        } else {
            String lowerQuery = query.toLowerCase();
            displayedRecipes = AnvilRecipeManager.getAllRecipes().stream()
                .filter(recipe -> {
                    ItemStack output = recipe.getResultItem(RegistryAccess.EMPTY);
                    String itemName = output.getHoverName().getString().toLowerCase();
                    return itemName.contains(lowerQuery);
                })
                .collect(Collectors.toList());
        }
        recipeScrollIndex = 0;
        updateSelectedRecipe();
    }

    private void updateSelectedRecipe() {
        if (!displayedRecipes.isEmpty() && recipeScrollIndex < displayedRecipes.size()) {
            selectedRecipe = displayedRecipes.get(recipeScrollIndex);
        } else {
            selectedRecipe = null;
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Рисуем список рецептов
        renderRecipeList(guiGraphics, x, y);

        // Рисуем требуемые ресурсы
        renderRequiredResources(guiGraphics, x, y);
    }

    private void renderRecipeList(GuiGraphics guiGraphics, int x, int y) {
        int startX = x + 45;
        int startY = y + 30;
        int index = recipeScrollIndex;

        // Рисуем 2 ряда по 5 рецептов
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                if (index < displayedRecipes.size()) {
                    AnvilRecipe recipe = displayedRecipes.get(index);
                    // ИСПРАВЛЕНО: использование getResultItem вместо getOutput
                    ItemStack output = recipe.getResultItem(RegistryAccess.EMPTY);
                    int posX = startX + col * 18;
                    int posY = startY + row * 18;

                    guiGraphics.renderItem(output, posX, posY);
                    guiGraphics.renderItemDecorations(this.font, output, posX, posY);
                    index++;
                }
            }
        }
    }

    private void renderRequiredResources(GuiGraphics guiGraphics, int x, int y) {
        if (selectedRecipe == null) return;

        int startX = x + 180;
        int startY = y + 20;

        // Заголовок "Inputs:"
        guiGraphics.drawString(this.font, "Inputs:", startX, startY, 0xFFFFFF, false);
        startY += 12;

        // Рисуем требуемые ресурсы
        int index = 0;
        for (ItemStack required : selectedRecipe.getRequiredItems()) {
            int posX = startX + (index % 3) * 18;
            int posY = startY + (index / 3) * 18;

            guiGraphics.renderItem(required, posX, posY);
            guiGraphics.renderItemDecorations(this.font, required, posX, posY);
            index++;
        }

        startY += ((index / 3) + 1) * 18 + 10;

        // Заголовок "Outputs:"
        guiGraphics.drawString(this.font, "Outputs:", startX, startY, 0xFFFFFF, false);
        startY += 12;

        // ИСПРАВЛЕНО: использование getResultItem вместо getOutput
        ItemStack output = selectedRecipe.getResultItem(RegistryAccess.EMPTY);
        guiGraphics.renderItem(output, startX, startY);
        guiGraphics.renderItemDecorations(this.font, output, startX, startY);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем клик по рецептам
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int startX = x + 45;
        int startY = y + 30;

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int index = recipeScrollIndex + row * 5 + col;
                if (index < displayedRecipes.size()) {
                    int posX = startX + col * 18;
                    int posY = startY + row * 18;

                    if (mouseX >= posX && mouseX < posX + 16 && mouseY >= posY && mouseY < posY + 16) {
                        selectedRecipe = displayedRecipes.get(index);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
