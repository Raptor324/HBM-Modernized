package com.hbm_m.datagen.recipes.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.PrecAssRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * 1:1 {@code PrecAssRecipes.registerDefaults}: Bauplanmappen aus Papier, Farbstoff/Zinnober und Kugelfisch. Die
 * Rezepte des 528-Modus ({@code if(GeneralConfig.enable528)}) stehen in {@link PrecAss528RecipeGenerator}.
 */
public final class PrecAssRecipeGenerator {

    private PrecAssRecipeGenerator() { }

    public static void generate(Consumer<FinishedRecipe> writer) {
        int min = 1_200;

        // all hail the pufferfish, driver of all innovation
        new Builder(5 * min, 20_000L)
                .in(Ingredient.of(Items.PAPER), 16)
                .in(Ingredient.of(net.minecraftforge.common.Tags.Items.DYES_BLUE), 16)
                .in(Ingredient.of(Items.PUFFERFISH), 4)
                .out(1F, new ItemStack(ModItems.BLUEPRINT_FOLDER.get()), 10, new ItemStack(Items.PAPER, 16), 90)
                .save(writer, "precass/blueprints");

        new Builder(5 * min, 50_000L)
                .in(Ingredient.of(Items.PAPER), 24)
                .in(Ingredient.of(ModItems.CINNEBAR.get()), 24)
                .in(Ingredient.of(Items.PUFFERFISH), 8)
                .out(1F, new ItemStack(ModItems.BLUEPRINT_FOLDER_DISCOVER.get()), 5, new ItemStack(Items.PAPER, 24), 95)
                .save(writer, "precass/beigeprints");

        PrecAss528RecipeGenerator.generate(writer);
    }

    /** Minimaler Datagen-Builder fuer {@link PrecAssRecipe} (Format des {@link PrecAssRecipe.Serializer}). */
    public static final class Builder {

        private final int duration;
        private final long power;
        private final List<Object[]> inputs = new ArrayList<>();
        private final List<Object[]> outputs = new ArrayList<>();
        private final List<Object[]> fluidInputs = new ArrayList<>();
        private final List<Object[]> fluidOutputs = new ArrayList<>();
        @Nullable private String pool;
        @Nullable private ItemStack icon;

        public Builder(int duration, long power) {
            this.duration = duration;
            this.power = power;
        }

        public Builder in(Ingredient ing, int count) {
            inputs.add(new Object[] { ing, count });
            return this;
        }

        /** Eine Ausgabegruppe: abwechselnd Stack und Gewicht. */
        public Builder out(float chance, Object... stackWeight) {
            outputs.add(new Object[] { chance, stackWeight });
            return this;
        }

        public Builder pool(String pool) {
            this.pool = pool;
            return this;
        }

        /** Original {@code inputFluids(new FluidStack(type, fill))}. */
        public Builder fluidIn(net.minecraft.world.level.material.Fluid fluid, int amount) {
            fluidInputs.add(new Object[] { fluid, amount });
            return this;
        }

        public Builder fluidOut(net.minecraft.world.level.material.Fluid fluid, int amount) {
            fluidOutputs.add(new Object[] { fluid, amount });
            return this;
        }

        /** Original {@code setIcon(...)}. */
        public Builder icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        private static JsonObject stackJson(ItemStack st) {
            JsonObject item = new JsonObject();
            item.addProperty("item", BuiltInRegistries.ITEM.getKey(st.getItem()).toString());
            if (st.getCount() > 1) item.addProperty("count", st.getCount());
            if (st.getTag() != null) item.addProperty("nbt", st.getTag().toString());
            return item;
        }

        private static JsonArray fluidsJson(List<Object[]> list) {
            JsonArray arr = new JsonArray();
            for (Object[] o : list) {
                JsonObject f = new JsonObject();
                f.addProperty("fluid", BuiltInRegistries.FLUID.getKey((net.minecraft.world.level.material.Fluid) o[0]).toString());
                f.addProperty("amount", (int) o[1]);
                arr.add(f);
            }
            return arr;
        }

        public void save(Consumer<FinishedRecipe> writer, String path) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", path);
            writer.accept(new FinishedRecipe() {
                @Override
                public void serializeRecipeData(@NotNull JsonObject json) {
                    json.addProperty("duration", duration);
                    json.addProperty("power", power);
                    if (pool != null) json.addProperty("blueprint_pool", pool);
                    if (icon != null) json.add("icon_item", stackJson(icon));
                    if (!fluidInputs.isEmpty()) json.add("fluid_inputs", fluidsJson(fluidInputs));
                    if (!fluidOutputs.isEmpty()) json.add("fluid_outputs", fluidsJson(fluidOutputs));

                    JsonArray in = new JsonArray();
                    for (Object[] o : inputs) {
                        JsonObject e = new JsonObject();
                        e.addProperty("count", (int) o[1]);
                        e.add("items", ((Ingredient) o[0]).toJson());
                        in.add(e);
                    }
                    json.add("item_inputs", in);

                    JsonArray out = new JsonArray();
                    for (Object[] o : outputs) {
                        JsonObject g = new JsonObject();
                        g.addProperty("chance", (float) o[0]);
                        JsonArray entries = new JsonArray();
                        Object[] sw = (Object[]) o[1];
                        for (int i = 0; i < sw.length; i += 2) {
                            ItemStack st = (ItemStack) sw[i];
                            JsonObject entry = new JsonObject();
                            entry.add("item", stackJson(st));
                            entry.addProperty("weight", (int) sw[i + 1]);
                            entries.add(entry);
                        }
                        g.add("entries", entries);
                        out.add(g);
                    }
                    json.add("item_outputs", out);
                }

                @Override public @NotNull ResourceLocation getId() { return id; }
                @Override public @NotNull RecipeSerializer<?> getType() { return PrecAssRecipe.Serializer.INSTANCE; }
                @Nullable @Override public JsonObject serializeAdvancement() { return null; }
                @Nullable @Override public ResourceLocation getAdvancementId() { return null; }
            });
        }
    }
}
