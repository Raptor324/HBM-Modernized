package com.hbm_m.handler;

import com.hbm_m.platform.EffectHooks;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.armormod.item.ItemModCladding;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.ShadyUtil;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code com.hbm.handler.HazmatRegistry}: Strahlungsresistenz je Ruestungsteil (Summe ueber die
 * getragenen Teile), plus Verkleidung ({@code hfr_cladding} bzw. Verkleidungs-Mod). FSB-Sets tragen ihre
 * Vollset-Resistenz ueber {@code ArmorFSB.setRadResist} in {@link #external} ein.
 * Wie im Original ueberschreibbar per {@code config/hbm_m/hbmRadResist.json} (Vorlage {@code _hbmRadResist.json}).
 */
public final class HazmatRegistry {

    public static final double HELMET = 0.2D;
    public static final double CHEST = 0.4D;
    public static final double LEGS = 0.3D;
    public static final double BOOTS = 0.1D;

    public record ExternalEntry(Item item, double resistance) {}

    public static final List<ExternalEntry> external = new ArrayList<>();

    private static final Map<Item, Double> entries = new HashMap<>();

    private HazmatRegistry() {}

    /** Original ArmorFSB.setRadResist: Vollsetwert anteilig nach Teil. */
    public static void registerExternalFullSet(ArmorItem item, double fullSet) {
        double mult = switch (item.getType()) {
            case HELMET -> HELMET;
            case CHESTPLATE -> CHEST;
            case LEGGINGS -> LEGS;
            default -> BOOTS;
        };
        external.add(new ExternalEntry(item, fullSet * mult));
    }

    public static final com.google.gson.Gson gson = new com.google.gson.Gson();

    /**
     * Original registerHazmats: Standardwerte setzen; fehlt {@code hbmRadResist.json} im Konfig-Ordner, wird die Vorlage
     * {@code _hbmRadResist.json} geschrieben, sonst ersetzt die Datei alle Eintraege.
     */
    public static void registerHazmats() {
        java.io.File folder = com.hbm_m.config.ConfigPaths.configRoot().toFile();
        folder.mkdirs();

        java.io.File config = new java.io.File(folder.getAbsolutePath() + java.io.File.separatorChar + "hbmRadResist.json");
        java.io.File template = new java.io.File(folder.getAbsolutePath() + java.io.File.separatorChar + "_hbmRadResist.json");

        initDefault();

        if (!config.exists()) {
            writeDefault(template);
        } else {
            HashMap<Item, Double> conf = readConfig(config);

            if (conf != null) {
                entries.clear();
                entries.putAll(conf);
            }
        }
    }

    private static void writeDefault(java.io.File file) {
        try {
            com.google.gson.stream.JsonWriter writer = new com.google.gson.stream.JsonWriter(new java.io.FileWriter(file));
            writer.setIndent("  ");                 //pretty formatting
            writer.beginObject();                   //initial '{'
            writer.name("comment").value("Template file, remove the underscore ('_') from the name to enable the config.");
            writer.name("entries").beginArray();    //all recipes are stored in an array called "entries"

            for (Map.Entry<Item, Double> entry : entries.entrySet()) {
                writer.beginObject();               //begin object for a single recipe
                writer.name("item").value(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
                writer.name("resistance").value(entry.getValue());
                writer.endObject();                 //end recipe object
            }

            writer.endArray();                      //end recipe array
            writer.endObject();                     //final '}'
            writer.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private static HashMap<Item, Double> readConfig(java.io.File config) {
        try (java.io.FileReader reader = new java.io.FileReader(config)) {
            com.google.gson.JsonObject json = gson.fromJson(reader, com.google.gson.JsonObject.class);
            com.google.gson.JsonArray array = json.get("entries").getAsJsonArray();
            HashMap<Item, Double> conf = new HashMap<>();

            for (com.google.gson.JsonElement element : array) {
                com.google.gson.JsonObject object = (com.google.gson.JsonObject) element;

                try {
                    String name = object.get("item").getAsString();
                    net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(name);
                    Item item = id != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id) ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id) : null;
                    double resistance = object.get("resistance").getAsDouble();
                    if (item != null) {
                        conf.put(item, resistance);
                    } else {
                        com.hbm_m.main.MainRegistry.LOGGER.error("Tried loading unknown item " + name + " for hazmat entry.");
                    }
                } catch (Exception ex) {
                    com.hbm_m.main.MainRegistry.LOGGER.error("Encountered " + ex + " trying to read hazmat entry " + element.toString());
                }
            }
            return conf;

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private static void reg(RegistrySupplier<Item> item, double resistance) {
        entries.put(item.get(), resistance);
    }

    private static void set(RegistrySupplier<Item> h, RegistrySupplier<Item> c, RegistrySupplier<Item> l, RegistrySupplier<Item> b, double mat) {
        if (h != null) reg(h, mat * HELMET);
        reg(c, mat * CHEST);
        reg(l, mat * LEGS);
        reg(b, mat * BOOTS);
    }

    public static void initDefault() {

        for (ExternalEntry pair : external) {
            registerHazmat(pair.item(), pair.resistance());
        }

        //assuming coefficient of 10
        //real coefficient turned out to be 5
        //oops

        double iron = 0.0225D; // 5%
        double gold = 0.0225D; // 5%
        double steel = 0.045D; // 10%
        double titanium = 0.045D; // 10%
        double alloy = 0.07D; // 15%
        double cobalt = 0.125D; // 25%

        double hazYellow = 0.6D; // 50%
        double hazRed = 1.0D; // 90%
        double hazGray = 2D; // 99%
        double paa = 1.7D; // 97%
        double liquidator = 2.4D; // 99.6%

        double security = 0.825D; // 85%
        double star = 1D; // 90%
        double cmb = 1.3D; // 95%
        double schrab = 3D; // 99.9%
        double euph = 10D; // <100%

        set(ModItems.HAZMAT_HELMET, ModItems.HAZMAT_PLATE, ModItems.HAZMAT_LEGS, ModItems.HAZMAT_BOOTS, hazYellow);
        set(ModItems.HAZMAT_HELMET_RED, ModItems.HAZMAT_PLATE_RED, ModItems.HAZMAT_LEGS_RED, ModItems.HAZMAT_BOOTS_RED, hazRed);
        set(ModItems.HAZMAT_HELMET_GREY, ModItems.HAZMAT_PLATE_GREY, ModItems.HAZMAT_LEGS_GREY, ModItems.HAZMAT_BOOTS_GREY, hazGray);
        set(ModItems.LIQUIDATOR_HELMET, ModItems.LIQUIDATOR_PLATE, ModItems.LIQUIDATOR_LEGS, ModItems.LIQUIDATOR_BOOTS, liquidator);
        set(null, ModItems.PAA_PLATE, ModItems.PAA_LEGS, ModItems.PAA_BOOTS, paa);
        set(ModItems.HAZMAT_PAA_HELMET, ModItems.HAZMAT_PAA_PLATE, ModItems.HAZMAT_PAA_LEGS, ModItems.HAZMAT_PAA_BOOTS, paa);
        set(ModItems.SECURITY_HELMET, ModItems.SECURITY_PLATE, ModItems.SECURITY_LEGS, ModItems.SECURITY_BOOTS, security);
        set(ModItems.STARMETAL_HELMET, ModItems.STARMETAL_PLATE, ModItems.STARMETAL_LEGS, ModItems.STARMETAL_BOOTS, star);

        reg(ModItems.JACKT, 0.1);
        reg(ModItems.JACKT2, 0.1);

        reg(ModItems.GAS_MASK, 0.07);
        reg(ModItems.GAS_MASK_M65, 0.095);

        set(ModItems.STEEL_HELMET, ModItems.STEEL_PLATE, ModItems.STEEL_LEGS, ModItems.STEEL_BOOTS, steel);
        set(ModItems.TITANIUM_HELMET, ModItems.TITANIUM_PLATE, ModItems.TITANIUM_LEGS, ModItems.TITANIUM_BOOTS, titanium);
        set(ModItems.COBALT_HELMET, ModItems.COBALT_PLATE, ModItems.COBALT_LEGS, ModItems.COBALT_BOOTS, cobalt);

        registerHazmat(Items.IRON_HELMET, iron * HELMET);
        registerHazmat(Items.IRON_CHESTPLATE, iron * CHEST);
        registerHazmat(Items.IRON_LEGGINGS, iron * LEGS);
        registerHazmat(Items.IRON_BOOTS, iron * BOOTS);

        registerHazmat(Items.GOLDEN_HELMET, gold * HELMET);
        registerHazmat(Items.GOLDEN_CHESTPLATE, gold * CHEST);
        registerHazmat(Items.GOLDEN_LEGGINGS, gold * LEGS);
        registerHazmat(Items.GOLDEN_BOOTS, gold * BOOTS);

        set(ModItems.ALLOY_HELMET, ModItems.ALLOY_PLATE, ModItems.ALLOY_LEGS, ModItems.ALLOY_BOOTS, alloy);
        set(ModItems.CMB_HELMET, ModItems.CMB_PLATE, ModItems.CMB_LEGS, ModItems.CMB_BOOTS, cmb);
        set(ModItems.SCHRABIDIUM_HELMET, ModItems.SCHRABIDIUM_PLATE, ModItems.SCHRABIDIUM_LEGS, ModItems.SCHRABIDIUM_BOOTS, schrab);
        set(ModItems.EUPHEMIUM_HELMET, ModItems.EUPHEMIUM_PLATE, ModItems.EUPHEMIUM_LEGS, ModItems.EUPHEMIUM_BOOTS, euph);
    }

    public static void registerHazmat(Item item, double resistance) {
        entries.put(item, resistance);
    }

    public static double getResistance(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return 0;

        double cladding = getCladding(stack);

        Double f = entries.get(stack.getItem());

        if (f != null)
            return f + cladding;

        return cladding;
    }

    public static double getCladding(ItemStack stack) {

        if (StackNbt.has(stack) && StackNbt.read(stack).getFloat("hfr_cladding") > 0)
            return StackNbt.read(stack).getFloat("hfr_cladding");

        if (ArmorModificationHelper.hasMods(stack)) {
            ItemStack cladding = ArmorModificationHelper.pryMods(stack)[ArmorModificationHelper.cladding];

            if (cladding != null && cladding.getItem() instanceof ItemModCladding mod) {
                return mod.rad;
            }
        }

        return 0;
    }

    public static float getResistance(Player player) {

        float res = 0.0F;

        if (player.getUUID().toString().equals(ShadyUtil.Pu_238)) {
            res += 0.4F;
        }

        for (ItemStack stack : player.getArmorSlots()) {
            res += (float) getResistance(stack);
        }

        if (player.hasEffect(EffectHooks.of(com.hbm_m.effect.ModEffects.RADX)))
            res += 0.2F;

        return res;
    }
}
