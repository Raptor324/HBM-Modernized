package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.condition.ConfigRecipeCondition;
import com.hbm_m.recipe.condition.ConfigRecipeFlags;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-Hilfen fuer Rezepte, die im Original von Konfig-Schaltern abhaengen (LBSM, 528, Expensive):
 * <ul>
 *   <li>{@link #when}: Rezept existiert nur bei passenden Schaltern (Forge {@code "conditions"} mit
 *       {@link ConfigRecipeCondition}),</li>
 *   <li>{@link #variants}: gleiche Rezept-ID, andere Fassung je Schalter ({@value ConfigRecipeFlags#VARIANTS_KEY},
 *       aufgeloest in {@code PlatformRecipeSerializer} - nur fuer Maschinenrezepte).</li>
 * </ul>
 * Schalternamen siehe {@link ConfigRecipeFlags}.
 */
public final class ConfigRecipes {

    private ConfigRecipes() {}

    /** Haengt die Bedingungen (alle muessen gelten) an jedes Rezept, das ueber den Rueckgabe-Writer gespeichert wird. */
    public static Consumer<FinishedRecipe> when(Consumer<FinishedRecipe> writer, String... flags) {
        if (flags.length == 0) return writer;
        return r -> writer.accept(new Wrapped(r, flags, null));
    }

    /** Wie {@link #when}, aber nur fuer die genannten Rezept-IDs (Port-eigene Rezepte, die im Original bedingt sind). */
    public static Consumer<FinishedRecipe> byId(Consumer<FinishedRecipe> writer, Map<String, String[]> idFlags) {
        return r -> {
            String[] flags = idFlags.get(r.getId().toString());
            writer.accept(flags == null ? r : new Wrapped(r, flags, null));
        };
    }

    public static Variants variants(Consumer<FinishedRecipe> writer) {
        return new Variants(writer);
    }

    private static JsonArray conditions(String[] flags) {
        JsonArray arr = new JsonArray();
        for (String f : flags) {
            ConfigRecipeFlags.test(f); // Tippfehler im Datagen abfangen
            arr.add(ConfigRecipeCondition.Serializer.INSTANCE.getJson(new ConfigRecipeCondition(f)));
        }
        return arr;
    }

    /** Sammelt Grundfassung und Konfig-Fassungen eines Rezepts (je ein {@code save} auf den uebergebenen Writer). */
    public static final class Variants {
        private final Consumer<FinishedRecipe> writer;
        private FinishedRecipe base;
        private final List<String[]> flags = new ArrayList<>();
        private final List<FinishedRecipe> recipes = new ArrayList<>();
        private String[] conditions = new String[0];

        private Variants(Consumer<FinishedRecipe> writer) {
            this.writer = writer;
        }

        public Variants base(Consumer<Consumer<FinishedRecipe>> save) {
            this.base = capture(save);
            return this;
        }

        /** Fassung, die gilt, wenn alle Schalter an sind; die erste passende gewinnt. */
        public Variants variant(Consumer<Consumer<FinishedRecipe>> save, String... flags) {
            this.flags.add(flags);
            this.recipes.add(capture(save));
            return this;
        }

        /** Zusaetzliche Existenzbedingung fuer das ganze Rezept. */
        public Variants when(String... flags) {
            this.conditions = flags;
            return this;
        }

        public void save() {
            if (base == null) throw new IllegalStateException("Grundfassung fehlt");
            for (FinishedRecipe r : recipes) {
                if (!r.getId().equals(base.getId()) || r.getType() != base.getType())
                    throw new IllegalStateException("Fassung mit anderer ID/Typ: " + r.getId());
            }
            JsonArray arr = new JsonArray();
            for (int i = 0; i < recipes.size(); i++) {
                JsonObject v = new JsonObject();
                JsonArray f = new JsonArray();
                for (String s : flags.get(i)) {
                    ConfigRecipeFlags.test(s);
                    f.add(s);
                }
                v.add("flags", f);
                v.add("recipe", recipes.get(i).serializeRecipe());
                arr.add(v);
            }
            writer.accept(new Wrapped(base, conditions, arr.isEmpty() ? null : arr));
        }

        private static FinishedRecipe capture(Consumer<Consumer<FinishedRecipe>> save) {
            List<FinishedRecipe> out = new ArrayList<>();
            save.accept(out::add);
            if (out.size() != 1) throw new IllegalStateException("Genau ein Rezept erwartet, bekommen: " + out.size());
            return out.get(0);
        }
    }

    private record Wrapped(FinishedRecipe inner, String[] flags, @Nullable JsonArray variants) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            inner.serializeRecipeData(json);
            if (flags.length > 0) json.add("conditions", conditions(flags));
            if (variants != null) json.add(ConfigRecipeFlags.VARIANTS_KEY, variants);
        }

        @Override
        public ResourceLocation getId() {
            return inner.getId();
        }

        @Override
        public RecipeSerializer<?> getType() {
            return inner.getType();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() {
            return inner.serializeAdvancement();
        }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() {
            return inner.getAdvancementId();
        }
    }
}
//?}
