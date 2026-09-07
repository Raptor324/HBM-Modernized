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
        return props -> {
            List<Component> lines = new ArrayList<>();
            if (italicLore != null) {
                lines.add(Component.literal(italicLore).withStyle(ChatFormatting.ITALIC));
            }
            if (cooling) {
                lines.add(Component.translatable("tooltip.hbm_m.waste_cooling.desc").withStyle(ChatFormatting.GOLD));
            }
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
        };
        for (String[] w : waste) {
            add(new Entry(w[0] + "_cooling", "waste_cooling:" + w[4],
                    w[1], w[2], w[3], null, 0xFFBFA5, null, true, wasteLore(null, true)));
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
                    "nuclear_waste_long", null, 0, iso[1], false, wasteLore(iso[1], false)));
        }
        // 5000 nuclear_waste_long_tiny — 5 мет
        for (String[] iso : longIso) {
            add(new Entry("nuclear_waste_long_tiny_" + iso[0], "nw_long_tiny",
                    "Tiny Pile of Long-Lived Nuclear Waste", "Кучка долгоживущих ядерных отходов",
                    "nuclear_waste_long_tiny", null, 0, iso[1], false, wasteLore(iso[1], false)));
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
                    "nuclear_waste_long_depleted", null, 0, iso[1], false, wasteLore(iso[1], false)));
        }
        // 5004 nuclear_waste_long_depleted_tiny — 5 мет
        for (String[] iso : longIso) {
            add(new Entry("nuclear_waste_long_depleted_tiny_" + iso[0], "nw_long_dep_tiny",
                    "Tiny Pile of Decayed Long-Lived Nuclear Waste", "Кучка разложившихся долгоживущих ядерных отходов",
                    "nuclear_waste_long_depleted_tiny", null, 0, iso[1], false, wasteLore(iso[1], false)));
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
