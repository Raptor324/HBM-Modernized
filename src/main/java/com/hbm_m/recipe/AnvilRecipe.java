package com.hbm_m.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.block.custom.machines.anvils.AnvilTier;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AnvilRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final ItemStack inputA;
    private final ItemStack inputB;
    private final boolean consumeA;
    private final boolean consumeB;
    private final List<ItemStack> inventoryInputs;
    private final List<ResultEntry> outputs;
    private final ItemStack displayStack;
    private final AnvilTier requiredTier;
    @Nullable
    private final AnvilTier upperTier;
    @Nullable
    private final String blueprintPool;
    private final OverlayType overlay;

    public AnvilRecipe(ResourceLocation id, ItemStack inputA, ItemStack inputB,
                       List<ItemStack> inventoryInputs, List<ResultEntry> outputs,
                       AnvilTier requiredTier, @Nullable AnvilTier upperTier,
                       @Nullable String blueprintPool, OverlayType overlay, boolean consumeA, boolean consumeB) {
        this.id = id;
        this.inputA = inputA == null ? ItemStack.EMPTY : inputA;
        this.inputB = inputB == null ? ItemStack.EMPTY : inputB;
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
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (container.getContainerSize() < 2) return false;

        if (!usesMachineInputs()) {
            return false;
        }
        return matches(container.getItem(0), container.getItem(1));
    }

    public boolean matches(ItemStack slotA, ItemStack slotB) {
        if (!usesMachineInputs()) {
            return false;
        }
        return matchesExact(slotA, slotB) || matchesExact(slotB, slotA);
    }

    private boolean matchesExact(ItemStack slotA, ItemStack slotB) {
        return ItemStack.isSameItemSameTags(slotA, inputA) &&
               ItemStack.isSameItemSameTags(slotB, inputB) &&
               slotA.getCount() >= inputA.getCount() &&
               slotB.getCount() >= inputB.getCount();
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
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return outputs.get(0).stack().copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public ItemStack getInputA() { return inputA; }
    public ItemStack getInputB() { return inputB; }

    public boolean consumesA() { return consumeA; }
    public boolean consumesB() { return consumeB; }

    public List<ItemStack> getRequiredItems() {
        return inventoryInputs;
    }

    public List<ItemStack> getInventoryInputs() {
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
        if (!inventoryInputs.isEmpty()) {
            return inventoryInputs.get(0).copy();
        }
        
        if (!inputA.isEmpty()) {
            return inputA.copy();
        }
        
        if (!inputB.isEmpty()) {
            return inputB.copy();
        }
        
        return ItemStack.EMPTY;
    }

    private ItemStack computeDisplayStack() {
        if (overlay == OverlayType.RECYCLING) {
            if (!inventoryInputs.isEmpty()) {
                return inventoryInputs.get(0).copy();
            }
            if (!inputA.isEmpty()) {
                return inputA.copy();
            }
        }
        if (!outputs.isEmpty()) {
            return outputs.get(0).stack().copy();
        }
        return ItemStack.EMPTY;
    }

    public record ResultEntry(ItemStack stack, float chance) { }

    public static class Type implements RecipeType<AnvilRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "anvil";
    }

    public static class Serializer implements RecipeSerializer<AnvilRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public AnvilRecipe fromJson(ResourceLocation id, JsonObject json) {
            ItemStack inputA = json.has("input_a")
                    ? itemStackFromJson(GsonHelper.getAsJsonObject(json, "input_a"))
                    : ItemStack.EMPTY;
            ItemStack inputB = json.has("input_b")
                    ? itemStackFromJson(GsonHelper.getAsJsonObject(json, "input_b"))
                    : ItemStack.EMPTY;

            boolean consumeA = GsonHelper.getAsBoolean(json, "consume_a", true);
            boolean consumeB = GsonHelper.getAsBoolean(json, "consume_b", true);

            List<ItemStack> inventoryInputs = new ArrayList<>();
            if (json.has("required_items")) {
                JsonArray reqArray = GsonHelper.getAsJsonArray(json, "required_items");
                reqArray.forEach(element -> inventoryInputs.add(itemStackFromJson(element.getAsJsonObject())));
            }
            if (json.has("inventory_inputs")) {
                JsonArray reqArray = GsonHelper.getAsJsonArray(json, "inventory_inputs");
                reqArray.forEach(element -> inventoryInputs.add(itemStackFromJson(element.getAsJsonObject())));
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

            return new AnvilRecipe(id, inputA, inputB, inventoryInputs, outputs, tier, upper, blueprintPool, overlay, consumeA, consumeB);
        }

        @Override
        public AnvilRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ItemStack inputA = buffer.readItem();
            ItemStack inputB = buffer.readItem();

            boolean consumeA = buffer.readBoolean();
            boolean consumeB = buffer.readBoolean();

            int size = buffer.readInt();
            List<ItemStack> inventoryInputs = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                inventoryInputs.add(buffer.readItem());
            }

            int outputsSize = buffer.readInt();
            List<ResultEntry> outputs = new ArrayList<>();
            for (int i = 0; i < outputsSize; i++) {
                ItemStack stack = buffer.readItem();
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

            return new AnvilRecipe(id, inputA, inputB, inventoryInputs, outputs, tier, upper, blueprintPool, overlay, consumeA, consumeB);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AnvilRecipe recipe) {
            buffer.writeItem(recipe.inputA);
            buffer.writeItem(recipe.inputB);

            buffer.writeBoolean(recipe.consumeA);
            buffer.writeBoolean(recipe.consumeB);

            buffer.writeInt(recipe.inventoryInputs.size());
            for (ItemStack item : recipe.inventoryInputs) {
                buffer.writeItem(item);
            }

            buffer.writeInt(recipe.outputs.size());
            for (ResultEntry entry : recipe.outputs) {
                buffer.writeItem(entry.stack());
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
        }

        private static ItemStack itemStackFromJson(JsonObject object) {
            return ShapedRecipe.itemStackFromJson(object);
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
