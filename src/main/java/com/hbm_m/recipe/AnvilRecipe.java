package com.hbm_m.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.block.AnvilTier;
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
    private final ItemStack output;
    private final List<ItemStack> requiredItems;
    private final AnvilTier requiredTier;
    @Nullable
    private final AnvilTier upperTier;
    @Nullable
    private final String blueprintPool;
    private final OverlayType overlay;
    private final float outputChance;

    public AnvilRecipe(ResourceLocation id, ItemStack inputA, ItemStack inputB, ItemStack output,
                       List<ItemStack> requiredItems, AnvilTier requiredTier, @Nullable AnvilTier upperTier,
                       @Nullable String blueprintPool, OverlayType overlay, float outputChance) {
        this.id = id;
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
        this.requiredItems = Collections.unmodifiableList(requiredItems);
        this.requiredTier = requiredTier;
        this.upperTier = upperTier;
        this.blueprintPool = blueprintPool;
        this.overlay = overlay;
        this.outputChance = outputChance;
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (container.getContainerSize() < 2) return false;

        ItemStack first = container.getItem(0);
        ItemStack second = container.getItem(1);
        return matches(first, second);
    }

    public boolean matches(ItemStack slotA, ItemStack slotB) {
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
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return output.copy();
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

    public ItemStack getInputA() {
        return inputA;
    }

    public ItemStack getInputB() {
        return inputB;
    }

    public List<ItemStack> getRequiredItems() {
        return requiredItems;
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
        if (overlay == OverlayType.RECYCLING && !inputA.isEmpty()) {
            return inputA.copy();
        }
        return output.copy();
    }

    public float getOutputChance() {
        return outputChance;
    }

    public static class Type implements RecipeType<AnvilRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "anvil";
    }

    public static class Serializer implements RecipeSerializer<AnvilRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public AnvilRecipe fromJson(ResourceLocation id, JsonObject json) {
            ItemStack inputA = itemStackFromJson(GsonHelper.getAsJsonObject(json, "input_a"));
            ItemStack inputB = itemStackFromJson(GsonHelper.getAsJsonObject(json, "input_b"));
            ItemStack output = itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));

            List<ItemStack> required = new ArrayList<>();
            if (json.has("required_items")) {
                JsonArray reqArray = GsonHelper.getAsJsonArray(json, "required_items");
                reqArray.forEach(element -> required.add(itemStackFromJson(element.getAsJsonObject())));
            }

            String tierName = GsonHelper.getAsString(json, "tier", "iron");
            AnvilTier tier = AnvilTier.valueOf(tierName.toUpperCase(Locale.ROOT));
            AnvilTier upper = null;
            if (json.has("tier_upper")) {
                upper = AnvilTier.valueOf(GsonHelper.getAsString(json, "tier_upper").toUpperCase(Locale.ROOT));
            }
            String blueprintPool = GsonHelper.getAsString(json, "blueprint_pool", null);
            OverlayType overlay = OverlayType.byName(GsonHelper.getAsString(json, "overlay", "none"));
            float outputChance = Mth.clamp(GsonHelper.getAsFloat(json, "output_chance", 1.0F), 0.0F, 1.0F);

            return new AnvilRecipe(id, inputA, inputB, output, required, tier, upper, blueprintPool, overlay, outputChance);
        }

        @Override
        public AnvilRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ItemStack inputA = buffer.readItem();
            ItemStack inputB = buffer.readItem();
            ItemStack output = buffer.readItem();

            int size = buffer.readInt();
            List<ItemStack> requiredItems = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                requiredItems.add(buffer.readItem());
            }

            AnvilTier tier = AnvilTier.values()[buffer.readVarInt()];
            AnvilTier upper = null;
            int upperVal = buffer.readVarInt();
            if (upperVal >= 0 && upperVal < AnvilTier.values().length) {
                upper = AnvilTier.values()[upperVal];
            }
            String blueprintPool = buffer.readBoolean() ? buffer.readUtf() : null;
            OverlayType overlay = OverlayType.values()[buffer.readVarInt()];
            float outputChance = buffer.readFloat();

            return new AnvilRecipe(id, inputA, inputB, output, requiredItems, tier, upper, blueprintPool, overlay, outputChance);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AnvilRecipe recipe) {
            buffer.writeItem(recipe.inputA);
            buffer.writeItem(recipe.inputB);
            buffer.writeItem(recipe.output);

            buffer.writeInt(recipe.requiredItems.size());
            for (ItemStack item : recipe.requiredItems) {
                buffer.writeItem(item);
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
            buffer.writeFloat(recipe.outputChance);
        }

        private static ItemStack itemStackFromJson(JsonObject object) {
            return ShapedRecipe.itemStackFromJson(object);
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
