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
 * Chemical Plant recipe (1.20.1).
 *
 * <p>Важно: {@link #matchesRecipe(RecipeInputWrapper, Level)} здесь не используется машиной напрямую — химзавод
 * работает позиционно по своим слотам/бакам и выбирает рецепт по ID. Тем не менее, рецепт должен быть
 * валиден для загрузки/показа/синхронизации и datagen.</p>
 *
 * <p><b>Унификация жидкостей:</b> жидкостные входы хранятся как {@link List}{@code <}{@link FluidStack}{@code >}
 * (Architectury), в едином формате с жидкостными выходами. Сериализация ведётся через общие хелперы
 * {@link RecipeHooks#readFluidStack}/{@link RecipeHooks#writeFluidStack} — те же, что у
 * {@code ArcFurnaceRecipe}/{@code CombinationOvenRecipe}/{@code CrystallizerRecipe}/{@code MixerRecipe}.
 * Прежний record {@code FluidIngredient(ResourceLocation, int)} удалён как дублирующий слой абстракции.</p>
 */
public class ChemicalPlantRecipe extends PlatformRecipe {

    public record CountedIngredient(Ingredient ingredient, int count) {}

    private final List<CountedIngredient> itemInputs;
    private final List<FluidStack> fluidInputs;
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
                               List<FluidStack> fluidInputs,
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

    /** Жидкостные входы — единый тип {@link FluidStack} (Architectury), как и выходы. */
    public List<FluidStack> getFluidInputs() {
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
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
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

    public static final class Type implements RecipeType<ChemicalPlantRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "chemical_plant";
    }

    public static final class Serializer extends PlatformRecipeSerializer<ChemicalPlantRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "chemical_plant");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "chemical_plant");
        //?}


        @Override
        public ChemicalPlantRecipe readJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            int power = GsonHelper.getAsInt(json, "power", 1000);
            String blueprintPool = GsonHelper.getAsString(json, "blueprint_pool", null);

            List<CountedIngredient> itemInputs = readItemInputs(json);
            List<FluidStack> fluidInputs = readFluidInputs(json);
            List<ItemStack> itemOutputs = readItemOutputs(json);
            List<FluidStack> fluidOutputs = readFluidOutputs(json);

            ResourceLocation iconFluid = readIconFluid(json);
            ItemStack iconItem = finalizeIconStack(readIconItem(json), iconFluid);

            return new ChemicalPlantRecipe(recipeId, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration, power, iconItem, iconFluid, blueprintPool);
        }

        @Override
        public ChemicalPlantRecipe readNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
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

            // Жидкостные входы — единый кросс-лоадерный формат (RecipeHooks.readFluidStack).
            int fluidInCount = buf.readVarInt();
            List<FluidStack> fluidInputs = new ArrayList<>(fluidInCount);
            for (int i = 0; i < fluidInCount; i++) {
                fluidInputs.add(RecipeHooks.readFluidStack(buf));
            }

            int itemOutCount = buf.readVarInt();
            List<ItemStack> itemOutputs = new ArrayList<>(itemOutCount);
            for (int i = 0; i < itemOutCount; i++) {
                itemOutputs.add(RecipeHooks.readItem(buf));
            }

            int fluidOutCount = buf.readVarInt();
            List<FluidStack> fluidOutputs = new ArrayList<>(fluidOutCount);
            for (int i = 0; i < fluidOutCount; i++) {
                fluidOutputs.add(RecipeHooks.readFluidStack(buf));
            }

            return new ChemicalPlantRecipe(recipeId, itemInputs, fluidInputs, itemOutputs, fluidOutputs, duration, power, iconItem, iconFluid, blueprintPool);
        }

        @Override
        public void writeNetwork(@NotNull FriendlyByteBuf buf, @NotNull ChemicalPlantRecipe recipe) {
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

            // Жидкостные входы — единый кросс-лоадерный формат (RecipeHooks.writeFluidStack).
            buf.writeVarInt(recipe.fluidInputs.size());
            for (FluidStack fi : recipe.fluidInputs) {
                RecipeHooks.writeFluidStack(buf, fi);
            }

            buf.writeVarInt(recipe.itemOutputs.size());
            for (ItemStack out : recipe.itemOutputs) {
                RecipeHooks.writeItem(buf, out);
            }

            buf.writeVarInt(recipe.fluidOutputs.size());
            for (FluidStack out : recipe.fluidOutputs) {
                RecipeHooks.writeFluidStack(buf, out);
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

        /**
         * Жидкостные входы — единый формат {@code { "fluid": <id>, "amount": <mB> }} через
         * {@link RecipeHooks#fluidStackOf}. Совпадает с форматом fluid_outputs и других рецептов.
         */
        private static List<FluidStack> readFluidInputs(JsonObject json) {
            if (!json.has("fluid_inputs")) return List.of();
            JsonArray arr = GsonHelper.getAsJsonArray(json, "fluid_inputs");
            List<FluidStack> result = new ArrayList<>(arr.size());
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid"));
                if (id == null) continue;
                long amount = GsonHelper.getAsLong(obj, "amount", 0L);
                result.add(RecipeHooks.fluidStackOf(id, amount));
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
                long amount = GsonHelper.getAsLong(obj, "amount", 0L);
                result.add(RecipeHooks.fluidStackOf(id, amount));
            }
            return result;
        }
    }
}
