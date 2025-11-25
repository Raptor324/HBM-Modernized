package com.hbm_m.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.block.AnvilTier;
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
    private final ItemStack output;
    private final List<ItemStack> requiredItems = new ArrayList<>();
    private final AnvilTier tier;
    private final Advancement.Builder advancement = Advancement.Builder.advancement();
    @Nullable
    private String blueprintPool;
    @Nullable
    private AnvilTier upperTier;
    private AnvilRecipe.OverlayType overlay = AnvilRecipe.OverlayType.NONE;
    private float outputChance = 1.0F;

    private AnvilRecipeBuilder(ItemStack inputA, ItemStack inputB, ItemStack output, AnvilTier tier) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
        this.tier = tier;
    }

    public static AnvilRecipeBuilder anvilRecipe(ItemStack inputA, ItemStack inputB, ItemStack output, AnvilTier tier) {
        return new AnvilRecipeBuilder(inputA, inputB, output, tier);
    }

    public AnvilRecipeBuilder addRequirement(ItemStack stack) {
        this.requiredItems.add(stack);
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

    public AnvilRecipeBuilder withOutputChance(float chance) {
        this.outputChance = Mth.clamp(chance, 0.0F, 1.0F);
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
        return this.output.getItem();
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
            json.add("input_a", stackToJson(builder.inputA));
            json.add("input_b", stackToJson(builder.inputB));
            json.add("output", stackToJson(builder.output));

            if (!builder.requiredItems.isEmpty()) {
                JsonArray array = new JsonArray();
                builder.requiredItems.forEach(stack -> array.add(stackToJson(stack)));
                json.add("required_items", array);
            }

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
            if (builder.outputChance < 1.0F) {
                json.addProperty("output_chance", builder.outputChance);
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
}

