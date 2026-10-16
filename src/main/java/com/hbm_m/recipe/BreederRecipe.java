package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Datapack-facing рецепт реактора-размножителя ({@code hbm_m:breeder}).
 *
 * <p>Порт статического {@code com.hbm_m.recipe.BreederRecipes} (Java-порт 1.7.10
 * {@code com.hbm.inventory.recipes.BreederRecipes}): один предметный вход → один предметный
 * выход за фиксированные 400 тиков, с энергопотреблением {@code energy_per_tick} HE/тик
 * (число «flux» из оригинала, переиспользованное 1:1 как FE/тик).</p>
 *
 * <p><b>Материальные подстановки</b> (оригинал оперирует {@code ItemBreedingRod} мета-предметами,
 * которые не портированы; каждый рецепт перемаплен на ближайший существующий слиток):
 * Co→Co60, Ra226→Ac227, Th232→ThF~Th, U235→Np237, Np237→Pu238, Pu238→Pu239,
 * U238/U→Pu-Mix («reactor grade plutonium»), Pu-Mix→nuclear waste. Не портированы (как и ранее):
 * Li→T (тритий только как жидкость) и easter-egg с метеоритным мечом.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code BreederRecipes} хранил {@code Map<Item, BreederRecipe>}
 * в Java-статике, что блокировало data-driven рецепты. Теперь источник правды — {@code RecipeManager}
 * (JSON), а {@code MachineBreederBlockEntity} ищет рецепт через
 * {@link RecipeHooks#getAllRecipes(Level, RecipeType)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:breeder",
 *   "ingredient": { "item": "..." },
 *   "result": { "item": "...", "count": 1 },
 *   "energy_per_tick": 100
 * }
 * }</pre>
 */
public class BreederRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final ItemStack output;
    private final int energyPerTick;

    public BreederRecipe(ResourceLocation id, Ingredient input, ItemStack output, int energyPerTick) {
        super(id);
        this.input = input;
        this.output = output;
        this.energyPerTick = Math.max(0, energyPerTick);
    }

    public Ingredient getInput() { return input; }
    public ItemStack getOutput() { return output.copy(); }
    /** Энергия (HE/тик) на время работы рецепта — «flux»-число оригинала 1:1. */
    public int getEnergyPerTick() { return energyPerTick; }

    /** Совпадение входа (точный предмет, как прежний {@code Map<Item, ...>} lookup). */
    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && input.test(stack);
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // У размножителя один входной слот (slot 0 = input, slot 1 = output).
        return !level.isClientSide()
                && matches(container.getItem(0));
    }

    @Override
    public ItemStack assembleSafe() { return output.copy(); }

    @Override
    public ItemStack getResultItemSafe() { return output.copy(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<BreederRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "breeder";
    }

    public static class Serializer extends PlatformRecipeSerializer<BreederRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "breeder");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "breeder");
        //?}

        @Override
        public BreederRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("ingredient"));
            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int energyPerTick = GsonHelper.getAsInt(json, "energy_per_tick", 0);
            return new BreederRecipe(recipeId, input, output, energyPerTick);
        }

        @Override
        public BreederRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            ItemStack output = RecipeHooks.readItem(buf);
            int energyPerTick = buf.readVarInt();
            return new BreederRecipe(recipeId, input, output, energyPerTick);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, BreederRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            RecipeHooks.writeItem(buf, recipe.output);
            buf.writeVarInt(recipe.energyPerTick);
        }
    }
}
