package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;

/**
 * Datagen builder for {@link ChemicalPlantRecipe}.
 */
public class ChemicalPlantRecipeBuilder extends BaseRecipeBuilder<ChemicalPlantRecipeBuilder> {

    private final int duration;
    private final int power;

    private final List<CountedIngredient> itemInputs = new ArrayList<>();
    private final List<FluidAmount> fluidInputs = new ArrayList<>();
    private final List<ItemStack> itemOutputs = new ArrayList<>();
    private final List<FluidAmount> fluidOutputs = new ArrayList<>();

    @Nullable
    private ItemStack iconItem;

    @Nullable
    private Fluid iconFluid;

    @Nullable
    private String blueprintPool;

    private ChemicalPlantRecipeBuilder(int duration, int power) {
        this.duration = duration;
        this.power = power;
    }

    public static ChemicalPlantRecipeBuilder chemicalPlantRecipe(int duration, int power) {
        return new ChemicalPlantRecipeBuilder(duration, power);
    }

    public ChemicalPlantRecipeBuilder addItemInput(Ingredient ingredient, int count) {
        this.itemInputs.add(new CountedIngredient(ingredient, count));
        return this;
    }

    public ChemicalPlantRecipeBuilder addItemInput(Item item, int count) {
        return addItemInput(Ingredient.of(item), count);
    }

    public ChemicalPlantRecipeBuilder addFluidInput(Fluid fluid, int amountMb) {
        this.fluidInputs.add(new FluidAmount(fluid, amountMb));
        return this;
    }

    public ChemicalPlantRecipeBuilder addItemOutput(ItemStack stack) {
        this.itemOutputs.add(stack);
        return this;
    }

    public ChemicalPlantRecipeBuilder addFluidOutput(Fluid fluid, int amountMb) {
        this.fluidOutputs.add(new FluidAmount(fluid, amountMb));
        return this;
    }

    /** Переход на единый FluidStack-вход: вместo {@code Fluid + int} принимаем {@link FluidStack}. */
    public ChemicalPlantRecipeBuilder addFluidInput(FluidStack stack) {
        if (stack != null && !stack.isEmpty()) {
            this.fluidInputs.add(new FluidAmount(stack.getFluid(), (int) stack.getAmount()));
        }
        return this;
    }

    public ChemicalPlantRecipeBuilder addFluidOutput(FluidStack stack) {
        if (stack != null && !stack.isEmpty()) {
            this.fluidOutputs.add(new FluidAmount(stack.getFluid(), (int) stack.getAmount()));
        }
        return this;
    }

    public ChemicalPlantRecipeBuilder withIconItem(ItemStack stack) {
        this.iconItem = (stack == null || stack.isEmpty()) ? null : stack.copy();
        return this;
    }

    public ChemicalPlantRecipeBuilder withIconItem(Item item) {
        return withIconItem(new ItemStack(item));
    }

    public ChemicalPlantRecipeBuilder withIconFluid(Fluid fluid) {
        this.iconFluid = fluid;
        return this;
    }

    public ChemicalPlantRecipeBuilder withBlueprintPool(String pool) {
        this.blueprintPool = pool;
        return this;
    }

    private record CountedIngredient(Ingredient ingredient, int count) {}

    private record FluidAmount(Fluid fluid, int amount) {}

    @Override
    public Item getResult() {
        for (ItemStack stack : itemOutputs) {
            if (!stack.isEmpty()) return stack.getItem();
        }
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.addProperty("duration", duration);
        json.addProperty("power", power);

        if (blueprintPool != null) {
            json.addProperty("blueprint_pool", blueprintPool);
        }

        if (iconItem != null && !iconItem.isEmpty()) {
            // Унифицированная сериализация иконки-предмета (через BaseRecipeBuilder.stackToJson).
            json.add("icon_item", stackToJson(iconItem));
        }

        if (iconFluid != null) {
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(iconFluid);
            if (fluidId != null) {
                json.addProperty("icon_fluid", fluidId.toString());
            }
        }

        JsonArray itemInputsJson = new JsonArray();
        for (CountedIngredient ci : itemInputs) {
            itemInputsJson.add(AssemblerRecipe.toCountedIngredientJson(ci.ingredient(), ci.count()));
        }
        json.add("item_inputs", itemInputsJson);

        // Жидкостные входы: единый формат { "fluid", "amount" } через fluidStackToJson.
        JsonArray fluidInputsJson = new JsonArray();
        for (FluidAmount fa : fluidInputs) {
            if (fa.fluid() == null) continue;
            fluidInputsJson.add(fluidStackToJson(FluidStack.create(fa.fluid(), fa.amount())));
        }
        json.add("fluid_inputs", fluidInputsJson);

        // Предметные выходы — единый формат через stackToJson.
        JsonArray itemOutputsJson = new JsonArray();
        for (ItemStack out : itemOutputs) {
            if (out == null || out.isEmpty()) continue;
            itemOutputsJson.add(stackToJson(out));
        }
        json.add("item_outputs", itemOutputsJson);

        // Жидкостные выходы — единый формат через fluidStackToJson.
        JsonArray fluidOutputsJson = new JsonArray();
        for (FluidAmount fa : fluidOutputs) {
            if (fa.fluid() == null) continue;
            fluidOutputsJson.add(fluidStackToJson(FluidStack.create(fa.fluid(), fa.amount())));
        }
        json.add("fluid_outputs", fluidOutputsJson);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return ChemicalPlantRecipe.Serializer.INSTANCE;
    }
}
//?}