package com.hbm_m.inventory.recipes;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.config.ConfigPaths;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.recipes.LegacyStacks.ChanceStack;
import com.hbm_m.inventory.recipes.LegacyStacks.Input;
import com.hbm_m.inventory.recipes.LegacyStacks.LegacyFluid;
import com.hbm_m.item.ModItems;
import com.mojang.logging.LogUtils;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1:1 {@code CustomMachineRecipes} (SerializableRecipe "hbmCustomMachines.json"): Rezeptsaetze je {@code recipeKey}.
 * Liegt {@code config/hbm_m/hbmRecipes/hbmCustomMachines.json} vor, ersetzt sie die Vorgaben; sonst werden die Vorgaben
 * (Papierpresse) registriert und als Vorlage {@code _hbmCustomMachines.json} geschrieben - wie im Original.
 */
public final class CustomMachineRecipes {

    private CustomMachineRecipes() { }

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public static final HashMap<String, List<CustomMachineRecipe>> recipes = new HashMap<>();

    public static class CustomMachineRecipe {
        public LegacyFluid[] inputFluids;
        public Input[] inputItems;
        public LegacyFluid[] outputFluids;
        public ChanceStack[] outputItems;

        public int duration;
        public int consumptionPerTick;

        public String pollutionType;
        public float pollutionAmount;
        public float radiationAmount;
        public int flux;
        public int heat;
    }

    public static void registerDefaults() {
        List<CustomMachineRecipe> list = new ArrayList<>();
        CustomMachineRecipe recipe = new CustomMachineRecipe();
        recipe.inputFluids = new LegacyFluid[] { new LegacyFluid(ModFluids.WATER.getSource(), 250, 0) };
        recipe.inputItems = new Input[] { new Input(Ingredient.of(ModItems.POWDER_SAWDUST.get()), 1, "") };
        recipe.outputFluids = new LegacyFluid[0];
        recipe.outputItems = new ChanceStack[] { new ChanceStack(new ItemStack(Items.PAPER, 3), 1F) };
        recipe.duration = 60;
        recipe.consumptionPerTick = 10;
        recipe.pollutionType = "SOOT";
        recipe.pollutionAmount = 0.03F;
        recipe.radiationAmount = 0;
        recipe.flux = 0;
        recipe.heat = 0;
        list.add(recipe);
        recipes.put("paperPress", list);
    }

    public static void initialize() {
        recipes.clear();
        File dir = ConfigPaths.configRoot().resolve("hbmRecipes").toFile();
        if (!dir.exists()) dir.mkdirs();
        File recFile = new File(dir, "hbmCustomMachines.json");

        if (recFile.isFile()) {
            LOGGER.info("[hbm_m] Reading recipe file {}", recFile.getName());
            try (FileReader reader = new FileReader(recFile)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                for (JsonElement el : json.get("recipes").getAsJsonArray()) if (el != null) readRecipe(el);
            } catch (Exception ex) {
                LOGGER.error("[hbm_m] Failed to read {}", recFile, ex);
            }
        } else {
            LOGGER.info("[hbm_m] No recipe file found, registering defaults for hbmCustomMachines.json");
            registerDefaults();
            writeTemplate(new File(dir, "_hbmCustomMachines.json"));
        }
    }

    public static void readRecipe(JsonElement recipe) {
        JsonObject obj = recipe.getAsJsonObject();
        String name = obj.get("recipeKey").getAsString();
        List<CustomMachineRecipe> list = new ArrayList<>();
        JsonArray array = obj.get("recipes").getAsJsonArray();

        for (int i = 0; i < array.size(); i++) {
            JsonObject rec = array.get(i).getAsJsonObject();
            CustomMachineRecipe r = new CustomMachineRecipe();
            r.inputFluids = fluids(rec.get("inputFluids").getAsJsonArray());
            JsonArray items = rec.get("inputItems").getAsJsonArray();
            r.inputItems = new Input[items.size()];
            for (int j = 0; j < items.size(); j++) r.inputItems[j] = LegacyStacks.readAStack(items.get(j).getAsJsonArray());
            r.outputFluids = fluids(rec.get("outputFluids").getAsJsonArray());
            JsonArray outs = rec.get("outputItems").getAsJsonArray();
            r.outputItems = new ChanceStack[outs.size()];
            for (int j = 0; j < outs.size(); j++) r.outputItems[j] = LegacyStacks.readItemStackChance(outs.get(j).getAsJsonArray());
            r.duration = rec.get("duration").getAsInt();
            r.consumptionPerTick = rec.get("consumptionPerTick").getAsInt();

            if (rec.has("pollutionType") && rec.has("pollutionAmount")) {
                r.pollutionType = rec.get("pollutionType").getAsString();
                r.pollutionAmount = rec.get("pollutionAmount").getAsFloat();
            } else {
                r.pollutionType = "";
            }
            if (rec.has("radiationAmount")) r.radiationAmount = rec.get("radiationAmount").getAsFloat();
            if (rec.has("flux")) r.flux = rec.get("flux").getAsInt();
            if (rec.has("heat")) r.heat = rec.get("heat").getAsInt();
            list.add(r);
        }
        recipes.put(name, list);
    }

    private static LegacyFluid[] fluids(JsonArray array) {
        LegacyFluid[] out = new LegacyFluid[array.size()];
        for (int i = 0; i < array.size(); i++) out[i] = LegacyStacks.readFluidStack(array.get(i).getAsJsonArray());
        return out;
    }

    private static void writeTemplate(File template) {
        try (JsonWriter writer = new JsonWriter(new FileWriter(template))) {
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("recipes").beginArray();
            for (Entry<String, List<CustomMachineRecipe>> entry : recipes.entrySet()) {
                writer.beginObject();
                writer.name("recipeKey").value(entry.getKey());
                writer.name("recipes").beginArray();
                for (CustomMachineRecipe r : entry.getValue()) {
                    writer.beginObject();
                    writer.name("inputFluids").beginArray();
                    for (LegacyFluid f : r.inputFluids) writeFluid(f, writer);
                    writer.endArray();
                    writer.name("inputItems").beginArray();
                    for (Input in : r.inputItems) {
                        writer.beginArray().setIndent("");
                        ItemStack[] stacks = in.ingredient().getItems();
                        writer.value("item").value(stacks.length > 0 ? LegacyStacks.name(stacks[0].getItem()) : "minecraft:air");
                        if (in.stacksize() != 1) writer.value(in.stacksize());
                        writer.endArray().setIndent("  ");
                    }
                    writer.endArray();
                    writer.name("outputFluids").beginArray();
                    for (LegacyFluid f : r.outputFluids) writeFluid(f, writer);
                    writer.endArray();
                    writer.name("outputItems").beginArray();
                    for (ChanceStack c : r.outputItems) {
                        writer.beginArray().setIndent("");
                        writer.value(LegacyStacks.name(c.stack().getItem()));
                        if (c.stack().getCount() != 1) writer.value(c.stack().getCount());
                        writer.value(c.chance());
                        writer.endArray().setIndent("  ");
                    }
                    writer.endArray();
                    writer.name("duration").value(r.duration);
                    writer.name("consumptionPerTick").value(r.consumptionPerTick);
                    writer.name("pollutionType").value(r.pollutionType);
                    writer.name("pollutionAmount").value(r.pollutionAmount);
                    writer.name("radiationAmount").value(r.radiationAmount);
                    writer.name("flux").value(r.flux);
                    writer.name("heat").value(r.heat);
                    writer.endObject();
                }
                writer.endArray();
                writer.endObject();
            }
            writer.endArray();
            writer.endObject();
        } catch (IOException ex) {
            LOGGER.error("[hbm_m] Failed to write {}", template, ex);
        }
    }

    private static void writeFluid(LegacyFluid f, JsonWriter writer) throws IOException {
        writer.beginArray().setIndent("");
        writer.value(LegacyStacks.fluidName(f.type())).value(f.fill());
        if (f.pressure() != 0) writer.value(f.pressure());
        writer.endArray().setIndent("  ");
    }
}
