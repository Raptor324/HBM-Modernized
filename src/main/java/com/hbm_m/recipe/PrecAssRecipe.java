package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Rezept des Praezisionsassemblers ({@code PrecAssRecipes}, GenericRecipe): bis zu 9 Item-Eingaenge (positionsgebunden),
 * ein Fluessigkeitseingang, bis zu 9 Ausgaben. Jede Ausgabe ist eine {@link OutputGroup}: entweder eine gewichtete
 * Auswahl ({@code ChanceOutputMulti}) oder ein einzelner Gegenstand mit Wahrscheinlichkeit ({@code ChanceOutput}).
 */
public class PrecAssRecipe extends PlatformRecipe {

    public record CountedIngredient(Ingredient ingredient, int count) {}
    public record FluidIngredient(ResourceLocation fluidId, int amount) {}
    public record WeightedStack(ItemStack stack, int weight) {}

    /** {@code IOutput}: {@code chance} gilt nur fuer Einzelausgaben, mehrere Eintraege werden nach Gewicht gewaehlt. */
    public record OutputGroup(List<WeightedStack> entries, float chance) {

        public boolean possibleMultiOutput() {
            return entries.size() > 1;
        }

        @Nullable
        public ItemStack getSingle() {
            return entries.size() == 1 ? entries.get(0).stack() : null;
        }

        /** {@code collapse()}: das tatsaechlich erzeugte Ergebnis oder leer. */
        public ItemStack collapse(RandomSource rand) {
            if (entries.isEmpty()) return ItemStack.EMPTY;
            if (entries.size() == 1) {
                return rand.nextFloat() < chance ? entries.get(0).stack().copy() : ItemStack.EMPTY;
            }
            int total = 0;
            for (WeightedStack w : entries) total += w.weight();
            if (total <= 0) return ItemStack.EMPTY;
            int roll = rand.nextInt(total);
            for (WeightedStack w : entries) {
                roll -= w.weight();
                if (roll < 0) return w.stack().copy();
            }
            return ItemStack.EMPTY;
        }
    }

    private final List<CountedIngredient> itemInputs;
    private final List<FluidIngredient> fluidInputs;
    private final List<OutputGroup> itemOutputs;
    private final List<FluidIngredient> fluidOutputs;
    private final int duration;
    private final long power;
    @Nullable private final String blueprintPool;
    @Nullable private final ItemStack icon;

    public PrecAssRecipe(ResourceLocation id, List<CountedIngredient> itemInputs, List<FluidIngredient> fluidInputs,
                         List<OutputGroup> itemOutputs, List<FluidIngredient> fluidOutputs, int duration, long power,
                         @Nullable String blueprintPool, @Nullable ItemStack icon) {
        super(id);
        this.itemInputs = itemInputs;
        this.fluidInputs = fluidInputs;
        this.itemOutputs = itemOutputs;
        this.fluidOutputs = fluidOutputs;
        this.duration = duration;
        this.power = power;
        this.blueprintPool = blueprintPool;
        this.icon = icon != null && !icon.isEmpty() ? icon : null;
    }

    public List<CountedIngredient> getItemInputs() { return itemInputs; }
    public List<FluidIngredient> getFluidInputs() { return fluidInputs; }
    public List<OutputGroup> getItemOutputs() { return itemOutputs; }
    public List<FluidIngredient> getFluidOutputs() { return fluidOutputs; }
    public int getDuration() { return duration; }
    public long getPower() { return power; }
    @Nullable public String getBlueprintPool() { return blueprintPool; }
    public boolean isPooled() { return blueprintPool != null && !blueprintPool.isEmpty(); }

    @Override public boolean matchesRecipe(RecipeInputWrapper input, Level level) { return false; }
    @Override public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    /** {@code GenericRecipe.getIcon}: eigenes Symbol oder der erste moegliche Ausgang. */
    @Override
    public ItemStack getResultItemSafe() {
        if (icon != null) return icon.copy();
        for (OutputGroup g : itemOutputs) if (!g.entries().isEmpty()) return g.entries().get(0).stack().copy();
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (CountedIngredient ci : itemInputs) list.add(ci.ingredient());
        return list;
    }

    @Override public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }
    @Override public RecipeType<?> getType() { return Type.INSTANCE; }

    public static final class Type implements RecipeType<PrecAssRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "precass";
    }

    public static final class Serializer extends PlatformRecipeSerializer<PrecAssRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public PrecAssRecipe readJson(ResourceLocation id, JsonObject json) {
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            long power = GsonHelper.getAsLong(json, "power", 100);
            String pool = GsonHelper.getAsString(json, "blueprint_pool", null);
            ItemStack icon = json.has("icon_item") ? RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "icon_item")) : ItemStack.EMPTY;

            List<CountedIngredient> in = new ArrayList<>();
            if (json.has("item_inputs")) for (JsonElement el : GsonHelper.getAsJsonArray(json, "item_inputs")) {
                JsonObject o = el.getAsJsonObject();
                in.add(new CountedIngredient(RecipeHooks.ingredientFromJson(o.get("items")), GsonHelper.getAsInt(o, "count", 1)));
            }
            List<FluidIngredient> fin = readFluids(json, "fluid_inputs");
            List<FluidIngredient> fout = readFluids(json, "fluid_outputs");

            List<OutputGroup> out = new ArrayList<>();
            if (json.has("item_outputs")) for (JsonElement el : GsonHelper.getAsJsonArray(json, "item_outputs")) {
                JsonObject o = el.getAsJsonObject();
                List<WeightedStack> entries = new ArrayList<>();
                for (JsonElement e : GsonHelper.getAsJsonArray(o, "entries")) {
                    JsonObject eo = e.getAsJsonObject();
                    entries.add(new WeightedStack(RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(eo, "item")), GsonHelper.getAsInt(eo, "weight", 1)));
                }
                out.add(new OutputGroup(entries, GsonHelper.getAsFloat(o, "chance", 1F)));
            }
            return new PrecAssRecipe(id, in, fin, out, fout, duration, power, pool, icon);
        }

        private static List<FluidIngredient> readFluids(JsonObject json, String key) {
            List<FluidIngredient> list = new ArrayList<>();
            if (!json.has(key)) return list;
            JsonArray arr = GsonHelper.getAsJsonArray(json, key);
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                ResourceLocation f = ResourceLocation.tryParse(GsonHelper.getAsString(o, "fluid"));
                if (f != null) list.add(new FluidIngredient(f, GsonHelper.getAsInt(o, "amount", 0)));
            }
            return list;
        }

        @Override
        public PrecAssRecipe readNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int duration = buf.readVarInt();
            long power = buf.readVarLong();
            String pool = buf.readBoolean() ? buf.readUtf() : null;
            ItemStack icon = buf.readBoolean() ? RecipeHooks.readItem(buf) : ItemStack.EMPTY;

            int n = buf.readVarInt();
            List<CountedIngredient> in = new ArrayList<>(n);
            for (int i = 0; i < n; i++) in.add(new CountedIngredient(RecipeHooks.readIngredient(buf), buf.readVarInt()));
            List<FluidIngredient> fin = readFluids(buf);
            n = buf.readVarInt();
            List<OutputGroup> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                float chance = buf.readFloat();
                int m = buf.readVarInt();
                List<WeightedStack> entries = new ArrayList<>(m);
                for (int j = 0; j < m; j++) entries.add(new WeightedStack(RecipeHooks.readItem(buf), buf.readVarInt()));
                out.add(new OutputGroup(entries, chance));
            }
            List<FluidIngredient> fout = readFluids(buf);
            return new PrecAssRecipe(id, in, fin, out, fout, duration, power, pool, icon);
        }

        private static List<FluidIngredient> readFluids(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<FluidIngredient> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(new FluidIngredient(buf.readResourceLocation(), buf.readVarInt()));
            return list;
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, PrecAssRecipe r) {
            buf.writeVarInt(r.duration);
            buf.writeVarLong(r.power);
            buf.writeBoolean(r.blueprintPool != null);
            if (r.blueprintPool != null) buf.writeUtf(r.blueprintPool);
            buf.writeBoolean(r.icon != null);
            if (r.icon != null) RecipeHooks.writeItem(buf, r.icon);

            buf.writeVarInt(r.itemInputs.size());
            for (CountedIngredient ci : r.itemInputs) {
                RecipeHooks.writeIngredient(buf, ci.ingredient());
                buf.writeVarInt(ci.count());
            }
            writeFluids(buf, r.fluidInputs);
            buf.writeVarInt(r.itemOutputs.size());
            for (OutputGroup g : r.itemOutputs) {
                buf.writeFloat(g.chance());
                buf.writeVarInt(g.entries().size());
                for (WeightedStack w : g.entries()) {
                    RecipeHooks.writeItem(buf, w.stack());
                    buf.writeVarInt(w.weight());
                }
            }
            writeFluids(buf, r.fluidOutputs);
        }

        private static void writeFluids(FriendlyByteBuf buf, List<FluidIngredient> list) {
            buf.writeVarInt(list.size());
            for (FluidIngredient f : list) {
                buf.writeResourceLocation(f.fluidId());
                buf.writeVarInt(f.amount());
            }
        }
    }
}
