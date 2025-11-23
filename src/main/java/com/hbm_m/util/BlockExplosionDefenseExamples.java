package com.hbm_m.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/**
 * ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ BlockExplosionDefense
 * С ПОЛНОЙ ТАБЛИЦЕЙ ВАНИЛЬНЫХ БЛОКОВ И ИХ УРОВНЕЙ ЗАЩИТЫ
 *
 * Этот класс демонстрирует различные способы использования системы защиты от взрывов
 * и содержит конвертацию всех ванильных блоков в нашу кастомную систему (0-15)
 */
public class BlockExplosionDefenseExamples {

    /**
     * ТАБЛИЦА: ВСЕ ВАНИЛЬНЫЕ БЛОКИ И ИХ УРОВНИ ЗАЩИТЫ (0-15)
     *
     * Структура: [Блок] → [Прочность] → [Уровень защиты]
     */
    public static void printAllVanillaBlocksWithDefenseLevel() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║           ТАБЛИЦА ВАНИЛЬНЫХ БЛОКОВ И ИХ УРОВНЕЙ ЗАЩИТЫ (0-15)                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        Map<String, VanillaBlockInfo> blockMap = getVanillaBlocksTable();

        // Сортируем по уровню защиты
        blockMap.values().stream()
                .sorted(Comparator.comparingInt(b -> b.defenseLevel))
                .forEach(block -> {
                    System.out.printf("├─ [Уровень %2d] %-40s (прочность: %6.2f) - %s\n",
                            block.defenseLevel,
                            block.blockName,
                            block.hardness,
                            block.category);
                });

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * ТАБЛИЦА: ВСЕ ВАНИЛЬНЫЕ БЛОКИ, СГРУППИРОВАННЫЕ ПО КАТЕГОРИЯМ
     */
    public static void printVanillaBlocksByCategory() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║        ВАНИЛЬНЫЕ БЛОКИ, СГРУППИРОВАННЫЕ ПО КАТЕГОРИЯМ И УРОВНЮ ЗАЩИТЫ                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        Map<String, VanillaBlockInfo> blockMap = getVanillaBlocksTable();
        Map<String, List<VanillaBlockInfo>> byCategory = new TreeMap<>();

        // Группируем по категориям
        blockMap.values().forEach(block ->
                byCategory.computeIfAbsent(block.category, k -> new ArrayList<>()).add(block)
        );

        // Выводим каждую категорию
        byCategory.forEach((category, blocks) -> {
            System.out.println("┌─ " + category.toUpperCase());
            System.out.println("│");

            blocks.stream()
                    .sorted(Comparator.comparingInt(b -> b.defenseLevel))
                    .forEach(block -> {
                        System.out.printf("│  ├─ [Уровень %2d] %-35s (прочность: %6.2f)\n",
                                block.defenseLevel,
                                block.blockName,
                                block.hardness);
                    });

            System.out.println("│");
        });

        System.out.println("═══════════════════════════════════════════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * ТАБЛИЦА: СТАТИСТИКА ПО УРОВНЯМ ЗАЩИТЫ
     */
    public static void printDefenseLevelStatistics() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    СТАТИСТИКА ПО УРОВНЯМ ЗАЩИТЫ                                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        Map<String, VanillaBlockInfo> blockMap = getVanillaBlocksTable();
        Map<Integer, Integer> countByLevel = new TreeMap<>();

        // Считаем блоки по уровням
        blockMap.values().forEach(block ->
                countByLevel.merge(block.defenseLevel, 1, Integer::sum)
        );

        System.out.println("Уровень защиты │ Кол-во блоков │ Поведение при взрыве");
        System.out.println("────────────────┼───────────────┼──────────────────────────────────────────────────");

        for (int level = 0; level <= 15; level++) {
            int count = countByLevel.getOrDefault(level, 0);
            String behavior = getDefenseLevelBehavior(level);
            System.out.printf("      %2d        │      %3d      │ %s\n", level, count, behavior);
        }

        System.out.println();
        System.out.printf("Всего блоков: %d\n", blockMap.size());
        System.out.println();
    }

    /**
     * ПОЛУЧИТЬ ТАБЛИЦУ ВСЕХ ВАНИЛЬНЫХ БЛОКОВ
     */
    private static Map<String, VanillaBlockInfo> getVanillaBlocksTable() {
        Map<String, VanillaBlockInfo> blocks = new LinkedHashMap<>();

        // УРОВЕНЬ 0 - Выпадают 100% (мягкие блоки, предметы)
        addBlock(blocks, "Лава", Blocks.LAVA, 0, "Растительность");
        addBlock(blocks, "Вода", Blocks.WATER, 0, "Растительность");
        addBlock(blocks, "Трава", Blocks.GRASS, 0, "Растительность");
        addBlock(blocks, "Высокая трава", Blocks.TALL_GRASS, 0, "Растительность");
        addBlock(blocks, "Морская трава", Blocks.SEAGRASS, 0, "Растительность");
        addBlock(blocks, "Высокая морская трава", Blocks.TALL_SEAGRASS, 0, "Растительность");
        addBlock(blocks, "Снег", Blocks.SNOW, 0, "Природные блоки");
        addBlock(blocks, "Цветок одуванчик", Blocks.DANDELION, 0, "Растительность");
        addBlock(blocks, "Цветок мак", Blocks.POPPY, 0, "Растительность");
        addBlock(blocks, "Голубая орхидея", Blocks.BLUE_ORCHID, 0, "Растительность");
        addBlock(blocks, "Красный тюльпан", Blocks.RED_TULIP, 0, "Растительность");
        addBlock(blocks, "Оранжевый тюльпан", Blocks.ORANGE_TULIP, 0, "Растительность");
        addBlock(blocks, "Белый тюльпан", Blocks.WHITE_TULIP, 0, "Растительность");
        addBlock(blocks, "Розовый тюльпан", Blocks.PINK_TULIP, 0, "Растительность");
        addBlock(blocks, "Лилия кувшинка", Blocks.LILY_OF_THE_VALLEY, 0, "Растительность");
        addBlock(blocks, "Люпин", Blocks.BLUE_ORCHID, 0, "Растительность");

        // УРОВЕНЬ 1 - Мягкие блоки
        addBlock(blocks, "Земля", Blocks.DIRT, 0.5F, "Природные блоки");
        addBlock(blocks, "Песок", Blocks.SAND, 0.5F, "Природные блоки");
        addBlock(blocks, "Красный песок", Blocks.RED_SAND, 0.5F, "Природные блоки");
        addBlock(blocks, "Гравий", Blocks.GRAVEL, 0.6F, "Природные блоки");
        addBlock(blocks, "Дубовые доски", Blocks.OAK_PLANKS, 2.0F, "Дерево");
        addBlock(blocks, "Еловые доски", Blocks.SPRUCE_PLANKS, 2.0F, "Дерево");
        addBlock(blocks, "Березовые доски", Blocks.BIRCH_PLANKS, 2.0F, "Дерево");
        addBlock(blocks, "Дубовое бревно", Blocks.OAK_LOG, 2.0F, "Дерево");
        addBlock(blocks, "Еловое бревно", Blocks.SPRUCE_LOG, 2.0F, "Дерево");
        addBlock(blocks, "Березовое бревно", Blocks.BIRCH_LOG, 2.0F, "Дерево");
        addBlock(blocks, "Листва дуба", Blocks.OAK_LEAVES, 0.2F, "Дерево");
        addBlock(blocks, "Листва ели", Blocks.SPRUCE_LEAVES, 0.2F, "Дерево");
        addBlock(blocks, "Листва березы", Blocks.BIRCH_LEAVES, 0.2F, "Дерево");
        addBlock(blocks, "Булыжник", Blocks.COBBLESTONE, 2.0F, "Природные блоки");
        addBlock(blocks, "Мох", Blocks.MOSS_BLOCK, 0.1F, "Растительность");

        // УРОВЕНЬ 2 - Слабый камень
        addBlock(blocks, "Камень", Blocks.STONE, 1.5F, "Камень");
        addBlock(blocks, "Гранит", Blocks.GRANITE, 1.5F, "Камень");
        addBlock(blocks, "Диорит", Blocks.DIORITE, 1.5F, "Камень");
        addBlock(blocks, "Андезит", Blocks.ANDESITE, 1.5F, "Камень");
        addBlock(blocks, "Песчаник", Blocks.SANDSTONE, 0.8F, "Камень");
        addBlock(blocks, "Красный песчаник", Blocks.RED_SANDSTONE, 0.8F, "Камень");
        addBlock(blocks, "Известняк", Blocks.CALCITE, 0.75F, "Камень");

        // УРОВЕНЬ 3 - Средний камень
        addBlock(blocks, "Каменный кирпич", Blocks.STONE_BRICKS, 1.5F, "Кирпич");
        addBlock(blocks, "Булыжник с трещинами", Blocks.CRACKED_STONE_BRICKS, 1.5F, "Кирпич");
        addBlock(blocks, "Мох булыжник", Blocks.MOSSY_COBBLESTONE, 2.0F, "Камень");
        addBlock(blocks, "Кирпич Нижнего мира", Blocks.NETHER_BRICKS, 0.4F, "Нижний мир");

        // УРОВЕНЬ 4 - Тяжелый камень
        addBlock(blocks, "Глубинный сланец", Blocks.DEEPSLATE, 3.0F, "Камень");
        addBlock(blocks, "Кирпич из глубинного сланца", Blocks.DEEPSLATE_BRICKS, 3.0F, "Кирпич");
        addBlock(blocks, "Облицовка из глубинного сланца", Blocks.DEEPSLATE_TILES, 3.0F, "Кирпич");
        addBlock(blocks, "Базальт", Blocks.BASALT, 1.25F, "Камень");
        addBlock(blocks, "Полированный базальт", Blocks.POLISHED_BASALT, 1.25F, "Камень");
        addBlock(blocks, "Блок угля", Blocks.COAL_BLOCK, 5.0F, "Блоки материалов");
        addBlock(blocks, "Блок сырого железа", Blocks.RAW_IRON_BLOCK, 5.0F, "Блоки материалов");
        addBlock(blocks, "Блок сырого меди", Blocks.RAW_COPPER_BLOCK, 5.0F, "Блоки материалов");
        addBlock(blocks, "Блок сырого золота", Blocks.RAW_GOLD_BLOCK, 5.0F, "Блоки материалов");

        // УРОВЕНЬ 5 - Железо и слабые руды
        addBlock(blocks, "Железная руда", Blocks.IRON_ORE, 3.0F, "Руды");
        addBlock(blocks, "Медная руда", Blocks.COPPER_ORE, 3.0F, "Руды");
        addBlock(blocks, "Золотая руда", Blocks.GOLD_ORE, 3.0F, "Руды");
        addBlock(blocks, "Угольная руда", Blocks.COAL_ORE, 3.0F, "Руды");
        addBlock(blocks, "Железный блок", Blocks.IRON_BLOCK, 5.0F, "Блоки материалов");
        addBlock(blocks, "Медный блок", Blocks.COPPER_BLOCK, 3.0F, "Блоки материалов");
        addBlock(blocks, "Блок окисленной меди", Blocks.OXIDIZED_COPPER, 3.0F, "Блоки материалов");
        addBlock(blocks, "Золотой блок", Blocks.GOLD_BLOCK, 3.0F, "Блоки материалов");
        addBlock(blocks, "Блок изумруда", Blocks.EMERALD_BLOCK, 5.0F, "Блоки материалов");
        addBlock(blocks, "Блок красного камня", Blocks.REDSTONE_BLOCK, 5.0F, "Блоки материалов");
        addBlock(blocks, "Лазулитовый блок", Blocks.LAPIS_BLOCK, 3.0F, "Блоки материалов");
        addBlock(blocks, "Блок аметиста", Blocks.AMETHYST_BLOCK, 1.5F, "Камень");

        // УРОВЕНЬ 6 - Средние руды
        addBlock(blocks, "Изумрудная руда", Blocks.EMERALD_ORE, 3.0F, "Руды");
        addBlock(blocks, "Руда красного камня", Blocks.REDSTONE_ORE, 3.0F, "Руды");
        addBlock(blocks, "Лазулитовая руда", Blocks.LAPIS_ORE, 3.0F, "Руды");
        addBlock(blocks, "Руда древнего обломка", Blocks.ANCIENT_DEBRIS, 30.0F, "Руды");
        addBlock(blocks, "Глубинный сланец железная руда", Blocks.DEEPSLATE_IRON_ORE, 4.5F, "Руды");
        addBlock(blocks, "Глубинный сланец медная руда", Blocks.DEEPSLATE_COPPER_ORE, 4.5F, "Руды");
        addBlock(blocks, "Глубинный сланец золотая руда", Blocks.DEEPSLATE_GOLD_ORE, 4.5F, "Руды");
        addBlock(blocks, "Глубинный сланец алмазная руда", Blocks.DEEPSLATE_DIAMOND_ORE, 4.5F, "Руды");

        // УРОВЕНЬ 8 - Алмаз и обсидиан
        addBlock(blocks, "Алмазная руда", Blocks.DIAMOND_ORE, 3.0F, "Руды");
        addBlock(blocks, "Алмазный блок", Blocks.DIAMOND_BLOCK, 5.0F, "Блоки материалов");
        addBlock(blocks, "Обсидиан", Blocks.OBSIDIAN, 50.0F, "Камень");
        addBlock(blocks, "Плач обсидиана", Blocks.CRYING_OBSIDIAN, 50.0F, "Нижний мир");
        addBlock(blocks, "Пурпур", Blocks.PURPUR_BLOCK, 1.5F, "Край");
        addBlock(blocks, "Конец стержня", Blocks.END_ROD, 0.0F, "Край");

        // УРОВЕНЬ 10 - Древний обсидиан и сложный камень
        addBlock(blocks, "Блок незерита", Blocks.NETHERITE_BLOCK, 250.0F, "Блоки материалов");
        addBlock(blocks, "Душевой песок", Blocks.SOUL_SAND, 0.5F, "Нижний мир");
        addBlock(blocks, "Магматический блок", Blocks.MAGMA_BLOCK, 0.5F, "Нижний мир");
        addBlock(blocks, "Блок черного камня", Blocks.BLACKSTONE, 1.5F, "Нижний мир");
        addBlock(blocks, "Позолоченный блокс", Blocks.GILDED_BLACKSTONE, 1.5F, "Нижний мир");
        addBlock(blocks, "Пурпуровый блок", Blocks.PURPUR_PILLAR, 1.5F, "Край");

        // УРОВЕНЬ 12 - Специальные структурные блоки
        addBlock(blocks, "Коренная порода", Blocks.BEDROCK, -1.0F, "Специальные");
        addBlock(blocks, "Конечная порода", Blocks.END_PORTAL_FRAME, -1.0F, "Край");
        addBlock(blocks, "Рамка портала Нижнего мира", Blocks.NETHER_PORTAL, -1.0F, "Нижний мир");

        // УРОВЕНЬ 14 - Очень специальные блоки
        addBlock(blocks, "Яйцо дракона", Blocks.DRAGON_EGG, -1.0F, "Край");
        addBlock(blocks, "Командный блок", Blocks.COMMAND_BLOCK, -1.0F, "Специальные");
        addBlock(blocks, "Цепь командных блоков", Blocks.CHAIN_COMMAND_BLOCK, -1.0F, "Специальные");
        addBlock(blocks, "Повторяющийся командный блок", Blocks.REPEATING_COMMAND_BLOCK, -1.0F, "Специальные");
        addBlock(blocks, "Блок структуры", Blocks.STRUCTURE_BLOCK, -1.0F, "Специальные");
        addBlock(blocks, "Синий лед", Blocks.BLUE_ICE, 2.8F, "Природные блоки");
        addBlock(blocks, "Упакованный лед", Blocks.PACKED_ICE, 0.5F, "Природные блоки");

        // УРОВЕНЬ 15 - Неразрушимые (bedrock-подобные)
        addBlock(blocks, "Барьер", Blocks.BARRIER, -1.0F, "Специальные");
        addBlock(blocks, "Светлый блок", Blocks.LIGHT, 0.0F, "Специальные");

        return blocks;
    }

    /**
     * Добавить блок в таблицу
     */
    private static void addBlock(Map<String, VanillaBlockInfo> map, String name, net.minecraft.world.level.block.Block block, float hardness, String category) {
        BlockState state = block.defaultBlockState();
        int defenseLevel = calculateDefenseLevel(hardness);
        map.put(name, new VanillaBlockInfo(name, hardness, defenseLevel, category));
    }

    /**
     * Конвертировать прочность блока в уровень защиты (0-15)
     */
    private static int calculateDefenseLevel(float hardness) {
        if (hardness < 0) return 15; // Bedrock-подобные блоки
        if (hardness < 0.5F) return 0;  // Мягкие блоки
        if (hardness < 1.0F) return 1;  // Очень мягкие
        if (hardness < 1.5F) return 2;  // Мягкий камень
        if (hardness < 3.0F) return 3;  // Слабый камень
        if (hardness < 5.0F) return 4;  // Средний камень
        if (hardness < 8.0F) return 5;  // Средний материал
        if (hardness < 10.0F) return 6; // Тяжелый материал
        if (hardness < 15.0F) return 7; // Очень тяжелый
        if (hardness < 20.0F) return 8; // Очень тяжелый+
        if (hardness < 30.0F) return 9; // Экстремально тяжелый
        if (hardness < 50.0F) return 10; // Супер тяжелый
        if (hardness < 100.0F) return 12; // Практически неразрушимый
        if (hardness < 250.0F) return 13; // Почти неразрушимый
        if (hardness < 1000.0F) return 14; // Крайне редко разрушимый
        return 15; // Полностью неразрушимый
    }

    /**
     * Получить описание поведения уровня защиты
     */
    private static String getDefenseLevelBehavior(int level) {
        return switch (level) {
            case 0 -> "Разрушить 100% (всегда выпадает)";
            case 1, 2, 3, 4, 5, 6, 7 -> "Разрушить 100% (близко) / Селлафит/ничего (далеко)";
            case 8, 9, 10 -> "Селлафит 100% (далеко) / Редко разрушается (близко)";
            case 11 -> "Селлафит 30%, Ничего 70% (далеко)";
            case 12 -> "Селлафит 15%, Ничего 85% (далеко)";
            case 13 -> "Селлафит 5%, Ничего 95% (далеко)";
            case 14 -> "Селлафит 1%, Ничего 99% (далеко)";
            case 15 -> "Ничего (полностью неразрушимо)";
            default -> "Неизвестно";
        };
    }

    /**
     * ПРИМЕР 1: Проверка уровня защиты блока
     */
    public static void exampleCheckDefenseLevel(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        int defenseLevel = BlockExplosionDefense.getExplosionDefenseLevel(state, level, pos);

        System.out.println("Блок " + state.getBlock().getName().getString() +
                " имеет уровень защиты: " + defenseLevel);
    }

    /**
     * ПРИМЕР 2: Простая проверка разрушится ли блок при взрыве
     */
    public static void exampleCheckBlockDamage(ServerLevel level, BlockPos blockPos, BlockPos centerPos, float radius) {
        RandomSource random = level.random;

        BlockExplosionDefense.ExplosionDefenseResult result =
                BlockExplosionDefense.calculateExplosionDamage(
                        level, blockPos, centerPos, radius, random
                );

        if (result.shouldBreak) {
            if (result.replaceWithSellafit) {
                System.out.println("Блок будет ЗАМЕНЕН на селлафит");
            } else {
                System.out.println("Блок будет УДАЛЕН");
            }
        } else {
            System.out.println("Блок останется НЕТРОНУТЫМ");
        }
    }

    /**
     * ПРИМЕР 3: Проверка нескольких блоков в радиусе
     */
    public static void exampleCheckMultipleBlocks(ServerLevel level, BlockPos centerPos, int radius) {
        int checkRadius = radius + 50;
        RandomSource random = level.random;

        for (int x = -checkRadius; x <= checkRadius; x++) {
            for (int z = -checkRadius; z <= checkRadius; z++) {
                BlockPos checkPos = centerPos.offset(x, 0, z);

                BlockExplosionDefense.ExplosionDefenseResult result =
                        BlockExplosionDefense.calculateExplosionDamage(
                                level, checkPos, centerPos, radius, random
                        );

                if (result.shouldBreak) {
                    System.out.println("Блок на " + checkPos + " будет разрушен");
                }
            }
        }
    }

    /**
     * ПРИМЕР 4: Использование альтернативной системы с базовыми вероятностями
     */
    public static void exampleBaseProbabilitySystem(ServerLevel level, BlockPos blockPos, BlockPos centerPos, float radius) {
        RandomSource random = level.random;

        // Вместо кольцевой системы используем базовые вероятности
        BlockExplosionDefense.ExplosionDefenseResult result =
                BlockExplosionDefense.calculateExplosionDamageWithBaseProbability(
                        level, blockPos, centerPos, radius, random
                );

        if (result.shouldBreak) {
            if (result.replaceWithSellafit) {
                System.out.println("Базовая система: блок становится селлафитом");
            } else {
                System.out.println("Базовая система: блок удаляется");
            }
        }
    }

    /**
     * ПРИМЕР 5: Тестирование специфических уровней защиты
     */
    public static void exampleTestDefenseLevels(ServerLevel level, BlockPos testPos) {
        // Создаем тестовые блоки и проверяем их уровни
        BlockState[] testBlocks = {
                Blocks.DIAMOND_BLOCK.defaultBlockState(),  // Должен быть 8-9
                Blocks.OBSIDIAN.defaultBlockState(),        // Должен быть 8-9
                Blocks.BEDROCK.defaultBlockState(),         // Должен быть 15
                Blocks.DIRT.defaultBlockState(),            // Должен быть 0-1
                Blocks.STONE.defaultBlockState(),           // Должен быть 2-3
                Blocks.OAK_LOG.defaultBlockState(),         // Должен быть 1-2
        };

        System.out.println("=== ТЕСТ УРОВНЕЙ ЗАЩИТЫ ===");
        for (BlockState block : testBlocks) {
            int defenseLevel = BlockExplosionDefense.getExplosionDefenseLevel(block, level, testPos);
            System.out.println(block.getBlock().getName().getString() + ": уровень " + defenseLevel);
        }
    }

    /**
     * ПРИМЕР 6: Имитация взрыва с проверкой всех блоков в радиусе
     */
    public static void exampleSimulateExplosion(ServerLevel level, BlockPos centerPos, int radius) {
        RandomSource random = level.random;
        float maxRadius = radius * 1.5F; // Используем тот же STRETCH_FACTOR как в CraterGenerator

        int destroyedCount = 0;
        int selafitCount = 0;
        int survivedCount = 0;

        System.out.println("=== ИМИТАЦИЯ ВЗРЫВА ===");
        System.out.println("Центр: " + centerPos + ", Радиус: " + radius);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);

                    // Проверяем находится ли блок в зоне действия взрыва (примерно)
                    double distance = Math.sqrt(x*x + z*z + y*y);
                    if (distance > maxRadius) continue;

                    BlockExplosionDefense.ExplosionDefenseResult result =
                            BlockExplosionDefense.calculateExplosionDamage(
                                    level, checkPos, centerPos, maxRadius, random
                            );

                    if (result.shouldBreak) {
                        if (result.replaceWithSellafit) {
                            selafitCount++;
                        } else {
                            destroyedCount++;
                        }
                    } else {
                        survivedCount++;
                    }
                }
            }
        }

        System.out.println("Результаты:");
        System.out.println("  Удалено: " + destroyedCount);
        System.out.println("  Селлафит: " + selafitCount);
        System.out.println("  Выжило: " + survivedCount);
    }

    /**
     * ПРИМЕР 7: Анализ поведения блока по кольцам
     */
    public static void exampleAnalyzeByRings(ServerLevel level, BlockState testBlockState, BlockPos centerPos, float maxRadius) {
        RandomSource random = level.random;
        int ringCount = 6;

        System.out.println("=== АНАЛИЗ ПО КОЛЬЦАМ ===");
        System.out.println("Блок: " + testBlockState.getBlock().getName().getString());
        int defenseLevel = BlockExplosionDefense.getExplosionDefenseLevel(testBlockState, level, centerPos);
        System.out.println("Уровень защиты: " + defenseLevel);
        System.out.println();

        for (int ring = 0; ring < ringCount; ring++) {
            // Симулируем позицию в этом кольце
            double distance = (maxRadius / ringCount) * (ring + 0.5);
            int testX = (int) distance;
            BlockPos testPos = centerPos.offset(testX, 0, 0);

            int destroyCount = 0;
            int selafitCount = 0;

            // Повторяем несколько раз для статистики
            for (int i = 0; i < 100; i++) {
                BlockExplosionDefense.ExplosionDefenseResult result =
                        BlockExplosionDefense.calculateExplosionDamage(
                                level, testPos, centerPos, maxRadius, random
                        );

                if (result.shouldBreak) {
                    if (result.replaceWithSellafit) {
                        selafitCount++;
                    } else {
                        destroyCount++;
                    }
                }
            }

            System.out.printf("Кольцо %d (расстояние ~%.0f): Удалено %d%%, Селлафит %d%%\n",
                    ring, distance, destroyCount, selafitCount);
        }
    }

    /**
     * ПРИМЕР 8: Таблица поведения всех уровней защиты
     */
    public static void examplePrintDefenseLevelTable() {
        System.out.println("=== ТАБЛИЦА УРОВНЕЙ ЗАЩИТЫ ===");
        System.out.println();
        System.out.println("КОЛЬЦА 0-3 (близко к центру, 0-66%):");
        System.out.println("  Уровень 0:      → Разрушить 100%");
        System.out.println("  Уровень 1-10:   → Разрушить 100%");
        System.out.println("  Уровень 11-14:  → Селлафит 100%");
        System.out.println("  Уровень 15:     → Ничего (неразрушимо)");
        System.out.println();
        System.out.println("КОЛЬЦА 4-5 (далеко от центра, 66-100%):");
        System.out.println("  Уровень 0:      → Разрушить 100%");
        System.out.println("  Уровень 1-5:    → Разрушить 100%");
        System.out.println("  Уровень 6-10:   → Селлафит 100%");
        System.out.println("  Уровень 11:     → Селлафит 30%, Ничего 70%");
        System.out.println("  Уровень 12:     → Селлафит 15%, Ничего 85%");
        System.out.println("  Уровень 13:     → Селлафит 5%, Ничего 95%");
        System.out.println("  Уровень 14:     → Селлафит 1%, Ничего 99%");
        System.out.println("  Уровень 15:     → Ничего (неразрушимо)");
    }

    /**
     * ПРИМЕР 9: Определение уровня защиты для кастомного блока
     */
    public static void exampleCustomBlockDefenseLevel() {
        System.out.println("=== ОПРЕДЕЛЕНИЕ УРОВНЯ ЗАЩИТЫ ===");
        System.out.println();
        System.out.println("Стандартное преобразование прочности → уровень защиты:");
        System.out.println("  hardness < 0.5: → Уровень 0 (Мягкие блоки)");
        System.out.println("  hardness < 1.0: → Уровень 1 (Очень мягкие)");
        System.out.println("  hardness < 1.5: → Уровень 2 (Мягкий камень)");
        System.out.println("  hardness < 3.0: → Уровень 3 (Слабый камень)");
        System.out.println("  hardness < 5.0: → Уровень 4 (Средний камень)");
        System.out.println("  hardness < 8.0: → Уровень 5 (Средний материал)");
        System.out.println("  hardness < 10.0: → Уровень 6 (Тяжелый материал)");
        System.out.println("  hardness < 15.0: → Уровень 7 (Очень тяжелый)");
        System.out.println("  hardness < 20.0: → Уровень 8 (Очень тяжелый+)");
        System.out.println("  hardness < 30.0: → Уровень 9 (Экстремально тяжелый)");
        System.out.println("  hardness < 50.0: → Уровень 10 (Супер тяжелый)");
        System.out.println("  hardness < 100.0: → Уровень 12 (Практически неразрушимый)");
        System.out.println("  hardness < 250.0: → Уровень 13 (Почти неразрушимый)");
        System.out.println("  hardness < 1000.0: → Уровень 14 (Крайне редко разрушимый)");
        System.out.println("  hardness < 0: → Уровень 15 (Bedrock, полностью неразрушимый)");
        System.out.println();
        System.out.println("Вы можете переопределить это в BlockExplosionDefense");
        System.out.println("для кастомных материалов вашего мода!");
    }

    /**
     * ПРИМЕР 10: Интеграция с системой выпадения селлафита
     */
    public static void exampleIntegrationWithSellafit(ServerLevel level, BlockPos blockPos, BlockPos centerPos, float radius) {
        RandomSource random = level.random;

        BlockExplosionDefense.ExplosionDefenseResult result =
                BlockExplosionDefense.calculateExplosionDamage(
                        level, blockPos, centerPos, radius, random
                );

        if (result.shouldBreak) {
            if (result.replaceWithSellafit) {
                // Используйте ваши блоки селлафита
                // fallingBlocks[randomIndex].defaultBlockState()
                System.out.println("Создать селлафит в позиции: " + blockPos);
            } else {
                // Просто удалите блок
                level.removeBlock(blockPos, false);
                System.out.println("Удалить блок в позиции: " + blockPos);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════════

    /**
     * ВНУТРЕННИЙ КЛАСС: Информация о ванильном блоке
     */
    private static class VanillaBlockInfo {
        String blockName;
        float hardness;
        int defenseLevel;
        String category;

        VanillaBlockInfo(String blockName, float hardness, int defenseLevel, String category) {
            this.blockName = blockName;
            this.hardness = hardness;
            this.defenseLevel = defenseLevel;
            this.category = category;
        }
    }
}