package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.inventory.fluid.FluidType;
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

/**
 * Datapack-facing дуговой-сварочный рецепт ({@code hbm_m:arc_welder}).
 *
 * <p>Порт 1.7.10 статического {@code com.hbm.inventory.recipes.ArcWelderRecipes}: до 3
 * предметных входов (каждый — {@link Ingredient} + обязательный {@code count}) → один предметный
 * выход, за фиксированную длительность, с энергопотреблением {@code consumption} HE/тик и опциональным
 * требованием жидкости ({@link FluidType}, mB) — как {@code ArcWelderRecipes.FluidRequirement}.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code ArcWelderRecipes} хранил {@code List<ArcWelderRecipe>}
 * в Java-статике, пополнявшуюся из {@code MainRegistry} вручную. Это блокировало data-driven рецепты.
 * Теперь источник правды — {@code RecipeManager}, а {@code MachineArcWelderBlockEntity.tick} ищет
 * рецепт через {@link RecipeHooks#getAllRecipes(Level, RecipeType)}.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:arc_welder",
 *   "ingredients": [                    // 1..3 элемента; каждый — { "ingredient": { ...Ingredient... }, "count": 2 }
 *     { "ingredient": { "item": "..." }, "count": 2 },
 *     { "ingredient": { "tag": "forge:ingots/plastic" }, "count": 1 }
 *   ],
 *   "fluid": { "fluid": "hbm_m:oxygen", "amount": 1000 },  // optional; отсутствие => без жидкости
 *   "result": { "item": "...", "count": 1 },
 *   "duration": 200,
 *   "consumption": 10000
 * }
 * }</pre>
 */
public class ArcWelderRecipe extends PlatformRecipe {

    private final Ingredient[] inputs;
    private final int[] counts;
    @Nullable
    private final FluidStack fluid;
    private final ItemStack output;
    private final int duration;
    private final long consumption;

    public ArcWelderRecipe(ResourceLocation id, Ingredient[] inputs, int[] counts,
                            @Nullable FluidStack fluid, ItemStack output, int duration, long consumption) {
        super(id);
        if (inputs.length != counts.length) {
            throw new IllegalArgumentException("ArcWelderRecipe: ingredients/counts length mismatch");
        }
        this.inputs = inputs;
        this.counts = counts;
        this.fluid = (fluid != null && !fluid.isEmpty() && fluid.getAmount() > 0) ? fluid : null;
        this.output = output;
        this.duration = Math.max(1, duration);
        this.consumption = Math.max(0, consumption);
    }

    public Ingredient[] getInputs() { return inputs; }
    public int[] getCounts() { return counts; }
    public int getInputCount(int idx) { return counts[idx]; }
    @Nullable public FluidStack getFluid() { return fluid; }
    public ItemStack getOutput() { return output.copy(); }
    public int getDuration() { return duration; }
    public long getConsumption() { return consumption; }

    /** Совпадение входа по 3 слотам машины (порядок не важен, как в оригинале). */
    public boolean matchesInputs(ItemStack slot0, ItemStack slot1, ItemStack slot2) {
        ItemStack[] stacks = { slot0, slot1, slot2 };
        boolean[] used = new boolean[inputs.length];
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            boolean matched = false;
            for (int i = 0; i < inputs.length; i++) {
                if (used[i]) continue;
                if (inputs[i].test(stack) && stack.getCount() >= counts[i]) {
                    used[i] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        // Все требуемые входы должны быть покрыты (used[i] == true для каждого i).
        for (boolean u : used) if (!u) return false;
        return true;
    }

    /** Проверка жидкостного требования по {@link FluidType} (как оригинальный {@code FluidRequirement}). */
    public boolean matchesFluid(FluidTank tank) {
        if (fluid == null) return true;
        if (tank == null) return false;
        FluidType stored = FluidType.forFluid(tank.getStoredFluid());
        FluidType required = FluidType.forFluid(fluid.getFluid());
        return stored == required && tank.getFill() >= (int) Math.min(Integer.MAX_VALUE, fluid.getAmount());
    }

    /** Утилита для BlockEntity: поглотить жидкость из бака, если требование удовлетворено. */
    public void consumeFluid(FluidTank tank) {
        if (fluid != null && matchesFluid(tank)) {
            tank.setFill(tank.getFill() - (int) Math.min(Integer.MAX_VALUE, fluid.getAmount()));
        }
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // У дуговой сварки 3 входных слота (0..2).
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

    public static class Type implements RecipeType<ArcWelderRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "arc_welder";
    }

    public static class Serializer extends PlatformRecipeSerializer<ArcWelderRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "arc_welder");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "arc_welder");
        //?}

        @Override
        public ArcWelderRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
            Ingredient[] inputs = new Ingredient[arr.size()];
            int[] counts = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                JsonObject entry = arr.get(i).getAsJsonObject();
                inputs[i] = RecipeHooks.ingredientFromJson(entry.get("ingredient"));
                counts[i] = GsonHelper.getAsInt(entry, "count", 1);
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
            int duration = GsonHelper.getAsInt(json, "duration", 200);
            long consumption = GsonHelper.getAsLong(json, "consumption", 0L);
            return new ArcWelderRecipe(recipeId, inputs, counts, fluid, output, duration, consumption);
        }

        @Override
        public ArcWelderRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
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
            long consumption = buf.readVarLong();
            return new ArcWelderRecipe(recipeId, inputs, counts, fluid, output, duration, consumption);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, ArcWelderRecipe recipe) {
            buf.writeVarInt(recipe.inputs.length);
            for (int i = 0; i < recipe.inputs.length; i++) {
                RecipeHooks.writeIngredient(buf, recipe.inputs[i]);
                buf.writeVarInt(recipe.counts[i]);
            }
            RecipeHooks.writeFluidStack(buf, recipe.fluid != null ? recipe.fluid : FluidStack.empty());
            RecipeHooks.writeItem(buf, recipe.output);
            buf.writeVarInt(recipe.duration);
            buf.writeVarLong(recipe.consumption);
        }
    }
}
