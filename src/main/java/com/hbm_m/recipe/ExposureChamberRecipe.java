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
 * Datapack-facing рецепт экспозиционной камеры ({@code hbm_m:exposure_chamber}).
 *
 * <p>Порт статического {@code com.hbm_m.recipe.ExposureChamberRecipes} (порт 1.7.10
 * {@code com.hbm.inventory.recipes.ExposureChamberRecipes}): предмет-частица
 * ({@code particle}, как правило капсула Higgs/Dark/Sparkticle) облучает второй вход
 * ({@code ingredient}) и превращает его в {@code result}. Одна капсула частицы даёт
 * {@code MachineExposureChamberBlockEntity.MAX_PARTICLES} использований — эта логика
 * живёт в BE, рецепт описывает только пару входов и выход.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code ExposureChamberRecipes} хранил {@code List<Recipe>}
 * в Java-статике, что блокировало data-driven рецепты. Теперь источник правды —
 * {@code RecipeManager} (JSON), а BE и JEI-категория ищут рецепты через
 * {@link RecipeHooks#getAllRecipes(Level, RecipeType)}.</p>
 *
 * <p>«Expensive mode»-альтернативы оригинала (degenerate matter вместо чистого шрабидия)
 * по-прежнему не портированы — нет конфиг-переключателя.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:exposure_chamber",
 *   "particle":   { "item": "..." },
 *   "ingredient": { ...Ingredient... },
 *   "result":     { "item": "...", "count": 1 }
 * }
 * }</pre>
 */
public class ExposureChamberRecipe extends PlatformRecipe {

    private final ItemStack particle;
    private final Ingredient ingredient;
    private final ItemStack output;

    public ExposureChamberRecipe(ResourceLocation id, ItemStack particle, Ingredient ingredient, ItemStack output) {
        super(id);
        this.particle = particle;
        this.ingredient = ingredient;
        this.output = output;
    }

    /** Предмет-частица (капсула) — точный предмет, как в оригинальном {@code Recipe.particle}. */
    public ItemStack getParticle() { return particle.copy(); }
    public Ingredient getIngredient() { return ingredient; }
    public ItemStack getOutput() { return output.copy(); }

    /** Мэтчинг по паре стеков (particle-слот и ingredient-слот машины). */
    public boolean matches(ItemStack particleStack, ItemStack ingredientStack) {
        if (particleStack == null || ingredientStack == null) return false;
        if (particleStack.isEmpty() || ingredientStack.isEmpty()) return false;
        return particleStack.is(particle.getItem()) && ingredient.test(ingredientStack);
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Камера двух-слотная: slot 0 = particle, slot 1 = ingredient.
        return !level.isClientSide()
                && matches(container.getItem(0), container.getItem(1));
    }

    @Override
    public ItemStack assembleSafe() { return output.copy(); }

    @Override
    public ItemStack getResultItemSafe() { return output.copy(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<ExposureChamberRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "exposure_chamber";
    }

    public static class Serializer extends PlatformRecipeSerializer<ExposureChamberRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "exposure_chamber");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "exposure_chamber");
        //?}

        @Override
        public ExposureChamberRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            ItemStack particle = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "particle"));
            Ingredient ingredient = RecipeHooks.ingredientFromJson(json.get("ingredient"));
            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new ExposureChamberRecipe(recipeId, particle, ingredient, output);
        }

        @Override
        public ExposureChamberRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            ItemStack particle = RecipeHooks.readItem(buf);
            Ingredient ingredient = RecipeHooks.readIngredient(buf);
            ItemStack output = RecipeHooks.readItem(buf);
            return new ExposureChamberRecipe(recipeId, particle, ingredient, output);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, ExposureChamberRecipe recipe) {
            RecipeHooks.writeItem(buf, recipe.particle);
            RecipeHooks.writeIngredient(buf, recipe.ingredient);
            RecipeHooks.writeItem(buf, recipe.output);
        }
    }
}
