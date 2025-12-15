package com.hbm_m.item.tags_and_tiers;

// Перечисление всех слитков в моде с поддержкой многоязычности.
// Для каждого слитка можно задать переводы на разные языки.

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum ModIngots {
    // Конструктор принимает название и пары "язык, перевод"
    URANIUM("uranium",
            "ru_ru", "Урановый слиток",
            "en_us", "Uranium Ingot"),
    URANIUM233("u233",
            "ru_ru", "Слиток урана-233",
            "en_us", "Uranium-233 Ingot"),
    URANIUM235("u235",
            "ru_ru", "Слиток урана-235",
            "en_us", "Uranium-235 Ingot"),
    URANIUM238("u238",
            "ru_ru", "Слиток урана-238",
            "en_us", "Uranium-238 Ingot"),
    THORIUM232("th232",
            "ru_ru", "Слиток тория-232",
            "en_us", "Thorium-232 Ingot"),
    PLUTONIUM("plutonium",
            "ru_ru", "Плутониевый слиток",
            "en_us", "Plutonium Ingot"),
    PLUTONIUM238("pu238",
            "ru_ru", "Слиток плутония-238",
            "en_us", "Plutonium-238 Ingot"),
    PLUTONIUM239("pu239",
            "ru_ru", "Слиток плутония-239",
            "en_us", "Plutonium-239 Ingot"),
    PLUTONIUM240("pu240",
            "ru_ru", "Слиток плутония-240",
            "en_us", "Plutonium-240 Ingot"),
    PLUTONIUM241("pu241",
            "ru_ru", "Слиток плутония-241",
            "en_us", "Plutonium-241 Ingot"),
    ACTINIUM("actinium",
            "ru_ru", "Слиток актиния-227",
            "en_us", "Actinium Ingot"),
    STEEL("steel",
            "ru_ru", "Стальной слиток",
            "en_us", "Steel Ingot"),
    ADVANCED_ALLOY("advanced_alloy",
            "ru_ru", "Слиток продвинутого сплава",
            "en_us", "Advanced Alloy Ingot"),
    ALUMINUM("aluminum",
            "ru_ru", "Слиток алюминия",
            "en_us", "Aluminum Ingot"),
    SCHRABIDIUM("schrabidium",
            "ru_ru", "Шрабидиевый слиток",
            "en_us", "Schrabidium Ingot"),
    SATURNITE("saturnite",
            "ru_ru", "Сатурнитовый слиток",
            "en_us", "Saturnite Ingot"),
    LEAD("lead",
            "ru_ru", "Свинцовый слиток",
            "en_us", "Lead Ingot"),
    GUNMETAL("gunmetal",
            "ru_ru", "Cлиток пушечной бронзы",
            "en_us", "Gunmetal Ingot"),
    GUNSTEEL("gunsteel",
            "ru_ru", "Слиток оружейной стали",
            "en_us", "Gunsteel Ingot"),
    RED_COPPER("red_copper",
            "ru_ru", "Слиток красной меди",
            "en_us", "Red Copper Ingot"),
    ASBESTOS("asbestos",
            "ru_ru", "Асбестовый лист",
            "en_us", "Asbestos"),
    TITANIUM("titanium",
            "ru_ru", "Титановый слиток",
            "en_us", "Titanium Ingot"),
    COBALT("cobalt",
            "ru_ru", "Кобальтовый слиток",
            "en_us", "Cobalt Ingot"),
    TUNGSTEN("tungsten",
            "ru_ru", "Вольфрамовый слиток",
            "en_us", "Tungsten Ingot"),
    STARMETAL("starmetal",
            "ru_ru", "Слиток Звёздного Металла",
            "en_us", "Star Metal Ingot"),
    BERYLLIUM("beryllium",
            "ru_ru", "Бериллиевый слиток",
            "en_us", "Beryllium Ingot"),

    BISMUTH("bismuth",
            "ru_ru", "Висмутовый слиток",
            "en_us", "Bismuth Ingot"),

    POLYMER("polymer",
        "ru_ru", "Полимер",
        "en_us", "Polymer Ingot"),

    BAKELITE("bakelite",
        "ru_ru", "Бакелит",
        "en_us", "Bakelite Ingot"),

    RUBBER("rubber",
        "ru_ru", "Резина",
        "en_us", "Rubber Ingot"),

    DESH("desh",
        "ru_ru", "Слиток Деш",
        "en_us", "Desh Ingot"),

    GRAPHITE("graphite",
        "ru_ru", "Графит",
        "en_us", "Graphite Ingot"),

    PHOSPHORUS("phosphorus",
        "ru_ru", "Белый фосфор",
        "en_us", "White Phosphorus Ingot"),

    LITHIUM_INGOT("les",
        "ru_ru", "Куб лития",
        "en_us", "Lithium Cube"),

    MAGNETIZED_TUNGSTEN("magnetized_tungsten",
        "ru_ru", "Намагниченный вольфрам",
        "en_us", "Magnetized Tungsten"),

    COMBINE_STEEL("combine_steel",
        "ru_ru", "Слиток Стали Альянса",
        "en_us", "Alliance Steel Ingot"),

    DURA_STEEL("dura_steel",
        "ru_ru", "Слиток высокоскоростной стали",
        "en_us", "Dura Steel Ingot"),

    POLYMER_COMPOSITE("pc",
        "ru_ru", "Твёрдый пластиковый брусок",
        "en_us", "Polymer Composite"),

    DIGAMMA("digamma",
        "ru_ru", "Слиток Дигаммы",
        "en_us", "Digamma Ingot"),

    EUPHEMIUM("euphemium",
        "ru_ru", "Эвфемиевый слиток",
        "en_us", "Euphemium Ingot"),

    DINEUTRONIUM("dineutronium",
        "ru_ru", "Динейтрониевый слиток",
        "en_us", "Dineutronium Ingot"),

    ELECTRONIUM("electronium",
        "ru_ru", "Электрониевый слиток",
        "en_us", "Electronium Ingot"),

    AUSTRALIUM("australium",
        "ru_ru", "Австралиевый слиток",
        "en_us", "Australium Ingot"),

    SOLINIUM("solinium",
        "ru_ru", "Солиниевый слиток",
        "en_us", "Solinium Ingot"),

    TANTALIUM("tantalium",
        "ru_ru", "Танталовый слиток",
        "en_us", "Tantalium Ingot"),

    CHAINSSTEEL("chainsteel",
        "ru_ru", "Цепная сталь",
        "en_us", "Chain Steel Ingot"),

    METEORITE("meteorite",
        "ru_ru", "Метеоритный слиток",
        "en_us", "Meteorite Ingot"),

    LANTHANIUM("lanthanium",
        "ru_ru", "Лантановый слиток",
        "en_us", "Lanthanium Ingot"),

    NEODYMIUM("neodymium",
        "ru_ru", "Неодимовый слиток",
        "en_us", "Neodymium Ingot"),

    NIOBIUM("niobium",
        "ru_ru", "Ниобиевый слиток",
        "en_us", "Niobium Ingot"),

    CERIUM("cerium",
        "ru_ru", "Цериевый слиток",
        "en_us", "Cerium Ingot"),

    CADMIUM("cadmium",
        "ru_ru", "Кадмиевый слиток",
        "en_us", "Cadmium Ingot"),

    CAESIUM("caesium",
        "ru_ru", "Цезиевый слиток",
        "en_us", "Caesium Ingot"),

    STRONTIUM("strontium",
        "ru_ru", "Стронциевый слиток",
        "en_us", "Strontium Ingot"),

    BROMINE("bromide",
        "ru_ru", "Слиток бромида",
        "en_us", "Bromide Ingot"),

    TENNESSINE("tennessine",
        "ru_ru", "Теннессиевый слиток",
        "en_us", "Tennessine Ingot"),

    ZIRCONIUM("zirconium",
        "ru_ru", "Цирконивый куб",
        "en_us", "Zirconium cube"),

    ARSENIC("arsenic",
        "ru_ru", "Мышьяковый слиток",
        "en_us", "Arsenic Ingot"),

    IODINE("iodine",
        "ru_ru", "Йодный слиток",
        "en_us", "Iodine Ingot"),

    ASTATINE("astatine",
        "ru_ru", "Астатовый слиток",
        "en_us", "Astatine Ingot"),

    AMERICIUM("americium",
        "ru_ru", "Америциевый слиток",
        "en_us", "Americium Ingot"),

    NEPTUNIUM("neptunium",
        "ru_ru", "Нептуниевый слиток",
        "en_us", "Neptunium Ingot"),

    POLONIUM("polonium",
        "ru_ru", "Полониевый слиток",
        "en_us", "Polonium Ingot"),

    TECHNETIUM("technetium",
        "ru_ru", "Технециевый слиток",
        "en_us", "Technetium Ingot"),

    BORON("boron",
        "ru_ru", "Борный слиток",
        "en_us", "Boron Ingot"),

    SCHRABIDATE("schrabidate",
        "ru_ru", "Шрабидат",
        "en_us", "Schrabidate"),

    SCHRARANIUM("schraranium",
        "ru_ru", "Шрараниевый слиток",
        "en_us", "Schraranium Ingot"),

    AU198("au198",
        "ru_ru", "Золото-198",
        "en_us", "Gold-198"),

    PB209("pb209",
        "ru_ru", "Свинец-209",
        "en_us", "Lead-209"),

    RA226("ra226",
        "ru_ru", "Радий-226",
        "en_us", "Radium-226"),

    THORIUM("thorium",
        "ru_ru", "Ториевый слиток",
        "en_us", "Thorium Ingot"),

    OSMIRIDIUM("osmiridium",
        "ru_ru", "Осмиридиевый слиток",
        "en_us", "Osmiridium Ingot"),

    SELENIUM("selenium",
        "ru_ru", "Селениевый слиток",
        "en_us", "Selenium Ingot"),

    CO60("co60",
        "ru_ru", "Кобальт-60",
        "en_us", "Cobalt-60"),

    SR90("sr90",
        "ru_ru", "Стронций-90",
        "en_us", "Strontium-90"),

    AM241("am241",
        "ru_ru", "Америций-241",
        "en_us", "Americium-241"),

    AM242("am242",
        "ru_ru", "Америций-242",
        "en_us", "Americium-242"),

    STEEL_DUSTED("steel_dusted",
        "ru_ru", "Пыльная сталь",
        "en_us", "Dusted Steel"),

    CALCIUM("calcium",
        "ru_ru", "Кальциевый слиток",
        "en_us", "Calcium Ingot"),

    GRAPHENE("graphene",
        "ru_ru", "Графеновый лист",
        "en_us", "Graphene Sheet"),

    MOX_FUEL("mox_fuel",
        "ru_ru", "MOX топливо",
        "en_us", "MOX Fuel"),

    SMORE("smore",
        "ru_ru", "Слиток S'more",
        "en_us", "S'more Ingot"),

    SCHRABIDIUM_FUEL("schrabidium_fuel",
        "ru_ru", "Шрабидиевое топливо",
        "en_us", "Schrabidium Fuel"),

    URANIUM_FUEL("uranium_fuel",
        "ru_ru", "Урановое топливо",
        "en_us", "Uranium Fuel"),

    THORIUM_FUEL("thorium_fuel",
        "ru_ru", "Ториевое топливо",
        "en_us", "Thorium Fuel"),

    PLUTONIUM_FUEL("plutonium_fuel",
        "ru_ru", "Плутониевое топливо",
        "en_us", "Plutonium Fuel"),

    NEPTUNIUM_FUEL("neptunium_fuel",
        "ru_ru", "Нептуниевое топливо",
        "en_us", "Neptunium Fuel"),

    AMERICIUM_FUEL("americium_fuel",
        "ru_ru", "Америциевое топливо",
        "en_us", "Americium Fuel"),

    BISMUTH_BRONZE("bismuth_bronze",
        "ru_ru", "Висмутовая бронза",
        "en_us", "Bismuth Bronze"),

    ARSENIC_BRONZE("arsenic_bronze",
        "ru_ru", "Мышьяковая бронза",
        "en_us", "Arsenic Bronze"),

    CRYSTALLINE("crystalline",
        "ru_ru", "Кристаллиниевый слиток",
        "en_us", "Crystalline Ingot"),

    MUD("mud",
        "ru_ru", "Грязь",
        "en_us", "Mud"),

    SILICON("silicon",
        "ru_ru", "Кремниевый брусок",
        "en_us", "Silicon Ingot"),

    FIBERGLASS("fiberglass",
        "ru_ru", "Стекловолокно",
        "en_us", "Fiberglass"),

    CERAMIC("ceramic",
        "ru_ru", "Керамический слиток",
        "en_us", "Ceramic Ingot"),

    PU_MIX("pu_mix",
        "ru_ru", "Плутониевая смесь",
        "en_us", "Plutonium Mix"),

    AM_MIX("am_mix",
        "ru_ru", "Америциевая смесь",
        "en_us", "Americium Mix"),

    PET("pet",
        "ru_ru", "ПЭТ слиток",
        "en_us", "PET Ingot"),

    FERROURANIUM("ferrouranium",
        "ru_ru", "Ферроурановый слиток",
        "en_us", "Ferrouranium Ingot"),

    PVC("pvc",
        "ru_ru", "ПВХ",
        "en_us", "PVC"),

    BIORUBBER("biorubber",
        "ru_ru", "Латекс",
        "en_us", "Latex"),

    SEMTEX("semtex",
        "ru_ru", "Семтекс",
        "en_us", "Semtex"),

    C4("c4",
        "ru_ru", "C4",
        "en_us", "C4"),

    STABALLOY("staballoy",
        "ru_ru", "Стабаллой",
        "en_us", "Staballoy"),

    METAL_SCRAP("metal.scrap",
            "ru_ru", "Металлолом",
            "en_us", "Metal Scrap"),

    METEORITE_FORGED("meteorite_forged",
            "ru_ru", "Кованый метеоритный слиток",
            "en_us", "Forged Meteorite Ingot"),

    METEORITE_FORGED_HOT("meteorite_forged_hot",
            "ru_ru", "Горячий кованый метеоритный слиток",
            "en_us", "Hot Forged Meteorite Ingot"),

    METEORITE_HOT("meteorite_hot",
            "ru_ru", "Горячий метеоритный слиток",
            "en_us", "Hot Meteorite Ingot"),

    IRON_SMALL("iron_small",
            "ru_ru", "Маленький железный слиток",
            "en_us", "Small Iron Ingot"),

    CHAINSTEEL_HOT("chainsteel_hot",
            "ru_ru", "Горячая цепная сталь",
            "en_us", "Hot Chain Steel"),

    STARMETAL_ORION_BASE("starmetal_orion_base",
            "ru_ru", "Базовый звёздный металл Орион",
            "en_us", "Star Metal Orion Base"),

    STARMETAL_ORION("starmetal_orion",
            "ru_ru", "Звёздный металл Орион",
            "en_us", "Star Metal Orion"),

    STARMETAL_URSA_BASE("starmetal_ursa_base",
            "ru_ru", "Базовый звёздный металл Урса",
            "en_us", "Star Metal Ursa Base"),

    STARMETAL_URSA("starmetal_ursa",
            "ru_ru", "Звёздный металл Урса",
            "en_us", "Star Metal Ursa"),

    STARMETAL_ASTRA_BASE("starmetal_astra_base",
            "ru_ru", "Базовый звёздный металл Астра",
            "en_us", "Star Metal Astra Base"),

    STARMETAL_ASTRA("starmetal_astra",
            "ru_ru", "Звёздный металл Астра",
            "en_us", "Star Metal Astra"),

    DESH_RAINBOW("desh_rainbow",
            "ru_ru", "Ненавижу пони.",
            "en_us", "Rainbow Desh"),

    TCALLOY("tcalloy",
            "ru_ru", "Слиток TCaloy",
            "en_us", "TCaloy Ingot"),

    U238M2("u238m2",
            "ru_ru", "Слиток урана-238M2",
            "en_us", "Uranium-238M2 Ingot"),

    CELNEUTRONOPHYRIUM_NEW("celneutronophyrium_new",
            "ru_ru", "Новый цельнейтронофириевый слиток",
            "en_us", "New Celneutronophyrium Ingot"),

    CELNEUTRONOPHYRIUM("celneutronophyrium",
            "ru_ru", "Цельнейтронофириевый слиток",
            "en_us", "Celneutronophyrium Ingot"),

    TETRANEUTRONIUM_NEW("tetraneutronium_new",
            "ru_ru", "Новый тетранейтрониевый слиток",
            "en_us", "New Tetraneutronium Ingot"),

    TETRANEUTRONIUM("tetraneutronium",
            "ru_ru", "Тетранейтрониевый слиток",
            "en_us", "Tetraneutronium Ingot"),

    CFT("cft",
            "ru_ru", "Слиток CFT",
            "en_us", "CFT Ingot"),

    CDALLOY("cdalloy",
            "ru_ru", "Слиток CDalloy",
            "en_us", "CDalloy Ingot"),

    BSCCO("bscco",
            "ru_ru", "Слиток BSCCO",
            "en_us", "BSCCO Ingot"),

    BESKAR_DOUBLE("beskar_double",
            "ru_ru", "Двойной бескаровый слиток",
            "en_us", "Double Beskar Ingot"),

    BESKAR("beskar",
            "ru_ru", "Бескаровый слиток",
            "en_us", "Beskar Ingot");

            
    // Чтобы добавить новый слиток, просто добавьте новую запись с его переводами

    private final String name;
    private final Map<String, String> translations;
    private static final Map<String, ModIngots> BY_NAME = new HashMap<>();

    /**
     * Конструктор для слитков с поддержкой нескольких языков.
     * @param name Системное имя (например, "uranium")
     * @param translationPairs Пары строк: "код_языка", "перевод". Например: "ru_ru", "Слиток", "en_us", "Ingot"
     */
    ModIngots(String name, String... translationPairs) {
        this.name = name;
        
        // Проверка, что количество элементов четное (пары ключ-значение)
        if (translationPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Translation pairs must be even for ingot: " + name);
        }

        Map<String, String> translationMap = new HashMap<>();
        for (int i = 0; i < translationPairs.length; i += 2) {
            String locale = translationPairs[i];
            String translation = translationPairs[i + 1];
            translationMap.put(locale, translation);
        }
        // Делаем карту неизменяемой после создания
        this.translations = Collections.unmodifiableMap(translationMap);
    }

    static {
        for (ModIngots ingot : values()) {
            BY_NAME.put(ingot.name, ingot);
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Получает перевод для указанного языка.
     * @param locale Код языка (например, "ru_ru").
     * @return Перевод или null, если он не найден.
     */
    public String getTranslation(String locale) {
        return translations.get(locale);
    }

    public static Optional<ModIngots> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }
}