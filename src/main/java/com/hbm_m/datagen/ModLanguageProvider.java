package com.hbm_m.datagen;

// Провайдер генерации локализаций (переводов) для мода.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.lib.RefStrings;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hbm_m.block.ModBlocks.ENABLED_INGOT_BLOCKS;
import static com.hbm_m.block.ModBlocks.getIngotBlock;

public class ModLanguageProvider extends LanguageProvider {
    // 1. Создаем НАШЕ СОБСТВЕННОЕ поле для хранения языка
    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, RefStrings.MODID, locale);
        // 2. Сохраняем язык в наше поле при создании объекта
        this.locale = locale;
    }

    private void addIngotPowderTranslations(Set<ResourceLocation> translatedPowders) {
        for (ModIngots ingot : ModIngots.values()) {
            if (ModItems.getPowder(ingot) != null) {
                var powder = ModItems.getPowder(ingot);
                if (!translatedPowders.contains(powder.getId())) {
                    add(powder.get(), buildPowderName(ingot, false));
                }
            }
            ModItems.getTinyPowder(ingot).ifPresent(tiny ->
                    add(tiny.get(), buildPowderName(ingot, true)));
        }

        if ("ru_ru".equals(this.locale)) {
            add(ModItems.DUST.get(), "Пыль");
            add(ModItems.DUST_TINY.get(), "Малая кучка пыли");
        } else {
            add(ModItems.DUST.get(), "Dust");
            add(ModItems.DUST_TINY.get(), "Tiny Dust");
        }
    }

    private String buildPowderName(ModIngots ingot, boolean tiny) {
        String base = ingot.getTranslation(this.locale);
        if (base == null || base.isBlank()) {
            base = formatName(ingot.getName());
        }

        String result = base;
        if ("ru_ru".equals(this.locale)) {
            String replaced = result.replace("Слиток", "Порошок").replace("слиток", "порошок");
            if (replaced.equals(result)) {
                replaced = "Порошок " + result;
            }
            result = replaced.trim();
            if (tiny) {
                result = "Малая кучка " + result;
            }
        } else {
            String replaced = result.replace("Ingot", "Powder").replace("ingot", "powder");
            if (replaced.equals(result)) {
                replaced = result + " Powder";
            }
            result = replaced.trim();
            if (tiny) {
                result = "Tiny " + result;
            }
        }

        return result;
    }

    private String formatName(String name) {
        return Arrays.stream(name.replace('.', '_').split("_"))
                .filter(part -> !part.isEmpty())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }
    private void addIngotBlockTranslations(Set<ResourceLocation> translatedBlocks) {
        for (ModIngots ingot : ModIngots.values()) {
            if (ENABLED_INGOT_BLOCKS.contains(ingot.getName())) {
                RegistryObject<Block> block = getIngotBlock(ingot);
                if (block != null && !translatedBlocks.contains(block.getId())) {
                    add(block.get(), buildBlockName(ingot));
                }
            }
        }
        // Можно добавить перевод для русской локали по умолчанию для общих блоков, если нужно
    }

    // Метод формирования имени блока с переводом
    private String buildBlockName(ModIngots ingot) {
        String base = ingot.getTranslation(this.locale);
        if (base == null || base.isBlank()) {
            base = formatName(ingot.getName());
        }

        if ("ru_ru".equals(this.locale)) {
            // Для русского языка заменяем "слиток" на "блок", либо добавляем приставку "Блок"
            String replaced = base.replace("Слиток", "Блок").replace("слиток", "блок");
            if (replaced.equals(base)) {
                replaced = "Блок " + base;
            }
            return replaced.trim();
        } else {
            // Для английского - добавляем приставку "Block" или заменяем "Ingot" на "Block"
            String replaced = base.replace("Ingot", "Block").replace("ingot", "block");
            if (replaced.equals(base)) {
                replaced = base + " Block";
            }
            return replaced.trim();
        }
    }
    @Override
    protected void addTranslations() {
        // АВТОМАТИЧЕСКАЯ ЛОКАЛИЗАЦИЯ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> ingotItem = ModItems.getIngot(ingot);
            if (ingotItem != null && ingotItem.isPresent()) {
                String translation = ingot.getTranslation(this.locale);
                if (translation != null) {
                    add(ingotItem.get(), translation);
                }
            }
        }

        Set<ResourceLocation> translatedPowders = new HashSet<>();

        // АВТОМАТИЧЕСКАЯ ЛОКАЛИЗАЦИЯ ПОРОШКОВ
        for (ModPowders powders : ModPowders.values()) {
            RegistryObject<Item> powderItem = ModItems.getPowders(powders);
            if (powderItem != null && powderItem.isPresent()) {
                String translation = powders.getTranslation(this.locale);
                if (translation != null) {
                    add(powderItem.get(), translation);
                    translatedPowders.add(powderItem.getId());
                }
            }
        }

        // ДОБАВЛЕНИЕ ЛОКАЛИЗАЦИИ ДЛЯ ПОРОШКОВ ИЗ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            RegistryObject<Item> powder = ModItems.getPowder(ingot);
            if (powder != null && powder.isPresent() && !translatedPowders.contains(powder.getId())) {
                add(powder.get(), buildPowderName(ingot, false));
            }
            ModItems.getTinyPowder(ingot).ifPresent(tiny -> {
                if (tiny != null && tiny.isPresent()) {
                    add(tiny.get(), buildPowderName(ingot, true));
                }
            });
        }



    // ЯВНАЯ ЛОКАЛИЗАЦИЯ ДЛЯ ОСТАЛЬНЫХ КЛЮЧЕЙ
        switch (this.locale) {
            case "ru_ru":
                // КРЕАТИВНЫЕ ВКЛАДКИ
                add("itemGroup.hbm_m.ntm_resources_tab", "Слитки и ресурсы NTM");
                add("itemGroup.hbm_m.ntm_fuel_tab", "Топливо и элементы механизмов NTM");
                add("itemGroup.hbm_m.ntm_templates_tab", "Шаблоны и штампы NTM");
                add("itemGroup.hbm_m.ntm_ores_tab", "Руды и блоки NTM");
                add("itemGroup.hbm_m.ntm_building_tab", "Строительные блоки NTM");
                add("itemGroup.hbm_m.ntm_machines_tab", "Механизмы и станки NTM");
                add("itemGroup.hbm_m.ntm_instruments_tab", "Броня и инструменты NTM");
                add("itemGroup.hbm_m.ntm_spareparts_tab", "Запчасти NTM");
                add("itemGroup.hbm_m.ntm_bombs_tab", "Бомбы NTM");
                add("itemGroup.hbm_m.ntm_missiles_tab", "Ракеты и спутники NTM");
                add("itemGroup.hbm_m.ntm_weapons_tab", "Оружие и турели NTM");
                add("itemGroup.hbm_m.ntm_consumables_tab", "Расходники и снаряжение NTM");
                
                // СНАРЯГА
                add("item.hbm_m.alloy_sword", "Меч из продвинутого сплава");
                add("item.hbm_m.alloy_pickaxe", "Кирка из продвинутого сплава");
                add("item.hbm_m.alloy_axe", "Топор из продвинутого сплава");
                add("item.hbm_m.alloy_hoe", "Мотыга из продвинутого сплава");
                add("item.hbm_m.alloy_shovel", "Лопата из продвинутого сплава");

                add("item.hbm_m.steel_sword", "Стальной меч");
                add("item.hbm_m.steel_pickaxe", "Стальная кирка");
                add("item.hbm_m.steel_axe", "Стальной топор");
                add("item.hbm_m.steel_hoe", "Стальная мотыга");
                add("item.hbm_m.steel_shovel", "Стальная лопата");

                add("item.hbm_m.titanium_sword", "Титановый меч");
                add("item.hbm_m.titanium_pickaxe", "Титановая кирка");
                add("item.hbm_m.titanium_axe", "Титановый топор");
                add("item.hbm_m.titanium_hoe", "Титановая мотыга");
                add("item.hbm_m.titanium_shovel", "Титановая лопата");

                add("item.hbm_m.starmetal_sword", "Меч из звёздного металла");
                add("item.hbm_m.starmetal_pickaxe", "Кирка из звёздного металла");
                add("item.hbm_m.starmetal_axe", "Топор из звёздного металла");
                add("item.hbm_m.starmetal_hoe", "Мотыга из звёздного металла");
                add("item.hbm_m.starmetal_shovel", "Лопата из звёздного металла");

                add("gui.hbm_m.energy", "Энергия: %s/%s HE");
                add("gui.hbm_m.shredder.blade_warning.title", "Нет лезвий!");
                add("gui.hbm_m.shredder.blade_warning.desc", "Установите или отремонтируйте лезвия шреддера.");
                // БРОНЯ
                add("item.hbm_m.alloy_helmet", "Шлем из продвинутого сплава");
                add("item.hbm_m.alloy_chestplate", "Нагрудник из продвинутого сплава");
                add("item.hbm_m.alloy_leggings", "Поножи из продвинутого сплава");
                add("item.hbm_m.alloy_boots", "Ботинки из продвинутого сплава");

                add("item.hbm_m.cobalt_helmet", "Кобальтовый шлем");
                add("item.hbm_m.cobalt_chestplate", "Кобальтовый нагрудник");
                add("item.hbm_m.cobalt_leggings", "Кобальтовые поножи");
                add("item.hbm_m.cobalt_boots", "Кобальтовые ботинки");

                add("item.hbm_m.titanium_helmet", "Титановый шлем");
                add("item.hbm_m.titanium_chestplate", "Титановый нагрудник");
                add("item.hbm_m.titanium_leggings", "Титановые поножи");
                add("item.hbm_m.titanium_boots", "Титановые ботинки");

                add("item.hbm_m.security_helmet", "Шлем охранника");
                add("item.hbm_m.security_chestplate", "Нагрудник охранника");
                add("item.hbm_m.security_leggings", "Поножи охранника");
                add("item.hbm_m.security_boots", "Ботинки охранника");

                add("item.hbm_m.ajr_helmet", "Шлем Стальных Рейнджеров");
                add("item.hbm_m.ajr_chestplate", "Нагрудник Стальных Рейнджеров");
                add("item.hbm_m.ajr_leggings", "Поножи Стальных Рейнджеров");
                add("item.hbm_m.ajr_boots", "Ботинки Стальных Рейнджеров");

                add("item.hbm_m.steel_helmet", "Стальной шлем");
                add("item.hbm_m.steel_chestplate", "Стальной нагрудник");
                add("item.hbm_m.steel_leggings", "Стальные поножи");
                add("item.hbm_m.steel_boots", "Стальные ботинки");

                add("item.hbm_m.asbestos_helmet", "Огнезащитный шлем");
                add("item.hbm_m.asbestos_chestplate", "Огнезащитный нагрудник");
                add("item.hbm_m.asbestos_leggings", "Огнезащитные поножи");
                add("item.hbm_m.asbestos_boots", "Огнезащитные ботинки");

                add("item.hbm_m.hazmat_helmet", "Защитный шлем");
                add("item.hbm_m.hazmat_chestplate", "Защитный нагрудник");
                add("item.hbm_m.hazmat_leggings", "Защитные поножи");
                add("item.hbm_m.hazmat_boots", "Защитные ботинки");

                add("item.hbm_m.liquidator_helmet", "Шлем костюма Ликвидатора");
                add("item.hbm_m.liquidator_chestplate", "Нагрудник костюма Ликвидатора");
                add("item.hbm_m.liquidator_leggings", "Поножи костюма Ликвидатора");
                add("item.hbm_m.liquidator_boots", "Ботинки костюма Ликвидатора");

                add("item.hbm_m.paa_helmet", "Боевой защитный шлем PaA");
                add("item.hbm_m.paa_chestplate", "Защищающая нагрудная пластина из PaA");
                add("item.hbm_m.paa_leggings", "Укреплённые поножи из PaA");
                add("item.hbm_m.paa_boots", "''Старые добрые ботинки'' из PaA");

                add("item.hbm_m.starmetal_helmet", "Шлем из звёздного металла");
                add("item.hbm_m.starmetal_chestplate", "Нагрудник из звёздного металла");
                add("item.hbm_m.starmetal_leggings", "Поножи из звёздного металла");
                add("item.hbm_m.starmetal_boots", "Ботинки из звёздного металла");

                add("item.hbm_m.geiger_counter", "Счетчик Гейгера");
                add("item.hbm_m.dosimeter", "Дозиметр");
                add("item.hbm_m.battery_creative", "Бесконечная батарейка");
                add("tooltip.hbm_m.creative_battery_desc","Предоставляет бесконечное количество энергии");
                add("tooltip.hbm_m.creative_battery_flavor","Бесконечность — не предел!!");
                add(ModItems.COIL_TUNGSTEN.get(), "Нагревательный элемент");
                // ПРЕДМЕТЫ
                add(ModItems.BATTERY_POTATO.get(), "Картофельная батарейка");
                add(ModItems.BATTERY.get(), "Батарейка");
                add(ModItems.BATTERY_RED_CELL.get(), "Красная энергоячейка");
                add(ModItems.BATTERY_RED_CELL_6.get(), "Красная энергоячейка x6");
                add(ModItems.BATTERY_RED_CELL_24.get(), "Красная энергоячейка x24");
                add(ModItems.BATTERY_ADVANCED.get(), "Продвинутая батарея");
                add(ModItems.BATTERY_ADVANCED_CELL.get(), "Продвинутая энергоячейка");
                add(ModItems.BATTERY_ADVANCED_CELL_4.get(), "Продвинутая энергоячейка x4");
                add(ModItems.BATTERY_ADVANCED_CELL_12.get(), "Продвинутая энергоячейка x12");
                add(ModItems.BATTERY_LITHIUM.get(), "Литиевая батарея");
                add(ModItems.BATTERY_LITHIUM_CELL.get(), "Литиевая энергоячейка");
                add(ModItems.BATTERY_LITHIUM_CELL_3.get(), "Литиевая энергоячейка x3");
                add(ModItems.BATTERY_LITHIUM_CELL_6.get(), "Литиевая энергоячейка x6");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL.get(), "Шрабидиевая энергоячейка");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL_2.get(), "Шрабидиевая энергоячейка x2");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL_4.get(), "Шрабидиевая энергоячейка x4");
                add(ModItems.BATTERY_SPARK.get(), "Спарк батарея");
                add(ModItems.BATTERY_TRIXITE.get(), "Нефритовый стержень спарк батарей оригинал");
                add(ModItems.BATTERY_SPARK_CELL_6.get(), "Спарк энергоячейка");
                add(ModItems.BATTERY_SPARK_CELL_25.get(), "Спарк магический аккумулятор");
                add(ModItems.BATTERY_SPARK_CELL_100.get(), "Спарк магический массив хранения энергии");
                add(ModItems.BATTERY_SPARK_CELL_1000.get(), "Спарк магическая масс-энергитическая пустота");
                add(ModItems.BATTERY_SPARK_CELL_2500.get(), "Спарк магическое море Дирака");
                add(ModItems.BATTERY_SPARK_CELL_10000.get(), "Устойчивый пространственно-временной спарк кристалл");
                add(ModItems.BATTERY_SPARK_CELL_POWER.get(), "Абсурдный физический спарк блок накопления энергии");

                add(ModItems.WIRE_RED_COPPER.get(), "Провод из красной меди");
                add(ModItems.WIRE_COPPER.get(), "Медный провод");
                add(ModItems.WIRE_ALUMINIUM.get(), "Алюминиевый провод");
                add(ModItems.WIRE_GOLD.get(), "Золотой провод");
                add(ModItems.WIRE_TUNGSTEN.get(), "Вольфрамовый провод");
                add(ModItems.WIRE_MAGNETIZED_TUNGSTEN.get(), "Провод из намагниченного вольфрама");
                add(ModItems.WIRE_FINE.get(), "Железный провод");
                add(ModItems.WIRE_CARBON.get(), "Провод из свинца");
                add(ModItems.WIRE_SCHRABIDIUM.get(), "Шрабидиевый провод");
                add(ModItems.WIRE_ADVANCED_ALLOY.get(), "Провод из продвинутого сплава");

                add(ModItems.BATTERY_SCHRABIDIUM.get(), "Шрабидиевая батарейка");

                add(ModItems.STAMP_STONE_FLAT.get(), "Плоский каменный штамп");
                add(ModItems.STAMP_STONE_PLATE.get(), "Каменный штамп пластины");
                add(ModItems.STAMP_STONE_WIRE.get(), "Каменный штамп провода");
                add(ModItems.STAMP_STONE_CIRCUIT.get(), "Каменный штамп чипа");
                add(ModItems.STAMP_IRON_FLAT.get(), "Плоский железный штамп");
                add(ModItems.STAMP_IRON_PLATE.get(), "Железный штамп пластины");
                add(ModItems.STAMP_IRON_WIRE.get(), "Железный штамп провода");
                add(ModItems.STAMP_IRON_CIRCUIT.get(), "Железный штамп чипа");
                add(ModItems.STAMP_STEEL_FLAT.get(), "Плоский стальной штамп");
                add(ModItems.STAMP_STEEL_PLATE.get(), "Стальной штамп пластины");
                add(ModItems.STAMP_STEEL_WIRE.get(), "Стальной штамп провода");
                add(ModItems.STAMP_STEEL_CIRCUIT.get(), "Стальной штамп чипа");
                add(ModItems.STAMP_TITANIUM_FLAT.get(), "Плоский титановый штамп");
                add(ModItems.STAMP_TITANIUM_PLATE.get(), "Титановый штамп пластины");
                add(ModItems.STAMP_TITANIUM_WIRE.get(), "Титановый штамп провода");
                add(ModItems.STAMP_TITANIUM_CIRCUIT.get(), "Титановый штамп чипа");
                add(ModItems.STAMP_OBSIDIAN_FLAT.get(), "Плоский обсидиановый штамп");
                add(ModItems.STAMP_OBSIDIAN_PLATE.get(), "Обсидиановый штамп пластины");
                add(ModItems.STAMP_OBSIDIAN_WIRE.get(), "Обсидиановый штамп провода");
                add(ModItems.STAMP_OBSIDIAN_CIRCUIT.get(), "Обсидиановый штамп чипа");
                add(ModItems.STAMP_DESH_FLAT.get(), "Плоский деш штамп");
                add(ModItems.STAMP_DESH_PLATE.get(), "Деш штамп пластины");
                add(ModItems.STAMP_DESH_WIRE.get(), "Деш штамп провода");
                add(ModItems.STAMP_DESH_CIRCUIT.get(), "Деш штамп чипа");
                add(ModItems.STAMP_DESH_9.get(), "Деш штамп 9мм");
                add(ModItems.STAMP_DESH_44.get(), "Деш штамп .44 Magnum");
                add(ModItems.STAMP_DESH_50.get(), "Деш штамп .50 BMG");
                add(ModItems.STAMP_DESH_357.get(), "Деш штамп .357 Magnum");
                add(ModItems.STAMP_IRON_357.get(), "Железный штамп .357 Magnum");
                add(ModItems.STAMP_IRON_44.get(), "Железный штамп .44 Magnum");
                add(ModItems.STAMP_IRON_50.get(), "Железный штамп .50 BMG");
                add(ModItems.STAMP_IRON_9.get(), "Железный штамп 9мм");




                add("item.hbm_m.heart_piece", "Частичка сердца");
                add(ModItems.HEART_CONTAINER.get(), "Контейнер для сердца");
                add(ModItems.HEART_BOOSTER.get(), "Усилитель сердца");
                add(ModItems.HEART_FAB.get(), "Фаб-сердце");
                add(ModItems.BLACK_DIAMOND.get(), "Черный алмаз");
                add(ModBlocks.SMOKE_BOMB.get(), "Семтекс");
                add(ModItems.TEMPLATE_FOLDER.get(), "Папка шаблонов машин");
                add(ModItems.ASSEMBLY_TEMPLATE.get(), "Шаблон сборочной машины: %s");
                add("tooltip.hbm_m.template_broken", "Шаблон сломан!");
                add("tooltip.hbm_m.created_with_template_folder", "Создано с помощью Папки шаблонов машин");
                add("tooltip.hbm_m.output", "Выход: ");
                add("tooltip.hbm_m.input", "Вход: ");
                add("tooltip.hbm_m.production_time", "Время производства: ");
                add("tooltip.hbm_m.seconds", "секунд");
                add("tooltip.hbm_m.energy_consumption", "Потребление энергии:");
                add("tooltip.hbm_m.tags", "Теги (OreDict):");
                add("item.hbm_m.template_folder.desc", "Шаблоны машин: Бумага + Краситель$Идентификатор: Железная пластина + Краситель$Штамп для пресса: Плоский штамп$Трек сирены: Изолятор + Стальная пластина");
                add("desc.gui.template", "Вставьте сборочный шаблон");
                add("desc.gui.assembler.warning", "Некорректный шаблон!");
                // === ИНСТРУМЕНТЫ И УСТРОЙСТВА ===
                add("tooltip.hbm_m.gigadet.line1", "Был создан по приколу");
                add("tooltip.hbm_m.nuclear_charge.line1", "Ядерное оружие высокой мощности!");
                add("tooltip.hbm_m.nuclear_charge.line2", "На данный момент, это самый");
                add("tooltip.hbm_m.nuclear_charge.line3", "разрушительный блок в нашем моде");
                add("tooltip.hbm_m.nuclear_charge.line4", "Если кратер загрузился некорректно");
                add("tooltip.hbm_m.nuclear_charge.line5", "или без биомов, то перезапустите мир");

                add("tooltip.hbm_m.detminer.line1", "Не наносит урон сущностям и игрокам");
                add("tooltip.hbm_m.detminer.line4", "Позволяет добывать глубинные руды и камень");

                add("tooltip.hbm_m.dudnuke.line1", "Ядерное оружие высокой мощности!");
                add("tooltip.hbm_m.dudnuke.line4", "Если кратер загрузился некорректно");
                add("tooltip.hbm_m.dudnuke.line5", "или без биомов, то перезапустите мир");
                add("tooltip.hbm_m.dudnuke.line6", "Может быть обезврежена");

                add("tooltip.hbm_m.dudsalted.line1", "Ядерное оружие высокой мощности!");
                add("tooltip.hbm_m.dudsalted.line4", "Если кратер загрузился некорректно");
                add("tooltip.hbm_m.dudsalted.line5", "или без биомов, то перезапустите мир");
                add("tooltip.hbm_m.dudsalted.line6", "Может быть обезврежена");

                add("tooltip.hbm_m.dudfugas.line1", "Фугасная бомба высокой мощности!");
                add("tooltip.hbm_m.dudfugas.line6", "Может быть обезврежена");

                add("tooltip.hbm_m.defuser.line1", "Устройство для обезвреживания мин и бомб");

                add("tooltip.hbm_m.crowbar.line1", "Инструмент для вскрытия контейнеров");
                add("tooltip.hbm_m.crowbar.line2", "Открывает ящики по нажатию ПКМ");

                add("tooltip.hbm_m.mine_nuke.line1", "Ядерное оружие!");
                add("tooltip.hbm_m.mine_nuke.line2", "Радиус поражения: 35 метров");
                add("tooltip.hbm_m.mine_nuke.line3", "Может быть обезврежена");

                add("tooltip.hbm_m.mine.line1", "Может быть обезврежена");

// ДЕТОНАТОР
                add("tooltip.hbm_m.detonator.target", "Цель: ");
                add("tooltip.hbm_m.detonator.no_target", "Нет цели");
                add("tooltip.hbm_m.detonator.right_click", "ПКМ - активировать");
                add("tooltip.hbm_m.detonator.shift_right_click", "Shift+ПКМ - установить");

// СКАНЕР КЛАСТЕРОВ
                add("tooltip.hbm_m.depth_ores_scanner.scans_chunks", "Сканирует чанки в поисках");
                add("tooltip.hbm_m.depth_ores_scanner.deep_clusters", "глубинных кластеров под игроком");
                add("tooltip.hbm_m.depth_ores_scanner.depth_warning", "Работает на глубине -30 и ниже!");
                // DEPTH ORES SCANNER (сообщения)
                add("message.hbm_m.depth_ores_scanner.invalid_height", "Сканер работает только на высоте -30 или ниже!");
                add("message.hbm_m.depth_ores_scanner.directly_below", "Глубинный кластер прямо под нами!");
                add("message.hbm_m.depth_ores_scanner.in_chunk", "В нашем чанке обнаружен глубинный кластер!");
                add("message.hbm_m.depth_ores_scanner.adjacent_chunk", "В соседнем чанке обнаружен глубинный кластер!");
                add("message.hbm_m.depth_ores_scanner.none_found", "Не обнаружено глубинных кластеров поблизости");

// MULTI DETONATOR TOOLTIPS
                add("tooltip.hbm_m.multi_detonator.active_point", "➤ %s:");
                add("tooltip.hbm_m.multi_detonator.point_set", "✅ %s:");
                add("tooltip.hbm_m.multi_detonator.coordinates", "   %d, %d, %d");
                add("tooltip.hbm_m.multi_detonator.point_empty", "○ Точка %d:");
                add("tooltip.hbm_m.multi_detonator.not_set", "   Не установлена");
                add("tooltip.hbm_m.multi_detonator.key_r", "R - открыть меню");
                add("tooltip.hbm_m.multi_detonator.shift_rmb", "Shift+ПКМ - сохранить в активную точку");
                add("tooltip.hbm_m.multi_detonator.rmb_activate", "ПКМ - активировать активную точку");

// MULTI DETONATOR MESSAGES
                add("message.hbm_m.multi_detonator.position_saved", "Позиция '%s' сохранена: %d, %d, %d");
                add("message.hbm_m.multi_detonator.no_coordinates", "Нет заданных координат!");
                add("message.hbm_m.multi_detonator.point_not_set", "Точка %d не установлена!");
                add("message.hbm_m.multi_detonator.chunk_not_loaded", "Позиция не загружена!");
                add("message.hbm_m.multi_detonator.activated", "%s активирован!");
                add("message.hbm_m.multi_detonator.activation_error", "Ошибка при активации!");
                add("message.hbm_m.multi_detonator.incompatible_block", "Блок несовместим!");





// ДЕТЕКТОР НЕФТИ (тултип)
                add("tooltip.hbm_m.oil_detector.scans_chunks", "Сканирует чанки в поисках");
                add("tooltip.hbm_m.oil_detector.oil_deposits", "нефтяных залеж под игроком");

// ДЕТЕКТОР НЕФТИ (сообщения использования)
                add("message.hbm_m.oil_detector.directly_below", "Залежи нефти прямо под нами!");
                add("message.hbm_m.oil_detector.in_chunk", "В нашем чанке обнаружена нефть!");
                add("message.hbm_m.oil_detector.adjacent_chunk", "В соседнем чанке обнаружены залежи нефти!");
                add("message.hbm_m.oil_detector.none_found", "Не обнаружено залежь нефти поблизости");

                // RANGE DETONATOR
                add("tooltip.hbm_m.range_detonator.desc", "Активирует совместимые блоки");
                add("tooltip.hbm_m.range_detonator.hint", "по лучу до 256 блоков.");
                add("message.hbm_m.range_detonator.pos_not_loaded", "Позиция несовместима или не прогружена");
                add("message.hbm_m.range_detonator.activated", "Успешно активировано");

                add("tooltip.hbm_m.grenade_nuc.line1", "Ядерное оружие!");
                add("tooltip.hbm_m.grenade_nuc.line2", "Зона поражения: 25 метров");
                add("tooltip.hbm_m.grenade_nuc.line3", "Задержка: 6с");

                add("tooltip.hbm_m.grenade.common.line1", "Ручная граната");

                add("tooltip.hbm_m.grenade.smart.line2", "Детонирует при прямом попадании в сущность");
                add("tooltip.hbm_m.grenade.fire.line2", "Оставляет огонь после детонации");
                add("tooltip.hbm_m.grenade.slime.line2", "Сильно отскакивает от поверхностей");
                add("tooltip.hbm_m.grenade.standard.line2", "Слабый осколочный взрыв");
                add("tooltip.hbm_m.grenade.he.line2", "Усиленный фугасный взрыв");
                add("tooltip.hbm_m.grenade.default.line2", "Кидайте и взрывайте!");

                add("tooltip.hbm_m.grenade_if.common.line1", "IF-Граната");

                add("tooltip.hbm_m.grenade_if.he.line2", "Мощный фугасный взрыв");
                add("tooltip.hbm_m.grenade_if.slime.line2", "Сильно отскакивает от поверхностей");
                add("tooltip.hbm_m.grenade_if.fire.line2", "Оставляет огонь после детонации");
                add("tooltip.hbm_m.grenade_if.standard.line2", "Стандартный взрыв с таймером");
                add("tooltip.hbm_m.grenade_if.default.line2", "Аллах одобряет!");

                // ru_ru case
                // ru_ru case
                add(ModBlocks.BARREL_IRON.get(), "Железная бочка");
                add(ModBlocks.BARREL_STEEL.get(), "Стальная бочка");
                add(ModBlocks.BARREL_TCALLOY.get(), "Бочка из технециевой стали");
                add(ModItems.ZIRCONIUM_SHARP.get(), "Осколок циркония");
                add(ModBlocks.CRATE_CONSERVE.get(), "Ящик с консервами");
                add(ModBlocks.CAGE_LAMP.get(), "Лампа в клетке");
                add(ModBlocks.FLOOD_LAMP.get(), "Прожектор");
                add(ModBlocks.B29.get(), "B-29");
                add(ModBlocks.DORNIER.get(), "Dornier");
                add(ModBlocks.FILE_CABINET.get(), "Шкафчик");
                add(ModBlocks.TAPE_RECORDER.get(), "Магнитофон");
                add(ModBlocks.CRT_BROKEN.get(), "Сломанный монитор");
                add(ModBlocks.CRT_BSOD.get(), "BSOD Монитор ");
                add(ModBlocks.CRT_CLEAN.get(), "Монитор");
                add(ModBlocks.TOASTER.get(), "Тостер");
                add(ModBlocks.BARREL_CORRODED.get(), "Проржавевшая бочка");
                add(ModBlocks.BARREL_LOX.get(), "Бочка с жидким кислородом");
                add(ModBlocks.BARREL_PINK.get(), "Бочка с керосином");
                add(ModBlocks.BARREL_YELLOW.get(), "Бочка с ядерными отходами");
                add(ModBlocks.BARREL_VITRIFIED.get(), "Бочка с остеклованными ядерными отходами");
                add(ModBlocks.BARREL_TAINT.get(), "Бочка с говном");
                add(ModBlocks.FIRE_DOOR.get(), "Пожарная дверь");
                add(ModBlocks.SLIDING_SEAL_DOOR.get(), "Скользящая герметичная дверь");
                add(ModBlocks.SECURE_ACCESS_DOOR.get(), "Усиленная дверь");
                add(ModBlocks.QE_CONTAINMENT.get(), "QE дверь биологического сдерживания");
                add(ModBlocks.QE_SLIDING.get(), "QE раздвижная дверь");
                add(ModBlocks.WATER_DOOR.get(), "Подводный люк");
                add(ModBlocks.SILO_HATCH.get(), "Малый люк");
                add(ModBlocks.SILO_HATCH_LARGE.get(), "Люк ракетной шахты");


                add(ModBlocks.DUD_SALTED.get(), "Неразорвавшаяся солёная бомба");
                add(ModBlocks.DUD_NUKE.get(), "Неразорвавшаяся ядерная бомба");
                add(ModBlocks.DUD_FUGAS_TONG.get(), "Неразорвавшаяся фугасная бомба");
                add(ModBlocks.MINE_FAT.get(), "Мина 'Толстяк'");
                add(ModBlocks.MINE_AP.get(), "Противопехотная мина");
                add(ModItems.GRENADE_NUC.get(), "Ядерная граната");
                add(ModItems.GRENADE_IF_HE.get(), "IF-Граната: фугасная");
                add(ModItems.GRENADE_IF_FIRE.get(), "IF-Граната: зажигательная");
                add(ModItems.GRENADE_IF_SLIME.get(), "IF-Граната: прыгучая");
                add(ModItems.MULTI_DETONATOR.get(), "Мульти-детонатор");
                add(ModItems.RANGE_DETONATOR.get(), "Детонатор дальнего действия");
                add(ModItems.DETONATOR.get(), "Детонатор");
                add(ModBlocks.BARBED_WIRE_POISON.get(), "Колючая проволока (яд)");
                add(ModBlocks.BARBED_WIRE_FIRE.get(), "Колючая проволока (огонь)");
                add(ModBlocks.BARBED_WIRE_RAD.get(), "Колючая проволока (радиация)");
                add(ModBlocks.BARBED_WIRE.get(), "Колючая проволока");
                add(ModBlocks.BARBED_WIRE_WITHER.get(), "Колючая проволока (иссушение)");
                add(ModBlocks.WASTE_CHARGE.get(), "Отходный заряд");
                add(ModBlocks.GIGA_DET.get(), "Чёртов заряд горняка");
                add(ModBlocks.NUCLEAR_CHARGE.get(), "Ядерный заряд");
                add(ModBlocks.C4.get(), "Заряд C4");
                add(ModItems.DEFUSER.get(), "Устройство для разминирования");
                add(ModItems.CROWBAR.get(), "Лом");
                add(ModItems.DEPTH_ORES_SCANNER.get(), "Сканер глубинных кластеров");
                add(ModItems.OIL_DETECTOR.get(), "Детектор нефти");

                add(ModItems.GHIORSIUM_CLADDING.get(), "Прокладка из гиорсия");
                add(ModItems.DESH_CLADDING.get(), "Обшивка из деш");
                add(ModItems.RUBBER_CLADDING.get(), "Резиновая обшивка");
                add(ModItems.LEAD_CLADDING.get(), "Свинцовая обшивка");
                add(ModItems.PAINT_CLADDING.get(), "Свинцовая краска");
                add(ModItems.CRT_DISPLAY.get(), "Электро-лучевая трубка");
                add(ModItems.MAN_CORE.get(), "Плутониевое ядро");
                add(ModItems.GRENADESMART.get(), "УМная отскок граната");
                add(ModItems.GRENADESLIME.get(), "Отскок-отскок граната");
                add(ModItems.GRENADE.get(), "Отскок граната");
                add(ModItems.GRENADEHE.get(), "Мощная отскок граната");
                add(ModItems.GRENADEFIRE.get(), "Зажигательная отскок граната");

                add(ModItems.GRENADE_IF.get(), "IF граната");

                add("item.hbm_m.radaway", "Антирадин");
                add("item.hbm_m.wood_ash_powder", "Древесный пепел");
                add("effect.hbm_m.radaway", "Очищение от радиации");


// ru_ru case
                add(ModBlocks.CONVERTER_BLOCK.get(), "Конвертер энергии");
                add(ModBlocks.MACHINE_BATTERY_DINEUTRONIUM.get(), "Динейтрониевое энергохранилище");
                add(ModBlocks.MACHINE_BATTERY_SCHRABIDIUM.get(), "Шрабидиевое энергохранилище");
                add(ModBlocks.MACHINE_BATTERY_LITHIUM.get(), "Литиевое энергохранилище");
                add(ModBlocks.SEQUESTRUM_ORE.get(), "Селитровая руда");
                add(ModItems.SEQUESTRUM.get(), "Селитра");
                // русский:
                add(ModBlocks.ASPHALT.get(), "Асфальт");
                add(ModBlocks.BARRICADE.get(), "Мешки с песком");
                add(ModBlocks.CONCRETE_PILLAR.get(), "Колонна из бетона");
                add(ModBlocks.BASALT_BRICK.get(), "Базальтовые кирпичи");
                add(ModBlocks.BASALT_POLISHED.get(), "Отполированный базальт");
                add(ModBlocks.BRICK_BASE.get(), "Отполированные кирпичи");
                add(ModBlocks.BRICK_DUCRETE.get(), "Дюкритовые кирпичи");
                add(ModBlocks.BRICK_FIRE.get(), "Огнеупорные кирпичи");
                add(ModBlocks.BRICK_LIGHT.get(), "Легкие кирпичи");
                add(ModBlocks.BRICK_OBSIDIAN.get(), "Обсидиановые кирпичи");
                add(ModBlocks.CONCRETE_ASBESTOS.get(), "Асбестобетон");
                add(ModBlocks.CONCRETE_BLACK.get(), "Чёрный бетон");
                add(ModBlocks.CONCRETE_BLUE.get(), "Синий бетон");
                add(ModBlocks.CONCRETE_BROWN.get(), "Коричневый бетон");
                add(ModBlocks.CONCRETE_COLORED_BRONZE.get(), "Бронзовый бетон");
                add(ModBlocks.CONCRETE_COLORED_INDIGO.get(), "Индиго бетон");
                add(ModBlocks.CONCRETE_COLORED_MACHINE.get(), "Бетон 'Выбор Пломбира'");
                add(ModBlocks.CONCRETE_COLORED_MACHINE_STRIPE.get(), "Полосатый бетон 'Выбор Пломбира'");
                add(ModBlocks.CONCRETE_COLORED_PINK.get(), "Розовый бетон");
                add(ModBlocks.CONCRETE_COLORED_PURPLE.get(), "Фиолетовый бетон");
                add(ModBlocks.CONCRETE_COLORED_SAND.get(), "Бетон 'Техас'");
                add(ModBlocks.CONCRETE_CYAN.get(), "Бирюзовый бетон");
                add(ModBlocks.CONCRETE_GRAY.get(), "Серый бетон");
                add(ModBlocks.CONCRETE_GREEN.get(), "Зелёный бетон");
                add(ModBlocks.CONCRETE_LIGHT_BLUE.get(), "Голубой бетон");
                add(ModBlocks.CONCRETE_LIME.get(), "Лаймовый бетон");
                add(ModBlocks.CONCRETE_MAGENTA.get(), "Пурпурный бетон");
                add(ModBlocks.CONCRETE_ORANGE.get(), "Оранжевый бетон");
                add(ModBlocks.CONCRETE_PINK.get(), "Розовый бетон");
                add(ModBlocks.CONCRETE_PURPLE.get(), "Фиолетовый бетон");
                add(ModBlocks.CONCRETE_REBAR.get(), "Грубый бетон");
                add(ModBlocks.CONCRETE_REBAR_ALT.get(), "Бетон с арматурой");
                add(ModBlocks.CONCRETE_RED.get(), "Красный бетон");
                add(ModBlocks.CONCRETE_SILVER.get(), "Серебристый бетон");
                add(ModBlocks.CONCRETE_SUPER.get(), "Супер бетон");
                add(ModBlocks.CONCRETE_SUPER_BROKEN.get(), "Разбитый супер бетон");
                add(ModBlocks.CONCRETE_SUPER_M0.get(), "Супер бетон M0");
                add(ModBlocks.CONCRETE_SUPER_M1.get(), "Супер бетон M1");
                add(ModBlocks.CONCRETE_SUPER_M2.get(), "Супер бетон M2");
                add(ModBlocks.CONCRETE_SUPER_M3.get(), "Супер бетон M3");
                add(ModBlocks.CONCRETE_TILE.get(), "Бетонная плитка");
                add(ModBlocks.CONCRETE_TILE_TREFOIL.get(), "Помеченная бетонная плитка");
                add(ModBlocks.CONCRETE_WHITE.get(), "Белый бетон");
                add(ModBlocks.CONCRETE_YELLOW.get(), "Жёлтый бетон");
                add(ModBlocks.CONCRETE_FLAT.get(), "Плоский бетон");
                add(ModBlocks.DEPTH_BRICK.get(), "Глубинные кирпичи");
                add(ModBlocks.DEPTH_NETHER_BRICK.get(), "Адские глубинные кирпичи");
                add(ModBlocks.DEPTH_NETHER_TILES.get(), "Адская глубинная плитка");
                add(ModBlocks.DEPTH_STONE_NETHER.get(), "Адский глубинный камень");
                add(ModBlocks.DEPTH_TILES.get(), "Глубинная плитка");
                add(ModBlocks.GNEISS_BRICK.get(), "Кирпичи из графитового сланца");
                add(ModBlocks.GNEISS_CHISELED.get(), "Резной графитовый сланец");
                add(ModBlocks.GNEISS_STONE.get(), "Графитовый сланец");
                add(ModBlocks.GNEISS_TILE.get(), "Плитка из графитового сланца");
                add(ModBlocks.METEOR.get(), "Блок метеорита");
                add(ModBlocks.METEOR_BRICK.get(), "Метеоритные кирпичи");
                add(ModBlocks.METEOR_BRICK_CHISELED.get(), "Резные метеоритные кирпичи");
                add(ModBlocks.METEOR_BRICK_CRACKED.get(), "Треснутые метеоритные кирпичи");
                add(ModBlocks.METEOR_BRICK_MOSSY.get(), "Замшелые метеоритные кирпичи");
                add(ModBlocks.METEOR_COBBLE.get(), "Метеоритный булыжник");
                add(ModBlocks.METEOR_CRUSHED.get(), "Дроблёный метеорит");
                add(ModBlocks.METEOR_PILLAR.get(), "Метеоритная колонна");
                add(ModBlocks.METEOR_POLISHED.get(), "Отполированный метеорит");
                add(ModBlocks.METEOR_TREASURE.get(), "Блок метеоритных сокровищ");
                add(ModBlocks.VINYL_TILE.get(), "Виниловая плитка");
                add(ModBlocks.VINYL_TILE_SMALL.get(), "Мелкая виниловая плитка");

                add(ModBlocks.CONCRETE_ASBESTOS_SLAB.get(), "Плита из асбестобетона");
                add(ModBlocks.CONCRETE_BLACK_SLAB.get(), "Чёрная бетонная плита");
                add(ModBlocks.CONCRETE_BLUE_SLAB.get(), "Синяя бетонная плита");
                add(ModBlocks.CONCRETE_BROWN_SLAB.get(), "Коричневая бетонная плита");
                add(ModBlocks.CONCRETE_COLORED_BRONZE_SLAB.get(), "Плита из бронзового бетона");
                add(ModBlocks.CONCRETE_COLORED_INDIGO_SLAB.get(), "Плита из индиго бетона");
                add(ModBlocks.CONCRETE_COLORED_MACHINE_SLAB.get(), "Плита из бетона 'Выбор Пломбира'");
                add(ModBlocks.CONCRETE_COLORED_PINK_SLAB.get(), "Плита из розового бетона");
                add(ModBlocks.CONCRETE_COLORED_PURPLE_SLAB.get(), "Плита из фиолетового бетона");
                add(ModBlocks.CONCRETE_COLORED_SAND_SLAB.get(), "Плита из бетона 'Техас'");
                add(ModBlocks.CONCRETE_CYAN_SLAB.get(), "Бирюзовая бетонная плита");
                add(ModBlocks.CONCRETE_GRAY_SLAB.get(), "Серая бетонная плита");
                add(ModBlocks.CONCRETE_GREEN_SLAB.get(), "Зелёная бетонная плита");
                add(ModBlocks.CONCRETE_LIGHT_BLUE_SLAB.get(), "Голубая бетонная плита");
                add(ModBlocks.CONCRETE_LIME_SLAB.get(), "Лаймовая бетонная плита");
                add(ModBlocks.CONCRETE_MAGENTA_SLAB.get(), "Пурпурная бетонная плита");
                add(ModBlocks.CONCRETE_ORANGE_SLAB.get(), "Оранжевая бетонная плита");
                add(ModBlocks.CONCRETE_PINK_SLAB.get(), "Розовая бетонная плита");
                add(ModItems.AIRSTRIKE_TEST.get(), "Авиаудар");
                add(ModBlocks.CONCRETE_PURPLE_SLAB.get(), "Фиолетовая бетонная плита");
                add(ModBlocks.CONCRETE_RED_SLAB.get(), "Красная бетонная плита");
                add(ModBlocks.CONCRETE_SILVER_SLAB.get(), "Серебристая бетонная плита");
                add(ModBlocks.CONCRETE_WHITE_SLAB.get(), "Белая бетонная плита");
                add(ModBlocks.CONCRETE_YELLOW_SLAB.get(), "Жёлтая бетонная плита");
                add(ModBlocks.CONCRETE_SUPER_SLAB.get(), "Плита из супер бетона");
                add(ModBlocks.CONCRETE_SUPER_M0_SLAB.get(), "Плита из супер бетона M0");
                add(ModBlocks.CONCRETE_SUPER_M1_SLAB.get(), "Плита из супер бетона M1");
                add(ModBlocks.CONCRETE_SUPER_M2_SLAB.get(), "Плита из супер бетона M2");
                add(ModBlocks.CONCRETE_SUPER_M3_SLAB.get(), "Плита из супер бетона M3");
                add(ModBlocks.CONCRETE_SUPER_BROKEN_SLAB.get(), "Плита из разбитого супер бетона");
                add(ModBlocks.CONCRETE_REBAR_SLAB.get(), "Плита из грубого бетона");
                add(ModBlocks.CONCRETE_FLAT_SLAB.get(), "Плита из плоского бетона");
                add(ModBlocks.CONCRETE_TILE_SLAB.get(), "Плита из бетонной плитки");
                add(ModBlocks.DEPTH_BRICK_SLAB.get(), "Плита из глубинных кирпичей");
                add(ModBlocks.DEPTH_TILES_SLAB.get(), "Плита из глубинной плитки");
                add(ModBlocks.DEPTH_NETHER_BRICK_SLAB.get(), "Плита из адских глубинных кирпичей");
                add(ModBlocks.DEPTH_NETHER_TILES_SLAB.get(), "Плита из адской глубинной плитки");
                add(ModBlocks.GNEISS_TILE_SLAB.get(), "Плита из плитки графитового сланца");
                add(ModBlocks.GNEISS_BRICK_SLAB.get(), "Плита из кирпичей графитового сланца");
                add(ModBlocks.BRICK_BASE_SLAB.get(), "Плита из отполированных кирпичей");
                add(ModBlocks.BRICK_LIGHT_SLAB.get(), "Плита из легких кирпичей");
                add(ModBlocks.BRICK_FIRE_SLAB.get(), "Плита из огнеупорных кирпичей");
                add(ModBlocks.BRICK_OBSIDIAN_SLAB.get(), "Плита из обсидиановых кирпичей");
                add(ModBlocks.VINYL_TILE_SLAB.get(), "Плита из виниловой плитки");
                add(ModBlocks.VINYL_TILE_SMALL_SLAB.get(), "Плита из мелкой виниловой плитки");
                add(ModBlocks.BRICK_DUCRETE_SLAB.get(), "Плита из дюкритовых кирпичей");
                add(ModBlocks.ASPHALT_SLAB.get(), "Асфальтовая плита");
                add(ModBlocks.BASALT_POLISHED_SLAB.get(), "Плита из отполированного базальта");
                add(ModBlocks.BASALT_BRICK_SLAB.get(), "Плита из базальтовых кирпичей");
                add(ModBlocks.METEOR_POLISHED_SLAB.get(), "Плита из отполированного метеорита");
                add(ModBlocks.METEOR_BRICK_SLAB.get(), "Плита из метеоритных кирпичей");
                add(ModBlocks.METEOR_BRICK_CRACKED_SLAB.get(), "Плита из треснутых метеоритных кирпичей");
                add(ModBlocks.METEOR_BRICK_MOSSY_SLAB.get(), "Плита из замшелых метеоритных кирпичей");
                add(ModBlocks.METEOR_CRUSHED_SLAB.get(), "Плита из дроблёного метеорита");

                add(ModBlocks.CONCRETE_ASBESTOS_STAIRS.get(), "Ступени из асбестобетона");
                add(ModBlocks.CONCRETE_BLACK_STAIRS.get(), "Чёрные бетонные ступени");
                add(ModBlocks.CONCRETE_BLUE_STAIRS.get(), "Синие бетонные ступени");
                add(ModBlocks.CONCRETE_BROWN_STAIRS.get(), "Коричневые бетонные ступени");
                add(ModBlocks.CONCRETE_COLORED_BRONZE_STAIRS.get(), "Ступени из бронзового бетона");
                add(ModBlocks.CONCRETE_COLORED_INDIGO_STAIRS.get(), "Ступени из индиго бетона");
                add(ModBlocks.CONCRETE_COLORED_MACHINE_STAIRS.get(), "Ступени из бетона 'Выбор Пломбира'");
                add(ModBlocks.CONCRETE_COLORED_PINK_STAIRS.get(), "Ступени из розового бетона");
                add(ModBlocks.CONCRETE_COLORED_PURPLE_STAIRS.get(), "Ступени из фиолетового бетона");
                add(ModBlocks.CONCRETE_COLORED_SAND_STAIRS.get(), "Ступени из бетона 'Техас'");
                add(ModBlocks.CONCRETE_CYAN_STAIRS.get(), "Бирюзовые бетонные ступени");
                add(ModBlocks.CONCRETE_GRAY_STAIRS.get(), "Серые бетонные ступени");
                add(ModBlocks.CONCRETE_GREEN_STAIRS.get(), "Зелёные бетонные ступени");
                add(ModBlocks.CONCRETE_LIGHT_BLUE_STAIRS.get(), "Голубые бетонные ступени");
                add(ModBlocks.CONCRETE_LIME_STAIRS.get(), "Лаймовые бетонные ступени");
                add(ModBlocks.CONCRETE_MAGENTA_STAIRS.get(), "Пурпурные бетонные ступени");
                add(ModBlocks.CONCRETE_ORANGE_STAIRS.get(), "Оранжевые бетонные ступени");
                add(ModBlocks.CONCRETE_PINK_STAIRS.get(), "Розовые бетонные ступени");
                add(ModBlocks.CONCRETE_PURPLE_STAIRS.get(), "Фиолетовые бетонные ступени");
                add(ModBlocks.CONCRETE_RED_STAIRS.get(), "Красные бетонные ступени");
                add(ModBlocks.CONCRETE_SILVER_STAIRS.get(), "Серебристые бетонные ступени");
                add(ModBlocks.CONCRETE_WHITE_STAIRS.get(), "Белые бетонные ступени");
                add(ModBlocks.CONCRETE_YELLOW_STAIRS.get(), "Жёлтые бетонные ступени");
                add(ModBlocks.CONCRETE_SUPER_STAIRS.get(), "Ступени из супер бетона");
                add(ModBlocks.CONCRETE_SUPER_M0_STAIRS.get(), "Ступени из супер бетона M0");
                add(ModBlocks.CONCRETE_SUPER_M1_STAIRS.get(), "Ступени из супер бетона M1");
                add(ModBlocks.CONCRETE_SUPER_M2_STAIRS.get(), "Ступени из супер бетона M2");
                add(ModBlocks.CONCRETE_SUPER_M3_STAIRS.get(), "Ступени из супер бетона M3");
                add(ModBlocks.CONCRETE_SUPER_BROKEN_STAIRS.get(), "Ступени из разбитого супер бетона");
                add(ModBlocks.CONCRETE_REBAR_STAIRS.get(), "Ступени из грубого бетона");
                add(ModBlocks.CONCRETE_FLAT_STAIRS.get(), "Ступени из плоского бетона");
                add(ModBlocks.CONCRETE_TILE_STAIRS.get(), "Ступени из бетонной плитки");
                add(ModBlocks.DEPTH_BRICK_STAIRS.get(), "Ступени из глубинных кирпичей");
                add(ModBlocks.DEPTH_TILES_STAIRS.get(), "Ступени из глубинной плитки");
                add(ModBlocks.DEPTH_NETHER_BRICK_STAIRS.get(), "Ступени из адских глубинных кирпичей");
                add(ModBlocks.DEPTH_NETHER_TILES_STAIRS.get(), "Ступени из адской глубинной плитки");
                add(ModBlocks.GNEISS_TILE_STAIRS.get(), "Ступени из плитки графитового сланца");
                add(ModBlocks.GNEISS_BRICK_STAIRS.get(), "Ступени из кирпичей графитового сланца");
                add(ModBlocks.BRICK_BASE_STAIRS.get(), "Ступени из отполированных кирпичей");
                add(ModBlocks.BRICK_LIGHT_STAIRS.get(), "Ступени из легких кирпичей");
                add(ModBlocks.BRICK_FIRE_STAIRS.get(), "Ступени из огнеупорных кирпичей");
                add(ModBlocks.BRICK_OBSIDIAN_STAIRS.get(), "Ступени из обсидиановых кирпичей");
                add(ModBlocks.VINYL_TILE_STAIRS.get(), "Ступени из виниловой плитки");
                add(ModBlocks.VINYL_TILE_SMALL_STAIRS.get(), "Ступени из мелкой виниловой плитки");
                add(ModBlocks.BRICK_DUCRETE_STAIRS.get(), "Ступени из дюкритовых кирпичей");
                add(ModBlocks.ASPHALT_STAIRS.get(), "Асфальтовые ступени");
                add(ModBlocks.BASALT_POLISHED_STAIRS.get(), "Ступени из отполированного базальта");
                add(ModBlocks.BASALT_BRICK_STAIRS.get(), "Ступени из базальтовых кирпичей");
                add(ModBlocks.METEOR_POLISHED_STAIRS.get(), "Ступени из отполированного метеорита");
                add(ModBlocks.METEOR_BRICK_STAIRS.get(), "Ступени из метеоритных кирпичей");
                add(ModBlocks.METEOR_BRICK_CRACKED_STAIRS.get(), "Ступени из треснутых метеоритных кирпичей");
                add(ModBlocks.METEOR_BRICK_MOSSY_STAIRS.get(), "Ступени из замшелых метеоритных кирпичей");
                add(ModBlocks.METEOR_CRUSHED_STAIRS.get(), "Ступени из дроблёного метеорита");


                add(ModBlocks.DEPTH_STONE.get(), "Глубинный камень");
                add(ModBlocks.DEPTH_CINNABAR.get(), "Глубинная киноварная руда");
                add(ModBlocks.DEPTH_IRON.get(), "Глубинная железная руда");
                add(ModBlocks.DEPTH_ZIRCONIUM.get(), "Глубинная циркониевая руда");
                add(ModBlocks.DEPTH_BORAX.get(), "Глубинная бура");
                add(ModBlocks.DEPTH_TUNGSTEN.get(), "Глубинная вольфрамовая руда");
                add(ModBlocks.DEPTH_TITANIUM.get(), "Глубинная титановая руда");
                add(ModBlocks.BEDROCK_OIL.get(), "Бедроковая нефть");
                add(ModBlocks.BURNED_GRASS.get(), "Выжженная трава");
                add(ModBlocks.WASTE_PLANKS.get(), "Выжженные доски");
                add(ModBlocks.WASTE_LOG.get(), "Выжженное бревно");
                add(ModBlocks.SELLAFIELD_SLAKED.get(), "Погашенный селлафит");
                add(ModBlocks.SELLAFIELD_SLAKED1.get(), "Погашенный селлафит I");
                add(ModBlocks.SELLAFIELD_SLAKED2.get(), "Погашенный селлафит II");
                add(ModBlocks.SELLAFIELD_SLAKED3.get(), "Погашенный селлафит III");
                add(ModItems.COIL_MAGNETIZED_TUNGSTEN_TORUS.get(), "Кольцевая катушка из намагниченного вольфрама");
                add(ModItems.COIL_MAGNETIZED_TUNGSTEN.get(), "Катушка из намагниченного вольфрама");
                add(ModItems.COIL_ADVANCED_ALLOY_TORUS.get(), "Кольцевая катушка из продвинутого сплава");
                add(ModItems.COIL_ADVANCED_ALLOY.get(), "Катушка из продвинутого сплава");
                add(ModItems.COIL_COPPER_TORUS.get(), "Кольцевая медная катушка");
                add(ModItems.COIL_GOLD_TORUS.get(), "Кольцевая золотая катушка");
                add(ModItems.COIL_COPPER.get(), "Медная катушка");
                add(ModItems.COIL_GOLD.get(), "Медная катушка");
                add(ModItems.DUST.get(), "Кучка пыли");
                add(ModItems.DUST_TINY.get(), "Маленькая кучка пыли");
                add(ModItems.SCRAP.get(), "Мусор");
                add(ModItems.POWDER_COAL.get(), "Угольный порошок");
                add(ModItems.POWDER_COAL_SMALL.get(), "Маленькая кучка угольного порошока");
                add(ModItems.BILLET_PLUTONIUM.get(), "Заготовка плутония");



                add("tooltip.hbm_m.depthstone.line1", "Может быть уничтожен только взрывом!");
                add("tooltip.hbm_m.depthstone.line4", "Используйте Шахтёрский заряд для безопасной добычи");
                add(ModItems.MOTOR_BISMUTH.get(), "Висмутовый мотор");
                add(ModItems.MOTOR_DESH.get(), "Деш мотор");
                add(ModItems.MOTOR.get(), "Мотор");
                add(ModItems.BLADE_TEST.get(), "Деш лезвия");
                add(ModItems.BLADE_STEEL.get(), "Стальные лезвия");
                add(ModItems.BLADE_TITANIUM.get(), "Титановые лезвия");
                add(ModItems.BLADE_ALLOY.get(), "Лезвия из продвинутого сплава");
                add(ModItems.BORAX.get(), "Бура");
                add(ModItems.BALL_TNT.get(), "Взрывчатка");
                add(ModItems.BOLT_STEEL.get(), "Болт");
                add(ModItems.CANNED_ASBESTOS.get(), "Консервированный асбест");
                add(ModItems.CANNED_ASS.get(), "Консервированная задница");
                add(ModItems.CANNED_BARK.get(), "Консервированная кора");
                add(ModItems.CANNED_BEEF.get(), "Консервированная говядина");
                add(ModItems.CANNED_BHOLE.get(), "Консервированная черная дыра");
                add(ModItems.CANNED_CHEESE.get(), "Консервированный сыр");
                add(ModItems.CANNED_CHINESE.get(), "Консервированное китайское блюдо");
                add(ModItems.CANNED_DIESEL.get(), "Консервированный дизель");
                add(ModItems.CANNED_FIST.get(), "Консервированный кулак");
                add(ModItems.CANNED_FRIED.get(), "Жареная консерва");
                add(ModItems.CANNED_HOTDOGS.get(), "Консервированные хот-доги");
                add(ModItems.CANNED_JIZZ.get(), "Консервированное жеребцовое молочко");
                add(ModItems.CANNED_KEROSENE.get(), "Консервированный керосин");
                add(ModItems.CANNED_LEFTOVERS.get(), "Консервированные остатки");
                add(ModItems.CANNED_MILK.get(), "Консервированное молоко");
                add(ModItems.CANNED_MYSTERY.get(), "Консервированная загадка");
                add(ModItems.CANNED_NAPALM.get(), "Консервированный напалм");
                add(ModItems.CANNED_OIL.get(), "Консервированная нефть");
                add(ModItems.CANNED_PASHTET.get(), "Консервированный паштет");
                add(ModItems.CANNED_PIZZA.get(), "Консервированная пицца");
                add(ModItems.CANNED_RECURSION.get(), "Консервированная рекурсия");
                add(ModItems.CANNED_SPAM.get(), "Консервированный спам");
                add(ModItems.CANNED_STEW.get(), "Консервированное рагу");
                add(ModItems.CANNED_TOMATO.get(), "Консервированный томат");
                add(ModItems.CANNED_TUNA.get(), "Консервированный тунец");
                add(ModItems.CANNED_TUBE.get(), "Консервированная трубка");
                add(ModItems.CANNED_YOGURT.get(), "Консервированный йогурт");
                add(ModItems.CAN_BEPIS.get(), "Напиток Бепис");
                add(ModItems.CAN_BREEN.get(), "Напиток Breen");
                add(ModItems.CAN_CREATURE.get(), "Напиток Creature");
                add(ModItems.CAN_EMPTY.get(), "Пустая банка");
                add(ModItems.CAN_KEY.get(), "Консервный нож");
                add(ModItems.CAN_LUNA.get(), "Напиток Luna");
                add(ModItems.CAN_MRSUGAR.get(), "Напиток Mrsugar");
                add(ModItems.CAN_MUG.get(), "Напиток Mug");
                add(ModItems.CAN_OVERCHARGE.get(), "Напиток Overcharge");
                add(ModItems.CAN_REDBOMB.get(), "Напиток RedBomb");
                add(ModItems.CAN_SMART.get(), "Напиток Smart");


                add(ModItems.QUANTUM_CHIP.get(), "Квантовый чип");
                add(ModItems.QUANTUM_CIRCUIT.get(), "Квантовая микросхема");
                add(ModItems.QUANTUM_COMPUTER.get(), "Квантовый компьютер");
                add(ModItems.SILICON_CIRCUIT.get(), "Опечатанная кремниевая пластина");
                add(ModItems.BISMOID_CHIP.get(), "Передовой микрочип");
                add(ModItems.BISMOID_CIRCUIT.get(), "Передовая микросхема");
                add(ModItems.CONTROLLER_CHASSIS.get(), "Корпус контроллера");
                add(ModItems.CONTROLLER.get(), "Контроллер");
                add(ModItems.CONTROLLER_ADVANCED.get(), "Продвинутый контроллер");
                add(ModItems.CAPACITOR_BOARD.get(), "Конденсаторная плата");
                add(ModItems.CAPACITOR_TANTALUM.get(), "Танталовый конденсатор");
                add(ModItems.ANALOG_CIRCUIT.get(), "Аналоговая плата");
                add(ModItems.INTEGRATED_CIRCUIT.get(), "Интегральная схема");
                add(ModItems.ADVANCED_CIRCUIT.get(), "Военная микросхема");
                add(ModItems.MICROCHIP.get(), "Микрочип");
                add(ModItems.ATOMIC_CLOCK.get(), "Атомные часы");
                add(ModItems.VACUUM_TUBE.get(), "Вакуумная трубка");
                add(ModItems.CAPACITOR.get(), "Конденсатор");
                add(ModItems.PCB.get(), "Печатная плата");
                add(ModItems.INSULATOR.get(), "Изолятор");
                add(ModItems.NUGGET_SILICON.get(), "Самородок кремния");
                add(ModItems.BILLET_SILICON.get(), "Заготовка кремния");

                add(ModItems.BATTLE_GEARS.get(), "Боевые детали");
                add(ModItems.BATTLE_CASING.get(), "Боевой корпус");
                add(ModItems.BATTLE_COUNTER.get(), "Боевой блок управления");
                add(ModItems.BATTLE_SENSOR.get(), "Боевой сенсор");
                add(ModItems.BATTLE_MODULE.get(), "Боевой модуль");
                add(ModItems.METAL_ROD.get(), "Металлический стержень");
                add(ModItems.STRAWBERRY.get(), "Клубника");
                add(ModItems.PLATE_GOLD.get(), "Золотая пластина");
                add(ModItems.PLATE_GUNMETAL.get(), "Пластина пушечной бронзы");
                add(ModItems.PLATE_TITANIUM.get(), "Титановая пластина");
                add(ModItems.PLATE_GUNSTEEL.get(), "Пластина оружейной стали");
                add(ModItems.PLATE_IRON.get(), "Железная пластина");
                add(ModItems.PLATE_KEVLAR.get(), "Кевларовая пластина");
                add(ModItems.PLATE_LEAD.get(), "Свинцовая пластина");
                add(ModItems.PLATE_MIXED.get(), "Композитная пластина");
                add(ModItems.PLATE_PAA.get(), "Пластина сплава РаА");
                add(ModItems.PLATE_SATURNITE.get(), "Сатурнитовая пластина");
                add(ModItems.PLATE_SCHRABIDIUM.get(), "Шрабидиевая пластина");
                add(ModItems.PLATE_STEEL.get(), "Стальная пластина");
                add(ModItems.PLATE_ADVANCED_ALLOY.get(), "Пластина из продвинутого сплава");
                add(ModItems.PLATE_ALUMINUM.get(), "Алюминиевая пластина");
                add(ModItems.PLATE_COPPER.get(), "Медная пластина");
                add(ModItems.PLATE_BISMUTH.get(), "Висмутовая пластина");
                add(ModItems.PLATE_ARMOR_AJR.get(), "Броневая пластина AJR");
                add(ModItems.PLATE_ARMOR_DNT.get(), "Броневая пластина DNT");
                add(ModItems.PLATE_ARMOR_DNT_RUSTED.get(), "Ржавая броневая пластина DNT");
                add(ModItems.PLATE_ARMOR_FAU.get(), "Броневая пластина FAU");
                add(ModItems.PLATE_ARMOR_HEV.get(), "Броневая пластина HEV");
                add(ModItems.PLATE_ARMOR_LUNAR.get(), "Лунная броневая пластина");
                add(ModItems.PLATE_ARMOR_TITANIUM.get(), "Титановая броневая пластина");
                add(ModItems.PLATE_CAST.get(), "Литая пластина");
                add(ModItems.PLATE_CAST_ALT.get(), "Альтернативная литая пластина");
                add(ModItems.PLATE_CAST_BISMUTH.get(), "Висмутовая литая пластина");
                add(ModItems.PLATE_CAST_DARK.get(), "Тёмная литая пластина");
                add(ModItems.PLATE_COMBINE_STEEL.get(), "Пластина из комбинированной стали");
                add(ModItems.PLATE_DURA_STEEL.get(), "Пластина из прочной стали");
                add(ModItems.PLATE_DALEKANIUM.get(), "Далеканиевая пластина");
                add(ModItems.PLATE_DESH.get(), "Дешевая пластина");
                add(ModItems.PLATE_DINEUTRONIUM.get(), "Динейтрониевая пластина");
                add(ModItems.PLATE_EUPHEMIUM.get(), "Эуфемиевая пластина");
                add(ModItems.PLATE_FUEL_MOX.get(), "Топливная пластина MOX");
                add(ModItems.PLATE_FUEL_PU238BE.get(), "Топливная пластина Pu-238/Be");
                add(ModItems.PLATE_FUEL_PU239.get(), "Топливная пластина Pu-239");
                add(ModItems.PLATE_FUEL_RA226BE.get(), "Топливная пластина Ra-226/Be");
                add(ModItems.PLATE_FUEL_SA326.get(), "Топливная пластина SA-326");
                add(ModItems.PLATE_FUEL_U233.get(), "Топливная пластина U-233");
                add(ModItems.PLATE_FUEL_U235.get(), "Топливная пластина U-235");


                add("item.hbm_m.firebrick", "Шамотный кирпич");
                add("item.hbm_m.uranium_raw", "Рудный уран");
                add("item.hbm_m.tungsten_raw", "Рудный вольфрам");
                add("item.hbm_m.titanium_raw", "Рудный титан");
                add("item.hbm_m.thorium_raw", "Рудный торий");
                add("item.hbm_m.lead_raw", "Рудный свинец");
                add("item.hbm_m.cobalt_raw", "Рудный кобальт");
                add("item.hbm_m.beryllium_raw", "Рудный бериллий");
                add("item.hbm_m.aluminum_raw", "Рудный алюминий");
                add("item.hbm_m.cinnabar", "Киноварь");
                add("item.hbm_m.sulfur", "Сера");
                add("item.hbm_m.rareground_ore_chunk", "Кусок редкоземельной руды");
                add("item.hbm_m.lignite", "Бурый уголь");
                add("item.hbm_m.fluorite", "Флюорит");
                add("item.hbm_m.fireclay_ball", "Комок огнеупорной глины");
            
                add("item.hbm_m.blueprint_folder", "Папка шаблонов");
                add("item.hbm_m.blueprint_folder.named", "Папка шаблонов машин");
                add("item.hbm_m.blueprint_folder.empty", "Пустая папка");
                add("item.hbm_m.blueprint_folder.obsolete", "Устаревший шаблон (группа удалена)");
                add("item.hbm_m.blueprint_folder.desc", "Вставьте в Сборочную машину для разблокировки рецептов");
                add("item.hbm_m.blueprint_folder.recipes", "Содержит рецепты:");
                add("gui.hbm_m.recipe_from_group", "Из группы:");
                
                add("sounds.hbm_m.radaway_use", "Использование антирадина");
                
                add("tooltip.hbm_m.mods", "Модификации:");
                add("tooltip.hbm_m.heart_piece.effect", "+5 Здоровья");
                
                add("tooltip.hbm_m.applies_to", "Применяется к:");

                add("tooltip.hbm_m.helmet", "Шлему");
                add("tooltip.hbm_m.chestplate", "Нагруднику");
                add("tooltip.hbm_m.leggings", "Поножам");
                add("tooltip.hbm_m.boots", "Ботинкам");
                add("tooltip.hbm_m.armor.all", "Любой броне");
                add("tooltip.hbm_m.rad_protection.value_short", "%s сопр. радиации.");

                add("gui.hbm_m.blast_furnace.accepts", "Принимает предметы со стороны: %s");
                add("direction.hbm_m.down", "Вниз");
                add("direction.hbm_m.up", "Вверх");
                add("direction.hbm_m.north", "Север");
                add("direction.hbm_m.south", "Юг");
                add("direction.hbm_m.west", "Запад");
                add("direction.hbm_m.east", "Восток");
                add("gui.hbm_m.anvil.inputs", "Входы:");
                add("gui.hbm_m.anvil.outputs", "Выходы:");
                add("gui.hbm_m.anvil.search", "Поиск");
                add("gui.hbm_m.anvil.search_hint", "Поиск...");
                add("gui.hbm_m.anvil.tier", "Требуемый уровень: %s");
                add("tier.hbm_m.anvil.iron", "Железо");
                add("tier.hbm_m.anvil.steel", "Сталь");
                add("tier.hbm_m.anvil.oil", "Нефтяной");
                add("tier.hbm_m.anvil.nuclear", "Ядерный");
                add("tier.hbm_m.anvil.rbmk", "РБМК");
                add("tier.hbm_m.anvil.fusion", "Термоядерный");
                add("tier.hbm_m.anvil.particle", "Частичный ускоритель");
                add("tier.hbm_m.anvil.gerald", "Джеральд");
                add("tier.hbm_m.anvil.murky", "Мрачный");

                // БЛОКИ
                add(ModBlocks.RESOURCE_ASBESTOS.get(), "Асбестовый кластер");
                add(ModBlocks.RESOURCE_BAUXITE.get(), "Боксит");
                add(ModBlocks.RESOURCE_HEMATITE.get(), "Гематит");
                add(ModBlocks.RESOURCE_LIMESTONE.get(), "Известняк");
                add(ModBlocks.RESOURCE_MALACHITE.get(), "Малахит");
                add(ModBlocks.RESOURCE_SULFUR.get(), "Серный кластер");
                add("block.hbm_m.anvil_block", "Индустриальная наковальня");
                add("block.hbm_m.anvil_iron", "Железная наковальня");
                add("block.hbm_m.anvil_lead", "Свинцовая наковальня");
                add("block.hbm_m.anvil_steel", "Стальная наковальня");
                add("block.hbm_m.anvil_desh", "Наковальня из деша");
                add("block.hbm_m.anvil_ferrouranium", "Наковальня из ферроурания");
                add("block.hbm_m.anvil_saturnite", "Сатурнитовая наковальня");
                add("block.hbm_m.anvil_bismuth_bronze", "Наковальня из висмутовой бронзы");
                add("block.hbm_m.anvil_arsenic_bronze", "Наковальня из мышьяковой бронзы");
                add("block.hbm_m.anvil_schrabidate", "Шрабидатовая наковальня");
                add("block.hbm_m.anvil_dnt", "Наковальня DNT");
                add("block.hbm_m.anvil_osmiridium", "Осмиридиевая наковальня");
                add("block.hbm_m.anvil_murky", "Мрачная наковальня");
                add("block.hbm_m.door_office", "Офисная дверь");
                add("block.hbm_m.door_bunker", "Бункерная дверь");
                add("block.hbm_m.metal_door", "Металлическая дверь");
                add("block.hbm_m.demon_lamp", "Милая лампа (WIP)");
                add("block.hbm_m.explosive_charge", "Заряд взрывчатки");
                add("block.hbm_m.det_miner", "Шахтёрский заряд");
                add("block.hbm_m.concrete_vent", "Вентиляция в бетоне");
                add("block.hbm_m.concrete_fan", "Вентилятор в бетоне");
                add("block.hbm_m.concrete_marked", "Помеченный бетон");
                add("block.hbm_m.concrete_cracked", "Потрескавшийся бетон");
                add("block.hbm_m.concrete_mossy", "Замшелый бетон");
                add("block.hbm_m.concrete", "Бетон");
                add("block.hbm_m.reinforced_glass", "Усиленное стекло");
                add("block.hbm_m.crate", "Ящик");
                add("block.hbm_m.crate_lead", "Свинцовый ящик");
                add("block.hbm_m.crate_metal", "Металлический ящик");
                add("block.hbm_m.crate_weapon", "Ящик с оружием");
                add("block.hbm_m.uranium_block", "Урановый блок");
                add("block.hbm_m.plutonium_block", "Плутониевый блок");
                add("block.hbm_m.plutonium_fuel_block", "Блок плутониевого топлива");
                add("block.hbm_m.polonium210_block", "Блок полония-210");
                add("block.hbm_m.armor_table", "Стол модификации брони");
                add("block.hbm_m.machine_assembler", "Сборочная машина (Старая)");
                add("block.hbm_m.advanced_assembly_machine", "Сборочная машина");
                add("block.hbm_m.machine_battery", "Энергохранилище");


                add("block.hbm_m.wire_coated", "Провод из красной меди");
                add("block.hbm_m.wood_burner", "Дровяной генератор");
                add("block.hbm_m.shredder", "Измельчитель");
                add("block.hbm_m.blast_furnace", "Доменная печь");
                add("block.hbm_m.blast_furnace_extension", "Расширение доменной печи");
                add("block.hbm_m.press", "Пресс");
                add("block.hbm_m.geiger_counter_block", "Стационарный счетчик Гейгера");
                add("block.hbm_m.freaky_alien_block", "Блок ебанутого инопланетянина");
                add("block.hbm_m.reinforced_stone", "Уплотнённый камень");
                add("block.hbm_m.reinforced_stone_slab", "Плита из уплотнённого камня");
                add("block.hbm_m.reinforced_stone_stairs", "Ступеньки из уплотнённого камня");
                add("block.hbm_m.concrete_hazard", "Бетон ''Выбор строителя'' - Полоса опасности");
                add("block.hbm_m.concrete_hazard_slab", "Бетонная плита ''Выбор строителя'' - Полоса опасности");
                add("block.hbm_m.concrete_hazard_stairs", "Бетонные ступеньки ''Выбор строителя'' - Полоса опасности");
                add("block.hbm_m.concrete_stairs", "Бетонные ступеньки");
                add("block.hbm_m.concrete_slab", "Бетонная плита");
                add("block.hbm_m.concrete_cracked_slab", "Плита из треснутого бетона");
                add("block.hbm_m.concrete_cracked_stairs", "Ступени из треснутого бетона");
                add("block.hbm_m.concrete_mossy_slab", "Плита из замшелого бетона");
                add("block.hbm_m.concrete_mossy_stairs", "Ступени из замшелого бетона");
                add("block.hbm_m.switch", "Рубильник");
                add("block.hbm_m.large_vehicle_door", "Дверь для крупногабаритного транспорта");
                add("block.hbm_m.round_airlock_door", "Круглая воздушная дверь");
                add("block.hbm_m.strawberry_bush", "Куст клубники");
                add("block.hbm_m.strawberry", "Клубника");
                add("block.hbm_m.brick_concrete", "Бетонные кирпичи");
                add("block.hbm_m.brick_concrete_slab", "Плита из бетонных кирпичей");
                add("block.hbm_m.brick_concrete_stairs", "Ступени из бетонных кирпичей");
                add("block.hbm_m.brick_concrete_broken", "Сломанные бетонные кирпичи");
                add("block.hbm_m.brick_concrete_broken_slab", "Плита из сломанных бетонных кирпичей");
                add("block.hbm_m.brick_concrete_broken_stairs", "Ступени из сломанных бетонных кирпичей");
                add("block.hbm_m.brick_concrete_cracked", "Треснутые Бетонные кирпичи");
                add("block.hbm_m.brick_concrete_cracked_slab", "Плита из треснутых бетонных кирпичей");
                add("block.hbm_m.brick_concrete_cracked_stairs", "Ступени из треснутых бетонных кирпичей");
                add("block.hbm_m.brick_concrete_mossy", "Замшелые бетонные кирпичи");
                add("block.hbm_m.brick_concrete_mossy_slab", "Плита из замшелых бетонных кирпичей");
                add("block.hbm_m.brick_concrete_mossy_stairs", "Ступени из замшелых бетонных кирпичей");
                add("block.hbm_m.brick_concrete_marked", "Помеченные бетонные кирпичи");


                // РУДЫ

                add("block.hbm_m.uranium_ore", "Урановая руда");
                add("block.hbm_m.aluminum_ore", "Алюминиевая руда");
                add("block.hbm_m.aluminum_ore_deepslate", "Глубинная алюминиевая руда");
                add("block.hbm_m.cinnabar_ore_deepslate", "Глубинная киноварная руда");
                add("block.hbm_m.cobalt_ore_deepslate", "Глубинная кобальтовая руда");
                add("block.hbm_m.titanium_ore", "Титановая руда");
                add("block.hbm_m.titanium_ore_deepslate", "Глубинная титановая руда");
                add("block.hbm_m.tungsten_ore", "Вольфрамовая руда");
                add("block.hbm_m.asbestos_ore", "Асбестовая руда");
                add("block.hbm_m.sulfur_ore", "Серная руда");
                add("block.hbm_m.cobalt_ore", "Кобальтовая руда");
                add("block.hbm_m.lignite_ore", "Руда бурого угля");
                add("block.hbm_m.uranium_ore_h", "Обогащённая урановая руда");
                add("block.hbm_m.uranium_ore_deepslate", "Глубинная урановая руда");
                add("block.hbm_m.thorium_ore", "Ториевая руда");
                add("block.hbm_m.thorium_ore_deepslate", "Глубинная ториевая руда");
                add("block.hbm_m.rareground_ore", "Руда редкоземельных металлов");
                add("block.hbm_m.rareground_ore_deepslate", "Глубинная руда редкоземельных металлов");
                add("block.hbm_m.beryllium_ore", "Бериллиевая руда");
                add("block.hbm_m.beryllium_ore_deepslate", "Глубинная бериллиевая руда");
                add("block.hbm_m.fluorite_ore", "Флюоритовая руда");
                add("block.hbm_m.lead_ore", "Свинцовая руда");
                add("block.hbm_m.lead_ore_deepslate", "Глубинная свинцовая руда");
                add("block.hbm_m.cinnabar_ore", "Киноварная руда");
                add("block.hbm_m.crate_iron", "Железный ящик");
                add("block.hbm_m.crate_steel", "Стальной ящик");
                add("block.hbm_m.crate_desh", "Деш ящик");

                add("block.hbm_m.waste_grass", "Мёртвая трава");
                add("block.hbm_m.waste_leaves", "Мёртвая листва");

                // MACHINE GUI
                
                add("tooltip.hbm_m.armor_table.main_slot", "Вставьте броню, чтобы ее модифицировать...");
                add("tooltip.hbm_m.slot", "Слот");
                add("tooltip.hbm_m.armor_table.helmet_slot", "Шлем");
                add("tooltip.hbm_m.armor_table.chestplate_slot", "Нагрудник");
                add("tooltip.hbm_m.armor_table.leggings_slot", "Поножи");
                add("tooltip.hbm_m.armor_table.boots_slot", "Ботинки");
                add("tooltip.hbm_m.armor_table.battery_slot", "Аккумулятор");
                add("tooltip.hbm_m.armor_table.special_slot", "Особое");
                add("tooltip.hbm_m.armor_table.plating_slot", "Пластина");
                add("tooltip.hbm_m.armor_table.casing_slot", "Обшивка");
                add("tooltip.hbm_m.armor_table.servos_slot", "Сервоприводы");

                add("tooltip.hbm_m.rad_protection.value", "Сопротивление радиации: %s");

                add("container.inventory", "Инвентарь");
                add("container.hbm_m.armor_table", "Стол модификации брони");
                add("container.hbm_m.machine_assembler", "Сборочная машина");
                add("container.hbm_m.advanced_assembly_machine", "Сборочная машина");
                add("container.hbm_m.wood_burner", "Дровяной генератор");
                add("container.hbm_m.machine_battery", "Энергохранилище");
                add("container.hbm_m.press", "Пресс");
                add("container.hbm_m.anvil_block", "Индустриальная наковальня");
                add("container.hbm_m.anvil", "Наковальня %s");
                add("container.hbm_m.crate_iron", "Железный ящик");
                add("container.hbm_m.crate_steel", "Стальной ящик");
                add("container.hbm_m.crate_desh", "Душ ящик");

                add("gui.hbm_m.battery.priority.0", "Приоритет: Низкий");
                add("gui.hbm_m.battery.priority.0.desc", "Низший приоритет. Опустошается в первую очередь, заполняется в последнюю");
                add("gui.hbm_m.battery.priority.1", "Приоритет: Нормальный");
                add("gui.hbm_m.battery.priority.1.desc", "Стандартный приоритет для передачи энергии.");
                add("gui.hbm_m.battery.priority.2", "Приоритет: Высокий");
                add("gui.hbm_m.battery.priority.2.desc", "Высший приоритет. Заполняется первым, опустошается последним.");
                add("gui.hbm_m.battery.priority.recommended", "(Рекомендуется)");

                add("gui.hbm_m.battery.condition.no_signal", "Когда НЕТ редстоун-сигнала:");
                add("gui.hbm_m.battery.condition.with_signal", "Когда ЕСТЬ редстоун-сигнал:");

                add("gui.hbm_m.battery.mode.both", "Режим: Приём и Передача");
                add("gui.hbm_m.battery.mode.both.desc", "Разрешены все операции с энергией.");
                add("gui.hbm_m.battery.mode.input", "Режим: Только Приём");
                add("gui.hbm_m.battery.mode.input.desc", "Разрешён только приём энергии.");
                add("gui.hbm_m.battery.mode.output", "Режим: Только Передача");
                add("gui.hbm_m.battery.mode.output.desc", "Разрешена только отдача энергии.");
                add("gui.hbm_m.battery.mode.locked", "Режим: Заблокировано");
                add("gui.hbm_m.battery.mode.locked.desc", "Все операции с энергией отключены.");

                add("gui.recipe.setRecipe", "Выбрать рецепт");

                add("tooltip.hbm_m.battery.stored", "Хранится энергии:");
                add("tooltip.hbm_m.battery.transfer_rate", "Скорость зарядки: %1$s HE/t");
                add("tooltip.hbm_m.battery.discharge_rate", "Скорость разрядки: %1$s HE/t");

                add("tooltip.hbm_m.machine_battery.capacity", "Ёмкость: %1$s HE");
                add("tooltip.hbm_m.machine_battery.charge_speed", "Скорость зарядки: %1$s HE/т");
                add("tooltip.hbm_m.machine_battery.discharge_speed", "Скорость разрядки: %1$s HE/т");
                add("tooltip.hbm_m.machine_battery.stored", "Заряжено: %1$s / %2$s HE");
                add("tooltip.hbm_m.requires", "Требуется");


                add("hazard.hbm_m.radiation", "[Радиоактивный]");
                add("hazard.hbm_m.radiation.format", "%s РАД/с");
                add("hazard.hbm_m.hydro_reactive", "[Гидрореактивный]");
                add("hazard.hbm_m.explosive_on_fire", "[Воспламеняющийся / Взрывоопасный]");
                add("hazard.hbm_m.pyrophoric", "[Пирофорный / Горячий]");
                add("hazard.hbm_m.explosion_strength.format", " Сила взрыва - %s");
                add("hazard.hbm_m.stack", "Стак: %s");

                add("item.hbm_m.meter.geiger_counter.name", "СЧЁТЧИК ГЕЙГЕРА");
                add("item.hbm_m.meter.dosimeter.name", "ДОЗИМЕТР");
                add("item.hbm_m.meter.title_format", "%s");
                add("hbm_m.render.shader_detected", "§e[HBM] §7Обнаружен активный шейдер. Переключение на совместимый рендер...");
                add("hbm_m.render.shader_disabled", "§a[HBM] §7Шейдер отключен. Возврат к оптимизированному VBO рендеру.");
                add("hbm_m.render.path_changed", "§e[HBM] §7Путь рендера установлен: %s");
                add("hbm_m.render.status", "§e[HBM] §7Текущий путь рендера: §f%s\n§7Внешний шейдер обнаружен: §f%s");

                add("tooltip.hbm_m.abilities", "Способности:");
                add("tooltip.hbm_m.vein_miner", "Жилковый майнер (%s)");
                add("tooltip.hbm_m.aoe", "Зона действия %s");
                add("tooltip.hbm_m.silk_touch", "Шёлковое касание");
                add("tooltip.hbm_m.fortune", "Удача (%s)");
                add("tooltip.hbm_m.right_click", "ПКМ - переключить способность");
                add("tooltip.hbm_m.shift_right_click", "Shift + ПКМ - выключить всё");

                add("message.hbm_m.vein_miner.enabled", "Жилковый майнер %s активирован!");
                add("message.hbm_m.vein_miner.disabled", "Жилковый майнер %s деактивирован!");
                add("message.hbm_m.aoe.enabled", "Зона действия %1$s x %1$s x %1$s активирована!");
                add("message.hbm_m.aoe.disabled", "Зона действия %s x %s x %s деактивирована!");
                add("message.hbm_m.silk_touch.enabled", "Шёлковое касание активировано!");
                add("message.hbm_m.silk_touch.disabled", "Шёлковое касание деактивировано!");
                add("message.hbm_m.fortune.enabled", "Удача %s активирована!");
                add("message.hbm_m.fortune.disabled", "Удача %s деактивирована!");
                add("message.hbm_m.disabled", "Все способности выключены!");

                add("item.hbm_m.meter.chunk_rads", "§eТекущий уровень радиации в чанке: %s\n");
                add("item.hbm_m.meter.env_rads", "§eОбщее радиационное заражение среды: %s");
                add("item.hbm_m.meter.player_rads", "§eУровень радиоактивного заражения игрока: %s\n");
                add("item.hbm_m.meter.protection", "§eЗащищённость игрока: %s (%s)");

                add("item.hbm_m.meter.rads_over_limit", ">%s RAD/s");
                add("tooltip.hbm_m.hold_shift_for_details", "<Зажмите SHIFT для деталей>");
                
                add("sounds.hbm_m.geiger_counter", "Щелчки счетчика Гейгера");
                add("sounds.hbm_m.tool.techboop", "Пик счетчика Гейгера");
                
                add("commands.hbm_m.rad.cleared", "Радиация очищена у %s игроков.");
                add("commands.hbm_m.rad.cleared.self", "Ваша радиация очищена.");
                add("commands.hbm_m.rad.added", "Добавлено %s радиации %s игрокам.");
                add("commands.hbm_m.rad.added.self", "Вам добавлено %s радиации.");
                add("commands.hbm_m.rad.removed", "Убрано %s радиации у %s игроков.");
                add("commands.hbm_m.rad.removed.self", "У вас убрано %s радиации.");

                add("death.attack.radiation", "Игрок %s умер от лучевой болезни");
                add("advancements.hbm_m.radiation_200.title", "Ура, Радиация!");
                add("advancements.hbm_m.radiation_200.description", "Достигнуть уровня радиации в 200 РАД");
                add("advancements.hbm_m.radiation_1000.title", "Ай, Радиация!");
                add("advancements.hbm_m.radiation_1000.description", "Умереть от лучевой болезни");

                add("chat.hbm_m.structure.obstructed", "Другие блоки мешают установке структуры!!");


                add("text.autoconfig.hbm_m.title", "Настройки радиации (HBM Modernized)");

                add("text.autoconfig.hbm_m.category.general", "Общие настройки");
                add("text.autoconfig.hbm_m.option.enableRadiation", "Включить радиацию");
                add("text.autoconfig.hbm_m.option.enableChunkRads", "Включить радиацию в чанках");
                add("text.autoconfig.hbm_m.option.usePrismSystem", "Использовать систему PRISM (иначе Simple, WIP)");

                add("text.autoconfig.hbm_m.category.world_effects", "Эффекты мира");
                add("text.autoconfig.hbm_m.option.worldRadEffects", "Эффекты радиации на мир (изменения блоков)");
                add("text.autoconfig.hbm_m.option.worldRadEffects.@Tooltip", "Включает/выключает эффекты разрушения мира от высокой радиации (замена блоков, гибель растительности и т.д.).");

                add("text.autoconfig.hbm_m.option.worldRadEffectsThreshold", "Порог радиации для разрушения");
                add("text.autoconfig.hbm_m.option.worldRadEffectsThreshold.@Tooltip", "Минимальный уровень фоновой радиации в чанке, при котором начинаются эффекты разрушения.");

                add("text.autoconfig.hbm_m.option.worldRadEffectsBlockChecks", "Проверок блоков в тик");
                add("text.autoconfig.hbm_m.option.worldRadEffectsBlockChecks.@Tooltip", "Количество случайных проверок блоков в затронутом чанке за один тик. Влияет на скорость разрушения. Большие значения могут повлиять на производительность.");

                add("text.autoconfig.hbm_m.option.worldRadEffectsMaxScaling", "Макс. множитель разрушения");
                add("text.autoconfig.hbm_m.option.worldRadEffectsMaxScaling.@Tooltip", "Максимальное ускорение разрушения мира при пиковой радиации. 1 = скорость не меняется, 4 = скорость может быть до 4 раз выше. Макс значение - 10х");

                add("text.autoconfig.hbm_m.option.worldRadEffectsMaxDepth", "Глубина разрушения");
                add("text.autoconfig.hbm_m.option.worldRadEffectsMaxDepth.@Tooltip", "Максимальная глубина (в блоках) от поверхности, на которую могут распространяться эффекты разрушения мира.");

                add("text.autoconfig.hbm_m.option.enableRadFogEffect", "Включить эффект радиоактивного тумана");
                add("text.autoconfig.hbm_m.option.enableRadFogEffect.@Tooltip", "Включает/выключает появление радиоактивного тумана в чанках с высоким уровнем радиации.");
                
                add("text.autoconfig.hbm_m.option.radFogThreshold", "Порог для появления тумана");
                add("text.autoconfig.hbm_m.option.radFogThreshold.@Tooltip", "Минимальный уровень фоновой радиации в чанке, при котором может появиться туман.");
                
                add("text.autoconfig.hbm_m.option.radFogChance", "Шанс появления тумана");
                add("text.autoconfig.hbm_m.option.radFogChance.@Tooltip", "Шанс появления частиц тумана в подходящем чанке за секунду. Рассчитывается как 1 к X. Чем меньше значение, тем чаще появляется туман.");

                add("text.autoconfig.hbm_m.category.player", "Игрок");
                add("text.autoconfig.hbm_m.option.maxPlayerRad", "Максимальный уровень радиации у игрока");
                add("text.autoconfig.hbm_m.option.radDecay", "Скорость распада радиации у игрока");
                add("text.autoconfig.hbm_m.option.radDamage", "Урон от радиации");
                add("text.autoconfig.hbm_m.option.radDamageThreshold", "Порог урона от радиации");
                add("text.autoconfig.hbm_m.option.radSickness", "Порог для тошноты");
                add("text.autoconfig.hbm_m.option.radWater", "Порог для негативного эффекта воды, WIP");
                add("text.autoconfig.hbm_m.option.radConfusion", "Порог для замешательства, WIP");
                add("text.autoconfig.hbm_m.option.radBlindness", "Порог для слепоты");

                add("text.autoconfig.hbm_m.category.overlay", "Экранные наложения");

                add("text.autoconfig.hbm_m.option.enableRadiationPixelEffect", "Экранный эффект радиационных помех");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectThreshold", "Порог срабатывания эффекта");
                add("text.autoconfig.hbm_m.option.radiationPixelMaxIntensityRad", "Максимальная интенсивность эффекта");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectMaxDots", "Макс. количество пикселей");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectGreenChance", "Шанс зеленого пикселя");
                add("text.autoconfig.hbm_m.option.radiationPixelMinLifetime", "Мин. время жизни пикселя");
                add("text.autoconfig.hbm_m.option.radiationPixelMaxLifetime", "Макс. время жизни пикселя");
                add("text.autoconfig.hbm_m.option.enableObstructionHighlight", "Включить подсветку препятствий");
                add("text.autoconfig.hbm_m.option.enableObstructionHighlight.@Tooltip", "Если включено, блоки, мешающие размещению мультиблока, \nбудут подсвечиваться красной рамкой.");
                add("text.autoconfig.hbm_m.option.obstructionHighlightDuration", "Длительность подсветки (сек)");
                add("text.autoconfig.hbm_m.option.obstructionHighlightDuration.@Tooltip", "Время в секундах, в течение которого будет видна подсветка препятствий.");
                add("text.autoconfig.hbm_m.option.obstructionHighlightAlpha", "Непрозрачность подсветки препятствий");
                add("text.autoconfig.hbm_m.option.obstructionHighlightAlpha.@Tooltip", "Устанавливает непрозрачность заливки подсветки.\n0% = Невидимая, 100% = Непрозрачная.");

                add("text.autoconfig.hbm_m.category.chunk", "Чанк");
                
                add("text.autoconfig.hbm_m.option.maxRad", "Максимальная радиация в чанке");
                add("text.autoconfig.hbm_m.option.fogRad", "Порог радиации для появления тумана");
                add("text.autoconfig.hbm_m.option.fogCh", "Шанс появления тумана (1 из fogCh), WIP");
                add("text.autoconfig.hbm_m.option.radChunkDecay", "Скорость распада радиации в чанке");
                add("text.autoconfig.hbm_m.option.radChunkSpreadFactor", "Фактор распространения радиации между чанками");
                add("text.autoconfig.hbm_m.option.radSpreadThreshold", "Порог распространения радиации");
                add("text.autoconfig.hbm_m.option.minRadDecayAmount", "Мин. распад радиации за тик");
                add("text.autoconfig.hbm_m.option.radSourceInfluenceFactor", "Влияние источников радиации на чанк");
                add("text.autoconfig.hbm_m.option.radRandomizationFactor", "Фактор рандомизации радиации в чанке");

                add("text.autoconfig.hbm_m.category.rendering", "Рендеринг");

                add("text.autoconfig.hbm_m.option.modelUpdateDistance", "Дистанция для рендеринга динамических частей .obj моделей");
                add("text.autoconfig.hbm_m.option.enableOcclusionCulling", "Включить куллинг моделей");

                add("text.autoconfig.hbm_m.category.debug", "Отладка");

                add("text.autoconfig.hbm_m.option.enableDebugRender", "Включить отладочный рендер радиации");
                add("text.autoconfig.hbm_m.option.debugRenderTextSize", "Размер текста отладочного рендера");
                add("text.autoconfig.hbm_m.option.debugRenderDistance", "Дальность отладочного рендеринга (чанки)");
                add("text.autoconfig.hbm_m.option.debugRenderInSurvival", "Показывать отладочный рендер в режиме выживания");
                add("text.autoconfig.hbm_m.option.enableDebugLogging", "Включить отладочные логи");

                add("text.autoconfig.hbm_m.option.enableRadiation.@Tooltip", "Если выключено, вся радиация отключается (чанки, предметы)");
                add("text.autoconfig.hbm_m.option.enableChunkRads.@Tooltip", "Если выключено, радиация в чанках всегда 0");
                add("text.autoconfig.hbm_m.option.usePrismSystem.@Tooltip", "Использовать систему PRISM для радиации в чанках (WIP)");

                add("text.autoconfig.hbm_m.option.maxPlayerRad.@Tooltip", "Максимальная радиация, которую может накопить игрок");
                add("text.autoconfig.hbm_m.option.radDecay.@Tooltip", "Скорость распада радиации у игрока за тик");
                add("text.autoconfig.hbm_m.option.radDamage.@Tooltip", "Урон за тик при превышении порога");
                add("text.autoconfig.hbm_m.option.radDamageThreshold.@Tooltip", "Игрок начинает получать урон выше этого значения");
                add("text.autoconfig.hbm_m.option.radSickness.@Tooltip", "Порог для эффекта тошноты");
                add("text.autoconfig.hbm_m.option.radWater.@Tooltip", "Порог для негативного эффекта воды (WIP)");
                add("text.autoconfig.hbm_m.option.radConfusion.@Tooltip", "Порог для эффекта замешательства (WIP)");
                add("text.autoconfig.hbm_m.option.radBlindness.@Tooltip", "Порог для эффекта слепоты");

                add("text.autoconfig.hbm_m.option.enableRadiationPixelEffect.@Tooltip", "Включает/выключает эффект случайных мерцающих пикселей на экране, когда игрок подвергается радиационному облучению.");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectThreshold.@Tooltip", "Минимальный уровень входящей радиации (в RAD/с), при котором начинает появляться эффект визуальных помех.");
                add("text.autoconfig.hbm_m.option.radiationPixelMaxIntensityRad.@Tooltip", "Уровень входящей радиации (в RAD/с), при котором эффект помех достигает своей максимальной силы (максимальное количество пикселей).");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectMaxDots.@Tooltip", "Максимальное количество пикселей, которое может одновременно находиться на экране при пиковой интенсивности эффекта. Влияет на производительность на слабых системах.");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectGreenChance.@Tooltip", "Вероятность (от 0.0 до 1.0), что новый появившийся пиксель будет зеленым, а не белым. Например, 0.1 = 10% шанс.");
                add("text.autoconfig.hbm_m.option.radiationPixelMinLifetime.@Tooltip", "Минимальное время (в тиках), которое один пиксель будет оставаться на экране. 20 тиков = 1 секунда.");
                add("text.autoconfig.hbm_m.option.radiationPixelMaxLifetime.@Tooltip", "Максимальное время (в тиках), которое один пиксель будет оставаться на экране. Для каждого пикселя выбирается случайное значение между минимальным и максимальным временем жизни.");

                add("text.autoconfig.hbm_m.option.maxRad.@Tooltip", "Максимальная радиация в чанке");
                add("text.autoconfig.hbm_m.option.fogRad.@Tooltip", "Порог радиации для появления тумана (WIP)");
                add("text.autoconfig.hbm_m.option.fogCh.@Tooltip", "Шанс появления тумана (WIP)");
                add("text.autoconfig.hbm_m.option.radChunkDecay.@Tooltip", "Скорость распада радиации в чанке");
                add("text.autoconfig.hbm_m.option.radChunkSpreadFactor.@Tooltip", "Сколько радиации распространяется на соседние чанки");
                add("text.autoconfig.hbm_m.option.radSpreadThreshold.@Tooltip", "Ниже этого значения радиация не распространяется");
                add("text.autoconfig.hbm_m.option.minRadDecayAmount.@Tooltip", "Минимальный распад радиации за тик в чанке");
                add("text.autoconfig.hbm_m.option.radSourceInfluenceFactor.@Tooltip", "Влияние источников радиации на чанк.");
                add("text.autoconfig.hbm_m.option.radRandomizationFactor.@Tooltip", "Фактор рандомизации радиации в чанке");

                add("text.autoconfig.hbm_m.option.modelUpdateDistance.@Tooltip", "Дистанция для рендеринга динамических частей .obj моделей (в чанках)");
                add("text.autoconfig.hbm_m.option.enableOcclusionCulling.@Tooltip", "Включить куллинг моделей (выключите, если ваши модели рендерятся некорректно)");

                add("text.autoconfig.hbm_m.option.enableDebugRender.@Tooltip", "Показывать отладочный оверлей радиации в чанках (F3)");
                add("text.autoconfig.hbm_m.option.debugRenderTextSize.@Tooltip", "Размер текста для отладочного оверлея");
                add("text.autoconfig.hbm_m.option.debugRenderDistance.@Tooltip", "Дальность отладочного рендеринга (чанки)");
                add("text.autoconfig.hbm_m.option.debugRenderInSurvival.@Tooltip", "Показывать отладочный рендер в режиме выживания");
                add("text.autoconfig.hbm_m.option.enableDebugLogging.@Tooltip", "Если выключено, будет активно глубокое логгирование игровых событий. Не стоит включать, если не испытываете проблем");
                break;
            
            case "en_us":

                // TABS
                add("itemGroup.hbm_m.ntm_resources_tab", "NTM Ingots and Resources");
                add("itemGroup.hbm_m.ntm_fuel_tab", "NTM Fuel and Machine Components");
                add("itemGroup.hbm_m.ntm_templates_tab", "NTM Templates");
                add("itemGroup.hbm_m.ntm_ores_tab", "NTM Ores and Blocks");
                add("itemGroup.hbm_m.ntm_machines_tab", "NTM Machines");
                add("itemGroup.hbm_m.ntm_bombs_tab", "NTM Bombs");
                add("itemGroup.hbm_m.ntm_missiles_tab", "NTM Missiles and Satellites");
                add("itemGroup.hbm_m.ntm_weapons_tab", "NTM Weapons and Turrets");
                add("itemGroup.hbm_m.ntm_consumables_tab", "NTM Consumables and Equipment");
                add("itemGroup.hbm_m.ntm_spareparts_tab", "NTM Spare Parts");
                add("itemGroup.hbm_m.ntm_instruments_tab", "NTM Instruments");
                add("itemGroup.hbm_m.ntm_building_tab", "NTM Building Blocks");


                // EQUIPMENT
                add("item.hbm_m.alloy_sword", "Alloy Sword");
                add("item.hbm_m.alloy_pickaxe", "Alloy Pickaxe");
                add("item.hbm_m.alloy_axe", "Alloy Axe");
                add("item.hbm_m.alloy_hoe", "Alloy Hoe");
                add("item.hbm_m.alloy_shovel", "Alloy Shovel");

                add("item.hbm_m.steel_sword", "Steel Sword");
                add("item.hbm_m.steel_pickaxe", "Steel Pickaxe");
                add("item.hbm_m.steel_axe", "Steel Axe");
                add("item.hbm_m.steel_hoe", "Steel Hoe");
                add("item.hbm_m.steel_shovel", "Steel Shovel");

                add("gui.hbm_m.energy", "Energy: %s/%s HE");
                add("gui.hbm_m.shredder.blade_warning.title", "Blades missing!");
                add("gui.hbm_m.shredder.blade_warning.desc", "Install or repair the shredder blades.");
                add("item.hbm_m.titanium_sword", "Titanium Sword");
                add("item.hbm_m.titanium_pickaxe", "Titanium Pickaxe");
                add("item.hbm_m.titanium_axe", "Titanium Axe");
                add("item.hbm_m.titanium_hoe", "Titanium Hoe");
                add("item.hbm_m.titanium_shovel", "Titanium Shovel");

                add("item.hbm_m.starmetal_sword", "Starmetal Sword");
                add("item.hbm_m.starmetal_pickaxe", "Starmetal Pickaxe");
                add("item.hbm_m.starmetal_axe", "Starmetal Axe");
                add("item.hbm_m.starmetal_hoe", "Starmetal Hoe");
                add("item.hbm_m.starmetal_shovel", "Starmetal Shovel");

                // ARMOR

                add("item.hbm_m.alloy_helmet", "Alloy Helmet");
                add("item.hbm_m.alloy_chestplate", "Alloy Chestplate");
                add("item.hbm_m.alloy_leggings", "Alloy Leggings");
                add("item.hbm_m.alloy_boots", "Alloy Boots");

                add("item.hbm_m.cobalt_helmet", "Cobalt Helmet");
                add("item.hbm_m.cobalt_chestplate", "Cobalt Chestplate");
                add("item.hbm_m.cobalt_leggings", "Cobalt Leggings");
                add("item.hbm_m.cobalt_boots", "Cobalt Boots");

                add("item.hbm_m.titanium_helmet", "Titanium Helmet");
                add("item.hbm_m.titanium_chestplate", "Titanium Chestplate");
                add("item.hbm_m.titanium_leggings", "Titanium Leggings");
                add("item.hbm_m.titanium_boots", "Titanium Boots");

                add("item.hbm_m.security_helmet", "Security Helmet");
                add("item.hbm_m.security_chestplate", "Security Chestplate");
                add("item.hbm_m.security_leggings", "Security Leggings");
                add("item.hbm_m.security_boots", "Security Boots");

                add("item.hbm_m.ajr_helmet", "Steel Ranger Helmet");
                add("item.hbm_m.ajr_chestplate", "Steel Ranger Chestplate");
                add("item.hbm_m.ajr_leggings", "Steel Ranger Leggings");
                add("item.hbm_m.ajr_boots", "Steel Ranger Boots");

                add("item.hbm_m.steel_helmet", "Steel Helmet");
                add("item.hbm_m.steel_chestplate", "Steel Chestplate");
                add("item.hbm_m.steel_leggings", "Steel Leggings");
                add("item.hbm_m.steel_boots", "Steel Boots");

                add("item.hbm_m.asbestos_helmet", "Fire Proximity Helmet");
                add("item.hbm_m.asbestos_chestplate", "Fire Proximity Chestplate");
                add("item.hbm_m.asbestos_leggings", "Fire Proximity Leggings");
                add("item.hbm_m.asbestos_boots", "Fire Proximity Boots");

                add("item.hbm_m.hazmat_helmet", "Hazmat Helmet");
                add("item.hbm_m.hazmat_chestplate", "Hazmat Chestplate");
                add("item.hbm_m.hazmat_leggings", "Hazmat Leggings");
                add("item.hbm_m.hazmat_boots", "Hazmat Boots");

                add("item.hbm_m.liquidator_helmet", "Liquidator Suit Helmet");
                add("item.hbm_m.liquidator_chestplate", "Liquidator Suit Chestplate");
                add("item.hbm_m.liquidator_leggings", "Liquidator Suit Leggings");
                add("item.hbm_m.liquidator_boots", "Liquidator Suit Boots");

                add("item.hbm_m.paa_helmet", "PaA Battle Hazmat Suit Helmet");
                add("item.hbm_m.paa_chestplate", "PaA Chest Protection Plate");
                add("item.hbm_m.paa_leggings", "PaA Leg Reinforcements");
                add("item.hbm_m.paa_boots", "PaA ''good ol` shoes''");

                add("item.hbm_m.starmetal_helmet", "Starmetal Helmet");
                add("item.hbm_m.starmetal_chestplate", "Starmetal Chestplate");
                add("item.hbm_m.starmetal_leggings", "Starmetal Leggings");
                add("item.hbm_m.starmetal_boots", "Starmetal Boots");

                // ITEMS
                add(ModItems.CANNED_ASBESTOS.get(), "Canned Asbestos");
                add(ModItems.CANNED_ASS.get(), "Canned Ass Meat");
                add(ModItems.CANNED_BARK.get(), "Canned Bark");
                add(ModItems.CANNED_BEEF.get(), "Canned Beef");
                add(ModItems.CANNED_BHOLE.get(), "Canned Black Hole");
                add(ModItems.CANNED_CHEESE.get(), "Canned Cheese");
                add(ModItems.CANNED_CHINESE.get(), "Canned Chinese");
                add(ModItems.CANNED_DIESEL.get(), "Canned Diesel");
                add(ModItems.CANNED_FIST.get(), "Canned Fist");
                add(ModItems.CANNED_FRIED.get(), "Canned Fried");
                add(ModItems.CANNED_HOTDOGS.get(), "Canned Hotdogs");
                add(ModItems.CANNED_JIZZ.get(), "Mystery Canned Item");
                add(ModItems.CANNED_KEROSENE.get(), "Canned Kerosene");
                add(ModItems.CANNED_LEFTOVERS.get(), "Canned Leftovers");
                add(ModItems.CANNED_MILK.get(), "Canned Milk");
                add(ModItems.CANNED_MYSTERY.get(), "Canned Mystery");
                add(ModItems.CANNED_NAPALM.get(), "Canned Napalm");
                add(ModItems.CANNED_OIL.get(), "Canned Oil");
                add(ModItems.CANNED_PASHTET.get(), "Canned Pate");
                add(ModItems.CANNED_PIZZA.get(), "Canned Pizza");
                add(ModItems.CANNED_RECURSION.get(), "Canned Recursion");
                add(ModItems.CANNED_SPAM.get(), "Canned Spam");
                add(ModItems.CANNED_STEW.get(), "Canned Stew");
                add(ModItems.CANNED_TOMATO.get(), "Canned Tomato");
                add(ModItems.CANNED_TUNA.get(), "Canned Tuna");
                add(ModItems.CANNED_TUBE.get(), "Canned Tube");
                add(ModItems.CANNED_YOGURT.get(), "Canned Yogurt");
                add(ModItems.CAN_BEPIS.get(), "Can of Bepis");
                add(ModItems.CAN_BREEN.get(), "Can of Breen");
                add(ModItems.CAN_CREATURE.get(), "Can of Creature");
                add(ModItems.CAN_EMPTY.get(), "Empty Can");
                add(ModItems.CAN_KEY.get(), "Can Key");
                add(ModItems.CAN_LUNA.get(), "Can of Luna");
                add(ModItems.CAN_MRSUGAR.get(), "Can of Mrsugar");
                add(ModItems.CAN_MUG.get(), "Can of Mug");
                add(ModItems.CAN_OVERCHARGE.get(), "Can of Overcharge");
                add(ModItems.CAN_REDBOMB.get(), "Can of Redbomb");
                add(ModItems.CAN_SMART.get(), "Can of Smart");


                add(ModItems.BATTERY_POTATO.get(), "Potato Battery");
                add(ModItems.BATTERY.get(), "Battery");
                add(ModItems.BATTERY_RED_CELL.get(), "Red Energy Cell");
                add(ModItems.BATTERY_RED_CELL_6.get(), "Red Energy Cell x6");
                add(ModItems.BATTERY_RED_CELL_24.get(), "Red Energy Cell x24");
                add(ModItems.BATTERY_ADVANCED.get(), "Advanced Battery");
                add(ModItems.BATTERY_ADVANCED_CELL.get(), "Advanced Energy Cell");
                add(ModItems.BATTERY_ADVANCED_CELL_4.get(), "Advanced Energy Cell x4");
                add(ModItems.BATTERY_ADVANCED_CELL_12.get(), "Advanced Energy Cell x12");
                add(ModItems.BATTERY_LITHIUM.get(), "Lithium Battery");
                add(ModItems.BATTERY_LITHIUM_CELL.get(), "Lithium Energy Cell");
                add(ModItems.BATTERY_LITHIUM_CELL_3.get(), "Lithium Energy Cell x3");
                add(ModItems.BATTERY_LITHIUM_CELL_6.get(), "Lithium Energy Cell x6");
                add(ModItems.BATTERY_SCHRABIDIUM.get(), "Schrabidium Battery");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL.get(), "Schrabidium Energy Cell");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL_2.get(), "Schrabidium Energy Cell x2");
                add(ModItems.BATTERY_SCHRABIDIUM_CELL_4.get(), "Schrabidium Energy Cell x4");
                add(ModItems.BATTERY_SPARK.get(), "Spark Battery");
                add(ModItems.BATTERY_TRIXITE.get(), "Trixite Battery");
                add(ModItems.BATTERY_SPARK_CELL_6.get(), "Spark Energy Cell x6");
                add(ModItems.BATTERY_SPARK_CELL_25.get(), "Spark Energy Cell x25");
                add(ModItems.BATTERY_SPARK_CELL_100.get(), "Spark Energy Cell x100");
                add(ModItems.BATTERY_SPARK_CELL_1000.get(), "Spark Energy Cell x1000");
                add(ModItems.BATTERY_SPARK_CELL_2500.get(), "Spark Energy Cell x2500");
                add(ModItems.BATTERY_SPARK_CELL_10000.get(), "Spark Energy Cell x10000");
                add(ModItems.BATTERY_SPARK_CELL_POWER.get(), "Spark Power Cell");

                add(ModItems.WIRE_RED_COPPER.get(), "Red Copper Wire");
                add(ModItems.WIRE_COPPER.get(), "Copper Wire");
                add(ModItems.WIRE_ALUMINIUM.get(), "Aluminium Wire");
                add(ModItems.WIRE_GOLD.get(), "Golden Wire");
                add(ModItems.WIRE_TUNGSTEN.get(), "Tungsten Wire");
                add(ModItems.WIRE_MAGNETIZED_TUNGSTEN.get(), "Magnetized Tungsten Wire");
                add(ModItems.WIRE_FINE.get(), "Fine Wire");
                add(ModItems.WIRE_CARBON.get(), "Lead Wire");
                add(ModItems.WIRE_SCHRABIDIUM.get(), "Shrabidium Wire");
                add(ModItems.WIRE_ADVANCED_ALLOY.get(), "Advanced Alloy Wire");

                add(ModItems.STAMP_STONE_FLAT.get(), "Stone Flat Stamp");
                add(ModItems.STAMP_STONE_PLATE.get(), "Stone Plate Stamp");
                add(ModItems.STAMP_STONE_WIRE.get(), "Stone Wire Stamp");
                add(ModItems.STAMP_STONE_CIRCUIT.get(), "Stone Circuit Stamp");
                add(ModItems.STAMP_IRON_FLAT.get(), "Iron Flat Stamp");
                add(ModItems.STAMP_IRON_PLATE.get(), "Iron Plate Stamp");
                add(ModItems.STAMP_IRON_WIRE.get(), "Iron Wire Stamp");
                add(ModItems.STAMP_IRON_CIRCUIT.get(), "Iron Circuit Stamp");
                add(ModItems.STAMP_STEEL_FLAT.get(), "Steel Flat Stamp");
                add(ModItems.STAMP_STEEL_PLATE.get(), "Steel Plate Stamp");
                add(ModItems.STAMP_STEEL_WIRE.get(), "Steel Wire Stamp");
                add(ModItems.STAMP_STEEL_CIRCUIT.get(), "Steel Circuit Stamp");
                add(ModItems.STAMP_TITANIUM_FLAT.get(), "Titanium Flat Stamp");
                add(ModItems.STAMP_TITANIUM_PLATE.get(), "Titanium Plate Stamp");
                add(ModItems.STAMP_TITANIUM_WIRE.get(), "Titanium Wire Stamp");
                add(ModItems.STAMP_TITANIUM_CIRCUIT.get(), "Titanium Circuit Stamp");
                add(ModItems.STAMP_OBSIDIAN_FLAT.get(), "Obsidian Flat Stamp");
                add(ModItems.STAMP_OBSIDIAN_PLATE.get(), "Obsidian Plate Stamp");
                add(ModItems.STAMP_OBSIDIAN_WIRE.get(), "Obsidian Wire Stamp");
                add(ModItems.STAMP_OBSIDIAN_CIRCUIT.get(), "Obsidian Circuit Stamp");
                add(ModItems.STAMP_DESH_FLAT.get(), "Desh Flat Stamp");
                add(ModItems.STAMP_DESH_PLATE.get(), "Desh Plate Stamp");
                add(ModItems.STAMP_DESH_WIRE.get(), "Desh Wire Stamp");
                add(ModItems.STAMP_DESH_CIRCUIT.get(), "Desh Circuit Stamp");
                add(ModItems.STAMP_DESH_9.get(), "Desh 9mm Stamp");
                add(ModItems.STAMP_DESH_44.get(), "Desh .44 Magnum Stamp");
                add(ModItems.STAMP_DESH_50.get(), "Desh .50 BMG Stamp");
                add(ModItems.STAMP_DESH_357.get(), "Desh .357 Magnum Stamp");
                add(ModItems.STAMP_IRON_357.get(), "Iron .357 Magnum Stamp");
                add(ModItems.STAMP_IRON_44.get(), "Iron .44 Magnum Stamp");
                add(ModItems.STAMP_IRON_50.get(), "Iron .50 BMG Stamp");
                add(ModItems.STAMP_IRON_9.get(), "Iron 9mm Stamp");


                add(ModItems.QUANTUM_CHIP.get(), "Quantum Chip");
                add(ModItems.QUANTUM_CIRCUIT.get(), "Quantum Circuit");
                add(ModItems.QUANTUM_COMPUTER.get(), "Quantum Computer");
                add(ModItems.SILICON_CIRCUIT.get(), "Silicone Circuit");
                add(ModItems.BISMOID_CHIP.get(), "Bismoid Chip");
                add(ModItems.BISMOID_CIRCUIT.get(), "Bismoid Circuit");
                add(ModItems.CONTROLLER_CHASSIS.get(), "Controller Chassis");
                add(ModItems.CONTROLLER.get(), "Controller");
                add(ModItems.CONTROLLER_ADVANCED.get(), "Controller Advanced");
                add(ModItems.CAPACITOR_BOARD.get(), "Capacitor Board");
                add(ModItems.CAPACITOR_TANTALUM.get(), "Tantalum Capacitor");
                add(ModItems.ANALOG_CIRCUIT.get(), "Analog Circuit");
                add(ModItems.INTEGRATED_CIRCUIT.get(), "Integrated Circuit");
                add(ModItems.ADVANCED_CIRCUIT.get(), "Advancer Circuit");
                add(ModItems.MICROCHIP.get(), "Microchip");
                add(ModItems.ATOMIC_CLOCK.get(), "Atomic Clock");
                add(ModItems.VACUUM_TUBE.get(), "Vacuum Tube");
                add(ModItems.CAPACITOR.get(), "Capacitor");
                add(ModItems.PCB.get(), "PCB");
                add(ModItems.STRAWBERRY.get(), "Strawberry");


                add(ModItems.BATTLE_GEARS.get(), "Battle Gears");
                add(ModItems.BATTLE_CASING.get(), "Battle Casing");
                add(ModItems.BATTLE_COUNTER.get(), "Battle Counter");
                add(ModItems.BATTLE_SENSOR.get(), "Battle Sensor");
                add(ModItems.BATTLE_MODULE.get(), "Battle Module");
                add(ModItems.METAL_ROD.get(), "Metal Rod");
                

                add(ModItems.GRENADE.get(), "Bouncing Grenade");
                add(ModItems.GRENADEHE.get(), "Powerful Bouncing Grenade");
                add(ModItems.GRENADEFIRE.get(), "Fire Bouncing Grenade");
                add(ModItems.GRENADESLIME.get(), "Bouncy Bouncing Grenade");
                add(ModItems.GRENADESMART.get(), "Smart Bouncing Grenade");

                add(ModItems.GRENADE_IF.get(), "IF Grenade");

                add("item.hbm_m.geiger_counter", "Geiger Counter");
                add("item.hbm_m.dosimeter", "Dosimeter");
                add("item.hbm_m.battery_creative", "Creative Battery");
                add("tooltip.hbm_m.creative_battery_desc","Provides an infinite amount of power");
                add("tooltip.hbm_m.creative_battery_flavor","To infinity... and beyond!!");
                add("item.hbm_m.blueprint_folder", "Template Folder");
                add("item.hbm_m.blueprint_folder.named", "Machine Template Folder");
                add("item.hbm_m.blueprint_folder.empty", "Empty folder");
                add("item.hbm_m.blueprint_folder.obsolete", "Folder is Deprecated (Group was removed)");
                add("item.hbm_m.blueprint_folder.desc", "Insert into Assembly Machine to unlock recipes");
                add("item.hbm_m.blueprint_folder.recipes", "Contains recipes:");
                add("gui.hbm_m.recipe_from_group", "From Group:");

                add("item.hbm_m.heart_piece", "Heart Piece");
                add(ModItems.HEART_CONTAINER.get(), "Heart Container");
                add(ModItems.HEART_BOOSTER.get(), "Heart Booster");
                add(ModItems.HEART_FAB.get(), "Heart of Darkness");
                add(ModItems.BLACK_DIAMOND.get(), "Black Diamond");

                add(ModItems.GHIORSIUM_CLADDING.get(), "Ghiorsium Cladding");
                add(ModItems.DESH_CLADDING.get(), "Desh Cladding");
                add(ModItems.RUBBER_CLADDING.get(), "Rubber Cladding");
                add(ModItems.LEAD_CLADDING.get(), "Lead Cladding");
                add(ModItems.PAINT_CLADDING.get(), "Lead Paint");

                add("item.hbm_m.radaway", "Radaway");
                add("effect.hbm_m.radaway", "Radiation cleansing");
                add("sounds.hbm_m.radaway_use", "Use of radaway");

                add(ModItems.TEMPLATE_FOLDER.get(), "Template Folder");
                add(ModItems.ASSEMBLY_TEMPLATE.get(), "Assembly Template: %s");
                add("tooltip.hbm_m.template_broken", "Broken template");
                add("tooltip.hbm_m.created_with_template_folder", "Created via Template Folder");
                add("tooltip.hbm_m.output", "Output: ");
                add("tooltip.hbm_m.input", "Input: ");
                add("tooltip.hbm_m.production_time", "Production time:");
                add("tooltip.hbm_m.seconds", "seconds");
                add("tooltip.hbm_m.energy_consumption", "Energy Consumption:");
                add("tooltip.hbm_m.tags", "Тags (OreDict):");
                add("item.hbm_m.template_folder.desc", "Machine Templates: Paper + Dye$Fluid IDs: Iron Plate + Dye$Press Stamps: Flat Stamp$Siren Tracks: Insulator + Steel Plate");
                add("desc.gui.template", "Insert Assembly Template");
                add("desc.gui.assembler.warning", "No valid template!");

// === ИНСТРУМЕНТЫ И УСТРОЙСТВА ===
                add("tooltip.hbm_m.crowbar.line1", "Tool for prying open containers.");
                add("tooltip.hbm_m.crowbar.line2", "Opens crates on right-click");
                add("tooltip.hbm_m.defuser.line1", "Device for disarming mines and bombs");
                add("tooltip.hbm_m.defuser.line2", "RMB on a compatible device to disarm");
                add("tooltip.hbm_m.mine.line1", "Can be defused");
                add("tooltip.hbm_m.gigadet.line1", "Was made for fun");

                add("tooltip.hbm_m.nuclear_charge.line1", "High-yield nuclear weapon!");
                add("tooltip.hbm_m.nuclear_charge.line2", "At the moment, this is the");
                add("tooltip.hbm_m.nuclear_charge.line3", "most destructive block in our mod.");
                add("tooltip.hbm_m.nuclear_charge.line4", "If the crater loaded incorrectly");
                add("tooltip.hbm_m.nuclear_charge.line5", "or without biomes, restart the world.");

                add("tooltip.hbm_m.mine_nuke.line1", "Nuclear weapon!");
                add("tooltip.hbm_m.mine_nuke.line2", "Blast radius: 35 meters");
                add("tooltip.hbm_m.mine_nuke.line3", "Can be defused");

                add("tooltip.hbm_m.dudnuke.line1", "High-yield nuclear weapon!");
                add("tooltip.hbm_m.dudnuke.line4", "If the crater loaded incorrectly");
                add("tooltip.hbm_m.dudnuke.line5", "or without biomes, restart the world");
                add("tooltip.hbm_m.dudnuke.line6", "Can be defused");

                add("tooltip.hbm_m.dudsalted.line1", "High-yield nuclear weapon!");
                add("tooltip.hbm_m.dudsalted.line4", "If the crater loaded incorrectly");
                add("tooltip.hbm_m.dudsalted.line5", "or without biomes, restart the world");
                add("tooltip.hbm_m.dudsalted.line6", "Can be defused");

                add("tooltip.hbm_m.dudfugas.line1", "High-yield explosion!");
                add("tooltip.hbm_m.dudfugas.line6", "Can be defused");

// ДЕТОНАТОР
                add("tooltip.hbm_m.detonator.target", "Target: ");
                add("tooltip.hbm_m.detonator.no_target", "No target");
                add("tooltip.hbm_m.detonator.right_click", "RMB - Activate");
                add("tooltip.hbm_m.detonator.shift_right_click", "Shift+RMB - Set target");

// СКАНЕР КЛАСТЕРОВ
                add("tooltip.hbm_m.depth_ores_scanner.scans_chunks", "Scans chunks for");
                add("tooltip.hbm_m.depth_ores_scanner.deep_clusters", "depth clusters beneath the player");
                add("tooltip.hbm_m.depth_ores_scanner.depth_warning", "works at depth -30 and below!");
// DEPTH ORES SCANNER (сообщения)
                add("message.hbm_m.depth_ores_scanner.invalid_height", "Scanner works only at height -30 or below!");
                add("message.hbm_m.depth_ores_scanner.directly_below", "Depth cluster directly below us!");
                add("message.hbm_m.depth_ores_scanner.in_chunk", "Depth cluster found in our chunk!");
                add("message.hbm_m.depth_ores_scanner.adjacent_chunk", "Depth cluster found in adjacent chunk!");
                add("message.hbm_m.depth_ores_scanner.none_found", "No depth clusters found nearby");

// ДЕТЕКТОР НЕФТИ (тултип)
                add("tooltip.hbm_m.oil_detector.scans_chunks", "Scans chunks for");
                add("tooltip.hbm_m.oil_detector.oil_deposits", "oil deposits beneath the player");

// ДЕТЕКТОР НЕФТИ (сообщения использования)
                add("message.hbm_m.oil_detector.directly_below", "Oil deposits directly below us!");
                add("message.hbm_m.oil_detector.in_chunk", "Oil found in our chunk!");
                add("message.hbm_m.oil_detector.adjacent_chunk", "Oil deposits found in adjacent chunk!");
                add("message.hbm_m.oil_detector.none_found", "No oil deposits found nearby");

// MULTI DETONATOR TOOLTIPS
                add("tooltip.hbm_m.multi_detonator.active_point", "➤ %s:");
                add("tooltip.hbm_m.multi_detonator.point_set", "✅ %s:");
                add("tooltip.hbm_m.multi_detonator.coordinates", "   %d, %d, %d");
                add("tooltip.hbm_m.multi_detonator.point_empty", "○ Point %d:");
                add("tooltip.hbm_m.multi_detonator.not_set", "   Not set");
                add("tooltip.hbm_m.multi_detonator.key_r", "R - open menu");
                add("tooltip.hbm_m.multi_detonator.shift_rmb", "Shift+RMB - save to active point");
                add("tooltip.hbm_m.multi_detonator.rmb_activate", "RMB - activate active point");

// MULTI DETONATOR MESSAGES
                add("message.hbm_m.multi_detonator.position_saved", "Position '%s' saved: %d, %d, %d");
                add("message.hbm_m.multi_detonator.no_coordinates", "No coordinates set!");
                add("message.hbm_m.multi_detonator.point_not_set", "Point %d not set!");
                add("message.hbm_m.multi_detonator.chunk_not_loaded", "Position not loaded!");
                add("message.hbm_m.multi_detonator.activated", "%s activated!");
                add("message.hbm_m.multi_detonator.activation_error", "Activation error!");
                add("message.hbm_m.multi_detonator.incompatible_block", "Block incompatible!");
// RANGE DETONATOR
                add("tooltip.hbm_m.range_detonator.desc", "Activates compatible blocks");
                add("tooltip.hbm_m.range_detonator.hint", "along a ray up to 256 blocks.");
                add("message.hbm_m.range_detonator.pos_not_loaded", "Position incompatible or not loaded");
                add("message.hbm_m.range_detonator.activated", "Successfully activated");


                add("tooltip.hbm_m.grenade_nuc.line1", "Nuclear weapon!");
                add("tooltip.hbm_m.grenade_nuc.line2", "Blast radius: 25 meters");
                add("tooltip.hbm_m.grenade_nuc.line3", "Fuse time: 7s");
                add("tooltip.hbm_m.detminer.line1", "Does not damage entities or players");
                add("tooltip.hbm_m.detminer.line4", "Allows mining depth ores and stone");


                add("tooltip.hbm_m.grenade.common.line1", "Hand grenade");

                add("tooltip.hbm_m.grenade.smart.line2", "Detonates on direct hit with an entity");
                add("tooltip.hbm_m.grenade.fire.line2", "Leaves fire after detonation");
                add("tooltip.hbm_m.grenade.slime.line2", "Bounces strongly off surfaces");
                add("tooltip.hbm_m.grenade.standard.line2", "Weak fragmentation blast");
                add("tooltip.hbm_m.grenade.he.line2", "Enhanced high-explosive blast");
                add("tooltip.hbm_m.grenade.default.line2", "Throw it and watch it boom!");

                add("tooltip.hbm_m.grenade_if.common.line1", "IF-Grenade");

                add("tooltip.hbm_m.grenade_if.he.line2", "Powerful high-explosive blast");
                add("tooltip.hbm_m.grenade_if.slime.line2", "Bounces strongly off surfaces");
                add("tooltip.hbm_m.grenade_if.fire.line2", "Leaves fire after detonation");
                add("tooltip.hbm_m.grenade_if.standard.line2", "Standard timed explosion");
                add("tooltip.hbm_m.grenade_if.default.line2", "Throw it and wait for the boom");

                add("tooltip.hbm_m.depthstone.line1", "Can be mined or destroyed only by explosion!");
                add("tooltip.hbm_m.depthstone.line4", "Use Det Miner to safe-mine depth ores");

// en_us case
                // английский:
                add(ModItems.MAN_CORE.get(), "Plutonium Core");
                add(ModItems.CRT_DISPLAY.get(), "CRT");
                add(ModBlocks.DEPTH_STONE.get(), "Depth Stone");
                add(ModBlocks.DEPTH_CINNABAR.get(), "Deep Cinnabar Ore");
                add(ModBlocks.DEPTH_IRON.get(), "Deep Iron Ore");
                add(ModBlocks.DEPTH_ZIRCONIUM.get(), "Deep Zirconium Ore");
                add(ModBlocks.DEPTH_BORAX.get(), "Deep Borax Ore");
                add(ModBlocks.DEPTH_TUNGSTEN.get(), "Deep Tungsten Ore");
                add(ModBlocks.DEPTH_TITANIUM.get(), "Deep Titanium Ore");
                add(ModBlocks.BEDROCK_OIL.get(), "Bedrock Oil");
                add(ModBlocks.BURNED_GRASS.get(), "Burned Grass");
                add(ModBlocks.WASTE_PLANKS.get(), "Burned Planks");
                add(ModBlocks.WASTE_LOG.get(), "Burned Log");
                add(ModBlocks.CONCRETE_PILLAR.get(), "Concrete Pillar");
                add(ModBlocks.SELLAFIELD_SLAKED.get(), "Slaked Sellafield");
                add(ModBlocks.SELLAFIELD_SLAKED1.get(), "Slaked Sellafield I");
                add(ModBlocks.SELLAFIELD_SLAKED2.get(), "Slaked Sellafield II");
                add(ModBlocks.SELLAFIELD_SLAKED3.get(), "Slaked Sellafield III");
                add(ModItems.MOTOR_BISMUTH.get(), "Bismuth Motor");
                add(ModItems.MOTOR_DESH.get(), "Desh Motor");
                add(ModItems.MOTOR.get(), "Motor");
                add(ModBlocks.ASPHALT.get(), "Asphalt");
                add(ModBlocks.BARRICADE.get(), "Sand Barricade");
                add(ModBlocks.BASALT_BRICK.get(), "Basalt Bricks");
                add(ModBlocks.BASALT_POLISHED.get(), "Polished Basalt");
                add(ModBlocks.BRICK_BASE.get(), "Polished Bricks");
                add(ModBlocks.BRICK_DUCRETE.get(), "Ducrete Bricks");
                add(ModBlocks.BRICK_FIRE.get(), "Fire Bricks");
                add(ModBlocks.BRICK_LIGHT.get(), "Light Bricks");
                add(ModBlocks.BRICK_OBSIDIAN.get(), "Obsidian Bricks");
                add(ModBlocks.CONCRETE_ASBESTOS.get(), "Asbestos Concrete");
                add(ModBlocks.CONCRETE_BLACK.get(), "Black Concrete");
                add(ModBlocks.CONCRETE_BLUE.get(), "Blue Concrete");
                add(ModBlocks.CONCRETE_BROWN.get(), "Brown Concrete");
                add(ModBlocks.CONCRETE_COLORED_BRONZE.get(), "Bronze Concrete");
                add(ModBlocks.CONCRETE_COLORED_INDIGO.get(), "Indigo Concrete");
                add(ModBlocks.CONCRETE_COLORED_MACHINE.get(), "Concrete 'Machine'");
                add(ModBlocks.CONCRETE_COLORED_MACHINE_STRIPE.get(), "Striped Concrete 'Machine'");
                add(ModBlocks.CONCRETE_COLORED_PINK.get(), "Pink Concrete");
                add(ModBlocks.CONCRETE_COLORED_PURPLE.get(), "Purple Concrete");
                add(ModBlocks.CONCRETE_COLORED_SAND.get(), "Concrete 'Texas'");
                add(ModBlocks.CONCRETE_CYAN.get(), "Cyan Concrete");
                add(ModBlocks.CONCRETE_GRAY.get(), "Gray Concrete");
                add(ModBlocks.CONCRETE_GREEN.get(), "Green Concrete");
                add(ModBlocks.CONCRETE_LIGHT_BLUE.get(), "Light Blue Concrete");
                add(ModBlocks.CONCRETE_LIME.get(), "Lime Concrete");
                add(ModBlocks.CONCRETE_MAGENTA.get(), "Magenta Concrete");
                add(ModBlocks.CONCRETE_ORANGE.get(), "Orange Concrete");
                add(ModBlocks.CONCRETE_PINK.get(), "Pink Concrete");
                add(ModBlocks.CONCRETE_PURPLE.get(), "Purple Concrete");
                add(ModBlocks.CONCRETE_REBAR.get(), "Rough Concrete");
                add(ModBlocks.CONCRETE_REBAR_ALT.get(), "Rebar Concrete");
                add(ModBlocks.CONCRETE_RED.get(), "Red Concrete");
                add(ModBlocks.CONCRETE_SILVER.get(), "Silver Concrete");
                add(ModBlocks.CONCRETE_SUPER.get(), "Super Concrete");
                add(ModBlocks.CONCRETE_SUPER_BROKEN.get(), "Broken Super Concrete");
                add(ModBlocks.CONCRETE_SUPER_M0.get(), "Super Concrete MO");
                add(ModBlocks.CONCRETE_SUPER_M1.get(), "Super Concrete M1");
                add(ModBlocks.CONCRETE_SUPER_M2.get(), "Super Concrete M2");
                add(ModBlocks.CONCRETE_SUPER_M3.get(), "Super Concrete M3");
                add(ModBlocks.CONCRETE_TILE.get(), "Concrete Tile");
                add(ModBlocks.CONCRETE_TILE_TREFOIL.get(), "Marked Concrete Tile");
                add(ModBlocks.CONCRETE_WHITE.get(), "White Concrete");
                add(ModBlocks.CONCRETE_YELLOW.get(), "Yellow Concrete");
                add(ModBlocks.CONCRETE_FLAT.get(), "Flat Concrete");
                add(ModBlocks.DEPTH_BRICK.get(), "Depth Bricks");
                add(ModBlocks.DEPTH_NETHER_BRICK.get(), "Depth Nether Bricks");
                add(ModBlocks.DEPTH_NETHER_TILES.get(), "Depth Nether Tiles");
                add(ModBlocks.DEPTH_STONE_NETHER.get(), "Depth Nether Stone");
                add(ModBlocks.DEPTH_TILES.get(), "Depth Tiles");
                add(ModBlocks.GNEISS_BRICK.get(), "Gneiss Brick");
                add(ModBlocks.GNEISS_CHISELED.get(), "Chiseled Gneiss");
                add(ModBlocks.GNEISS_STONE.get(), "Graphite Slate");
                add(ModBlocks.GNEISS_TILE.get(), "Graphite Slate Tile");
                add(ModBlocks.METEOR.get(), "Meteor Block");
                add(ModBlocks.METEOR_BRICK.get(), "Meteor Bricks");
                add(ModBlocks.METEOR_BRICK_CHISELED.get(), "Chiseled Meteor Bricks");
                add(ModBlocks.METEOR_BRICK_CRACKED.get(), "Cracked Meteor Bricks");
                add(ModBlocks.METEOR_BRICK_MOSSY.get(), "Mossy Meteor Bricks");
                add(ModBlocks.METEOR_COBBLE.get(), "Meteor Cobble");
                add(ModBlocks.METEOR_CRUSHED.get(), "Crushed Meteor");
                add(ModBlocks.METEOR_PILLAR.get(), "Meteor Pillar");
                add(ModBlocks.METEOR_POLISHED.get(), "Polished Meteor");
                add(ModBlocks.METEOR_TREASURE.get(), "Meteor Treasure");
                add(ModBlocks.VINYL_TILE.get(), "Vinyl Tiles");
                add(ModBlocks.VINYL_TILE_SMALL.get(), "Small Vinyl Tiles");


                add(ModBlocks.CONCRETE_ASBESTOS_SLAB.get(), "Asbestos Concrete Slab");
                add(ModBlocks.CONCRETE_BLACK_SLAB.get(), "Black Concrete Slab");
                add(ModBlocks.CONCRETE_BLUE_SLAB.get(), "Blue Concrete Slab");
                add(ModBlocks.CONCRETE_BROWN_SLAB.get(), "Brown Concrete Slab");
                add(ModBlocks.CONCRETE_COLORED_BRONZE_SLAB.get(), "Bronze Concrete Slab");
                add(ModBlocks.CONCRETE_COLORED_INDIGO_SLAB.get(), "Indigo Concrete Slab");
                add(ModBlocks.CONCRETE_COLORED_MACHINE_SLAB.get(), "Concrete 'Machine' Slab");
                add(ModBlocks.CONCRETE_COLORED_PINK_SLAB.get(), "Pink Concrete Slab");
                add(ModBlocks.CONCRETE_COLORED_PURPLE_SLAB.get(), "Purple Concrete Slab");
                add(ModBlocks.CONCRETE_COLORED_SAND_SLAB.get(), "Concrete 'Texas' Slab");
                add(ModBlocks.CONCRETE_CYAN_SLAB.get(), "Cyan Concrete Slab");
                add(ModBlocks.CONCRETE_GRAY_SLAB.get(), "Gray Concrete Slab");
                add(ModBlocks.CONCRETE_GREEN_SLAB.get(), "Green Concrete Slab");
                add(ModBlocks.CONCRETE_LIGHT_BLUE_SLAB.get(), "Light Blue Concrete Slab");
                add(ModBlocks.CONCRETE_LIME_SLAB.get(), "Lime Concrete Slab");
                add(ModBlocks.CONCRETE_MAGENTA_SLAB.get(), "Magenta Concrete Slab");
                add(ModBlocks.CONCRETE_ORANGE_SLAB.get(), "Orange Concrete Slab");
                add(ModBlocks.CONCRETE_PINK_SLAB.get(), "Pink Concrete Slab");
                add(ModBlocks.CONCRETE_PURPLE_SLAB.get(), "Purple Concrete Slab");
                add(ModBlocks.CONCRETE_RED_SLAB.get(), "Red Concrete Slab");
                add(ModBlocks.CONCRETE_SILVER_SLAB.get(), "Silver Concrete Slab");
                add(ModBlocks.CONCRETE_WHITE_SLAB.get(), "White Concrete Slab");
                add(ModBlocks.CONCRETE_YELLOW_SLAB.get(), "Yellow Concrete Slab");
                add(ModBlocks.CONCRETE_SUPER_SLAB.get(), "Super Concrete Slab");
                add(ModBlocks.CONCRETE_SUPER_M0_SLAB.get(), "Super Concrete M0 Slab");
                add(ModBlocks.CONCRETE_SUPER_M1_SLAB.get(), "Super Concrete M1 Slab");
                add(ModBlocks.CONCRETE_SUPER_M2_SLAB.get(), "Super Concrete M2 Slab");
                add(ModBlocks.CONCRETE_SUPER_M3_SLAB.get(), "Super Concrete M3 Slab");
                add(ModBlocks.CONCRETE_SUPER_BROKEN_SLAB.get(), "Broken Super Concrete Slab");
                add(ModBlocks.CONCRETE_REBAR_SLAB.get(), "Rough Concrete Slab");
                add(ModBlocks.CONCRETE_FLAT_SLAB.get(), "Flat Concrete Slab");
                add(ModBlocks.CONCRETE_TILE_SLAB.get(), "Concrete Tile Slab");
                add(ModBlocks.DEPTH_BRICK_SLAB.get(), "Depth Bricks Slab");
                add(ModBlocks.DEPTH_TILES_SLAB.get(), "Depth Tiles Slab");
                add(ModBlocks.DEPTH_NETHER_BRICK_SLAB.get(), "Depth Nether Bricks Slab");
                add(ModBlocks.DEPTH_NETHER_TILES_SLAB.get(), "Depth Nether Tiles Slab");
                add(ModBlocks.GNEISS_TILE_SLAB.get(), "Graphite Slate Tile Slab");
                add(ModBlocks.GNEISS_BRICK_SLAB.get(), "Gneiss Brick Slab");
                add(ModBlocks.BRICK_BASE_SLAB.get(), "Polished Bricks Slab");
                add(ModBlocks.BRICK_LIGHT_SLAB.get(), "Light Bricks Slab");
                add(ModBlocks.BRICK_FIRE_SLAB.get(), "Fire Bricks Slab");
                add(ModBlocks.BRICK_OBSIDIAN_SLAB.get(), "Obsidian Bricks Slab");
                add(ModBlocks.VINYL_TILE_SLAB.get(), "Vinyl Tiles Slab");
                add(ModBlocks.VINYL_TILE_SMALL_SLAB.get(), "Small Vinyl Tiles Slab");
                add(ModBlocks.BRICK_DUCRETE_SLAB.get(), "Ducrete Bricks Slab");
                add(ModBlocks.ASPHALT_SLAB.get(), "Asphalt Slab");
                add(ModBlocks.BASALT_POLISHED_SLAB.get(), "Polished Basalt Slab");
                add(ModBlocks.BASALT_BRICK_SLAB.get(), "Basalt Bricks Slab");
                add(ModBlocks.METEOR_POLISHED_SLAB.get(), "Polished Meteor Slab");
                add(ModBlocks.METEOR_BRICK_SLAB.get(), "Meteor Bricks Slab");
                add(ModBlocks.METEOR_BRICK_CRACKED_SLAB.get(), "Cracked Meteor Bricks Slab");
                add(ModBlocks.METEOR_BRICK_MOSSY_SLAB.get(), "Mossy Meteor Bricks Slab");
                add(ModBlocks.METEOR_CRUSHED_SLAB.get(), "Crushed Meteor Slab");

                add(ModBlocks.CONCRETE_ASBESTOS_STAIRS.get(), "Asbestos Concrete Stairs");
                add(ModBlocks.CONCRETE_BLACK_STAIRS.get(), "Black Concrete Stairs");
                add(ModBlocks.CONCRETE_BLUE_STAIRS.get(), "Blue Concrete Stairs");
                add(ModBlocks.CONCRETE_BROWN_STAIRS.get(), "Brown Concrete Stairs");
                add(ModBlocks.CONCRETE_COLORED_BRONZE_STAIRS.get(), "Bronze Concrete Stairs");
                add(ModBlocks.CONCRETE_COLORED_INDIGO_STAIRS.get(), "Indigo Concrete Stairs");
                add(ModBlocks.CONCRETE_COLORED_MACHINE_STAIRS.get(), "Concrete 'Machine' Stairs");
                add(ModBlocks.CONCRETE_COLORED_PINK_STAIRS.get(), "Pink Concrete Stairs");
                add(ModBlocks.CONCRETE_COLORED_PURPLE_STAIRS.get(), "Purple Concrete Stairs");
                add(ModBlocks.CONCRETE_COLORED_SAND_STAIRS.get(), "Concrete 'Texas' Stairs");
                add(ModBlocks.CONCRETE_CYAN_STAIRS.get(), "Cyan Concrete Stairs");
                add(ModBlocks.CONCRETE_GRAY_STAIRS.get(), "Gray Concrete Stairs");
                add(ModBlocks.CONCRETE_GREEN_STAIRS.get(), "Green Concrete Stairs");
                add(ModBlocks.CONCRETE_LIGHT_BLUE_STAIRS.get(), "Light Blue Concrete Stairs");
                add(ModBlocks.CONCRETE_LIME_STAIRS.get(), "Lime Concrete Stairs");
                add(ModBlocks.CONCRETE_MAGENTA_STAIRS.get(), "Magenta Concrete Stairs");
                add(ModBlocks.CONCRETE_ORANGE_STAIRS.get(), "Orange Concrete Stairs");
                add(ModBlocks.CONCRETE_PINK_STAIRS.get(), "Pink Concrete Stairs");
                add(ModBlocks.CONCRETE_PURPLE_STAIRS.get(), "Purple Concrete Stairs");
                add(ModBlocks.CONCRETE_RED_STAIRS.get(), "Red Concrete Stairs");
                add(ModBlocks.CONCRETE_SILVER_STAIRS.get(), "Silver Concrete Stairs");
                add(ModBlocks.CONCRETE_WHITE_STAIRS.get(), "White Concrete Stairs");
                add(ModBlocks.CONCRETE_YELLOW_STAIRS.get(), "Yellow Concrete Stairs");
                add(ModBlocks.CONCRETE_SUPER_STAIRS.get(), "Super Concrete Stairs");
                add(ModBlocks.CONCRETE_SUPER_M0_STAIRS.get(), "Super Concrete M0 Stairs");
                add(ModBlocks.CONCRETE_SUPER_M1_STAIRS.get(), "Super Concrete M1 Stairs");
                add(ModBlocks.CONCRETE_SUPER_M2_STAIRS.get(), "Super Concrete M2 Stairs");
                add(ModBlocks.CONCRETE_SUPER_M3_STAIRS.get(), "Super Concrete M3 Stairs");
                add(ModBlocks.CONCRETE_SUPER_BROKEN_STAIRS.get(), "Broken Super Concrete Stairs");
                add(ModBlocks.CONCRETE_REBAR_STAIRS.get(), "Rough Concrete Stairs");
                add(ModBlocks.CONCRETE_FLAT_STAIRS.get(), "Flat Concrete Stairs");
                add(ModBlocks.CONCRETE_TILE_STAIRS.get(), "Concrete Tile Stairs");
                add(ModBlocks.DEPTH_BRICK_STAIRS.get(), "Depth Bricks Stairs");
                add(ModBlocks.DEPTH_TILES_STAIRS.get(), "Depth Tiles Stairs");
                add(ModBlocks.DEPTH_NETHER_BRICK_STAIRS.get(), "Depth Nether Bricks Stairs");
                add(ModBlocks.DEPTH_NETHER_TILES_STAIRS.get(), "Depth Nether Tiles Stairs");
                add(ModBlocks.GNEISS_TILE_STAIRS.get(), "Graphite Slate Tile Stairs");
                add(ModBlocks.GNEISS_BRICK_STAIRS.get(), "Gneiss Brick Stairs");
                add(ModBlocks.BRICK_BASE_STAIRS.get(), "Polished Bricks Stairs");
                add(ModBlocks.BRICK_LIGHT_STAIRS.get(), "Light Bricks Stairs");
                add(ModBlocks.BRICK_FIRE_STAIRS.get(), "Fire Bricks Stairs");
                add(ModBlocks.BRICK_OBSIDIAN_STAIRS.get(), "Obsidian Bricks Stairs");
                add(ModBlocks.VINYL_TILE_STAIRS.get(), "Vinyl Tiles Stairs");
                add(ModBlocks.VINYL_TILE_SMALL_STAIRS.get(), "Small Vinyl Tiles Stairs");
                add(ModBlocks.BRICK_DUCRETE_STAIRS.get(), "Ducrete Bricks Stairs");
                add(ModBlocks.ASPHALT_STAIRS.get(), "Asphalt Stairs");
                add(ModBlocks.BASALT_POLISHED_STAIRS.get(), "Polished Basalt Stairs");
                add(ModBlocks.BASALT_BRICK_STAIRS.get(), "Basalt Bricks Stairs");
                add(ModBlocks.METEOR_POLISHED_STAIRS.get(), "Polished Meteor Stairs");
                add(ModBlocks.METEOR_BRICK_STAIRS.get(), "Meteor Bricks Stairs");
                add(ModBlocks.METEOR_BRICK_CRACKED_STAIRS.get(), "Cracked Meteor Bricks Stairs");
                add(ModBlocks.METEOR_BRICK_MOSSY_STAIRS.get(), "Mossy Meteor Bricks Stairs");
                add(ModBlocks.METEOR_CRUSHED_STAIRS.get(), "Crushed Meteor Stairs");

                add(ModItems.COIL_TUNGSTEN.get(), "Heating Element");
                add(ModBlocks.CONVERTER_BLOCK.get(), "Energy Converter");
                add(ModBlocks.MACHINE_BATTERY_DINEUTRONIUM.get(), "Spark Battery");
                add(ModBlocks.MACHINE_BATTERY_SCHRABIDIUM.get(), "Shrabidium Battery");
                add(ModBlocks.MACHINE_BATTERY_LITHIUM.get(), "Lithium Battery");
                // en_us case

                add(ModItems.COIL_MAGNETIZED_TUNGSTEN_TORUS.get(), "Magnetized Tungsten Torus Coil");
                add(ModItems.COIL_MAGNETIZED_TUNGSTEN.get(), "Magnetized Tungsten Coil");
                add(ModItems.COIL_ADVANCED_ALLOY_TORUS.get(), "Advanced Alloy Torus Coil");
                add(ModItems.COIL_ADVANCED_ALLOY.get(), "Advanced Alloy Coil");
                add(ModItems.COIL_COPPER_TORUS.get(), "Copper Torus Coil");
                add(ModItems.COIL_COPPER.get(), "Copper Coil");

                add(ModItems.COIL_GOLD_TORUS.get(), "Golden Torus Coil");
                add(ModItems.COIL_GOLD.get(), "Golden Coil");

                add(ModItems.DUST.get(), "Dust");
                add(ModItems.DUST_TINY.get(), "Tiny Dust");
                add(ModItems.SCRAP.get(), "Scrap");
                add(ModItems.POWDER_COAL.get(), "Coal Powder");
                add(ModItems.POWDER_COAL_SMALL.get(), "Tiny Coal Powder");
                add(ModItems.BILLET_PLUTONIUM.get(), "Plutonium Billet");

                add(ModItems.BLADE_TEST.get(), "Desh Blades");
                add(ModItems.BLADE_STEEL.get(), "Steel Blades");
                add(ModItems.BLADE_TITANIUM.get(), "Titanium Blades");
                add(ModItems.BLADE_ALLOY.get(), "Advanced Alloy Blades");
                add(ModItems.BORAX.get(), "Borax");
                add(ModItems.BALL_TNT.get(), "TNT Ball");
                add(ModItems.BOLT_STEEL.get(), "Steel Bolt");
                add(ModItems.ZIRCONIUM_SHARP.get(), "Zirconium Sharp");
                add(ModBlocks.CRATE_CONSERVE.get(), "Canned Goods Crate");
                add(ModBlocks.CAGE_LAMP.get(), "Cage Lamp");
                add(ModBlocks.FLOOD_LAMP.get(), "Flood Lamp");
                add(ModBlocks.B29.get(), "B-29");
                add(ModBlocks.DORNIER.get(), "Dornier");
                add(ModBlocks.FILE_CABINET.get(), "File Cabinet");
                add(ModBlocks.TAPE_RECORDER.get(), "Tape Recorder");
                add(ModBlocks.CRT_BROKEN.get(), "Broken CRT");
                add(ModBlocks.CRT_BSOD.get(), "BSOD CRT");
                add(ModBlocks.CRT_CLEAN.get(), "Clean CRT");
                add(ModBlocks.TOASTER.get(), "Toaster");
                add(ModBlocks.BARREL_CORRODED.get(), "Corroded Barrel");
                add(ModBlocks.BARREL_LOX.get(), "LOX Barrel");
                add(ModBlocks.BARREL_PINK.get(), "Pink Barrel");
                add(ModBlocks.BARREL_YELLOW.get(), "Yellow Barrel");
                add(ModBlocks.BARREL_VITRIFIED.get(), "Vitrified Barrel");
                add(ModBlocks.BARREL_TAINT.get(), "Tainted Barrel");
                add(ModBlocks.BARREL_IRON.get(), "Iron Barrel");
                add(ModBlocks.BARREL_STEEL.get(), "Steel Barrel");
                add(ModBlocks.BARREL_TCALLOY.get(), "Iron Barrel");
                add(ModBlocks.FIRE_DOOR.get(), "Fire Door");
                add(ModBlocks.SLIDING_SEAL_DOOR.get(), "Sliding Seal Door");
                add(ModBlocks.SECURE_ACCESS_DOOR.get(), "Secure Access Door");
                add(ModBlocks.QE_CONTAINMENT.get(), "QE Containment Door");
                add(ModBlocks.QE_SLIDING.get(), "QE Sliding Door");
                add(ModBlocks.WATER_DOOR.get(), "Waterproof Hatch");
                add(ModBlocks.SILO_HATCH.get(), "Silo Hatch");
                add(ModBlocks.SILO_HATCH_LARGE.get(), "Large Silo Hatch");


                add(ModBlocks.DUD_SALTED.get(), "Unexploded Salted Bomb");
                add(ModBlocks.DUD_NUKE.get(), "Unexploded Nuclear Bomb");
                add(ModBlocks.DUD_FUGAS_TONG.get(), "Unexploded High-Explosive Bomb");
                add(ModBlocks.MINE_FAT.get(), "FatMan Mine");
                add(ModBlocks.MINE_AP.get(), "Anti-Personnel Mine");
                add(ModItems.GRENADE_NUC.get(), "Nuclear Grenade");
                add(ModItems.GRENADE_IF_HE.get(), "IF Grenade: HE");
                add(ModItems.GRENADE_IF_FIRE.get(), "IF Grenade: Incendiary");
                add(ModItems.GRENADE_IF_SLIME.get(), "IF Grenade: Bouncy");
                add(ModItems.MULTI_DETONATOR.get(), "Multi Detonator");
                add(ModItems.RANGE_DETONATOR.get(), "Range Detonator");
                add(ModItems.DETONATOR.get(), "Detonator");
                add(ModBlocks.BARBED_WIRE_POISON.get(), "Poison Barbed Wire");
                add(ModBlocks.BARBED_WIRE_FIRE.get(), "Fire Barbed Wire");
                add(ModBlocks.BARBED_WIRE_RAD.get(), "Radiation Barbed Wire");
                add(ModBlocks.BARBED_WIRE.get(), "Barbed Wire");
                add(ModBlocks.BARBED_WIRE_WITHER.get(), "Wither Barbed Wire");
                add(ModBlocks.WASTE_CHARGE.get(), "Waste Charge");
                add(ModBlocks.GIGA_DET.get(), "Giga Det");
                add(ModBlocks.NUCLEAR_CHARGE.get(), "Nuclear Charge");
                add(ModBlocks.C4.get(), "C4 Charge");
                add(ModItems.DEFUSER.get(), "Defuser");
                add(ModItems.CROWBAR.get(), "Crowbar");
                add(ModItems.DEPTH_ORES_SCANNER.get(), "Depth Ore Scanner");
                add(ModItems.OIL_DETECTOR.get(), "Oil Detector");


                add(ModBlocks.SMOKE_BOMB.get(), "Semtex");
                add(ModItems.NUGGET_SILICON.get(), "Silicon Nugget");
                add(ModItems.BILLET_SILICON.get(), "Silicon Billet");
                add(ModItems.PLATE_GOLD.get(), "Golden Plate");
                add(ModItems.PLATE_GUNMETAL.get(), "Gunmetal Plate");
                add(ModItems.PLATE_GUNSTEEL.get(), "Gunsteel Plate");
                add(ModItems.PLATE_TITANIUM.get(), "Titanium Plate");
                add(ModItems.PLATE_IRON.get(), "Iron Plate");
                add(ModItems.PLATE_KEVLAR.get(), "Kevlar Plate");
                add(ModItems.PLATE_LEAD.get(), "Lead Plate");
                add(ModItems.PLATE_MIXED.get(), "Mixed Plate");
                add(ModItems.PLATE_PAA.get(), "PAA Plate");
                add(ModItems.INSULATOR.get(), "Insulator");
                add(ModItems.PLATE_SATURNITE.get(), "Saturnite Plate");
                add(ModItems.PLATE_SCHRABIDIUM.get(), "Schrabidium Plate");
                add(ModItems.PLATE_STEEL.get(), "Steel Plate");
                add(ModItems.PLATE_ADVANCED_ALLOY.get(), "Advanced Alloy Plate");
                add(ModItems.PLATE_ALUMINUM.get(), "Aluminum Plate");
                add(ModItems.PLATE_COPPER.get(), "Copper Plate");
                add(ModItems.PLATE_BISMUTH.get(), "Bismuth Plate");
                add(ModItems.PLATE_ARMOR_AJR.get(), "AJR Armor Plate");
                add(ModItems.PLATE_ARMOR_DNT.get(), "DNT Armor Plate");
                add(ModItems.PLATE_ARMOR_DNT_RUSTED.get(), "Rusted DNT Armor Plate");
                add(ModItems.PLATE_ARMOR_FAU.get(), "FAU Armor Plate");
                add(ModItems.PLATE_ARMOR_HEV.get(), "HEV Armor Plate");
                add(ModItems.PLATE_ARMOR_LUNAR.get(), "Lunar Armor Plate");
                add(ModItems.PLATE_ARMOR_TITANIUM.get(), "Titanium Armor Plate");
                add(ModItems.PLATE_CAST.get(), "Casting Mold");
                add(ModItems.PLATE_CAST_ALT.get(), "Alternative Casting Mold");
                add(ModItems.PLATE_CAST_BISMUTH.get(), "Bismuth Casting Mold");
                add(ModItems.PLATE_CAST_DARK.get(), "Dark Casting Mold");
                add(ModItems.PLATE_COMBINE_STEEL.get(), "Combine Steel Plate");
                add(ModItems.PLATE_DURA_STEEL.get(), "Dura Steel Plate");
                add(ModItems.PLATE_DALEKANIUM.get(), "Dalekanium Plate");
                add(ModItems.PLATE_DESH.get(), "Desh Plate");
                add(ModItems.PLATE_DINEUTRONIUM.get(), "Dineutronium Plate");
                add(ModItems.PLATE_EUPHEMIUM.get(), "Euphemium Plate");
                add(ModItems.PLATE_FUEL_MOX.get(), "MOX Fuel Plate");
                add(ModItems.PLATE_FUEL_PU238BE.get(), "Pu-238/Be Fuel Plate");
                add(ModItems.PLATE_FUEL_PU239.get(), "Pu-239 Fuel Plate");
                add(ModItems.PLATE_FUEL_RA226BE.get(), "Ra-226/Be Fuel Plate");
                add(ModItems.PLATE_FUEL_SA326.get(), "SA-326 Fuel Plate");
                add(ModItems.PLATE_FUEL_U233.get(), "U-233 Fuel Plate");
                add(ModItems.PLATE_FUEL_U235.get(), "U-235 Fuel Plate");


                add("item.hbm_m.firebrick", "Firebrick");
                add("item.hbm_m.uranium_raw", "Raw Uranium");
                add("item.hbm_m.tungsten_raw", "Raw Tungsten");
                add("item.hbm_m.titanium_raw", "Raw Titanium");
                add("item.hbm_m.thorium_raw", "Raw Thorium");
                add("item.hbm_m.lead_raw", "Raw Lead");
                add("item.hbm_m.cobalt_raw", "Raw Cobalt");
                add("item.hbm_m.beryllium_raw", "Raw Beryllium");
                add("item.hbm_m.aluminum_raw", "Raw Aluminum");
                add("item.hbm_m.cinnabar", "Cinnabar");
                add("item.hbm_m.sulfur", "Sulfur");
                add("item.hbm_m.rareground_ore_chunk", "Rareground Ore Chunk");
                add("item.hbm_m.lignite", "Lignite");
                add("item.hbm_m.fluorite", "Fluorite");
                add("item.hbm_m.fireclay_ball", "Fireclay Ball");
                add("item.hbm_m.wood_ash_powder", "Wood Ash Powder");

                
                add("tooltip.hbm_m.mods", "Modifications:");
                add("tooltip.hbm_m.heart_piece.effect", "+5 Max Health");
                
                add("tooltip.hbm_m.applies_to", "Applies to:");

                // ARMOR MODIFICATION TABLE TOOLTIPS

                add("tooltip.hbm_m.helmet", "Helmet");
                add("tooltip.hbm_m.chestplate", "Chestplate");
                add("tooltip.hbm_m.leggings", "Leggings");
                add("tooltip.hbm_m.boots", "Boots");
                add("tooltip.hbm_m.armor.all", "Any Armor");

                add("tooltip.hbm_m.armor_table.main_slot", "Insert armor to be modified...");
                add("tooltip.hbm_m.slot", "Slot");
                add("tooltip.hbm_m.armor_table.helmet_slot", "Helmet");
                add("tooltip.hbm_m.armor_table.chestplate_slot", "Chestplate");
                add("tooltip.hbm_m.armor_table.leggings_slot", "Leggings");
                add("tooltip.hbm_m.armor_table.boots_slot", "Boots");
                add("tooltip.hbm_m.armor_table.battery_slot", "Battery");
                add("tooltip.hbm_m.armor_table.special_slot", "Special");
                add("tooltip.hbm_m.armor_table.plating_slot", "Plating");
                add("tooltip.hbm_m.armor_table.casing_slot", "Casing");
                add("tooltip.hbm_m.armor_table.servos_slot", "Servos");

                add("gui.hbm_m.blast_furnace.accepts", "Accepts items from: %s");
                add("direction.hbm_m.down", "Down");
                add("direction.hbm_m.up", "Up");
                add("direction.hbm_m.north", "North");
                add("direction.hbm_m.south", "South");
                add("direction.hbm_m.west", "West");
                add("direction.hbm_m.east", "East");
                add("gui.hbm_m.anvil.inputs", "Inputs:");
                add("gui.hbm_m.anvil.outputs", "Outputs:");
                add("gui.hbm_m.anvil.search", "Search");
                add("gui.hbm_m.anvil.search_hint", "Search...");
                add("gui.hbm_m.anvil.tier", "Required Tier: %s");
                add("tier.hbm_m.anvil.iron", "Iron");
                add("tier.hbm_m.anvil.steel", "Steel");
                add("tier.hbm_m.anvil.oil", "Oil");
                add("tier.hbm_m.anvil.nuclear", "Nuclear");
                add("tier.hbm_m.anvil.rbmk", "RBMK");
                add("tier.hbm_m.anvil.fusion", "Fusion");
                add("tier.hbm_m.anvil.particle", "Particle");
                add("tier.hbm_m.anvil.gerald", "Gerald");
                add("tier.hbm_m.anvil.murky", "Murky");

                // BLOCKS
                add("block.hbm_m.anvil_block", "Industrial Anvil");
                add("block.hbm_m.anvil_iron", "Iron Anvil");
                add("block.hbm_m.anvil_lead", "Lead Anvil");
                add("block.hbm_m.anvil_steel", "Steel Anvil");
                add("block.hbm_m.anvil_desh", "Desh Anvil");
                add("block.hbm_m.anvil_ferrouranium", "Ferrouranium Anvil");
                add("block.hbm_m.anvil_saturnite", "Saturnite Anvil");
                add("block.hbm_m.anvil_bismuth_bronze", "Bismuth Bronze Anvil");
                add("block.hbm_m.anvil_arsenic_bronze", "Arsenic Bronze Anvil");
                add("block.hbm_m.anvil_schrabidate", "Schrabidate Anvil");
                add("block.hbm_m.anvil_dnt", "DNT Anvil");
                add("block.hbm_m.anvil_osmiridium", "Osmiridium Anvil");
                add("block.hbm_m.anvil_murky", "Murky Anvil");
                add("block.hbm_m.door_office", "Office Door");
                add("block.hbm_m.door_bunker", "Bunker Door");
                add("block.hbm_m.metal_door", "Metal Door");
                add("block.hbm_m.demon_lamp", "Demon Lamp (WIP)");
                add("block.hbm_m.explosive_charge", "Explosive Charge");
                add("block.hbm_m.reinforced_glass", "Reinforced Glass");
                add("block.hbm_m.crate", "Crate");
                add("block.hbm_m.crate_lead", "Lead Crate");
                add("block.hbm_m.crate_metal", "Metal Crate");
                add("block.hbm_m.crate_weapon", "Weapon Crate");
                add("block.hbm_m.uranium_block", "Uranium Block");
                add("block.hbm_m.plutonium_block", "Plutonium Block");
                add("block.hbm_m.plutonium_fuel_block", "Plutonium Fuel Block");
                add("block.hbm_m.polonium210_block", "Polonium-210 Block");
                add("block.hbm_m.armor_table", "Armor Modification Table");
                add("block.hbm_m.machine_assembler", "Assembly Machine (Legacy)");
                add("block.hbm_m.advanced_assembly_machine", "Assembly Machine");
                add("block.hbm_m.machine_battery", "Machine Battery");
                add("block.hbm_m.shredder", "Shredder");
                add("block.hbm_m.wood_burner", "Wood Burner Generator");
                add("block.hbm_m.blast_furnace", "Blast Furnace");
                add("block.hbm_m.blast_furnace_extension", "Blast Furnace Extension");
                add("block.hbm_m.press", "Press");
                add("block.hbm_m.crate_iron", "Iron Crate");
                add("block.hbm_m.crate_steel", "Steel Crate");
                add("block.hbm_m.crate_desh", "Desh Crate");

                add("block.hbm_m.det_miner", "Det Miner");
                add("block.hbm_m.concrete_vent", "Concrete Vent");
                add("block.hbm_m.concrete_fan", "Concrete Fan");
                add("block.hbm_m.concrete_marked", "Marked Concrete");
                add("block.hbm_m.concrete_cracked", "Cracked Concrete");
                add("block.hbm_m.concrete_mossy", "Mossy Concrete");
                add("block.hbm_m.concrete", "Concrete");
                add("block.hbm_m.concrete_cracked_stairs", "Concrete Cracked Stairs");
                add("block.hbm_m.concrete_cracked_slab", "Concrete Cracked Slab");
                add("block.hbm_m.concrete_mossy_stairs", "Concrete Mossy Stairs");
                add("block.hbm_m.concrete_mossy_slab", "Concrete Mossy Slab");
                add("block.hbm_m.brick_concrete", "Concrete Bricks");
                add("block.hbm_m.brick_concrete_slab", "Concrete Bricks Slab");
                add("block.hbm_m.brick_concrete_stairs", "Concrete Bricks Stairs");
                add("block.hbm_m.brick_concrete_broken", "Broken Concrete Bricks");
                add("block.hbm_m.brick_concrete_broken_slab", "Broken Concrete Bricks Slab");
                add("block.hbm_m.brick_concrete_broken_stairs", "Broken Concrete Bricks Stairs");
                add("block.hbm_m.brick_concrete_cracked", "Cracked Concrete Bricks");
                add("block.hbm_m.brick_concrete_cracked_slab", "Cracked Concrete Bricks Slab");
                add("block.hbm_m.brick_concrete_cracked_stairs", "Cracked Concrete Bricks Stairs");
                add("block.hbm_m.brick_concrete_mossy", "Mossy Concrete Bricks");
                add("block.hbm_m.brick_concrete_mossy_slab", "Mossy Concrete Bricks Slab");
                add("block.hbm_m.brick_concrete_mossy_stairs", "Mossy Concrete Bricks Stairs");
                add("block.hbm_m.brick_concrete_marked", "Marked Concrete Bricks");

                add("block.hbm_m.concrete_hazard", "Concrete Block with Hazard line");
                add("block.hbm_m.concrete_hazard_slab", "Concrete Slab with Hazard line");
                add("block.hbm_m.concrete_hazard_stairs", "Concrete Stairs with Hazard line");
                add("block.hbm_m.concrete_stairs", "Concrete Stairs");
                add("block.hbm_m.concrete_slab", "Concrete Slab");
                add("block.hbm_m.large_vehicle_door", "Large Vehicle Door");
                add("block.hbm_m.round_airlock_door", "Round Airlock Door");
                add("block.hbm_m.strawberry_bush", "Strawberry Bush");

                add("block.hbm_m.geiger_counter_block", "Geiger Counter Block");
                add("block.hbm_m.wire_coated", "Red Copper Wire");

                // ORES
                add(ModBlocks.SEQUESTRUM_ORE.get(), "Salpeter Ore");
                add(ModItems.SEQUESTRUM.get(), "Salpeter");
                add(ModItems.AIRSTRIKE_TEST.get(), "Airstrike");
                add(ModBlocks.RESOURCE_ASBESTOS.get(), "Asbestos Cluster");
                add(ModBlocks.RESOURCE_BAUXITE.get(), "Bauxite");
                add(ModBlocks.RESOURCE_HEMATITE.get(), "Hematite");
                add(ModBlocks.RESOURCE_LIMESTONE.get(), "Limestone");
                add(ModBlocks.RESOURCE_MALACHITE.get(), "Malachite");
                add(ModBlocks.RESOURCE_SULFUR.get(), "Sulfur Cluster");
                add("block.hbm_m.cinnabar_ore_deepslate", "Deepslate Cinnabar Ore");
                add("block.hbm_m.cobalt_ore_deepslate", "Deepslate Cobalt Ore");
                add("block.hbm_m.uranium_ore", "Uranium Ore");
                add("block.hbm_m.aluminum_ore", "Aluminum Ore");
                add("block.hbm_m.aluminum_ore_deepslate", "Deepslate Aluminum Ore");
                add("block.hbm_m.titanium_ore", "Titanium Ore");
                add("block.hbm_m.titanium_ore_deepslate", "Deepslate Titanium Ore");
                add("block.hbm_m.tungsten_ore", "Tungsten Ore");
                add("block.hbm_m.asbestos_ore", "Asbestos Ore");
                add("block.hbm_m.sulfur_ore", "Sulfur Ore");
                add("block.hbm_m.cobalt_ore", "Cobalt Ore");
                add("block.hbm_m.uranium_ore_h", "High-Yield Uranium Ore");
                add("block.hbm_m.uranium_ore_deepslate", "Deepslate Uranium Ore");
                add("block.hbm_m.thorium_ore", "Thorium Ore");
                add("block.hbm_m.thorium_ore_deepslate", "Deepslate Thorium Ore");
                add("block.hbm_m.rareground_ore", "Rare Earth Ore");
                add("block.hbm_m.rareground_ore_deepslate", "Deepslate Rare Earth Ore");
                add("block.hbm_m.lignite_ore", "Lignite Ore");
                add("block.hbm_m.beryllium_ore", "Beryllium Ore");
                add("block.hbm_m.beryllium_ore_deepslate", "Deepslate Beryllium Ore");
                add("block.hbm_m.fluorite_ore", "Fluorite Ore");
                add("block.hbm_m.lead_ore", "Lead Ore");
                add("block.hbm_m.lead_ore_deepslate", "Deepslate Lead Ore");
                add("block.hbm_m.cinnabar_ore", "Cinnabar Ore");
                add("block.hbm_m.waste_grass", "Waste Grass");
                add("block.hbm_m.waste_leaves", "Waste Leaves");
                add("block.hbm_m.freaky_alien_block", "Freaky Allien Block");
                add("block.hbm_m.reinforced_stone", "Reinforced Stone");
                add("block.hbm_m.reinforced_stone_slab", "Reinforced Stone Slab");
                add("block.hbm_m.reinforced_stone_stairs", "Reinforced Stone Stairs");
                add("block.hbm_m.switch", "Switch");
                add("tooltip.hbm_m.rad_protection.value", "Radiation Resistance: %s");
                add("tooltip.hbm_m.rad_protection.value_short", "%s rad-resistance");

                // MACHINE GUI

                add("container.inventory", "Inventory");
                add("container.hbm_m.armor_table", "Armor Modification Table");
                add("container.hbm_m.machine_assembler", "Assembly Machine");
                add("container.hbm_m.wood_burner", "Wood Burner Generator");
                add("container.hbm_m.advanced_assembly_machine", "Assembly Machine");
                add("container.hbm_m.machine_battery", "Machine Battery");
                add("container.hbm_m.press", "Press");
                add("container.hbm_m.anvil_block", "Industrial Anvil");
                add("container.hbm_m.anvil", "%s Anvil");
                add("container.hbm_m.crate_iron", "Iron Crate");
                add("container.hbm_m.crate_steel", "Steel Crate");
                add("container.hbm_m.crate_desh", "Desh Crate");
                add("gui.hbm_m.battery.priority.0", "Priority: Low");
                add("gui.hbm_m.battery.priority.0.desc", "Lowest priority. Will be drained first and filled last.");
                add("gui.hbm_m.battery.priority.1", "Priority: Normal");
                add("gui.hbm_m.battery.priority.1.desc", "Standard priority for energy transfer.");
                add("gui.hbm_m.battery.priority.2", "Priority: High");
                add("gui.hbm_m.battery.priority.2.desc", "Highest priority. Will be filled first and drained last.");
                add("gui.hbm_m.battery.priority.recommended", "(Recommended)");

                add("gui.hbm_m.battery.condition.no_signal", "When there is NO redstone signal:");
                add("gui.hbm_m.battery.condition.with_signal", "When there IS a redstone signal:");

                add("gui.hbm_m.battery.mode.both", "Mode: Input & Output");
                add("gui.hbm_m.battery.mode.both.desc", "All energy operations are allowed.");
                add("gui.hbm_m.battery.mode.input", "Mode: Input Only");
                add("gui.hbm_m.battery.mode.input.desc", "Only receiving energy is allowed.");
                add("gui.hbm_m.battery.mode.output", "Mode: Output Only");
                add("gui.hbm_m.battery.mode.output.desc", "Only sending energy is allowed.");
                add("gui.hbm_m.battery.mode.locked", "Mode: Locked");
                add("gui.hbm_m.battery.mode.locked.desc", "All energy operations are disabled.");

                add("gui.recipe.setRecipe", "Set Recipe");

                add("tooltip.hbm_m.machine_battery.capacity", "Capacity: %1$s HE");
                add("tooltip.hbm_m.machine_battery.charge_speed", "Charge Speed: %1$s HE/t");
                add("tooltip.hbm_m.machine_battery.discharge_speed", "Discharge Speed: %1$s HE/t");
                add("tooltip.hbm_m.machine_battery.stored", "Stored: %1$s / %2$s HE");

                // HAZARD TOOLTIPS

                add("hazard.hbm_m.radiation", "[Radioactive]");
                add("hazard.hbm_m.radiation.format", "%s RAD/s");
                add("hazard.hbm_m.hydro_reactive", "[Hydro-reactive]");
                add("hazard.hbm_m.explosive_on_fire", "[Flammable / Explosive]");
                add("hazard.hbm_m.pyrophoric", "[Pyrophoric / Hot]");
                add("hazard.hbm_m.explosion_strength.format", " Explosion Strength - %s");
                add("hazard.hbm_m.stack", "Stack: %s");

                add("tooltip.hbm_m.abilities", "Abilities:");
                add("tooltip.hbm_m.vein_miner", "Vein Miner (%s)");
                add("tooltip.hbm_m.aoe", "AOE (%s x %s x %s)");
                add("tooltip.hbm_m.silk_touch", "Silk Touch");
                add("tooltip.hbm_m.fortune", "Fortune (%s)");
                add("tooltip.hbm_m.right_click", "Right click - toggle ability");
                add("tooltip.hbm_m.shift_right_click", "Shift + Right click - disable all");

                add("message.hbm_m.vein_miner.enabled", "Vein Miner %s enabled!");
                add("message.hbm_m.vein_miner.disabled", "Vein Miner %s disabled!");
                add("message.hbm_m.aoe.enabled", "AOE %s x %s x %s enabled!");
                add("message.hbm_m.aoe.disabled", "AOE %s x %s x %s disabled!");
                add("message.hbm_m.silk_touch.enabled", "Silk Touch enabled!");
                add("message.hbm_m.silk_touch.disabled", "Silk Touch disabled!");
                add("message.hbm_m.fortune.enabled", "Fortune %s enabled!");
                add("message.hbm_m.fortune.disabled", "Fortune %s disabled!");
                add("message.hbm_m.disabled", "All abilities disabled!");

                add("item.hbm_m.meter.geiger_counter.name", "GEIGER COUNTER");
                add("item.hbm_m.meter.dosimeter.name", "DOSIMETER");
                add("item.hbm_m.meter.title_format", "%s");
                add("hbm_m.render.shader_detected", "§e[HBM] §7External shader detected. Switching to compatible renderer...");
                add("hbm_m.render.shader_disabled", "§a[HBM] §7Shader disabled. Returning to optimized VBO renderer.");
                add("hbm_m.render.path_changed", "§e[HBM] §7Render path set to: %s");
                add("hbm_m.render.status", "§e[HBM] §7Current render path: §f%s\n§7External shader detected: §f%s");

                add("item.hbm_m.meter.chunk_rads", "§eCurrent chunk radiation: %s\n");
                add("item.hbm_m.meter.env_rads", "§eTotal environment contamination: %s");
                add("item.hbm_m.meter.player_rads", "§ePlayer contamination: %s\n");
                add("item.hbm_m.meter.protection", "§ePlayer protection: %s (%s)");

                add("item.hbm_m.meter.rads_over_limit", ">%s RAD/s");
                add("gui.hbm_m.battery.energy.info", "%s / %s HE");
                add("gui.hbm_m.battery.energy.delta", "%s HE/t");
                add("tooltip.hbm_m.hold_shift_for_details", "<Hold SHIFT to display more info>");

                add("sounds.hbm_m.geiger_counter", "Geiger Counter clicking");
                add("sounds.hbm_m.tool.techboop", "Geiger counter beep");
                
                add("commands.hbm_m.rad.cleared", "Radiation cleared for %s players.");
                add("commands.hbm_m.rad.cleared.self", "Your radiation has been cleared.");
                add("commands.hbm_m.rad.added", "Added %s radiation to %s players.");
                add("commands.hbm_m.rad.added.self", "You have been given %s radiation.");
                add("commands.hbm_m.rad.removed", "Removed %s radiation from %s players.");
                add("commands.hbm_m.rad.removed.self", "%s radiation has been removed from you.");

                add("death.attack.radiation", "Player %s died from radiation sickness");
                add("advancements.hbm_m.radiation_200.title", "Hooray, Radiation!");
                add("advancements.hbm_m.radiation_200.description", "Reach a radiation level of 200 RAD");
                add("advancements.hbm_m.radiation_1000.title", "Ouch, Radiation!");
                add("advancements.hbm_m.radiation_1000.description", "Die from radiation sickness");

                add("chat.hbm_m.structure.obstructed", "Placement obstructed by other blocks!");
                
                add("text.autoconfig.hbm_m.title", "Radiation Settings (HBM Modernized)");

                // CONFIG

                add("text.autoconfig.hbm_m.category.general", "General Settings");
                add("text.autoconfig.hbm_m.option.enableRadiation", "Enable radiation");
                add("text.autoconfig.hbm_m.option.enableChunkRads", "Enable chunk radiation");
                add("text.autoconfig.hbm_m.option.usePrismSystem", "Use PRISM system (otherwise Simple), WIP");

                add("text.autoconfig.hbm_m.category.world_effects", "World Effects");

                add("text.autoconfig.hbm_m.option.worldRadEffects", "World Radiation Effects");
                add("text.autoconfig.hbm_m.option.worldRadEffects.@Tooltip", "Enables/disables world destruction effects from high radiation (block replacement, vegetation decay, etc.).");

                add("text.autoconfig.hbm_m.option.worldRadEffectsThreshold", "World Destruction Threshold");
                add("text.autoconfig.hbm_m.option.worldRadEffectsThreshold.@Tooltip", "The minimum ambient radiation level in a chunk at which world destruction effects begin.");

                add("text.autoconfig.hbm_m.option.worldRadEffectsBlockChecks", "Block Checks per Tick");
                add("text.autoconfig.hbm_m.option.worldRadEffectsBlockChecks.@Tooltip", "The number of random block checks in an affected chunk per tick. Affects the speed of destruction. Higher values may impact performance.");

                add("text.autoconfig.hbm_m.option.worldRadEffectsMaxScaling", "Max Destruction Scaler");
                add("text.autoconfig.hbm_m.option.worldRadEffectsMaxScaling.@Tooltip", "The maximum speed multiplier for world destruction at peak radiation. 1 = no scaling, 4 = can be up to 4 times faster. Max value - 10x");

                add("text.autoconfig.hbm_m.option.worldRadEffectsMaxDepth", "Destruction Depth");
                add("text.autoconfig.hbm_m.option.worldRadEffectsMaxDepth.@Tooltip", "The maximum depth (in blocks) from the surface that world destruction effects can reach.");

                add("text.autoconfig.hbm_m.option.enableRadFogEffect", "Enable Radiation Fog Effect");
                add("text.autoconfig.hbm_m.option.radFogThreshold", "Fog Threshold");
                add("text.autoconfig.hbm_m.option.radFogChance", "Fog Chance");

                add("text.autoconfig.hbm_m.category.player", "Player");

                add("text.autoconfig.hbm_m.option.maxPlayerRad", "Max player radiation level");
                add("text.autoconfig.hbm_m.option.radDecay", "Player radiation decay rate");
                add("text.autoconfig.hbm_m.option.radDamage", "Radiation damage");
                add("text.autoconfig.hbm_m.option.radDamageThreshold", "Radiation damage threshold");
                add("text.autoconfig.hbm_m.option.radSickness", "Nausea threshold");
                add("text.autoconfig.hbm_m.option.radWater", "Water negative effect threshold, WIP");
                add("text.autoconfig.hbm_m.option.radConfusion", "Confusion threshold, WIP");
                add("text.autoconfig.hbm_m.option.radBlindness", "Blindness threshold");

                add("text.autoconfig.hbm_m.category.overlay", "Screen Overlays");

                add("text.autoconfig.hbm_m.option.enableRadiationPixelEffect", "Enable Radiation Screen Pixel Effect");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectThreshold", "Pixel Effect Threshold");
                add("text.autoconfig.hbm_m.option.radiationPixelMaxIntensityRad", "Pixel Effect Max Intensity");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectMaxDots", "Max Pixel Count");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectGreenChance", "Green Pixel Chance");
                add("text.autoconfig.hbm_m.option.radiationPixelMinLifetime", "Min Pixel Lifetime");
                add("text.autoconfig.hbm_m.option.radiationPixelMaxLifetime", "Max Pixel Lifetime");
                add("text.autoconfig.hbm_m.option.enableObstructionHighlight", "Enable Obstruction Highlight");
                add("text.autoconfig.hbm_m.option.enableObstructionHighlight.@Tooltip", "If enabled, blocks obstructing multiblock placement\nwill be highlighted with a red box.");
                add("text.autoconfig.hbm_m.option.obstructionHighlightAlpha", "Obstruction Highlight Opacity");
                add("text.autoconfig.hbm_m.option.obstructionHighlightAlpha.@Tooltip", "Sets the opacity of the highlight box's fill.\n0% = Invisible, 100% = Solid.");

                add("text.autoconfig.hbm_m.option.obstructionHighlightDuration", "Highlight Duration (sec)");
                add("text.autoconfig.hbm_m.option.obstructionHighlightDuration.@Tooltip", "The duration in seconds for how long the obstruction highlight will be visible.");

                add("text.autoconfig.hbm_m.category.chunk", "Chunk");

                add("text.autoconfig.hbm_m.option.maxRad", "Max chunk radiation");
                add("text.autoconfig.hbm_m.option.fogRad", "Fog radiation threshold");
                add("text.autoconfig.hbm_m.option.fogCh", "Fog chance (1 in fogCh), WIP");
                add("text.autoconfig.hbm_m.option.radChunkDecay", "Chunk radiation decay rate");
                add("text.autoconfig.hbm_m.option.radChunkSpreadFactor", "Chunk radiation spread factor");
                add("text.autoconfig.hbm_m.option.radSpreadThreshold", "Radiation spread threshold");
                add("text.autoconfig.hbm_m.option.minRadDecayAmount", "Min decay per tick");
                add("text.autoconfig.hbm_m.option.radSourceInfluenceFactor", "Source influence factor");
                add("text.autoconfig.hbm_m.option.radRandomizationFactor", "Chunk radiation randomization factor");

                add("text.autoconfig.hbm_m.category.rendering", "Rendering");

                add("text.autoconfig.hbm_m.option.modelUpdateDistance", "Distance for .obj model dynamic parts rendering");
                add("text.autoconfig.hbm_m.option.enableOcclusionCulling", "Enable model occlusion culling");

                add("text.autoconfig.hbm_m.category.debug", "Debug");

                add("text.autoconfig.hbm_m.option.enableDebugRender", "Enable radiation debug render");
                add("text.autoconfig.hbm_m.option.debugRenderTextSize", "Debug render text size");
                add("text.autoconfig.hbm_m.option.debugRenderDistance", "Debug render distance (chunks)");
                add("text.autoconfig.hbm_m.option.debugRenderInSurvival", "Show debug render in survival mode");
                add("text.autoconfig.hbm_m.option.enableDebugLogging", "Enable debug logging");

                add("key.hbm_m.open_config", "Open configuration menu");
                add("key.categories.hbm_m", "HBM Modernized");

                add("text.autoconfig.hbm_m.option.enableRadiation.@Tooltip", "If disabled, all radiation is turned off (chunks, items)");
                add("text.autoconfig.hbm_m.option.enableChunkRads.@Tooltip", "If disabled, chunk radiation is always 0");
                add("text.autoconfig.hbm_m.option.usePrismSystem.@Tooltip", "Use PRISM system for chunk radiation (WIP)");

                add("text.autoconfig.hbm_m.option.enableRadFogEffect.@Tooltip", "Enables/disables the radioactive fog particle effect in chunks with high radiation levels.");
                add("text.autoconfig.hbm_m.option.radFogThreshold.@Tooltip", "The minimum ambient radiation level in a chunk for the fog effect to appear.");
                add("text.autoconfig.hbm_m.option.radFogChance.@Tooltip", "The chance for fog particles to spawn in a suitable chunk per second. Calculated as 1 in X. A lower value means more frequent fog.");

                add("text.autoconfig.hbm_m.option.maxPlayerRad.@Tooltip", "Maximum radiation the player can accumulate");
                add("text.autoconfig.hbm_m.option.radDecay.@Tooltip", "How fast player radiation decays per tick");
                add("text.autoconfig.hbm_m.option.radDamage.@Tooltip", "Damage per tick when above threshold (Will be reworked)");
                add("text.autoconfig.hbm_m.option.radDamageThreshold.@Tooltip", "Player starts taking damage above this value");
                add("text.autoconfig.hbm_m.option.radSickness.@Tooltip", "Threshold for nausea effect");
                add("text.autoconfig.hbm_m.option.radWater.@Tooltip", "Threshold for water negative effect (WIP)");
                add("text.autoconfig.hbm_m.option.radConfusion.@Tooltip", "Threshold for confusion effect (WIP)");
                add("text.autoconfig.hbm_m.option.radBlindness.@Tooltip", "Threshold for blindness effect");

                add("text.autoconfig.hbm_m.option.enableRadiationPixelEffect.@Tooltip", "Shows random, flickering pixels on the screen when the player is exposed to incoming radiation.");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectThreshold.@Tooltip", "The minimum incoming radiation (RAD/s) required for the visual interference effect to appear.");
                add("text.autoconfig.hbm_m.option.radiationPixelMaxIntensityRad.@Tooltip", "The level of incoming radiation (RAD/s) at which the pixel effect reaches its maximum strength (maximum number of pixels).");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectMaxDots.@Tooltip", "The maximum number of pixels that can be on the screen at once when the effect is at its peak intensity. Affects performance on weak systems.");
                add("text.autoconfig.hbm_m.option.radiationPixelEffectGreenChance.@Tooltip", "The probability (from 0.0 to 1.0) that a newly appeared pixel will be green instead of white. E.g., 0.1 = 10% chance.");
                add("text.autoconfig.hbm_m.option.radiationPixelMinLifetime.@Tooltip", "The minimum time (in ticks) a single pixel will stay on the screen. 20 ticks = 1 second.");
                add("text.autoconfig.hbm_m.option.radiationPixelMaxLifetime.@Tooltip", "The maximum time (in ticks) a single pixel will stay on the screen. A random value between min and max lifetime is chosen for each pixel.");
                
                add("text.autoconfig.hbm_m.option.maxRad.@Tooltip", "Maximum chunk radiation");
                add("text.autoconfig.hbm_m.option.fogRad.@Tooltip", "Chunk radiation for fog to appear (WIP)");
                add("text.autoconfig.hbm_m.option.fogCh.@Tooltip", "Chance for fog to appear (WIP)");
                add("text.autoconfig.hbm_m.option.radChunkDecay.@Tooltip", "How fast chunk radiation decays");
                add("text.autoconfig.hbm_m.option.radChunkSpreadFactor.@Tooltip", "How much radiation spreads to neighboring chunks");
                add("text.autoconfig.hbm_m.option.radSpreadThreshold.@Tooltip", "Below this, radiation doesn't spread");
                add("text.autoconfig.hbm_m.option.minRadDecayAmount.@Tooltip", "Minimum decay per tick in chunk");
                add("text.autoconfig.hbm_m.option.radSourceInfluenceFactor.@Tooltip", "Influence of radioactive blocks in chunk");
                add("text.autoconfig.hbm_m.option.radRandomizationFactor.@Tooltip", "Randomization factor for chunk radiation");

                add("text.autoconfig.hbm_m.option.modelUpdateDistance.@Tooltip", "Distance for .obj model dynamic parts rendering (in chunks)");
                add("text.autoconfig.hbm_m.option.enableOcclusionCulling.@Tooltip", "Enable model occlusion culling (disable if your models are not rendering correctly)");

                add("text.autoconfig.hbm_m.option.enableDebugRender.@Tooltip", "Whether radiation debug render is enabled (F3)");
                add("text.autoconfig.hbm_m.option.debugRenderTextSize.@Tooltip", "Debug render text size");
                add("text.autoconfig.hbm_m.option.debugRenderDistance.@Tooltip", "Debug render distance (in chunks)");
                add("text.autoconfig.hbm_m.option.debugRenderInSurvival.@Tooltip", "Show debug renderer in survival mode");
                add("text.autoconfig.hbm_m.option.enableDebugLogging.@Tooltip", "If disabled, deep logging of game events will be active. Do not enable unless you experience problems");
                break;
        }
    }
}