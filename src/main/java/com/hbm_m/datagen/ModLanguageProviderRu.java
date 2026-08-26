package com.hbm_m.datagen;
//? if forge {

import static com.hbm_m.block.ModBlocks.ENABLED_INGOT_BLOCKS;
import static com.hbm_m.block.ModBlocks.getIngotBlock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.lib.RefStrings;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProviderRu extends LanguageProvider {

    public ModLanguageProviderRu(PackOutput output) {
        super(output, RefStrings.MODID, "ru_ru");
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

        add(ModItems.DUST.get(), "Пыль");
        add(ModItems.DUST_TINY.get(), "Малая кучка пыли");
        add(ModItems.FALLOUT.get(), "Куча радиоактивных осадков");
    }

    private String buildPowderName(ModIngots ingot, boolean tiny) {
        String base = ingot.getTranslation("ru_ru");
        if (base == null || base.isBlank()) {
            base = formatName(ingot.getName());
        }

        String replaced = base.replace("Слиток", "Порошок").replace("слиток", "порошок");
        if (replaced.equals(base)) {
            replaced = "Порошок " + base;
        }
        String result = replaced.trim();
        if (tiny) {
            result = "Малая кучка " + result;
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
                RegistrySupplier<Block> block = getIngotBlock(ingot);
                if (block != null && !translatedBlocks.contains(block.getId())) {
                    add(block.get(), buildBlockName(ingot));
                }
            }
        }
    }

    private String buildBlockName(ModIngots ingot) {
        String base = ingot.getTranslation("ru_ru");
        if (base == null || base.isBlank()) {
            base = formatName(ingot.getName());
        }

        String replaced = base.replace("Слиток", "Блок").replace("слиток", "блок");
        if (replaced.equals(base)) {
            replaced = "Блок " + base;
        }
        return replaced.trim();
    }

    private void addConfigTranslations() {
        add("config.hbm_m.tab.client.tooltip", "Клиентские настройки — хранятся локально в client.json");
        add("config.hbm_m.tab.server.tooltip", "Серверные настройки. Доступны в одиночной игре или с правами оператора.");
        add("config.hbm_m.category.general.tooltip", "Общие тумблеры мода (радиация, MOTD).");
        add("config.hbm_m.category.world_effects.tooltip", "Эффекты мира (радиоактивный туман, следы порчи).");
        add("config.hbm_m.category.weapons.tooltip", "Поведение оружия и падение предметов.");
        add("config.hbm_m.category.player.tooltip", "Пороги радиации игрока.");
        add("config.hbm_m.category.chunk.tooltip", "Движок радиации чанков.");
        add("config.hbm_m.category.machines.tooltip", "Настройка механизмов и мультиблоков.");
        add("config.hbm_m.category.nukes.tooltip", "Радиусы взрывов ядерных устройств.");
        add("config.hbm_m.category.explosions.tooltip", "Движок взрывов и радиоактивные осадки.");
        add("config.hbm_m.category.missile_track.tooltip", "Сетевое отслеживание ракет.");
        add("config.hbm_m.category.debug.tooltip", "Отладочный рендер и логирование.");
        add("config.hbm_m.category.rendering.tooltip", "Клиентские опции рендеринга.");
        add("config.hbm_m.category.overlay.tooltip", "Экранные оверлеи (пиксельный эффект, подсветка).");
    }

    @Override
    protected void addTranslations() {
        addConfigTranslations();

        // Автоматическая локализация слитков
        for (ModIngots ingot : ModIngots.values()) {
            RegistrySupplier<Item> ingotItem = ModItems.getIngot(ingot);
            if (ingotItem != null && ingotItem.isPresent()) {
                String translation = ingot.getTranslation("ru_ru");
                if (translation != null) {
                    add(ingotItem.get(), translation);
                }
            }
        }

        Set<ResourceLocation> translatedPowders = new HashSet<>();

        // Автоматическая локализация порошков
        for (ModPowders powders : ModPowders.values()) {
            RegistrySupplier<Item> powderItem = ModItems.getPowders(powders);
            if (powderItem != null && powderItem.isPresent()) {
                String translation = powders.getTranslation("ru_ru");
                if (translation != null) {
                    add(powderItem.get(), translation);
                    translatedPowders.add(powderItem.getId());
                }
            }
        }

        // Автоматическая локализация порошков из слитков
        for (ModIngots ingot : ModIngots.values()) {
            RegistrySupplier<Item> powder = ModItems.getPowder(ingot);
            if (powder != null && powder.isPresent() && !translatedPowders.contains(powder.getId())) {
                add(powder.get(), buildPowderName(ingot, false));
            }
            ModItems.getTinyPowder(ingot).ifPresent(tiny -> {
                if (tiny != null && tiny.isPresent()) {
                    add(tiny.get(), buildPowderName(ingot, true));
                }
            });
        }

        // DEV / Боеприпасы / Порошки (из старого if ("ru_ru".equals(this.locale)))
        add(ModItems.POWDER_SAWDUST.get(), "Опилки");
        add(ModItems.POWDER_YELLOWCAKE.get(), "Уранинит (порошок)");
        add(ModItems.POWDER_BALEFIRE.get(), "Порошок белфайра");
        add(ModItems.POWDER_PALEOGENITE.get(), "Порошок палеогенита");
        add(ModItems.POWDER_THERMITE.get(), "Термит");
        add(ModItems.POWDER_FERTILIZER.get(), "Удобрение");
        add(ModItems.POWDER_FLUX.get(), "Флюс");
        add(ModItems.POWDER_MAGIC.get(), "Волшебный порошок");
        add(ModItems.POWDER_ICE.get(), "Порошковый лёд");
        add(ModItems.POWDER_SPARK_MIX.get(), "Искровая смесь");
        add(ModItems.POWDER_SEMTEX_MIX.get(), "Смесь семтекса");
        add(ModItems.POWDER_DESH_READY.get(), "Готовый деш (порошок)");
        add(ModItems.POWDER_COLTAN.get(), "Колтан (порошок)");
        add(ModItems.TURRET_AMMO.get(), "Патроны для турели");
        add(ModItems.AMMO_9MM_SP.get(), "9-мм патрон (мягкая пуля)");
        add(ModItems.AMMO_9MM_FMJ.get(), "9-мм патрон (оболочечная пуля)");
        add(ModItems.AMMO_9MM_JHP.get(), "9-мм патрон (экспансивная пуля)");
        add(ModItems.AMMO_9MM_AP.get(), "9-мм патрон (бронебойный)");
        add(ModItems.AMMO_50_SP.get(), ".50 патрон (мягкая пуля)");
        add(ModItems.AMMO_50_FMJ.get(), ".50 патрон (оболочечная пуля)");
        add(ModItems.AMMO_50_JHP.get(), ".50 патрон (экспансивная пуля)");
        add(ModItems.AMMO_50_AP.get(), ".50 патрон (бронебойный)");
        add(ModItems.AMMO_50_DU.get(), ".50 патрон (обеднённый уран)");
        add(ModItems.AMMO_556_SP.get(), "5.56-мм патрон (мягкая пуля)");
        add(ModItems.AMMO_556_FMJ.get(), "5.56-мм патрон (оболочечная пуля)");
        add(ModItems.AMMO_556_JHP.get(), "5.56-мм патрон (экспансивная пуля)");
        add(ModItems.AMMO_556_AP.get(), "5.56-мм патрон (бронебойный)");
        add(ModItems.ROCKET_TURRET_STANDARD.get(), "Ракета турели (наведение)");
        add(ModItems.ROCKET_HIMARS_STANDARD.get(), "Ракета HIMARS (стандартная)");
        add(ModItems.ROCKET_HIMARS_HE.get(), "Ракета HIMARS (фугасная)");
        add(ModItems.ROCKET_HIMARS_LAVA.get(), "Ракета HIMARS (лава)");
        add(ModItems.ROCKET_HIMARS_MINI_NUKE.get(), "Ракета HIMARS (мини-ядерная)");
        add(ModItems.ROCKET_HIMARS_WP.get(), "Ракета HIMARS (белый фосфор)");
        add(ModItems.ROCKET_HIMARS_THERMOBARIC.get(), "Ракета HIMARS (термобарическая)");
        add(ModItems.AMMO_TAU_URANIUM.get(), "Урановый заряд (тау-ускоритель)");
        add(ModItems.AMMO_FLAME_DIESEL.get(), "Дизельное топливо (огнемёт)");
        add(ModItems.MISSILE_FUSELAGE.get(), "Фюзеляж ракеты");
        add(ModItems.MISSILE_CHIP.get(), "Чип наведения ракеты");

        // =========================================================================
        // СЮДА ВСТАВЛЯЕТСЯ ВЕСЬ КОД ИЗ СТАРЫХ МЕТОДОВ ПО ПОРЯДКУ:
        // 1. Все строки изнутри addTranslationsRuRuPart1()
        // 2. Все строки изнутри addTranslationsRuRuPart2()
        // 3. Все строки изнутри addTranslationsRuRuPart3()
        // 4. Все строки изнутри addTranslationsRuRuPart4()
        // (Сами объявления методов private void addTranslationsRuRuPartX() не нужны)
        // =========================================================================

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
        add("itemGroup.hbm_m.ntm_dev_tab", "Dev Items");
        
        // СНАРЯГА
        add("item.hbm_m.alloy_sword", "Меч из продвинутого сплава");
        add("item.hbm_m.alloy_pickaxe", "Кирка из продвинутого сплава");
        add("item.hbm_m.alloy_axe", "Топор из продвинутого сплава");
        add("item.hbm_m.alloy_hoe", "Мотыга из продвинутого сплава");
        add("item.hbm_m.alloy_shovel", "Лопата из продвинутого сплава");

        add("item.hbm_m.meteorite_sword", "Метеоритовый меч");
        add("item.hbm_m.meteorite_sword_seared", "Закалённый метеоритовый меч");
        add("item.hbm_m.meteorite_sword_hardened", "Упрочнённый метеоритовый меч");
        add("item.hbm_m.meteorite_sword_alloyed", "Легированный метеоритовый меч");

        add("item.hbm_m.steel_sword", "Стальной меч");
        add("item.hbm_m.steel_pickaxe", "Стальная кирка");
        add("item.hbm_m.steel_axe", "Стальной топор");
        add("item.hbm_m.steel_hoe", "Стальная мотыга");
        add("item.hbm_m.steel_shovel", "Стальная лопата");

        add("item.hbm_m.titanium_sword", "Титановый меч");
        add("item.hbm_m.titanium_pickaxe", "Титановая кирка");
        add("item.hbm_m.drill_titanium", "Титановый бур");
        add("item.hbm_m.titanium_axe", "Титановый топор");
        add("item.hbm_m.titanium_hoe", "Титановая мотыга");
        add("item.hbm_m.titanium_shovel", "Титановая лопата");

        add("item.hbm_m.starmetal_sword", "Меч из звёздного металла");
        add("item.hbm_m.starmetal_pickaxe", "Кирка из звёздного металла");
        add("item.hbm_m.starmetal_axe", "Топор из звёздного металла");
        add("item.hbm_m.starmetal_hoe", "Мотыга из звёздного металла");
        add("item.hbm_m.starmetal_shovel", "Лопата из звёздного металла");

        // ПРОТОТИП РАКЕТЫ
        add("item.hbm_m.missile_test", "Тестовая баллистическая ракета");
        add("item.hbm_m.missile_abm", "Противобаллистическая ракета");
        add("item.hbm_m.missile_micro", "Микроядерная ракета");
        add("item.hbm_m.missile_schrabidium", "Ракета со шрабидием");
        add("item.hbm_m.missile_bhole", "Ракета с чёрной дырой");
        add(ModItems.BLACK_HOLE.get(), "Миниатюрная чёрная дыра");
        add(ModItems.PELLET_ANTIMATTER.get(), "Кластер антиматерии");
        add(ModItems.FLAME_PONY.get(), "Картинка цветной лошади");
        add("death.attack.black_hole", "%1$s превратился в спагетти.");
        add("item.hbm_m.missile_taint", "Ракета с заражением");
        add("item.hbm_m.missile_emp", "ЭМИ-ракета");
        add("item.hbm_m.missile_generic", "Ракета общего назначения");
        add("item.hbm_m.missile_incendiary", "Зажигательная ракета");
        add("item.hbm_m.missile_cluster", "Кассетная ракета");
        add("item.hbm_m.missile_buster", "Бункерная ракета");
        add("item.hbm_m.missile_decoy", "Ложная ракета");
        add("item.hbm_m.missile_stealth", "Стелс-ракета");
        add("item.hbm_m.missile_strong", "Мощная ракета");
        add("item.hbm_m.missile_incendiary_strong", "Мощная зажигательная ракета");
        add("item.hbm_m.missile_cluster_strong", "Мощная кассетная ракета");
        add("item.hbm_m.missile_buster_strong", "Мощная бункерная ракета");
        add("item.hbm_m.missile_emp_strong", "Мощная ЭМИ-ракета");
        add("item.hbm_m.missile_burst", "Ракета Burst");
        add("item.hbm_m.missile_inferno", "Ракета Inferno");
        add("item.hbm_m.missile_rain", "Ракета Rain");
        add("item.hbm_m.missile_drill", "Ракета Drill");
        add("item.hbm_m.missile_shuttle", "Шаттл-ракета");
        add("item.hbm_m.missile_nuclear", "Ядерная ракета Atlas");
        add("item.hbm_m.missile_nuclear_cluster", "Кассетная ядерная ракета");
        add("item.hbm_m.missile_volcano", "Ракета Volcano");
        add("item.hbm_m.missile_doomsday", "Ракета Doomsday");
        add("item.hbm_m.missile_doomsday_rusted", "Ржавая ракета Doomsday");
        add("item.hbm_m.missile.tier.tier0", "Ракета: уровень 0");
        add("item.hbm_m.missile.tier.tier1", "Ракета: уровень 1");
        add("item.hbm_m.missile.tier.tier2", "Ракета: уровень 2");
        add("item.hbm_m.missile.tier.tier3", "Ракета: уровень 3");
        add("item.hbm_m.missile.tier.tier4", "Ракета: уровень 4");
        add("item.hbm_m.missile.desc.notLaunchable", "Нельзя запустить с пусковой площадки");
        add("item.hbm_m.missile.desc.fuel", "Топливо");
        add("item.hbm_m.missile.desc.fuelCapacity", "Запас топлива");
        add("item.hbm_m.missile.desc.fluidNotRequiredWip", "Топливо из баков временно не требуется для пуска (WIP)");
        add("item.hbm_m.missile.fuel.solid.prefueled", "Твёрдое топливо (заправлена)");
        add("item.hbm_m.missile.fuel.ethanol_peroxide", "Этанол + пероксид");
        add("item.hbm_m.missile.fuel.kerosene_peroxide", "Керосин + пероксид");
        add("item.hbm_m.missile.fuel.kerosene_loxy", "Керосин + жидкий кислород");
        add("item.hbm_m.missile.fuel.jetfuel_loxy", "Реактивное топливо + жидкий кислород");
        
        add("gui.hbm_m.energy", "Энергия: %s/%s HE");
        add("gui.hbm_m.burn_time", "Время горения: %s%%");
        add("container.hbm_m.gas_centrifuge", "Газовая центрифуга");
        add("desc.gui.gasCent.enrichment", "Обогащение");
        add("desc.gui.gasCent.output", "Выход");
        add("pseudofluid.hbm_m.none", "Нет");
        add("pseudofluid.hbm_m.nuf6", "UF6 (природный)");
        add("pseudofluid.hbm_m.leuf6", "UF6 (низкообогащённый)");
        add("pseudofluid.hbm_m.meuf6", "UF6 (среднеобогащённый)");
        add("pseudofluid.hbm_m.heuf6", "UF6 (высокообогащённый)");
        add("pseudofluid.hbm_m.pf6", "PuF6 (обогащённый)");
        add("pseudofluid.hbm_m.mud", "Отходы WATZ");
        add("pseudofluid.hbm_m.mud_heavy", "Отходы WATZ (концентрат)");
        add("jei.hbm_m.gas_centrifuge.info", "Требуется %s центрифуг(и)");
        add("jei.hbm_m.gas_centrifuge.info_high_speed", "Требуется %s центрифуг(и) (ускоренная)");
        add("tooltip.hbm_m.satchip.freq", "Частота: %s");
        add("message.hbm_m.satchip.freq_set", "Частота установлена: %s");
        add("gui.launchPad.notReady", "Не готова");
        add("gui.launchPad.loading", "Загрузка...");
        add("gui.launchPad.ready", "Готова");
        add("container.launchPad", "Пусковая площадка");
        add("container.launchPadLarge", "Большая пусковая площадка");
        add("container.launchPadRusted", "Ржавая пусковая площадка");
        add("container.hbm_m.shredder", "Измельчитель");
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

        add(ModItems.T51_HELMET.get(), "Шлем силовой брони T-51b");
        add(ModItems.T51_CHESTPLATE.get(), "Нагрудник силовой брони T-51b");
        add(ModItems.T51_LEGGINGS.get(), "Поножи силовой брони T-51b");
        add(ModItems.T51_BOOTS.get(), "Ботинки силовой брони T-51b");

        add(ModItems.AJR_HELMET.get(), "Шлем Стальных Рейнджеров");
        add(ModItems.AJR_CHESTPLATE.get(), "Нагрудник Стальных Рейнджеров");
        add(ModItems.AJR_LEGGINGS.get(), "Поножи Стальных Рейнджеров");
        add(ModItems.AJR_BOOTS.get(), "Ботинки Стальных Рейнджеров");

        add(ModItems.BISMUTH_HELMET.get(), "Висмутовый шлем силовой брони");
        add(ModItems.BISMUTH_CHESTPLATE.get(), "Висмутовый нагрудник силовой брони");
        add(ModItems.BISMUTH_LEGGINGS.get(), "Висмутовые поножи силовой брони");
        add(ModItems.BISMUTH_BOOTS.get(), "Висмутовые ботинки силовой брони");

        add(ModItems.AJRO_HELMET.get(), "Шлем силовой брони AJR");
        add(ModItems.AJRO_CHESTPLATE.get(), "Нагрудник силовой брони AJR");
        add(ModItems.AJRO_LEGGINGS.get(), "Поножи силовой брони AJR");
        add(ModItems.AJRO_BOOTS.get(), "Ботинки силовой брони AJR");

        add(ModItems.DNT_HELMET.get(), "Шлем DNT-Нанокостюма");
        add(ModItems.DNT_CHESTPLATE.get(), "Нагрудник DNT-Нанокостюма");
        add(ModItems.DNT_LEGGINGS.get(), "Поножи DNT-Нанокостюма");
        add(ModItems.DNT_BOOTS.get(), "Ботинки DNT-Нанокостюма");

        add("item.hbm_m.geiger_counter", "Счетчик Гейгера");
        add("item.hbm_m.dosimeter", "Дозиметр");
        add(ModItems.DIGAMMA_DIAGNOSTIC.get(), "Диагностика Диггама");
        add(ModItems.MUSIC_DISC_GLASS.get(), "Стеклянная пластинка");
        add("item.hbm_m.music_disc_glass.desc", "Увертюра 1812 года");
        add("item.hbm_m.battery_creative", "Бесконечная батарейка");
        add("tooltip.hbm_m.creative_battery_desc","Предоставляет бесконечное количество энергии");
        add("tooltip.hbm_m.creative_battery_flavor","Бесконечность - не предел!!");
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
        add(ModItems.HEART_PIECE.get(), "Частичка сердца");
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
        add("desc.gui.upgrade", "Улучшения");
        add("desc.gui.upgrade.speed", "Скорость: -25% времени за уровень");
        add("desc.gui.upgrade.power", "Мощность: -25% потребления за уровень");
        add("desc.gui.upgrade.effectiveness", "Эффективность: шанс бесплатного крафта");
        add("desc.gui.upgrade.afterburner", "Форсаж: дополнительные эффекты");
        add("desc.gui.upgrade.overdrive", "Овердрайв: x2 циклов за тик за уровень");

        // Upgrade items
        add("item.hbm_m.upgrade_speed_1", "Апгрейд скорости Mk.I");
        add("item.hbm_m.upgrade_speed_2", "Апгрейд скорости Mk.II");
        add("item.hbm_m.upgrade_speed_3", "Апгрейд скорости Mk.III");
        add("item.hbm_m.upgrade_effect_1", "Апгрейд эффективности Mk.I");
        add("item.hbm_m.upgrade_effect_2", "Апгрейд эффективности Mk.II");
        add("item.hbm_m.upgrade_effect_3", "Апгрейд эффективности Mk.III");
        add("item.hbm_m.upgrade_power_1", "Апгрейд мощности Mk.I");
        add("item.hbm_m.upgrade_power_2", "Апгрейд мощности Mk.II");
        add("item.hbm_m.upgrade_power_3", "Апгрейд мощности Mk.III");
        add("item.hbm_m.upgrade_fortune_1", "Апгрейд удачи Mk.I");
        add("item.hbm_m.upgrade_fortune_2", "Апгрейд удачи Mk.II");
        add("item.hbm_m.upgrade_fortune_3", "Апгрейд удачи Mk.III");
        add("item.hbm_m.upgrade_afterburn_1", "Апгрейд форсажа Mk.I");
        add("item.hbm_m.upgrade_afterburn_2", "Апгрейд форсажа Mk.II");
        add("item.hbm_m.upgrade_afterburn_3", "Апгрейд форсажа Mk.III");
        add("item.hbm_m.upgrade_overdrive_1", "Апгрейд овердрайва Mk.I");
        add("item.hbm_m.upgrade_overdrive_2", "Апгрейд овердрайва Mk.II");
        add("item.hbm_m.upgrade_overdrive_3", "Апгрейд овердрайва Mk.III");

        // Upgrade tooltips
        add("tooltip.hbm_m.upgrade.type.speed", "Скорость: ускоряет работу, увеличивает потребление");
        add("tooltip.hbm_m.upgrade.type.effect", "Эффективность: увеличивает радиус/эффект");
        add("tooltip.hbm_m.upgrade.type.power", "Мощность: снижает потребление энергии");
        add("tooltip.hbm_m.upgrade.type.fortune", "Удача: увеличивает выход продукции");
        add("tooltip.hbm_m.upgrade.type.afterburn", "Форсаж: добавляет дополнительные эффекты");
        add("tooltip.hbm_m.upgrade.type.overdrive", "Овердрайв: экстремальное ускорение");
        add("tooltip.hbm_m.upgrade.tier", "Уровень: %d");

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
        add("tooltip.hbm_m.naval_mine.line1", "Взрывается при контакте с игроком");

        // Сигарета (порт ItemCigarette, 1.7.10)
        add("tooltip.hbm_m.cigarette.line1", "✓ Асбестовый фильтр");
        add("tooltip.hbm_m.cigarette.line2", "✓ Высокое содержание смол");
        add("tooltip.hbm_m.cigarette.line3", "✓ Табак содержит 100% полония-210");
        add("tooltip.hbm_m.cigarette.line4", "✓ Вкусно");

// ДЕТОНАТОР
        add("tooltip.hbm_m.detonator.target", "Цель: ");
        add("tooltip.hbm_m.detonator.no_target", "Нет цели");
        add("tooltip.hbm_m.detonator.right_click", "ПКМ - активировать");
        add("tooltip.hbm_m.detonator.shift_right_click", "Shift+ПКМ - установить");

// СКАНЕР КЛАСТЕРОВ
        add("tooltip.hbm_m.depth_ores_scanner.scans_chunks", "Сканирует чанки в поисках");
        add("tooltip.hbm_m.depth_ores_scanner.deep_clusters", "глубинных кластеров под игроком");
        add("tooltip.hbm_m.depth_ores_scanner.depth_warning", "Работает на глубине -30 и ниже!");
        add("tooltip.hbm_m.explosion_defense.unbreakable", "§6Взрывоустойчивость: §cНЕДОЕМИЕ§r");
        add("tooltip.hbm_m.explosion_defense.value", "§6Взрывоустойчивость: §e%s§r");

        add("tooltip.hbm_m.airstrike.common", "Вызывает авиаудар в целевую точку");
        add("tooltip.hbm_m.airstrike.normal", "Дождь из случайных гранат");
        add("tooltip.hbm_m.airstrike.heavy", "3 мощных фугасных бомбы");
        add("tooltip.hbm_m.airstrike.agent", "Химическое оружие 'Agent Orange'");
        add("tooltip.hbm_m.airstrike.nuke", "1 тактическая ядерная бомба");

        add("message.hbm_m.airstrike.not_loaded", "Целевой чанк не загружен!");
        add("message.hbm_m.airstrike.called", "Авиаудар вызван на координатах: %d, %d, %d");
        add("message.hbm_m.airstrike.no_target", "Нет целевого блока в видимости!");
        // DEPTH ORES SCANNER (сообщения)
        add("message.hbm_m.depth_ores_scanner.invalid_height", "Сканер работает только на высоте -30 или ниже!");
        add("message.hbm_m.depth_ores_scanner.directly_below", "Глубинный кластер прямо под нами!");
        add("message.hbm_m.depth_ores_scanner.in_chunk", "В нашем чанке обнаружен глубинный кластер!");
        add("message.hbm_m.depth_ores_scanner.adjacent_chunk", "В соседнем чанке обнаружен глубинный кластер!");
        add("message.hbm_m.depth_ores_scanner.none_found", "Не обнаружено глубинных кластеров поблизости");

// MULTI DETONATOR TOOLTIPS
        add("tooltip.hbm_m.multi_detonator.active_point", "➤ %s:");
        add("tooltip.hbm_m.multi_detonator.point_set", " %s:");
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
        add("message.hbm_m.detonator.saved", "Позиция сохранена: %d, %d, %d");
        add("message.hbm_m.detonator.pos_not_compatible", "Позиция несовместима или не прогружена");
        add("message.hbm_m.detonator.activated", "Успешно активировано");

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
        add(ModBlocks.STEEL_POLE.get(), "Antenna Pole"); //NEEDS TRANSLATION
        add(ModBlocks.ANTENNA_TOP.get(), "Antenna Top"); //NEEDS TRANSLATION
        add(ModBlocks.PUTER.get(), "(PC) Personal Computer"); //NEEDS TRANSLATION
        add(ModBlocks.BARREL_CORRODED.get(), "Проржавевшая бочка");
        add(ModBlocks.BARREL_LOX.get(), "Бочка с жидким кислородом");
        add(ModBlocks.BARREL_ANTIMATTER.get(), "Магнитный контейнер антиматерии");
        add(ModBlocks.BARREL_PLASTIC.get(), "Безопасная бочка™");
        add(ModBlocks.BARREL_PINK.get(), "Бочка с керосином");
        add(ModBlocks.BARREL_YELLOW.get(), "Бочка с ядерными отходами");
        add(ModBlocks.BARREL_VITRIFIED.get(), "Бочка с остеклованными ядерными отходами");
        add(ModBlocks.BARREL_TAINT.get(), "Бочка с говном");
        add(ModBlocks.TAINT.get(), "Порча");
        add("effect.hbm_m.taint", "Порча");
        add("entity.hbm_m.entity_mob_tainted_creeper", "Заражённый порчей крипер");
        add("item.hbm_m.entity_mob_tainted_creeper_spawn_egg", "Яйцо призыва заражённого крипера");
        add("entity.hbm_m.entity_mob_volatile_creeper", "Возгораемый крипер");
        add("item.hbm_m.entity_mob_volatile_creeper_spawn_egg", "Яйцо призыва возгораемого крипера");
        add("entity.hbm_m.entity_mob_phosgene_creeper", "Фосгеновый крипер");
        add("item.hbm_m.entity_mob_phosgene_creeper_spawn_egg", "Яйцо призыва фосгенового крипера");
        add("entity.hbm_m.entity_mob_gold_creeper", "Золотой крипер");
        add("item.hbm_m.entity_mob_gold_creeper_spawn_egg", "Яйцо призыва золотого крипера");
        add("entity.hbm_m.entity_mob_nuclear_creeper", "Ядерный крипер");
        add("item.hbm_m.entity_mob_nuclear_creeper_spawn_egg", "Яйцо призыва ядерного крипера");
        add("death.attack.taint", "%1$s умер от невероятного количества опухолей.");
// MULTIBLOCK DOORS
        add(ModBlocks.LARGE_VEHICLE_DOOR.get(), "Дверь для крупногабаритного транспорта");
        add(ModBlocks.ROUND_AIRLOCK_DOOR.get(), "Круглая воздушная дверь");
        add(ModBlocks.TRANSITION_SEAL.get(), "Транзитный люк");
        add(ModBlocks.SLIDE_DOOR.get(), "Раздвижная дверь");
        add(ModBlocks.FIRE_DOOR.get(), "Пожарная дверь");
        add(ModBlocks.SLIDING_SEAL_DOOR.get(), "Скользящая герметичная дверь");
        add(ModBlocks.SECURE_ACCESS_DOOR.get(), "Усиленная дверь");
        add(ModBlocks.QE_CONTAINMENT.get(), "QE дверь биологического сдерживания");
        add(ModBlocks.QE_SLIDING.get(), "QE раздвижная дверь");
        add(ModBlocks.VAULT_DOOR.get(), "Дверь убежища Vault-Tec");
        add(ModBlocks.WATER_DOOR.get(), "Подводный люк");
        add(ModBlocks.SILO_HATCH.get(), "Малый люк");
        add(ModBlocks.SILO_HATCH_LARGE.get(), "Люк ракетной шахты");
        add(ModBlocks.CARGO_DOOR.get(), "Грузовая дверь");

        add(ModBlocks.DUD_SALTED.get(), "Неразорвавшаяся солёная бомба");
        add(ModBlocks.DUD_NUKE.get(), "Неразорвавшаяся ядерная бомба");
        add(ModBlocks.DUD_CONVENTIONAL.get(), "Неразорвавшаяся фугасная бомба");
        add(ModBlocks.MINE_FAT.get(), "Мина 'Толстяк'");
        add(ModBlocks.NUKE_FAT_MAN.get(), "Ядерная бомба 'Толстяк'");
        add(ModItems.NUKE_PROTOTYPE.get(), "Прототип атомной бомбы");
        add("container.hbm_m.nuke_prototype", "Прототип");
        add(ModItems.IGNITER.get(), "Детонатор");
        add(ModItems.CELL_SAS3.get(), "Топливная ячейка СА-С3");
        add(ModItems.ROD_QUAD_LEAD.get(), "Четырёхкратный свинцовый стержень");
        add(ModItems.ROD_QUAD_NP237.get(), "Четырёхкратный стержень Нп-237");
        add(ModItems.ROD_QUAD_URANIUM.get(), "Четырёхкратный урановый стержень");
        add(ModItems.FAT_MAN_EXPLOSIVE.get(), "Набор взрывных линз первого поколения");
        add(ModItems.FAT_MAN_IGNITER.get(), "Зажигатель");
        add(ModItems.FAT_MAN_CORE.get(), "Плутониевое ядро");
        add("container.hbm_m.nuke_fat_man", "Ядерная бомба 'Толстяк'");
        add("gui.hbm_m.nuke_fat_man.requires", "Требует:");
        add("gui.hbm_m.nuke_fat_man.line_lenses", " * 4 набора взрывных линз первого поколения");
        add("gui.hbm_m.nuke_fat_man.line_core", " * Плутониевое ядро");
        add("gui.hbm_m.nuke_fat_man.line_igniter", " * Зажигатель");
        // Тултип предмета «линзы» (как early_explosive_lenses.desc, 1.7.10)
        add("tooltip.hbm_m.fat_man_explosive.desc1", "Сборка из 8 осколочно-фугасных линз с алюминиевым");
        add("tooltip.hbm_m.fat_man_explosive.desc2", "толкателем, дюралюминиевой оболочкой и проволочными детонаторами.");
        // Большие ядерные бомбы
        add(ModBlocks.NUKE_GADGET.get(), "«Устройство»");
        add(ModBlocks.NUKE_BOY.get(), "Малыш");
        add(ModBlocks.NUKE_MIKE.get(), "Айви Майк");
        add(ModBlocks.NUKE_TSAR.get(), "Царь-бомба");
        add("container.hbm_m.nuke_gadget", "«Устройство»");
        add("container.hbm_m.nuke_boy", "Ядерная бомба 'Малыш'");
        add("container.hbm_m.nuke_mike", "Ядерная бомба 'Айви Майк'");
        add("container.hbm_m.nuke_tsar", "Царь-бомба");
        add(ModItems.EARLY_EXPLOSIVE_LENSES.get(), "Взрывные линзы первого поколения");
        add(ModItems.EXPLOSIVE_LENSES.get(), "Взрывные линзы");
        add(ModBlocks.NUKE_FLEIJA.get(), "Ф.Л.Е.Й.Д.Ж.А.");
        add("container.hbm_m.nuke_fleija", "Ф.Л.Е.Й.Д.Ж.А.");
        add(ModBlocks.NUKE_N2.get(), "Мина N²");
        add("container.hbm_m.nuke_n2", "Мина N²");
        add(ModBlocks.NUKE_SOLINIUM.get(), "«Синяя стирка»");
        add("container.hbm_m.nuke_solinium", "«Синяя стирка»");
        add(ModBlocks.NUKE_FSTBMB.get(), "Бомба бейлфайра");
        add("container.hbm_m.nuke_fstbmb", "Бомба бейлфайра");
        add(ModBlocks.NUKE_CUSTOM.get(), "Кастомная бомба");
        add("container.hbm_m.nuke_custom", "Кастомная бомба");
        add(ModBlocks.BOMB_MULTI.get(), "Многоцелевая бомба");
        add("container.hbm_m.bomb_multi", "Многоцелевая бомба");
        add("gui.hbm_m.bomb_multi.ready", "ГОТОВА — не подносить к редстоуну");
        add("gui.hbm_m.nuke_fstbmb.start", "Взвести");
        add("gui.hbm_m.nuke_fstbmb.timer", "Подрыв через %s");
        add("gui.hbm_m.nuke_custom.euph", "Заряд: ЭЙФЕМИУМ — полное стирание");
        add("gui.hbm_m.nuke_custom.schrab", "Заряд: ШРАБИДИУМ, радиус %s");
        add("gui.hbm_m.nuke_custom.hydro", "Заряд: ВОДОРОД, радиус %s");
        add("gui.hbm_m.nuke_custom.nuke", "Заряд: ЯДЕРНЫЙ, радиус %s");
        add("gui.hbm_m.nuke_custom.tnt_big", "Заряд: КОНВЕНЦИОНАЛЬНЫЙ, радиус %s");
        add("gui.hbm_m.nuke_custom.tnt_small", "Заряд: слабое конвенциональное ВВ");
        add("gui.hbm_m.nuke_custom.empty", "Заряд: пусто");
        add(ModBlocks.MINE_AP.get(), "Противопехотная мина");
        add(ModItems.GRENADE_NUC.get(), "Ядерная граната");
        add(ModItems.GRENADE_IF_HE.get(), "IF-Граната: фугасная");
        add(ModItems.GRENADE_IF_FIRE.get(), "IF-Граната: зажигательная");
        add(ModItems.GRENADE_IF_SLIME.get(), "IF-Граната: прыгучая");
        add(ModItems.MULTI_DETONATOR.get(), "Мульти-детонатор");
        add(ModItems.RANGEFINDER.get(), "Дальномер");
        add(ModItems.CONFETTI_TESTER.get(), "Тестер *конфетти* эффектов");
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
        add(ModItems.CROWBAR.get(), "Mk.V Устройство Для Вскрытия Ящиков ''Лом''");
        add(ModItems.DEPTH_ORES_SCANNER.get(), "Сканер глубинных кластеров");
        add(ModItems.OIL_DETECTOR.get(), "Детектор нефти");

        add(ModItems.GHIORSIUM_CLADDING.get(), "Прокладка из гиорсия");
        add(ModItems.DESH_CLADDING.get(), "Обшивка из деш");
        add(ModItems.RUBBER_CLADDING.get(), "Резиновая обшивка");
        add(ModItems.LEAD_CLADDING.get(), "Свинцовая обшивка");
        add(ModItems.PAINT_CLADDING.get(), "Свинцовая краска");
        add(ModItems.CRT_DISPLAY.get(), "Электро-лучевая трубка");
        add(ModItems.MAGNETRON.get(), "Магнетрон");
        add(ModItems.TURBINE_TITANIUM.get(), "Титановая турбина");
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
        add(ModItems.AIRSTRIKE_TEST.get(), "Устройство для обозначения авиаудара");
        add(ModItems.AIRSTRIKE_HEAVY.get(), "Устройство для обозначения авиаудара");
        add(ModItems.AIRSTRIKE_AGENT.get(), "Устройство для обозначения авиаудара");
        add(ModItems.AIRSTRIKE_NUKE.get(), "Устройство для обозначения авиаудара");
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
        add(ModBlocks.SELLAFIELD_BEDROCK.get(), "Селлафитовая коренная порода");
        add(ModBlocks.ORE_SELLAFIELD_DIAMOND.get(), "Sellafite алмазная руда");
        add(ModBlocks.ORE_SELLAFIELD_EMERALD.get(), "Sellafite изумрудная руда");
        add(ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get(), "Sellafite обожжённая урановая руда");
        add(ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get(), "Sellafite шрабидиевая руда");
        add(ModBlocks.ORE_SELLAFIELD_RADGEM.get(), "Sellafite радиоактивная руда");
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
        add(ModItems.COAL_POWDER_TINY.get(), "Маленькая кучка угольной пыли");
        add(ModItems.SCRAP.get(), "Мусор");
        add(ModItems.BILLET_PLUTONIUM.get(), "Заготовка плутония");
        add("item.hbm_m.fluid_barrel", "Жидкостная бочка: %s");
        add("item.hbm_m.fluid_barrel.empty", "Пустая бочка для жидкости");
        add("item.hbm_m.fluid_barrel_infinite", "Бесконечная жидкостная бочка");
        add(ModItems.PIPE_IRON.get(), "Железная труба");
        add(ModItems.PIPE_COPPER.get(), "Медная труба");
        add(ModItems.PIPE_GOLD.get(), "Золотая труба");
        add(ModItems.PIPE_LEAD.get(), "Свинцовая труба");
        add(ModItems.PIPE_STEEL.get(), "Стальная труба");
        add(ModItems.PIPE_TUNGSTEN.get(), "Вольфрамовая труба");
        add(ModItems.PIPE_TITANIUM.get(), "Титановая труба");
        add(ModItems.PIPE_ALUMINUM.get(), "Алюминиевая труба");
        add(ModItems.PIPE_DURA_STEEL.get(), "Труба из прочной стали");
        add("item.hbm_m.fluid_identifier", "Мульти-жидкостный идентификатор: %s");
        add("item.hbm_m.fluid_identifier.none", "Мульти-жидкостный идентификатор");
        add("item.hbm_m.fluid_identifier.info", "Жидкостный идентификатор для:");
        add("item.hbm_m.fluid_identifier.info2", "Второй тип:");
        add("toast.hbm_m.fluid_identifier_active", "Текущий тип: %s");
        add("item.hbm_m.bucket_crude_oil", "Ведро сырой нефти (WIP)");
        add("item.hbm_m.inf_water", "Бочка с бесконечной водой");
        add("item.hbm_m.inf_water_mk2", "Бочка с бесконечной водой mk2");

        add("gui.hbm_m.fluid_tank.empty", "Пусто");
        add("gui.hbm_m.fluid_tank.empty_locked", "Пусто (тип цистерны: %s)");
        add("gui.hbm_m.fluid_tank.empty_filter", "Пусто (фильтр: %s)");
        add("gui.hbm_m.fluid_tank.filter_set", "Тип установлен: %s!");
        add("gui.hbm_m.fluid_tank.mode.0", "Режим: Только вывод");
        add("gui.hbm_m.fluid_tank.mode.1", "Режим: Буфер");
        add("gui.hbm_m.fluid_tank.mode.2", "Режим: Только ввод");
        add("gui.hbm_m.fluid_tank.mode.3", "Режим: Отключено");
        add("gui.hbm_m.fluid_tank.pressure", "Давление: %s PU");
        add("gui.hbm_m.fluid_tank.pressurized", "Под давлением — используйте компрессор!");
        add("gui.hbm_m.fluid_tank.hold_shift_more", "Удерживайте <LSHIFT> для подробностей");
        add("fluid.hbm_m.trait.polluting", "[Загрязняющая]");
        add("fluid.hbm_m.trait.polluting.when_spilled", "При разливе:");
        add("fluid.hbm_m.trait.polluting.when_burned", "При сжигании:");
        add("fluid.hbm_m.trait.polluting.line", "%s — %s");
        add("fluid.hbm_m.trait.heatable.thermal_capacity", "Теплоёмкость: %s TU на %s mB");
        add("fluid.hbm_m.trait.efficiency_pct", "КПД: %s%%");
        add("fluid.hbm_m.trait.pwr_flux_multiplier", "[Модератор потока PWR]");
        add("fluid.hbm_m.trait.core_flux_pct", "Поток в активной зоне: %s%%");
        add("fluid.hbm_m.trait.pheromone_glyphid", "[Феромоны — глифид]");
        add("fluid.hbm_m.trait.pheromone_modified", "[Феромоны — модифицированные]");
        add("fluid.hbm_m.trait.toxin_header", "[Токсин]");
        add("fluid.hbm_m.trait.toxic_fumes", "Токсичные испарения");
        add("fluid.hbm_m.toxin.chlorine.line1", "Повреждение лёгких (облако)");
        add("fluid.hbm_m.toxin.chlorine.line2", "Сильный раздражитель дыхательных путей");
        add("fluid.hbm_m.toxin.phosgene.line1", "Тяжёлое поражение лёгких (облако)");
        add("fluid.hbm_m.toxin.phosgene.line2", "Очень опасен при вдыхании");
        add("fluid.hbm_m.toxin.mustard.line1", "Кожные и лёгочные поражения");
        add("fluid.hbm_m.toxin.mustard.line2", "Возможны химические ожоги и отравление");
        add("fluid.hbm_m.toxin.estradiol.line1", "Опасные мелкодисперсные частицы");
        add("fluid.hbm_m.toxin.estradiol.line2", "Системное воздействие при контакте");
        add("fluid.hbm_m.toxin.redmud.line1", "Щёлочные и тяжёлые металлы");
        add("fluid.hbm_m.toxin.redmud.line2", "Кожные ожоги и системная интоксикация");


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
        add(ModItems.CANNED_BHOLE.get(), "Консервированная чёрная дыра");
        add("item.hbm_m.canned_bhole.desc", "Сделано из настоящих сингулярностей. Нет, правда.");
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
        add(ModItems.NUGGET_TANTALIUM.get(), "Самородок тантала");
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
    
        // КРИСТАЛЛЫ
        add("item.hbm_m.crystal_aluminium", "Алюминиевый кристалл");
        add("item.hbm_m.crystal_beryllium", "Бериллиевый кристалл");
        add("item.hbm_m.crystal_charred", "Обожжённый кристалл");
        add("item.hbm_m.crystal_cinnebar", "Киноварный кристалл");
        add("item.hbm_m.crystal_coal", "Углеродный кристалл");
        add("item.hbm_m.crystal_cobalt", "Кобальтовый кристалл");
        add("item.hbm_m.crystal_copper", "Медный кристалл");
        add("item.hbm_m.crystal_diamond", "Алмазный кристалл");
        add("item.hbm_m.crystal_fluorite", "Кристалл флюорита");
        add("item.hbm_m.crystal_gold", "Кристалл золота");
        add("item.hbm_m.crystal_hardened", "Упрочнённый кристалл");
        add("item.hbm_m.crystal_horn", "Кристаллический рог");
        add("item.hbm_m.crystal_iron", "Кристалл железа");
        add("item.hbm_m.crystal_lapis", "Кристалл лазурита");
        add("item.hbm_m.crystal_lead", "Кристалл свинца");
        add("item.hbm_m.crystal_lithium", "Кристалл лития");
        add("item.hbm_m.crystal_niter", "Кристалл селитры");
        add("item.hbm_m.crystal_osmiridium", "Кристалл осмиридия");
        add("item.hbm_m.crystal_phosphorus", "Кристалл фосфора");
        add("item.hbm_m.crystal_plutonium", "Кристалл плутония");
        add("item.hbm_m.crystal_pulsar", "Импульсный кристалл");
        add("item.hbm_m.crystal_rare", "Кристалл редкоземельного металла");
        add("item.hbm_m.crystal_redstone", "Редстоуновый кристалл");
        add("item.hbm_m.crystal_schrabidium", "Шрабидиевый кристалл");
        add("item.hbm_m.crystal_schraranium", "Кристалл шрарания");
        add("item.hbm_m.crystal_starmetal", "Кристалл звёздного металла");
        add("item.hbm_m.crystal_sulfur", "Кристалл серы");
        add("item.hbm_m.crystal_thorium", "Кристалл тория");
        add("item.hbm_m.crystal_titanium", "Кристалл титана");
        add("item.hbm_m.crystal_trixite", "Кристалл триксита");
        add("item.hbm_m.crystal_tungsten", "Кристалл вольфрама");
        add("item.hbm_m.crystal_uranium", "Кристалл урана");
        add("item.hbm_m.crystal_virus", "Кристалл вируса");
        add("item.hbm_m.crystal_xen", "Ксеноновый кристалл");

        add("item.hbm_m.blueprint_folder", "Папка шаблонов");
        add("item.hbm_m.blueprint_folder.named", "Папка шаблонов машин");
        add("item.hbm_m.blueprint_folder.empty", "Пустая папка");
        add("item.hbm_m.blueprint_folder.obsolete", "Устаревший шаблон (группа удалена)");
        add("item.hbm_m.blueprint_folder.desc", "Вставьте в Сборочную машину для разблокировки рецептов");
        add("item.hbm_m.blueprint_folder.recipes", "Содержит рецепты:");
        add("gui.hbm_m.recipe_from_group", "Из группы:");
        
        add("sounds.hbm_m.radaway_use", "Использование антирадина");
        
        
        add("armorMod.applicableTo", "Применяется к:");
        add("armorMod.all", "Всему");
        add("armorMod.helmets", "Шлему");
        add("armorMod.chestplates", "Нагруднику");
        add("armorMod.leggings", "Поножам");
        add("armorMod.boots", "Ботинкам");
        add("armorMod.insertHere", "Вставьте броню, чтобы её модифицировать...");
        add("armorMod.type.helmet", "Шлем");
        add("armorMod.type.chestplate", "Нагрудник");
        add("armorMod.type.leggings", "Поножи");
        add("armorMod.type.boots", "Ботинки");
        add("armorMod.type.servo", "Сервоприводы");
        add("armorMod.type.cladding", "Обшивка");
        add("armorMod.type.insert", "Пластина");
        add("armorMod.type.special", "Особое");
        add("armorMod.type.battery", "Аккумулятор");

        add("gui.hbm_m.blast_furnace.accepts", "Принимает предметы со стороны: %s");
        add("gui.hbm_m.blast_furnace.speed", "Скорость: %s%%");
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
        add("block.hbm_m.steam_condenser", "Паровой конденсатор");
        add("block.hbm_m.anvil_desh", "Наковальня из деша");
        add("block.hbm_m.block_steel", "Стальной блок");
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
        add("block.hbm_m.machine_siren", "Сирена");
        add("container.hbm_m.machine_siren", "Сирена");
        add("block.hbm_m.broadcaster", "Передатчик");
        add("block.hbm_m.crate", "Ящик");
        add("block.hbm_m.crate_lead", "Свинцовый ящик");
        add("block.hbm_m.crate_metal", "Металлический ящик");
        add("block.hbm_m.crate_weapon", "Ящик с оружием");
        add("block.hbm_m.plutonium_fuel_block", "Блок плутониевого топлива");
        add("block.hbm_m.polonium210_block", "Блок полония-210");
        add("block.hbm_m.armor_table", "Стол модификации брони");
        add("block.hbm_m.machine_assembler", "Сборочная машина (Старая)");
        add("block.hbm_m.advanced_assembly_machine", "Сборочная машина");
        add("block.hbm_m.block_uranium", "Урановый блок");
        add(ModBlocks.FLUID_TANK.get(), "Цистерна");
        add(ModBlocks.BAT9000.get(), "Цистернище");
        add(ModBlocks.MACHINE_BATTERY_SOCKET.get(), "Аккумуляторный разъём");
        add(ModBlocks.FLUID_DUCT.get(), "Жидкостная труба (NEO)");
        add(ModBlocks.FLUID_DUCT_COLORED.get(), "Жидкостная труба (цветная)");
        add(ModBlocks.FLUID_DUCT_SILVER.get(), "Жидкостная труба (серебристая)");
        add("item.hbm_m.fluid_duct", "Жидкостная труба: %s");
        add("item.hbm_m.fluid_duct.empty", "Пустая жидкостная труба");
        add("item.hbm_m.fluid_duct_colored", "Цветная жидкостная труба: %s");
        add("item.hbm_m.fluid_duct_colored.empty", "Пустая цветная жидкостная труба");
        add("item.hbm_m.fluid_duct_silver", "Серебристая жидкостная труба: %s");
        add("item.hbm_m.fluid_duct_silver.empty", "Пустая серебристая жидкостная труба");
        add(ModBlocks.FLUID_VALVE.get(), "Жидкостный клапан");
        add(ModBlocks.FLUID_PUMP.get(), "Жидкостный насос");
        add(ModBlocks.FLUID_EXHAUST.get(), "Выхлопная труба");
        add("gui.hbm_m.fluid_duct.overlay.fluid_empty", "Жидкость не задана");
        add(ModBlocks.INDUSTRIAL_BOILER.get(), "Промышленный котел");
        add(ModBlocks.SOLAR_BOILER.get(), "Солнечный котел");
        add(ModBlocks.SOLAR_MIRRORS.get(), "Солнечные зеркала (WIP)");
        add(ModBlocks.WATZ_POWERPLANT.get(), "Электростанция Ватц");
        add(ModBlocks.HYDROTREATER.get(), "Гидроочиститель (WIP)");
        add(ModBlocks.CATALYTIC_REFORMER.get(), "Каталитический риформер (WIP)");
        add(ModBlocks.DEUTERIUM_TOWER.get(), "Башня дейтерия (WIP)");
        add(ModBlocks.CHEMICAL_FACTORY.get(), "Химический завод (WIP)");
        add(ModBlocks.STEAM_TURBINE.get(), "Паровая турбина (WIP)");
        add(ModBlocks.LIQUEFACTOR.get(), "Сжижитель (WIP)");
        add(ModBlocks.CORE_EMITTER.get(), "Эмиттер ядра (WIP)");
        add(ModBlocks.CORE_INJECTOR.get(), "Инжектор ядра (WIP)");
        add(ModBlocks.CORE_RECEIVER.get(), "Приемник ядра (WIP)");
        add(ModBlocks.VACUUM_DISTILL.get(), "Вакуумная дистилляция (WIP)");
        add(ModBlocks.TURBOFAN.get(), "Турбовентилятор (WIP)");

        // --- WIP Machines (3D OBJ models) ---
        add(ModBlocks.AMMO_PRESS.get(), "Пресс для патронов");
        add("container.hbm_m.ammo_press", "Пресс для патронов");
        add(ModBlocks.ANNIHILATOR.get(), "Аннигилятор");
        add("container.hbm_m.annihilator", "Аннигилятор");
        add("gui.hbm_m.annihilator.pool", "Пул: %s");
        add("gui.hbm_m.annihilator.destroyed", "Уничтожено: %s");
        add(ModBlocks.ARC_FURNACE.get(), "Дуговая печь");
        add(ModBlocks.ASSEMBLY_FACTORY.get(), "Сборочный завод (WIP)");
        add(ModBlocks.AUTOSAW.get(), "Автопила");
        add(ModBlocks.BEAMLINE.get(), "Канал пучка (WIP)");
        add(ModBlocks.BOILER.get(), "Котёл");
        add("container.hbm_m.boiler", "Котёл");
        add(ModBlocks.PUMP_STEAM.get(), "Паровой насос для грунтовых вод");
        add(ModBlocks.PUMP_ELECTRIC.get(), "Электрический насос для грунтовых вод");
        add("container.hbm_m.machine_pump_electric", "Электрический насос для грунтовых вод");
        add(ModBlocks.BOILER_FUSION.get(), "Термоядерный котёл (WIP)");
        add(ModBlocks.BREEDER_FUSION.get(), "Термоядерный бридер (WIP)");
        add(ModBlocks.CHIMNEY_BRICK.get(), "Кирпичная труба");
        add(ModBlocks.CHIMNEY_INDUSTRIAL.get(), "Промышленная труба");
        add("container.hbm_m.chimney", "Дымовая труба");
        add(ModBlocks.COKER.get(), "Коксователь");
        add(ModBlocks.COLLECTOR.get(), "Коллектор (WIP)");
        add(ModBlocks.COMBINATION_OVEN.get(), "Комбинированная печь");
        add("container.hbm_m.combination_oven", "Комбинированная печь");
        add("container.hbm_m.arc_furnace", "Дуговая печь");
        add(ModBlocks.COMBUSTION_ENGINE.get(), "Двигатель внутреннего сгорания");
        add("container.hbm_m.combustion_engine", "Двигатель внутреннего сгорания");
        add(ModBlocks.COMPRESSOR.get(), "Компрессор");
        add("container.hbm_m.compressor", "Компрессор");
        add(ModBlocks.CONDENSER_POWERED.get(), "Электрический конденсатор");
        add(ModBlocks.LPW2.get(), "LPW2");
        add(ModItems.COKE_PETROLEUM.get(), "Нефтяной кокс");
        add(ModItems.ASH_WOOD.get(), "Древесная зола");
        add(ModItems.ASH_COAL.get(), "Угольная зола");
        add(ModItems.ASH_MISC.get(), "Пепел");
        add(ModItems.ASH_FLY.get(), "Летучая зола");
        add(ModItems.ASH_SOOT.get(), "Мелкая сажа");
        add(ModItems.WASTE_PLATE_U233.get(), "Обеднённая топливная пластина (Высокообогащённый уран-233)");
        add(ModItems.WASTE_PLATE_U235.get(), "Обеднённая топливная пластина (Высокообогащённый уран-235)");
        add(ModItems.WASTE_PLATE_PU239.get(), "Обеднённая топливная пластина (Высокообогащённый плутоний-239)");
        add(ModBlocks.CONVEYOR.get(), "Конвейерная лента");
        add(ModBlocks.CONVEYOR_DOUBLE.get(), "Двойная конвейерная лента");
        add(ModBlocks.CONVEYOR_EXPRESS.get(), "Скоростная конвейерная лента");
        add(ModBlocks.CONVEYOR_TRIPLE.get(), "Тройная конвейерная лента");
        add(ModBlocks.CONVEYOR_LIFT.get(), "Конвейерный подъёмник");
        add(ModBlocks.CONVEYOR_CHUTE.get(), "Конвейерный жёлоб");
        add(ModBlocks.CONVEYOR_PRESS.get(), "Конвейерный пресс");
        add(ModBlocks.COUPLER.get(), "Сцепка (WIP)");
        add(ModBlocks.DETECTOR.get(), "Детектор (WIP)");
        add(ModBlocks.DIESELGEN.get(), "Дизельный генератор");
        add("container.hbm_m.dieselgen", "Дизельный генератор");
        add(ModBlocks.DIPOLE.get(), "Дипольный магнит (WIP)");
        add(ModBlocks.DRONE.get(), "Дрон (WIP)");
        add(ModBlocks.ELECTRIC_HEATER.get(), "Электронагреватель");
        add("container.hbm_m.machine_electric_heater", "Электронагреватель");
        add(ModBlocks.ELECTROLYSER.get(), "Электролизер");
        add("container.hbm_m.electrolyser", "Электролизер");
        add(ModBlocks.EPRESS.get(), "Электрический пресс");
        add("container.hbm_m.epress", "Электрический пресс");
        add(ModBlocks.EXPOSURE_CHAMBER.get(), "Камера облучения");
        add("container.hbm_m.exposure_chamber", "Камера облучения");
        add(ModBlocks.FENSU.get(), "Промышленный вентилятор (WIP)");
        add(ModBlocks.FENSU2.get(), "Реддендитовая батарея");
        add("container.hbm_m.machine_battery_redd", "Реддендитовая батарея");
        add(ModBlocks.FIREBOX.get(), "Топка");
        add("container.hbm_m.firebox", "Топка");
        add(ModBlocks.FRACTION_SPACER.get(), "Фракционный разделитель (WIP)");
        add(ModBlocks.FURNACE_IRON.get(), "Железная печь");
        add("container.hbm_m.furnace_iron", "Железная печь");
        add(ModBlocks.FURNACE_STEEL.get(), "Стальная печь");
        add("container.hbm_m.furnace_steel", "Стальная печь");
        add(ModBlocks.HEATEX.get(), "Теплообменник");
        add("container.hbm_m.heatex", "Теплообменник");
        add(ModBlocks.HEPHAESTUS.get(), "Гефест");
        add(ModBlocks.ICF.get(), "Инерциальный термоядерный синтез (WIP)");
        add(ModBlocks.INTAKE.get(), "Воздухозаборник (WIP)");
        add(ModBlocks.KLYSTRON.get(), "Клистрон (WIP)");
        add(ModBlocks.MHDT.get(), "МГД-турбина (WIP)");
        add(ModBlocks.MICROWAVE.get(), "Микроволновка");
        add("container.hbm_m.microwave", "Микроволновка");
        add(ModBlocks.MINING_LASER.get(), "Горнодобывающий лазер");
        add("container.hbm_m.mining_laser", "Горнодобывающий лазер");
        add("gui.hbm_m.mining_laser.depth", "Глубина: %d");
        add(ModBlocks.OILBURNER.get(), "Нефтяная горелка");
        add("container.hbm_m.oilburner", "Нефтяная горелка");
        add(ModBlocks.OILBURNER_HP.get(), "Нефтяная горелка ВД");
        add(ModBlocks.ORBUS.get(), "Орбус (WIP)");
        add(ModBlocks.ORE_SLOPPER.get(), "Рудный шлаппер");
        add("container.hbm_m.ore_slopper", "Рудный шлаппер");
        add(ModBlocks.PLASMA_FORGE.get(), "Плазменная кузница (WIP)");
        add(ModBlocks.PYROOVEN.get(), "Пиролизная печь");
        add(ModBlocks.QUADRUPOLE.get(), "Квадрупольный магнит (WIP)");
        add(ModBlocks.RADGEN.get(), "Радиоизотопный генератор (WIP)");
        add(ModBlocks.RADIOLYSIS.get(), "Камера радиолиза");
        add("container.hbm_m.radiolysis", "Камера радиолиза");
        add(ModBlocks.REACTOR_SMALL.get(), "Малый реактор (WIP)");
        add(ModBlocks.RFC.get(), "Генератор RFC (WIP)");
        add(ModBlocks.ROTARY_FURNACE.get(), "Вращающаяся печь");
        add("container.hbm_m.rotary_furnace", "Вращающаяся печь");
        add(ModBlocks.SAWMILL.get(), "Лесопилка");
        add(ModBlocks.SOLIDIFIER.get(), "Затвердитель");
        add(ModBlocks.ASHPIT.get(), "Зольник");
        add(ModBlocks.REACTOR_RESEARCH.get(), "Исследовательский реактор");
        add(ModBlocks.MACHINE_RADGEN.get(), "Радиационный двигатель");
        add(ModBlocks.SOURCE.get(), "Источник нейтронов (WIP)");
        add(ModBlocks.INDUSTRIAL_GENERATOR.get(), "Промышленный генератор");
        add("container.hbm_m.industrial_generator", "Промышленный генератор");
        add(ModBlocks.STEAM_ENGINE.get(), "Паровой двигатель");
        add("container.hbm_m.steam_engine", "Паровой двигатель");
        add(ModBlocks.STIRLING.get(), "Двигатель Стирлинга");
        add(ModBlocks.STIRLING_CREATIVE.get(), "Креативный двигатель Стирлинга");
        add(ModBlocks.STIRLING_STEEL.get(), "Стальной двигатель Стирлинга");
        add("container.hbm_m.stirling", "Двигатель Стирлинга");
        add(ModBlocks.STRAND_CASTER.get(), "Машина непрерывного литья");
        add("container.hbm_m.strand_caster", "Машина непрерывного литья");
        add(ModBlocks.THRESHER.get(), "Молотилка");
        add(ModBlocks.MACHINE_AUTOCRAFTER.get(), "Автоматический верстак");
        add("container.hbm_m.autocrafter", "Автоматический верстак");
        add(ModBlocks.MACHINE_FUNNEL.get(), "Воронка-комбинатор");
        add("container.hbm_m.funnel", "Воронка-комбинатор");
        add(ModBlocks.PUREX.get(), "PUREX");
        add("container.hbm_m.purex", "PUREX");
        add(ModBlocks.TORUS.get(), "Тор (WIP)");
        add(ModBlocks.TURBINEGAS.get(), "Газовая турбина");
        add(ModBlocks.WATZ_PUMP.get(), "Насос WATZ (WIP)");
        add(ModBlocks.CHUNGUS.get(), "Чангус (WIP)");

        add("block.hbm_m.machine_battery", "Энергохранилище");
        add("block.hbm_m.ore_oil", "Нефтеносная руда");
        add("block.hbm_m.geysir_dirt", "Гейзерный грунт");
        add("block.hbm_m.geysir_stone", "Гейзерный камень");
        add("block.hbm_m.nuclear_fallout", "Радиоактивный осадок");
        add("block.hbm_m.block_fallout", "Блок радиоактивных осадков");
        add("block.hbm_m.dead_dirt", "Мёртвая земля");
        add("block.hbm_m.block_u233", "Блок урана-233");
        add("block.hbm_m.block_u235", "Блок урана-235");
        add("block.hbm_m.block_u238", "Блок урана-238");
        add("block.hbm_m.block_plutonium", "Плутониевый блок");
        add("block.hbm_m.block_pu238", "Блок плутония-238");
        add("block.hbm_m.block_pu239", "Блок плутония-239");
        add("block.hbm_m.block_pu240", "Блок плутония-240");
        add("block.hbm_m.block_pu241", "Блок плутония-241");
        add("block.hbm_m.block_actinium", "Актиниевый блок");
        add("block.hbm_m.block_advanced_alloy", "Блок продвинутого сплава");
        add("block.hbm_m.block_aluminum", "Алюминиевый блок");
        add("block.hbm_m.block_schrabidium", "Шрабидиевый блок");
        add("block.hbm_m.block_saturnite", "Сатурнитовый блок");
        add("block.hbm_m.block_lead", "Свинцовый блок");
        add("block.hbm_m.block_red_copper", "Блок красной меди");
        add("block.hbm_m.block_titanium", "Титановый блок");
        add("block.hbm_m.block_cobalt", "Кобальтовый блок");
        add("block.hbm_m.block_tungsten", "Вольфрамовый блок");
        add("block.hbm_m.block_starmetal", "Блок звёздного металла");
        add("block.hbm_m.block_beryllium", "Бериллиевый блок");
        add("block.hbm_m.block_bismuth", "Висмутовый блок");
        add("block.hbm_m.block_desh", "Блок деша");
        add("block.hbm_m.block_combine_steel", "Блок комбинированной стали");
        // Надо чекнуть верность перевода dura steel, ибо у нас предметы dura steel почему-то переведены как высокоскоростные
        add("block.hbm_m.block_dura_steel", "Блок высокопрочной стали");
        add("block.hbm_m.block_euphemium", "Эвфемиевый блок");
        add("block.hbm_m.block_dineutronium", "Динейтрониевый блок");
        add("block.hbm_m.block_australium", "Австралиевый блок");
        add("block.hbm_m.block_lanthanium", "Лантановый блок");
        add("block.hbm_m.block_niobium", "Ниобиевый блок");
        add("block.hbm_m.block_cadmium", "Кадмиевый блок");
        add("block.hbm_m.block_zirconium", "Циркониевый блок");
        add("block.hbm_m.block_neptunium", "Нептуниевый блок");
        add("block.hbm_m.block_boron", "Борный блок");
        add("block.hbm_m.block_ra226", "Блок радия-226");
        add("block.hbm_m.block_thorium", "Ториевый блок");
        add("block.hbm_m.block_mox_fuel", "Блок MOX-топлива");
        add("block.hbm_m.block_schrabidium_fuel", "Блок шрабидиевого топлива");
        add("block.hbm_m.block_schraranium", "Шрараниевый блок");
        add("block.hbm_m.block_schrabidate", "Шрабидатовый блок");
        add("block.hbm_m.block_solinium", "Солиниевый блок");
        add("block.hbm_m.block_schrabidium_cluster", "Шрабидиевый кластер");
        add("block.hbm_m.schrabidium_ore", "Шрабидиевая руда");
        add("block.hbm_m.schrabidium_ore_nether", "Адская шрабидиевая руда");
        add("block.hbm_m.schrabidium_ore_gneiss", "Гнейсовая шрабидиевая руда");
        add("block.hbm_m.block_uranium_fuel", "Блок уранового топлива");
        add("block.hbm_m.block_thorium_fuel", "Блок ториевого топлива");
        add("block.hbm_m.block_plutonium_fuel", "Блок плутониевого топлива");
        add("block.hbm_m.block_ferrouranium", "Ферроурановый блок");
        // перевод tc alloy block неточный
        add("block.hbm_m.block_tcalloy", "Блок TCalloy");
        // перевод cd alloy block также неточный
        add("block.hbm_m.block_cdalloy", "Блок CDalloy");
        add("block.hbm_m.deco_steel", "Стальной декоративный блок");
        add(ModBlocks.DECO_RUSTY_STEEL.get(), "Ржавый стальной декоративный блок");
        add("block.hbm_m.depth_stone_slab", "Плита из глубинного камня");
        add("block.hbm_m.depth_stone_nether_slab", "Плита из адского глубинного камня");
        add("block.hbm_m.depth_stone_stairs", "Ступеньки из глубинных кирпичей");
        add("block.hbm_m.barrel_red", "Красная бочка");
        add("block.hbm_m.airbomb", "Авиабомба");
        add("block.hbm_m.balebomb_test", "Жар-бомба");
        add("item.hbm_m.airbomb_a", "Авиабомба [снаряд]");
        add("item.hbm_m.airnukebomb_a", "Ядерная бомба [снаряд]");


        add("block.hbm_m.wire_coated", "Провод из красной меди");
        add("block.hbm_m.wood_burner", "Дровяной генератор");
        add("block.hbm_m.shredder", "Измельчитель");
        add("block.hbm_m.blast_furnace", "Доменная печь");
        add("block.hbm_m.blast_furnace_extension", "Расширение доменной печи");
        add("block.hbm_m.heating_oven", "Нагревательная печь");
        add("block.hbm_m.press", "Пресс");
        add("block.hbm_m.geiger_counter_block", "Стационарный счетчик Гейгера");
        add("block.hbm_m.decon", "Обеззараживатель игрока");
        add("block.hbm_m.rad_absorber.base", "Поглотитель радиации");
        add("block.hbm_m.rad_absorber.red", "Усовершенствованный поглотитель радиации");
        add("block.hbm_m.rad_absorber.green", "Элитный поглотитель радиации");
        add("block.hbm_m.rad_absorber.pink", "Продвинутый поглотитель радиации");
        add("item.hbm_m.powder_desh_mix", "Смесь деш");
        add("item.hbm_m.powder_nitan_mix", "Нитановая смесь");
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

        // ЖИДКОСТИ

        add("fluid.hbm_m.water", "Вода");
        add("fluid.hbm_m.air", "Воздух");
        add("fluid.hbm_m.crude_oil", "Сырая нефть");
        add("fluid.hbm_m.petroleum", "Нефть");
        add("fluid.hbm_m.diesel", "Дизель");
        add("fluid.hbm_m.diesel_crack", "Крекированный дизель");
        add("fluid.hbm_m.diesel_crack_reform", "Очищенный крекированный дизель");
        add("fluid.hbm_m.diesel_reform", "Риформинг-дизель");
        add("fluid.hbm_m.gasoline", "Бензин");
        add("fluid.hbm_m.gasoline_leaded", "Этилированный бензин");
        add("fluid.hbm_m.kerosene", "Керосин");
        add("fluid.hbm_m.kerosene_reform", "Риформинг-керосин");
        add("fluid.hbm_m.heavyoil", "Тяжёлая нефть");
        add("fluid.hbm_m.heavyoil_vacuum", "Вакуумная тяжёлая нефть");
        add("fluid.hbm_m.lightoil", "Лёгкая нефть");
        add("fluid.hbm_m.lightoil_crack", "Лёгкая крекированная нефть");
        add("fluid.hbm_m.lightoil_ds", "Обессеренная лёгкая нефть");
        add("fluid.hbm_m.lightoil_vacuum", "Вакуумная лёгкая нефть");
        add("fluid.hbm_m.heatingoil", "Печное топливо");
        add("fluid.hbm_m.heatingoil_vacuum", "Вакуумное печное топливо");
        add("fluid.hbm_m.naphtha", "Нафта");
        add("fluid.hbm_m.naphtha_coker", "Коксовая нафта");
        add("fluid.hbm_m.naphtha_crack", "Крекированная нафта");
        add("fluid.hbm_m.naphtha_ds", "Обессеренная нафта");
        add("fluid.hbm_m.bitumen", "Битум");
        add("fluid.hbm_m.lubricant", "Смазка");
        add("fluid.hbm_m.crackoil", "Крекированная нефть");
        add("fluid.hbm_m.crackoil_ds", "Обессеренная крекированная нефть");
        add("fluid.hbm_m.hotcrackoil", "Горячая крекированная нефть");
        add("fluid.hbm_m.hotcrackoil_ds", "Обессеренная горячая крекированная нефть");
        add("fluid.hbm_m.hotoil", "Горячая нефть");
        add("fluid.hbm_m.hotoil_ds", "Обессеренная горячая нефть");
        add("fluid.hbm_m.oil_base", "Базовое масло");
        add("fluid.hbm_m.oil_coker", "Коксовая нефть");
        add("fluid.hbm_m.oil_ds", "Обессеренная нефть");
        add("fluid.hbm_m.reclaimed", "Регенерированное масло");
        add("fluid.hbm_m.slop", "Нефтяной шлак");
        add("fluid.hbm_m.lpg", "СУГ (Сжиженный углеводородный газ)");
        add("fluid.hbm_m.petroil", "Нефтяное масло");
        add("fluid.hbm_m.petroil_leaded", "Этилированное нефтяное масло");
        add("fluid.hbm_m.reformate", "Риформат");
        add("fluid.hbm_m.aromatics", "Ароматические углеводороды");
        add("fluid.hbm_m.unsaturateds", "Ненасыщенные углеводороды");
        add("fluid.hbm_m.xylene", "Ксилол");
        add("fluid.hbm_m.coalcreosote", "Угольный креозот");
        add("fluid.hbm_m.coaloil", "Синтетическое угольное топливо");
        add("fluid.hbm_m.woodoil", "Древесное масло");
        add("fluid.hbm_m.fishoil", "Рыбий жир");
        add("fluid.hbm_m.sunfloweroil", "Подсолнечное масло");
        add("fluid.hbm_m.gas", "Природный газ");
        add("fluid.hbm_m.gas_coker", "Коксовый газ");
        add("fluid.hbm_m.coalgas", "Угольный газ");
        add("fluid.hbm_m.coalgas_leaded", "Этилированный угольный газ");
        add("fluid.hbm_m.syngas", "Синтез-газ");
        add("fluid.hbm_m.reformgas", "Риформинг-газ");
        add("fluid.hbm_m.sourgas", "Высокосернистый газ");
        add("fluid.hbm_m.biogas", "Биогаз");
        add("fluid.hbm_m.hydrogen", "Водород");
        add("fluid.hbm_m.oxygen", "Кислород");
        add("fluid.hbm_m.carbondioxide", "Углекислый газ");
        add("fluid.hbm_m.oxyhydrogen", "Гремучий газ");
        add("fluid.hbm_m.smoke", "Дым");
        add("fluid.hbm_m.smoke_leaded", "Этилированный дым");
        add("fluid.hbm_m.smoke_poison", "Ядовитый дым");
        add("fluid.hbm_m.wastegas", "Отработанный газ");
        add("fluid.hbm_m.chlorine", "Хлор");
        add("fluid.hbm_m.phosgene", "Phosgene");
        add("fluid.hbm_m.mustardgas", "Mustard Gas");
        add("fluid.hbm_m.xenon", "Xenon");
        add("fluid.hbm_m.deuterium", "Deuterium");
        add("fluid.hbm_m.tritium", "Tritium");
        add("fluid.hbm_m.helium3", "Helium-3");
        add("fluid.hbm_m.helium4", "Helium-4");
        add("fluid.hbm_m.uf6", "Uranium Hexafluoride");
        add("fluid.hbm_m.puf6", "Plutonium Hexafluoride");
        add("fluid.hbm_m.plasma_dt", "D-T Plasma");
        add("fluid.hbm_m.plasma_hd", "H-D Plasma");
        add("fluid.hbm_m.plasma_ht", "H-T Plasma");
        add("fluid.hbm_m.plasma_dh3", "D-He3 Plasma");
        add("fluid.hbm_m.plasma_xm", "Xenon-Mercury Plasma");
        add("fluid.hbm_m.plasma_bf", "Balefire Plasma");
        add("fluid.hbm_m.steam", "Steam");
        add("fluid.hbm_m.airblast", "Воздушное дутьё");
        add("fluid.hbm_m.flue", "Дымовые газы");
        add("fluid.hbm_m.hotsteam", "Hot Steam");
        add("fluid.hbm_m.superhotsteam", "Super Hot Steam");
        add("fluid.hbm_m.ultrahotsteam", "Ultra Hot Steam");
        add("fluid.hbm_m.spentsteam", "Spent Steam");
        add("fluid.hbm_m.heavywater", "Heavy Water");
        add("fluid.hbm_m.heavywater_hot", "Hot Heavy Water");
        add("fluid.hbm_m.coolant", "Coolant");
        add("fluid.hbm_m.coolant_hot", "Hot Coolant");
        add("fluid.hbm_m.cryogel", "Cryogel");
        add("fluid.hbm_m.perfluoromethyl", "Perfluoromethyl");
        add("fluid.hbm_m.perfluoromethyl_cold", "Cold Perfluoromethyl");
        add("fluid.hbm_m.perfluoromethyl_hot", "Hot Perfluoromethyl");
        add("fluid.hbm_m.sulfuric_acid", "Sulfuric Acid");
        add("fluid.hbm_m.nitric_acid", "Nitric Acid");
        add("fluid.hbm_m.nitroglycerin", "Nitroglycerin");
        add("fluid.hbm_m.peroxide", "Hydrogen Peroxide");
        add("fluid.hbm_m.lye", "Lye");
        add("fluid.hbm_m.vitriol", "Vitriol");
        add("fluid.hbm_m.solvent", "Solvent");
        add("fluid.hbm_m.fracksol", "Fracking Solution");
        add("fluid.hbm_m.ethanol", "Ethanol");
        add("fluid.hbm_m.biofuel", "Biofuel");
        add("fluid.hbm_m.mercury", "Mercury");
        add("fluid.hbm_m.lead", "Liquid Lead");
        add("fluid.hbm_m.lead_hot", "Hot Liquid Lead");
        add("fluid.hbm_m.sodium", "Liquid Sodium");
        add("fluid.hbm_m.sodium_hot", "Hot Liquid Sodium");
        add("fluid.hbm_m.calcium_solution", "Calcium Solution");
        add("fluid.hbm_m.calcium_chloride", "Calcium Chloride");
        add("fluid.hbm_m.potassium_chloride", "Potassium Chloride");
        add("fluid.hbm_m.chlorocalcite_solution", "Chlorocalcite Solution");
        add("fluid.hbm_m.chlorocalcite_mix", "Chlorocalcite Mix");
        add("fluid.hbm_m.chlorocalcite_cleaned", "Cleaned Chlorocalcite");
        add("fluid.hbm_m.bauxite_solution", "Bauxite Solution");
        add("fluid.hbm_m.alumina", "Alumina");
        add("fluid.hbm_m.sodium_aluminate", "Sodium Aluminate");
        add("fluid.hbm_m.redmud", "Red Mud");
        add("fluid.hbm_m.schrabidic", "Schrabidic Acid");
        add("fluid.hbm_m.aschrab", "Anti-Schrabidium");
        add("fluid.hbm_m.sas3", "SAS-3");
        add("fluid.hbm_m.balefire", "Balefire");
        add("fluid.hbm_m.amat", "Antimatter");
        add("fluid.hbm_m.thorium_salt", "Thorium Salt");
        add("fluid.hbm_m.thorium_salt_hot", "Hot Thorium Salt");
        add("fluid.hbm_m.thorium_salt_depleted", "Depleted Thorium Salt");
        add("fluid.hbm_m.watz", "Watz Fluid");
        add("fluid.hbm_m.lava", "Lava");
        add("fluid.hbm_m.concrete", "Concrete");
        add("fluid.hbm_m.blood", "Blood");
        add("fluid.hbm_m.blood_hot", "Hot Blood");
        add("fluid.hbm_m.colloid", "Colloid");
        add("fluid.hbm_m.smear", "Smear");
        add("fluid.hbm_m.wastefluid", "Waste Fluid");
        add("fluid.hbm_m.radiosolvent", "Radiosolvent");
        add("fluid.hbm_m.salient", "Salient");
        add("fluid.hbm_m.iongel", "Ion Gel");
        add("fluid.hbm_m.fullerene", "Fullerene");
        add("fluid.hbm_m.nitan", "Nitan Mix");
        add("fluid.hbm_m.dhc", "DHC");
        add("fluid.hbm_m.egg", "Liquid Egg");
        add("fluid.hbm_m.cholesterol", "Cholesterol");
        add("fluid.hbm_m.estradiol", "Estradiol");
        add("fluid.hbm_m.pheromone", "Pheromone");
        add("fluid.hbm_m.pheromone_m", "Male Pheromone");
        add("fluid.hbm_m.seedslurry", "Seed Slurry");
        add("fluid.hbm_m.enderjuice", "Ender Juice");
        add("fluid.hbm_m.xpjuice", "XP Juice");
        add("fluid.hbm_m.mug", "Mug Root Beer");
        add("fluid.hbm_m.mug_hot", "Hot Mug Root Beer");
        add("fluid.hbm_m.none", "None");
        add("fluid.hbm_m.death", "Death");
        add("fluid.hbm_m.pain", "Pain");
        add("fluid.hbm_m.stellar_flux", "Stellar Flux");
        add("fluid.hbm_m.bromide", "Bromide");


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
        add("block.hbm_m.crate_tungsten", "Вольфрамовый ящик");
        add("block.hbm_m.crate_template", "Шаблонный ящик");

        add("block.hbm_m.waste_grass", "Мёртвая трава");
        add("block.hbm_m.waste_leaves", "Мёртвая листва");

        // MACHINE GUI
        
        add("tooltip.hbm_m.armor_table.main_slot", "Вставьте броню, чтобы её модифицировать...");
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

        // POWER ARMOR CONTROLS
        add("key.hbm_m.power_armor_dash", "Рывок силовой брони");
        add("key.hbm_m.power_armor_vats", "VATS силовой брони");
        add("key.hbm_m.power_armor_thermal", "Тепловизор силовой брони");
        add("key.hbm_m.rbmk_crane_up", "РБМК Кран: вверх");
        add("key.hbm_m.rbmk_crane_down", "РБМК Кран: вниз");
        add("key.hbm_m.rbmk_crane_left", "РБМК Кран: влево");
        add("key.hbm_m.rbmk_crane_right", "РБМК Кран: вправо");
        add("key.hbm_m.rbmk_crane_load", "РБМК Кран: загрузка");

        add("hud.hbm_m.vats.on", "HUD: ON");
        add("hud.hbm_m.vats.off", "HUD: OFF");
        add("hud.hbm_m.thermal.on", "Тепловизор: ON");
        add("hud.hbm_m.thermal.off", "Тепловизор: OFF");
        add("hud.hbm_m.thermal.warning", "Внимание: шейдерная версия тепловизора экспериментальная и может быть нестабильной. В случае проблем переключитесь на более простую версию в конфиге (Alt + 0, вкладка Рендеринг). Чтобы включить тепловизор, нажмите еще раз. Это сообщение больше не будет показано для данного мира.");
        add("hud.hbm_m.dash.perform", "Рывок выполнен");
        add("text.autoconfig.hbm_m.option.thermalRenderMode", "Режим рендера тепловизора");
        add("text.autoconfig.hbm_m.option.thermalRenderMode.@Tooltip", "Выберите способ рендера тепловизора.");
        add("text.autoconfig.hbm_m.option.thermalRenderMode.FULL_SHADER", "Полный шейдер");
        add("text.autoconfig.hbm_m.option.thermalRenderMode.ORIGINAL_FALLBACK", "Спектральный фолбэк");

        // ARMOR BATTERIES
        add("item.hbm_m.armor_battery", "Батарея брони");
        add("item.hbm_m.armor_battery_mk2", "Батарея брони MK2");
        add("item.hbm_m.armor_battery_mk3", "Батарея брони MK3");

        // ARMOR MODIFICATIONS
        add("tooltip.hbm_m.mod.servos.description", "Увеличивает скорость движения");
        add("tooltip.hbm_m.mod.servos.effect", "+Скорость движения");
        add("tooltip.hbm_m.mod.cladding.description", "Защищает от коррозии и повреждений");
        add("tooltip.hbm_m.mod.cladding.effect", "+Защита от коррозии");
        add("tooltip.hbm_m.mod.kevlar.description", "Увеличивает защиту от пуль");
        add("tooltip.hbm_m.mod.kevlar.effect", "+Защита от пуль");
        add("tooltip.hbm_m.mod.extra.description", "Расширяет возможности брони");
        add("tooltip.hbm_m.mod.extra.effect", "+Дополнительные возможности");
        add("tooltip.hbm_m.mod.battery.description", "Увеличивает емкость батареи");
        add("tooltip.hbm_m.mod.battery.effect", "+25% емкость батареи");
        add("tooltip.hbm_m.mod.battery_mk2.effect", "+50% емкость батареи");
        add("tooltip.hbm_m.mod.battery_mk3.effect", "+100% емкость батареи");

        add("tooltip.hbm_m.rad_protection.value", "Сопротивление радиации: %s");

        add("container.hbm_m.armor_table", "Стол модификации брони");
        add("container.hbm_m.machine_assembler", "Сборочная машина (Старая)");
        add("container.hbm_m.advanced_assembly_machine", "Сборочная машина");
        add(ModBlocks.CRUCIBLE.get(), "Тигель (WIP)");
        add(ModBlocks.FOUNDRY_BASIN.get(), "Литейный бассейн");
        add(ModBlocks.FOUNDRY_CHANNEL.get(), "Литейный канал");
        add("container.hbm_m.crucible", "Тигель");
        add(ModBlocks.LAUNCH_PAD.get(), "Пусковая площадка");
        add(ModBlocks.LAUNCH_PAD_RUSTED.get(), "Ржавая пусковая площадка");
        add(ModItems.DESIGNATOR.get(), "Целеуказатель");
        add(ModItems.DESIGNATOR_RANGE.get(), "Целеуказатель с дальномером");
        add(ModItems.DESIGNATOR_MANUAL.get(), "Ручной целеуказатель");
        add("tooltip.hbm_m.designator.target", "Координаты цели:");
        add("tooltip.hbm_m.designator.no_target", "Выберите цель.");
        add("message.hbm_m.designator.position_set", "Позиция установлена!");
        add("message.hbm_m.designator.position_set_xy", "Позиция установлена: X: %s, Z: %s");
        add("gui.hbm_m.designator", "Ручной целеуказатель");
        add("gui.hbm_m.designator.set_x", "Установить текущую позицию X...");
        add("gui.hbm_m.designator.set_z", "Установить текущую позицию Z...");
        add(ModBlocks.CRYSTALLIZER.get(), "Рудный окислитель");
        add(ModBlocks.BREEDER.get(), "Реактор-размножитель (WIP)");
        add(ModBlocks.LARGE_PYLON.get(), "Большой пилон (WIP)");
        add(ModBlocks.HYDRAULIC_FRACKINING_TOWER.get(), "Башня гидроразрыва пласта");
        add(ModBlocks.COOLING_TOWER.get(), "Градирня");
        add(ModBlocks.TOWER_SMALL.get(), "Градирня (малая)");
        add("container.hbm_m.cooling_tower", "Градирня");
        add("info.hbm_m.cooling_tower.status", "Горячий теплоноситель: %d / %d мБ | Теплоноситель: %d / %d мБ | %s");
        add("info.hbm_m.cooling_tower.active", "Охлаждение");
        add("info.hbm_m.cooling_tower.idle", "Простой");
        add(ModBlocks.CYCLOTRON.get(), "Циклотрон (WIP)");
        add(ModItems.PART_LITHIUM.get(),   "Частица лития");
        add(ModItems.PART_BERYLLIUM.get(), "Частица бериллия");
        add(ModItems.PART_CARBON.get(),    "Частица углерода");
        add(ModItems.PART_COPPER.get(),    "Частица меди");
        add(ModItems.PART_PLUTONIUM.get(), "Частица плутония");
        add(ModBlocks.ZIRNOX.get(), "Зирнокс (WIP)");
        add(ModBlocks.ARC_WELDER.get(), "Дуговой сварщик (WIP)");
        add(ModBlocks.SOLDERING_STATION.get(), "Паяльная станция (WIP)");
        add(ModBlocks.MIXER.get(), "Промышленный миксер (WIP)");
        add(ModBlocks.DERRICK.get(), "Деррик (WIP)");
        add(ModBlocks.RBMK_CONSOLE.get(), "Пульт РБМК (WIP)");
        add("msg.hbm_m.rbmk_console.linked", "Привязано к пульту РБМК");
        add("msg.hbm_m.rbmk_crane.linked", "Кран привязан к колонне");
        add("gui.hbm_m.save", "Сохранить");
        add("gui.hbm_m.close", "Закрыть");
        add("gui.hbm_m.rbmk_gauge", "РБМК Манометр");
        add("gui.hbm_m.rbmk_indicator", "РБМК Индикатор");
        add("gui.hbm_m.rbmk_numitron", "РБМК Нумитрон");
        add("gui.hbm_m.rbmk_graph", "РБМК График");
        add("gui.hbm_m.rbmk_lever", "РБМК Рычаг");
        add("gui.hbm_m.rbmk_keypad", "РБМК Клавиатура");
        add("gui.hbm_m.rbmk_terminal", "РБМК Терминал");
        add("sounds.hbm_m.subtitle.upgrade_plug", "Вставка топливного стержня");
        add("msg.hbm_m.rbmk_console.no_console_found", "Поблизости нет пульта РБМК");
        add("msg.hbm_m.rbmk_tool.stored", "Позиция сохранена: %s, %s, %s");
        add("msg.hbm_m.rbmk_tool.no_position", "Сначала нажмите на колонну ПКМ");
        add("tooltip.hbm_m.rbmk_tool.stored", "Сохранённая позиция: %s, %s, %s");
        add("tooltip.hbm_m.rbmk_tool.empty", "Позиция не сохранена");
        add(ModBlocks.RBMK_ROD.get(), "РБМК Топливный канал");
        add(ModBlocks.RBMK_ROD_MOD.get(), "РБМК Топливный канал (с замедлителем)");
        add(ModBlocks.RBMK_ROD_REASIM.get(), "РБМК Топливный канал (ReaSim)");
        add(ModBlocks.RBMK_ROD_REASIM_MOD.get(), "РБМК Топливный канал (ReaSim, с замедлителем)");
        add(ModBlocks.RBMK_CONTROL.get(), "РБМК Стержень управления");
        add(ModBlocks.RBMK_CONTROL_AUTO.get(), "РБМК Автостержень управления");
        add(ModBlocks.RBMK_MODERATOR.get(), "РБМК Замедлитель");
        add(ModBlocks.RBMK_ABSORBER.get(), "РБМК Поглотитель");
        add(ModBlocks.RBMK_REFLECTOR.get(), "РБМК Отражатель");
        add(ModBlocks.RBMK_COOLER.get(), "РБМК Охладитель");
        add(ModBlocks.RBMK_BOILER.get(), "РБМК Паровой канал");
        add(ModBlocks.RBMK_HEATER.get(), "РБМК Нагреватель");
        add(ModBlocks.RBMK_OUTGASSER.get(), "РБМК Канал облучения");
        add(ModBlocks.RBMK_STORAGE.get(), "РБМК Хранилище");
        add(ModBlocks.RBMK_BLANK.get(), "РБМК Заглушка");
        add(ModBlocks.RBMK_CONTROL_BLUE.get(),        "РБМК Стержень управления (синий)");
        add(ModBlocks.RBMK_CONTROL_GREEN.get(),       "РБМК Стержень управления (зелёный)");
        add(ModBlocks.RBMK_CONTROL_YELLOW.get(),      "РБМК Стержень управления (жёлтый)");
        add(ModBlocks.RBMK_CONTROL_PURPLE.get(),      "РБМК Стержень управления (фиолетовый)");
        add(ModBlocks.RBMK_CONTROL_MOD.get(),         "РБМК Стержень управления с замедлителем");
        add(ModBlocks.RBMK_CONTROL_MOD_AUTO.get(),    "РБМК Авто-стержень с замедлителем");
        add(ModBlocks.RBMK_CONTROL_REASIM.get(),      "РБМК Стержень управления (ReaSim)");
        add(ModBlocks.RBMK_CONTROL_REASIM_AUTO.get(), "РБМК Авто-стержень (ReaSim)");
        add(ModBlocks.RBMK_STEAM_INLET.get(),         "РБМК Вход пара");
        add(ModBlocks.RBMK_STEAM_OUTLET.get(),        "РБМК Выход пара");
        add(ModBlocks.RBMK_LOADER.get(),              "РБМК Загрузчик (основание)");
        add(ModBlocks.RBMK_AUTOLOADER.get(),          "РБМК Автозагрузчик");
        add(ModBlocks.RBMK_CRANE_CONSOLE.get(),       "РБМК Пульт крана");
        add(ModBlocks.RBMK_DEBRIS.get(),              "РБМК Обломки");
        add(ModBlocks.RBMK_DEBRIS_BURNING.get(),      "РБМК Горящие обломки");
        add(ModBlocks.RBMK_DEBRIS_DIGAMMA.get(),      "РБМК Обломки (дигамма)");
        add(ModBlocks.RBMK_DEBRIS_RADIATING.get(),    "РБМК Радиоактивные обломки");
        add(ModBlocks.RBMK_CORIUM.get(),              "РБМК Кориум");
        add(ModBlocks.RBMK_DISPLAY.get(),             "РБМК Дисплей");
        add(ModBlocks.RBMK_GAUGE.get(),               "РБМК Манометр");
        add(ModBlocks.RBMK_INDICATOR.get(),           "РБМК Индикатор");
        add(ModBlocks.RBMK_LEVER.get(),               "РБМК Рычаг");
        add(ModBlocks.RBMK_NUMITRON.get(),            "РБМК Нумитрон");
        add(ModBlocks.RBMK_GRAPH.get(),               "РБМК Граф");
        add(ModBlocks.RBMK_TERMINAL.get(),            "РБМК Терминал");
        add(ModBlocks.RBMK_KEYPAD.get(),              "РБМК Клавиатура");
        add(ModBlocks.RBMK_DISPLAY_BLANK.get(),       "РБМК Пустая панель");
        add(ModBlocks.FLARE_STACK.get(), "Факельная башня (WIP)");
        add(ModBlocks.PUMPJACK.get(), "Станок-качалка (WIP)");
        add(ModBlocks.RADAR.get(), "Радар (WIP)");
        add(ModBlocks.LARGE_RADAR.get(), "Большой радар (WIP)");
        add(ModBlocks.RADAR_SCREEN.get(), "Экран радара");
        add(ModBlocks.CRACKING_TOWER.get(), "Крекинг башня (WIP)");
        add(ModBlocks.FRACTION_TOWER.get(), "Фракционная башня (WIP)");
        add(ModBlocks.MINING_DRILL.get(), "Large Mining Drill");
        add(ModBlocks.FEL.get(), "FEL (WIP)");
        add(ModBlocks.SILEX.get(), "Silex (WIP)");
        add(ModBlocks.CHEMICAL_PLANT.get(), "Химическая установка");
        add(ModBlocks.CENTRIFUGE.get(), "Центрифуга (WIP)");
        add(ModBlocks.INDUSTRIAL_TURBINE.get(), "Промышленная турбина");
        add(ModBlocks.TURBINE.get(), "Турбина (WIP)");
        add(ModBlocks.SUBSTATION.get(), "Подстанция (WIP)");
        add("container.hbm_m.wood_burner", "Дровяной генератор");
        add(ModBlocks.TURRET_SENTRY.get(), "Турель \"Часовой\"");
        add("container.hbm_m.turret_sentry", "Турель \"Часовой\"");
        add(ModBlocks.TURRET_CHEKHOV.get(), "Турель \"Чехов\"");
        add("container.hbm_m.turret_chekhov", "Турель \"Чехов\"");
        add(ModBlocks.TURRET_FRIENDLY.get(), "Дружественная турель");
        add("container.hbm_m.turret_friendly", "Дружественная турель");
        add(ModBlocks.TURRET_JEREMY.get(), "Турель \"Джереми\"");
        add("container.hbm_m.turret_jeremy", "Турель \"Джереми\"");
        add(ModBlocks.TURRET_TAUON.get(), "Тау-турель");
        add("container.hbm_m.turret_tauon", "Тау-турель");
        add(ModBlocks.TURRET_RICHARD.get(), "Турель \"Ричард\"");
        add("container.hbm_m.turret_richard", "Турель \"Ричард\"");
        add(ModBlocks.TURRET_HOWARD.get(), "Турель \"Говард\"");
        add("container.hbm_m.turret_howard", "Турель \"Говард\"");
        add(ModBlocks.TURRET_MAXWELL.get(), "Турель \"Максвелл\"");
        add("container.hbm_m.turret_maxwell", "Турель \"Максвелл\"");
        add(ModBlocks.TURRET_FRITZ.get(), "Турель \"Фриц\"");
        add("container.hbm_m.turret_fritz", "Турель \"Фриц\"");
        add(ModBlocks.TURRET_ARTY.get(), "Артиллерийская турель");
        add("container.hbm_m.turret_arty", "Артиллерийская турель");
        add(ModBlocks.TURRET_HIMARS.get(), "Турель HIMARS");
        add("container.hbm_m.turret_himars", "Турель HIMARS");
        add("container.hbm_m.industrial_boiler", "Промышленный котел");
        add("gui.hbm_m.industrial_boiler.water", "Вода");
        add("gui.hbm_m.industrial_boiler.steam", "Пар");
        add("gui.hbm_m.industrial_boiler.heat", "Тепло");
        add("gui.hbm_m.solar_boiler.sunlight", "Солнечный свет");
        add("gui.hbm_m.solar_boiler.mirrors", "Активные зеркала: %s");
        add("container.hbm_m.solar_boiler", "Солнечный котел");
        add("tooltip.hbm_m.barrel.capacity", "Объём: %s мБ");
        add("tooltip.hbm_m.barrel.hot.yes", "Может хранить горячие жидкости");
        add("tooltip.hbm_m.barrel.hot.no", "Не может хранить горячие жидкости");
        add("tooltip.hbm_m.barrel.corrosive.yes", "Может хранить едкие жидкости");
        add("tooltip.hbm_m.barrel.corrosive.no", "Не может хранить едкие жидкости");
        add("tooltip.hbm_m.barrel.highly_corrosive.yes", "Может хранить сильно едкие жидкости");
        add("tooltip.hbm_m.barrel.highly_corrosive.no", "Не может хранить сильно едкие жидкости как следует");
        add("tooltip.hbm_m.barrel.antimatter.yes", "Может хранить антиматерию");
        add("tooltip.hbm_m.barrel.antimatter.no", "Не может хранить антиматерию");
        add("container.hbm_m.solar_mirrors", "Солнечные зеркала");
        add("msg.hbm_m.solar_mirror.sky_access", "Зеркало освещено солнцем");
        add("msg.hbm_m.solar_mirror.no_sky_access", "Зеркало затенено");
        add("container.hbm_m.watz_powerplant", "Электростанция Ватц");
        add("container.hbm_m.hydrotreater", "Гидроочиститель");
        add("container.hbm_m.catalytic_reformer", "Каталитический риформер");
        add("container.hbm_m.deuterium_tower", "Башня дейтерия");
        add("container.hbm_m.chemical_factory", "Химический завод");
        add("container.hbm_m.steam_turbine", "Паровая турбина");
        add("container.hbm_m.steam_condenser", "Паровой конденсатор");
        add("container.hbm_m.liquefactor", "Сжижитель");
        add("container.hbm_m.core_emitter", "Эмиттер ядра");
        add("container.hbm_m.core_injector", "Инжектор ядра");
        add("container.hbm_m.core_receiver", "Приемник ядра");
        add("container.hbm_m.vacuum_distill", "Вакуумная дистилляция");
        add("container.hbm_m.turbofan", "Турбовентилятор");
        add("container.hbm_m.industrial_turbine", "Промышленная турбина");
        add("container.hbm_m.radar", "Радар (WIP)");
        add("chat.radar.tolow", "Радар должен быть установлен выше!");
        add("gui.hbm_m.radar.scan_missiles", "Сканировать ракеты");
        add("gui.hbm_m.radar.scan_players", "Сканировать игроков");
        add("gui.hbm_m.radar.smart_mode", "Умный режим");
        add("gui.hbm_m.radar.tooltip.detect_missiles", "Обнаружение ракет");
        add("gui.hbm_m.radar.tooltip.detect_shells", "Обнаружение снарядов");
        add("gui.hbm_m.radar.tooltip.detect_players", "Обнаружение игроков");
        add("gui.hbm_m.radar.tooltip.smart_mode", "Умный режим");
        add("gui.hbm_m.radar.tooltip.smart_mode.desc", "Выход редстоуна игнорирует удаляющиеся ракеты");
        add("gui.hbm_m.radar.tooltip.red_mode", "Режим красного камня");
        add("gui.hbm_m.radar.tooltip.red_mode.on", "Включён: Сигнал редстоуна базируется на дальности");
        add("gui.hbm_m.radar.tooltip.red_mode.off", "Выключен: Сигнал редстоуна базируется на уровне цели");
        add("gui.hbm_m.radar.contact.velocity", "Скорость: %s б/т");
        add("gui.hbm_m.radar.contact.distance", "Дистанция: %s");
        add("gui.hbm_m.radar.contact.distance_h", "Гор.: %s");
        add("gui.hbm_m.radar.contact.coords", "X/Y/Z: %s / %s / %s");
        add("gui.hbm_m.radar.contact.alt", "Выс.: %s");
        add("gui.hbm_m.radar.slots", "Слоты");
        add("gui.hbm_m.radar.tooltip.show_map", "Показать карту");
        add("gui.hbm_m.radar.tooltip.toggle_gui", "Переключить интерфейс");
        add("gui.hbm_m.radar.tooltip.clear_map", "Очистить карту");
        add("tooltip.hbm_m.radar_linker.linked", "Привязан к:");
        add("tooltip.hbm_m.radar_linker.not_linked", "Не привязан. ПКМ по пусковой установке.");
        add("message.hbm_m.radar_linker.linked", "Пусковая установка привязана к радару");
        add("container.hbm_m.fraction_tower", "Фракционная башня (WIP)");
        add("container.hbm_m.turbine", "Турбина (WIP)");
        add("container.hbm_m.substation", "Подстанция (WIP)");
        add("container.hbm_m.heating_oven", "Нагревательная печь");
        add("container.hbm_m.machine_battery", "Энергохранилище");
        add("container.hbm_m.battery_socket", "Аккумуляторный разъём");
        add("container.hbm_m.press", "Пресс");
        add("container.hbm_m.anvil_block", "Индустриальная наковальня");
        add("container.hbm_m.anvil", "Наковальня %s");
        add("container.hbm_m.crate_iron", "Железный ящик");
        add("container.hbm_m.crate_steel", "Стальной ящик");
        add("container.hbm_m.crate_desh", "Деш ящик");
        add("container.hbm_m.crate_tungsten", "Вольфрамовый ящик");
        add("container.hbm_m.crate_template", "Шаблонный ящик");
        add("container.hbm_m.crystallizer", "Кристаллизатор");
        add("container.hbm_m.breeder", "Бридер");
        add("container.hbm_m.large_pylon", "Большой пилон");
        add("container.hbm_m.chemical_plant", "Химическая установка");
        add("container.hbm_m.centrifuge", "Центрифуга");

        add("gui.hbm_m.battery.priority.0", "Приоритет: Низкий");
        add("gui.hbm_m.battery.priority.0.desc", "Низший приоритет. Опустошается в первую очередь, заполняется в последнюю");
        add("gui.hbm_m.battery.priority.1", "Приоритет: Нормальный");
        add("gui.hbm_m.battery.priority.1.desc", "Стандартный приоритет для передачи энергии.");
        add("gui.hbm_m.battery.priority.2", "Приоритет: Высокий");
        add("gui.hbm_m.battery.priority.2.desc", "Высший приоритет. Заполняется первым, опустошается последним.");
        add("gui.hbm_m.battery.priority.recommended", "(Рекомендуется)");

        add("gui.hbm_m.battery.condition.no_signal", "Когда НЕТ редстоун-сигнала:");
        add("gui.hbm_m.battery.condition.with_signal", "Когда ЕСТЬ редстоун-сигнал:");

        add("gui.hbm_m.door_model_selection.title", "Выбор модели двери");
        add("door.model_type.hbm_m.legacy", "Классика");
        add("door.skin.hbm_m.round_airlock_door.default", "Новая");
        add("door.skin.hbm_m.round_airlock_door.clean", "Чистая");
        add("door.skin.hbm_m.round_airlock_door.green", "Зелёная");
        add("door.skin.hbm_m.fire_door.default", "Новая");
        add("door.skin.hbm_m.fire_door.black", "Чёрная");
        add("door.skin.hbm_m.fire_door.orange", "Оранжевая");
        add("door.skin.hbm_m.fire_door.trefoil", "Радиация");
        add("door.skin.hbm_m.fire_door.yellow", "Жёлтая");
        add("door.skin.hbm_m.secure_access_door.default", "Новая");
        add("door.skin.hbm_m.secure_access_door.gray", "Серая");
        add("door.skin.hbm_m.secure_access_door.black", "Черная");
        add("door.skin.hbm_m.secure_access_door.yellow", "Жёлтая");
        add("door.skin.hbm_m.sliding_blast_door.default", "Стандартная");
        add("door.skin.hbm_m.sliding_blast_door.variant1", "Вариант 1");
        add("door.skin.hbm_m.sliding_blast_door.variant2", "Вариант 2");
        add("door.skin.hbm_m.sliding_seal_door.default", "Новая");
        add("door.skin.hbm_m.large_vehicle_door.default", "Чистая");
        add("door.skin.hbm_m.large_vehicle_door.rad", "Радиация");
        add("door.skin.hbm_m.large_vehicle_door.clear", "Стандартная");
        add("door.skin.hbm_m.water_door.default", "Новая");
        add("door.skin.hbm_m.water_door.clean", "Чистая");
        add("door.skin.hbm_m.qe_sliding_door.default", "Новая");
        add("door.skin.hbm_m.qe_containment_door.default", "Новая");
        add("door.skin.hbm_m.qe_containment_door.trefoil", "Радиация");
        add("door.skin.hbm_m.qe_containment_door.trefoil_yellow", "Радиация 2");
        add("door.skin.hbm_m.vault_door.default", "Vault 101");
        add("door.skin.hbm_m.vault_door.skin_106", "Vault 106");
        add("door.skin.hbm_m.vault_door.skin_2", "Vault 2");
        add("door.skin.hbm_m.vault_door.skin_99", "Vault 99");
        add("door.skin.hbm_m.vault_door.skin_81", "Vault 81");
        add("door.skin.hbm_m.vault_door.skin_111", "Vault 111");

        add(ModItems.SCREWDRIVER.get(), "Отвёртка");
        add("tooltip.hbm_m.screwdriver", "Клик ПКМ - настройка конвертера энергии или смена скина двери");
        add("tooltip.hbm_m.door_skin", "Используй отвёртку, чтобы сменить скин!");

        add("gui.hbm_m.battery.mode.both", "Режим: Приём и Передача");
        add("gui.hbm_m.battery.mode.both.desc", "Разрешены все операции с энергией.");
        add("gui.hbm_m.battery.mode.input", "Режим: Только Приём");
        add("gui.hbm_m.battery.mode.input.desc", "Разрешён только приём энергии.");
        add("gui.hbm_m.battery.mode.output", "Режим: Только Передача");
        add("gui.hbm_m.battery.mode.output.desc", "Разрешена только отдача энергии.");
        add("gui.hbm_m.battery.mode.locked", "Режим: Заблокировано");
        add("gui.hbm_m.battery.mode.locked.desc", "Все операции с энергией отключены.");

        add("gui.recipe.setRecipe", "Выбрать рецепт");
        add("gui.recipe.duration", "Время");
        add("gui.recipe.consumption", "Потребление");
        add("gui.recipe.input", "Вход");
        add("gui.recipe.output", "Выход");
        add("gui.hbm_m.fluid.empty", "Пусто");

        add("tooltip.hbm_m.battery.stored", "Хранится энергии:");
        add("tooltip.hbm_m.battery.transfer_rate", "Скорость зарядки: %1$s HE/t");
        add("tooltip.hbm_m.battery.discharge_rate", "Скорость разрядки: %1$s HE/t");

        add("tooltip.hbm_m.machine_battery.capacity", "Ёмкость: %1$s HE");
        add("tooltip.hbm_m.machine_battery.charge_speed", "Скорость зарядки: %1$s HE/т");
        add("tooltip.hbm_m.machine_battery.discharge_speed", "Скорость разрядки: %1$s HE/т");
        add("tooltip.hbm_m.machine_battery.stored", "Заряжено: %1$s / %2$s HE");
        add("tooltip.hbm_m.requires", "Требуется");


        add("trait.radioactive", "Радиоактивный");
        add("trait.asbestos", "Содержит асбест");
        add("trait.blinding", "Ослепление");
        add("trait.coal", "Угольная пыль");
        add("trait.digamma", "Дигамма-радиация");
        add("trait.explosive", "Воспламеняющийся / Взрывоопасный");
        add("trait.hot", "Пирофорный / Горячий");
        add("trait.hydro", "Гидрореактивный");
        add("trait.drop", "Опасно выкидывать");
        add("desc.item.wasteCooling", "Охладите в бочке с отработанным топливом");
        add("trait.rbmk.coreTemp", "Температура стержня: %s");
        add("trait.rbmk.depletion", "Обеднение: %s");
        add("trait.rbmk.diffusion", "Рассеивание: %s");
        add("trait.rbmk.fluxFunc", "Функция потока: %s");
        add("trait.rbmk.funcType", "Тип функции: %s");
        add("trait.rbmk.heat", "Тепло за тик при полной мощности: %s");
        add("trait.rbmk.melt", "Точка плавления: %s");
        add("trait.rbmk.neutron.any", "Любые нейтроны");
        add("trait.rbmk.neutron.fast", "Быстрые нейтроны");
        add("trait.rbmk.neutron.slow", "Медленные нейтроны");
        add("trait.rbmk.skinTemp", "Температура оболочки стержня: %s");
        add("trait.rbmk.source", "Самовоспламеняющийся");
        add("trait.rbmk.splitsInto", "Распадается на: %s");
        add("trait.rbmk.splitsWith", "Распадается с: %s");
        add("trait.rbmk.xenon", "Ксеноновое отравление: %s");
        add("trait.rbmk.xenonBurn", "Функция выгорания ксенона: %s");
        add("trait.rbmk.xenonGen", "Функция накопления ксенона: %s");
        add("sounds.hbm_m.subtitle.debris", "Обломки падают");
        add("hazard.hbm_m.radiation.format", "%s РАД/с");
        add("hazard.hbm_m.radiation.stack", "Стак: %s РАД/с");
        add("hazard.hbm_m.digamma", "[Дигамма]");
        add("hazard.hbm_m.digamma.format", "%s мДРХ/с");
        add("hazard.hbm_m.digamma.stack", "Стак: %s мДРХ/с");
        add("hazard.hbm_m.asbestos", "[Содержит асбест]");
        add("hazard.hbm_m.coal", "[Угольная пыль]");
        add("hazard.hbm_m.blinding", "[Ослепление]");
        add("hazard.hbm_m.hydro_reactive", "[Гидрореактивный]");
        add("hazard.hbm_m.explosive_on_fire", "[Воспламеняющийся / Взрывоопасный]");
        add("hazard.hbm_m.pyrophoric", "[Пирофорный / Горячий]");
        add("hazard.hbm_m.explosion_strength.format", " Сила взрыва - %s");
        add("hazard.hbm_m.stack", "Стак: %s");

        add("geiger.title", "СЧЁТЧИК ГЕЙГЕРА");
        add("geiger.title.dosimeter", "ДОЗИМЕТР");
        add("geiger.chunkRad", "Текущий уровень радиации в чанке:");
        add("geiger.envRad", "Общее радиационное заражение среды:");
        add("geiger.playerRad", "Уровень радиоактивного заражения игрока:");
        add("geiger.playerRes", "Защищённость игрока:");
        add("digamma.title", "ДИАГНОСТИКА ДИГАММЫ");
        add("digamma.playerDigamma", "Доза дигаммы:");
        add("digamma.playerHealth", "Влияние дигаммы:");
        add("digamma.playerRes", "Сопротивление к дигамме:");

        add("tooltip.hbm_m.abilities", "Способности:");
        add("tooltip.hbm_m.vein_miner", "Жилковый майнер (%s)");
        add("tooltip.hbm_m.aoe", "Зона действия %s");
        add("tooltip.hbm_m.silk_touch", "Шёлковое касание");
        add("tooltip.hbm_m.fortune", "Удача (%s)");
        add("tooltip.hbm_m.right_click", "ПКМ - переключить способность");
        add("tooltip.hbm_m.rbmk_fuel_drx", "Продвинутый ядерный топливный стержень с повышенной энергоотдачей");
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
        add("message.hbm_m.loaded", "Мир загружен с %s %s для Minecraft %s!");
        add("message.hbm_m.modernized", "Hbm's Nuclear Tech Mod: Modernized");
        add("message.hbm_m.new_version", "Доступна новая версия %s!");
        add("message.hbm_m.download_now", "Скачать: ");
        add("message.hbm_m.button_modrinth", "[Modrinth]");
        add("message.hbm_m.button_curseforge", "[CurseForge]");
        add("message.hbm_m.support_pitch", "Нравится мод? Поддержите команду:");
        add("message.hbm_m.button_boosty", "[Boosty]");
        add("message.hbm_m.button_crypto", "[Crypto]");

        add("item.hbm_m.meter.rads_over_limit", ">%s RAD/s");
        add("tooltip.hbm_m.hold_shift_for_details", "<Зажмите SHIFT для деталей>");
        
        add("sounds.hbm_m.geiger_counter", "Щелчки счетчика Гейгера");
        add("sounds.hbm_m.tool.techboop", "Пик счетчика Гейгера");
        add("sounds.hbm_m.thermal_vision_on", "Включение тепловизора");
        add("sounds.hbm_m.thermal_vision_off", "Выключение тепловизора");
        
        add("commands.hbm_m.rad.cleared", "Радиация очищена у %s игроков.");
        add("commands.hbm_m.rad.cleared.self", "Ваша радиация очищена.");
        add("commands.hbm_m.rad.added", "Добавлено %s радиации %s игрокам.");
        add("commands.hbm_m.rad.added.self", "Вам добавлено %s радиации.");
        add("commands.hbm_m.rad.removed", "Убрано %s радиации у %s игроков.");
        add("commands.hbm_m.rad.removed.self", "У вас убрано %s радиации.");
        add("commands.hbm_m.explosion.success", "Вызван ядерный взрыв: %s");
        add("commands.hbm_m.explosion.unknown_type", "Неизвестный тип взрыва");
        add("commands.hbm_m.explosion.not_server_level", "Эта команда должна выполняться на серверном уровне");
        add("commands.hbm_m.explosion.greedy_too_short", "Ожидаются опции key:value и три координаты (x y z)");
        add("commands.hbm_m.explosion.bad_coord_suffix", "Нужны все три координаты (x y z) или ни одной - тогда позиция источника");
        add("commands.hbm_m.explosion.bad_key_value", "Неверный токен key:value или координаты");
        add("commands.hbm_m.explosion.unsupported_key", "Этот тип взрыва не поддерживает опцию: %s");

        // СООБЩЕНИЯ О СМЕРТИ
        add("death.attack.radiation", "Игрок %s умер от лучевой болезни");
        add("death.attack.hardlanding_smash", "%1$s был раздавлен в лепешку %2$s");

        add("advancements.hbm_m.radiation_200.title", "Ура, Радиация!");
        add("advancements.hbm_m.radiation_200.description", "Достигнуть уровня радиации в 200 РАД");
        add("advancements.hbm_m.radiation_1000.title", "Ай, Радиация!");
        add("advancements.hbm_m.radiation_1000.description", "Умереть от лучевой болезни");

        add("chat.hbm_m.structure.obstructed", "Другие блоки мешают установке структуры!!");
        add("chat.hbm_m.chungus.on", "Турбина Левиафан: ВКЛ");
        add("chat.hbm_m.chungus.off", "Турбина Левиафан: ВЫКЛ");


        add("text.autoconfig.hbm_m.title", "Настройки радиации (HBM Modernized)");

        // ── Config GUI (config.hbm_m.*) — собственное меню на vanilla-виджетах ──
        add("config.hbm_m.title", "Настройки HBM Modernized");
        add("config.hbm_m.tab.client", "Клиент");
        add("config.hbm_m.tab.server", "Сервер");
        add("config.hbm_m.save", "Сохранить");
        add("config.hbm_m.apply", "Применить");
        add("config.hbm_m.restart.title", "Требуется перезапуск");
        add("config.hbm_m.restart.message", "Некоторые изменения вступят в силу только после перезапуска игры или перезагрузки ресурсов (F3+T). Применить?");
        add("config.hbm_m.reset", "Сбросить");
        add("config.hbm_m.reset.all", "Сбросить всё");
        add("config.hbm_m.reset.title", "Сбросить настройки?");
        add("config.hbm_m.reset.message", "Сбросить все значения текущей стороны к значениям по умолчанию? Изменения применятся после сохранения.");

        add("text.autoconfig.hbm_m.category.general", "Общие настройки");
        add("text.autoconfig.hbm_m.option.enableRadiation", "Включить радиацию");
        add("text.autoconfig.hbm_m.option.enableChunkRads", "Включить радиацию в чанках");
        add("text.autoconfig.hbm_m.option.enableMOTD", "Сообщение при входе в мир (MOTD)");
        add("text.autoconfig.hbm_m.option.usePrismSystem", "Использовать систему PRISM (иначе Simple, WIP)");

        add("text.autoconfig.hbm_m.category.world_effects", "Эффекты мира");
        add("text.autoconfig.hbm_m.option.enableRadFogEffect", "Радиоактивный туман в чанках");
        add("text.autoconfig.hbm_m.option.enableRadFogEffect.@Tooltip", "Частицы тумана при ≥100 RAD в чанке (1 из 20 тиков), как fogRad/fogCh в 1.7.10. Порог и частота заданы в коде.");
        add("text.autoconfig.hbm_m.option.worldRadEffects", "Эффекты радиации на мир (изменения блоков)");
        add("text.autoconfig.hbm_m.option.worldRadEffects.@Tooltip", "Как в 1.7.10 (RadiationConfig.worldRadEffects): замена травы/листьев при ≥10 RAD в чанке, 5 чанков/тик, 10 проходов. Пороги не настраиваются.");
        add("text.autoconfig.hbm_m.option.taintTrails","Следы заражения");
        add("text.autoconfig.hbm_m.option.taintTrails.@Tooltip","При заражении игрок оставляет следы заражения под собой.");

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

        add("text.autoconfig.hbm_m.option.radiationPixelEffect", "Экранные помехи от радиации");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.enableRadiationPixelEffect", "Экранный эффект радиационных помех");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelEffectThreshold", "Порог срабатывания эффекта");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelMaxIntensityRad", "Максимальная интенсивность эффекта");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelEffectMaxDots", "Макс. количество пикселей");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelEffectGreenChance", "Шанс зеленого пикселя");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelMinLifetime", "Мин. время жизни пикселя");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelMaxLifetime", "Макс. время жизни пикселя");
        add("text.autoconfig.hbm_m.option.obstructionHighlight", "Подсветка препятствий мультиблоков");
        add("text.autoconfig.hbm_m.option.obstructionHighlight.enableObstructionHighlight", "Включить подсветку препятствий");
        add("text.autoconfig.hbm_m.option.obstructionHighlight.enableObstructionHighlight.@Tooltip", "Если включено, блоки, мешающие размещению мультиблока, \nбудут подсвечиваться красной рамкой.");
        add("text.autoconfig.hbm_m.option.obstructionHighlight.obstructionHighlightDuration", "Длительность подсветки (сек)");
        add("text.autoconfig.hbm_m.option.obstructionHighlight.obstructionHighlightDuration.@Tooltip", "Время в секундах, в течение которого будет видна подсветка препятствий.");
        add("text.autoconfig.hbm_m.option.obstructionHighlight.obstructionHighlightAlpha", "Непрозрачность подсветки препятствий");
        add("text.autoconfig.hbm_m.option.obstructionHighlight.obstructionHighlightAlpha.@Tooltip", "Устанавливает непрозрачность заливки подсветки.\n0% = Невидимая, 100% = Непрозрачная.");

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
        add("text.autoconfig.hbm_m.option.modelStaticRenderDistance", "Дистанция для рендеринга статических частей .obj моделей");
        add("text.autoconfig.hbm_m.option.enableOcclusionCulling", "Включить куллинг моделей");
        add("text.autoconfig.hbm_m.option.useInstancedStaticRendering", "Батчинг частей obj моделей");
        add("text.autoconfig.hbm_m.option.useSlicedLight", "Sliced light (2×4×2 зонды)");
        add("text.autoconfig.hbm_m.option.useMultiDrawIndirect", "Multi-draw indirect (MDI)");

        add("text.autoconfig.hbm_m.option.vatsRenderDistanceChunks", "Дальность прорисовки VATS");

        add("text.autoconfig.hbm_m.category.debug", "Отладка");

        add("text.autoconfig.hbm_m.option.enableDebugRender", "Включить отладочный рендер радиации");
        add("text.autoconfig.hbm_m.option.debugRenderTextSize", "Размер текста отладочного рендера");
        add("text.autoconfig.hbm_m.option.debugRenderDistance", "Дальность отладочного рендеринга (чанки)");
        add("text.autoconfig.hbm_m.option.debugRenderInSurvival", "Показывать отладочный рендер в режиме выживания");
        add("text.autoconfig.hbm_m.option.enableDebugLogging", "Включить отладочные логи");

        add("text.autoconfig.hbm_m.option.enableRadiation.@Tooltip", "Если выключено, вся радиация отключается (чанки, предметы)");
        add("text.autoconfig.hbm_m.option.enableChunkRads.@Tooltip", "Если выключено, радиация в чанках всегда 0");
        add("text.autoconfig.hbm_m.option.enableMOTD.@Tooltip", "Приветствие при входе в мир и уведомление, если на Modrinth есть более новая версия мода");
        add("text.autoconfig.hbm_m.option.usePrismSystem.@Tooltip", "Использовать систему PRISM для радиации в чанках (WIP)");

        add("text.autoconfig.hbm_m.option.maxPlayerRad.@Tooltip", "Максимальная радиация, которую может накопить игрок");
        add("text.autoconfig.hbm_m.option.radDecay.@Tooltip", "Скорость распада радиации у игрока за тик");
        add("text.autoconfig.hbm_m.option.radDamage.@Tooltip", "Урон за тик при превышении порога");
        add("text.autoconfig.hbm_m.option.radDamageThreshold.@Tooltip", "Игрок начинает получать урон выше этого значения");
        add("text.autoconfig.hbm_m.option.radSickness.@Tooltip", "Порог для эффекта тошноты");
        add("text.autoconfig.hbm_m.option.radWater.@Tooltip", "Порог для негативного эффекта воды (WIP)");
        add("text.autoconfig.hbm_m.option.radConfusion.@Tooltip", "Порог для эффекта замешательства (WIP)");
        add("text.autoconfig.hbm_m.option.radBlindness.@Tooltip", "Порог для эффекта слепоты");

        add("text.autoconfig.hbm_m.option.radiationPixelEffect.enableRadiationPixelEffect.@Tooltip", "Включает/выключает эффект случайных мерцающих пикселей на экране, когда игрок подвергается радиационному облучению.");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelEffectThreshold.@Tooltip", "Минимальный уровень входящей радиации (в RAD/с), при котором начинает появляться эффект визуальных помех.");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelMaxIntensityRad.@Tooltip", "Уровень входящей радиации (в RAD/с), при котором эффект помех достигает своей максимальной силы (максимальное количество пикселей).");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelEffectMaxDots.@Tooltip", "Максимальное количество пикселей, которое может одновременно находиться на экране при пиковой интенсивности эффекта. Влияет на производительность на слабых системах.");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelEffectGreenChance.@Tooltip", "Вероятность (от 0.0 до 1.0), что новый появившийся пиксель будет зеленым, а не белым. Например, 0.1 = 10% шанс.");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelMinLifetime.@Tooltip", "Минимальное время (в тиках), которое один пиксель будет оставаться на экране. 20 тиков = 1 секунда.");
        add("text.autoconfig.hbm_m.option.radiationPixelEffect.radiationPixelMaxLifetime.@Tooltip", "Максимальное время (в тиках), которое один пиксель будет оставаться на экране. Для каждого пикселя выбирается случайное значение между минимальным и максимальным временем жизни.");
        add("text.autoconfig.hbm_m.option.infoToastOffsetX", "Положение подсказки: отступ слева");
        add("text.autoconfig.hbm_m.option.infoToastOffsetY", "Положение подсказки: отступ сверху");
        add("text.autoconfig.hbm_m.option.infoToastOffsetX.@Tooltip", "Расстояние от левого края экрана.");
        add("text.autoconfig.hbm_m.option.infoToastOffsetY.@Tooltip", "Расстояние от верхнего края экрана.");

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
        add("text.autoconfig.hbm_m.option.modelStaticRenderDistance.@Tooltip", "Дистанция для рендеринга статических частей .obj моделей (в чанках)");
        add("text.autoconfig.hbm_m.option.enableMissileNetworkTrack.@Tooltip", "Включите это, если хотите чтобы сервер отсылал всем клиентам пакеты с местоположением баллистических ракет, чтобы их было видно ЗА ванильной дальностью прорисовки.");
        add("text.autoconfig.hbm_m.option.enableOcclusionCulling.@Tooltip", "Включить куллинг моделей (выключите, если ваши модели рендерятся некорректно)");
        add("text.autoconfig.hbm_m.option.useInstancedStaticRendering.@Tooltip", "Использовать батчинговый рендер для частей obj. Сильно повышает производительность рендеринга в бесшейдерном режиме, при проблемах отключите.");
        add("text.autoconfig.hbm_m.option.useSlicedLight.@Tooltip", "16 зондов освещения вместо 8 углов — лучше на высоких башнях. Несовместимо с MDI: при включении части снова рисуются отдельными instanced draw. После смены — F3+T.");
        add("text.autoconfig.hbm_m.option.useMultiDrawIndirect.@Tooltip", "Один glMultiDrawElementsIndirect на кадр вместо многих instanced draw (без shader pack). Выигрыш заметен при большом числе одинаковых машин. Не работает с sliced light и GPU bone skinning.");

        add("text.autoconfig.hbm_m.option.vatsRenderDistanceChunks.@Tooltip", "Дальность отрисовки полосок здоровья мобов (чанки). Больше значение - дальше видно, но выше нагрузка.");

        add("text.autoconfig.hbm_m.option.enableDebugRender.@Tooltip", "Показывать отладочный оверлей радиации в чанках (F3)");
        add("text.autoconfig.hbm_m.option.debugRenderTextSize.@Tooltip", "Размер текста для отладочного оверлея");
        add("text.autoconfig.hbm_m.option.debugRenderDistance.@Tooltip", "Дальность отладочного рендеринга (чанки)");
        add("text.autoconfig.hbm_m.option.debugRenderInSurvival.@Tooltip", "Показывать отладочный рендер в режиме выживания");
        add("text.autoconfig.hbm_m.option.enableDebugLogging.@Tooltip", "Если выключено, будет активно глубокое логгирование игровых событий. Не стоит включать, если не испытываете проблем");

        // ── Недостающие категории и опции (weapons / machines / nukes / explosions / пробелы rendering) ──
        add("text.autoconfig.hbm_m.category.weapons", "Оружие");
        add("text.autoconfig.hbm_m.category.machines", "Станки и механизмы");
        add("text.autoconfig.hbm_m.category.nukes", "Ядерка");
        add("text.autoconfig.hbm_m.category.explosions", "Взрывчатка");

        add("text.autoconfig.hbm_m.option.dropSingularity", "Сингулярность при падении предмета");
        add("text.autoconfig.hbm_m.option.dropSingularity.@Tooltip", "Создаёт сингулярность/чёрную дыру при падении предмета-сингулярности (WeaponConfig.dropSing).");
        add("text.autoconfig.hbm_m.option.dropCell", "Антиматерия при падении ячейки");
        add("text.autoconfig.hbm_m.option.dropCell.@Tooltip", "Вызывает аннигиляционный взрыв при падении антиматериальной ячейки/пеллета (WeaponConfig.dropCell).");

        add("text.autoconfig.hbm_m.option.machineRadar", "Радар");
        add("text.autoconfig.hbm_m.option.machineRadar.generateChunks", "Генерировать чанки");
        add("text.autoconfig.hbm_m.option.machineRadar.generateChunks.@Tooltip", "Если включено, радар принудительно прогружает/генерирует сканируемые чанки.");
        add("text.autoconfig.hbm_m.option.frackingTower", "Фрекинговая вышка");
        add("text.autoconfig.hbm_m.option.frackingTower.maxPower", "Макс. энергия");
        add("text.autoconfig.hbm_m.option.frackingTower.maxPower.@Tooltip", "Максимальный запас энергии фрекинговой вышки.");
        add("text.autoconfig.hbm_m.option.frackingTower.consumption", "Потребление энергии");
        add("text.autoconfig.hbm_m.option.frackingTower.consumption.@Tooltip", "Энергия, потребляемая за одну операцию.");
        add("text.autoconfig.hbm_m.option.frackingTower.solutionRequired", "Требуется раствора");
        add("text.autoconfig.hbm_m.option.frackingTower.solutionRequired.@Tooltip", "Расход фрекингового раствора (мБ) за операцию.");
        add("text.autoconfig.hbm_m.option.frackingTower.delay", "Задержка операции");
        add("text.autoconfig.hbm_m.option.frackingTower.delay.@Tooltip", "Тиков между операциями фрекинга.");
        add("text.autoconfig.hbm_m.option.frackingTower.oilPerDeposit", "Нефть на месторождение");
        add("text.autoconfig.hbm_m.option.frackingTower.oilPerDeposit.@Tooltip", "Нефть (мБ) с обычного месторождения.");
        add("text.autoconfig.hbm_m.option.frackingTower.gasPerDepositMin", "Мин. газа на месторождение");
        add("text.autoconfig.hbm_m.option.frackingTower.gasPerDepositMin.@Tooltip", "Минимум газа (мБ) с обычного месторождения.");
        add("text.autoconfig.hbm_m.option.frackingTower.gasPerDepositMax", "Макс. газа на месторождение");
        add("text.autoconfig.hbm_m.option.frackingTower.gasPerDepositMax.@Tooltip", "Максимум газа (мБ) с обычного месторождения.");
        add("text.autoconfig.hbm_m.option.frackingTower.drainChance", "Шанс истощения");
        add("text.autoconfig.hbm_m.option.frackingTower.drainChance.@Tooltip", "Шанс (0–1), что операция истощает месторождение.");
        add("text.autoconfig.hbm_m.option.frackingTower.oilPerBedrockDeposit", "Нефть на бедрок-месторождение");
        add("text.autoconfig.hbm_m.option.frackingTower.oilPerBedrockDeposit.@Tooltip", "Нефть (мБ) с бедрок-месторождения.");
        add("text.autoconfig.hbm_m.option.frackingTower.gasPerBedrockDepositMin", "Мин. газа на бедрок-месторождение");
        add("text.autoconfig.hbm_m.option.frackingTower.gasPerBedrockDepositMin.@Tooltip", "Минимум газа (мБ) с бедрок-месторождения.");
        add("text.autoconfig.hbm_m.option.frackingTower.gasPerBedrockDepositMax", "Макс. газа на бедрок-месторождение");
        add("text.autoconfig.hbm_m.option.frackingTower.gasPerBedrockDepositMax.@Tooltip", "Максимум газа (мБ) с бедрок-месторождения.");
        add("text.autoconfig.hbm_m.option.frackingTower.destructionRange", "Радиус разрушения");
        add("text.autoconfig.hbm_m.option.frackingTower.destructionRange.@Tooltip", "Радиус взрыва при разрушении фрекинговой вышки.");

        add("text.autoconfig.hbm_m.option.gadgetRadius", "Радиус «Gadget»");
        add("text.autoconfig.hbm_m.option.gadgetRadius.@Tooltip", "Радиус взрыва «Gadget».");
        add("text.autoconfig.hbm_m.option.boyRadius", "Радиус «Little Boy»");
        add("text.autoconfig.hbm_m.option.boyRadius.@Tooltip", "Радиус взрыва «Little Boy».");
        add("text.autoconfig.hbm_m.option.manRadius", "Радиус «Fat Man»");
        add("text.autoconfig.hbm_m.option.manRadius.@Tooltip", "Радиус взрыва «Fat Man».");
        add("text.autoconfig.hbm_m.option.mikeRadius", "Радиус «Ivy Mike»");
        add("text.autoconfig.hbm_m.option.mikeRadius.@Tooltip", "Радиус взрыва «Ivy Mike».");
        add("text.autoconfig.hbm_m.option.tsarRadius", "Радиус «Царь-бомбы»");
        add("text.autoconfig.hbm_m.option.tsarRadius.@Tooltip", "Радиус взрыва «Царь-бомбы».");
        add("text.autoconfig.hbm_m.option.prototypeRadius", "Радиус «Prototype»");
        add("text.autoconfig.hbm_m.option.prototypeRadius.@Tooltip", "Радиус взрыва «Prototype».");
        add("text.autoconfig.hbm_m.option.fleijaRadius", "Радиус «F.L.E.I.J.A.»");
        add("text.autoconfig.hbm_m.option.fleijaRadius.@Tooltip", "Радиус взрыва «F.L.E.I.J.A.».");
        add("text.autoconfig.hbm_m.option.soliniumRadius", "Радиус «Solinium»");
        add("text.autoconfig.hbm_m.option.soliniumRadius.@Tooltip", "Радиус взрыва солиниевого заряда (blue rinse).");
        add("text.autoconfig.hbm_m.option.n2Radius", "Радиус N2-мины");
        add("text.autoconfig.hbm_m.option.n2Radius.@Tooltip", "Радиус взрыва N2-мины.");
        add("text.autoconfig.hbm_m.option.missileRadius", "Радиус ядерной ракеты");
        add("text.autoconfig.hbm_m.option.missileRadius.@Tooltip", "Радиус взрыва ядерной баллистической ракеты.");
        add("text.autoconfig.hbm_m.option.mirvRadius", "Радиус РГЧ (MIRV)");
        add("text.autoconfig.hbm_m.option.mirvRadius.@Tooltip", "Радиус взрыва боеголовки типа MIRV.");
        add("text.autoconfig.hbm_m.option.fatmanRadius", "Радиус гранатомёта «Fatman»");
        add("text.autoconfig.hbm_m.option.fatmanRadius.@Tooltip", "Радиус взрыва мини-ядерки гранатомёта «Fatman Launcher».");
        add("text.autoconfig.hbm_m.option.nukaRadius", "Радиус «Nuka»-гранаты");
        add("text.autoconfig.hbm_m.option.nukaRadius.@Tooltip", "Радиус взрыва «Nuka»-гранаты.");
        add("text.autoconfig.hbm_m.option.aSchrabRadius", "Радиус антишрабидия");
        add("text.autoconfig.hbm_m.option.aSchrabRadius.@Tooltip", "Радиус взрыва упавшего антишрабидия.");

        add("text.autoconfig.hbm_m.option.mk5TickTimeMs", "Время тика MK5 (мс)");
        add("text.autoconfig.hbm_m.option.mk5TickTimeMs.@Tooltip", "Минимум миллисекунд на тик для обработки чанков взрывом MK5.");
        add("text.autoconfig.hbm_m.option.blastSpeed", "Скорость взрыва");
        add("text.autoconfig.hbm_m.option.blastSpeed.@Tooltip", "Базовая скорость взрыва MK3/Tom (блоков/тик).");
        add("text.autoconfig.hbm_m.option.falloutRangePercent", "Радиус осадков (%)");
        add("text.autoconfig.hbm_m.option.falloutRangePercent.@Tooltip", "Радиус зоны радиоактивных осадков в % от базового радиуса взрыва.");
        add("text.autoconfig.hbm_m.option.falloutDelay", "Задержка осадков");
        add("text.autoconfig.hbm_m.option.falloutDelay.@Tooltip", "Сколько тиков ждать перед следующим расчётом чанка осадков.");
        add("text.autoconfig.hbm_m.option.enableChunkLoading", "Прогрузка чанков");
        add("text.autoconfig.hbm_m.option.enableChunkLoading.@Tooltip", "Разрешает процедурным взрывам удерживать центральный чанк загруженным и генерировать новые чанки.");
        add("text.autoconfig.hbm_m.option.explosionAlgorithm", "Алгоритм взрыва");
        add("text.autoconfig.hbm_m.option.explosionAlgorithm.@Tooltip", "0 = Legacy, 1 = Threaded DDA, 2 = Threaded DDA с накоплением урона.");
        add("text.autoconfig.hbm_m.option.enableCraterBiomes", "Биомы кратеров");
        add("text.autoconfig.hbm_m.option.enableCraterBiomes.@Tooltip", "Превращает кратер от ядерного взрыва в радиоактивные биомы кратеров.");
        add("text.autoconfig.hbm_m.option.craterBiomeInnerRad", "RAD/s внутреннего биома кратера");
        add("text.autoconfig.hbm_m.option.craterBiomeInnerRad.@Tooltip", "RAD/s для игрока внутри биома inner_crater.");
        add("text.autoconfig.hbm_m.option.craterBiomeRad", "RAD/s биома кратера");
        add("text.autoconfig.hbm_m.option.craterBiomeRad.@Tooltip", "RAD/s для игрока внутри биома crater.");
        add("text.autoconfig.hbm_m.option.craterBiomeOuterRad", "RAD/s внешнего биома кратера");
        add("text.autoconfig.hbm_m.option.craterBiomeOuterRad.@Tooltip", "RAD/s для игрока внутри биома outer_crater.");
        add("text.autoconfig.hbm_m.option.craterBiomeWaterMult", "Множитель осадков в воде");
        add("text.autoconfig.hbm_m.option.craterBiomeWaterMult.@Tooltip", "Множитель RAD/s в биомах кратера, когда игрок в воде или под дождём.");
        add("text.autoconfig.hbm_m.option.limitExplosionLifespan", "Лимит жизни взрыва");
        add("text.autoconfig.hbm_m.option.limitExplosionLifespan.@Tooltip", "Сколько секунд невыгруженный взрыв может просуществовать. 0 = без лимита.");

        // Rendering — недостающие опции трекинга ракет и instanced-рендера
        add("text.autoconfig.hbm_m.option.enableMissileNetworkTrack", "Отслеживать ракеты по сети");
        add("text.autoconfig.hbm_m.option.missileTrackMaxRangeBlocks", "Макс. дальность трекинга ракет (блоки)");
        add("text.autoconfig.hbm_m.option.missileTrackMaxRangeBlocks.@Tooltip", "Максимальное расстояние (в блоках), на котором ракеты синхронизируются по сети. 0 = без лимита.");
        add("text.autoconfig.hbm_m.option.missileTrackInterval", "Интервал трекинга ракет");
        add("text.autoconfig.hbm_m.option.missileTrackInterval.@Tooltip", "Как часто (в тиках) позиции ракет отправляются клиентам.");
        add("text.autoconfig.hbm_m.option.instanceVboOrphanBeforeUpload", "Orphaning instance VBO");
        add("text.autoconfig.hbm_m.option.instanceVboOrphanBeforeUpload.@Tooltip", "Перед заливкой instance VBO вызывать glBufferData(NULL) того же размера, чтобы драйвер не синхронизировался с предыдущим кадром. Отключайте только при проблемах.");
        add("text.autoconfig.hbm_m.option.gpuBoneSkinning", "GPU bone skinning");
        add("text.autoconfig.hbm_m.option.gpuBoneSkinning.@Tooltip", "Считать матрицы base×part на GPU для продвинутого сборщика (только vanilla; под Iris/Oculus — отдельный путь).");
        add("text.autoconfig.hbm_m.option.mdiDebugLogDispatch", "Лог MDI-диспетча");
        add("text.autoconfig.hbm_m.option.mdiDebugLogDispatch.@Tooltip", "Писать одну строку INFO на каждый MDI-dispatch (число sub-draw, инстансов, атлас).");
        add("text.autoconfig.hbm_m.option.mdiVerboseSubdraws", "Подробный лог MDI");
        add("text.autoconfig.hbm_m.option.mdiVerboseSubdraws.@Tooltip", "Доп. строка INFO на каждую MDI-команду (тег части, baseInstance и т.д.).");
        add("text.autoconfig.hbm_m.option.maxInstancedInstancesPerPart", "Макс. инстансов на часть");
        add("text.autoconfig.hbm_m.option.maxInstancedInstancesPerPart.@Tooltip", "Максимум инстансов на одну OBJ-часть для instanced-рендера. Большие поля машин требуют 4096+.");

        // FSB ARMOR TOOLTIPS
        add("tooltip.hbm_m.fsb_bonus", "Бонусы полного набора брони:");
        add("tooltip.hbm_m.fsb_resistances", "Сопротивления при полном наборе брони:");
        add("tooltip.hbm_m.res.fire", "Огню");
        add("tooltip.hbm_m.res.phys", "Физическому");
        add("tooltip.hbm_m.res.expl", "Взрывам");
        add("tooltip.hbm_m.res.fall", "Падению");
        add("tooltip.hbm_m.res.proj", "Лазерам");
        add("tooltip.hbm_m.res.other", "Прочему");
        add("armor.fsb.geigerCounter", "Звуковой счётчик Гейгера");
        add("armor.fsb.geigerHUD", "HUD счётчика Гейгера");
        add("armor.fsb.vats", "Детектор врагов");
        add("armor.fsb.thermalVision", "Тепловизор");
        add("armor.fsb.hardLanding", "Жёсткая посадка");
        add("armor.fsb.stepSize", "Шаг: %d");
        add("armor.fsb.dash", "Дополнительных рывков: %d");
    }
}
//?}



