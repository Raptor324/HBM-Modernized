package com.hbm_m.qmaw;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm_m.inventory.recipes.LegacyStacks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 {@code com.hbm.qmaw.QMAWLoader}: liest beim Ressourcen-Neuladen alle Handbuchdateien
 * {@code assets/hbm_m/manual/**.json}.
 *
 * <p>Die "agonyEngine" des Originals (Mod-Jar, Entwicklungsordner, ZIP- und Ordner-Ressourcenpakete
 * einzeln zerlegen) entfaellt: der Ressourcenmanager von 1.20 liefert genau diese Quellen schon
 * zusammengefuehrt. Gegenstandsnamen in {@code icon}/{@code trigger} sind die 1.7.10-Namen
 * ({@code "hbm:item.ingot_desh"}) und werden ueber {@link LegacyStacks#item} aufgeloest; die Meta
 * des Originals entfaellt, weil Meta-Items im Port eigene IDs haben.</p>
 */
public class QMAWLoader implements ResourceManagerReloadListener {

    private static final Logger LOGGER = LogManager.getLogger("hbm_m");

    public static HashMap<String, QuickManualAndWiki> qmaw = new HashMap<>();
    public static HashMap<Item, QuickManualAndWiki> triggers = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager resMan) {
        long timestamp = System.currentTimeMillis();
        LOGGER.info("[QMAW] Reloading manual...");
        init(resMan);
        LOGGER.info("[QMAW] Loaded " + qmaw.size() + " manual entries! (" + (System.currentTimeMillis() - timestamp) + "ms)");
    }

    /** Searches the asset folder for QMAW format JSON files and adds entries based on that */
    public static void init(ResourceManager resMan) {
        qmaw.clear();
        triggers.clear();

        Map<ResourceLocation, Resource> found = resMan.listResources("manual", rl -> rl.getPath().endsWith(".json"));
        for (Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            ResourceLocation rl = entry.getKey();
            if (!rl.getNamespace().equals(RefStrings.MODID)) continue;
            String name = rl.getPath().substring("manual/".length());
            try (Reader reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                registerJson(name, obj);
                logFoundManual(name);
            } catch (Exception ex) {
                LOGGER.info("[QMAW] Error reading manual " + name + ": " + ex);
            }
        }
    }

    public static void logFoundManual(String name) { LOGGER.debug("[QMAW] Found manual " + name); }

    /** Original {@code GunFactory.EnumAmmo} in Ordinalreihenfolge (Meta von {@code ammo_standard}). */
    private static final String[] AMMO = { "stone", "stone_ap", "stone_iron", "stone_shot", "m357_bp", "m357_sp", "m357_fmj", "m357_jhp", "m357_ap", "m357_express", "m44_bp", "m44_sp", "m44_fmj", "m44_jhp", "m44_ap", "m44_express", "p22_sp", "p22_fmj", "p22_jhp", "p22_ap", "p9_sp", "p9_fmj", "p9_jhp", "p9_ap", "r556_sp", "r556_fmj", "r556_jhp", "r556_ap", "r762_sp", "r762_fmj", "r762_jhp", "r762_ap", "r762_du", "bmg50_sp", "bmg50_fmj", "bmg50_jhp", "bmg50_ap", "bmg50_du", "b75", "b75_inc", "b75_exp", "g12_bp", "g12_bp_magnum", "g12_bp_slug", "g12", "g12_slug", "g12_flechette", "g12_magnum", "g12_explosive", "g12_phosphorus", "g26_flare", "g26_flare_supply", "g26_flare_weapon", "g40_he", "g40_heat", "g40_demo", "g40_inc", "g40_phosphorus", "rocket_he", "rocket_heat", "rocket_demo", "rocket_inc", "rocket_phosphorus", "flame_diesel", "flame_gas", "flame_napalm", "flame_balefire", "capacitor", "capacitor_overcharge", "capacitor_ir", "tau_uranium", "coil_tungsten", "coil_ferrouranium", "nuke_standard", "nuke_demo", "nuke_high", "nuke_tots", "nuke_hive", "g10", "g10_shrapnel", "g10_du", "g10_slug", "r762_he", "bmg50_he", "g10_explosive", "p45_sp", "p45_fmj", "p45_jhp", "p45_ap", "p45_du", "ct_hook", "ct_mortar", "ct_mortar_charge", "nuke_balefire", "bmg50_sm" };
    /** Original {@code ItemSatellite.EnumSatType}. */
    private static final String[] SATELLITE = { "spy", "scanner", "radar", "miner_astro", "miner_lunar", "precision_laser", "death_ray", "xenium_resonator", "relay", "detector", "ray_scan" };
    /** Fluessigkeits-IDs des Originals, die in den Handbuchdateien vorkommen ({@code fluid_icon}-Meta). */
    private static final Map<Integer, String> FLUID_IDS = Map.of(1, "WATER", 10, "OIL", 12, "HEAVYOIL", 19, "NAPHTHA",
            21, "LIGHTOIL", 24, "PETROLEUM", 65, "LIGHTOIL_CRACK", 67, "AROMATICS", 68, "UNSATURATEDS");

    /**
     * Umbenennungen 1.7.10 -> Port fuer die Handbuchdateien ({@code name} oder {@code name#meta}); was
     * hier fehlt, loest {@link LegacyStacks#item} ueber die Wortreihenfolge auf. Ein Platzhalter-Meta
     * (32767) steht fuer alle Varianten und kann deshalb mehrere Gegenstaende liefern.
     */
    private static final Map<String, String[]> ALIAS = new HashMap<>();
    private static void alias(String from, String... to) { ALIAS.put(from, to); }
    static {
        alias("hbm:item.chunk_ore#0", "rareground_ore_chunk");
        alias("hbm:item.coke#0", "coal_coke");
        alias("hbm:item.coke#32767", "coal_coke", "lignite_coke", "coke_petroleum");
        alias("hbm:item.ingot_copper", "minecraft:copper_ingot");
        alias("hbm:item.powder_aluminium", "aluminum_powder");
        alias("hbm:item.powder_borax", "borax");
        alias("hbm:item.pa_coil#0", "pa_coil_gold");
        alias("hbm:item.pa_coil#32767", "pa_coil_gold", "pa_coil_niobium", "pa_coil_bscco", "pa_coil_chlorophyte");
        alias("hbm:tile.furnace_combination", "combination_oven");
        alias("hbm:tile.fusion_collector", "collector");
        alias("hbm:tile.fusion_coupler", "coupler");
        alias("hbm:tile.fusion_klystron", "klystron");
        alias("hbm:tile.fusion_mhdt", "mhdt");
        alias("hbm:tile.fusion_torus", "torus");
        alias("hbm:tile.heater_oven", "heating_oven");
        alias("hbm:tile.machine_assembly_machine", "advanced_assembly_machine");
        alias("hbm:tile.machine_catalytic_cracker", "cracking_tower");
        alias("hbm:tile.machine_condenser", "steam_condenser");
        alias("hbm:tile.machine_diesel", "dieselgen");
        alias("hbm:tile.machine_fracking_tower", "hydraulic_frackining_tower");
        alias("hbm:tile.machine_tower_large", "cooling_tower");
        alias("hbm:tile.machine_well", "derrick");
        alias("hbm:tile.pa_beamline", "beamline");
        alias("hbm:tile.pa_dipole", "dipole");
        alias("hbm:tile.pa_quadrupole", "quadrupole");
        alias("hbm:tile.pa_rfc", "rfc");
        alias("hbm:tile.pile_device#0", "pile_loader");
        alias("hbm:tile.pile_device#1", "pile_vent");
        alias("hbm:tile.pile_device#2", "pile_control");
        alias("hbm:tile.rbmk_rod", "rbmk_element");
        alias("hbm:tile.rbmk_rod_mod", "rbmk_element_mod");
        for (int i = 0; i < AMMO.length; i++) alias("hbm:item.ammo_standard#" + i, "ammo_standard_" + AMMO[i]);
        for (int i = 0; i < SATELLITE.length; i++) alias("hbm:item.satellite#" + i, "satellite_" + SATELLITE[i]);
    }

    private static Item byId(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : RefStrings.MODID + ":" + id);
        if (rl == null) return Items.AIR;
        return BuiltInRegistries.ITEM.get(rl);
    }

    /** Original {@code SerializableRecipe.readItemStack}: {@code [name, (count, (meta))]}; mehrere Treffer nur bei Platzhalter-Meta. */
    public static List<ItemStack> readItemStacks(JsonArray array) {
        List<ItemStack> out = new ArrayList<>();
        String name = array.get(0).getAsString();
        int count = array.size() > 1 ? array.get(1).getAsInt() : 1;
        int meta = array.size() > 2 ? array.get(2).getAsInt() : 0;

        // fluid_icon: im Port der Fluessigkeitskennzeichner mit gesetzter Fluessigkeit
        if (name.equals("hbm:item.fluid_icon")) {
            String fluid = FLUID_IDS.get(meta);
            net.minecraft.world.level.material.Fluid f = fluid != null ? LegacyStacks.fluid(fluid) : null;
            if (f != null) {
                ItemStack stack = new ItemStack(ModItems.FLUID_IDENTIFIER.get(), count);
                FluidIdentifierItem.setType(stack, f, true);
                out.add(stack);
            }
            return out;
        }

        String[] alias = ALIAS.get(name + "#" + meta);
        if (alias == null) alias = ALIAS.get(name);
        if (alias != null) {
            for (String id : alias) {
                Item item = byId(id);
                if (item != Items.AIR) out.add(new ItemStack(item, count));
            }
            return out;
        }

        Item item = LegacyStacks.item(name);
        if (item == Items.AIR) {
            // Port laesst die Praefixe machine_/heater_/furnace_ meist weg
            String path = name.substring(name.indexOf(':') + 1);
            if (path.startsWith("item.") || path.startsWith("tile.")) path = path.substring(5);
            for (String pre : new String[] { "machine_", "heater_", "furnace_" }) {
                if (path.startsWith(pre)) {
                    item = LegacyStacks.item("hbm:" + path.substring(pre.length()));
                    if (item != Items.AIR) break;
                }
            }
        }
        if (item != Items.AIR) out.add(new ItemStack(item, count));
        return out;
    }

    public static ItemStack readItemStack(JsonArray array) {
        List<ItemStack> stacks = readItemStacks(array);
        return stacks.isEmpty() ? null : stacks.get(0);
    }

    /** Extracts all the info from a json file's main object to add a QMAW to the system. Very barebones, only handles name, icon and the localized text. */
    public static void registerJson(String file, JsonObject json) {

        String name = json.get("name").getAsString();

        if (QMAWLoader.qmaw.containsKey(name)) {
            LOGGER.info("[QMAW] Overriding existing entry " + file);
        }

        QuickManualAndWiki qmaw = new QuickManualAndWiki(name);

        if (json.has("icon")) {
            qmaw.setIcon(readItemStack(json.get("icon").getAsJsonArray()));
        }

        JsonObject title = json.get("title").getAsJsonObject();
        for (Entry<String, JsonElement> part : title.entrySet()) {
            qmaw.addTitle(part.getKey(), part.getValue().getAsString());
        }

        JsonObject content = json.get("content").getAsJsonObject();
        for (Entry<String, JsonElement> part : content.entrySet()) {
            qmaw.addLang(part.getKey(), part.getValue().getAsString());
        }

        JsonArray triggers = json.get("trigger").getAsJsonArray();

        for (JsonElement element : triggers) {
            List<ItemStack> trigger = readItemStacks(element.getAsJsonArray());
            // items get renamed and removed all the time, so we add some more debug goodness for those cases
            if (trigger.isEmpty()) {
                LOGGER.info("[QMAW] Manual " + file + " references nonexistant trigger " + element);
            } else {
                for (ItemStack t : trigger) QMAWLoader.triggers.put(t.getItem(), qmaw);
            }
        }

        if (json.has("noindex") && json.get("noindex").getAsBoolean()) {
            qmaw.noIndex();
        }

        if (!qmaw.contents.isEmpty()) {
            QMAWLoader.qmaw.put(name, qmaw);
        }
    }
}
