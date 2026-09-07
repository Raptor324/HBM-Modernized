package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 1:1-Port von {@code com.hbm.inventory.recipes.PlasmaForgeRecipe} (1.7.10) als datapack-faehiger
 * Rezepttyp {@code hbm_m:plasma_forge}.
 *
 * <p>Die Plasmaschmiede laeuft nur, wenn der Torus ihr mindestens {@code ignition_temp} TU/t
 * Plasmaleistung liefert; ansonsten verhaelt sie sich wie eine gewoehnliche Werkbankmaschine mit
 * bis zu zwoelf Item-Eingaben und einer Fluid-Eingabe.</p>
 */
public class PlasmaForgeRecipe extends PlatformRecipe {

    public record CountedIngredient(Ingredient ingredient, int count) {}

    private final List<CountedIngredient> itemInputs;
    private final List<FluidStack> fluidInputs;
    private final ItemStack output;
    private final int duration;
    private final long power;
    private final long ignitionTemp;
    @Nullable
    private final String blueprintPool;

    public PlasmaForgeRecipe(ResourceLocation id,
                             List<CountedIngredient> itemInputs,
                             List<FluidStack> fluidInputs,
                             ItemStack output,
                             int duration,
                             long power,
                             long ignitionTemp,
                             @Nullable String blueprintPool) {
        super(id);
        this.itemInputs = itemInputs != null ? itemInputs : List.of();
        this.fluidInputs = fluidInputs != null ? fluidInputs : List.of();
        this.output = output != null ? output : ItemStack.EMPTY;
        this.duration = duration;
        this.power = power;
        this.ignitionTemp = ignitionTemp;
        this.blueprintPool = blueprintPool;
    }

    public List<CountedIngredient> getItemInputs() { return itemInputs; }
    public List<FluidStack> getFluidInputs() { return fluidInputs; }
    public ItemStack getOutput() { return output.copy(); }
    public int getDuration() { return duration; }
    public long getPower() { return power; }
    /** Original: {@code ignitionTemp} - minimale Plasmaleistung des Torus. */
    public long getIgnitionTemp() { return ignitionTemp; }

    @Nullable
    public String getBlueprintPool() { return blueprintPool; }

    public boolean requiresBlueprint() {
        return blueprintPool != null && !blueprintPool.isEmpty();
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        return false; // Auswahl per ID, kein Shaped-Matching.
    }

    @Override
    public ItemStack assembleSafe() {
        return output.copy();
    }

    @Override
    public ItemStack getResultItemSafe() {
        return output.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> expanded = NonNullList.create();
        for (CountedIngredient ci : itemInputs) {
            int count = Math.max(1, ci.count());
            for (int i = 0; i < count; i++) expanded.add(ci.ingredient());
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

    public static final class Type implements RecipeType<PlasmaForgeRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "plasma_forge";
    }

    public static final class Serializer extends PlatformRecipeSerializer<PlasmaForgeRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "plasma_forge");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plasma_forge");
        //?}

        @Override
        public PlasmaForgeRecipe readJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            long power = GsonHelper.getAsLong(json, "power", 0L);
            long ignition = GsonHelper.getAsLong(json, "ignition_temp", 0L);
            String pool = GsonHelper.getAsString(json, "blueprint_pool", null);

            List<CountedIngredient> itemInputs = new ArrayList<>();
            if (json.has("item_inputs")) {
                JsonArray arr = GsonHelper.getAsJsonArray(json, "item_inputs");
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
                    itemInputs.add(new CountedIngredient(ing, count));
                }
            }

            List<FluidStack> fluidInputs = new ArrayList<>();
            if (json.has("fluid_inputs")) {
                JsonArray arr = GsonHelper.getAsJsonArray(json, "fluid_inputs");
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    ResourceLocation fid = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid"));
                    if (fid == null) continue;
                    fluidInputs.add(RecipeHooks.fluidStackOf(fid, GsonHelper.getAsLong(obj, "amount", 0L)));
                }
            }

            ItemStack output = json.has("result")
                    ? RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"))
                    : ItemStack.EMPTY;

            return new PlasmaForgeRecipe(recipeId, itemInputs, fluidInputs, output, duration, power, ignition, pool);
        }

        @Override
        public PlasmaForgeRecipe readNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
            int duration = buf.readVarInt();
            long power = buf.readLong();
            long ignition = buf.readLong();
            String pool = buf.readBoolean() ? buf.readUtf() : null;

            int itemInCount = buf.readVarInt();
            List<CountedIngredient> itemInputs = new ArrayList<>(itemInCount);
            for (int i = 0; i < itemInCount; i++) {
                Ingredient ing = RecipeHooks.readIngredient(buf);
                int count = buf.readVarInt();
                itemInputs.add(new CountedIngredient(ing, count));
            }

            int fluidInCount = buf.readVarInt();
            List<FluidStack> fluidInputs = new ArrayList<>(fluidInCount);
            for (int i = 0; i < fluidInCount; i++) fluidInputs.add(RecipeHooks.readFluidStack(buf));

            ItemStack output = RecipeHooks.readItem(buf);

            return new PlasmaForgeRecipe(recipeId, itemInputs, fluidInputs, output, duration, power, ignition, pool);
        }

        @Override
        public void writeNetwork(@NotNull FriendlyByteBuf buf, @NotNull PlasmaForgeRecipe recipe) {
            buf.writeVarInt(recipe.duration);
            buf.writeLong(recipe.power);
            buf.writeLong(recipe.ignitionTemp);

            if (recipe.blueprintPool != null) {
                buf.writeBoolean(true);
                buf.writeUtf(recipe.blueprintPool);
            } else {
                buf.writeBoolean(false);
            }

            buf.writeVarInt(recipe.itemInputs.size());
            for (CountedIngredient ci : recipe.itemInputs) {
                RecipeHooks.writeIngredient(buf, ci.ingredient());
                buf.writeVarInt(ci.count());
            }

            buf.writeVarInt(recipe.fluidInputs.size());
            for (FluidStack fs : recipe.fluidInputs) RecipeHooks.writeFluidStack(buf, fs);

            RecipeHooks.writeItem(buf, recipe.output);
        }
    }
}
