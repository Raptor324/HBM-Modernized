package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import dev.architectury.fluid.FluidStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Datapack-facing recipe shape ({@code hbm_m:crystallizer}) для рудного оксидатора (Crystallizer).
 *
 * <p>Порт 1.7.10 {@code com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe}: один предметный
 * вход ({@link Ingredient} + {@code count}) + одна кислота (Architectury {@link FluidStack}, mB) →
 * один предметный выход, за фиксированную длительность, с шансом «не потратить вход» ({@code productivity}).</p>
 *
 * <p><b>Унификация жидкостей:</b> жидкостный вход (кислота) хранится как Architectury {@link FluidStack}
 * и сериализуется общими хелперами {@link RecipeHooks#readFluidStack}/{@link RecipeHooks#writeFluidStack}
 * (тот же формат, что у {@code ChemicalPlantRecipe}/{@code CombinationOvenRecipe}/{@code MixerRecipe}).
 * {@code null}/{@link FluidStack#empty()} — рецепт работает с любой жидкостью в баке (старое поведение
 * рецептов без указания кислоты в оригинале).</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:crystallizer",
 *   "ingredient": { ...Ingredient... },
 *   "count": 1,                 // optional, default 1
 *   "acid": {                    // optional; absence => any acid
 *     "fluid": "hbm_m:peroxide",
 *     "amount": 500
 *   },
 *   "result": { "item": "...", "count": 1 },
 *   "duration": 600,
 *   "productivity": 0.05
 * }
 * }</pre>
 *
 * <p>Runtime-поиск рецепта ведётся через {@code CrystallizerRecipes.findRecipe(level, input, tankFluid)},
 * который итерирует рецепты типа {@link Type#INSTANCE} из {@link net.minecraft.world.item.crafting.RecipeManager}.</p>
 */
public class CrystallizerRecipe extends PlatformRecipe {

    private final Ingredient input;
    private final int inputCount;
    @Nullable
    private final FluidStack acid;
    private final ItemStack output;
    private final int duration;
    private final float productivity;

    public CrystallizerRecipe(ResourceLocation id, Ingredient input, int inputCount, @Nullable FluidStack acid,
                              ItemStack output, int duration, float productivity) {
        super(id);
        this.input = input;
        this.inputCount = Math.max(1, inputCount);
        // null/empty/0-amount → null (рецепт «без кислоты», любой бак проходит)
        this.acid = (acid != null && !acid.isEmpty() && acid.getAmount() > 0) ? acid : null;
        this.output = output;
        this.duration = Math.max(1, duration);
        this.productivity = Math.max(0f, Math.min(1f, productivity));
    }

    public Ingredient getInput() { return input; }
    public int getInputCount() { return inputCount; }
    @Nullable public FluidStack getAcid() { return acid; }
    public ItemStack getOutput() { return output.copy(); }
    public int getDuration() { return duration; }
    public float getProductivity() { return productivity; }

    public int getAcidAmount() {
        return acid == null ? 0 : (int) Math.min(Integer.MAX_VALUE, acid.getAmount());
    }

    /** Проверяет совпадение входа рецепта со стэком в слоте (без проверки количества — это в машине). */
    public boolean matchesInput(ItemStack stack) {
        return input.test(stack);
    }

    /** Если рецепт не требует конкретной кислоты — true. Иначе — точное совпадение жидкости. */
    public boolean matchesAcid(FluidStack tankFluid) {
        if (acid == null) return true;
        if (tankFluid == null || tankFluid.isEmpty()) return false;
        return tankFluid.getFluid() == acid.getFluid();
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        return !level.isClientSide() && input.test(container.getItem(0));
    }

    @Override
    public ItemStack assembleSafe() {
        return output.copy();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return output.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<CrystallizerRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "crystallizer";
    }

    public static class Serializer extends PlatformRecipeSerializer<CrystallizerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "crystallizer");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "crystallizer");
        //?}

        @Override
        public CrystallizerRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient input = RecipeHooks.ingredientFromJson(json.get("ingredient"));
            int inputCount = GsonHelper.getAsInt(json, "count", 1);

            FluidStack acid = null;
            if (json.has("acid")) {
                JsonObject acidObj = GsonHelper.getAsJsonObject(json, "acid");
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(acidObj, "fluid"));
                int amount = GsonHelper.getAsInt(acidObj, "amount", 0);
                if (id != null && amount > 0) {
                    acid = RecipeHooks.fluidStackOf(id, amount);
                }
            }

            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int duration = GsonHelper.getAsInt(json, "duration", 600);
            float productivity = GsonHelper.getAsFloat(json, "productivity", 0f);

            return new CrystallizerRecipe(recipeId, input, inputCount, acid, output, duration, productivity);
        }

        @Override
        public CrystallizerRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = RecipeHooks.readIngredient(buf);
            int inputCount = buf.readVarInt();
            FluidStack acid = RecipeHooks.readFluidStack(buf);
            ItemStack output = RecipeHooks.readItem(buf);
            int duration = buf.readVarInt();
            float productivity = buf.readFloat();
            return new CrystallizerRecipe(recipeId, input, inputCount, acid, output, duration, productivity);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, CrystallizerRecipe recipe) {
            RecipeHooks.writeIngredient(buf, recipe.input);
            buf.writeVarInt(recipe.inputCount);
            RecipeHooks.writeFluidStack(buf, recipe.acid != null ? recipe.acid : FluidStack.empty());
            RecipeHooks.writeItem(buf, recipe.output);
            buf.writeVarInt(recipe.duration);
            buf.writeFloat(recipe.productivity);
        }
    }
}
