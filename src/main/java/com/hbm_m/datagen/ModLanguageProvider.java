package com.hbm_m.datagen;

// Провайдер генерации локализаций (переводов) для мода.

import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    // 1. Создаем НАШЕ СОБСТВЕННОЕ поле для хранения языка
    private final String locale;

    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, RefStrings.MODID, locale);
        // 2. Сохраняем язык в наше поле при создании объекта
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        // АВТОМАТИЧЕСКАЯ ЛОКАЛИЗАЦИЯ СЛИТКОВ 
        for (ModIngots ingot : ModIngots.values()) {
            // 3. Теперь мы используем наше поле 'this.locale', к которому у нас есть доступ
            String translation = ingot.getTranslation(this.locale);
            if (translation != null) {
                add(ModItems.getIngot(ingot).get(), translation);
            }
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
                add("itemGroup.hbm_m.ntm_instruments_tab", "Инструменты NTM");
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

                // ПРЕДМЕТЫ

                add("item.hbm_m.heart_piece", "Частичка сердца");
                add(ModItems.HEART_CONTAINER.get(), "Контейнер для сердца");
                add(ModItems.HEART_BOOSTER.get(), "Усилитель сердца");
                add(ModItems.HEART_FAB.get(), "Фаб-сердце");
                add(ModItems.BLACK_DIAMOND.get(), "Черный алмаз");

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
                

                add(ModItems.GHIORSIUM_CLADDING.get(), "Прокладка из гиорсия");
                add(ModItems.DESH_CLADDING.get(), "Обшивка из деш");
                add(ModItems.RUBBER_CLADDING.get(), "Резиновая обшивка");
                add(ModItems.LEAD_CLADDING.get(), "Свинцовая обшивка");
                add(ModItems.PAINT_CLADDING.get(), "Свинцовая краска");


                add(ModItems.GRENADESMART.get(), "УМная отскок граната");
                add(ModItems.GRENADESLIME.get(), "Отскок-отскок граната");
                add(ModItems.GRENADE.get(), "Отскок граната");
                add(ModItems.GRENADEHE.get(), "Мощная отскок граната");
                add(ModItems.GRENADEFIRE.get(), "Зажигательная Отскок граната");

                add(ModItems.GRENADEIF.get(), "IF граната (WIP)");

                add("item.hbm_m.radaway", "Антирадин");
                add("item.hbm_m.wood_ash_powder", "Древесный пепел");
                add("effect.hbm_m.radaway", "Очищение от радиации");


                add(ModItems.QUANTUM_CHIP.get(), "Квантовый Чип");
                add(ModItems.QUANTUM_CIRCUIT.get(), "Квантовая Микросхема");
                add(ModItems.QUANTUM_COMPUTER.get(), "Квантовый Компьютер");
                add(ModItems.SILICON_CIRCUIT.get(), "Опечатанная Кремниевая Пластина");
                add(ModItems.BISMOID_CHIP.get(), "Передовой Микрочип");
                add(ModItems.BISMOID_CIRCUIT.get(), "Передовая Микросхема");
                add(ModItems.CONTROLLER_CHASSIS.get(), "Корпус Контроллера");
                add(ModItems.CONTROLLER.get(), "Контроллер");
                add(ModItems.CONTROLLER_ADVANCED.get(), "Продвинутый Контроллер");
                add(ModItems.CAPACITOR_BOARD.get(), "Конденсаторная Плата");
                add(ModItems.CAPACITOR_TANTALUM.get(), "Танталовый Конденсатор");
                add(ModItems.ANALOG_CIRCUIT.get(), "Аналоговая Плата");
                add(ModItems.INTEGRATED_CIRCUIT.get(), "Интегральная Схема");
                add(ModItems.ADVANCED_CIRCUIT.get(), "Военная Микросхема");
                add(ModItems.MICROCHIP.get(), "Микрочип");
                add(ModItems.ATOMIC_CLOCK.get(), "Атомные Часы");
                add(ModItems.VACUUM_TUBE.get(), "Вакуумная Трубка");
                add(ModItems.CAPACITOR.get(), "Конденсатор");
                add(ModItems.PCB.get(), "Печатная Плата");


                add(ModItems.BATTLE_GEARS.get(), "Боевые Детали");
                add(ModItems.BATTLE_CASING.get(), "Боевой Корпус");
                add(ModItems.BATTLE_COUNTER.get(), "Боевой Блок Управления");
                add(ModItems.BATTLE_SENSOR.get(), "Боевой Сенсор");
                add(ModItems.BATTLE_MODULE.get(), "Боевой Модуль");
                add(ModItems.METAL_ROD.get(), "Металлический стержень");
                add(ModItems.STRAWBERRY.get(), "Клубника");
                add(ModItems.PLATE_GOLD.get(), "Золотая пластина");
                add(ModItems.PLATE_GUNMETAL.get(), "Пластина пушечной бронзы");
                add(ModItems.PLATE_TITANIUM.get(), "Титановая пластина");
                add(ModItems.PLATE_GUNSTEEL.get(), "Пластина оружейной стали");
                add(ModItems.PLATE_IRON.get(), "Железная пластина");
                add(ModItems.PLATE_KEVLAR.get(), "Кевларовая пластина");
                add(ModItems.PLATE_LEAD.get(), "Оловянная пластина");
                add(ModItems.PLATE_MIXED.get(), "Композитная пластина");
                add(ModItems.PLATE_PAA.get(), "Пластина сплава РаА");
                add(ModItems.PLATE_POLYMER.get(), "Полимерная пластина");
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
                add(ModItems.PLATE_CAST.get(), "литая пластина");
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

                // БЛОКИ

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
                add("block.hbm_m.shredder", "Шредер (WIP)");
                add("block.hbm_m.blast_furnace", "Доменная печь");
                add("block.hbm_m.press", "Пресс");
                add("block.hbm_m.geiger_counter_block", "Стационарный счетчик Гейгера");
                add("block.hbm_m.freaky_alien_block", "Блок ебанутого инопланетянина");
                add("block.hbm_m.reinforced_stone", "Уплотнённый камень");
                add("block.hbm_m.reinforced_stone_slab", "Плита из уплотнённого камня");
                add("block.hbm_m.reinforced_stone_stairs", "Ступеньки из уплотнённого камня");
                add("block.hbm_m.concrete_hazard", "Бетон ''Выбор строителя'' - Полоса опасности");
                add("block.hbm_m.concrete_hazard_slab", "Бетонная плита ''Выбор строителя'' - Полоса опасности");
                add("block.hbm_m.concrete_hazard_stairs", "Бетонные ступеньки ''Выбор строителя'' - Полоса опасности");
                add("block.hbm_m.large_vehicle_door", "Дверь для крупногабаритного транспорта");
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

                add("tooltip.hbm_m.machine_battery.capacity", "Ёмкость: %1$s FE");
                add("tooltip.hbm_m.machine_battery.charge_speed", "Скорость зарядки: %1$s FE/т");
                add("tooltip.hbm_m.machine_battery.discharge_speed", "Скорость разрядки: %1$s FE/т");
                add("tooltip.hbm_m.machine_battery.stored", "Заряжено: %1$s / %2$s FE");
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

                add("text.autoconfig.hbm_m.option.hazmatMod.@Tooltip", "Защита обычного костюма химзащиты (1.0 = нет защиты)");
                add("text.autoconfig.hbm_m.option.advHazmatMod.@Tooltip", "Защита продвинутого костюма химзащиты");
                add("text.autoconfig.hbm_m.option.paaHazmatMod.@Tooltip", "Защита костюма PAA");

                add("text.autoconfig.hbm_m.option.enableDebugRender.@Tooltip", "Показывать отладочный оверлей чанков (F3)");
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

                add(ModItems.GRENADEIF.get(), "IF Grenade (WIP)");

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

                add(ModItems.PLATE_GOLD.get(), "Golden Plate");
                add(ModItems.PLATE_GUNMETAL.get(), "Gunmetal Plate");
                add(ModItems.PLATE_GUNSTEEL.get(), "Gunsteel Plate");
                add(ModItems.PLATE_TITANIUM.get(), "Titanium Plate");
                add(ModItems.PLATE_IRON.get(), "Iron Plate");
                add(ModItems.PLATE_KEVLAR.get(), "Kevlar Plate");
                add(ModItems.PLATE_LEAD.get(), "Lead Plate");
                add(ModItems.PLATE_MIXED.get(), "Mixed Plate");
                add(ModItems.PLATE_PAA.get(), "PAA Plate");
                add(ModItems.PLATE_POLYMER.get(), "Polymer Plate");
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

                // BLOCKS
                
                add("block.hbm_m.uranium_block", "Uranium Block");
                add("block.hbm_m.plutonium_block", "Plutonium Block");
                add("block.hbm_m.plutonium_fuel_block", "Plutonium Fuel Block");
                add("block.hbm_m.polonium210_block", "Polonium-210 Block");
                add("block.hbm_m.armor_table", "Armor Modification Table");
                add("block.hbm_m.machine_assembler", "Assembly Machine (Legacy)");
                add("block.hbm_m.advanced_assembly_machine", "Assembly Machine");
                add("block.hbm_m.machine_battery", "Machine Battery");
                add("block.hbm_m.shredder", "Shredder (WIP)");
                add("block.hbm_m.wood_burner", "Wood Burner Generator");
                add("block.hbm_m.blast_furnace", "Blast Furnace");
                add("block.hbm_m.press", "Press");

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
                add("block.hbm_m.large_vehicle_door", "Large Vehicle Door");
                add("block.hbm_m.strawberry_bush", "Strawberry Bush");

                add("block.hbm_m.geiger_counter_block", "Geiger Counter Block");
                add("block.hbm_m.wire_coated", "Red Copper Wire");

                // ORES

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
                
                add("tooltip.hbm_m.rad_protection.value", "Radiation Resistance: %s");
                add("tooltip.hbm_m.rad_protection.value_short", "%s rad-resistance");

                // MACHINE GUI

                add("container.inventory", "Inventory");
                add("container.hbm_m.armor_table", "Armor Modification Table");
                add("container.hbm_m.machine_assembler", "Assembly Machine");
                add("container.hbm_m.wood_burner", "Wood Burner Generator");
                add("container.hbm_m.advanced_assembly_machine", "Assembly Machine");

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

                add("tooltip.hbm_m.machine_battery.capacity", "Capacity: %1$s FE");
                add("tooltip.hbm_m.machine_battery.charge_speed", "Charge Speed: %1$s FE/t");
                add("tooltip.hbm_m.machine_battery.discharge_speed", "Discharge Speed: %1$s FE/t");
                add("tooltip.hbm_m.machine_battery.stored", "Stored: %1$s / %2$s FE");

                // HAZARD TOOLTIPS

                add("hazard.hbm_m.radiation", "[Radioactive]");
                add("hazard.hbm_m.radiation.format", "%s RAD/s");
                add("hazard.hbm_m.hydro_reactive", "[Hydro-reactive]");
                add("hazard.hbm_m.explosive_on_fire", "[Flammable / Explosive]");
                add("hazard.hbm_m.pyrophoric", "[Pyrophoric / Hot]");
                add("hazard.hbm_m.explosion_strength.format", " Explosion Strength - %s");
                add("hazard.hbm_m.stack", "Stack: %s");

                add("item.hbm_m.meter.geiger_counter.name", "GEIGER COUNTER");
                add("item.hbm_m.meter.dosimeter.name", "DOSIMETER");
                add("item.hbm_m.meter.title_format", "%s");

                add("item.hbm_m.meter.chunk_rads", "§eCurrent chunk radiation: %s\n");
                add("item.hbm_m.meter.env_rads", "§eTotal environment contamination: %s");
                add("item.hbm_m.meter.player_rads", "§ePlayer contamination: %s\n");
                add("item.hbm_m.meter.protection", "§ePlayer protection: %s (%s)");

                add("item.hbm_m.meter.rads_over_limit", ">%s RAD/s");
                add("gui.hbm_m.battery.energy.info", "%s / %s FE");
                add("gui.hbm_m.battery.energy.delta", "%s FE/t");
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

                add("text.autoconfig.hbm_m.option.hazmatMod.@Tooltip", "Protection for regular hazmat suit (1.0 = no protection)");
                add("text.autoconfig.hbm_m.option.advHazmatMod.@Tooltip", "Protection for advanced hazmat suit");
                add("text.autoconfig.hbm_m.option.paaHazmatMod.@Tooltip", "Protection for PAA suit");

                add("text.autoconfig.hbm_m.option.enableDebugRender.@Tooltip", "Whether radiation debug render is enabled (F3)");
                add("text.autoconfig.hbm_m.option.debugRenderTextSize.@Tooltip", "Debug render text size");
                add("text.autoconfig.hbm_m.option.debugRenderDistance.@Tooltip", "Debug render distance (in chunks)");
                add("text.autoconfig.hbm_m.option.debugRenderInSurvival.@Tooltip", "Show debug renderer in survival mode");
                add("text.autoconfig.hbm_m.option.enableDebugLogging.@Tooltip", "If disabled, deep logging of game events will be active. Do not enable unless you experience problems");
                break;
        }
    }
}