package com.hbm_m.powerarmor.resist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.config.ConfigPaths;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.mob.EntityCreeperNuclear;
import com.hbm_m.interfaces.IResistanceProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.powerarmor.ModArmorFSBPowered;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.util.DamageResistanceHandler}: Schadensschwelle (DT) und Schadensresistenz (DR) fuer
 * einzelne Ruestungsteile, volle Sets und Entity-Klassen. Die Werte stehen wie im Original in
 * {@code config/hbm_m/hbmArmor.json}; fehlt die Datei, gelten die Standardwerte und {@code _hbmArmor.json}
 * wird als Vorlage geschrieben. Die beiden Event-Handler ruft {@code HbmForgeEvents} auf.
 *
 * @author hbm
 */
public class DamageResistanceHandler {

    /** Currently cached DT reduction */
    public static float currentPDT = 0F;
    /** Currently cached armor piercing % */
    public static float currentPDR = 0F;

    public static final String CATEGORY_EXPLOSION = "EXPL";
    public static final String CATEGORY_FIRE = "FIRE";
    public static final String CATEGORY_PHYSICAL = "PHYS";
    public static final String CATEGORY_ENERGY = "EN";

    public static final Gson gson = new Gson();

    public static HashMap<Item, ResistanceStats> itemStats = new HashMap<>();
    public static HashMap<Quartet, ResistanceStats> setStats = new HashMap<>();
    public static HashMap<Class<? extends Entity>, ResistanceStats> entityStats = new HashMap<>();

    public static HashMap<Item, List<Quartet>> itemInfoSet = new HashMap<>();

    /** Original {@code Tuple.Quartet<Item, Item, Item, Item>}: Helm, Brust, Beine, Stiefel. */
    public record Quartet(Item w, Item x, Item y, Item z) {
        public Item getW() { return w; }
        public Item getX() { return x; }
        public Item getY() { return y; }
        public Item getZ() { return z; }
    }

    public static void init() {
        File folder = ConfigPaths.configRoot().toFile();
        folder.mkdirs();

        File config = new File(folder.getAbsolutePath() + File.separatorChar + "hbmArmor.json");
        File template = new File(folder.getAbsolutePath() + File.separatorChar + "_hbmArmor.json");

        clearSystem();

        if (!config.exists()) {
            initDefaults();
            writeDefault(template);
        } else {
            readConfig(config);
        }
    }

    private static void clearSystem() {
        itemStats.clear();
        setStats.clear();
        entityStats.clear();
        itemInfoSet.clear();
    }

    private static void writeDefault(File file) {

        MainRegistry.LOGGER.info("No armor file found, registering defaults for " + file.getName());

        try {
            JsonWriter writer = new JsonWriter(new FileWriter(file));
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("comment").value("Template file, remove the underscore ('_') from the name to enable the config.");

            serialize(writer);

            writer.endObject();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readConfig(File file) {

        MainRegistry.LOGGER.info("Reading armor file " + file.getName());

        try {
            JsonObject json = gson.fromJson(new FileReader(file), JsonObject.class);
            deserialize(json);

        } catch (FileNotFoundException ex) {
            clearSystem();
            initDefaults();
            ex.printStackTrace();
        }
    }

    private static Item i(RegistrySupplier<Item> item) {
        return item.get();
    }

    public static void initDefaults() {

        entityStats.put(Creeper.class, new ResistanceStats().addCategory(CATEGORY_EXPLOSION, 2F, 0.25F));
        entityStats.put(EntityCreeperNuclear.class, new ResistanceStats().addCategory(CATEGORY_EXPLOSION, 5F, 0.35F));

        itemStats.put(i(ModItems.JACKT), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 1F, 0.20F));
        itemStats.put(i(ModItems.JACKT2), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.25F));

        registerSet(i(ModItems.STEEL_HELMET), i(ModItems.STEEL_PLATE), i(ModItems.STEEL_LEGS), i(ModItems.STEEL_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet(i(ModItems.TITANIUM_HELMET), i(ModItems.TITANIUM_PLATE), i(ModItems.TITANIUM_LEGS), i(ModItems.TITANIUM_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 3F, 0.1F));
        registerSet(i(ModItems.ALLOY_HELMET), i(ModItems.ALLOY_PLATE), i(ModItems.ALLOY_LEGS), i(ModItems.ALLOY_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet(i(ModItems.COBALT_HELMET), i(ModItems.COBALT_PLATE), i(ModItems.COBALT_LEGS), i(ModItems.COBALT_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.1F));
        registerSet(i(ModItems.STARMETAL_HELMET), i(ModItems.STARMETAL_PLATE), i(ModItems.STARMETAL_LEGS), i(ModItems.STARMETAL_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 3F, 0.25F)
                .setOther(1F, 0.1F));
        registerSet(i(ModItems.ZIRCONIUM_LEGS), i(ModItems.ZIRCONIUM_LEGS), i(ModItems.ZIRCONIUM_LEGS), i(ModItems.ZIRCONIUM_LEGS), new ResistanceStats()
                .setOther(0F, 1F));
        registerSet(i(ModItems.DNT_HELMET), i(ModItems.DNT_PLATE), i(ModItems.DNT_LEGS), i(ModItems.DNT_BOOTS), new ResistanceStats());
        registerSet(i(ModItems.CMB_HELMET), i(ModItems.CMB_PLATE), i(ModItems.CMB_LEGS), i(ModItems.CMB_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .setOther(5F, 0.25F));
        registerSet(i(ModItems.SCHRABIDIUM_HELMET), i(ModItems.SCHRABIDIUM_PLATE), i(ModItems.SCHRABIDIUM_LEGS), i(ModItems.SCHRABIDIUM_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 10F, 0.65F)
                .setOther(5F, 0.5F));
        registerSet(i(ModItems.ROBES_HELMET), i(ModItems.ROBES_PLATE), i(ModItems.ROBES_LEGS), i(ModItems.ROBES_BOOTS), new ResistanceStats());

        registerSet(i(ModItems.SECURITY_HELMET), i(ModItems.SECURITY_PLATE), i(ModItems.SECURITY_LEGS), i(ModItems.SECURITY_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 2F, 0.25F));
        registerSet(i(ModItems.STEAMSUIT_HELMET), i(ModItems.STEAMSUIT_PLATE), i(ModItems.STEAMSUIT_LEGS), i(ModItems.STEAMSUIT_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.25F)
                .addExact("fall", 5F, 0.25F)
                .setOther(0F, 0.1F));
        registerSet(i(ModItems.DIESELSUIT_HELMET), i(ModItems.DIESELSUIT_PLATE), i(ModItems.DIESELSUIT_LEGS), i(ModItems.DIESELSUIT_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 1F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 2F, 0.15F)
                .setOther(0F, 0.1F));
        registerSet(i(ModItems.T51_HELMET), i(ModItems.T51_PLATE), i(ModItems.T51_LEGS), i(ModItems.T51_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(0F, 0.1F));
        registerSet(i(ModItems.AJR_HELMET), i(ModItems.AJR_PLATE), i(ModItems.AJR_LEGS), i(ModItems.AJR_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 4F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                .addCategory(CATEGORY_EXPLOSION, 7.5F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(0F, 0.15F));
        registerSet(i(ModItems.AJRO_HELMET), i(ModItems.AJRO_PLATE), i(ModItems.AJRO_LEGS), i(ModItems.AJRO_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 4F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.35F)
                .addCategory(CATEGORY_EXPLOSION, 7.5F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(0F, 0.15F));
        registerSet(i(ModItems.RPA_HELMET), i(ModItems.RPA_PLATE), i(ModItems.RPA_LEGS), i(ModItems.RPA_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 25F, 0.65F)
                .addCategory(CATEGORY_FIRE, 10F, 0.9F)
                .addCategory(CATEGORY_EXPLOSION, 15F, 0.25F)
                .addCategory(CATEGORY_ENERGY, 25F, 0.75F)
                .addExact("fall", 0F, 1F)
                .setOther(15F, 0.3F));
        registerSet(i(ModItems.NCRPA_HELMET), i(ModItems.NCRPA_PLATE), i(ModItems.NCRPA_LEGS), i(ModItems.NCRPA_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 25F, 0.65F)
                .addCategory(CATEGORY_FIRE, 10F, 0.9F)
                .addCategory(CATEGORY_EXPLOSION, 15F, 0.25F)
                .addCategory(CATEGORY_ENERGY, 10F, 0.5F)
                .addExact("fall", 0F, 1F)
                .setOther(15F, 0.25F));
        ResistanceStats bj = new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_FIRE, 2.5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 10F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(2F, 0.15F);
        registerSet(i(ModItems.BJ_HELMET), i(ModItems.BJ_PLATE), i(ModItems.BJ_LEGS), i(ModItems.BJ_BOOTS), bj);
        registerSet(i(ModItems.BJ_HELMET), i(ModItems.BJ_PLATE_JETPACK), i(ModItems.BJ_LEGS), i(ModItems.BJ_BOOTS), bj);
        registerSet(i(ModItems.ENVSUIT_HELMET), i(ModItems.ENVSUIT_PLATE), i(ModItems.ENVSUIT_LEGS), i(ModItems.ENVSUIT_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_FIRE, 2F, 0.75F)
                .addExact("drown", 0F, 1F)
                .addExact("fall", 5F, 0.75F)
                .setOther(0F, 0.1F));
        registerSet(i(ModItems.HEV_HELMET), i(ModItems.HEV_PLATE), i(ModItems.HEV_LEGS), i(ModItems.HEV_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.25F)
                .addCategory(CATEGORY_FIRE, 0.5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact("onFire", 0F, 1F)
                .addExact("fall", 10F, 0F)
                .setOther(2F, 0.25F));
        registerSet(i(ModItems.BISMUTH_HELMET), i(ModItems.BISMUTH_PLATE), i(ModItems.BISMUTH_LEGS), i(ModItems.BISMUTH_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact("fall", 0F, 1F)
                .setOther(2F, 0.25F));
        registerSet(i(ModItems.FAU_HELMET), i(ModItems.FAU_PLATE), i(ModItems.FAU_LEGS), i(ModItems.FAU_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 100F, 0.99F)
                .addCategory(CATEGORY_EXPLOSION, 50F, 0.95F)
                .addCategory(CATEGORY_FIRE, 100F, 1F)
                .addExact(DamageClass.LASER.name(), 25F, 0.95F)
                .addExact("fall", 0F, 1F)
                .setOther(100F, 0.99F));
        registerSet(i(ModItems.DNS_HELMET), i(ModItems.DNS_PLATE), i(ModItems.DNS_LEGS), i(ModItems.DNS_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 1000F, 1F)
                .addCategory(CATEGORY_EXPLOSION, 100F, 0.99F)
                .addCategory(CATEGORY_FIRE, 0F, 1F)
                .setOther(1000F, 1F));
        registerSet(i(ModItems.TAURUN_HELMET), i(ModItems.TAURUN_PLATE), i(ModItems.TAURUN_LEGS), i(ModItems.TAURUN_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 2F, 0.15F)
                .addCategory(CATEGORY_FIRE, 0F, 0.25F)
                .addCategory(CATEGORY_EXPLOSION, 0F, 0.25F)
                .addExact("fall", 4F, 0.5F)
                .setOther(2F, 0.1F));
        registerSet(i(ModItems.TRENCHMASTER_HELMET), i(ModItems.TRENCHMASTER_PLATE), i(ModItems.TRENCHMASTER_LEGS), i(ModItems.TRENCHMASTER_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_PHYSICAL, 5F, 0.5F)
                .addCategory(CATEGORY_FIRE, 5F, 0.5F)
                .addCategory(CATEGORY_EXPLOSION, 5F, 0.25F)
                .addExact(DamageClass.LASER.name(), 15F, 0.9F)
                .addExact("fall", 10F, 0.5F)
                .setOther(5F, 0.25F));

        registerSet(i(ModItems.EUPHEMIUM_HELMET), i(ModItems.EUPHEMIUM_PLATE), i(ModItems.EUPHEMIUM_LEGS), i(ModItems.EUPHEMIUM_BOOTS), new ResistanceStats()
                .setOther(1_000_000F, 1F));

        registerSet(i(ModItems.HAZMAT_HELMET), i(ModItems.HAZMAT_PLATE), i(ModItems.HAZMAT_LEGS), i(ModItems.HAZMAT_BOOTS), new ResistanceStats());
        registerSet(i(ModItems.HAZMAT_HELMET_RED), i(ModItems.HAZMAT_PLATE_RED), i(ModItems.HAZMAT_LEGS_RED), i(ModItems.HAZMAT_BOOTS_RED), new ResistanceStats());
        registerSet(i(ModItems.HAZMAT_HELMET_GREY), i(ModItems.HAZMAT_PLATE_GREY), i(ModItems.HAZMAT_LEGS_GREY), i(ModItems.HAZMAT_BOOTS_GREY), new ResistanceStats());
        registerSet(i(ModItems.LIQUIDATOR_HELMET), i(ModItems.LIQUIDATOR_PLATE), i(ModItems.LIQUIDATOR_LEGS), i(ModItems.LIQUIDATOR_BOOTS), new ResistanceStats());
        registerSet(i(ModItems.HAZMAT_PAA_HELMET), i(ModItems.HAZMAT_PAA_PLATE), i(ModItems.HAZMAT_PAA_LEGS), i(ModItems.HAZMAT_PAA_BOOTS), new ResistanceStats());
        registerSet(i(ModItems.ASBESTOS_HELMET), i(ModItems.ASBESTOS_PLATE), i(ModItems.ASBESTOS_LEGS), i(ModItems.ASBESTOS_BOOTS), new ResistanceStats()
                .addCategory(CATEGORY_FIRE, 10F, 0.9F));
    }

    public static void registerSet(Item helmet, Item plate, Item legs, Item boots, ResistanceStats stats) {
        Quartet set = new Quartet(helmet, plate, legs, boots);
        setStats.put(set, stats);
        if (helmet != null) addToListInHashMap(helmet, itemInfoSet, set);
        if (plate != null) addToListInHashMap(plate, itemInfoSet, set);
        if (legs != null) addToListInHashMap(legs, itemInfoSet, set);
        if (boots != null) addToListInHashMap(boots, itemInfoSet, set);
    }

    public static <K, V> void addToListInHashMap(K key, HashMap<K, List<V>> map, V listElement) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(listElement);
    }

    private static String res(String key) {
        return Component.translatable(key).getString();
    }

    private static String line(Resistance r) {
        return r.threshold + "/" + ((int) (r.resistance * 100)) + "%";
    }

    private static List<Component> statLines(ResistanceStats stats) {
        List<Component> toAdd = new ArrayList<>();
        for (Entry<String, Resistance> entry : stats.categoryResistances.entrySet()) {
            toAdd.add(Component.literal(res("damage.category." + entry.getKey()) + ": " + line(entry.getValue())));
        }
        for (Entry<String, Resistance> entry : stats.exactResistances.entrySet()) {
            toAdd.add(Component.literal(res("damage.exact." + entry.getKey()) + ": " + line(entry.getValue())));
        }
        if (stats.otherResistance != null) toAdd.add(Component.literal(res("damage.other") + ": " + line(stats.otherResistance)));
        return toAdd;
    }

    public static void addInfo(ItemStack stack, List<Component> desc) {
        if (stack == null || stack.isEmpty()) return;

        if (itemInfoSet.containsKey(stack.getItem())) {
            List<Quartet> sets = itemInfoSet.get(stack.getItem());

            for (Quartet set : sets) {

                ResistanceStats stats = setStats.get(set);
                if (stats == null) continue;

                List<Component> toAdd = statLines(stats);

                if (!toAdd.isEmpty()) {
                    desc.add(Component.literal(res("damage.inset")).withStyle(ChatFormatting.DARK_PURPLE));
                    //this sucks ass!
                    if (set.getW() != null) desc.add(Component.literal("  ").append(new ItemStack(set.getW()).getHoverName()).withStyle(ChatFormatting.DARK_PURPLE));
                    if (set.getX() != null) desc.add(Component.literal("  ").append(new ItemStack(set.getX()).getHoverName()).withStyle(ChatFormatting.DARK_PURPLE));
                    if (set.getY() != null) desc.add(Component.literal("  ").append(new ItemStack(set.getY()).getHoverName()).withStyle(ChatFormatting.DARK_PURPLE));
                    if (set.getZ() != null) desc.add(Component.literal("  ").append(new ItemStack(set.getZ()).getHoverName()).withStyle(ChatFormatting.DARK_PURPLE));
                    desc.addAll(toAdd);
                }

                break; //TEMP, only show one set for now
            }
        }

        if (itemStats.containsKey(stack.getItem())) {
            List<Component> toAdd = statLines(itemStats.get(stack.getItem()));

            if (!toAdd.isEmpty()) {
                desc.add(Component.literal(res("damage.item")).withStyle(ChatFormatting.DARK_PURPLE));
                desc.addAll(toAdd);
            }
        }
    }

    private static String name(Item item) {
        if (item == null) return null;
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static Item item(String name) {
        ResourceLocation rl = ResourceLocation.tryParse(name);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return null;
        return BuiltInRegistries.ITEM.get(rl);
    }

    public static void serialize(JsonWriter writer) throws IOException {
        /// ITEMS ///
        writer.name("itemStats").beginArray();
        for (Entry<Item, ResistanceStats> entry : itemStats.entrySet()) {
            writer.beginArray().setIndent("");
            writer.value(name(entry.getKey())).setIndent("  ");
            writer.beginObject();
            entry.getValue().serialize(writer);
            writer.setIndent("");
            writer.endObject().endArray().setIndent("  ");
        }
        writer.endArray();

        /// SETS ///
        writer.name("setStats").beginArray();
        for (Entry<Quartet, ResistanceStats> entry : setStats.entrySet()) {
            writer.beginArray().setIndent("");
            writer.value(name(entry.getKey().getW()))
                    .value(name(entry.getKey().getX()))
                    .value(name(entry.getKey().getY()))
                    .value(name(entry.getKey().getZ())).setIndent("  ");
            writer.beginObject();
            entry.getValue().serialize(writer);
            writer.setIndent("");
            writer.endObject().endArray().setIndent("  ");
        }
        writer.endArray();

        /// ENTITIES ///
        writer.name("entityStats").beginArray();
        for (Entry<Class<? extends Entity>, ResistanceStats> entry : entityStats.entrySet()) {
            writer.beginArray().setIndent("");
            writer.value(entry.getKey().getName()).setIndent("  ");
            writer.beginObject();
            entry.getValue().serialize(writer);
            writer.setIndent("");
            writer.endObject().endArray().setIndent("  ");
        }
        writer.endArray();
    }

    @SuppressWarnings("unchecked")
    public static void deserialize(JsonObject json) {
        /// ITEMS ///
        JsonArray itemStatsArray = json.get("itemStats").getAsJsonArray();
        for (JsonElement element : itemStatsArray) {
            JsonArray statArray = element.getAsJsonArray();
            Item item = item(statArray.get(0).getAsString());
            JsonObject stats = statArray.get(1).getAsJsonObject();
            itemStats.put(item, ResistanceStats.deserialize(stats));
        }

        /// SETS ///
        JsonArray setStatsArray = json.get("setStats").getAsJsonArray();
        for (JsonElement element : setStatsArray) {
            JsonArray statArray = element.getAsJsonArray();
            Item helmet = statArray.get(0).isJsonNull() ? null : item(statArray.get(0).getAsString());
            Item plate = statArray.get(1).isJsonNull() ? null : item(statArray.get(1).getAsString());
            Item legs = statArray.get(2).isJsonNull() ? null : item(statArray.get(2).getAsString());
            Item boots = statArray.get(3).isJsonNull() ? null : item(statArray.get(3).getAsString());
            JsonObject stats = statArray.get(4).getAsJsonObject();
            registerSet(helmet, plate, legs, boots, ResistanceStats.deserialize(stats));
        }

        /// ENTITIES ///
        JsonArray entityStatsArray = json.get("entityStats").getAsJsonArray();
        for (JsonElement element : entityStatsArray) {
            JsonArray statArray = element.getAsJsonArray();
            try {
                Class<? extends Entity> clazz = (Class<? extends Entity>) Class.forName(statArray.get(0).getAsString());
                JsonObject stats = statArray.get(1).getAsJsonObject();
                entityStats.put(clazz, ResistanceStats.deserialize(stats));
            } catch (ClassNotFoundException e) { }
        }
    }

    public static enum DamageClass {
        PHYSICAL,
        FIRE,
        EXPLOSIVE,
        ELECTRIC,
        PLASMA,
        LASER,
        MICROWAVE,
        SUBATOMIC,
        OTHER;

        public boolean isApplicable(String name) {
            return name.toLowerCase(Locale.US).equals(this.name().toLowerCase(Locale.US));
        }
    }

    public static void setup(float dt, float dr) {
        currentPDT = dt;
        currentPDR = dr;
    }

    public static void reset() {
        currentPDT = 0;
        currentPDR = 0;
    }

    /** Original {@code DamageSource.isDamageAbsolute()}. */
    public static boolean isDamageAbsolute(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_EFFECTS);
    }

    /** Original {@code DamageSource.isUnblockable()}. */
    public static boolean isUnblockable(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_ARMOR);
    }

    /** Original {@code onEntityAttacked(LivingAttackEvent)}. Rueckgabe true = Angriff abbrechen. */
    public static boolean onEntityAttacked(LivingEntity e, DamageSource source, float amount) {
        if (isDamageAbsolute(source)) return false;

        float[] vals = getDTDR(e, source, amount, currentPDT, currentPDR);
        float dt = vals[0] - currentPDT;
        float dr = vals[1] - currentPDR;

        if ((dt > 0 && dt >= amount) || dr >= 1F) {
            damageArmorNT(e, amount);
            return true;
        }
        return false;
    }

    /** Original {@code EntityDamageUtil.damageArmorNT}: im Original leer. */
    public static void damageArmorNT(LivingEntity living, float amount) { }

    /** Original {@code onEntityDamaged(LivingHurtEvent)}: gibt den neuen Schaden zurueck. */
    public static float onEntityDamaged(LivingEntity e, DamageSource source, float amount) {

        if (source.getMsgId().toLowerCase(Locale.US).equals(DamageClass.ELECTRIC.name().toLowerCase(Locale.US))) {
            ItemStack chest = e.getItemBySlot(EquipmentSlot.CHEST);
            if (!chest.isEmpty() && chest.getItem() instanceof ModArmorFSBPowered) {
                amount *= 5;
            }
        }

        amount = calculateDamage(e, source, amount, currentPDT, currentPDR);
        if (e instanceof IResistanceProvider irp) {
            irp.onDamageDealt(source, amount);
        }
        return amount;
    }

    public static String typeToCategory(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return CATEGORY_EXPLOSION;
        if (source.is(DamageTypeTags.IS_FIRE)) return CATEGORY_FIRE;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return CATEGORY_PHYSICAL;
        String damageType = source.getMsgId();
        if (DamageClass.LASER.isApplicable(damageType)) return CATEGORY_ENERGY;
        if (DamageClass.PLASMA.isApplicable(damageType)) return CATEGORY_ENERGY;
        if (DamageClass.MICROWAVE.isApplicable(damageType)) return CATEGORY_ENERGY;
        if (DamageClass.SUBATOMIC.isApplicable(damageType)) return CATEGORY_ENERGY;
        if (DamageClass.ELECTRIC.isApplicable(damageType)) return CATEGORY_ENERGY;
        if (source.is(DamageTypes.CACTUS)) return CATEGORY_PHYSICAL;
        if (source.is(ModDamageTypes.SPIKES)) return CATEGORY_PHYSICAL;
        if (source.is(ModDamageTypes.ELECTRICITY)) return CATEGORY_ENERGY;
        if (source.is(ModDamageTypes.MICROWAVE)) return CATEGORY_ENERGY;
        if (source.getEntity() != null || source.getDirectEntity() != null) return CATEGORY_PHYSICAL;
        return damageType;
    }

    public static float calculateDamage(LivingEntity entity, DamageSource damage, float amount, float pierceDT, float pierce) {
        if (isDamageAbsolute(damage)) return amount;

        float[] vals = getDTDR(entity, damage, amount, pierceDT, pierce);
        float dt = vals[0];
        float dr = vals[1];

        dt = Math.max(0F, dt - pierceDT);
        if (dt >= amount) return 0F;
        amount -= dt;
        dr *= Mth.clamp(1F - pierce, 0F, 2F /* we allow up to -1 armor piercing, which can double effective armor values */);

        return amount *= (1F - dr);
    }

    public static float[] getDTDR(LivingEntity entity, DamageSource damage, float amount, float pierceDT, float pierce) {

        float dt = 0;
        float dr = 0;

        if (entity instanceof IResistanceProvider irp) {
            float[] res = irp.getCurrentDTDR(damage, amount, pierceDT, pierce);
            dt += res[0];
            dr += res[1];
        }

        /// SET HANDLING ///
        Quartet wornSet = new Quartet(
                itemOrNull(entity.getItemBySlot(EquipmentSlot.HEAD)),
                itemOrNull(entity.getItemBySlot(EquipmentSlot.CHEST)),
                itemOrNull(entity.getItemBySlot(EquipmentSlot.LEGS)),
                itemOrNull(entity.getItemBySlot(EquipmentSlot.FEET))
        );

        ResistanceStats setResistance = setStats.get(wornSet);
        if (setResistance != null) {
            Resistance res = setResistance.getResistance(damage);
            if (res != null) {
                dt += res.threshold;
                dr += res.resistance;
            }
        }

        /// ARMOR ///
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack armor = entity.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            ResistanceStats stats = itemStats.get(armor.getItem());
            if (stats == null) continue;
            Resistance res = stats.getResistance(damage);
            if (res == null) continue;
            dt += res.threshold;
            dr += res.resistance;
        }

        /// ENTITY CLASS HANDLING ///
        ResistanceStats innateResistance = entityStats.get(entity.getClass());
        if (innateResistance != null) {
            Resistance res = innateResistance.getResistance(damage);
            if (res != null) {
                dt += res.threshold;
                dr += res.resistance;
            }
        }

        return new float[] {dt, dr};
    }

    private static Item itemOrNull(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getItem();
    }

    public static class ResistanceStats {

        public HashMap<String, Resistance> exactResistances = new HashMap<>();
        public HashMap<String, Resistance> categoryResistances = new HashMap<>();
        public Resistance otherResistance;

        public Resistance getResistance(DamageSource source) {
            Resistance exact = exactResistances.get(source.getMsgId().toLowerCase());
            if (exact != null) return exact;
            Resistance category = categoryResistances.get(typeToCategory(source));
            if (category != null) return category;
            return isUnblockable(source) ? null : otherResistance;
        }

        public ResistanceStats addExact(String type, float threshold, float resistance) { exactResistances.put(type.toLowerCase(Locale.US), new Resistance(threshold, resistance)); return this; }
        public ResistanceStats addCategory(String type, float threshold, float resistance) { categoryResistances.put(type, new Resistance(threshold, resistance)); return this; }
        public ResistanceStats setOther(float threshold, float resistance) { otherResistance = new Resistance(threshold, resistance); return this; }

        public void serialize(JsonWriter writer) throws IOException {

            if (!exactResistances.isEmpty()) {
                writer.name("exact").beginArray();
                for (Entry<String, Resistance> entry : exactResistances.entrySet()) {
                    writer.beginArray().setIndent("");
                    writer.value(entry.getKey()).value(entry.getValue().threshold).value(entry.getValue().resistance).endArray().setIndent("  ");
                }
                writer.endArray();
            }

            if (!categoryResistances.isEmpty()) {
                writer.name("category").beginArray();
                for (Entry<String, Resistance> entry : categoryResistances.entrySet()) {
                    writer.beginArray().setIndent("");
                    writer.value(entry.getKey()).value(entry.getValue().threshold).value(entry.getValue().resistance).endArray().setIndent("  ");
                }
                writer.endArray();
            }

            if (otherResistance != null) {
                writer.name("other").beginArray().setIndent("");
                writer.value(otherResistance.threshold).value(otherResistance.resistance).endArray().setIndent("  ");
            }
        }

        public static ResistanceStats deserialize(JsonObject json) {
            ResistanceStats stats = new ResistanceStats();

            if (json.has("exact")) {
                JsonArray exact = json.get("exact").getAsJsonArray();
                for (JsonElement element : exact) {
                    JsonArray array = element.getAsJsonArray();
                    stats.exactResistances.put(array.get(0).getAsString(), new Resistance(array.get(1).getAsFloat(), array.get(2).getAsFloat()));
                }
            }

            if (json.has("category")) {
                JsonArray category = json.get("category").getAsJsonArray();
                for (JsonElement element : category) {
                    JsonArray array = element.getAsJsonArray();
                    stats.categoryResistances.put(array.get(0).getAsString(), new Resistance(array.get(1).getAsFloat(), array.get(2).getAsFloat()));
                }
            }

            if (json.has("other")) {
                JsonArray other = json.get("other").getAsJsonArray();
                stats.otherResistance = new Resistance(other.get(0).getAsFloat(), other.get(1).getAsFloat());
            }

            return stats;
        }
    }

    public static class Resistance {

        public float threshold;
        public float resistance;

        public Resistance(float threshold, float resistance) {
            this.threshold = threshold;
            this.resistance = resistance;
        }
    }
}
