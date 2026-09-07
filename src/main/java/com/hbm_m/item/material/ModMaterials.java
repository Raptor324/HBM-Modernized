package com.hbm_m.item.material;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Единый реестр материалов (порт идеи NTMMaterial из 1.7.10).
 * Материал объявляет набор форм {@link MaterialShape}; предметы, блоки, lang,
 * модели и рецепты генерируются циклами по этому enum.
 * Сгенерировано tools/material_registry/gen_modmaterials.py; переводы из ModIngots/ModPowders.
 * id материалов совпадают с историческими идентификаторами регистрации (совместимость сейвов).
 */
public enum ModMaterials {
    URANIUM("uranium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.CRYSTAL, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Урановый слиток", "en_us", "Uranium Ingot"),
    URANIUM233("u233", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток урана-233", "en_us", "Uranium-233 Ingot"),
    URANIUM235("u235", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток урана-235", "en_us", "Uranium-235 Ingot"),
    URANIUM238("u238", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток урана-238", "en_us", "Uranium-238 Ingot"),
    THORIUM232("th232", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток тория-232", "en_us", "Thorium-232 Ingot"),
    PLUTONIUM("plutonium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.CRYSTAL, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Плутониевый слиток", "en_us", "Plutonium Ingot"),
    PLUTONIUM238("pu238", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток плутония-238", "en_us", "Plutonium-238 Ingot"),
    PLUTONIUM239("pu239", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток плутония-239", "en_us", "Plutonium-239 Ingot"),
    PLUTONIUM240("pu240", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток плутония-240", "en_us", "Plutonium-240 Ingot"),
    PLUTONIUM241("pu241", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток плутония-241", "en_us", "Plutonium-241 Ingot"),
    ACTINIUM("actinium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.POWDER, MaterialShape.POWDER_TINY), "ru_ru", "Слиток актиния-227", "en_us", "Actinium-227 Ingot"),
    STEEL("steel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED, MaterialShape.POWDER, MaterialShape.POWDER_TINY, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Стальной слиток", "en_us", "Steel Ingot"),
    ADVANCED_ALLOY("advanced_alloy", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.PLATE, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Слиток продвинутого сплава", "en_us", "Advanced Alloy Ingot"),
    ALUMINUM("aluminum", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.PLATE, MaterialShape.POWDER), "ru_ru", "Слиток алюминия", "en_us", "Aluminium Ingot"),
    SCHRABIDIUM("schrabidium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.CRYSTAL, MaterialShape.NUGGET, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Шрабидиевый слиток", "en_us", "Schrabidium Ingot"),
    SATURNITE("saturnite", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Сатурнитовый слиток", "en_us", "Saturnite Ingot"),
    LEAD("lead", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.CRYSTAL, MaterialShape.NUGGET, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Свинцовый слиток", "en_us", "Lead Ingot"),
    GUNMETAL("gunmetal", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.PLATE), "ru_ru", "Слиток пушечной бронзы", "en_us", "Gunmetal Ingot"),
    GUNSTEEL("gunsteel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.PLATE), "ru_ru", "Слиток оружейной стали", "en_us", "Gunsteel Ingot"),
    WEAPONSTEEL("weaponsteel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.PLATE, MaterialShape.PLATE_CAST), "ru_ru", "Слиток боевой стали", "en_us", "Weapon Steel Ingot"),
    // RED_COPPER = ориг. MAT_MINGRADE ("Minecraft Grade Copper"): провода — автоген WIRE + DENSEWIRE.
    RED_COPPER("red_copper", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Слиток красной меди", "en_us", "Minecraft Grade Copper Ingot"),
    ASBESTOS("asbestos", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Асбестовый лист", "en_us", "Asbestos Sheet"),
    TITANIUM("titanium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.CRYSTAL, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Титановый слиток", "en_us", "Titanium Ingot"),
    COBALT("cobalt", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.CRYSTAL, MaterialShape.NUGGET, MaterialShape.POWDER, MaterialShape.POWDER_TINY), "ru_ru", "Кобальтовый слиток", "en_us", "Cobalt Ingot"),
    TUNGSTEN("tungsten", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.CRYSTAL, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Вольфрамовый слиток", "en_us", "Tungsten Ingot"),
    STARMETAL("starmetal", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.CRYSTAL), "ru_ru", "§9Звёздный металл§r", "en_us", "§9Starmetal Ingot§r"),
    BERYLLIUM("beryllium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.CRYSTAL, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Бериллиевый слиток", "en_us", "Beryllium Ingot"),
    BISMUTH("bismuth", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.POWDER), "ru_ru", "Слиток висмута", "en_us", "Bismuth Ingot"),
    POLYMER("polymer", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.PLATE, MaterialShape.POWDER), "ru_ru", "Брусок полимера", "en_us", "Polymer Bar"),
    BAKELITE("bakelite", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Брусок бакелита", "en_us", "Bakelite Bar"),
    RUBBER("rubber", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Брусок резины", "en_us", "Rubber Bar"),
    DESH("desh", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.POWDER), "ru_ru", "Деш-слиток", "en_us", "Desh Ingot"),
    GRAPHITE("graphite", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Графитовый слиток", "en_us", "Graphite Ingot"),
    PHOSPHORUS("phosphorus", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.CRYSTAL), "ru_ru", "Брусок белого фосфора", "en_us", "Bar of White Phosphorus"),
    // Низкообогащённое шрабидиево топливо (оригинал: ingot_les/billet_les/nugget_les).
    // Слиток — отдельный ручной предмет ModItems.INGOT_LES (id "ingot_les": конвенция INGOT-формы
    // даёт "<id>_ingot", что не совпадает с историческим "ingot_les").
    LES_FUEL("les", java.util.EnumSet.of(MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток низкообогащённого шрабидиевого топлива", "en_us", "Low Enriched Schrabidium Fuel Ingot"),
    MAGNETIZED_TUNGSTEN("magnetized_tungsten", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Слиток намагниченного вольфрама", "en_us", "Magnetized Tungsten Ingot"),
    COMBINE_STEEL("combine_steel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.PLATE, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Слиток Стали Альянса", "en_us", "CMB Steel Ingot"),
    DURA_STEEL("dura_steel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.POWDER), "ru_ru", "Слиток быстрорежущей стали", "en_us", "High-Speed Steel Ingot"),
    POLYMER_COMPOSITE("pc", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Брусок твёрдого пластика", "en_us", "Hard Plastic Bar"),
    DIGAMMA("digamma", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Слиток Дигаммы", "en_us", "Digamma Ingot"),
    EUPHEMIUM("euphemium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.PLATE, MaterialShape.POWDER), "ru_ru", "Эвфемиевый слиток", "en_us", "Euphemium Ingot"),
    DINEUTRONIUM("dineutronium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.PLATE, MaterialShape.POWDER), "ru_ru", "Динейтрониевый слиток", "en_us", "Dineutronium Ingot"),
    ELECTRONIUM("electronium", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Электрониевый слиток", "en_us", "Electronium Ingot"),
    AUSTRALIUM("australium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Австралиевый слиток", "en_us", "Australium Ingot"),
    SOLINIUM("solinium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Солиниевый слиток", "en_us", "Solinium Ingot"),
    TANTALIUM("tantalium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Танталовый слиток", "en_us", "Tantalum Ingot"),
    CHAINSSTEEL("chainsteel", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Тяжёлая цепная сталь", "en_us", "Heavy Chainsteel"),
    METEORITE("meteorite", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER, MaterialShape.POWDER_TINY), "ru_ru", "Метеоритовый слиток", "en_us", "Meteorite Ingot"),
    LANTHANIUM("lanthanium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.POWDER, MaterialShape.POWDER_TINY), "ru_ru", "Полустабильный слиток лантана", "en_us", "Semi-Stable Lanthanium Ingot"),
    NEODYMIUM("neodymium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER, MaterialShape.POWDER_TINY, MaterialShape.WIRE_DENSE), "ru_ru", "Неодимовый слиток", "en_us", "Neodymium Ingot"),
    NIOBIUM("niobium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.POWDER, MaterialShape.POWDER_TINY, MaterialShape.WIRE_DENSE), "ru_ru", "Ниобиевый слиток", "en_us", "Niobium Ingot"),
    CERIUM("cerium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER, MaterialShape.POWDER_TINY), "ru_ru", "Цериевый слиток", "en_us", "Cerium Ingot"),
    CADMIUM("cadmium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.POWDER), "ru_ru", "Кадмиевый слиток", "en_us", "Cadmium Ingot"),
    CAESIUM("caesium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Цезиевый слиток", "en_us", "Caesium Ingot"),
    STRONTIUM("strontium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Стронциевый слиток", "en_us", "Strontium Ingot"),
    BROMINE("bromide", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Слиток бромида", "en_us", "Bromide Ingot"),
    TENNESSINE("tennessine", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Теннессиевый слиток", "en_us", "Tennessine Ingot"),
    ZIRCONIUM("zirconium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED, MaterialShape.POWDER, MaterialShape.WIRE), "ru_ru", "Циркониевый куб", "en_us", "Zirconium Cube"),
    ARSENIC("arsenic", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.NUGGET), "ru_ru", "Слиток мышьяка", "en_us", "Arsenic Ingot"),
    IODINE("iodine", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Йодный слиток", "en_us", "Iodine Ingot"),
    ASTATINE("astatine", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Астатовый слиток", "en_us", "Astatine Ingot"),
    // Астат-209 (ориг. MAT_AT209 / powder_at209, 4297): радиоактивный изотоп, отдельный
    // от обычного астатина (powder_astatine, 4339) — как в 1.7.10 (OreDictManager.DictFrame AT209).
    AT209("at209", java.util.EnumSet.of(MaterialShape.POWDER), "ru_ru", "Астат-209", "en_us", "Astatine-209"),
    // ДНТ-сплав (ориг. MAT_DNT): в оригинале из автогена существует только плотный провод.
    DNT("dnt", java.util.EnumSet.of(MaterialShape.WIRE_DENSE), "ru_ru", "ДНТ", "en_us", "DNT"),
    AMERICIUM("americium", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Америциевый слиток", "en_us", "Americium Ingot"),
    NEPTUNIUM("neptunium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Нептуниевый слиток", "en_us", "Neptunium Ingot"),
    POLONIUM("polonium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Слиток полония-210", "en_us", "Polonium-210 Ingot"),
    TECHNETIUM("technetium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток технеция-99", "en_us", "Technetium-99 Ingot"),
    BORON("boron", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.POWDER, MaterialShape.POWDER_TINY), "ru_ru", "Борный слиток", "en_us", "Boron Ingot"),
    SCHRABIDATE("schrabidate", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.POWDER, MaterialShape.PLATE_CAST, MaterialShape.WIRE_DENSE), "ru_ru", "Слиток шрабидата железа", "en_us", "Ferric Schrabidate Ingot"),
    SCHRARANIUM("schraranium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.CRYSTAL), "ru_ru", "Шрараниевый слиток", "en_us", "Schraranium Ingot"),
    AU198("au198", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Слиток золота-198", "en_us", "Gold-198 Ingot"),
    PB209("pb209", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток свинца-209", "en_us", "Lead-209 Ingot"),
    RA226("ra226", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Слиток радия-226", "en_us", "Radium-226 Ingot"),
    THORIUM("thorium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.CRYSTAL, MaterialShape.POWDER), "ru_ru", "Ториевый слиток", "en_us", "Thorium Ingot"),
    OSMIRIDIUM("osmiridium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.CRYSTAL, MaterialShape.NUGGET, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED), "ru_ru", "Осмиридиевый слиток", "en_us", "Osmiridium Ingot"),
    SELENIUM("selenium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Селениевый слиток", "en_us", "Selenium Ingot"),
    CO60("co60", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET, MaterialShape.POWDER), "ru_ru", "Слиток кобальта-60", "en_us", "Cobalt-60 Ingot"),
    SR90("sr90", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET, MaterialShape.POWDER, MaterialShape.POWDER_TINY), "ru_ru", "Слиток стронция-90", "en_us", "Strontium-90 Ingot"),
    AM241("am241", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток америция-241", "en_us", "Americium-241 Ingot"),
    AM242("am242", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток америция-242", "en_us", "Americium-242 Ingot"),
    STEEL_DUSTED("steel_dusted", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Запылённый стальной слиток", "en_us", "Dusted Steel Ingot"),
    CALCIUM("calcium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.POWDER), "ru_ru", "Кальциевый слиток", "en_us", "Calcium Ingot"),
    GRAPHENE("graphene", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Графеновый лист", "en_us", "Graphene Sheet"),
    MOX_FUEL("mox_fuel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток МОКС-топлива", "en_us", "Ingot of MOX Fuel"),
    SMORE("smore", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Слиток с'мора", "en_us", "S'more Ingot"),
    SCHRABIDIUM_FUEL("schrabidium_fuel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток шрабидиевого топлива", "en_us", "Ingot of Schrabidium Fuel"),
    URANIUM_FUEL("uranium_fuel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток уранового топлива", "en_us", "Ingot of Uranium Fuel"),
    THORIUM_FUEL("thorium_fuel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток ториевого топлива", "en_us", "Ingot of Thorium Fuel"),
    PLUTONIUM_FUEL("plutonium_fuel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.BLOCK, MaterialShape.NUGGET), "ru_ru", "Слиток плутониевого топлива", "en_us", "Ingot of Plutonium Fuel"),
    NEPTUNIUM_FUEL("neptunium_fuel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток нептуниевого топлива", "en_us", "Ingot of Neptunium Fuel"),
    AMERICIUM_FUEL("americium_fuel", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток америциевого топлива", "en_us", "Ingot of Americium Fuel"),
    BISMUTH_BRONZE("bismuth_bronze", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Слиток висмутовой бронзы", "en_us", "Bismuth Bronze Ingot"),
    ARSENIC_BRONZE("arsenic_bronze", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Слиток мышьяковой бронзы", "en_us", "Arsenic Bronze Ingot"),
    CRYSTALLINE("crystalline", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Кристаллиниевый слиток", "en_us", "Crystalline Ingot"),
    MUD("mud", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Кирпич из твёрдых отходов", "en_us", "Solid Mud Brick"),
    SILICON("silicon", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Брусок кремния", "en_us", "Silicon Boule"),
    FIBERGLASS("fiberglass", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Брусок стекловолокна", "en_us", "Fiberglass Bar"),
    CERAMIC("ceramic", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Керамический слиток", "en_us", "Ceramic Ingot"),
    PU_MIX("pu_mix", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток плутония реакторного качества", "en_us", "Reactor Grade Plutonium Ingot"),
    AM_MIX("am_mix", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET), "ru_ru", "Слиток америция реакторного качества", "en_us", "Reactor Grade Americium Ingot"),
    PET("pet", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "ПЭТ слиток", "en_us", "PET Ingot"),
    FERROURANIUM("ferrouranium", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.POWDER, MaterialShape.PLATE_CAST), "ru_ru", "Ферроурановый слиток", "en_us", "Ferrouranium Ingot"),
    PVC("pvc", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Брусок ПВХ", "en_us", "PVC Bar"),
    BIORUBBER("biorubber", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Брусок латекса", "en_us", "Latex Bar"),
    SEMTEX("semtex", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Брусок семтекса", "en_us", "Bar of Semtex"),
    C4("c4", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Брусок C-4", "en_us", "Bar of Composition C-4"),
    STABALLOY("staballoy", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Стабаллой", "en_us", "Staballoy"),
    METAL_SCRAP("metal.scrap", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Металлолом", "en_us", "Metal Scrap"),
    METEORITE_FORGED("meteorite_forged", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Выкованный метеоритовый слиток", "en_us", "Forged Meteorite Ingot"),
    METEORITE_FORGED_HOT("meteorite_forged_hot", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Горячий кованый метеоритный слиток", "en_us", "Hot Forged Meteorite Ingot"),
    METEORITE_HOT("meteorite_hot", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Горячий метеоритный слиток", "en_us", "Hot Meteorite Ingot"),
    IRON_SMALL("iron_small", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Маленький железный слиток", "en_us", "Small Iron Ingot"),
    CHAINSTEEL_HOT("chainsteel_hot", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Горячая цепная сталь", "en_us", "Hot Chain Steel"),
    STARMETAL_ORION_BASE("starmetal_orion_base", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Базовый звёздный металл Орион", "en_us", "Star Metal Orion Base"),
    STARMETAL_ORION("starmetal_orion", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Звёздный металл Орион", "en_us", "Star Metal Orion"),
    STARMETAL_URSA_BASE("starmetal_ursa_base", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Базовый звёздный металл Урса", "en_us", "Star Metal Ursa Base"),
    STARMETAL_URSA("starmetal_ursa", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Звёздный металл Урса", "en_us", "Star Metal Ursa"),
    STARMETAL_ASTRA_BASE("starmetal_astra_base", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Базовый звёздный металл Астра", "en_us", "Star Metal Astra Base"),
    STARMETAL_ASTRA("starmetal_astra", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Звёздный металл Астра", "en_us", "Star Metal Astra"),
    DESH_RAINBOW("desh_rainbow", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Ненавижу пони.", "en_us", "Rainbow Desh"),
    TCALLOY("tcalloy", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED), "ru_ru", "Слиток технециевой стали", "en_us", "Technetium Steel Ingot"),
    U238M2("u238m2", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Слиток метастабильного урана-238M2", "en_us", "Metastable Uranium-238M2 Ingot"),
    CELNEUTRONOPHYRIUM_NEW("celneutronophyrium_new", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Новый цельнейтронофириевый слиток", "en_us", "New Celneutronophyrium Ingot"),
    CELNEUTRONOPHYRIUM("celneutronophyrium", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Цельнейтронофириевый слиток", "en_us", "Celneutronophyrium Ingot"),
    TETRANEUTRONIUM_NEW("tetraneutronium_new", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Новый тетранейтрониевый слиток", "en_us", "New Tetraneutronium Ingot"),
    TETRANEUTRONIUM("tetraneutronium", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Тетранейтрониевый слиток", "en_us", "Tetraneutronium Ingot"),
    CFT("cft", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Кристаллический фуллерит", "en_us", "Crystalline Fullerite"),
    CDALLOY("cdalloy", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BLOCK, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED), "ru_ru", "Слиток кадмиевой стали", "en_us", "Cadmium Steel Ingot"),
    BSCCO("bscco", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.WIRE_DENSE), "ru_ru", "Слиток BSCCO", "en_us", "BSCCO Ingot"),
    BESKAR_DOUBLE("beskar_double", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Двойной бескаровый слиток", "en_us", "Double Beskar Ingot"),
    BESKAR("beskar", java.util.EnumSet.of(MaterialShape.INGOT), "ru_ru", "Бескаровый слиток", "en_us", "Beskar Ingot"),
    IRON("iron", java.util.EnumSet.of(MaterialShape.CRYSTAL, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Железный порошок", "en_us", "Iron Powder"),
    COAL("coal", java.util.EnumSet.of(MaterialShape.CRYSTAL, MaterialShape.POWDER, MaterialShape.POWDER_TINY), "ru_ru", "Угольный порошок", "en_us", "Coal Powder"),
    GOLD("gold", java.util.EnumSet.of(MaterialShape.CRYSTAL, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Золотой порошок", "en_us", "Golden Powder"),
    CEMENT("cement", java.util.EnumSet.of(MaterialShape.POWDER), "ru_ru", "Цемент", "en_us", "Cement"),
    LIMESTONE("limestone", java.util.EnumSet.of(MaterialShape.POWDER), "ru_ru", "Известняковый порошок", "en_us", "Limestone Powder"),
    COPPER("copper", java.util.EnumSet.of(MaterialShape.CRYSTAL, MaterialShape.PLATE, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED, MaterialShape.POWDER, MaterialShape.WIRE, MaterialShape.WIRE_DENSE), "ru_ru", "Медный порошок", "en_us", "Copper Powder"),
    DIAMOND("diamond", java.util.EnumSet.of(MaterialShape.CRYSTAL, MaterialShape.POWDER), "ru_ru", "Алмазная пыль", "en_us", "Diamond Powder"),
    EMERALD("emerald", java.util.EnumSet.of(MaterialShape.POWDER), "ru_ru", "Изумрудная пыль", "en_us", "Emerald Powder"),
    LAPIS("lapis", java.util.EnumSet.of(MaterialShape.CRYSTAL, MaterialShape.POWDER), "ru_ru", "Лазуритовая пыль", "en_us", "Lapis Powder"),
    QUARTZ("quartz", java.util.EnumSet.of(MaterialShape.POWDER), "ru_ru", "Кварцевый порошок", "en_us", "Quartz Powder"),
    LITHIUM("lithium", java.util.EnumSet.of(MaterialShape.CRYSTAL, MaterialShape.POWDER, MaterialShape.POWDER_TINY)),
    I131("i131", java.util.EnumSet.of(MaterialShape.POWDER, MaterialShape.POWDER_TINY)),
    XE135("xe135", java.util.EnumSet.of(MaterialShape.POWDER, MaterialShape.POWDER_TINY)),
    PALEOGENITE("paleogenite", java.util.EnumSet.of(MaterialShape.POWDER, MaterialShape.POWDER_TINY)),
    CS137("cs137", java.util.EnumSet.of(MaterialShape.POWDER, MaterialShape.POWDER_TINY)),
    AUSTRALIUM_GREATER("australium_greater", java.util.EnumSet.of(MaterialShape.BILLET, MaterialShape.NUGGET)),
    AUSTRALIUM_LESSER("australium_lesser", java.util.EnumSet.of(MaterialShape.BILLET, MaterialShape.NUGGET)),
    GH336("gh336", java.util.EnumSet.of(MaterialShape.INGOT, MaterialShape.BILLET, MaterialShape.NUGGET)),
    HES("hes", java.util.EnumSet.of(MaterialShape.BILLET, MaterialShape.NUGGET)),
    NUCLEAR_WASTE("nuclear_waste", java.util.EnumSet.of(MaterialShape.BILLET)),
    UZH("uzh", java.util.EnumSet.of(MaterialShape.BILLET)),
    BALEFIRE_GOLD("balefire_gold", java.util.EnumSet.of(MaterialShape.BILLET)),
    ZFB_BISMUTH("zfb_bismuth", java.util.EnumSet.of(MaterialShape.BILLET)),
    FLASHLEAD("flashlead", java.util.EnumSet.of(MaterialShape.BILLET)),
    PU238BE("pu238be", java.util.EnumSet.of(MaterialShape.BILLET)),
    RA226BE("ra226be", java.util.EnumSet.of(MaterialShape.BILLET)),
    PO210BE("po210be", java.util.EnumSet.of(MaterialShape.BILLET)),
    ZFB_AM_MIX("zfb_am_mix", java.util.EnumSet.of(MaterialShape.BILLET)),
    ZFB_PU241("zfb_pu241", java.util.EnumSet.of(MaterialShape.BILLET)),
    YHARONITE("yharonite", java.util.EnumSet.of(MaterialShape.BILLET)),
    VIRUS("virus", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    REDSTONE("redstone", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    SULFUR("sulfur", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    HORN("horn", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    NITER("niter", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    PULSAR("pulsar", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    HARDENED("hardened", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    XEN("xen", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    CINNEBAR("cinnebar", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    CHARRED("charred", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    FLUORITE("fluorite", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    TRIXITE("trixite", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    ALUMINIUM("aluminium", java.util.EnumSet.of(MaterialShape.CRYSTAL, MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED, MaterialShape.WIRE, MaterialShape.WIRE_DENSE)),
    RARE("rare", java.util.EnumSet.of(MaterialShape.CRYSTAL)),
    CMB("cmb", java.util.EnumSet.of(MaterialShape.PLATE_CAST, MaterialShape.PLATE_WELDED)),
    ABRONZE("abronze", java.util.EnumSet.of(MaterialShape.PLATE_CAST)),
    BBRONZE("bbronze", java.util.EnumSet.of(MaterialShape.PLATE_CAST)),
    STAR_METAL("star_metal", java.util.EnumSet.of(MaterialShape.PLATE_CAST, MaterialShape.WIRE_DENSE)),
    ALLOY("alloy", java.util.EnumSet.of(MaterialShape.PLATE_CAST)),
    CARBON("carbon", java.util.EnumSet.of(MaterialShape.WIRE)),
    SCRAP_PLASTIC("scrap_plastic", java.util.EnumSet.of(MaterialShape.SCRAP)),
    SCRAP("scrap", java.util.EnumSet.of(MaterialShape.SCRAP)),
    COMBINE_SCRAP("combine_scrap", java.util.EnumSet.of(MaterialShape.SCRAP)),
    SCRAP_NUCLEAR("scrap_nuclear", java.util.EnumSet.of(MaterialShape.SCRAP)),
    SCRAP_OIL("scrap_oil", java.util.EnumSet.of(MaterialShape.SCRAP)),
    // Материалы ориг. Mats.java, существующие только как литейный шлак (ItemScraps 4765).
    STONE("stone", java.util.EnumSet.noneOf(MaterialShape.class)),
    OBSIDIAN("obsidian", java.util.EnumSet.noneOf(MaterialShape.class)),
    HEMATITE("hematite", java.util.EnumSet.noneOf(MaterialShape.class)),
    WROUGHT_IRON("wrought_iron", java.util.EnumSet.noneOf(MaterialShape.class)),
    PIG_IRON("pig_iron", java.util.EnumSet.noneOf(MaterialShape.class)),
    MALACHITE("malachite", java.util.EnumSet.noneOf(MaterialShape.class)),
    PLUTONIUM_RG("plutoniumrg", java.util.EnumSet.noneOf(MaterialShape.class)),
    AMERICIUM_RG("americiumrg", java.util.EnumSet.noneOf(MaterialShape.class)),
    GHIORSIUM("ghiorsium336", java.util.EnumSet.noneOf(MaterialShape.class)),
    BORAX("borax", java.util.EnumSet.noneOf(MaterialShape.class)),
    SODIUM("sodium", java.util.EnumSet.noneOf(MaterialShape.class)),
    FLUX("flux", java.util.EnumSet.noneOf(MaterialShape.class)),
    SLAG("slag", java.util.EnumSet.noneOf(MaterialShape.class));

    private final String id;
    private final java.util.Set<MaterialShape> shapes;
    private final Map<String, String> translations;

    ModMaterials(String id, java.util.Set<MaterialShape> shapes, String... translationPairs) {
        this.id = id;
        this.shapes = Collections.unmodifiableSet(shapes);
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < translationPairs.length; i += 2) {
            map.put(translationPairs[i], translationPairs[i + 1]);
        }
        this.translations = Collections.unmodifiableMap(map);
    }

    public String getId() { return id; }

    /** Формы, существующие у материала. */
    public java.util.Set<MaterialShape> getShapes() { return shapes; }

    public boolean has(MaterialShape shape) { return shapes.contains(shape); }

    /**
     * Перевод базовой (ingot) формы; для материалов без слитка — null.
     * Переводы прочих форм выводятся в датагене из этого значения или задаются шаблоном формы.
     */
    public String getTranslation(String locale) { return translations.get(locale); }

    private static final Map<String, ModMaterials> BY_ID = new HashMap<>();

    static {
        for (ModMaterials m : values()) {
            BY_ID.put(m.id, m);
        }
    }

    public static ModMaterials byId(String id) { return BY_ID.get(id); }
}
