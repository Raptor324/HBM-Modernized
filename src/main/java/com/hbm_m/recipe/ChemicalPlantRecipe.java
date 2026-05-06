package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * Chemical Plant recipe (1.20.1).
 *
 * <p>Важно: {@link #matches(SimpleContainer, Level)} здесь не используется машиной напрямую — химзавод
 * работает позиционно по своим слотам/бакам и выбирает рецепт по ID. Тем не менее, рецепт должен быть
 * валиден для загрузки/показа/синхронизации и datagen.</p>
 */
public class ChemicalPlantRecipe implements Recipe<SimpleContainer> {

    public record CountedIngredient(Ingredient ingredient, int count) {}

    public record FluidIngredient(ResourceLocation fluidId, int amount) {}

    private final ResourceLocation id;
    private final List<CountedIngredient> itemInputs;
    private final List<FluidIngredient> fluidInputs;
    private final List<ItemStack> itemOutputs;
    private final List<FluidStack> fluidOutputs;
    private final int duration;
    private final int powerConsumption;

    @Nullable
    private final ItemStack iconItem;

    @Nullable
    private final ResourceLocation iconFluid;

    @Nullable
    private final String blueprintPool;

    public ChemicalPlantRecipe(ResourceLocation id,
                               List<CountedIngredient> itemInputs,
                               List<FluidIngredient> fluidInputs,
                               List<ItemStack> itemOutputs,
                               List<FluidStack> fluidOutputs,
                               int duration,
                               int powerConsumption,
                               @Nullable ItemStack iconItem,
                               @Nullable ResourceLocation iconFluid,
                               @Nullable String blueprintPool) {
        this.id = id;
        this.itemInputs = itemInputs != null ? itemInputs : List.of();
        this.fluidInputs = fluidInputs != null ? fluidInputs : List.of();
        this.itemOutputs = itemOutputs != null ? itemOutputs : List.of();
        this.fluidOutputs = fluidOutputs != null ? fluidOutputs : List.of();
        this.duration = duration;
        this.powerConsumption = powerConsumption;
        this.iconItem = iconItem != null && !iconItem.isEmpty() ? iconItem : null;
        this.iconFluid = iconFluid;
        this.blueprintPool = blueprintPool;
    }

    public List<CountedIngredient> getItemInputs() {
        return itemInputs;
    }

    public List<FluidIngredient> getFluidInputs() {
        return fluidInputs;
    }

    public List<ItemStack> getItemOutputs() {
        return itemOutputs;
    }

    public List<FluidStack> getFluidOutputs() {
        return fluidOutputs;
    }

    public int getDuration() {
        return duration;
    }

    public int getPowerConsumption() {
        return powerConsumption;
    }

    @Nullable
    public String getBlueprintPool() {
        return blueprintPool;
    }

    public boolean requiresBlueprint() {
        return blueprintPool != null && !blueprintPool.isEmpty();
    }

    @Override
    public boolean matches(@NotNull SimpleContainer container, @NotNull Level level) {
        // Машина не использует стандартный shaped-мэтчинг.
        return false;
    }

    @Override
    public ItemStack assemble(@NotNull SimpleContainer container, @NotNull RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        if (iconItem != null && !iconItem.isEmpty()) {
            return iconItem.copy();
        }
        for (ItemStack out : itemOutputs) {
            if (!out.isEmpty()) return out.copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> expanded = NonNullList.create();
        for (CountedIngredient ci : itemInputs) {
            int count = Math.max(1, ci.count());
            for (int i = 0; i < count; i++) {
                expanded.add(ci.ingredient());
            }
        }
        return expanded;
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

    public static final class Type implements RecipeType<ChemicalPlantRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "chemical_plant";
    }

    public static final class Serializer implements RecipeSerializer<ChemicalPlantRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "chemical_plant");
        //?} else {
                /*public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "chemical_plant");
        *///?}


        @Override
        public ChemicalPlantRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            int power = GsonHelper.getAsInt(json, "power", 1000);
            String blueprintPool = GsonHelper.getAsString(json, "blueprint_pool", null);

            List<CountedIngredient> itemInputs = readItemInputs(json);
            List<FluidIngredient> fluidInputs = readFluidInputs(json);
            List<ItemStack> itemOutputs = readItemOutputs(json);
            List<FluidStack> fluidOutputs = readFluidOutputs(json);

            ItemStack iconItem = readIconItem(json);
            ResourceLocation iconFluid = readIconFluid(json);
            applyIconFluid(iconItem, iconFluid);

            return new ChemicalPlantRecipe(recipeId, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration, power, iconItem, iconFluid, blueprintPool);
        }

        @Override
        public @Nullable ChemicalPlantRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
            int duration = buf.readVarInt();
            int power = buf.readVarInt();
            String blueprintPool = buf.readBoolean() ? buf.readUtf() : null;

            ItemStack iconItem = buf.readBoolean() ? buf.readItem() : ItemStack.EMPTY;
            ResourceLocation iconFluid = buf.readBoolean() ? buf.readResourceLocation() : null;
            applyIconFluid(iconItem, iconFluid);

            int itemInCount = buf.readVarInt();
            List<CountedIngredient> itemInputs = new ArrayList<>(itemInCount);
            for (int i = 0; i < itemInCount; i++) {
                Ingredient ing = Ingredient.fromNetwork(buf);
                int count = buf.readVarInt();
                itemInputs.add(new CountedIngredient(ing, count));
            }

            int fluidInCount = buf.readVarInt();
            List<FluidIngredient> fluidInputs = new ArrayList<>(fluidInCount);
            for (int i = 0; i < fluidInCount; i++) {
                ResourceLocation fluidId = buf.readResourceLocation();
                int amount = buf.readVarInt();
                fluidInputs.add(new FluidIngredient(fluidId, amount));
            }

            int itemOutCount = buf.readVarInt();
            List<ItemStack> itemOutputs = new ArrayList<>(itemOutCount);
            for (int i = 0; i < itemOutCount; i++) {
                itemOutputs.add(buf.readItem());
            }

            int fluidOutCount = buf.readVarInt();
            List<FluidStack> fluidOutputs = new ArrayList<>(fluidOutCount);
            for (int i = 0; i < fluidOutCount; i++) {
                fluidOutputs.add(readFluidStack(buf));
            }

            return new ChemicalPlantRecipe(recipeId, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration, power, iconItem, iconFluid, blueprintPool);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull ChemicalPlantRecipe recipe) {
            buf.writeVarInt(recipe.duration);
            buf.writeVarInt(recipe.powerConsumption);

            if (recipe.blueprintPool != null) {
                buf.writeBoolean(true);
                buf.writeUtf(recipe.blueprintPool);
            } else {
                buf.writeBoolean(false);
            }

            if (recipe.iconItem != null && !recipe.iconItem.isEmpty()) {
                buf.writeBoolean(true);
                buf.writeItem(recipe.iconItem);
            } else {
                buf.writeBoolean(false);
            }

            if (recipe.iconFluid != null) {
                buf.writeBoolean(true);
                buf.writeResourceLocation(recipe.iconFluid);
            } else {
                buf.writeBoolean(false);
            }

            buf.writeVarInt(recipe.itemInputs.size());
            for (CountedIngredient ci : recipe.itemInputs) {
                ci.ingredient().toNetwork(buf);
                buf.writeVarInt(ci.count());
            }

            buf.writeVarInt(recipe.fluidInputs.size());
            for (FluidIngredient fi : recipe.fluidInputs) {
                buf.writeResourceLocation(fi.fluidId());
                buf.writeVarInt(fi.amount());
            }

            buf.writeVarInt(recipe.itemOutputs.size());
            for (ItemStack out : recipe.itemOutputs) {
                buf.writeItem(out);
            }

            buf.writeVarInt(recipe.fluidOutputs.size());
            for (FluidStack out : recipe.fluidOutputs) {
                writeFluidStack(buf, out);
            }
        }

        private static ItemStack readIconItem(JsonObject json) {
            if (!json.has("icon_item")) return ItemStack.EMPTY;
            JsonObject obj = GsonHelper.getAsJsonObject(json, "icon_item");
            return ShapedRecipe.itemStackFromJson(obj);
        }

        @Nullable
        private static ResourceLocation readIconFluid(JsonObject json) {
            if (!json.has("icon_fluid")) return null;
            return ResourceLocation.tryParse(GsonHelper.getAsString(json, "icon_fluid"));
        }

        private static void applyIconFluid(ItemStack iconItem, @Nullable ResourceLocation iconFluid) {
            if (iconItem == null || iconItem.isEmpty() || iconFluid == null) return;

            var fluid = BuiltInRegistries.FLUID.get(iconFluid);
            if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) return;

            // На данный момент поддерживаем «иконку+жидкость» через FLUID_IDENTIFIER (он умеет хранить fluid в NBT).
            // Если позже появятся канистры/гастэнки с совместимым NBT — расширим этот хук.
            if (iconItem.getItem() instanceof com.hbm_m.item.liquids.FluidIdentifierItem) {
                com.hbm_m.item.liquids.FluidIdentifierItem.setType(iconItem, fluid, true);
            }
        }

        /**
         * Platform-agnostic сериализация FluidStack.
         *
         * Не используем Forge-specific {@code FriendlyByteBuf#readFluidStack}/{@code writeFluidStack}
         * и {@code FluidStackHooksForge}, чтобы общий код компилировался на всех лоадерах.
         *
         * Формат:
         * - boolean present
         * - ResourceLocation fluidId
         * - varLong amount (mB)
         */
        private static FluidStack readFluidStack(FriendlyByteBuf buf) {
            boolean present = buf.readBoolean();
            if (!present) {
                return FluidStack.empty();
            }
            ResourceLocation id = buf.readResourceLocation();
            long amount = buf.readVarLong();
            var fluid = BuiltInRegistries.FLUID.get(id);
            if (fluid == null) {
                return FluidStack.empty();
            }
            if (amount <= 0) {
                return FluidStack.empty();
            }
            return FluidStack.create(fluid, amount);
        }

        private static void writeFluidStack(FriendlyByteBuf buf, FluidStack stack) {
            if (stack == null || stack.isEmpty()) {
                buf.writeBoolean(false);
                return;
            }
            buf.writeBoolean(true);
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
            buf.writeResourceLocation(id != null ? id : ResourceLocation.tryParse("minecraft:empty"));
            buf.writeVarLong(stack.getAmount());
        }

        private static List<CountedIngredient> readItemInputs(JsonObject json) {
            if (!json.has("item_inputs")) return List.of();
            JsonArray arr = GsonHelper.getAsJsonArray(json, "item_inputs");
            List<CountedIngredient> result = new ArrayList<>(arr.size());
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                Ingredient ing = Ingredient.fromJson(obj);
                int count = GsonHelper.getAsInt(obj, "count", 1);
                result.add(new CountedIngredient(ing, count));
            }
            return result;
        }

        private static List<FluidIngredient> readFluidInputs(JsonObject json) {
            if (!json.has("fluid_inputs")) return List.of();
            JsonArray arr = GsonHelper.getAsJsonArray(json, "fluid_inputs");
            List<FluidIngredient> result = new ArrayList<>(arr.size());
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid"));
                if (id == null) continue;
                int amount = GsonHelper.getAsInt(obj, "amount", 0);
                result.add(new FluidIngredient(id, amount));
            }
            return result;
        }

        private static List<ItemStack> readItemOutputs(JsonObject json) {
            if (!json.has("item_outputs")) return List.of();
            JsonArray arr = GsonHelper.getAsJsonArray(json, "item_outputs");
            List<ItemStack> result = new ArrayList<>(arr.size());
            for (JsonElement el : arr) {
                result.add(ShapedRecipe.itemStackFromJson(el.getAsJsonObject()));
            }
            return result;
        }

        private static List<FluidStack> readFluidOutputs(JsonObject json) {
            if (!json.has("fluid_outputs")) return List.of();
            JsonArray arr = GsonHelper.getAsJsonArray(json, "fluid_outputs");
            List<FluidStack> result = new ArrayList<>(arr.size());
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid"));
                if (id == null) continue;
                var fluid = BuiltInRegistries.FLUID.get(id);
                if (fluid == null) continue;
                int amount = GsonHelper.getAsInt(obj, "amount", 0);
                result.add(FluidStack.create(fluid, (long) amount));
            }
            return result;
        }
    }
}

