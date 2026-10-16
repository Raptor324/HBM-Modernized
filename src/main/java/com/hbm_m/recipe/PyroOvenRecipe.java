package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonObject;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.tank.FluidTank;
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

/**
 * Datapack-рецепт пиро-печи ({@code hbm_m:pyro_oven}).
 *
 * <p>Порт статического {@code PyroOvenRecipes}: опциональная входная жидкость (mB) + опциональный
 * входной предмет (count) → опциональный предметный выход и/или опциональная выходная жидкость (mB),
 * за фиксированную длительность. Рецепты проверяются по порядку, побеждает первый подходящий —
 * 1:1 как в оригинале ({@code getMatchingRecipe()}).</p>
 *
 * <p><b>Замена статике:</b> ~27 «Solid Fuel»-рецептов рассчитывались из {@code FT_Flammable}-энергии
 * жидкости ({@code mB = tuPerSF * 1000 * 0.5 / heatEnergy}, округление вниз до 1000/100/10 mB) и 30
 * roast-рецептов bedrock-руд генерировались циклом 6 Type × 5 Grade-пар. В датагене всё это
 * запечено литералами/циклами (см. {@code PyroOvenRecipeGenerator}).</p>
 *
 * <p>JSON-формат (все четыре входа/выхода опциональны, но хотя бы один выход обязателен):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:pyro_oven",
 *   "fluid_input":  { "fluid": "hbm_m:syngas", "amount": 2000 },      // optional
 *   "item_input":   { "ingredient": { "item": "..." }, "count": 1 },  // optional
 *   "item_output":  { "item": "...", "count": 1 },                    // optional
 *   "fluid_output": { "fluid": "hbm_m:spentsteam", "amount": 1000 },  // optional
 *   "duration": 300
 * }
 * }</pre>
 */
public class PyroOvenRecipe extends PlatformRecipe {

    @Nullable
    private final FluidStack inputFluid;
    @Nullable
    private final Ingredient inputItem;
    private final int inputItemCount;
    @Nullable
    private final ItemStack outputItem;
    @Nullable
    private final FluidStack outputFluid;
    private final int duration;

    public PyroOvenRecipe(ResourceLocation id,
                          @Nullable FluidStack inputFluid,
                          @Nullable Ingredient inputItem, int inputItemCount,
                          @Nullable ItemStack outputItem,
                          @Nullable FluidStack outputFluid,
                          int duration) {
        super(id);
        this.inputFluid = inputFluid != null && !inputFluid.isEmpty() && inputFluid.getAmount() > 0 ? inputFluid : null;
        this.inputItem = inputItem;
        this.inputItemCount = Math.max(1, inputItemCount);
        this.outputItem = outputItem != null && !outputItem.isEmpty() ? outputItem : null;
        this.outputFluid = outputFluid != null && !outputFluid.isEmpty() && outputFluid.getAmount() > 0 ? outputFluid : null;
        this.duration = Math.max(1, duration);
    }

    @Nullable public FluidStack getInputFluidStack() { return inputFluid; }
    @Nullable public Fluid getInputFluid() { return inputFluid != null ? inputFluid.getFluid() : null; }
    public int getInputFluidMb() { return inputFluid != null ? (int) Math.min(Integer.MAX_VALUE, inputFluid.getAmount()) : 0; }
    @Nullable public Ingredient getInputItem() { return inputItem; }
    public int getInputItemCount() { return inputItemCount; }
    @Nullable public ItemStack getOutputItem() { return outputItem != null ? outputItem.copy() : null; }
    @Nullable public FluidStack getOutputFluidStack() { return outputFluid; }
    @Nullable public Fluid getOutputFluid() { return outputFluid != null ? outputFluid.getFluid() : null; }
    public int getOutputFluidMb() { return outputFluid != null ? (int) Math.min(Integer.MAX_VALUE, outputFluid.getAmount()) : 0; }
    public int getDuration() { return duration; }

    /**
     * Проверка «формы» рецепта (как оригинальный {@code doesRecipeMatch}): тип входного бака совпадает
     * (или рецепт без жидкости), слот предмета совпадает (или пуст для рецептов без предмета).
     */
    public boolean matchesInputs(@Nullable FluidTank tank, ItemStack itemIn) {
        if (inputFluid != null) {
            if (tank == null || !VanillaFluidEquivalence.sameSubstance(tank.getTankType(), inputFluid.getFluid())) return false;
        }
        if (inputItem != null) {
            if (itemIn.isEmpty()) return false;
            if (!inputItem.test(itemIn)) return false;
        } else if (!itemIn.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Слот 0 обёртки соответствует предметному входу машины (SLOT_ITEM_IN).
        if (level.isClientSide()) return false;
        ItemStack itemIn = container.getItem(0);
        if (inputItem != null) {
            return !itemIn.isEmpty() && inputItem.test(itemIn);
        }
        return itemIn.isEmpty();
    }

    @Override
    public ItemStack assembleSafe() {
        return getResultItemSafe();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return outputItem != null ? outputItem.copy() : ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<PyroOvenRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "pyro_oven";
    }

    public static class Serializer extends PlatformRecipeSerializer<PyroOvenRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "pyro_oven");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "pyro_oven");
        //?}

        @Override
        public PyroOvenRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack inputFluid = json.has("fluid_input") ? readFluid(GsonHelper.getAsJsonObject(json, "fluid_input")) : null;

            Ingredient inputItem = null;
            int inputItemCount = 1;
            if (json.has("item_input")) {
                JsonObject itemObj = GsonHelper.getAsJsonObject(json, "item_input");
                inputItem = RecipeHooks.ingredientFromJson(itemObj.get("ingredient"));
                inputItemCount = GsonHelper.getAsInt(itemObj, "count", 1);
            }

            ItemStack outputItem = json.has("item_output") ? RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "item_output")) : null;
            FluidStack outputFluid = json.has("fluid_output") ? readFluid(GsonHelper.getAsJsonObject(json, "fluid_output")) : null;
            int duration = GsonHelper.getAsInt(json, "duration", 100);

            return new PyroOvenRecipe(recipeId, inputFluid, inputItem, inputItemCount, outputItem, outputFluid, duration);
        }

        @Override
        public PyroOvenRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack inputFluid = buf.readBoolean() ? RecipeHooks.readFluidStack(buf) : null;
            Ingredient inputItem = buf.readBoolean() ? RecipeHooks.readIngredient(buf) : null;
            int inputItemCount = buf.readVarInt();
            ItemStack outputItem = RecipeHooks.readItem(buf);
            FluidStack outputFluid = buf.readBoolean() ? RecipeHooks.readFluidStack(buf) : null;
            int duration = buf.readVarInt();
            return new PyroOvenRecipe(recipeId, inputFluid, inputItem, inputItemCount, outputItem, outputFluid, duration);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, PyroOvenRecipe recipe) {
            buf.writeBoolean(recipe.inputFluid != null);
            if (recipe.inputFluid != null) RecipeHooks.writeFluidStack(buf, recipe.inputFluid);
            buf.writeBoolean(recipe.inputItem != null);
            if (recipe.inputItem != null) RecipeHooks.writeIngredient(buf, recipe.inputItem);
            buf.writeVarInt(recipe.inputItemCount);
            RecipeHooks.writeItem(buf, recipe.outputItem != null ? recipe.outputItem : ItemStack.EMPTY);
            buf.writeBoolean(recipe.outputFluid != null);
            if (recipe.outputFluid != null) RecipeHooks.writeFluidStack(buf, recipe.outputFluid);
            buf.writeVarInt(recipe.duration);
        }

        /** { "fluid": <id>, "amount": <mB> } — единый формат мода (см. ChemicalPlantRecipe). */
        private static FluidStack readFluid(JsonObject obj) {
            ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid"));
            long amount = GsonHelper.getAsLong(obj, "amount", 0L);
            return RecipeHooks.fluidStackOf(id, amount);
        }
    }
}
