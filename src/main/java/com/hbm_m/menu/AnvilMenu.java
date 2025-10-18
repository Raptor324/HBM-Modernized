package com.hbm_m.menu;

import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.ModRecipes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class AnvilMenu extends AbstractContainerMenu {
    private final SimpleContainer anvilInventory = new SimpleContainer(3); // Слоты 0, 1, 2
    private final Level level;
    private List<AnvilRecipe> allRecipes;
    private List<AnvilRecipe> filteredRecipes;
    private int scrollPosition = 0;
    private String searchQuery = "";

    // Конструктор для сервера
    public AnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player.level());
    }

    // Конструктор для клиента (из пакета)
    public AnvilMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level());
    }

    private AnvilMenu(int containerId, Inventory playerInventory, Level level) {
        super(ModMenuTypes.ANVIL_MENU.get(), containerId);
        this.level = level;

        // Слоты наковальни
        this.addSlot(new Slot(anvilInventory, 0, 10, 116)); // Слот A (inputA)
        this.addSlot(new Slot(anvilInventory, 1, 46, 116)); // Слот B (inputB)
        this.addSlot(new Slot(anvilInventory, 2, 60, 116) { // Слот C (output)
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Только вывод
            }
        });

        // Инвентарь игрока
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 154 + i * 18));
            }
        }

        // Хотбар игрока
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 209));
        }

        // Загрузка рецептов
        loadRecipes();
    }

    private void loadRecipes() {
        if (level != null) {
            allRecipes = level.getRecipeManager()
                    .getAllRecipesFor(ModRecipes.ANVIL_RECIPE_TYPE.get());
            filteredRecipes = new ArrayList<>(allRecipes);
        } else {
            allRecipes = new ArrayList<>();
            filteredRecipes = new ArrayList<>();
        }
    }

    public List<AnvilRecipe> getVisibleRecipes() {
        int startIndex = scrollPosition * 5; // 2 ряда по 5 = 10 предметов
        int endIndex = Math.min(startIndex + 10, filteredRecipes.size());

        if (startIndex >= filteredRecipes.size()) {
            return new ArrayList<>();
        }

        return filteredRecipes.subList(startIndex, endIndex);
    }

    public void scrollLeft() {
        if (scrollPosition > 0) {
            scrollPosition--;
        }
    }

    public void scrollRight() {
        int maxScroll = (filteredRecipes.size() - 1) / 10;
        if (scrollPosition < maxScroll) {
            scrollPosition++;
        }
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query.toLowerCase();
        filterRecipes();
        scrollPosition = 0;
    }

    private void filterRecipes() {
        if (searchQuery.isEmpty()) {
            filteredRecipes = new ArrayList<>(allRecipes);
        } else {
            filteredRecipes = allRecipes.stream()
                    .filter(recipe -> {
                        String inputAName = recipe.getInputA().getHoverName().getString().toLowerCase();
                        String inputBName = recipe.getInputB().getHoverName().getString().toLowerCase();
                        String outputName = recipe.getOutput().getHoverName().getString().toLowerCase();

                        return inputAName.contains(searchQuery) ||
                                inputBName.contains(searchQuery) ||
                                outputName.contains(searchQuery);
                    })
                    .collect(Collectors.toList());
        }
    }

    public void craftItem(AnvilRecipe recipe, boolean craftAll) {
        Player player = level.players().stream()
                .filter(p -> p.containerMenu == this)
                .findFirst()
                .orElse(null);

        if (player == null) return;

        int maxCrafts = craftAll ? 64 : 1;
        int actualCrafts = 0;

        // Проверяем, сколько раз можем скрафтить
        for (int i = 0; i < maxCrafts; i++) {
            if (canCraft(player, recipe)) {
                actualCrafts++;
            } else {
                break;
            }
        }

        // Выполняем крафты
        for (int i = 0; i < actualCrafts; i++) {
            performCraft(player, recipe);
        }
    }

    private boolean canCraft(Player player, AnvilRecipe recipe) {
        // Проверяем наличие всех требуемых предметов
        for (ItemStack required : recipe.getRequiredItems()) {
            if (!hasItem(player, required)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasItem(Player player, ItemStack required) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, required)) {
                count += stack.getCount();
                if (count >= required.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void performCraft(Player player, AnvilRecipe recipe) {
        // Удаляем требуемые предметы
        for (ItemStack required : recipe.getRequiredItems()) {
            removeItem(player, required);
        }

        // Добавляем результат
        ItemStack result = recipe.getOutput().copy();
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }

    private void removeItem(Player player, ItemStack toRemove) {
        int remaining = toRemove.getCount();
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(stack, toRemove)) {
                int removeCount = Math.min(remaining, stack.getCount());
                stack.shrink(removeCount);
                remaining -= removeCount;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index < 3) {
                // Из наковальни в инвентарь
                if (!this.moveItemStackTo(slotStack, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Из инвентаря в наковальню
                if (!this.moveItemStackTo(slotStack, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // Добавьте проверку расстояния до блока если нужно
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            // Возвращаем предметы из слотов A и B
            for (int i = 0; i < 2; i++) {
                ItemStack stack = anvilInventory.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
        }
    }
}