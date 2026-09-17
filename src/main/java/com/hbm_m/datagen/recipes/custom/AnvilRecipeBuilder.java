package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipe.AnvilIngredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class AnvilRecipeBuilder extends BaseRecipeBuilder<AnvilRecipeBuilder> {

    private AnvilIngredient inputA;
    private AnvilIngredient inputB;
    private boolean consumeA = true;
    private boolean consumeB = true;
    private final ItemStack primaryOutput;
    private final List<AnvilIngredient> inventoryInputs = new ArrayList<>();
    private final List<OutputEntry> outputs = new ArrayList<>();
    private final AnvilTier tier;

    @Nullable
    private String blueprintPool;
    @Nullable
    private AnvilTier upperTier;
    private AnvilRecipe.OverlayType overlay = AnvilRecipe.OverlayType.NONE;
    private boolean shapeless = false;
    private int order = AnvilRecipe.NO_ORDER;

    private AnvilRecipeBuilder(AnvilIngredient inputA, AnvilIngredient inputB, ItemStack output, AnvilTier tier) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.primaryOutput = output.copy();
        this.outputs.add(new OutputEntry(this.primaryOutput.copy(), 1.0F));
        this.tier = tier;
    }

    public static AnvilRecipeBuilder anvilRecipe(ItemStack inputA, ItemStack inputB, ItemStack output, AnvilTier tier) {
        return new AnvilRecipeBuilder(AnvilIngredient.of(inputA), AnvilIngredient.of(inputB), output, tier);
    }

    public static AnvilRecipeBuilder anvilRecipe(AnvilIngredient inputA, AnvilIngredient inputB, ItemStack output, AnvilTier tier) {
        return new AnvilRecipeBuilder(inputA, inputB, output, tier);
    }

    /** Рецепт construction/recycling: оба машинных слота пусты, материалы берутся из инвентаря. */
    public static AnvilRecipeBuilder inventoryRecipe(ItemStack output, AnvilTier tier) {
        return new AnvilRecipeBuilder(AnvilIngredient.EMPTY, AnvilIngredient.EMPTY, output, tier);
    }

    public AnvilRecipeBuilder keepInputA() {
        this.consumeA = false;
        return this;
    }

    public AnvilRecipeBuilder keepInputB() {
        this.consumeB = false;
        return this;
    }

    /** Рецепт нечувствителен к порядку слотов (оригинальный {@code makeShapeless()}). */
    public AnvilRecipeBuilder withShapeless() {
        this.shapeless = true;
        return this;
    }

    /** Позиция рецепта в списке GUI — порядок регистрации оригинала. */
    public AnvilRecipeBuilder withOrder(int order) {
        this.order = order;
        return this;
    }

    public AnvilRecipeBuilder addRequirement(ItemStack stack) {
        return addInventoryRequirement(stack);
    }

    public AnvilRecipeBuilder addInventoryRequirement(ItemStack stack) {
        this.inventoryInputs.add(AnvilIngredient.of(stack));
        return this;
    }

    /** Ингредиент с несколькими допустимыми предметами (аналог ore dict стека). */
    public AnvilRecipeBuilder addInventoryRequirement(AnvilIngredient ingredient) {
        this.inventoryInputs.add(ingredient);
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
    public Item getResult() {
        return this.primaryOutput.getItem();
    }

    @Override
    protected JsonObject stackToJson(ItemStack stack) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getCount() > 1) {
            obj.addProperty("count", stack.getCount());
        }
        if (PlatformHooks.hasItemTag(stack)) {
            obj.addProperty("nbt", PlatformHooks.getItemTag(stack).toString());
        }
        return obj;
    }

    private JsonObject ingredientToJson(AnvilIngredient ingredient) {
        JsonObject obj = new JsonObject();
        obj.addProperty("count", ingredient.count());
        if (ingredient.variants().size() == 1) {
            JsonObject variant = stackToJson(ingredient.variants().get(0));
            obj.addProperty("item", variant.get("item").getAsString());
            if (variant.has("nbt")) {
                obj.add("nbt", variant.get("nbt"));
            }
        } else {
            JsonArray anyOf = new JsonArray();
            ingredient.variants().forEach(variant -> {
                JsonObject variantJson = stackToJson(variant);
                variantJson.remove("count");
                anyOf.add(variantJson);
            });
            obj.add("anyOf", anyOf);
        }
        return obj;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        if (!inputA.isEmpty()) {
            json.add("input_a", ingredientToJson(inputA));
        }
        if (!inputB.isEmpty()) {
            json.add("input_b", ingredientToJson(inputB));
        }

        if (!consumeA) json.addProperty("consume_a", false);
        if (!consumeB) json.addProperty("consume_b", false);

        if (!inventoryInputs.isEmpty()) {
            JsonArray array = new JsonArray();
            inventoryInputs.forEach(ingredient -> array.add(ingredientToJson(ingredient)));
            json.add("required_items", array);
        }

        if (outputs.isEmpty()) {
            throw new IllegalStateException("Anvil recipe has no outputs");
        }

        JsonArray outputsArray = new JsonArray();
        outputs.forEach(entry -> {
            JsonObject entryJson = stackToJson(entry.stack());
            if (entry.chance() < 1.0F) {
                entryJson.addProperty("chance", entry.chance());
            }
            outputsArray.add(entryJson);
        });
        json.add("outputs", outputsArray);

        json.addProperty("tier", tier.name().toLowerCase(Locale.ROOT));
        if (upperTier != null) {
            json.addProperty("tier_upper", upperTier.name().toLowerCase(Locale.ROOT));
        }
        if (blueprintPool != null) {
            json.addProperty("blueprint_pool", blueprintPool);
        }
        if (overlay != AnvilRecipe.OverlayType.NONE) {
            json.addProperty("overlay", overlay.name().toLowerCase(Locale.ROOT));
        }
        if (shapeless) {
            json.addProperty("shapeless", true);
        }
        if (order != AnvilRecipe.NO_ORDER) {
            json.addProperty("order", order);
        }
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return AnvilRecipe.Serializer.INSTANCE;
    }

    private record OutputEntry(ItemStack stack, float chance) { }
}
//?}