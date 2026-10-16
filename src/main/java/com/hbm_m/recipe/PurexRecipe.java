package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * PUREX recipe (cross-version 1.20.1 / 1.21.1).
 *
 * <p>Важно: {@link #matchesRecipe(RecipeInputWrapper, Level)} здесь не используется машиной напрямую — химзавод
 * работает позиционно по своим слотам/бакам и выбирает рецепт по ID. Тем не менее, рецепт должен быть
 * валиден для загрузки/показа/синхронизации и datagen.</p>
 *
 * <p>Наследуется от {@link PlatformRecipe} (кросс-версионная база: {@code Recipe<Container>} на 1.20.1,
 * {@code Recipe<RecipeInput>} на 1.21.1). Сериализатор наследуется от {@link PlatformRecipeSerializer},
 * который предоставляет {@code codec()}/{@code streamCodec()} для 1.21.1 через декоратор над
 * {@code readJson}/{@code readNetwork}/{@code writeNetwork}.</p>
 */
public class PurexRecipe extends PlatformRecipe {

    public record CountedIngredient(Ingredient ingredient, int count) {}

    public record FluidIngredient(ResourceLocation fluidId, int amount) {}

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

    public PurexRecipe(ResourceLocation id,
                               List<CountedIngredient> itemInputs,
                               List<FluidIngredient> fluidInputs,
                               List<ItemStack> itemOutputs,
                               List<FluidStack> fluidOutputs,
                               int duration,
                               int powerConsumption,
                               @Nullable ItemStack iconItem,
                               @Nullable ResourceLocation iconFluid,
                               @Nullable String blueprintPool) {
        super(id);
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

    // =====================================================================================
    //  PlatformRecipe: кросс-версионные контракты.
    //  matches/assemble/getResultItem на обеих версиях делегируют сюда (см. PlatformRecipe).
    // =====================================================================================

    @Override
    public boolean matchesRecipe(RecipeInputWrapper input, Level level) {
        // Машина не использует стандартный shaped-мэтчинг.
        return false;
    }

    @Override
    public ItemStack assembleSafe() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItemSafe() {
        if (iconItem != null && !iconItem.isEmpty()) {
            return iconItem.copy();
        }
        for (ItemStack out : itemOutputs) {
            if (!out.isEmpty()) return out.copy();
        }
        // Как в 1.7.10 GenericRecipe.getIcon: при отсутствии иконки/предмета — первый fluid output.
        for (FluidStack fs : fluidOutputs) {
            if (fs == null || fs.isEmpty()) continue;
            ItemStack stack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
            FluidIdentifierItem.setType(stack, fs.getFluid(), true);
            return stack;
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
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static final class Type implements RecipeType<PurexRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "purex";
    }

    public static final class Serializer extends PlatformRecipeSerializer<PurexRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "purex");
        *///?} else {
                public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "purex");
        //?}


        @Override
        public PurexRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            int power = GsonHelper.getAsInt(json, "power", 1000);
            String blueprintPool = GsonHelper.getAsString(json, "blueprint_pool", null);

            List<CountedIngredient> itemInputs = readItemInputs(json);
            List<FluidIngredient> fluidInputs = readFluidInputs(json);
            List<ItemStack> itemOutputs = readItemOutputs(json);
            List<FluidStack> fluidOutputs = readFluidOutputs(json);

            ResourceLocation iconFluid = readIconFluid(json);
            ItemStack iconItem = finalizeIconStack(readIconItem(json), iconFluid);

            return new PurexRecipe(recipeId, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration, power, iconItem, iconFluid, blueprintPool);
        }

        @Override
        public PurexRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            int duration = buf.readVarInt();
            int power = buf.readVarInt();
            String blueprintPool = buf.readBoolean() ? buf.readUtf() : null;

            ItemStack iconItem = buf.readBoolean() ? RecipeHooks.readItem(buf) : ItemStack.EMPTY;
            ResourceLocation iconFluid = buf.readBoolean() ? buf.readResourceLocation() : null;
            iconItem = finalizeIconStack(iconItem, iconFluid);

            int itemInCount = buf.readVarInt();
            List<CountedIngredient> itemInputs = new ArrayList<>(itemInCount);
            for (int i = 0; i < itemInCount; i++) {
                Ingredient ing = RecipeHooks.readIngredient(buf);
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
                itemOutputs.add(RecipeHooks.readItem(buf));
            }

            int fluidOutCount = buf.readVarInt();
            List<FluidStack> fluidOutputs = new ArrayList<>(fluidOutCount);
            for (int i = 0; i < fluidOutCount; i++) {
                fluidOutputs.add(readFluidStack(buf));
            }

            return new PurexRecipe(recipeId, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration, power, iconItem, iconFluid, blueprintPool);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, PurexRecipe recipe) {
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
                RecipeHooks.writeItem(buf, recipe.iconItem);
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
                RecipeHooks.writeIngredient(buf, ci.ingredient());
                buf.writeVarInt(ci.count());
            }

            buf.writeVarInt(recipe.fluidInputs.size());
            for (FluidIngredient fi : recipe.fluidInputs) {
                buf.writeResourceLocation(fi.fluidId());
                buf.writeVarInt(fi.amount());
            }

            buf.writeVarInt(recipe.itemOutputs.size());
            for (ItemStack out : recipe.itemOutputs) {
                RecipeHooks.writeItem(buf, out);
            }

            buf.writeVarInt(recipe.fluidOutputs.size());
            for (FluidStack out : recipe.fluidOutputs) {
                writeFluidStack(buf, out);
            }
        }

        private static ItemStack readIconItem(JsonObject json) {
            if (!json.has("icon_item")) return ItemStack.EMPTY;
            JsonObject obj = GsonHelper.getAsJsonObject(json, "icon_item");
            return RecipeHooks.itemStackFromJson(obj);
        }

        @Nullable
        private static ResourceLocation readIconFluid(JsonObject json) {
            if (!json.has("icon_fluid")) return null;
            return ResourceLocation.tryParse(GsonHelper.getAsString(json, "icon_fluid"));
        }

        /**
         * Если задан только {@code icon_fluid} — создаётся стак {@link FluidIdentifierItem} (как в 1.7.10 ItemFluidIcon).
         * Если заданы оба — для идентификатора прокидывается тип в NBT.
         */
        private static ItemStack finalizeIconStack(ItemStack iconItem, @Nullable ResourceLocation iconFluid) {
            if (iconItem != null && !iconItem.isEmpty()) {
                applyIconFluid(iconItem, iconFluid);
                return iconItem;
            }
            if (iconFluid == null) {
                return ItemStack.EMPTY;
            }
            Fluid fluid = BuiltInRegistries.FLUID.get(iconFluid);
            if (fluid == null || fluid == Fluids.EMPTY) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
            FluidIdentifierItem.setType(stack, fluid, true);
            return stack;
        }

        private static void applyIconFluid(ItemStack iconItem, @Nullable ResourceLocation iconFluid) {
            if (iconItem == null || iconItem.isEmpty() || iconFluid == null) return;

            Fluid fluid = BuiltInRegistries.FLUID.get(iconFluid);
            if (fluid == null || fluid == Fluids.EMPTY) return;

            // На данный момент поддерживаем «иконку+жидкость» через FLUID_IDENTIFIER (он умеет хранить fluid в NBT).
            // Если позже появятся канистры/гастэнки с совместимым NBT — расширим этот хук.
            if (iconItem.getItem() instanceof FluidIdentifierItem) {
                FluidIdentifierItem.setType(iconItem, fluid, true);
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
                int count = GsonHelper.getAsInt(obj, "count", 1);
                Ingredient ing;
                if (obj.has("items")) {
                    ing = RecipeHooks.ingredientFromJson(obj.get("items"));
                } else {
                    JsonObject clone = obj.deepCopy();
                    clone.remove("count");
                    ing = RecipeHooks.ingredientFromJson(clone);
                }
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
                result.add(RecipeHooks.itemStackFromJson(el.getAsJsonObject()));
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
