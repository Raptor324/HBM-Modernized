package com.hbm_m.platform.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//? if >= 1.21.1 {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?}

public class RecipeHooks {
    public static ItemStack readItem(FriendlyByteBuf buf) {
        //? if < 1.21.1 {
        return buf.readItem();
        //?} else {
        /*return ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
        *///?}
    }
    
    public static void writeItem(FriendlyByteBuf buf, ItemStack stack) {
        //? if < 1.21.1 {
        buf.writeItem(stack);
        //?} else {
        /*ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, stack);
        *///?}
    }

    public static Ingredient readIngredient(FriendlyByteBuf buf) {
        //? if < 1.21.1 {
        return Ingredient.fromNetwork(buf);
        //?} else {
        /*return Ingredient.CONTENTS_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buf);
        *///?}
    }

    public static void writeIngredient(FriendlyByteBuf buf, Ingredient ingredient) {
        //? if < 1.21.1 {
        ingredient.toNetwork(buf);
        //?} else {
        /*Ingredient.CONTENTS_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buf, ingredient);
        *///?}
    }

    public static Ingredient ingredientFromJson(JsonElement json) {
        //? if < 1.21.1 {
        return Ingredient.fromJson(json);
        //?} else {
        /*return Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        *///?}
    }

    public static JsonElement ingredientToJson(Ingredient ingredient) {
        //? if < 1.21.1 {
        return ingredient.toJson();
        //?} else {
        /*return Ingredient.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, ingredient).getOrThrow();
        *///?}
    }

    public static ItemStack itemStackFromJson(JsonObject json) {
        //? if < 1.21.1 {
        return net.minecraft.world.item.crafting.ShapedRecipe.itemStackFromJson(json);
        //?} else {
        /*return ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        *///?}
    }

    // =====================================================================================
    //  FluidStack (Architectury) — кросс-лоадерная сериализация жидкостных стаков.
    //
    //  Общий формат FriendlyByteBuf:
    //    - boolean present
    //    - ResourceLocation fluidId
    //    - varLong amount (mB)
    //  Используется рецептами с жидкостными выходами (ChemicalPlant, ArcFurnace, ...).
    //  Перенесено из ChemicalPlantRecipe.Serializer (private) — общий доступ без дублирования.
    // =====================================================================================

    public static dev.architectury.fluid.FluidStack readFluidStack(FriendlyByteBuf buf) {
        boolean present = buf.readBoolean();
        if (!present) {
            return dev.architectury.fluid.FluidStack.empty();
        }
        ResourceLocation id = buf.readResourceLocation();
        long amount = buf.readVarLong();
        net.minecraft.world.level.material.Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
            return dev.architectury.fluid.FluidStack.empty();
        }
        if (amount <= 0) {
            return dev.architectury.fluid.FluidStack.empty();
        }
        return dev.architectury.fluid.FluidStack.create(fluid, amount);
    }

    public static void writeFluidStack(FriendlyByteBuf buf, dev.architectury.fluid.FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(stack.getFluid());
        buf.writeResourceLocation(id != null ? id : ResourceLocation.tryParse("minecraft:empty"));
        buf.writeVarLong(stack.getAmount());
    }

    /** Создаёт FluidStack из ResourceLocation жидкости (с резолвом через {@code BuiltInRegistries.FLUID}) — null/empty-safe. */
    public static dev.architectury.fluid.FluidStack fluidStackOf(ResourceLocation fluidId, long amount) {
        if (fluidId == null || amount <= 0) {
            return dev.architectury.fluid.FluidStack.empty();
        }
        net.minecraft.world.level.material.Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
            return dev.architectury.fluid.FluidStack.empty();
        }
        return dev.architectury.fluid.FluidStack.create(fluid, amount);
    }

    // =====================================================================================
    //  RecipeManager access (кросс-версионный: прячет RecipeHolder на 1.21.1).
    //
    //  1.20.1: getAllRecipesFor/getRecipeFor/byKey возвращают сами рецепты; Recipe несёт
    //          свой id через getId().
    //  1.21.1: те же методы возвращают RecipeHolder<T>; id живёт ТОЛЬКО на holder
    //          (recipe.id == dummy, т.к. PlatformRecipeSerializer передаёт dummy-id при декоде).
    //  Все потребители зовут эти хелперы — ветвление собрано здесь, нигде больше.
    // =====================================================================================

    /**
     * Все рецепты типа, развёрнутые из {@code RecipeHolder} на 1.21.1.
     * Заменяет {@code manager.getAllRecipesFor(type)} в потребителях.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <R extends Recipe<?>> List<R> getAllRecipes(RecipeManager manager, RecipeType<R> type) {
        //? if < 1.21.1 {
        return (List<R>) manager.getAllRecipesFor((RecipeType) type);
        //?} else {
        /*return ((List<RecipeHolder<R>>) (Object) manager.getAllRecipesFor((RecipeType) type)).stream()
                .map(RecipeHolder::value)
                .toList();
        *///?}
    }

    /** Удобная обёртка: {@code getAllRecipes(level.getRecipeManager(), type)}. */
    public static <R extends Recipe<?>> List<R> getAllRecipes(Level level, RecipeType<R> type) {
        return getAllRecipes(level.getRecipeManager(), type);
    }

    /**
     * id → recipe. На 1.21.1 id берётся из {@code holder.id()} (recipe.id ненадёжен — dummy).
     * Заменяет итерации с {@code recipe.getId()} в потребителях.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <R extends Recipe<?>> Map<ResourceLocation, R> getAllRecipesById(RecipeManager manager, RecipeType<R> type) {
        Map<ResourceLocation, R> map = new LinkedHashMap<>();
        //? if < 1.21.1 {
        List<R> recipes = (List<R>) manager.getAllRecipesFor((RecipeType) type);
        for (R r : recipes) {
            map.put(r.getId(), r);
        }
        //?} else {
        /*List<RecipeHolder<R>> holders = (List<RecipeHolder<R>>) (Object) manager.getAllRecipesFor((RecipeType) type);
        for (RecipeHolder<R> holder : holders) {
            map.put(holder.id(), holder.value());
        }
        *///?}
        return map;
    }

    /** Удобная обёртка: {@code getAllRecipesById(level.getRecipeManager(), type)}. */
    public static <R extends Recipe<?>> Map<ResourceLocation, R> getAllRecipesById(Level level, RecipeType<R> type) {
        return getAllRecipesById(level.getRecipeManager(), type);
    }

    /**
     * {@code RecipeManager.byKey(id)}, развёрнутый из {@code Optional<RecipeHolder>} на 1.21.1.
     */
    @SuppressWarnings("unchecked")
    public static Optional<Recipe<?>> getRecipeByKey(RecipeManager manager, ResourceLocation id) {
        //? if < 1.21.1 {
        return (Optional<Recipe<?>>) (Object) manager.byKey(id);
        //?} else {
        /*return ((Optional<RecipeHolder<?>>) (Object) manager.byKey(id)).map(RecipeHolder::value);
        *///?}
    }

    /**
     * Reverse-lookup id рецепта по ссылке. На 1.20.1 — {@code recipe.getId()} напрямую;
     * на 1.21.1 — поиск по {@link #getAllRecipesById} (id живёт на holder, не на recipe,
     * т.к. сериализатор передаёт dummy-id при декоде).
     *
     * <p>Drop-in замена для {@code recipe.getId()}. Для редких UI/модульных вызовов;
     * в per-tick hot-loop кэшируйте результат или итерируйте id-карту напрямую.
     */
    public static <R extends Recipe<?>> ResourceLocation recipeId(RecipeManager manager, RecipeType<R> type, R recipe) {
        if (recipe == null) return null;
        //? if < 1.21.1 {
        return recipe.getId();
        //?} else {
        /*for (Map.Entry<ResourceLocation, R> e : getAllRecipesById(manager, type).entrySet()) {
            if (e.getValue() == recipe) return e.getKey();
        }
        return null;
        *///?}
    }
}