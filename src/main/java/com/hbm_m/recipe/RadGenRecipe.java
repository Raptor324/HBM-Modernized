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
 * Datapack-facing рецепт радиационного генератора ({@code hbm_m:radgen}).
 *
 * <p>Порт статического {@code com.hbm_m.recipe.RadGenRecipes} (Java-порт 1.7.10
 * {@code TileEntityMachineRadGen.fuels}): один предметный вход перерабатывается
 * {@code duration} тиков, производя {@code power} HE/тик, и превращается в
 * {@code result} (у «scrap»-рецепта выхода нет — поле {@code result} опционально,
 * отсутствие означает {@link ItemStack#EMPTY}).</p>
 *
 * <p><b>Замена статике:</b> прежний {@code RadGenRecipes} хранил {@code Map<Item, Recipe>}
 * в Java-статике, что блокировало data-driven рецепты. Теперь источник правды —
 * {@code RecipeManager} (JSON), а {@code MachineRadGenBlockEntity} ищет рецепт через
 * {@link RecipeHooks#getAllRecipes(Level, RecipeType)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:radgen",
 *   "ingredient": { "item": "..." },
 *   "power": 1500,
 *   "duration": 36000,
 *   "result": { "item": "...", "count": 1 }   // optional; отсутствие => без выхода (scrap)
 * }
 * }</pre>
 */
public class RadGenRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final int power;
    private final int duration;
    private final ItemStack output;

    public RadGenRecipe(ResourceLocation id, Ingredient input, int power, int duration, ItemStack output) {
        super(id);
        this.input = input;
        this.power = power;
        this.duration = Math.max(1, duration);
        this.output = output == null ? ItemStack.EMPTY : output;
    }

    public Ingredient getInput() { return input; }
    /** Производство HE/тик на время переработки. */
    public int getPower() { return power; }
    /** Длительность переработки в тиках. */
    public int getDuration() { return duration; }
    /** Выход; {@link ItemStack#EMPTY} для «scrap»-рецептов без выхода. */
    public ItemStack getOutput() { return output.copy(); }

    /** Совпадение входа (точный предмет, как прежний {@code Map<Item, ...>} lookup). */
    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && input.test(stack);
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // У radgen 12 входных слотов (0..11); матчим по первому.
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

    public static class Type implements RecipeType<RadGenRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "radgen";
    }

    public static class Serializer extends PlatformRecipeSerializer<RadGenRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "radgen");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "radgen");
        //?}

        @Override
        public RadGenRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("ingredient"));
            int power = GsonHelper.getAsInt(json, "power", 0);
            int duration = GsonHelper.getAsInt(json, "duration", 1);
            // result опционален: отсутствие => EMPTY (scrap сгорает без выхода).
            ItemStack output = json.has("result")
                    ? RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"))
                    : ItemStack.EMPTY;
            return new RadGenRecipe(recipeId, input, power, duration, output);
        }

        @Override
        public RadGenRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            int power = buf.readVarInt();
            int duration = buf.readVarInt();
            ItemStack output = RecipeHooks.readItem(buf);
            return new RadGenRecipe(recipeId, input, power, duration, output);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, RadGenRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            buf.writeVarInt(recipe.power);
            buf.writeVarInt(recipe.duration);
            RecipeHooks.writeItem(buf, recipe.output);
        }
    }
}
