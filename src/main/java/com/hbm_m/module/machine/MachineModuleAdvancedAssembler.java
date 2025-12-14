package com.hbm_m.module.machine;

import com.hbm_m.api.energy.IEnergyReceiver;
import com.hbm_m.recipe.AssemblerRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

// ИЗМЕНЕНИЕ: Импортируем нашу long-систему

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Модуль крафта для продвинутой сборочной машины.
 * Реализует логику обработки AssemblerRecipe.
 *
 * ОБНОВЛЕНО: Теперь использует ILongEnergyStorage для поддержки больших значений энергии
 */
public class MachineModuleAdvancedAssembler extends MachineModuleBase<AssemblerRecipe> {

    // ИЗМЕНЕНИЕ: Используем long вместо int
    private long energyPerTick = 100L;

    @Nullable
    private AssemblerRecipe preferredRecipe = null;

    // ИЗМЕНЕНИЕ: Конструктор теперь принимает ILongEnergyStorage
    public MachineModuleAdvancedAssembler(int moduleIndex, IEnergyReceiver energyStorage,
                                          IItemHandler itemHandler, Level level) {
        super(moduleIndex, energyStorage, itemHandler, level);

        // Настройка по умолчанию: 12 входных (4-15), 1 выходной (16)
        this.inputSlots = new int[12];
        for (int i = 0; i < 12; i++) {
            this.inputSlots[i] = 4 + i;
        }
        this.outputSlots = new int[] { 16 };
    }

    // ========== BUILDER METHODS ==========

    public MachineModuleAdvancedAssembler setInputSlots(int startSlot, int count) {
        this.inputSlots = new int[count];
        for (int i = 0; i < count; i++) {
            this.inputSlots[i] = startSlot + i;
        }
        return this;
    }

    public MachineModuleAdvancedAssembler setOutputSlot(int slot) {
        this.outputSlots = new int[] { slot };
        return this;
    }

    // ИЗМЕНЕНИЕ: Принимаем long
    public MachineModuleAdvancedAssembler setEnergyPerTick(long energy) {
        this.energyPerTick = energy;
        return this;
    }

    // ========== РЕАЛИЗАЦИЯ АБСТРАКТНЫХ МЕТОДОВ ==========

    @Override
    protected AssemblerRecipe.Type getRecipeType() {
        return AssemblerRecipe.Type.INSTANCE;
    }

    @Override
    @Nullable
    public AssemblerRecipe findRecipeForInputs() {
        if (level == null) return null;

        // Если preferredRecipe установлен, проверяем его
        if (preferredRecipe != null) {
            if (matchesRecipe(preferredRecipe)) {
                return preferredRecipe;
            } else {
                return null;
            }
        }

        // Читаем папку из слота 1
        ItemStack blueprint = itemHandler.getStackInSlot(1);

        // Автоматический поиск с учетом blueprint pool
        RecipeManager recipeManager = level.getRecipeManager();
        List<AssemblerRecipe> allRecipes = recipeManager.getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);

        for (AssemblerRecipe recipe : allRecipes) {
            // Проверяем и ингредиенты, И blueprint pool
            if (matchesRecipe(recipe) && isRecipeValidForBlueprint(recipe, blueprint)) {
                return recipe;
            }
        }

        return null;
    }

    /**
     * Проверяет, соответствует ли инвентарь данному рецепту.
     */
    private boolean matchesRecipe(AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        // Создаём копию входных предметов
        ItemStack[] inputCopy = new ItemStack[inputSlots.length];
        for (int i = 0; i < inputSlots.length; i++) {
            inputCopy[i] = itemHandler.getStackInSlot(inputSlots[i]).copy();
        }

        // Проверяем, что все ингредиенты присутствуют
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < inputCopy.length; i++) {
                if (!inputCopy[i].isEmpty() && ingredient.test(inputCopy[i])) {
                    inputCopy[i].shrink(1); // Убираем один предмет
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return true;
    }

    public void setPreferredRecipe(@Nullable AssemblerRecipe recipe) {
        this.preferredRecipe = recipe;

        if (currentRecipe != null && recipe != null
                && !Objects.equals(currentRecipe.getId(), recipe.getId())) {
            resetProgress();
        }
    }

    @Nullable
    public AssemblerRecipe getPreferredRecipe() {
        return this.preferredRecipe;
    }

    @Override
    protected boolean matchesCurrentRecipe(AssemblerRecipe recipe) {
        return matchesRecipe(recipe);
    }

    @Override
    public boolean canProcess(AssemblerRecipe recipe) {
        if (recipe == null) return false;

        // ИЗМЕНЕНИЕ: Работаем с long
        if (energyStorage.getEnergyStored() < this.energyPerTick) return false;

        // Проверяем входные предметы
        if (!matchesRecipe(recipe)) return false;

        // Проверяем выходной слот
        ItemStack outputSlot = itemHandler.getStackInSlot(outputSlots[0]);
        ItemStack result = recipe.getResultItem(level.registryAccess());

        if (outputSlot.isEmpty()) return true; // Слот пуст - ОК

        // Проверяем совместимость
        if (!ItemStack.isSameItemSameTags(outputSlot, result)) return false;

        // Проверяем, поместится ли результат
        return outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize();
    }

    @Override
    protected void processCraft(AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        // Забираем входные предметы
        for (Ingredient ingredient : ingredients) {
            for (int slot : inputSlots) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    itemHandler.extractItem(slot, 1, false);
                    break;
                }
            }
        }

        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();

        // insertItem автоматически объединит стаки, если возможно
        ItemStack remainder = itemHandler.insertItem(outputSlots[0], result, false);

        // Если remainder не пустой - слот переполнен (не должно произойти, т.к. canProcess проверяет)
        if (!remainder.isEmpty()) {
            // Логируем ошибку или выбрасываем предмет в мир
            if (level != null && !level.isClientSide()) {
                System.err.println("ОШИБКА: Не удалось вставить результат крафта!");
            }
        }
    }

    @Override
    protected int getRecipeDuration(AssemblerRecipe recipe) {
        return recipe.getDuration();
    }

    // ИЗМЕНЕНИЕ: Возвращаем long вместо int
    @Override
    protected long getRecipeEnergyCost(AssemblerRecipe recipe) {
        return recipe != null ? recipe.getPowerConsumption() : this.energyPerTick;
    }

    @Override
    @Nullable
    protected AssemblerRecipe findRecipeForItem(ItemStack stack) {
        if (level == null) return null;

        List<AssemblerRecipe> allRecipes = level.getRecipeManager()
                .getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);

        for (AssemblerRecipe recipe : allRecipes) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.test(stack)) {
                    return recipe;
                }
            }
        }

        return null;
    }

    /**
     * Реализация проверки blueprint pool
     * Использует AssemblerRecipeConfig для валидации
     */
    @Override
    protected boolean isRecipeValidForBlueprint(AssemblerRecipe recipe, ItemStack blueprint) {
        // Получаем pool рецепта
        String recipePool = recipe.getBlueprintPool();

        // Базовые рецепты (без pool) - всегда доступны
        if (recipePool == null || recipePool.isEmpty()) {
            return true;
        }

        // Если blueprint пустой - рецепты с pool НЕ доступны
        if (blueprint == null || blueprint.isEmpty()) {
            return false;
        }

        // Получаем pool из blueprint
        CompoundTag nbt = blueprint.getTag();
        if (nbt == null || !nbt.contains("blueprintPool")) {
            return false;
        }

        String blueprintPool = nbt.getString("blueprintPool");

        // Проверяем совпадение pool
        return recipePool.equals(blueprintPool);
    }
}