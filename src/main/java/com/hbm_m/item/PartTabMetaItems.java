package com.hbm_m.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.platform.PlatformHooks;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * Развёртка мета-предметов оригинала (один item с N мет) в ОТДЕЛЬНЫЕ предметы —
 * по одному на мету, как устоялось в порту (circuit, ash, chunk_ore, oil_tar и т.д.).
 * Каждая мета = отдельный СЛОТ в креатив-вкладке Parts (1:1 со scripts/parts_tab_true.tsv).
 *
 * Таблица ниже — единый источник истины для:
 *  - регистрации ({@link #registerAll()}, вызывается из static-блока ModItems);
 *  - датагена моделей ({@link #entries()} в ModItemModelProvider);
 *  - датагена локализаций en/ru ({@link #entries()}, поля en/ru — имена из
 *    en_US.lang / ru_RU.lang оригинала, для %-форматов имя материала подставлено);
 *  - клиентских тинтов ({@link #tintFor}, ClientSetup) — базовая текстура одна,
 *    цветные варианты через vanilla ItemColor, аппроксимация RGBMutatorInterpolatedComponentRemap
 *    значением solidColorLight из Mats.java оригинала;
 *  - хазардов (HazardRegistry, группы {@link #group()}).
 */
public final class PartTabMetaItems {

    /** Фабрика предмета (кросс-версионные свойства собираются внутри). */
    public interface ItemFactory { Item create(Item.Properties props); }

    public static final class Entry {
        public final String id;
        /** Группа для перебора в креатив-табе и HazardRegistry. */
        public final String group;
        public final String en;
        public final String ru;
        /** Текстура layer0 (путь под textures/item/ без .png); null -> id. */
        public final String layer0;
        /** Текстура layer1 (оверлей, тинтуется); null -> двухслойной модели нет. */
        public final String layer1;
        /** Тинт 0xRRGGBB; 0 — без тинта. */
        public final int tint;
        /** Италик-строка тултипа (изотоп отходов, как addInformation ItemWasteLong/Short). */
        public final String italicLore;
        /** Транслябл-тултип (охлаждающиеся отходы: tooltip.hbm_m.waste_cooling.desc, GOLD). */
        public final boolean coolingTooltip;
        public final ItemFactory factory;

        Entry(String id, String group, String en, String ru, String layer0, String layer1,
              int tint, String italicLore, boolean coolingTooltip, ItemFactory factory) {
            this.id = id; this.group = group; this.en = en; this.ru = ru;
            this.layer0 = layer0; this.layer1 = layer1; this.tint = tint;
            this.italicLore = italicLore; this.coolingTooltip = coolingTooltip;
            this.factory = factory;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, RegistrySupplier<Item>> ITEMS = new LinkedHashMap<>();

    private static void add(Entry e) { ENTRIES.add(e); }

    private static ItemFactory plain() {
        return props -> new Item(props);
    }

    /** LoreTooltipItem с италик-строкой и/или золотой строкой охлаждения. */
    private static ItemFactory wasteLore(final String italicLore, final boolean cooling) {
        return wasteLore(italicLore, cooling, false);
    }

    /** {@code nuclear}: Original-Klasse leitet von {@code ItemNuclearWaste} ab (ItemWasteLong, ItemDepletedFuel). */
    private static ItemFactory wasteLore(final String italicLore, final boolean cooling, final boolean nuclear) {
        return props -> {
            List<Component> lines = new ArrayList<>();
            if (italicLore != null) {
                lines.add(Component.literal(italicLore).withStyle(ChatFormatting.ITALIC));
            }
            if (cooling) {
                lines.add(Component.translatable("tooltip.hbm_m.waste_cooling.desc").withStyle(ChatFormatting.GOLD));
            }
            if (nuclear) return new com.hbm_m.item.special.ItemNuclearWaste(lines, props);
            return lines.isEmpty() ? new Item(props) : new LoreTooltipItem(lines, props);
        };
    }

    private static ItemFactory crayonFood() {
        // ItemCrayon оригинала: ItemFood(3, false) + setAlwaysEdible.
        return props -> new Item(props.food(
                //? if < 1.21.1 {
                PlatformHooks.foodBuilder(3, 0.6F).alwaysEat().build()
                //?} else {
                /*PlatformHooks.foodBuilder(3, 0.6F).alwaysEdible().build()
                *///?}
        ));
    }

    // =====================================================================================
    //  Таблица. Порядок добавления = порядок строк parts_tab_true.tsv.
    // =====================================================================================

    // ── 4533 shell (ItemAutogen SHELL): 4 из 6 мет уже есть в порту, доблены 2 ──────────
    static {
        // material solidColorLight из Mats.java (аппроксимация тинта ItemAutogen)
        add(new Entry("shell_weaponsteel", "shell", "Weapon Steel Shell", "Оболочка (Оружейная сталь)",
                "shell", null, 0xA0A0A0, null, false, plain()));
        add(new Entry("shell_saturnite", "shell", "Saturnite Shell", "Оболочка (Сатурнит)",
                "shell", null, 0x3AC4DA, null, false, plain()));
        // BIGMT в оригинале: hbmmat.bigmt отсутствует в lang, словарь OreDictManager — Saturnite.

        // ── 4534 pipe: 6 из 7 мет уже есть, доблена резина ─────────────────────────────
        add(new Entry("pipe_rubber", "pipe", "Rubber Pipe", "Резиновая труба",
                "pipe", null, 0x817F75, null, false, plain()));

        // ── 4576..4582 part_* (ItemAutogen LIGHTBARREL/HEAVYBARREL/LIGHTRECEIVER/
        //    HEAVYRECEIVER/MECHANISM/STOCK/GRIP) ─────────────────────────────────────────
        String[][] barrelLight = {
                {"steel", "Steel", "Сталь", "AFAFAF"},
                {"dura_steel", "High-Speed Steel", "Быстрорежущая сталь", "82A59C"},
                {"desh", "Desh", "Деш", "FF6D6D"},
                {"tcalloy", "Technetium Steel", "Технециевая сталь", "D4D6D6"},
                {"cdalloy", "Cadmium Steel", "Кадмиевая сталь", "F7DF8F"},
                {"bbronze", "Bismuth Bronze", "Висмутовая бронза", "E19A69"},
                {"abronze", "Arsenic Bronze", "Мышьяковая бронза", "DB9462"},
                {"gunmetal", "Gunmetal", "Пушечная бронза", "FFEF3F"},
                {"weaponsteel", "Weapon Steel", "Оружейная сталь", "A0A0A0"},
                {"saturnite", "Saturnite", "Сатурнит", "3AC4DA"},
        };
        for (String[] m : barrelLight) {
            add(new Entry("part_barrel_light_" + m[0], "barrel_light",
                    "Light " + m[1] + " Barrel", "Лёгкий ствол (" + m[2] + ")",
                    "part_barrel_light", null, (int) Long.parseLong(m[3], 16), null, false, plain()));
        }

        String[][] barrelHeavy = {
                {"steel", "Steel", "Сталь", "AFAFAF"},
                {"dura_steel", "High-Speed Steel", "Быстрорежущая сталь", "82A59C"},
                {"desh", "Desh", "Деш", "FF6D6D"},
                {"ferrouranium", "Ferrouranium", "Ферроуран", "B7B7C9"},
                {"tcalloy", "Technetium Steel", "Технециевая сталь", "D4D6D6"},
                {"cdalloy", "Cadmium Steel", "Кадмиевая сталь", "F7DF8F"},
                {"gunmetal", "Gunmetal", "Пушечная бронза", "FFEF3F"},
                {"weaponsteel", "Weapon Steel", "Оружейная сталь", "A0A0A0"},
                {"saturnite", "Saturnite", "Сатурнит", "3AC4DA"},
        };
        for (String[] m : barrelHeavy) {
            add(new Entry("part_barrel_heavy_" + m[0], "barrel_heavy",
                    "Heavy " + m[1] + " Barrel", "Тяжёлый ствол (" + m[2] + ")",
                    "part_barrel_heavy", null, (int) Long.parseLong(m[3], 16), null, false, plain()));
        }

        String[][] receiverLight = {
                {"steel", "Steel", "Сталь", "AFAFAF"},
                {"dura_steel", "High-Speed Steel", "Быстрорежущая сталь", "82A59C"},
                {"desh", "Desh", "Деш", "FF6D6D"},
                {"tcalloy", "Technetium Steel", "Технециевая сталь", "D4D6D6"},
                {"cdalloy", "Cadmium Steel", "Кадмиевая сталь", "F7DF8F"},
                {"bbronze", "Bismuth Bronze", "Висмутовая бронза", "E19A69"},
                {"abronze", "Arsenic Bronze", "Мышьяковая бронза", "DB9462"},
                {"gunmetal", "Gunmetal", "Пушечная бронза", "FFEF3F"},
                {"weaponsteel", "Weapon Steel", "Оружейная сталь", "A0A0A0"},
                {"saturnite", "Saturnite", "Сатурнит", "3AC4DA"},
        };
        for (String[] m : receiverLight) {
            add(new Entry("part_receiver_light_" + m[0], "receiver_light",
                    "Light " + m[1] + " Receiver", "Лёгкий ресивер (" + m[2] + ")",
                    "part_receiver_light", null, (int) Long.parseLong(m[3], 16), null, false, plain()));
        }

        String[][] receiverHeavy = {
                {"dura_steel", "High-Speed Steel", "Быстрорежущая сталь", "82A59C"},
                {"ferrouranium", "Ferrouranium", "Ферроуран", "B7B7C9"},
                {"tcalloy", "Technetium Steel", "Технециевая сталь", "D4D6D6"},
                {"cdalloy", "Cadmium Steel", "Кадмиевая сталь", "F7DF8F"},
                {"bbronze", "Bismuth Bronze", "Висмутовая бронза", "E19A69"},
                {"abronze", "Arsenic Bronze", "Мышьяковая бронза", "DB9462"},
                {"gunmetal", "Gunmetal", "Пушечная бронза", "FFEF3F"},
                {"weaponsteel", "Weapon Steel", "Оружейная сталь", "A0A0A0"},
                {"saturnite", "Saturnite", "Сатурнит", "3AC4DA"},
        };
        for (String[] m : receiverHeavy) {
            add(new Entry("part_receiver_heavy_" + m[0], "receiver_heavy",
                    "Heavy " + m[1] + " Receiver", "Тяжёлый ресивер (" + m[2] + ")",
                    "part_receiver_heavy", null, (int) Long.parseLong(m[3], 16), null, false, plain()));
        }

        String[][] mechanism = {
                {"gunmetal", "Gunmetal", "Пушечная бронза", "FFEF3F"},
                {"weaponsteel", "Weapon Steel", "Оружейная сталь", "A0A0A0"},
                {"saturnite", "Saturnite", "Сатурнит", "3AC4DA"},
        };
        for (String[] m : mechanism) {
            add(new Entry("part_mechanism_" + m[0], "mechanism",
                    m[1] + " Mechanism", "Оружейный механизм (" + m[2] + ")",
                    "part_mechanism", null, (int) Long.parseLong(m[3], 16), null, false, plain()));
        }

        String[][] stock = {
                {"wood", "Wood", "Дерево", "896727"},
                {"desh", "Desh", "Деш", "FF6D6D"},
                {"gunmetal", "Gunmetal", "Пушечная бронза", "FFEF3F"},
                {"weaponsteel", "Weapon Steel", "Оружейная сталь", "A0A0A0"},
                {"saturnite", "Saturnite", "Сатурнит", "3AC4DA"},
                {"polymer", "Polymer", "Полимер", "363636"},
                {"bakelite", "Bakelite", "Бакелит", "F28086"},
                {"pc", "Polycarbonate", "Поликарбонат", "EDE7C4"},
                {"pvc", "PVC", "ПВХ", "FCFCFC"},
        };
        for (String[] m : stock) {
            add(new Entry("part_stock_" + m[0], "stock",
                    m[1] + " Stock", "Приклад (" + m[2] + ")",
                    "part_stock", null, (int) Long.parseLong(m[3], 16), null, false, plain()));
        }

        String[][] grip = {
                {"wood", "Wood", "Дерево", "896727"},
                {"ivory", "Ivory", "Кость", "FFFEEE"},
                {"steel", "Steel", "Сталь", "AFAFAF"},
                {"dura_steel", "High-Speed Steel", "Быстрорежущая сталь", "82A59C"},
                {"desh", "Desh", "Деш", "FF6D6D"},
                {"gunmetal", "Gunmetal", "Пушечная бронза", "FFEF3F"},
                {"weaponsteel", "Weapon Steel", "Оружейная сталь", "A0A0A0"},
                {"saturnite", "Saturnite", "Сатурнит", "3AC4DA"},
                {"polymer", "Polymer", "Полимер", "363636"},
                {"bakelite", "Bakelite", "Бакелит", "F28086"},
                {"rubber", "Rubber", "Резина", "817F75"},
                {"pc", "Polycarbonate", "Поликарбонат", "EDE7C4"},
                {"pvc", "PVC", "ПВХ", "FCFCFC"},
        };
        for (String[] m : grip) {
            add(new Entry("part_grip_" + m[0], "grip",
                    m[1] + " Grip", "Рукоятка (" + m[2] + ")",
                    "part_grip", null, (int) Long.parseLong(m[3], 16), null, false, plain()));
        }

        // ── 4567 chemical_dye (ItemChemicalDye, 16 цветов EnumChemDye) ─────────────────
        // Двухслойная модель: база chemical_dye + оверлей chemical_dye_overlay, тинт = dye.color
        // (значения цветов — дословно из EnumChemDye оригинала).
        String[][] dyes = {
                {"black", "Black", "Чёрный", "1973019"},
                {"red", "Red", "Красный", "11743532"},
                {"green", "Green", "Зелёный", "3887386"},
                {"brown", "Brown", "Коричневый", "5320730"},
                {"blue", "Blue", "Синий", "2437522"},
                {"purple", "Purple", "Фиолетовый", "8073150"},
                {"cyan", "Cyan", "Голубой", "2651799"},
                {"silver", "Light Gray", "Светло-серый", "11250603"},
                {"gray", "Gray", "Серый", "4408131"},
                {"pink", "Pink", "Розовый", "14188952"},
                {"lime", "Lime", "Лаймовый", "4312372"},
                {"yellow", "Yellow", "Жёлтый", "14602026"},
                {"lightblue", "Light Blue", "Светло-синий", "6719955"},
                {"magenta", "Magenta", "Пурпурный", "12801229"},
                {"orange", "Orange", "Оранжевый", "15435844"},
                {"white", "White", "Белый", "15790320"},
        };
        for (String[] d : dyes) {
            int color = (int) Long.parseLong(d[3]);
            add(new Entry("chemical_dye_" + d[0], "dye",
                    "Chemical Dye (" + d[1] + ")", "Химический краситель (" + d[2] + ")",
                    "chemical_dye", "chemical_dye_overlay", color, null, false, plain()));
        }

        // ── 4568 crayon (ItemCrayon, те же 16 цветов; ItemFood(3)+alwaysEdible) ────────
        String[][] crayons = dyes;
        for (String[] d : crayons) {
            int color = (int) Long.parseLong(d[3]);
            add(new Entry("crayon_" + d[0], "crayon",
                    d[1] + " Crayon", d[2] + " мелок",
                    "crayon", "crayon_overlay", color, null, false, crayonFood()));
        }

        // ── 4569 part_generic (ItemGenericPart, отдельные текстуры) ────────────────────
        add(new Entry("part_generic_piston_pneumatic", "part_generic",
                "Pneumatic Piston", "Пневматический поршень", "piston_pneumatic", null, 0, null, false, plain()));
        add(new Entry("part_generic_piston_hydraulic", "part_generic",
                "Hydraulic Piston", "Гидравлический поршень", "piston_hydraulic", null, 0, null, false, plain()));
        add(new Entry("part_generic_piston_electric", "part_generic",
                "Electric Piston", "Электрический поршень", "piston_electric", null, 0, null, false, plain()));
        add(new Entry("part_generic_lde", "part_generic",
                "Low-Density Element", "Элемент малой плотности", "low_density_element", null, 0, null, false, plain()));
        add(new Entry("part_generic_hde", "part_generic",
                "Heavy Duty Element", "Элемент повышенной прочности", "heavy_duty_element", null, 0, null, false, plain()));
        add(new Entry("part_generic_glass_polarized", "part_generic",
                "Polarized Lens", "Поляризованная линза", "glass_polarized", null, 0, null, false, plain()));

        // ── 4573 parts_legendary (ItemEnumMulti EnumLegendaryType, multiName=false —
        //    все меты называются одинаково, текстуры свои) ──────────────────────────────
        for (int i = 1; i <= 3; i++) {
            add(new Entry("parts_legendary_tier" + i, "legendary",
                    "Legendary Parts", "Легендарные запчасти",
                    "parts_legendary_tier" + i, null, 0, null, false, plain()));
        }

        // ── 4574 gear_large (ItemGear, meta1 = "_steel") ───────────────────────────────
        add(new Entry("gear_large_steel", "gear",
                "Large Steel Gear", "Большая стальная шестерня",
                "gear_large", null, 0, null, false, plain()));

        // ── 4583 plant_item (ItemEnumMulti EnumPlantType, отдельные текстуры) ──────────
        add(new Entry("plant_item_tobacco", "plant",
                "Tobacco", "Табак", "plant_item_tobacco", null, 0, null, false, plain()));
        add(new Entry("plant_item_rope", "plant",
                "Rope", "Верёвка", "plant_item_rope", null, 0, null, false, plain()));
        add(new Entry("plant_item_mustardwillow", "plant",
                "Mustard Willow Leaf", "Лист горчичной ивы", "plant_item_mustardwillow", null, 0, null, false, plain()));

        // ── 4636 casing (ItemEnumMulti EnumCasingType, отдельные текстуры) ─────────────
        add(new Entry("casing_small", "casing",
                "Small Gunmetal Casing", "Маленькая гильза из пушечной бронзы",
                "casing_small", null, 0, null, false, plain()));
        add(new Entry("casing_large", "casing",
                "Large Gunmetal Casing", "Большая гильза из пушечной бронзы",
                "casing_large", null, 0, null, false, plain()));
        add(new Entry("casing_small_steel", "casing",
                "Small Weapon Steel Casing", "Маленькая гильза из оружейной стали",
                "casing_small_steel", null, 0, null, false, plain()));
        add(new Entry("casing_large_steel", "casing",
                "Large Weapon Steel Casing", "Большая гильза из оружейной стали",
                "casing_large_steel", null, 0, null, false, plain()));
        add(new Entry("casing_shotshell", "casing",
                "Black Powder Shotshell Casing", "Гильза дробового патрона для дымного пороха",
                "casing_shotshell", null, 0, null, false, plain()));
        add(new Entry("casing_buckshot", "casing",
                "Plastic Shotshell Casing", "Пластиковая гильза дробового патрона",
                "casing_buckshot", null, 0, null, false, plain()));
        add(new Entry("casing_buckshot_advanced", "casing",
                "Advanced Shotshell Casing", "Продвинутая гильза дробового патрона",
                "casing_buckshot_advanced", null, 0, null, false, plain()));

        // ── 4631 drive (ItemDrive, ItemEnumMulti EnumDriveType, отдельные текстуры).
        //    Порядок = порядок объявления EnumDriveType (IOrderedEnum нет). ────────────────
        add(new Entry("drive_flash_empty", "drive",
                "Flash Drive (Empty)", "Дата-флешка (Пустой)", "drive_flash_empty", null, 0, null, false, plain()));
        add(new Entry("drive_disk_empty", "drive",
                "Disk Drive (Empty)", "Дата-диск (Пустой)", "drive_disk_empty", null, 0, null, false, plain()));
        add(new Entry("drive_flash_broken", "drive",
                "Flash Drive (Broken)", "Дата-флешка (Сломанный)", "drive_flash_broken", null, 0, null, false, plain()));
        add(new Entry("drive_disk_broken", "drive",
                "Disk Drive (Broken)", "Дата-диск (Сломанный)", "drive_disk_broken", null, 0, null, false, plain()));
        add(new Entry("drive_flash_flightsim", "drive",
                "Flash Drive (Flight Simulation Data)", "Дата-флешка (Данные симуляции полета)",
                "drive_flash_flightsim", null, 0, null, false, plain()));
        add(new Entry("drive_flash_particlesim", "drive",
                "Flash Drive (Particle Simulation Data)", "Дата-флешка (Данные симуляции частиц)",
                "drive_flash_particlesim", null, 0, null, false, plain()));
        add(new Entry("drive_disk_flightdata", "drive",
                "Disk Drive (Flight Data)", "Дата-диск (Данные полета)",
                "drive_disk_flightdata", null, 0, null, false, plain()));
        add(new Entry("drive_disk_flightdata_processed", "drive",
                "Disk Drive (Processed Flight Data)", "Дата-диск (Обработанные данные полета)",
                "drive_disk_flightdata_processed", null, 0, null, false, plain()));
        add(new Entry("drive_disk_orbitdata", "drive",
                "Disk Drive (Orbital Data)", "Дата-диск (Орбитальные данные)",
                "drive_disk_orbitdata", null, 0, null, false, plain()));
        add(new Entry("drive_disk_orbitdata_processed", "drive",
                "Disk Drive (Processed Orbital Data)", "Дата-диск (Обработанные орбитальные данные)",
                "drive_disk_orbitdata_processed", null, 0, null, false, plain()));
        // item.drive.klaus.name=Klaus (в ru_RU.lang оригинала перевода нет)
        add(new Entry("drive_klaus", "drive",
                "Klaus", "Клаус", "drive_klaus", null, 0, null, false, plain()));

        // ── 4878..4886 waste_* (ItemDepletedFuel): мета0 = свежее (rad base*0.075),
        //    мета1 = охлаждающееся (тинт 0xFFBFA5, GOLD-тултип, rad base + HOT 5).
        //    Свежие предметы уже есть в порту, здесь — охлаждающиеся варианты. ─────────
        String[][] waste = {
                {"waste_natural_uranium", "Depleted Natural Uranium Fuel", "Обеднённое топливо (Природный уран)", "waste_natural_uranium", "11.5F"},
                {"waste_uranium", "Depleted Uranium Fuel", "Обеднённое топливо (Топливный уран)", "waste_uranium", "10F"},
                {"waste_thorium", "Depleted Thorium Fuel", "Обеднённое топливо (Топливный торий)", "waste_thorium", "7.5F"},
                {"waste_mox", "Depleted MOX Fuel", "Обеднённое топливо (МОКС)", "waste_mox", "10F"},
                {"waste_plutonium", "Depleted Plutonium Fuel", "Обеднённое топливо (Топливный плутоний)", "waste_plutonium", "12.5F"},
                {"waste_u233", "Depleted Uranium-233 Fuel", "Обеднённое топливо (Уран-233)", "waste_u233", "10F"},
                {"waste_u235", "Depleted Uranium-235 Fuel", "Обеднённое топливо (Уран-235)", "waste_u235", "11F"},
                {"waste_schrabidium", "Depleted Schrabidium Fuel", "Обеднённое топливо (Шрабидий)", "waste_schrabidium", "15F"},
                {"waste_zfb_mox", "Depleted ZFB MOX Fuel", "Обеднённое топливо (ЦБР МОКС)", "waste_zfb_mox", "5F"},
                // ItemDepletedFuel-Platten des Forschungsreaktors (Original: Metadaten 1 = heiss)
                {"waste_plate_u233", "Depleted HEU-233 Plate Fuel", "Обеднённая топливная пластина (Высокообогащённый уран-233)", "waste_plate_uranium", "13F"},
                {"waste_plate_u235", "Depleted HEU-235 Plate Fuel", "Обеднённая топливная пластина (Высокообогащённый уран-235)", "waste_plate_uranium", "10F"},
                {"waste_plate_mox", "Depleted MOX Plate Fuel", "Обеднённая топливная пластина (МОКС)", "waste_plate_mox", "16F"},
                {"waste_plate_pu239", "Depleted HEP-239 Plate Fuel", "Обеднённая топливная пластина (Высокообогащённый плутоний-239)", "waste_plate_mox", "13.5F"},
                {"waste_plate_sa326", "Depleted HES-326 Plate Fuel", "Обеднённая топливная пластина (Высокообогащённый шрабидий-326)", "waste_plate_sa326", "10F"},
                {"waste_plate_ra226be", "Depleted Ra226Be Plate Fuel", "Обеднённая топливная пластина (Радий-226-бериллий)", "waste_plate_ra226be", "0F"},
                {"waste_plate_pu238be", "Depleted Pu238Be Plate Fuel", "Обеднённая топливная пластина (Плутоний-238-бериллий)", "waste_plate_pu238be", "0F"},
        };
        for (String[] w : waste) {
            add(new Entry(w[0] + "_cooling", "waste_cooling:" + w[4],
                    w[1], w[2], w[3], null, 0xFFBFA5, null, true, wasteLore(null, true, true)));
        }

        // ── 4999..5006 nuclear_waste_long/short (ItemWasteLong/ItemWasteShort):
        //    все меты предмета имели одно имя + италик-строку изотопа; хазард — общий
        //    на предмет. Развёртка: отдельный предмет на мету, имя то же, лор — изотоп. ──
        String[][] longIso = {
                {"u235", "Uranium-235"},
                {"u233", "Uranium-233"},
                {"neptunium", "Neptunium-237"},
                {"thorium", "Thorium-232"},
                {"schrabidium", "Schrabidium-326"},
        };
        String[][] shortIso = {
                {"u235", "Uranium-235"},
                {"u233", "Uranium-233"},
                {"neptunium", "Neptunium-237"},
                {"pu239", "Plutonium-239"},
                {"pu240", "Plutonium-240"},
                {"pu241", "Plutonium-241"},
                {"am242", "Americium-242"},
                {"schrabidium", "Schrabidium-326"},
        };
        // 4999 nuclear_waste_long — 5 мет
        for (String[] iso : longIso) {
            add(new Entry("nuclear_waste_long_" + iso[0], "nw_long",
                    "Long-Lived Nuclear Waste", "Долгоживущие ядерные отходы",
                    "nuclear_waste_long", null, 0, iso[1], false, wasteLore(iso[1], false, true)));
        }
        // 5000 nuclear_waste_long_tiny — 5 мет
        for (String[] iso : longIso) {
            add(new Entry("nuclear_waste_long_tiny_" + iso[0], "nw_long_tiny",
                    "Tiny Pile of Long-Lived Nuclear Waste", "Кучка долгоживущих ядерных отходов",
                    "nuclear_waste_long_tiny", null, 0, iso[1], false, wasteLore(iso[1], false, true)));
        }
        // 5001 nuclear_waste_short — 8 мет
        for (String[] iso : shortIso) {
            add(new Entry("nuclear_waste_short_" + iso[0], "nw_short",
                    "Short-Lived Nuclear Waste", "Короткоживущие ядерные отходы",
                    "nuclear_waste_short", null, 0, iso[1], false, wasteLore(iso[1], false)));
        }
        // 5002 nuclear_waste_short_tiny — 8 мет
        for (String[] iso : shortIso) {
            add(new Entry("nuclear_waste_short_tiny_" + iso[0], "nw_short_tiny",
                    "Tiny Pile of Short-Lived Nuclear Waste", "Кучка короткоживущих ядерных отходов",
                    "nuclear_waste_short_tiny", null, 0, iso[1], false, wasteLore(iso[1], false)));
        }
        // 5003 nuclear_waste_long_depleted — 5 мет
        for (String[] iso : longIso) {
            add(new Entry("nuclear_waste_long_depleted_" + iso[0], "nw_long_dep",
                    "Decayed Long-Lived Nuclear Waste", "Разложившиеся долгоживущие ядерные отходы",
                    "nuclear_waste_long_depleted", null, 0, iso[1], false, wasteLore(iso[1], false, true)));
        }
        // 5004 nuclear_waste_long_depleted_tiny — 5 мет
        for (String[] iso : longIso) {
            add(new Entry("nuclear_waste_long_depleted_tiny_" + iso[0], "nw_long_dep_tiny",
                    "Tiny Pile of Decayed Long-Lived Nuclear Waste", "Кучка разложившихся долгоживущих ядерных отходов",
                    "nuclear_waste_long_depleted_tiny", null, 0, iso[1], false, wasteLore(iso[1], false, true)));
        }
        // 5005 nuclear_waste_short_depleted — 8 мет
        for (String[] iso : shortIso) {
            add(new Entry("nuclear_waste_short_depleted_" + iso[0], "nw_short_dep",
                    "Decayed Short-Lived Nuclear Waste", "Разложившиеся короткоживущие ядерные отходы",
                    "nuclear_waste_short_depleted", null, 0, iso[1], false, wasteLore(iso[1], false)));
        }
        // 5006 nuclear_waste_short_depleted_tiny — 8 мет
        for (String[] iso : shortIso) {
            add(new Entry("nuclear_waste_short_depleted_tiny_" + iso[0], "nw_short_dep_tiny",
                    "Tiny Pile of Decayed Short-Lived Nuclear Waste", "Кучка разложившихся короткоживущих ядерных отходов",
                    "nuclear_waste_short_depleted_tiny", null, 0, iso[1], false, wasteLore(iso[1], false)));
        }
    }

    static {
        // ── ItemBedrockOre (Legacy-Grundgesteinserz, creativeTab null): 10 Stufen x 15 Erze,
        //    Grundtextur + Erz-Overlay (ore_overlay) in der Erzfarbe (getColorFromItemStack pass 1) ──
        add(new Entry("ore_bedrock_iron", "bedrock_ore:ore_bedrock", "Iron Bedrock Ore", "Бедроковая руда (Железная)", "ore_bedrock", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_bedrock_copper", "bedrock_ore:ore_bedrock", "Copper Bedrock Ore", "Бедроковая руда (Медная)", "ore_bedrock", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_bedrock_borax", "bedrock_ore:ore_bedrock", "Borax Bedrock Ore", "Бедроковая руда (Буровая)", "ore_bedrock", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_bedrock_asbestos", "bedrock_ore:ore_bedrock", "Asbestos Bedrock Ore", "Бедроковая руда (Асбестовая)", "ore_bedrock", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_bedrock_niobium", "bedrock_ore:ore_bedrock", "Niobium Bedrock Ore", "Бедроковая руда (Ниобиевая)", "ore_bedrock", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_bedrock_titanium", "bedrock_ore:ore_bedrock", "Titanium Bedrock Ore", "Бедроковая руда (Титановая)", "ore_bedrock", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_bedrock_tungsten", "bedrock_ore:ore_bedrock", "Tungsten Bedrock Ore", "Бедроковая руда (Вольфрамовая)", "ore_bedrock", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_bedrock_gold", "bedrock_ore:ore_bedrock", "Gold Bedrock Ore", "Бедроковая руда (Золотая)", "ore_bedrock", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_bedrock_uranium", "bedrock_ore:ore_bedrock", "Uranium Bedrock Ore", "Бедроковая руда (Урановая)", "ore_bedrock", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_bedrock_thorium", "bedrock_ore:ore_bedrock", "Thorium Bedrock Ore", "Бедроковая руда (Ториевая)", "ore_bedrock", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_bedrock_chlorocalcite", "bedrock_ore:ore_bedrock", "Chlorocalcite Bedrock Ore", "Бедроковая руда (Хлоркальцитовая)", "ore_bedrock", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_bedrock_fluorite", "bedrock_ore:ore_bedrock", "Fluorite Bedrock Ore", "Бедроковая руда (Флюоритная)", "ore_bedrock", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_bedrock_hematite", "bedrock_ore:ore_bedrock", "Hematite Bedrock Ore", "Бедроковая руда (Гематитовая)", "ore_bedrock", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_bedrock_malachite", "bedrock_ore:ore_bedrock", "Malachite Bedrock Ore", "Бедроковая руда (Малахитовая)", "ore_bedrock", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_bedrock_neodymium", "bedrock_ore:ore_bedrock", "Neodymium Bedrock Ore", "Бедроковая руда (Неодимовая)", "ore_bedrock", "ore_overlay", 0x8F8F5F, null, false, plain()));
        add(new Entry("ore_centrifuged_iron", "bedrock_ore:ore_centrifuged", "Centrifuged Iron Ore", "Центрифугированная бедроковая руда (Железная)", "ore_centrifuged", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_centrifuged_copper", "bedrock_ore:ore_centrifuged", "Centrifuged Copper Ore", "Центрифугированная бедроковая руда (Медная)", "ore_centrifuged", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_centrifuged_borax", "bedrock_ore:ore_centrifuged", "Centrifuged Borax Ore", "Центрифугированная бедроковая руда (Буровая)", "ore_centrifuged", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_centrifuged_asbestos", "bedrock_ore:ore_centrifuged", "Centrifuged Asbestos Ore", "Центрифугированная бедроковая руда (Асбестовая)", "ore_centrifuged", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_centrifuged_niobium", "bedrock_ore:ore_centrifuged", "Centrifuged Niobium Ore", "Центрифугированная бедроковая руда (Ниобиевая)", "ore_centrifuged", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_centrifuged_titanium", "bedrock_ore:ore_centrifuged", "Centrifuged Titanium Ore", "Центрифугированная бедроковая руда (Титановая)", "ore_centrifuged", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_centrifuged_tungsten", "bedrock_ore:ore_centrifuged", "Centrifuged Tungsten Ore", "Центрифугированная бедроковая руда (Вольфрамовая)", "ore_centrifuged", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_centrifuged_gold", "bedrock_ore:ore_centrifuged", "Centrifuged Gold Ore", "Центрифугированная бедроковая руда (Золотая)", "ore_centrifuged", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_centrifuged_uranium", "bedrock_ore:ore_centrifuged", "Centrifuged Uranium Ore", "Центрифугированная бедроковая руда (Урановая)", "ore_centrifuged", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_centrifuged_thorium", "bedrock_ore:ore_centrifuged", "Centrifuged Thorium Ore", "Центрифугированная бедроковая руда (Ториевая)", "ore_centrifuged", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_centrifuged_chlorocalcite", "bedrock_ore:ore_centrifuged", "Centrifuged Chlorocalcite Ore", "Центрифугированная бедроковая руда (Хлоркальцитовая)", "ore_centrifuged", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_centrifuged_fluorite", "bedrock_ore:ore_centrifuged", "Centrifuged Fluorite Ore", "Центрифугированная бедроковая руда (Флюоритная)", "ore_centrifuged", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_centrifuged_hematite", "bedrock_ore:ore_centrifuged", "Centrifuged Hematite Ore", "Центрифугированная бедроковая руда (Гематитовая)", "ore_centrifuged", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_centrifuged_malachite", "bedrock_ore:ore_centrifuged", "Centrifuged Malachite Ore", "Центрифугированная бедроковая руда (Малахитовая)", "ore_centrifuged", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_centrifuged_neodymium", "bedrock_ore:ore_centrifuged", "Centrifuged Neodymium Ore", "Центрифугированная бедроковая руда (Неодимовая)", "ore_centrifuged", "ore_overlay", 0x8F8F5F, null, false, plain()));
        add(new Entry("ore_cleaned_iron", "bedrock_ore:ore_cleaned", "Cleaned Iron Ore", "Очищенная бедроковая руда (Железная)", "ore_cleaned", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_cleaned_copper", "bedrock_ore:ore_cleaned", "Cleaned Copper Ore", "Очищенная бедроковая руда (Медная)", "ore_cleaned", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_cleaned_borax", "bedrock_ore:ore_cleaned", "Cleaned Borax Ore", "Очищенная бедроковая руда (Буровая)", "ore_cleaned", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_cleaned_asbestos", "bedrock_ore:ore_cleaned", "Cleaned Asbestos Ore", "Очищенная бедроковая руда (Асбестовая)", "ore_cleaned", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_cleaned_niobium", "bedrock_ore:ore_cleaned", "Cleaned Niobium Ore", "Очищенная бедроковая руда (Ниобиевая)", "ore_cleaned", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_cleaned_titanium", "bedrock_ore:ore_cleaned", "Cleaned Titanium Ore", "Очищенная бедроковая руда (Титановая)", "ore_cleaned", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_cleaned_tungsten", "bedrock_ore:ore_cleaned", "Cleaned Tungsten Ore", "Очищенная бедроковая руда (Вольфрамовая)", "ore_cleaned", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_cleaned_gold", "bedrock_ore:ore_cleaned", "Cleaned Gold Ore", "Очищенная бедроковая руда (Золотая)", "ore_cleaned", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_cleaned_uranium", "bedrock_ore:ore_cleaned", "Cleaned Uranium Ore", "Очищенная бедроковая руда (Урановая)", "ore_cleaned", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_cleaned_thorium", "bedrock_ore:ore_cleaned", "Cleaned Thorium Ore", "Очищенная бедроковая руда (Ториевая)", "ore_cleaned", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_cleaned_chlorocalcite", "bedrock_ore:ore_cleaned", "Cleaned Chlorocalcite Ore", "Очищенная бедроковая руда (Хлоркальцитовая)", "ore_cleaned", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_cleaned_fluorite", "bedrock_ore:ore_cleaned", "Cleaned Fluorite Ore", "Очищенная бедроковая руда (Флюоритная)", "ore_cleaned", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_cleaned_hematite", "bedrock_ore:ore_cleaned", "Cleaned Hematite Ore", "Очищенная бедроковая руда (Гематитовая)", "ore_cleaned", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_cleaned_malachite", "bedrock_ore:ore_cleaned", "Cleaned Malachite Ore", "Очищенная бедроковая руда (Малахитовая)", "ore_cleaned", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_cleaned_neodymium", "bedrock_ore:ore_cleaned", "Cleaned Neodymium Ore", "Очищенная бедроковая руда (Неодимовая)", "ore_cleaned", "ore_overlay", 0x8F8F5F, null, false, plain()));
        add(new Entry("ore_separated_iron", "bedrock_ore:ore_separated", "Separated Iron Ore", "Отделённая Железная бедроковая руда", "ore_separated", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_separated_copper", "bedrock_ore:ore_separated", "Separated Copper Ore", "Отделённая Медная бедроковая руда", "ore_separated", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_separated_borax", "bedrock_ore:ore_separated", "Separated Borax Ore", "Отделённая Буровая бедроковая руда", "ore_separated", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_separated_asbestos", "bedrock_ore:ore_separated", "Separated Asbestos Ore", "Отделённая Асбестовая бедроковая руда", "ore_separated", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_separated_niobium", "bedrock_ore:ore_separated", "Separated Niobium Ore", "Отделённая Ниобиевая бедроковая руда", "ore_separated", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_separated_titanium", "bedrock_ore:ore_separated", "Separated Titanium Ore", "Отделённая Титановая бедроковая руда", "ore_separated", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_separated_tungsten", "bedrock_ore:ore_separated", "Separated Tungsten Ore", "Отделённая Вольфрамовая бедроковая руда", "ore_separated", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_separated_gold", "bedrock_ore:ore_separated", "Separated Gold Ore", "Отделённая Золотая бедроковая руда", "ore_separated", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_separated_uranium", "bedrock_ore:ore_separated", "Separated Uranium Ore", "Отделённая Урановая бедроковая руда", "ore_separated", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_separated_thorium", "bedrock_ore:ore_separated", "Separated Thorium Ore", "Отделённая Ториевая бедроковая руда", "ore_separated", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_separated_chlorocalcite", "bedrock_ore:ore_separated", "Separated Chlorocalcite Ore", "Отделённая Хлоркальцитовая бедроковая руда", "ore_separated", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_separated_fluorite", "bedrock_ore:ore_separated", "Separated Fluorite Ore", "Отделённая Флюоритная бедроковая руда", "ore_separated", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_separated_hematite", "bedrock_ore:ore_separated", "Separated Hematite Ore", "Отделённая Гематитовая бедроковая руда", "ore_separated", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_separated_malachite", "bedrock_ore:ore_separated", "Separated Malachite Ore", "Отделённая Малахитовая бедроковая руда", "ore_separated", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_separated_neodymium", "bedrock_ore:ore_separated", "Separated Neodymium Ore", "Отделённая Неодимовая бедроковая руда", "ore_separated", "ore_overlay", 0x8F8F5F, null, false, plain()));
        add(new Entry("ore_purified_iron", "bedrock_ore:ore_purified", "Purified Iron Ore", "Промытая Железная бедроковая руда", "ore_purified", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_purified_copper", "bedrock_ore:ore_purified", "Purified Copper Ore", "Промытая Медная бедроковая руда", "ore_purified", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_purified_borax", "bedrock_ore:ore_purified", "Purified Borax Ore", "Промытая Буровая бедроковая руда", "ore_purified", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_purified_asbestos", "bedrock_ore:ore_purified", "Purified Asbestos Ore", "Промытая Асбестовая бедроковая руда", "ore_purified", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_purified_niobium", "bedrock_ore:ore_purified", "Purified Niobium Ore", "Промытая Ниобиевая бедроковая руда", "ore_purified", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_purified_titanium", "bedrock_ore:ore_purified", "Purified Titanium Ore", "Промытая Титановая бедроковая руда", "ore_purified", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_purified_tungsten", "bedrock_ore:ore_purified", "Purified Tungsten Ore", "Промытая Вольфрамовая бедроковая руда", "ore_purified", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_purified_gold", "bedrock_ore:ore_purified", "Purified Gold Ore", "Промытая Золотая бедроковая руда", "ore_purified", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_purified_uranium", "bedrock_ore:ore_purified", "Purified Uranium Ore", "Промытая Урановая бедроковая руда", "ore_purified", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_purified_thorium", "bedrock_ore:ore_purified", "Purified Thorium Ore", "Промытая Ториевая бедроковая руда", "ore_purified", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_purified_chlorocalcite", "bedrock_ore:ore_purified", "Purified Chlorocalcite Ore", "Промытая Хлоркальцитовая бедроковая руда", "ore_purified", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_purified_fluorite", "bedrock_ore:ore_purified", "Purified Fluorite Ore", "Промытая Флюоритная бедроковая руда", "ore_purified", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_purified_hematite", "bedrock_ore:ore_purified", "Purified Hematite Ore", "Промытая Гематитовая бедроковая руда", "ore_purified", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_purified_malachite", "bedrock_ore:ore_purified", "Purified Malachite Ore", "Промытая Малахитовая бедроковая руда", "ore_purified", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_purified_neodymium", "bedrock_ore:ore_purified", "Purified Neodymium Ore", "Промытая Неодимовая бедроковая руда", "ore_purified", "ore_overlay", 0x8F8F5F, null, false, plain()));
        add(new Entry("ore_nitrated_iron", "bedrock_ore:ore_nitrated", "Nitrated Iron Ore", "Азотированная Железная бедроковая руда", "ore_nitrated", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_nitrated_copper", "bedrock_ore:ore_nitrated", "Nitrated Copper Ore", "Азотированная Медная бедроковая руда", "ore_nitrated", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_nitrated_borax", "bedrock_ore:ore_nitrated", "Nitrated Borax Ore", "Азотированная Буровая бедроковая руда", "ore_nitrated", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_nitrated_asbestos", "bedrock_ore:ore_nitrated", "Nitrated Asbestos Ore", "Азотированная Асбестовая бедроковая руда", "ore_nitrated", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_nitrated_niobium", "bedrock_ore:ore_nitrated", "Nitrated Niobium Ore", "Азотированная Ниобиевая бедроковая руда", "ore_nitrated", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_nitrated_titanium", "bedrock_ore:ore_nitrated", "Nitrated Titanium Ore", "Азотированная Титановая бедроковая руда", "ore_nitrated", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_nitrated_tungsten", "bedrock_ore:ore_nitrated", "Nitrated Tungsten Ore", "Азотированная Вольфрамовая бедроковая руда", "ore_nitrated", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_nitrated_gold", "bedrock_ore:ore_nitrated", "Nitrated Gold Ore", "Азотированная Золотая бедроковая руда", "ore_nitrated", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_nitrated_uranium", "bedrock_ore:ore_nitrated", "Nitrated Uranium Ore", "Азотированная Урановая бедроковая руда", "ore_nitrated", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_nitrated_thorium", "bedrock_ore:ore_nitrated", "Nitrated Thorium Ore", "Азотированная Ториевая бедроковая руда", "ore_nitrated", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_nitrated_chlorocalcite", "bedrock_ore:ore_nitrated", "Nitrated Chlorocalcite Ore", "Азотированная Хлоркальцитовая бедроковая руда", "ore_nitrated", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_nitrated_fluorite", "bedrock_ore:ore_nitrated", "Nitrated Fluorite Ore", "Азотированная Флюоритная бедроковая руда", "ore_nitrated", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_nitrated_hematite", "bedrock_ore:ore_nitrated", "Nitrated Hematite Ore", "Азотированная Гематитовая бедроковая руда", "ore_nitrated", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_nitrated_malachite", "bedrock_ore:ore_nitrated", "Nitrated Malachite Ore", "Азотированная Малахитовая бедроковая руда", "ore_nitrated", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_nitrated_neodymium", "bedrock_ore:ore_nitrated", "Nitrated Neodymium Ore", "Азотированная Неодимовая бедроковая руда", "ore_nitrated", "ore_overlay", 0x8F8F5F, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_iron", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Iron Ore", "Нитрокристаллическая Железная бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_copper", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Copper Ore", "Нитрокристаллическая Медная бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_borax", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Borax Ore", "Нитрокристаллическая Буровая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_asbestos", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Asbestos Ore", "Нитрокристаллическая Асбестовая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_niobium", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Niobium Ore", "Нитрокристаллическая Ниобиевая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_titanium", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Titanium Ore", "Нитрокристаллическая Титановая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_tungsten", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Tungsten Ore", "Нитрокристаллическая Вольфрамовая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_gold", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Gold Ore", "Нитрокристаллическая Золотая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_uranium", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Uranium Ore", "Нитрокристаллическая Урановая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_thorium", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Thorium Ore", "Нитрокристаллическая Ториевая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_chlorocalcite", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Chlorocalcite Ore", "Нитрокристаллическая Хлоркальцитовая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_fluorite", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Fluorite Ore", "Нитрокристаллическая Флюоритная бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_hematite", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Hematite Ore", "Нитрокристаллическая Гематитовая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_malachite", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Malachite Ore", "Нитрокристаллическая Малахитовая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_nitrocrystalline_neodymium", "bedrock_ore:ore_nitrocrystalline", "Nitrocrystalline Neodymium Ore", "Нитрокристаллическая Неодимовая бедроковая руда", "ore_nitrocrystalline", "ore_overlay", 0x8F8F5F, null, false, plain()));
        add(new Entry("ore_deepcleaned_iron", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Iron Ore", "Глубоко очищенная бедроковая руда (Железная)", "ore_deepcleaned", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_deepcleaned_copper", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Copper Ore", "Глубоко очищенная бедроковая руда (Медная)", "ore_deepcleaned", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_deepcleaned_borax", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Borax Ore", "Глубоко очищенная бедроковая руда (Буровая)", "ore_deepcleaned", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_deepcleaned_asbestos", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Asbestos Ore", "Глубоко очищенная бедроковая руда (Асбестовая)", "ore_deepcleaned", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_deepcleaned_niobium", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Niobium Ore", "Глубоко очищенная бедроковая руда (Ниобиевая)", "ore_deepcleaned", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_deepcleaned_titanium", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Titanium Ore", "Глубоко очищенная бедроковая руда (Титановая)", "ore_deepcleaned", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_deepcleaned_tungsten", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Tungsten Ore", "Глубоко очищенная бедроковая руда (Вольфрамовая)", "ore_deepcleaned", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_deepcleaned_gold", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Gold Ore", "Глубоко очищенная бедроковая руда (Золотая)", "ore_deepcleaned", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_deepcleaned_uranium", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Uranium Ore", "Глубоко очищенная бедроковая руда (Урановая)", "ore_deepcleaned", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_deepcleaned_thorium", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Thorium Ore", "Глубоко очищенная бедроковая руда (Ториевая)", "ore_deepcleaned", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_deepcleaned_chlorocalcite", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Chlorocalcite Ore", "Глубоко очищенная бедроковая руда (Хлоркальцитовая)", "ore_deepcleaned", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_deepcleaned_fluorite", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Fluorite Ore", "Глубоко очищенная бедроковая руда (Флюоритная)", "ore_deepcleaned", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_deepcleaned_hematite", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Hematite Ore", "Глубоко очищенная бедроковая руда (Гематитовая)", "ore_deepcleaned", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_deepcleaned_malachite", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Malachite Ore", "Глубоко очищенная бедроковая руда (Малахитовая)", "ore_deepcleaned", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_deepcleaned_neodymium", "bedrock_ore:ore_deepcleaned", "Deep Cleaned Neodymium Ore", "Глубоко очищенная бедроковая руда (Неодимовая)", "ore_deepcleaned", "ore_overlay", 0x8F8F5F, null, false, plain()));
        add(new Entry("ore_seared_iron", "bedrock_ore:ore_seared", "Seared Iron Ore", "Осушенная Железная бедроковая руда", "ore_seared", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_seared_copper", "bedrock_ore:ore_seared", "Seared Copper Ore", "Осушенная Медная бедроковая руда", "ore_seared", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_seared_borax", "bedrock_ore:ore_seared", "Seared Borax Ore", "Осушенная Буровая бедроковая руда", "ore_seared", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_seared_asbestos", "bedrock_ore:ore_seared", "Seared Asbestos Ore", "Осушенная Асбестовая бедроковая руда", "ore_seared", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_seared_niobium", "bedrock_ore:ore_seared", "Seared Niobium Ore", "Осушенная Ниобиевая бедроковая руда", "ore_seared", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_seared_titanium", "bedrock_ore:ore_seared", "Seared Titanium Ore", "Осушенная Титановая бедроковая руда", "ore_seared", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_seared_tungsten", "bedrock_ore:ore_seared", "Seared Tungsten Ore", "Осушенная Вольфрамовая бедроковая руда", "ore_seared", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_seared_gold", "bedrock_ore:ore_seared", "Seared Gold Ore", "Осушенная Золотая бедроковая руда", "ore_seared", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_seared_uranium", "bedrock_ore:ore_seared", "Seared Uranium Ore", "Осушенная Урановая бедроковая руда", "ore_seared", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_seared_thorium", "bedrock_ore:ore_seared", "Seared Thorium Ore", "Осушенная Ториевая бедроковая руда", "ore_seared", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_seared_chlorocalcite", "bedrock_ore:ore_seared", "Seared Chlorocalcite Ore", "Осушенная Хлоркальцитовая бедроковая руда", "ore_seared", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_seared_fluorite", "bedrock_ore:ore_seared", "Seared Fluorite Ore", "Осушенная Флюоритная бедроковая руда", "ore_seared", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_seared_hematite", "bedrock_ore:ore_seared", "Seared Hematite Ore", "Осушенная Гематитовая бедроковая руда", "ore_seared", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_seared_malachite", "bedrock_ore:ore_seared", "Seared Malachite Ore", "Осушенная Малахитовая бедроковая руда", "ore_seared", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_seared_neodymium", "bedrock_ore:ore_seared", "Seared Neodymium Ore", "Осушенная Неодимовая бедроковая руда", "ore_seared", "ore_overlay", 0x8F8F5F, null, false, plain()));
        add(new Entry("ore_enriched_iron", "bedrock_ore:ore_enriched", "Enriched Iron Ore", "Обогащённая Железная бедроковая руда", "ore_enriched", "ore_overlay", 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_enriched_copper", "bedrock_ore:ore_enriched", "Enriched Copper Ore", "Обогащённая Медная бедроковая руда", "ore_enriched", "ore_overlay", 0xEC9A63, null, false, plain()));
        add(new Entry("ore_enriched_borax", "bedrock_ore:ore_enriched", "Enriched Borax Ore", "Обогащённая Буровая бедроковая руда", "ore_enriched", "ore_overlay", 0xE4BE74, null, false, plain()));
        add(new Entry("ore_enriched_asbestos", "bedrock_ore:ore_enriched", "Enriched Asbestos Ore", "Обогащённая Асбестовая бедроковая руда", "ore_enriched", "ore_overlay", 0xBFBFB9, null, false, plain()));
        add(new Entry("ore_enriched_niobium", "bedrock_ore:ore_enriched", "Enriched Niobium Ore", "Обогащённая Ниобиевая бедроковая руда", "ore_enriched", "ore_overlay", 0xAF58D8, null, false, plain()));
        add(new Entry("ore_enriched_titanium", "bedrock_ore:ore_enriched", "Enriched Titanium Ore", "Обогащённая Титановая бедроковая руда", "ore_enriched", "ore_overlay", 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_enriched_tungsten", "bedrock_ore:ore_enriched", "Enriched Tungsten Ore", "Обогащённая Вольфрамовая бедроковая руда", "ore_enriched", "ore_overlay", 0x2C293C, null, false, plain()));
        add(new Entry("ore_enriched_gold", "bedrock_ore:ore_enriched", "Enriched Gold Ore", "Обогащённая Золотая бедроковая руда", "ore_enriched", "ore_overlay", 0xF9D738, null, false, plain()));
        add(new Entry("ore_enriched_uranium", "bedrock_ore:ore_enriched", "Enriched Uranium Ore", "Обогащённая Урановая бедроковая руда", "ore_enriched", "ore_overlay", 0x868D82, null, false, plain()));
        add(new Entry("ore_enriched_thorium", "bedrock_ore:ore_enriched", "Enriched Thorium Ore", "Обогащённая Ториевая бедроковая руда", "ore_enriched", "ore_overlay", 0x7D401D, null, false, plain()));
        add(new Entry("ore_enriched_chlorocalcite", "bedrock_ore:ore_enriched", "Enriched Chlorocalcite Ore", "Обогащённая Хлоркальцитовая бедроковая руда", "ore_enriched", "ore_overlay", 0xCDE036, null, false, plain()));
        add(new Entry("ore_enriched_fluorite", "bedrock_ore:ore_enriched", "Enriched Fluorite Ore", "Обогащённая Флюоритная бедроковая руда", "ore_enriched", "ore_overlay", 0xF6F3E7, null, false, plain()));
        add(new Entry("ore_enriched_hematite", "bedrock_ore:ore_enriched", "Enriched Hematite Ore", "Обогащённая Гематитовая бедроковая руда", "ore_enriched", "ore_overlay", 0xA37B72, null, false, plain()));
        add(new Entry("ore_enriched_malachite", "bedrock_ore:ore_enriched", "Enriched Malachite Ore", "Обогащённая Малахитовая бедроковая руда", "ore_enriched", "ore_overlay", 0x66B48C, null, false, plain()));
        add(new Entry("ore_enriched_neodymium", "bedrock_ore:ore_enriched", "Enriched Neodymium Ore", "Обогащённая Неодимовая бедроковая руда", "ore_enriched", "ore_overlay", 0x8F8F5F, null, false, plain()));
        // ── ItemByproduct (ore_byproduct, creativeTab null): Kristallfragmente, ganz eingefaerbt ──
        add(new Entry("ore_byproduct_b_iron", "ore_byproduct", "Crystalline Iron Fragment", "Железный побочный продукт", "byproduct", null, 0xE2C0AA, null, false, plain()));
        add(new Entry("ore_byproduct_b_copper", "ore_byproduct", "Crystalline Copper Fragment", "Медный побочный продукт", "byproduct", null, 0xEC9A63, null, false, plain()));
        add(new Entry("ore_byproduct_b_lithium", "ore_byproduct", "Crystalline Lithium Fragment", "Литиевый побочный продукт", "byproduct", null, 0xEDEDED, null, false, plain()));
        add(new Entry("ore_byproduct_b_silicon", "ore_byproduct", "Crystalline Silicon Fragment", "Кремниевый побочный продукт", "byproduct", null, 0xFFFBD1, null, false, plain()));
        add(new Entry("ore_byproduct_b_lead", "ore_byproduct", "Crystalline Lead Fragment", "Свинцовый побочный продукт", "byproduct", null, 0x646470, null, false, plain()));
        add(new Entry("ore_byproduct_b_titanium", "ore_byproduct", "Crystalline Titanium Fragment", "Титановый побочный продукт", "byproduct", null, 0xF2EFE2, null, false, plain()));
        add(new Entry("ore_byproduct_b_aluminium", "ore_byproduct", "Crystalline Aluminium Fragment", "Алюминиевый побочный продукт", "byproduct", null, 0xE8F2F9, null, false, plain()));
        add(new Entry("ore_byproduct_b_sulfur", "ore_byproduct", "Crystalline Sulfur Fragment", "Серный побочный продукт", "byproduct", null, 0xEAD377, null, false, plain()));
        add(new Entry("ore_byproduct_b_calcium", "ore_byproduct", "Crystalline Calcium Fragment", "Кальциевый побочный продукт", "byproduct", null, 0xCFCFA6, null, false, plain()));
        add(new Entry("ore_byproduct_b_bismuth", "ore_byproduct", "Crystalline Bismuth Fragment", "Висмутовый побочный продукт", "byproduct", null, 0x8D8577, null, false, plain()));
        add(new Entry("ore_byproduct_b_radium", "ore_byproduct", "Crystalline Radium Fragment", "Радиевый побочный продукт", "byproduct", null, 0xE9FAF6, null, false, plain()));
        add(new Entry("ore_byproduct_b_technetium", "ore_byproduct", "Crystalline Technetium Fragment", "Технециевый побочный продукт", "byproduct", null, 0xCADFDF, null, false, plain()));
        add(new Entry("ore_byproduct_b_polonium", "ore_byproduct", "Crystalline Polonium Fragment", "Полониевый побочный продукт", "byproduct", null, 0xCADFDF, null, false, plain()));
        add(new Entry("ore_byproduct_b_uranium", "ore_byproduct", "Crystalline Uranium Fragment", "Урановый побочный продукт", "byproduct", null, 0x868D82, null, false, plain()));
    }

    // =====================================================================================
    //  Регистрация и доступ
    // =====================================================================================

    /** Вызывается один раз из static-блока ModItems (после ModMaterialItems.registerAll). */
    public static void registerAll() {
        for (Entry e : ENTRIES) {
            if (ITEMS.containsKey(e.id)) continue; // повторный static-init датагеном не должен падать
            ITEMS.put(e.id, ModItems.ITEMS.register(e.id, () -> e.factory.create(new Item.Properties())));
        }
    }

    public static List<Entry> entries() { return List.copyOf(ENTRIES); }

    public static RegistrySupplier<Item> get(String id) { return ITEMS.get(id); }

    /** Развёрнутый Item или null (до прохождения регистрации не должен падать). */
    public static Item itemOrNull(String id) {
        RegistrySupplier<Item> sup = ITEMS.get(id);
        return sup != null && sup.isPresent() ? sup.get() : null;
    }

    /** Предметы группы в порядке таблицы (только уже зарегистрированные). */
    public static List<Item> group(String group) {
        List<Item> out = new ArrayList<>();
        for (Entry e : ENTRIES) {
            if (e.group.equals(group) || e.group.startsWith(group + ":")) {
                RegistrySupplier<Item> sup = ITEMS.get(e.id);
                if (sup != null && sup.isPresent()) out.add(sup.get());
            }
        }
        return out;
    }

    /** Тинт предмета для ClientSetup (ItemColor): двухслойные тинтуруются на layer1. */
    public static int tintFor(Item item, int tintIndex) {
        for (Entry e : ENTRIES) {
            RegistrySupplier<Item> sup = ITEMS.get(e.id);
            if (sup == null || !sup.isPresent() || sup.get() != item) continue;
            if (e.tint == 0) break;
            return (e.layer1 != null && tintIndex != 1) ? 0xFFFFFF : e.tint;
        }
        return 0xFFFFFF;
    }

    /** Все предметы с тинтом (для регистрации ItemColor в ClientSetup). */
    public static Item[] tintedItems() {
        List<Item> out = new ArrayList<>();
        for (Entry e : ENTRIES) {
            if (e.tint == 0) continue;
            RegistrySupplier<Item> sup = ITEMS.get(e.id);
            if (sup != null && sup.isPresent()) out.add(sup.get());
        }
        return out.toArray(new Item[0]);
    }
}
