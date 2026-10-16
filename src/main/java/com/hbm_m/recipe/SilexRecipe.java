package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
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
 * Datapack-facing рецепт SILEX ({@code hbm_m:silex}).
 *
 * <p>Порт статического {@code com.hbm_m.recipe.SilexRecipes} (упрощённый Java-порт 1.7.10
 * {@code com.hbm.inventory.recipes.SILEXRecipes}): один предметный вход перерабатывается
 * {@code duration} тиков, потребляя {@code peroxide_mb} mB перекиси из бака машины, и даёт
 * <b>взвешенно-случайный</b> выход из списка {@code outputs} (пары «стак × вес»). Распределения
 * весов — прямой порт оригинала: U → U235/U238, Pu-Mix → Pu239/Pu240, Am-Mix → Am241/Am242.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code SilexRecipes} хранил {@code Map<Item, SilexRecipe>}
 * в Java-статике, что блокировало data-driven рецепты. Теперь источник правды —
 * {@code RecipeManager} (JSON), а {@code MachineSilexBlockEntity} ищет рецепт через
 * {@link RecipeHooks#getAllRecipes(Level, RecipeType)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:silex",
 *   "ingredient": { "item": "..." },
 *   "peroxide_mb": 100,
 *   "duration": 100,
 *   "outputs": [
 *     { "result": { "item": "...", "count": 1 }, "weight": 1 },
 *     { "result": { "item": "...", "count": 1 }, "weight": 11 }
 *   ]
 * }
 * }</pre>
 */
public class SilexRecipe extends PlatformRecipe {

    /** Взвешенный выход: стак + относительный вес. */
    public record WeightedOutput(ItemStack stack, int weight) {}

    private final Ingredient input;
    private final int peroxideMb;
    private final int duration;
    private final List<WeightedOutput> outputs;

    public SilexRecipe(ResourceLocation id, Ingredient input, int peroxideMb, int duration,
                       List<WeightedOutput> outputs) {
        super(id);
        this.input = input;
        this.peroxideMb = Math.max(0, peroxideMb);
        this.duration = Math.max(1, duration);
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
    }

    public Ingredient getInput() { return input; }
    /** Расход перекиси (mB) из бака машины за один цикл. */
    public int getPeroxideMb() { return peroxideMb; }
    /** Длительность процесса в тиках. */
    public int getDuration() { return duration; }
    public List<WeightedOutput> getOutputs() { return outputs; }

    /** Сумма весов всех выходов (для {@code Random#nextInt(total)} ролла в BE). */
    public int getTotalWeight() {
        int total = 0;
        for (WeightedOutput out : outputs) total += out.weight();
        return total;
    }

    /** Совпадение входа (точный предмет, как прежний {@code Map<Item, ...>} lookup). */
    public boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && input.test(stack);
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // У SILEX один входной слот (slot 0 = input, slot 1 = output, slot 2 = батарея).
        return !level.isClientSide()
                && matches(container.getItem(0));
    }

    @Override
    public ItemStack assembleSafe() {
        // Выход случаен; для API-совместимости возвращаем первый (максимально вероятный не является
        // обязательным — потребитель BE всегда роллит сам через getOutputs()).
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).stack().copy();
    }

    @Override
    public ItemStack getResultItemSafe() { return assembleSafe(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<SilexRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "silex";
    }

    public static class Serializer extends PlatformRecipeSerializer<SilexRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "silex");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silex");
        //?}

        @Override
        public SilexRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("ingredient"));
            int peroxideMb = GsonHelper.getAsInt(json, "peroxide_mb", 0);
            int duration = GsonHelper.getAsInt(json, "duration", 1);

            JsonArray arr = GsonHelper.getAsJsonArray(json, "outputs");
            List<WeightedOutput> outputs = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject entry = arr.get(i).getAsJsonObject();
                ItemStack stack = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(entry, "result"));
                int weight = GsonHelper.getAsInt(entry, "weight", 1);
                outputs.add(new WeightedOutput(stack, Math.max(1, weight)));
            }
            return new SilexRecipe(recipeId, input, peroxideMb, duration, outputs);
        }

        @Override
        public SilexRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            int peroxideMb = buf.readVarInt();
            int duration = buf.readVarInt();
            int n = buf.readVarInt();
            List<WeightedOutput> outputs = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                ItemStack stack = RecipeHooks.readItem(buf);
                int weight = buf.readVarInt();
                outputs.add(new WeightedOutput(stack, weight));
            }
            return new SilexRecipe(recipeId, input, peroxideMb, duration, outputs);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, SilexRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            buf.writeVarInt(recipe.peroxideMb);
            buf.writeVarInt(recipe.duration);
            buf.writeVarInt(recipe.outputs.size());
            for (WeightedOutput out : recipe.outputs) {
                RecipeHooks.writeItem(buf, out.stack());
                buf.writeVarInt(out.weight());
            }
        }
    }
}
