package com.hbm_m.item.material;

import java.util.EnumMap;
import java.util.Map;

import com.hbm_m.item.LoreTooltipItem;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.RadioactiveItem;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;

/**
 * Автоматическая регистрация предметов материалов: по одному предмету на каждую
 * (материал, форма) из {@link ModMaterials#getShapes()}. Аналог ItemAutogen из 1.7.10,
 * но с отдельным Item на предмет (нет метаданных в 1.20.1/1.21.1).
 *
 * Регистрация вызывается из static-блока {@link ModItems} через один DeferredRegister.
 */
public final class ModMaterialItems {

    /** mat -> shape -> item; формы без предмета (BLOCK) отсутствуют. */
    private static final Map<ModMaterials, EnumMap<MaterialShape, RegistrySupplier<Item>>> ITEMS_BY_MATERIAL =
            new EnumMap<>(ModMaterials.class);

    private ModMaterialItems() {}

    /** Вызывается один раз из static-блока ModItems. */
    public static void registerAll() {
        for (ModMaterials mat : ModMaterials.values()) {
            for (MaterialShape shape : mat.getShapes()) {
                // Блоки хранения регистрирует ModBlocks (нужны BlockItem и свойства блока).
                if (shape == MaterialShape.BLOCK) continue;
                String id = shape.itemId(mat);
                RegistrySupplier<Item> item = ModItems.ITEMS.register(id, () -> createItem(mat, shape));
                ITEMS_BY_MATERIAL.computeIfAbsent(mat, m -> new EnumMap<>(MaterialShape.class))
                        .put(shape, item);
            }
        }
        // Литейные отходы (порт ItemScraps оригинала, item 4765): отдельный предмет
        // scraps_<материал> на каждый плавкий/присадочный материал Mats.java оригинала.
        for (ScrapEntry e : FOUNDRY_SCRAPS) {
            SCRAP_ITEMS.putIfAbsent(e.mat(), ModItems.ITEMS.register(
                    "scraps_" + e.mat().getId(), () -> new ScrapItem(new Item.Properties())));
        }
    }

    // =====================================================================================
    //  Литейные отходы (порт ItemScraps 1.7.10). В оригинале имя = "%s Scraps" ("%s Шлак")
    //  по hbmmat.* материала; здесь русские имена сделаны «красивыми» (адъективными).
    //  Порядок и состав списка = Mats.orderedList оригинала, см. ниже.
    //  Текстуры scraps_<id>.png запечены из scraps.png оригинала по solidColorLight/Dark
    //  Mats.java (RGBMutatorInterpolatedComponentRemap 0xFF..0x50 -> light..dark);
    //  висмут — готовая текстура scraps_bismuth.png (aot-оверрайд оригинала).
    // =====================================================================================
    public record ScrapEntry(ModMaterials mat, String en, String ru) {}

    private static ScrapEntry scrap(ModMaterials mat, String en, String ru) {
        return new ScrapEntry(mat, en, ru);
    }

    // Порядок = Mats.orderedList оригинала (порядок объявления в Mats.java): именно в нём
    // ItemScraps.getSubItems выдаёт суб-айтемы, он же задаёт порядок во вкладке Parts.
    // Полный набор = все SMELTABLE/ADDITIVE материалы оригинала (82 шт.).
    public static final List<ScrapEntry> FOUNDRY_SCRAPS = List.of(
        // Vanilla-like
        scrap(ModMaterials.STONE, "Stone Scraps", "Каменный шлак"), // 0
        scrap(ModMaterials.CARBON, "Carbon Scraps", "Углеродный шлак"), // 699, additive
        scrap(ModMaterials.IRON, "Iron Scraps", "Железный шлак"), // 2600
        scrap(ModMaterials.GOLD, "Gold Scraps", "Золотой шлак"), // 7900
        scrap(ModMaterials.REDSTONE, "Redstone Scraps", "Редстоуновый шлак"), // 1
        scrap(ModMaterials.OBSIDIAN, "Obsidian Scraps", "Обсидиановый шлак"), // 2
        scrap(ModMaterials.HEMATITE, "Hematite Scraps", "Гематитовый шлак"), // 2601, additive
        scrap(ModMaterials.WROUGHT_IRON, "Wrought Iron Scraps", "Шлак кованого железа"), // 2602
        scrap(ModMaterials.PIG_IRON, "Pig Iron Scraps", "Чугунный шлак"), // 2603
        scrap(ModMaterials.METEORITE, "Meteoric Iron Scraps", "Шлак метеоритного железа"), // 2604
        scrap(ModMaterials.MALACHITE, "Malachite Scraps", "Малахитовый шлак"), // 2901, additive
        // Radioactive
        scrap(ModMaterials.URANIUM, "Uranium Scraps", "Урановый шлак"), // 9200
        scrap(ModMaterials.URANIUM233, "Uranium-233 Scraps", "Шлак урана-233"), // 9233
        scrap(ModMaterials.URANIUM235, "Uranium-235 Scraps", "Шлак урана-235"), // 9235
        scrap(ModMaterials.URANIUM238, "Uranium-238 Scraps", "Шлак урана-238"), // 9238
        scrap(ModMaterials.THORIUM232, "Thorium-232 Scraps", "Шлак тория-232"), // 9032
        scrap(ModMaterials.PLUTONIUM, "Plutonium Scraps", "Плутониевый шлак"), // 9400
        scrap(ModMaterials.PLUTONIUM_RG, "Reactor-Grade Plutonium Scraps", "Шлак реакторного плутония"), // 9401
        scrap(ModMaterials.PLUTONIUM238, "Plutonium-238 Scraps", "Шлак плутония-238"), // 9438
        scrap(ModMaterials.PLUTONIUM239, "Plutonium-239 Scraps", "Шлак плутония-239"), // 9439
        scrap(ModMaterials.PLUTONIUM240, "Plutonium-240 Scraps", "Шлак плутония-240"), // 9440
        scrap(ModMaterials.PLUTONIUM241, "Plutonium-241 Scraps", "Шлак плутония-241"), // 9441
        scrap(ModMaterials.AMERICIUM_RG, "Reactor-Grade Americium Scraps", "Шлак реакторного америция"), // 9501
        scrap(ModMaterials.AM241, "Americium-241 Scraps", "Шлак америция-241"), // 9541
        scrap(ModMaterials.AM242, "Americium-242 Scraps", "Шлак америция-242"), // 9542
        scrap(ModMaterials.NEPTUNIUM, "Neptunium-237 Scraps", "Шлак нептуния-237"), // 9337
        scrap(ModMaterials.POLONIUM, "Polonium-210 Scraps", "Шлак полония-210"), // 8410
        scrap(ModMaterials.TECHNETIUM, "Technetium-99 Scraps", "Шлак технеция-99"), // 4399
        scrap(ModMaterials.RA226, "Radium-226 Scraps", "Шлак радия-226"), // 8826
        scrap(ModMaterials.ACTINIUM, "Actinium-227 Scraps", "Шлак актиния-227"), // 8927
        scrap(ModMaterials.CO60, "Cobalt-60 Scraps", "Шлак кобальта-60"), // 2760
        scrap(ModMaterials.AU198, "Gold-198 Scraps", "Шлак золота-198"), // 7998
        scrap(ModMaterials.PB209, "Lead-209 Scraps", "Шлак свинца-209"), // 8209
        scrap(ModMaterials.SCHRABIDIUM, "Schrabidium Scraps", "Шрабидиевый шлак"), // 12626
        scrap(ModMaterials.SOLINIUM, "Solinium Scraps", "Солиниевый шлак"), // 12627
        scrap(ModMaterials.SCHRABIDATE, "Ferric Schrabidate Scraps", "Шлак шрабидата железа"), // 12600
        scrap(ModMaterials.SCHRARANIUM, "Schraranium Scraps", "Шрараниевый шлак"), // 12601
        scrap(ModMaterials.GHIORSIUM, "Ghiorsium-336 Scraps", "Шлак гиорсия-336"), // 12836
        // Base metals
        scrap(ModMaterials.TITANIUM, "Titanium Scraps", "Титановый шлак"), // 2200
        scrap(ModMaterials.COPPER, "Copper Scraps", "Медный шлак"), // 2900
        scrap(ModMaterials.TUNGSTEN, "Tungsten Scraps", "Вольфрамовый шлак"), // 7400
        scrap(ModMaterials.ALUMINIUM, "Aluminium Scraps", "Алюминиевый шлак"), // 1300
        scrap(ModMaterials.LEAD, "Lead Scraps", "Свинцовый шлак"), // 8200
        scrap(ModMaterials.BISMUTH, "Bismuth Scraps", "Висмутовый шлак"), // 8300
        scrap(ModMaterials.ARSENIC, "Arsenic Scraps", "Мышьяковый шлак"), // 3300
        scrap(ModMaterials.TANTALIUM, "Tantalum Scraps", "Танталовый шлак"), // 7300
        scrap(ModMaterials.NEODYMIUM, "Neodymium Scraps", "Неодимовый шлак"), // 6000
        scrap(ModMaterials.NIOBIUM, "Niobium Scraps", "Ниобиевый шлак"), // 4100
        scrap(ModMaterials.BERYLLIUM, "Beryllium Scraps", "Бериллиевый шлак"), // 400
        scrap(ModMaterials.COBALT, "Cobalt Scraps", "Кобальтовый шлак"), // 2700
        scrap(ModMaterials.BORON, "Boron Scraps", "Борный шлак"), // 500
        scrap(ModMaterials.BORAX, "Borax Scraps", "Шлак буры"), // 501
        scrap(ModMaterials.LANTHANIUM, "Lanthanium Scraps", "Лантановый шлак"), // 5700
        scrap(ModMaterials.ZIRCONIUM, "Zirconium Scraps", "Циркониевый шлак"), // 4000
        scrap(ModMaterials.SODIUM, "Sodium Scraps", "Натриевый шлак"), // 1100
        scrap(ModMaterials.STRONTIUM, "Strontium Scraps", "Стронциевый шлак"), // 3800
        scrap(ModMaterials.CALCIUM, "Calcium Scraps", "Кальциевый шлак"), // 2000
        scrap(ModMaterials.LITHIUM, "Lithium Scraps", "Литиевый шлак"), // 300
        scrap(ModMaterials.CADMIUM, "Cadmium Scraps", "Кадмиевый шлак"), // 4800
        scrap(ModMaterials.SILICON, "Silicon Scraps", "Кремниевый шлак"), // 1400
        scrap(ModMaterials.ASBESTOS, "Asbestos Scraps", "Асбестовый шлак"), // 1401
        scrap(ModMaterials.OSMIRIDIUM, "Osmiridium Scraps", "Осмиридиевый шлак"), // 7699
        // Alloys (порядок объявления в Mats.java)
        scrap(ModMaterials.STEEL, "Steel Scraps", "Стальной шлак"), // 30
        scrap(ModMaterials.RED_COPPER, "Minecraft Grade Copper Scraps", "Красномедный шлак"), // 31, ориг. MAT_MINGRADE
        scrap(ModMaterials.DURA_STEEL, "High-Speed Steel Scraps", "Шлак быстрорежущей стали"), // 33
        scrap(ModMaterials.DESH, "Desh Scraps", "Шлак деша"), // 42
        scrap(ModMaterials.STAR_METAL, "Starmetal Scraps", "Шлак звёздного металла"), // 35
        scrap(ModMaterials.FERROURANIUM, "Ferrouranium Scraps", "Ферроурановый шлак"), // 37
        scrap(ModMaterials.TCALLOY, "Technetium Steel Scraps", "Шлак технециевой стали"), // 36
        scrap(ModMaterials.CDALLOY, "Cadmium Steel Scraps", "Шлак кадмиевой стали"), // 43
        scrap(ModMaterials.BBRONZE, "Bismuth Bronze Scraps", "Шлак висмутовой бронзы"), // 46
        scrap(ModMaterials.ABRONZE, "Arsenic Bronze Scraps", "Шлак мышьяковой бронзы"), // 47
        scrap(ModMaterials.BSCCO, "BSCCO Scraps", "Шлак BSCCO"), // 48
        scrap(ModMaterials.MAGNETIZED_TUNGSTEN, "Magnetized Tungsten Scraps", "Шлак намагниченного вольфрама"), // 38
        scrap(ModMaterials.COMBINE_STEEL, "Combine Steel Scraps", "Шлак стали Альянса"), // 39
        scrap(ModMaterials.DNT, "Dineutronium Scraps", "Динейтрониевый шлак"), // 45, DictFrame DNT = "Dineutronium"
        scrap(ModMaterials.FLUX, "Flux Scraps", "Флюсовый шлак"), // 40, additive
        scrap(ModMaterials.SLAG, "Slag Scraps", "Кусочки шлака"), // 41
        scrap(ModMaterials.MUD, "Mud Scraps", "Грязевой шлак"), // 44
        scrap(ModMaterials.GUNMETAL, "Gunmetal Scraps", "Шлак пушечной бронзы"), // 49
        scrap(ModMaterials.WEAPONSTEEL, "Weapon Steel Scraps", "Шлак оружейной стали"), // 50
        scrap(ModMaterials.SATURNITE, "Saturnite Scraps", "Сатурнитовый шлак") // 34
    );

    /** mat -> предмет scraps_<id>. */
    private static final Map<ModMaterials, RegistrySupplier<Item>> SCRAP_ITEMS = new EnumMap<>(ModMaterials.class);

    /** Таблица отходов только для чтения (датаген локализаций/моделей). */
    public static List<ScrapEntry> scrapEntries() {
        return FOUNDRY_SCRAPS;
    }

    /** Предмет отходов материала или null. */
    public static Item scrapItem(ModMaterials mat) {
        RegistrySupplier<Item> sup = SCRAP_ITEMS.get(mat);
        return sup != null && sup.isPresent() ? sup.get() : null;
    }

    /**
     * Редкость (цвет имени) 1:1 из оригинала: в 1.7.10 соответствующие предметы объявлялись
     * как {@code new ItemCustomLore().setRarity(EnumRarity.xxx)} в ModItems (partsTab).
     * vanilla EnumRarity 1.7.10 == vanilla Rarity 1.20.1/1.21.1 (common/uncommon/rare/epic).
     */
    private static final Map<ModMaterials, Map<MaterialShape, Rarity>> RARITIES = buildRarities();

    private static Map<ModMaterials, Map<MaterialShape, Rarity>> buildRarities() {
        Map<ModMaterials, Map<MaterialShape, Rarity>> map = new EnumMap<>(ModMaterials.class);
        addRarity(map, Rarity.EPIC,
                ModMaterials.EUPHEMIUM, MaterialShape.INGOT, MaterialShape.NUGGET, MaterialShape.PLATE, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.GH336, MaterialShape.BILLET, MaterialShape.NUGGET);
        addRarity(map, Rarity.RARE, ModMaterials.SCHRABIDIUM,
                MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET, MaterialShape.CRYSTAL, MaterialShape.PLATE, MaterialShape.POWDER);
        addRarity(map, Rarity.RARE, ModMaterials.SCHRABIDATE, MaterialShape.INGOT, MaterialShape.POWDER);
        addRarity(map, Rarity.RARE, ModMaterials.SATURNITE, MaterialShape.INGOT, MaterialShape.PLATE);
        addRarity(map, Rarity.RARE, ModMaterials.SCHRARANIUM, MaterialShape.CRYSTAL);
        addRarity(map, Rarity.RARE, ModMaterials.OSMIRIDIUM, MaterialShape.INGOT, MaterialShape.NUGGET);
        addRarity(map, Rarity.UNCOMMON, ModMaterials.AUSTRALIUM,
                MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET, MaterialShape.POWDER);
        addRarity(map, Rarity.UNCOMMON, ModMaterials.AUSTRALIUM_LESSER, MaterialShape.BILLET, MaterialShape.NUGGET);
        addRarity(map, Rarity.UNCOMMON, ModMaterials.AUSTRALIUM_GREATER, MaterialShape.BILLET, MaterialShape.NUGGET);
        addRarity(map, Rarity.UNCOMMON, ModMaterials.BALEFIRE_GOLD, MaterialShape.BILLET);
        addRarity(map, Rarity.UNCOMMON, ModMaterials.FLASHLEAD, MaterialShape.BILLET);
        addRarity(map, Rarity.UNCOMMON, ModMaterials.THORIUM, MaterialShape.POWDER);
        // Epic-порошки оригинала (powder_iodine, powder_neodymium, ... — все ItemCustomLore с epic).
        addRarity(map, Rarity.EPIC, ModMaterials.IODINE, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.NEODYMIUM, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.ASTATINE, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.CAESIUM, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.STRONTIUM, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.COBALT, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.BROMINE, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.NIOBIUM, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.TENNESSINE, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.CERIUM, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.LANTHANIUM, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.ACTINIUM, MaterialShape.POWDER);
        addRarity(map, Rarity.EPIC, ModMaterials.BORON, MaterialShape.POWDER);
        return map;
    }

    private static void addRarity(Map<ModMaterials, Map<MaterialShape, Rarity>> map, Rarity rarity,
                                  ModMaterials mat, MaterialShape... shapes) {
        EnumMap<MaterialShape, Rarity> byShape = new EnumMap<>(MaterialShape.class);
        for (MaterialShape shape : shapes) byShape.put(shape, rarity);
        map.put(mat, byShape);
    }

    private static Item createItem(ModMaterials mat, MaterialShape shape) {
        Rarity rarity = null;
        Map<MaterialShape, Rarity> byShape = RARITIES.get(mat);
        if (byShape != null) rarity = byShape.get(shape);
        Item.Properties props = rarity != null ? new Item.Properties().rarity(rarity) : new Item.Properties();
        // Исторические спецслучаи: урановый слиток и железный порошок радиоактивны.
        if (mat == ModMaterials.URANIUM && shape == MaterialShape.INGOT) {
            return new RadioactiveItem(props);
        }
        if (mat == ModMaterials.IRON && shape == MaterialShape.POWDER) {
            return new RadioactiveItem(props);
        }
        // Лор-строки оригинала (desc в 1.7.10) для материалов.
        List<Component> lore = materialLore(mat, shape);
        if (lore != null) {
            return new LoreTooltipItem(lore, props);
        }
        return new Item(props);
    }

    /** Лор-тултипы 1:1 с {@code item.<id>.desc} оригинального en_us.lang. */
    private static List<Component> materialLore(ModMaterials mat, MaterialShape shape) {
        switch (mat) {
            case NEPTUNIUM: if (shape == MaterialShape.INGOT)
                return lore("neptunium_ingot", "That one's my favourite!");
                break;
            case TANTALIUM: if (shape == MaterialShape.INGOT || shape == MaterialShape.POWDER)
                return lore("tantalum", "'Tantalum'");
                break;
            case LANTHANIUM: if (shape == MaterialShape.INGOT)
                return lore("lanthanium_ingot", "'Lanthanum'");
                break;
            case COMBINE_STEEL: if (shape == MaterialShape.INGOT)
                return lore("combine_steel_ingot", "*insert Civil Protection reference here*");
                break;
            case GH336: if (shape == MaterialShape.BILLET || shape == MaterialShape.NUGGET)
                return lore("gh336", "Seaborgium's colleague.");
                break;
            case FIBERGLASS: if (shape == MaterialShape.INGOT)
                return lore("fiberglass_ingot", "High in fiber, high in glass. Everything the body needs.");
                break;
            case EUPHEMIUM:
                if (shape == MaterialShape.INGOT) return lore("euphemium_ingot", "A very special and yet strange element.");
                if (shape == MaterialShape.POWDER) return lore("euphemium_powder", "Pulverized pink.", "Tastes like strawberries.");
                if (shape == MaterialShape.NUGGET) return lore("euphemium_nugget",
                        "A small piece of a pink metal.", "Its properties are still unknown,", "DEAL WITH IT CAREFULLY.");
                break;
            case SEMTEX: if (shape == MaterialShape.INGOT)
                return lore("semtex_ingot", "Semtex H Plastic Explosive", "Performant explosive for many applications.", "Edible");
                break;
            case FLASHLEAD: if (shape == MaterialShape.BILLET)
                return lore("flashlead_billet",
                        "The lattice decays, causing antimatter-matter annihilation reactions,",
                        "causing the release of pions, decaying into muons,",
                        "catalyzing fusion of the nuclei, creating the new element.",
                        "Please try to keep up.");
                break;
            case ASBESTOS:
                if (shape == MaterialShape.INGOT) return loreItalic("asbestos_ingot",
                        "\"Filled with life, self-doubt and asbestos. That comes with the air.\"");
                if (shape == MaterialShape.POWDER) return loreItalic("asbestos_powder",
                        "\"Sniffffffff- MHHHHHHMHHHHHHHHH\"");
                break;
            case CHARRED: if (shape == MaterialShape.CRYSTAL)
                return lore("crystal_charred", "High quality silicate, slightly burned.");
                break;
            case HORN: if (shape == MaterialShape.CRYSTAL)
                return lore("crystal_horn", "Not an actual horn.");
                break;
            default:
        }
        return null;
    }

    private static List<Component> lore(String key, String... lines) {
        List<Component> out = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            out.add(Component.translatable("tooltip.hbm_m." + key + ".desc" + (i + 1))
                    .withStyle(ChatFormatting.GRAY));
        }
        return out;
    }

    private static List<Component> loreItalic(String key, String... lines) {
        List<Component> out = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            out.add(Component.translatable("tooltip.hbm_m." + key + ".desc" + (i + 1))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        return out;
    }

    public static boolean has(ModMaterials mat, MaterialShape shape) {
        EnumMap<MaterialShape, RegistrySupplier<Item>> map = ITEMS_BY_MATERIAL.get(mat);
        return map != null && map.containsKey(shape);
    }

    public static RegistrySupplier<Item> get(ModMaterials mat, MaterialShape shape) {
        EnumMap<MaterialShape, RegistrySupplier<Item>> map = ITEMS_BY_MATERIAL.get(mat);
        return map == null ? null : map.get(shape);
    }

    /**
     * Развёрнутый Item или null, если форма не зарегистрирована ИЛИ реестр ещё
     * не наполнен (вызов в static-init до прохождения регистрации не должен падать).
     */
    public static Item item(ModMaterials mat, MaterialShape shape) {
        RegistrySupplier<Item> supplier = get(mat, shape);
        if (supplier == null || !supplier.isPresent()) return null;
        return supplier.get();
    }

    /** ItemStack из count предметов; null, если форма не зарегистрирована. */
    public static net.minecraft.world.item.ItemStack stack(ModMaterials mat, MaterialShape shape, int count) {
        Item item = item(mat, shape);
        return item == null ? null : new net.minecraft.world.item.ItemStack(item, count);
    }

    /** Все (материал, предмет) данной формы — для перебора в креатив-табах и датагене. */
    public static java.util.List<Map.Entry<ModMaterials, RegistrySupplier<Item>>> allOf(MaterialShape shape) {
        java.util.List<Map.Entry<ModMaterials, RegistrySupplier<Item>>> out = new java.util.ArrayList<>();
        for (ModMaterials mat : ModMaterials.values()) {
            RegistrySupplier<Item> supplier = get(mat, shape);
            if (supplier != null) out.add(Map.entry(mat, supplier));
        }
        return out;
    }

    /** Карта mat -> (shape -> supplier) только для чтения (например, датагеном). */
    public static Map<ModMaterials, EnumMap<MaterialShape, RegistrySupplier<Item>>> itemsByMaterial() {
        return java.util.Collections.unmodifiableMap(ITEMS_BY_MATERIAL);
    }
}
