package com.hbm_m.datagen.recipes.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.block.custom.machines.anvils.AnvilTier;
import com.hbm_m.recipe.AnvilRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class AnvilRecipeBuilder implements RecipeBuilder {
    private final ItemStack inputA;
    private final ItemStack inputB;
    private boolean consumeA = true;
    private boolean consumeB = true;
    private final ItemStack primaryOutput;
    private final List<ItemStack> inventoryInputs = new ArrayList<>();
    private final List<OutputEntry> outputs = new ArrayList<>();
    private final AnvilTier tier;
    private final Advancement.Builder advancement = Advancement.Builder.advancement();
    @Nullable
    private String blueprintPool;
    @Nullable
    private AnvilTier upperTier;
    private AnvilRecipe.OverlayType overlay = AnvilRecipe.OverlayType.NONE;

    private AnvilRecipeBuilder(ItemStack inputA, ItemStack inputB, ItemStack output, AnvilTier tier) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.primaryOutput = output.copy();
        this.outputs.add(new OutputEntry(this.primaryOutput.copy(), 1.0F));
        this.tier = tier;
    }

    public AnvilRecipeBuilder keepInputA() {
        this.consumeA = false;
        return this;
    }

    public AnvilRecipeBuilder keepInputB() {
        this.consumeB = false;
        return this;
    }

    public static AnvilRecipeBuilder anvilRecipe(ItemStack inputA, ItemStack inputB, ItemStack output, AnvilTier tier) {
        return new AnvilRecipeBuilder(inputA, inputB, output, tier);
    }

    public AnvilRecipeBuilder addRequirement(ItemStack stack) {
        return addInventoryRequirement(stack);
    }

    public AnvilRecipeBuilder addInventoryRequirement(ItemStack stack) {
        this.inventoryInputs.add(stack.copy());
        return this;
    }

    public AnvilRecipeBuilder withBlueprintPool(String pool) {
        this.blueprintPool = pool;
        return this;
    }

    public AnvilRecipeBuilder withTierUpper(AnvilTier tier) {
        this.upperTier = tier;
        return this;
    }

    public AnvilRecipeBuilder withOverlay(AnvilRecipe.OverlayType overlay) {
        this.overlay = overlay;
        return this;
    }

    public AnvilRecipeBuilder addOutput(ItemStack stack) {
        return addOutput(stack, 1.0F);
    }

    public AnvilRecipeBuilder addOutput(ItemStack stack, float chance) {
        this.outputs.add(new OutputEntry(stack.copy(), Mth.clamp(chance, 0.0F, 1.0F)));
        return this;
    }

    public AnvilRecipeBuilder clearOutputs() {
        this.outputs.clear();
        return this;
    }

    @Override
    public AnvilRecipeBuilder unlockedBy(@Nonnull String criterionName, @Nonnull CriterionTriggerInstance criterionTrigger) {
        this.advancement.addCriterion(criterionName, criterionTrigger);
        return this;
    }

    @Override
    public AnvilRecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    @Override
    public Item getResult() {
        return this.primaryOutput.getItem();
    }

    @Override
    public void save(@Nonnull Consumer<FinishedRecipe> consumer, @Nonnull ResourceLocation recipeId) {
        consumer.accept(new Result(recipeId, this));
    }

    private static JsonObject stackToJson(ItemStack stack) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
        if (stack.getCount() > 1) {
            obj.addProperty("count", stack.getCount());
        }
        if (stack.hasTag()) {
            obj.addProperty("nbt", stack.getTag().toString());
        }
        return obj;
    }

    private static class Result implements FinishedRecipe {
        private final ResourceLocation id;
        private final AnvilRecipeBuilder builder;

        private Result(ResourceLocation id, AnvilRecipeBuilder builder) {
            this.id = id;
            this.builder = builder;
        }

        @Override
        public void serializeRecipeData(@Nonnull JsonObject json) {
            if (!builder.inputA.isEmpty()) {
                json.add("input_a", stackToJson(builder.inputA));
            }
            if (!builder.inputB.isEmpty()) {
                json.add("input_b", stackToJson(builder.inputB));
            }

            if (!builder.consumeA) json.addProperty("consume_a", false);
            if (!builder.consumeB) json.addProperty("consume_b", false);

            if (!builder.inventoryInputs.isEmpty()) {
                JsonArray array = new JsonArray();
                builder.inventoryInputs.forEach(stack -> array.add(stackToJson(stack)));
                json.add("required_items", array);
            }

            if (builder.outputs.isEmpty()) {
                throw new IllegalStateException("Anvil recipe " + id + " has no outputs");
            }
            JsonArray outputsArray = new JsonArray();
            builder.outputs.forEach(entry -> {
                JsonObject entryJson = stackToJson(entry.stack());
                if (entry.chance() < 1.0F) {
                    entryJson.addProperty("chance", entry.chance());
                }
                outputsArray.add(entryJson);
            });
            json.add("outputs", outputsArray);

            json.addProperty("tier", builder.tier.name().toLowerCase(Locale.ROOT));
            if (builder.upperTier != null) {
                json.addProperty("tier_upper", builder.upperTier.name().toLowerCase(Locale.ROOT));
            }
            if (builder.blueprintPool != null) {
                json.addProperty("blueprint_pool", builder.blueprintPool);
            }
            if (builder.overlay != AnvilRecipe.OverlayType.NONE) {
                json.addProperty("overlay", builder.overlay.name().toLowerCase(Locale.ROOT));
            }
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return AnvilRecipe.Serializer.INSTANCE;
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

    private record OutputEntry(ItemStack stack, float chance) { }
}

