package com.hbm_m.datagen.recipes.custom;
//? if forge {
/*import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.ChemicalPlantRecipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;

/^*
 * Datagen builder for {@link ChemicalPlantRecipe}.
 *
 * <p>Формат JSON совпадает с {@link ChemicalPlantRecipe.Serializer}:\n
 * <ul>
 *   <li>{@code item_inputs}: массив объектов Ingredient + {@code count}</li>
 *   <li>{@code fluid_inputs}: массив объектов {@code {fluid, amount}}</li>
 *   <li>{@code item_outputs}: массив объектов ItemStack</li>
 *   <li>{@code fluid_outputs}: массив объектов {@code {fluid, amount}}</li>
 *   <li>{@code duration}, {@code power}, опционально {@code blueprint_pool}</li>
 * </ul>
 ^/
public class ChemicalPlantRecipeBuilder implements RecipeBuilder {

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

    private final Advancement.Builder advancement = Advancement.Builder.advancement();

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
    public RecipeBuilder unlockedBy(@NotNull String name, @NotNull CriterionTriggerInstance criterion) {
        this.advancement.addCriterion(name, criterion);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        // Для custom machine recipe это значение неважно; вернём первый item-output либо AIR.
        for (ItemStack stack : itemOutputs) {
            if (!stack.isEmpty()) return stack.getItem();
        }
        return net.minecraft.world.item.Items.AIR;
    }

    @Override
    public void save(@NotNull Consumer<FinishedRecipe> writer, @NotNull ResourceLocation recipeId) {
        writer.accept(new Result(recipeId, this));
    }

    public void save(@NotNull Consumer<FinishedRecipe> writer, @NotNull String path) {
        //? if fabric && < 1.21.1 {
        save(writer, new ResourceLocation("hbm_m", path));
        //?} else {
                /^save(writer, ResourceLocation.fromNamespaceAndPath("hbm_m", path));
        ^///?}

    }

    private static final class Result implements FinishedRecipe {
        private final ResourceLocation id;
        private final ChemicalPlantRecipeBuilder builder;

        private Result(ResourceLocation id, ChemicalPlantRecipeBuilder builder) {
            this.id = id;
            this.builder = builder;
        }

        @Override
        public void serializeRecipeData(@NotNull JsonObject json) {
            json.addProperty("duration", builder.duration);
            json.addProperty("power", builder.power);

            if (builder.blueprintPool != null) {
                json.addProperty("blueprint_pool", builder.blueprintPool);
            }

            if (builder.iconItem != null && !builder.iconItem.isEmpty()) {
                JsonObject o = new JsonObject();
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(builder.iconItem.getItem());
                if (itemId != null) {
                    o.addProperty("item", itemId.toString());
                    if (builder.iconItem.getCount() > 1) {
                        o.addProperty("count", builder.iconItem.getCount());
                    }
                    json.add("icon_item", o);
                }
            }

            if (builder.iconFluid != null) {
                ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(builder.iconFluid);
                if (fluidId != null) {
                    json.addProperty("icon_fluid", fluidId.toString());
                }
            }

            JsonArray itemInputs = new JsonArray();
            for (CountedIngredient ci : builder.itemInputs) {
                JsonObject ing = ci.ingredient().toJson().getAsJsonObject();
                ing.addProperty("count", ci.count());
                itemInputs.add(ing);
            }
            json.add("item_inputs", itemInputs);

            JsonArray fluidInputs = new JsonArray();
            for (FluidAmount fa : builder.fluidInputs) {
                ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fa.fluid());
                if (fluidId == null) continue;
                JsonObject obj = new JsonObject();
                obj.addProperty("fluid", fluidId.toString());
                obj.addProperty("amount", fa.amount());
                fluidInputs.add(obj);
            }
            json.add("fluid_inputs", fluidInputs);

            JsonArray itemOutputs = new JsonArray();
            for (ItemStack out : builder.itemOutputs) {
                JsonObject o = new JsonObject();
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(out.getItem());
                if (itemId == null) continue;
                o.addProperty("item", itemId.toString());
                if (out.getCount() > 1) {
                    o.addProperty("count", out.getCount());
                }
                itemOutputs.add(o);
            }
            json.add("item_outputs", itemOutputs);

            JsonArray fluidOutputs = new JsonArray();
            for (FluidAmount fa : builder.fluidOutputs) {
                ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fa.fluid());
                if (fluidId == null) continue;
                JsonObject obj = new JsonObject();
                obj.addProperty("fluid", fluidId.toString());
                obj.addProperty("amount", fa.amount());
                fluidOutputs.add(obj);
            }
            json.add("fluid_outputs", fluidOutputs);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return ChemicalPlantRecipe.Serializer.INSTANCE;
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return null;
        }
    }
}
*///?}
