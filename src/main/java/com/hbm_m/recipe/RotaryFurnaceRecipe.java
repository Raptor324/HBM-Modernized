package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
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
 * Datapack-facing рецепт вращающейся печи ({@code hbm_m:rotary_furnace}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.RotaryFurnaceRecipes}: до 3
 * предметных входов (каждый — {@link Ingredient} + обязательный {@code count}) → один предметный
 * выход за фиксированную длительность, с опциональной входной жидкостью (mB). Оригинальный
 * {@code MaterialStack}-выход (розлив расплавленного металла) в этом порте — предметный
 * {@link ItemStack} (та же упрощённость, что и раньше).</p>
 *
 * <p><b>Замена статике:</b> прежний {@code RotaryFurnaceRecipes} хранил {@code List<RotaryFurnaceRecipe>}
 * в Java-статике, пополнявшуюся из static-блока вручную. Это блокировало data-driven рецепты.
 * Теперь источник правды — {@code RecipeManager}, а {@code MachineRotaryFurnaceBlockEntity.tick}
 * ищет рецепт через {@link RecipeHooks#getAllRecipes(Level, RecipeType)}.</p>
 *
 * <p>Семантика мэтчинга 1:1 с оригиналом: ингредиенты рецепта должны быть покрыты тремя входными
 * слотами (один слот может покрывать несколько ингредиентов, «лишние» предметы в слотах рецепт
 * не блокируют).</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:rotary_furnace",
 *   "ingredients": [                    // 0..3 элемента; каждый — { "ingredient": { ...Ingredient... }, "count": 1 }
 *     { "ingredient": { "item": "minecraft:iron_ingot" }, "count": 1 },
 *     { "ingredient": { "item": "minecraft:coal" }, "count": 1 }
 *   ],
 *   "fluid": { "fluid": "hbm_m:lightoil", "amount": 100 },  // optional; отсутствие => без жидкости
 *   "result": { "item": "...", "count": 1 },
 *   "duration": 100
 * }
 * }</pre>
 */
public class RotaryFurnaceRecipe extends PlatformRecipe {

    private final Ingredient[] inputs;
    private final int[] counts;
    @Nullable
    private final FluidStack fluid;
    private final ItemStack output;
    private final int duration;

    public RotaryFurnaceRecipe(ResourceLocation id, Ingredient[] inputs, int[] counts,
                               @Nullable FluidStack fluid, ItemStack output, int duration) {
        super(id);
        if (inputs.length != counts.length) {
            throw new IllegalArgumentException("RotaryFurnaceRecipe: ingredients/counts length mismatch");
        }
        this.inputs = inputs;
        this.counts = counts;
        this.fluid = (fluid != null && !fluid.isEmpty() && fluid.getAmount() > 0) ? fluid : null;
        this.output = output;
        this.duration = Math.max(1, duration);
    }

    public Ingredient[] getInputs() { return inputs; }
    public int[] getCounts() { return counts; }
    public int getInputCount(int idx) { return counts[idx]; }
    @Nullable public FluidStack getFluid() { return fluid; }

    /** Количество входной жидкости в mB (0, если жидкость не требуется). */
    public int getFluidAmountMb() {
        return fluid != null ? (int) Math.min(Integer.MAX_VALUE, fluid.getAmount()) : 0;
    }

    public ItemStack getOutput() { return output.copy(); }
    public int getDuration() { return duration; }

    /**
     * Совпадение входа по 3 слотам машины (семантика оригинального {@code RotaryFurnaceRecipes.getRecipe}):
     * каждый ингредиент снимается из «пула» слотов (стек слота уменьшается), один слот может
     * покрывать несколько ингредиентов. Лишние предметы в слотах рецепт НЕ блокируют.
     */
    public boolean matchesInputs(ItemStack slot0, ItemStack slot1, ItemStack slot2) {
        ItemStack[] pool = { slot0.copy(), slot1.copy(), slot2.copy() };
        for (int i = 0; i < inputs.length; i++) {
            boolean found = false;
            for (int s = 0; s < pool.length; s++) {
                if (!pool[s].isEmpty() && inputs[i].test(pool[s]) && pool[s].getCount() >= counts[i]) {
                    pool[s].shrink(counts[i]);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /**
     * Проверка жидкостного требования: та же субстанция (с учётом {@link VanillaFluidEquivalence}
     * для пар HBM/vanilla water/lava, как оригинал) и достаточный уровень в баке.
     */
    public boolean matchesFluid(Fluid tankFluid, int tankFill) {
        if (fluid == null) return true;
        if (tankFluid == null || tankFluid.isSame(Fluids.EMPTY)) return false;
        if (!VanillaFluidEquivalence.sameSubstance(tankFluid, fluid.getFluid())) return false;
        return tankFill >= getFluidAmountMb();
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // У вращающейся печи 3 входных слота (0..2); жидкость машина проверяет отдельно через #matchesFluid.
        return !level.isClientSide()
                && matchesInputs(container.getItem(0), container.getItem(1), container.getItem(2));
    }

    @Override
    public ItemStack assembleSafe() { return output.copy(); }

    @Override
    public ItemStack getResultItemSafe() { return output.copy(); }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<RotaryFurnaceRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "rotary_furnace";
    }

    public static class Serializer extends PlatformRecipeSerializer<RotaryFurnaceRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "rotary_furnace");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "rotary_furnace");
        //?}

        @Override
        public RotaryFurnaceRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient[] inputs = new Ingredient[0];
            int[] counts = new int[0];
            if (json.has("ingredients")) {
                JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
                inputs = new Ingredient[arr.size()];
                counts = new int[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject entry = arr.get(i).getAsJsonObject();
                    inputs[i] = RecipeHooks.ingredientFromJson(entry.get("ingredient"));
                    counts[i] = GsonHelper.getAsInt(entry, "count", 1);
                }
            }

            FluidStack fluid = null;
            if (json.has("fluid")) {
                JsonObject fluidObj = GsonHelper.getAsJsonObject(json, "fluid");
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(fluidObj, "fluid"));
                int amount = GsonHelper.getAsInt(fluidObj, "amount", 0);
                if (id != null && amount > 0) {
                    fluid = RecipeHooks.fluidStackOf(id, amount);
                }
            }

            ItemStack output = RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            return new RotaryFurnaceRecipe(recipeId, inputs, counts, fluid, output, duration);
        }

        @Override
        public RotaryFurnaceRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            Ingredient[] inputs = new Ingredient[n];
            int[] counts = new int[n];
            for (int i = 0; i < n; i++) {
                inputs[i] = RecipeHooks.readIngredient(buf);
                counts[i] = buf.readVarInt();
            }
            FluidStack fluid = RecipeHooks.readFluidStack(buf);
            ItemStack output = RecipeHooks.readItem(buf);
            int duration = buf.readVarInt();
            return new RotaryFurnaceRecipe(recipeId, inputs, counts, fluid, output, duration);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, RotaryFurnaceRecipe recipe) {
            buf.writeVarInt(recipe.inputs.length);
            for (int i = 0; i < recipe.inputs.length; i++) {
                RecipeHooks.writeIngredient(buf, recipe.inputs[i]);
                buf.writeVarInt(recipe.counts[i]);
            }
            RecipeHooks.writeFluidStack(buf, recipe.fluid != null ? recipe.fluid : FluidStack.empty());
            RecipeHooks.writeItem(buf, recipe.output);
            buf.writeVarInt(recipe.duration);
        }
    }
}
