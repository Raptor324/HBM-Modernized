package com.hbm_m.recipe;

import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.block.machines.anvils.AnvilTier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class AnvilRecipe extends PlatformRecipe {

    /** Recipes without an explicit order sort last (original 1.7.10 uses registration order). */
    public static final int NO_ORDER = Integer.MAX_VALUE;

    private final AnvilIngredient inputA;
    private final AnvilIngredient inputB;
    private final boolean consumeA;
    private final boolean consumeB;
    private final List<AnvilIngredient> inventoryInputs;
    private final List<ResultEntry> outputs;
    private final ItemStack displayStack;
    private final AnvilTier requiredTier;
    @Nullable
    private final AnvilTier upperTier;
    @Nullable
    private final String blueprintPool;
    private final OverlayType overlay;
    private final boolean shapeless;
    private final int order;

    public AnvilRecipe(ResourceLocation id, AnvilIngredient inputA, AnvilIngredient inputB,
                       List<AnvilIngredient> inventoryInputs, List<ResultEntry> outputs,
                       AnvilTier requiredTier, @Nullable AnvilTier upperTier,
                       @Nullable String blueprintPool, OverlayType overlay,
                       boolean consumeA, boolean consumeB, boolean shapeless, int order) {
        super(id);
        this.inputA = inputA == null ? AnvilIngredient.EMPTY : inputA;
        this.inputB = inputB == null ? AnvilIngredient.EMPTY : inputB;
        this.consumeA = consumeA;
        this.consumeB = consumeB;
        this.inventoryInputs = Collections.unmodifiableList(new ArrayList<>(inventoryInputs));
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("Anvil recipe " + id + " must define at least one output");
        }
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
        this.displayStack = computeDisplayStack();
        this.requiredTier = requiredTier;
        this.upperTier = upperTier;
        this.blueprintPool = blueprintPool;
        this.overlay = overlay;
        this.shapeless = shapeless;
        this.order = order;
    }

    @Override
    public boolean matchesRecipe(RecipeInputWrapper container, Level level) {
        if (container.size() < 2) return false;

        if (!usesMachineInputs()) {
            return false;
        }
        return matches(container.getItem(0), container.getItem(1));
    }

    /**
     * Smithing-матчинг двух слотов. Порядок слотов имеет значение (как в 1.7.10),
     * зеркальный вариант принимается только для shapeless-рецептов.
     */
    public boolean matches(ItemStack slotA, ItemStack slotB) {
        if (!usesMachineInputs()) {
            return false;
        }
        return matchesExact(slotA, slotB) || (shapeless && matchesExact(slotB, slotA));
    }

    private boolean matchesExact(ItemStack slotA, ItemStack slotB) {
        return inputA.matches(slotA) && inputB.matches(slotB);
    }

    public boolean canCraftOn(AnvilTier tier) {
        int current = tier.getLegacyId();
        int min = this.requiredTier.getLegacyId();
        if (current < min) {
            return false;
        }
        if (upperTier != null) {
            return current <= upperTier.getLegacyId();
        }
        return true;
    }

    @Override
    public ItemStack assembleSafe() {
        return getResultItemSafe();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return outputs.get(0).stack().copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public AnvilIngredient getInputA() { return inputA; }
    public AnvilIngredient getInputB() { return inputB; }

    public boolean consumesA() { return consumeA; }
    public boolean consumesB() { return consumeB; }

    public List<AnvilIngredient> getInventoryInputs() {
        return inventoryInputs;
    }

    public List<ResultEntry> getOutputs() {
        return outputs;
    }

    public AnvilTier getRequiredTier() {
        return requiredTier;
    }

    @Nullable
    public String getBlueprintPool() {
        return blueprintPool;
    }

    public boolean requiresBlueprint() {
        return blueprintPool != null && !blueprintPool.isEmpty();
    }

    public boolean isShapeless() {
        return shapeless;
    }

    public int getOrder() {
        return order;
    }

    public OverlayType getOverlay() {
        return overlay;
    }

    public ItemStack getDisplayStack() {
        return displayStack.copy();
    }

    public boolean usesMachineInputs() {
        return !inputA.isEmpty() || !inputB.isEmpty();
    }

    public boolean isRecycling() {
        return overlay == OverlayType.RECYCLING;
    }

    // Возвращает входной предмет для отображения иконки при разборке
    public ItemStack getRecyclingInputStack() {
        if (!isRecycling()) {
            return ItemStack.EMPTY;
        }

        // Приоритет: inventoryInputs > inputA > inputB
        for (AnvilIngredient required : inventoryInputs) {
            if (!required.isEmpty()) {
                return required.display();
            }
        }

        if (!inputA.isEmpty()) {
            return inputA.display();
        }

        if (!inputB.isEmpty()) {
            return inputB.display();
        }

        return ItemStack.EMPTY;
    }

    private ItemStack computeDisplayStack() {
        if (overlay == OverlayType.RECYCLING) {
            ItemStack recycling = getRecyclingInputStack();
            if (!recycling.isEmpty()) {
                return recycling;
            }
        }
        if (!outputs.isEmpty()) {
            return outputs.get(0).stack().copy();
        }
        return ItemStack.EMPTY;
    }

    public record ResultEntry(ItemStack stack, float chance) { }

    /**
     * Ингредиент наковальни — аналог оригинального AStack 1.7.10:
     * список допустимых предметов (аналог ore dict) + требуемое количество.
     */
    public record AnvilIngredient(List<ItemStack> variants, int count) {

        public static final AnvilIngredient EMPTY = new AnvilIngredient(List.of(), 0);

        /** Один допустимый предмет (все варианты). Количество берётся из первого стека. */
        public static AnvilIngredient of(ItemStack first, ItemStack... more) {
            List<ItemStack> variants = new ArrayList<>();
            int count = 0;
            if (!first.isEmpty()) {
                count = Math.max(1, first.getCount());
                variants.add(single(first));
            }
            for (ItemStack extra : more) {
                if (!extra.isEmpty()) {
                    variants.add(single(extra));
                }
            }
            if (variants.isEmpty()) {
                return EMPTY;
            }
            return new AnvilIngredient(List.copyOf(variants), count);
        }

        public static AnvilIngredient ofCount(ItemStack stack, int count) {
            if (stack.isEmpty()) {
                return EMPTY;
            }
            return new AnvilIngredient(List.of(single(stack)), Math.max(1, count));
        }

        private static ItemStack single(ItemStack stack) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            return copy;
        }

        public boolean isEmpty() {
            return variants.isEmpty() || count <= 0;
        }

        public boolean matches(ItemStack actual) {
            if (isEmpty() || actual.isEmpty()) {
                return isEmpty() && actual.isEmpty();
            }
            for (ItemStack variant : variants) {
                if (PlatformHooks.hasItemTag(variant)) {
                    if (PlatformHooks.isSameItemSameTags(actual, variant)) {
                        return true;
                    }
                } else if (actual.is(variant.getItem())) {
                    return true;
                }
            }
            return false;
        }

        /** Репрезентативный стак для отображения (первый вариант, с количеством). */
        public ItemStack display() {
            if (variants.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack copy = variants.get(0).copy();
            copy.setCount(count);
            return copy;
        }

        /** Все допустимые имена — для поиска (как список oredict-вариантов в 1.7.10). */
        public List<String> displayNames() {
            List<String> names = new ArrayList<>();
            for (ItemStack variant : variants) {
                try {
                    names.add(variant.getHoverName().getString().toLowerCase(Locale.ROOT));
                } catch (Exception ex) {
                    names.add("error");
                }
            }
            return names;
        }
    }

    public static class Type implements RecipeType<AnvilRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "anvil";
    }

    public static class Serializer extends PlatformRecipeSerializer<AnvilRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public AnvilRecipe readJson(ResourceLocation id, JsonObject json) {
            AnvilIngredient inputA = json.has("input_a")
                    ? ingredientFromJson(GsonHelper.getAsJsonObject(json, "input_a"))
                    : AnvilIngredient.EMPTY;
            AnvilIngredient inputB = json.has("input_b")
                    ? ingredientFromJson(GsonHelper.getAsJsonObject(json, "input_b"))
                    : AnvilIngredient.EMPTY;

            boolean consumeA = GsonHelper.getAsBoolean(json, "consume_a", true);
            boolean consumeB = GsonHelper.getAsBoolean(json, "consume_b", true);

            List<AnvilIngredient> inventoryInputs = new ArrayList<>();
            if (json.has("required_items")) {
                JsonArray reqArray = GsonHelper.getAsJsonArray(json, "required_items");
                reqArray.forEach(element -> inventoryInputs.add(ingredientFromJson(element.getAsJsonObject())));
            }
            if (json.has("inventory_inputs")) {
                JsonArray reqArray = GsonHelper.getAsJsonArray(json, "inventory_inputs");
                reqArray.forEach(element -> inventoryInputs.add(ingredientFromJson(element.getAsJsonObject())));
            }

            List<ResultEntry> outputs = new ArrayList<>();
            if (json.has("outputs")) {
                JsonArray outArray = GsonHelper.getAsJsonArray(json, "outputs");
                outArray.forEach(element -> outputs.add(outputFromJson(element.getAsJsonObject())));
            } else if (json.has("output")) {
                ItemStack output = itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));
                float chance = Mth.clamp(GsonHelper.getAsFloat(json, "output_chance", 1.0F), 0.0F, 1.0F);
                outputs.add(new ResultEntry(output, chance));
            }

            String tierName = GsonHelper.getAsString(json, "tier", "iron");
            AnvilTier tier = AnvilTier.valueOf(tierName.toUpperCase(Locale.ROOT));
            AnvilTier upper = null;
            if (json.has("tier_upper")) {
                upper = AnvilTier.valueOf(GsonHelper.getAsString(json, "tier_upper").toUpperCase(Locale.ROOT));
            }
            String blueprintPool = GsonHelper.getAsString(json, "blueprint_pool", null);
            OverlayType overlay = OverlayType.byName(GsonHelper.getAsString(json, "overlay", "none"));
            boolean shapeless = GsonHelper.getAsBoolean(json, "shapeless", false);
            int order = GsonHelper.getAsInt(json, "order", NO_ORDER);

            return new AnvilRecipe(id, inputA, inputB, inventoryInputs, outputs, tier, upper,
                    blueprintPool, overlay, consumeA, consumeB, shapeless, order);
        }

        @Override
        public AnvilRecipe readNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            AnvilIngredient inputA = readIngredient(buffer);
            AnvilIngredient inputB = readIngredient(buffer);

            boolean consumeA = buffer.readBoolean();
            boolean consumeB = buffer.readBoolean();

            int size = buffer.readInt();
            List<AnvilIngredient> inventoryInputs = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                inventoryInputs.add(readIngredient(buffer));
            }

            int outputsSize = buffer.readInt();
            List<ResultEntry> outputs = new ArrayList<>();
            for (int i = 0; i < outputsSize; i++) {
                ItemStack stack = RecipeHooks.readItem(buffer);
                float chance = buffer.readFloat();
                outputs.add(new ResultEntry(stack, chance));
            }

            AnvilTier tier = AnvilTier.values()[buffer.readVarInt()];
            AnvilTier upper = null;
            int upperVal = buffer.readVarInt();
            if (upperVal >= 0 && upperVal < AnvilTier.values().length) {
                upper = AnvilTier.values()[upperVal];
            }
            String blueprintPool = buffer.readBoolean() ? buffer.readUtf() : null;
            OverlayType overlay = OverlayType.values()[buffer.readVarInt()];
            boolean shapeless = buffer.readBoolean();
            int order = buffer.readInt();

            return new AnvilRecipe(id, inputA, inputB, inventoryInputs, outputs, tier, upper,
                    blueprintPool, overlay, consumeA, consumeB, shapeless, order);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buffer, AnvilRecipe recipe) {
            writeIngredient(buffer, recipe.inputA);
            writeIngredient(buffer, recipe.inputB);

            buffer.writeBoolean(recipe.consumeA);
            buffer.writeBoolean(recipe.consumeB);

            buffer.writeInt(recipe.inventoryInputs.size());
            for (AnvilIngredient item : recipe.inventoryInputs) {
                writeIngredient(buffer, item);
            }

            buffer.writeInt(recipe.outputs.size());
            for (ResultEntry entry : recipe.outputs) {
                RecipeHooks.writeItem(buffer, entry.stack());
                buffer.writeFloat(entry.chance());
            }

            buffer.writeVarInt(recipe.requiredTier.ordinal());
            buffer.writeVarInt(recipe.upperTier != null ? recipe.upperTier.ordinal() : -1);
            if (recipe.blueprintPool != null) {
                buffer.writeBoolean(true);
                buffer.writeUtf(recipe.blueprintPool);
            } else {
                buffer.writeBoolean(false);
            }
            buffer.writeVarInt(recipe.overlay.ordinal());
            buffer.writeBoolean(recipe.shapeless);
            buffer.writeInt(recipe.order);
        }

        private static AnvilIngredient readIngredient(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            int variantCount = buffer.readVarInt();
            List<ItemStack> variants = new ArrayList<>(variantCount);
            for (int i = 0; i < variantCount; i++) {
                variants.add(RecipeHooks.readItem(buffer));
            }
            return new AnvilIngredient(List.copyOf(variants), count);
        }

        private static void writeIngredient(FriendlyByteBuf buffer, AnvilIngredient ingredient) {
            buffer.writeVarInt(ingredient.count());
            buffer.writeVarInt(ingredient.variants().size());
            for (ItemStack variant : ingredient.variants()) {
                RecipeHooks.writeItem(buffer, variant);
            }
        }

        private static AnvilIngredient ingredientFromJson(JsonObject object) {
            int count = GsonHelper.getAsInt(object, "count", 1);
            List<ItemStack> variants = new ArrayList<>();
            if (object.has("anyOf")) {
                JsonArray array = GsonHelper.getAsJsonArray(object, "anyOf");
                array.forEach(element -> variants.add(normalize(itemStackFromJson(element.getAsJsonObject()))));
            } else {
                variants.add(normalize(itemStackFromJson(object)));
            }
            return new AnvilIngredient(List.copyOf(variants), Mth.clamp(count, 0, Integer.MAX_VALUE));
        }

        private static ItemStack normalize(ItemStack stack) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            return copy;
        }

        private static ItemStack itemStackFromJson(JsonObject object) {
            return RecipeHooks.itemStackFromJson(object);
        }

        private static ResultEntry outputFromJson(JsonObject object) {
            ItemStack stack = itemStackFromJson(object);
            float chance = 1.0F;
            if (object.has("chance")) {
                chance = Mth.clamp(GsonHelper.getAsFloat(object, "chance"), 0.0F, 1.0F);
            }
            return new ResultEntry(stack, chance);
        }
    }

    public enum OverlayType {
        NONE,
        CONSTRUCTION,
        RECYCLING,
        SMITHING;

        private static OverlayType byName(String name) {
            for (OverlayType type : values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return NONE;
        }
    }
}
