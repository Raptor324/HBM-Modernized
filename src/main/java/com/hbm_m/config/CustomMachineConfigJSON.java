package com.hbm_m.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.custom.CMBlocks;
import com.hbm_m.inventory.recipes.LegacyStacks;
import com.hbm_m.item.ModItems;
import com.mojang.logging.LogUtils;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 1:1 {@code CustomMachineConfigJSON}: {@code config/hbm_m/hbmCustomMachines.json} beschreibt beliebige Maschinen
 * (Tankanzahl/-groesse, Item-Ein-/Ausgaenge, Generatorbetrieb, Fluss-/Waermemodus, Leistung) samt Bauplan aus
 * Bauteilbloecken und Werkbankrezept der Steuerung. Fehlt die Datei, wird die Papierpresse als Vorgabe geschrieben.
 *
 * <p>Bauteile duerfen alte Namen tragen ({@code "hbm:tile.cm_sheet"} + {@code "metas"}); jede Meta eines
 * {@code cm_*}-Blocks ist im Port ein eigener Block. Die Steuerung ist ein {@code custom_machine}-Gegenstand mit
 * {@code machineType} im NBT statt der Original-Meta {@code 100 + Index}.</p>
 */
public final class CustomMachineConfigJSON {

    private CustomMachineConfigJSON() { }

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    public static final Gson gson = new Gson();
    public static final HashMap<String, MachineConfiguration> customMachines = new HashMap<>();
    public static final List<MachineConfiguration> niceList = new ArrayList<>();
    /** Werkbankrezepte der Steuerungen; werden beim Serverstart in den RecipeManager gelegt. */
    public static final List<ShapedRecipe> controllerRecipes = new ArrayList<>();

    public static void initialize() {
        customMachines.clear();
        niceList.clear();
        controllerRecipes.clear();

        File folder = ConfigPaths.configRoot().toFile();
        if (!folder.exists()) folder.mkdirs();
        File config = new File(folder, "hbmCustomMachines.json");

        if (!config.exists()) writeDefault(config);
        readConfig(config);
    }

    public static void writeDefault(File config) {
        try (JsonWriter writer = new JsonWriter(new FileWriter(config))) {
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("machines").beginArray();

            writer.beginObject();
            writer.name("recipeKey").value("paperPress");
            writer.name("unlocalizedName").value("paperPress");
            writer.name("localization").beginObject();
            writer.name("de_DE").value("Papierpresse");
            writer.endObject();
            writer.name("localizedName").value("Paper Press");
            writer.name("fluidInCount").value(1);
            writer.name("fluidInCap").value(1_000);
            writer.name("itemInCount").value(1);
            writer.name("fluidOutCount").value(0);
            writer.name("fluidOutCap").value(0);
            writer.name("itemOutCount").value(1);
            writer.name("generatorMode").value(false);
            writer.name("maxPollutionCap").value(100);
            writer.name("fluxMode").value(false);
            writer.name("recipeSpeedMult").value(1.0D);
            writer.name("recipeConsumptionMult").value(1.0D);
            writer.name("maxPower").value(10_000L);
            writer.name("maxHeat").value(0);

            writer.name("recipeShape").beginArray();
            writer.value("IPI").value("PCP").value("IPI");
            writer.endArray();

            writer.name("recipeParts").beginArray().setIndent("");
            writer.value("I");
            writer.beginArray().value("dict").value("ingotSteel").endArray();
            writer.value("P");
            writer.beginArray().value("dict").value("plateSteel").endArray();
            writer.value("C");
            writer.beginArray().value("item").value(LegacyStacks.name(ModItems.INTEGRATED_CIRCUIT.get())).endArray();
            writer.endArray().setIndent("  ");

            writer.name("components").beginArray();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = 0; z <= 2; z++) {
                        if (!(x == 0 && y == 0 && z == 1) && !(x == 0 && z == 0)) {
                            component(writer, y == 0 ? "hbm:tile.cm_sheet" : "hbm:tile.cm_block", x, y, z);
                        }
                    }
                }
            }
            component(writer, "hbm:tile.cm_port", 0, -1, 0);
            component(writer, "hbm:tile.cm_port", 0, 1, 0);

            writer.endArray();
            writer.endObject();

            writer.endArray();
            writer.endObject();
        } catch (IOException e) {
            LOGGER.error("[hbm_m] Could not write {}", config, e);
        }
    }

    private static void component(JsonWriter writer, String block, int x, int y, int z) throws IOException {
        writer.beginObject().setIndent("");
        writer.name("block").value(block);
        writer.name("x").value(x);
        writer.name("y").value(y);
        writer.name("z").value(z);
        writer.name("metas").beginArray();
        writer.value(0);
        writer.endArray();
        writer.endObject().setIndent("  ");
    }

    public static void readConfig(File config) {
        try {
            JsonObject json = gson.fromJson(new InputStreamReader(Files.newInputStream(config.toPath()), StandardCharsets.UTF_8), JsonObject.class);
            JsonArray machines = json.get("machines").getAsJsonArray();

            for (int i = 0; i < machines.size(); i++) {
                JsonObject machineObject = machines.get(i).getAsJsonObject();

                MachineConfiguration configuration = new MachineConfiguration();
                configuration.recipeKey = machineObject.get("recipeKey").getAsString();
                configuration.unlocalizedName = machineObject.get("unlocalizedName").getAsString();
                configuration.localizedName = machineObject.get("localizedName").getAsString();
                if (machineObject.has("localization")) {
                    JsonObject localization = machineObject.get("localization").getAsJsonObject();
                    for (Entry<String, JsonElement> entry : localization.entrySet()) {
                        configuration.localization.put(entry.getKey().toLowerCase(java.util.Locale.US), entry.getValue().getAsString());
                    }
                }
                configuration.fluidInCount = machineObject.get("fluidInCount").getAsInt();
                configuration.fluidInCap = machineObject.get("fluidInCap").getAsInt();
                configuration.itemInCount = machineObject.get("itemInCount").getAsInt();
                configuration.fluidOutCount = machineObject.get("fluidOutCount").getAsInt();
                configuration.fluidOutCap = machineObject.get("fluidOutCap").getAsInt();
                configuration.itemOutCount = machineObject.get("itemOutCount").getAsInt();
                configuration.generatorMode = machineObject.get("generatorMode").getAsBoolean();
                if (machineObject.has("maxPollutionCap")) configuration.maxPollutionCap = machineObject.get("maxPollutionCap").getAsInt();
                if (machineObject.has("fluxMode")) configuration.fluxMode = machineObject.get("fluxMode").getAsBoolean();
                configuration.recipeSpeedMult = machineObject.get("recipeSpeedMult").getAsDouble();
                configuration.recipeConsumptionMult = machineObject.get("recipeConsumptionMult").getAsDouble();
                configuration.maxPower = machineObject.get("maxPower").getAsLong();
                if (machineObject.has("maxHeat")) configuration.maxHeat = machineObject.get("maxHeat").getAsInt();

                if (machineObject.has("recipeShape") && machineObject.has("recipeParts")) {
                    try {
                        controllerRecipes.add(buildRecipe(i, configuration, machineObject.get("recipeShape").getAsJsonArray(), machineObject.get("recipeParts").getAsJsonArray()));
                    } catch (Exception ex) {
                        LOGGER.error("[hbm_m] Caught exception trying to parse core recipe for custom machine {}", configuration.unlocalizedName);
                        LOGGER.error("[hbm_m] recipeShape was{}", machineObject.get("recipeShape"));
                        LOGGER.error("[hbm_m] recipeParts was{}", machineObject.get("recipeParts"));
                    }
                }

                JsonArray components = machineObject.get("components").getAsJsonArray();
                configuration.components = new ArrayList<>();

                for (int j = 0; j < components.size(); j++) {
                    JsonObject compObject = components.get(j).getAsJsonObject();
                    MachineConfiguration.ComponentDefinition compDef = new MachineConfiguration.ComponentDefinition();
                    compDef.x = compObject.get("x").getAsInt();
                    compDef.y = compObject.get("y").getAsInt();
                    compDef.z = compObject.get("z").getAsInt();
                    JsonArray metas = compObject.get("metas").getAsJsonArray();
                    compDef.blocks = resolveBlocks(compObject.get("block").getAsString(), metas);
                    configuration.components.add(compDef);
                }

                customMachines.put(configuration.unlocalizedName, configuration);
                niceList.add(configuration);
            }

        } catch (Exception ex) {
            LOGGER.error("[hbm_m] Failed to read {}", config, ex);
        }
    }

    /** Werkbankrezept der Steuerung ({@code CraftingManager.addRecipeAuto(stack, parts)}). */
    private static ShapedRecipe buildRecipe(int index, MachineConfiguration configuration, JsonArray shape, JsonArray parts) {
        int h = shape.size();
        int w = 0;
        for (JsonElement row : shape) w = Math.max(w, row.getAsString().length());
        HashMap<Character, Ingredient> keys = new HashMap<>();
        for (int j = 0; j + 1 < parts.size(); j += 2) {
            char c = parts.get(j).getAsString().charAt(0); //god is dead and we killed him
            keys.put(c, LegacyStacks.readAStack(parts.get(j + 1).getAsJsonArray()).ingredient());
        }
        NonNullList<Ingredient> grid = NonNullList.withSize(w * h, Ingredient.EMPTY);
        for (int r = 0; r < h; r++) {
            String row = shape.get(r).getAsString();
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                if (ch == ' ') continue;
                Ingredient ing = keys.get(ch);
                if (ing == null) throw new IllegalArgumentException("Undefined symbol " + ch);
                grid.set(r * w + c, ing);
            }
        }
        ItemStack stack = ModItems.CUSTOM_MACHINE.get().getDefaultInstance();
        stack.getOrCreateTag().putString("machineType", configuration.unlocalizedName);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", "custom_machine/" + index + "_" + configuration.unlocalizedName.toLowerCase(java.util.Locale.US).replaceAll("[^a-z0-9_.-]", "_"));
        //? if < 1.21.1 {
        return new ShapedRecipe(id, "", CraftingBookCategory.MISC, w, h, grid, stack);
        //?} else {
        /*return new ShapedRecipe("", CraftingBookCategory.MISC, new net.minecraft.world.item.crafting.ShapedRecipePattern(w, h, grid, java.util.Optional.empty()), stack);
        *///?}
    }

    /** Alter Name + Metas -> erlaubte Port-Bloecke (Reihenfolge = Metas, die erste wird von {@code buildStructure} gesetzt). */
    private static List<Block> resolveBlocks(String name, JsonArray metas) {
        List<Block> out = new ArrayList<>();
        String n = name.replace("hbm:tile.", "").replace("hbm:", "");
        for (int k = 0; k < metas.size(); k++) {
            int meta = metas.get(k).getAsInt();
            Block b = switch (n) {
                case "cm_block" -> enumBlock(ModBlocks.CM_BLOCK, CMBlocks.CMMaterial.values(), meta);
                case "cm_sheet" -> enumBlock(ModBlocks.CM_SHEET, CMBlocks.CMMaterial.values(), meta);
                case "cm_tank" -> enumBlock(ModBlocks.CM_TANK, CMBlocks.CMMaterial.values(), meta);
                case "cm_port" -> enumBlock(ModBlocks.CM_PORT, CMBlocks.CMMaterial.values(), meta);
                case "cm_engine" -> enumBlock(ModBlocks.CM_ENGINE, CMBlocks.CMEngine.values(), meta);
                case "cm_circuit" -> enumBlock(ModBlocks.CM_CIRCUIT, CMBlocks.CMCircuit.values(), meta);
                default -> LegacyStacks.block(name);
            };
            if (b == null || b == Blocks.AIR) {
                LOGGER.error("[hbm_m] Custom machine component {} meta {} has no port equivalent", name, meta);
                b = Blocks.AIR;
            }
            if (!out.contains(b)) out.add(b);
        }
        if (out.isEmpty()) out.add(LegacyStacks.block(name));
        return out;
    }

    private static <E extends Enum<E>> Block enumBlock(java.util.Map<E, dev.architectury.registry.registries.RegistrySupplier<Block>> map, E[] values, int meta) {
        if (meta < 0 || meta >= values.length) return Blocks.AIR;
        return map.get(values[meta]).get();
    }

    public static class MachineConfiguration {

        /** The name of the recipe set that this machine can handle */
        public String recipeKey;
        /** The internal name of this machine */
        public String unlocalizedName;
        /** The display name of this machine */
        public String localizedName;
        /** Sprachcode (klein, z.B. "de_de") -> Name */
        public HashMap<String, String> localization = new HashMap<>();

        public int fluidInCount;
        public int fluidInCap;
        public int itemInCount;
        public int fluidOutCount;
        public int fluidOutCap;
        public int itemOutCount;
        /** Whether inputs should be used up when the process begins */
        public boolean generatorMode;
        public int maxPollutionCap;
        public boolean fluxMode;
        public double recipeSpeedMult = 1D;
        public double recipeConsumptionMult = 1D;
        public long maxPower;
        public int maxHeat;

        /** Definitions of blocks that this machine is composed of */
        public List<ComponentDefinition> components;

        /** Anzeigename in der Sprache des Spielers (Original {@code localization.get(languageCode)}). */
        public String displayName(String languageCode) {
            String l = localization.get(languageCode == null ? "" : languageCode.toLowerCase(java.util.Locale.US));
            return l != null ? l : localizedName;
        }

        public static class ComponentDefinition {
            /** erlaubte Bloecke (Original: block + allowedMetas) */
            public List<Block> blocks;
            public int x;
            public int y;
            public int z;
        }
    }

    /** Original {@code MainRegistry.proxy.getLanguageCode()}: Server "en_us", Client die gewaehlte Sprache. */
    public static String languageCode() {
        //? if forge {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) return com.hbm_m.client.util.ClientLang.code();
        //?}
        return "en_us";
    }

    /** Registry-Name eines Blocks (Vorschau, Fehlermeldungen). */
    public static String blockName(Block b) {
        return BuiltInRegistries.BLOCK.getKey(b).toString();
    }
}
