package com.hbm_m.recipe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * W-R9B5-1: Mischer-Rezept aus dem Datapack ({@code hbm_m:mixer}). HBM selbst liefert keine solchen JSONs - die
 * eigenen Rezepte bleiben Code ({@link MixerRecipes#registerDefaults}). {@link MixerRecipes#getRecipes(net.minecraft.world.item.crafting.RecipeManager)}
 * haengt die Datapack-Rezepte an die Code-Rezepte desselben Ausgabefluids an (Reihenfolge {@code order}, dann Rezept-ID).
 *
 * <pre>{@code
 * {
 *   "type": "hbm_m:mixer",
 *   "output":   { "fluid": "space_m:smilk", "amount": 500 },
 *   "duration": 50,
 *   "input1":   { "fluid": "hbm_m:milk", "amount": 500 },      // optional (Original setStack1)
 *   "input2":   { "fluid": "...", "amount": 250 },             // optional (setStack2)
 *   "solid":    { "ingredient": { "tag": "..." }, "count": 4 }, // optional (setSolid)
 *   "order":    0,        // optional: Sortierung innerhalb des Ausgabefluids (kleiner zuerst)
 *   "replace":  false     // optional: true = Code-Rezepte dieses Ausgabefluids ersetzen (Original register() ueberschreibt)
 * }
 * }</pre>
 */
public class MixerDataRecipe extends PlatformRecipe {

    public record FluidAmount(ResourceLocation fluid, int amount) {
        @Nullable
        Fluid resolve() {
            var stack = RecipeHooks.fluidStackOf(fluid, Math.max(1, amount));
            return stack.isEmpty() ? null : stack.getFluid();
        }
    }

    private final FluidAmount output;
    private final int duration;
    @Nullable private final FluidAmount input1;
    @Nullable private final FluidAmount input2;
    @Nullable private final Ingredient solid;
    private final int solidCount;
    private final int order;
    private final boolean replace;

    public MixerDataRecipe(ResourceLocation id, FluidAmount output, int duration, @Nullable FluidAmount input1, @Nullable FluidAmount input2,
                           @Nullable Ingredient solid, int solidCount, int order, boolean replace) {
        super(id);
        this.output = output;
        this.duration = duration;
        this.input1 = input1;
        this.input2 = input2;
        this.solid = solid;
        this.solidCount = solidCount;
        this.order = order;
        this.replace = replace;
    }

    public FluidAmount getOutput() { return output; }
    public int getOrder() { return order; }
    public boolean isReplace() { return replace; }

    /** Ausgabefluid oder null, wenn es (z.B. ohne das liefernde Addon) nicht registriert ist. */
    @Nullable
    public Fluid outputFluid() { return output.resolve(); }

    /** Als Code-Rezept des Mischers; null, wenn ein Fluid fehlt (dann wird das Rezept uebergangen). */
    @Nullable
    public MixerRecipes.MixerRecipe toMixerRecipe() {
        MixerRecipes.MixerRecipe r = new MixerRecipes.MixerRecipe(output.amount(), duration);
        if (input1 != null) {
            Fluid f = input1.resolve();
            if (f == null) return null;
            r.setStack1(new MixerRecipes.FluidIn(f, input1.amount()));
        }
        if (input2 != null) {
            Fluid f = input2.resolve();
            if (f == null) return null;
            r.setStack2(new MixerRecipes.FluidIn(f, input2.amount()));
        }
        if (solid != null) {
            Ingredient ing = solid;
            r.setSolid(new MixerRecipes.SolidIn(ing, ing::getItems, solidCount));
        }
        return r;
    }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper input, @NotNull Level level) {
        return false; // Auswahl ueber MixerRecipes (Ausgabefluid + Rezeptindex), nicht ueber den RecipeManager
    }

    @Override
    public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    @Override
    public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }

    /** Kein Rezeptbuch-Eintrag (nur Maschinentabelle). */
    @Override
    public boolean isSpecial() { return true; }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<MixerDataRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mixer";

        @Override
        public String toString() { return RefStrings.MODID + ":" + ID; }
    }

    public static class Serializer extends PlatformRecipeSerializer<MixerDataRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public MixerDataRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidAmount output = fluid(GsonHelper.getAsJsonObject(json, "output"));
            int duration = GsonHelper.getAsInt(json, "duration", 20);
            FluidAmount in1 = json.has("input1") ? fluid(GsonHelper.getAsJsonObject(json, "input1")) : null;
            FluidAmount in2 = json.has("input2") ? fluid(GsonHelper.getAsJsonObject(json, "input2")) : null;
            Ingredient solid = null;
            int count = 1;
            if (json.has("solid")) {
                JsonObject s = GsonHelper.getAsJsonObject(json, "solid");
                solid = RecipeHooks.ingredientFromJson(s.get("ingredient"));
                count = GsonHelper.getAsInt(s, "count", 1);
            }
            return new MixerDataRecipe(recipeId, output, duration, in1, in2, solid, count,
                    GsonHelper.getAsInt(json, "order", 0), GsonHelper.getAsBoolean(json, "replace", false));
        }

        @Override
        public MixerDataRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidAmount output = readFluid(buf);
            int duration = buf.readVarInt();
            FluidAmount in1 = buf.readBoolean() ? readFluid(buf) : null;
            FluidAmount in2 = buf.readBoolean() ? readFluid(buf) : null;
            Ingredient solid = buf.readBoolean() ? RecipeHooks.readIngredient(buf) : null;
            int count = buf.readVarInt();
            int order = buf.readVarInt();
            boolean replace = buf.readBoolean();
            return new MixerDataRecipe(recipeId, output, duration, in1, in2, solid, count, order, replace);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, MixerDataRecipe r) {
            writeFluid(buf, r.output);
            buf.writeVarInt(r.duration);
            buf.writeBoolean(r.input1 != null);
            if (r.input1 != null) writeFluid(buf, r.input1);
            buf.writeBoolean(r.input2 != null);
            if (r.input2 != null) writeFluid(buf, r.input2);
            buf.writeBoolean(r.solid != null);
            if (r.solid != null) RecipeHooks.writeIngredient(buf, r.solid);
            buf.writeVarInt(r.solidCount);
            buf.writeVarInt(r.order);
            buf.writeBoolean(r.replace);
        }

        static FluidAmount fluid(JsonObject obj) {
            return new FluidAmount(ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid")), GsonHelper.getAsInt(obj, "amount", 0));
        }

        static FluidAmount readFluid(FriendlyByteBuf buf) {
            return new FluidAmount(buf.readResourceLocation(), buf.readVarInt());
        }

        static void writeFluid(FriendlyByteBuf buf, FluidAmount f) {
            buf.writeResourceLocation(f.fluid());
            buf.writeVarInt(f.amount());
        }
    }
}
