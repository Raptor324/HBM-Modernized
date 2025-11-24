package com.hbm_m.client.overlay;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.AnvilMenu;
import com.hbm_m.network.AnvilCraftC2SPacket;
import com.hbm_m.network.AnvilSelectRecipeC2SPacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraftforge.items.ItemStackHandler;

public class GUIAnvil extends AbstractContainerScreen<AnvilMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/processing/gui_anvil.png");

    private static final int DISPLAY_SLOTS = 10;

    private final List<AnvilRecipe> originRecipes = new ArrayList<>();
    private final List<AnvilRecipe> filteredRecipes = new ArrayList<>();

    private EditBox searchBox;
    private int columnOffset;
    private int maxColumnOffset;
    private int selectionIndex = -1;
    private int lastTextWidth;
    private ItemStack hoveredRecipeStack = ItemStack.EMPTY;

    public GUIAnvil(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        loadAvailableRecipes();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        searchBox = new EditBox(this.font, x + 10, y + 111, 84, 12,
                Component.translatable("gui.hbm_m.anvil.search"));
        searchBox.setMaxLength(25);
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setTextColorUneditable(0xFFFFFFFF);
        searchBox.setResponder(this::applySearch);
        searchBox.setSuggestion(Component.translatable("gui.hbm_m.anvil.search_hint").getString());
        searchBox.setValue("");

        this.addRenderableWidget(searchBox);

        // Инициализация с полным списком
        applySearch("");
    }

    /**
     * Загружает доступные рецепты с учетом tier наковальни
     */
    private void loadAvailableRecipes() {
        originRecipes.clear();
        RegistryAccess access = getRegistryAccess();

        originRecipes.addAll(
                AnvilRecipeManager.getClientRecipes().stream()
                        .filter(recipe -> recipe.canCraftOn(menu.blockEntity.getTier()))
                        .sorted(Comparator.comparing(recipe ->
                                recipe.getResultItem(access).getHoverName().getString().toLowerCase(Locale.ROOT)))
                        .toList()
        );
    }

    /**
     * Применяет поисковый фильтр к рецептам
     */
    private void applySearch(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        filteredRecipes.clear();

        if (query.isEmpty()) {
            filteredRecipes.addAll(originRecipes);
        } else {
            RegistryAccess access = getRegistryAccess();
            for (AnvilRecipe recipe : originRecipes) {
                // Поиск по имени результата и входных материалов
                List<String> searchTerms = buildSearchTerms(recipe, access);
                for (String term : searchTerms) {
                    if (term.contains(query)) {
                        filteredRecipes.add(recipe);
                        break;
                    }
                }
            }
        }

        resetPagination();
        syncSelectionToServer(false);
    }

    /**
     * Создает список поисковых терминов для рецепта
     */
    private List<String> buildSearchTerms(AnvilRecipe recipe, RegistryAccess access) {
        List<String> terms = new ArrayList<>();

        // Добавляем имя выходного предмета
        ItemStack output = recipe.getResultItem(access);
        if (!output.isEmpty()) {
            try {
                terms.add(output.getHoverName().getString().toLowerCase(Locale.ROOT));
            } catch (Exception ex) {
                terms.add("error");
            }
        }

        // Добавляем имена входных предметов
        for (ItemStack input : List.of(recipe.getInputA(), recipe.getInputB())) {
            if (!input.isEmpty()) {
                try {
                    terms.add(input.getHoverName().getString().toLowerCase(Locale.ROOT));
                } catch (Exception ex) {
                    terms.add("error");
                }
            }
        }

        // Добавляем дополнительные требуемые предметы
        for (ItemStack required : recipe.getRequiredItems()) {
            if (!required.isEmpty()) {
                try {
                    terms.add(required.getHoverName().getString().toLowerCase(Locale.ROOT));
                } catch (Exception ex) {
                    terms.add("error");
                }
            }
        }

        return terms;
    }

    /**
     * Сброс пагинации
     */
    private void resetPagination() {
        this.columnOffset = 0;
        this.selectionIndex = -1;
        recalculateBounds();
    }

    /**
     * Пересчет границ прокрутки
     */
    private void recalculateBounds() {
        int size = filteredRecipes.size();
        double raw = (size - DISPLAY_SLOTS) / 2.0D;
        maxColumnOffset = Math.max(0, (int) Math.ceil(raw));
        columnOffset = Mth.clamp(columnOffset, 0, maxColumnOffset);

        if (selectionIndex >= size) {
            selectionIndex = -1;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncSelectionToServer(false);
    }

    /**
     * Синхронизация выбранного рецепта с сервером
     */
    private void syncSelectionToServer(boolean forceDefault) {
        menu.blockEntity.getSelectedRecipeId().ifPresentOrElse(id -> {
            int index = findRecipeIndex(id);
            if (index >= 0 && index < filteredRecipes.size()) {
                selectionIndex = index;
                // Прокручиваем к выбранному рецепту
                columnOffset = Mth.clamp(index / 2, 0, maxColumnOffset);
            } else if (forceDefault && !filteredRecipes.isEmpty()) {
                selectionIndex = 0;
                notifyServerAboutSelection(filteredRecipes.get(0));
            }
        }, () -> {
            if (forceDefault && !filteredRecipes.isEmpty()) {
                selectionIndex = 0;
                notifyServerAboutSelection(filteredRecipes.get(0));
            } else if (!forceDefault) {
                selectionIndex = -1;
            }
        });
    }

    /**
     * Ищет индекс рецепта по его ID
     */
    private int findRecipeIndex(ResourceLocation id) {
        for (int i = 0; i < filteredRecipes.size(); i++) {
            if (filteredRecipes.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Внешний метод для фокусировки на рецепте (для NEI/JEI интеграции)
     */
    public void focusRecipe(@Nullable AnvilRecipe target) {
        if (target == null || !originRecipes.contains(target)) {
            return;
        }

        searchBox.setValue("");
        applySearch("");

        int pos = filteredRecipes.indexOf(target);
        if (pos >= 0) {
            selectionIndex = pos;
            columnOffset = Mth.clamp(pos / 2, 0, maxColumnOffset);
            notifyServerAboutSelection(target);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (isOverLeftArrow(mouseX, mouseY)) {
            playClickSound();
            scrollColumns(-1);
            return true;
        }

        if (isOverRightArrow(mouseX, mouseY)) {
            playClickSound();
            scrollColumns(1);
            return true;
        }

        if (isOverCraftButton(mouseX, mouseY) && selectionIndex >= 0) {
            playClickSound();
            boolean craftAll = hasShiftDown();
            ModPacketHandler.INSTANCE.sendToServer(
                    new AnvilCraftC2SPacket(menu.blockEntity.getBlockPos(), craftAll));
            return true;
        }

        if (isOverSearchButton(mouseX, mouseY)) {
            playClickSound();
            applySearch(searchBox.getValue());
            return true;
        }

        if (handleRecipeClick(mouseX, mouseY)) {
            playClickSound();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Воспроизводит звук нажатия кнопки
     */
    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    /**
     * Прокрутка столбцов рецептов
     */
    private void scrollColumns(int delta) {
        columnOffset = Mth.clamp(columnOffset + delta, 0, maxColumnOffset);
    }

    /**
     * Обработка клика по рецепту
     */
    private boolean handleRecipeClick(double mouseX, double mouseY) {
        int startIndex = columnOffset * 2;
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;

        for (int slot = 0; slot < DISPLAY_SLOTS; slot++) {
            int recipeIndex = startIndex + slot;
            if (recipeIndex >= filteredRecipes.size()) {
                break;
            }

            int col = slot / 2;
            int row = slot % 2;
            int overlayX = guiLeft + 16 + col * 18;
            int overlayY = guiTop + 71 + row * 18;

            if (mouseX >= overlayX && mouseX < overlayX + 18 &&
                    mouseY >= overlayY && mouseY < overlayY + 18) {

                if (selectionIndex == recipeIndex) {
                    // Отмена выбора
                    selectionIndex = -1;
                    notifyServerAboutSelection(null);
                } else {
                    // Выбор рецепта
                    selectionIndex = recipeIndex;
                    notifyServerAboutSelection(filteredRecipes.get(recipeIndex));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Уведомляет сервер о выбранном рецепте
     */
    private void notifyServerAboutSelection(@Nullable AnvilRecipe recipe) {
        ResourceLocation id = recipe != null ? recipe.getId() : null;
        menu.blockEntity.setSelectedRecipeId(id);
        ModPacketHandler.INSTANCE.sendToServer(
                new AnvilSelectRecipeC2SPacket(menu.blockEntity.getBlockPos(), id));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isOverRecipeGrid(mouseX, mouseY)) {
            if (delta > 0) {
                scrollColumns(-1);
            } else if (delta < 0) {
                scrollColumns(1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }


    // === Проверки позиций мыши ===

    private boolean isOverRecipeGrid(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 16 && mouseX < guiLeft + 106 &&
                mouseY >= guiTop + 71 && mouseY < guiTop + 107;
    }

    private boolean isOverLeftArrow(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 7 && mouseX < guiLeft + 16 &&
                mouseY >= guiTop + 71 && mouseY < guiTop + 107;
    }

    private boolean isOverRightArrow(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 106 && mouseX < guiLeft + 115 &&
                mouseY >= guiTop + 71 && mouseY < guiTop + 107;
    }

    private boolean isOverCraftButton(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 52 && mouseX < guiLeft + 70 &&
                mouseY >= guiTop + 53 && mouseY < guiTop + 71;
    }

    private boolean isOverSearchButton(double mouseX, double mouseY) {
        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;
        return mouseX >= guiLeft + 97 && mouseX < guiLeft + 115 &&
                mouseY >= guiTop + 107 && mouseY < guiTop + 125;
    }

    // === Отрисовка ===

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        hoveredRecipeStack = ItemStack.EMPTY;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int guiLeft = (width - imageWidth) / 2;
        int guiTop = (height - imageHeight) / 2;

        // Основная текстура GUI
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, imageWidth, imageHeight);

        renderSearchFieldBackground(guiGraphics, guiLeft, guiTop);

        // Подсветка кнопок при наведении
        drawButtonHighlights(guiGraphics, guiLeft, guiTop, mouseX, mouseY);

        // Отрисовка сетки рецептов
        renderRecipeGrid(guiGraphics, guiLeft, guiTop, mouseX, mouseY);

        // Отрисовка деталей рецепта
        renderRecipeDetails(guiGraphics, guiLeft, guiTop);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Заголовок GUI
        Component titleText = Component.translatable("container.hbm_m.anvil",
                menu.blockEntity.getTier());
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, (imageWidth - titleWidth) / 2, 6, 0x404040, false);

        // Название инвентаря
        Component inventoryLabel = Component.translatable("container.inventory");
        guiGraphics.drawString(this.font, inventoryLabel, 8, imageHeight - 96 + 2, 0x404040, false);

        Component searchLabel = Component.translatable("gui.hbm_m.anvil.search");
        guiGraphics.drawString(this.font, searchLabel, 10, 100, 0x606060, false);
    }

    /**
     * Подсветка кнопок при наведении
     */
    private void drawButtonHighlights(GuiGraphics guiGraphics, int guiLeft, int guiTop, int mouseX, int mouseY) {
        if (isOverLeftArrow(mouseX, mouseY) && columnOffset > 0) {
            guiGraphics.blit(TEXTURE, guiLeft + 7, guiTop + 71, 176, 186, 9, 36);
        }

        if (isOverRightArrow(mouseX, mouseY) && columnOffset < maxColumnOffset) {
            guiGraphics.blit(TEXTURE, guiLeft + 106, guiTop + 71, 185, 186, 9, 36);
        }

        if (isOverCraftButton(mouseX, mouseY) && selectionIndex >= 0) {
            guiGraphics.blit(TEXTURE, guiLeft + 52, guiTop + 53, 176, 150, 18, 18);
        }

        if (isOverSearchButton(mouseX, mouseY)) {
            guiGraphics.blit(TEXTURE, guiLeft + 97, guiTop + 107, 176, 168, 18, 18);
        }
    }

    private void renderSearchFieldBackground(GuiGraphics guiGraphics, int guiLeft, int guiTop) {
        if (searchBox == null) {
            return;
        }
        int left = guiLeft + 8;
        int top = guiTop + 108;
        int right = left + 88;
        int bottom = top + 16;
        int borderColor = searchBox.isFocused() ? 0xFF6BE5FF : 0xFF4F4F4F;
        int fillColor = searchBox.isFocused() ? 0xAA111111 : 0x66101010;
        guiGraphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xAA000000);
        guiGraphics.fill(left, top, right, bottom, fillColor);
        guiGraphics.fill(left - 1, top - 1, right + 1, top, borderColor);
        guiGraphics.fill(left - 1, bottom, right + 1, bottom + 1, borderColor);
        guiGraphics.fill(left - 1, top, left, bottom, borderColor);
        guiGraphics.fill(right, top, right + 1, bottom, borderColor);
    }

    /**
     * Отрисовка сетки рецептов
     */
    private void renderRecipeGrid(GuiGraphics guiGraphics, int guiLeft, int guiTop, int mouseX, int mouseY) {
        int startIndex = columnOffset * 2;

        for (int slot = 0; slot < DISPLAY_SLOTS; slot++) {
            int recipeIndex = startIndex + slot;
            if (recipeIndex >= filteredRecipes.size()) {
                break;
            }

            int col = slot / 2;
            int row = slot % 2;
            int itemX = guiLeft + 17 + col * 18;
            int itemY = guiTop + 72 + row * 18;
            int overlayX = itemX - 1;
            int overlayY = itemY - 1;

            AnvilRecipe recipe = filteredRecipes.get(recipeIndex);
            ItemStack displayStack = recipe.getDisplayStack();

            // Отрисовка предмета
            guiGraphics.renderItem(displayStack, itemX, itemY);
            guiGraphics.renderItemDecorations(this.font, displayStack, itemX, itemY);

            // Оверлей (tier/type индикатор)
            int overlayU = 18 + 18 * recipe.getOverlay().ordinal();
            guiGraphics.blit(TEXTURE, overlayX, overlayY, overlayU, 222, 18, 18);

            // Рамка выбора
            if (recipeIndex == selectionIndex) {
                guiGraphics.blit(TEXTURE, overlayX, overlayY, 0, 222, 18, 18);
            }

            // Сохранение стека для тултипа
            if (mouseX >= overlayX && mouseX < overlayX + 18 &&
                    mouseY >= overlayY && mouseY < overlayY + 18) {
                hoveredRecipeStack = displayStack;
            }
        }
    }

    /**
     * Отрисовка деталей выбранного рецепта
     */
    private void renderRecipeDetails(GuiGraphics guiGraphics, int guiLeft, int guiTop) {
        List<Component> lines = buildRecipeDetails();

        if (lines.isEmpty()) {
            lastTextWidth = 0;
            return;
        }

        // Вычисляем максимальную ширину текста
        int longest = 0;
        for (Component line : lines) {
            longest = Math.max(longest, this.font.width(line));
        }

        float scale = 0.5F;
        lastTextWidth = (int) (longest * scale);

        // Расширяющаяся панель для длинного текста
        int slide = Mth.clamp(lastTextWidth - 42, 0, 1000);
        for (int mul = 1; slide >= 51 * mul; mul++) {
            guiGraphics.blit(TEXTURE, guiLeft + 125 + 51 * mul, guiTop + 17, 125, 17, 54, 108);
        }
        guiGraphics.blit(TEXTURE, guiLeft + 125 + slide, guiTop + 17, 125, 17, 54, 108);

        // Отрисовка текста с масштабированием
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.pose().translate((guiLeft + 130) / scale, (guiTop + 25) / scale, 0.0F);

        int yOffset = 0;
        for (Component line : lines) {
            guiGraphics.drawString(this.font, line, 0, yOffset, 0xFFFFFF, false);
            yOffset += 9;
        }

        guiGraphics.pose().popPose();
    }

    /**
     * Формирует детальное описание выбранного рецепта
     */
    private List<Component> buildRecipeDetails() {
        if (selectionIndex < 0 || selectionIndex >= filteredRecipes.size() ||
                minecraft == null || minecraft.player == null) {
            return List.of();
        }

        AnvilRecipe recipe = filteredRecipes.get(selectionIndex);
        List<Component> lines = new ArrayList<>();

        // Заголовок входов
        lines.add(Component.translatable("gui.hbm_m.anvil.inputs")
                .withStyle(ChatFormatting.YELLOW));

        appendIngredientLines(lines, List.of(recipe.getInputA(), recipe.getInputB()), IngredientSource.MACHINE);
        appendIngredientLines(lines, recipe.getRequiredItems(), IngredientSource.PLAYER);

        // Разделитель
        lines.add(Component.empty());

        // Заголовок выходов
        lines.add(Component.translatable("gui.hbm_m.anvil.outputs")
                .withStyle(ChatFormatting.YELLOW));

        // Выходной предмет
        ItemStack output = recipe.getResultItem(getRegistryAccess());
        float chance = recipe.getOutputChance();
        String chanceText = chance < 1.0F ? " (" + (int)(chance * 100) + "%)" : "";

        lines.add(Component.literal("> " + output.getCount() + "x " +
                output.getHoverName().getString() + chanceText));

        return lines;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        hoveredRecipeStack = ItemStack.EMPTY;
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Тултип для рецепта при наведении
        if (!hoveredRecipeStack.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hoveredRecipeStack, mouseX, mouseY);
        }

        // Отрисовка поля поиска
        searchBox.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            playClickSound();
            applySearch(searchBox.getValue());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SLASH && !searchBox.isFocused()) {
            searchBox.setFocused(true);
            return true;
        }
        if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            applySearch(searchBox.getValue());
            return true;
        }
        if (searchBox.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox.charTyped(codePoint, modifiers)) {
            applySearch(searchBox.getValue());
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    /**
     * Подсчитывает количество предметов в инвентаре игрока
     */
    private int getPlayerItemCount(ItemStack stack) {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }

        int count = 0;
        Inventory inventory = minecraft.player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack invStack = inventory.getItem(i);
            if (ItemStack.isSameItemSameTags(invStack, stack)) {
                count += invStack.getCount();
            }
        }

        return count;
    }

    private void appendIngredientLines(List<Component> lines, List<ItemStack> stacks, IngredientSource source) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }

            int owned = source == IngredientSource.MACHINE
                    ? getMachineItemCount(stack)
                    : getPlayerItemCount(stack);
            boolean hasEnough = owned >= stack.getCount();

            Component line = Component.literal("> " + stack.getCount() + "x " +
                    stack.getHoverName().getString());
            line = line.copy().withStyle(hasEnough ? ChatFormatting.WHITE : ChatFormatting.RED);
            lines.add(line);
        }
    }

    private int getMachineItemCount(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ItemStackHandler handler = menu.blockEntity.getItemHandler();
        int count = 0;
        int slotLimit = Math.min(handler.getSlots(), 2);
        for (int slot = 0; slot < slotLimit; slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(slotStack, stack)) {
                count += slotStack.getCount();
            }
        }
        return count;
    }

    private enum IngredientSource {
        MACHINE,
        PLAYER
    }

    private RegistryAccess getRegistryAccess() {
        return minecraft != null && minecraft.level != null ?
                minecraft.level.registryAccess() : RegistryAccess.EMPTY;
    }

    @Override
    public void removed() {
        super.removed();
        // Отключаем повтор клавиш при закрытии GUI (аналог Keyboard.enableRepeatEvents(false))
    }
}
