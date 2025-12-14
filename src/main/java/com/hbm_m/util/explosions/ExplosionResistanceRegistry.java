package com.hbm_m.util.explosions;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/**
 * Система регистрации кастомной прочности блоков для взрывов
 * Прочность от 0 до 15:
 * 0-4: низкая прочность (легко уничтожаются)
 * 5-9: средняя прочность (могут защищать более слабые блоки)
 * 10-14: высокая прочность (редко уничтожаются, защищают соседние блоки)
 * 15: неуничтожаемые (как бедрок)
 */
public class ExplosionResistanceRegistry {

    private static final Map<Block, Integer> BLOCK_RESISTANCE = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Инициализирует реестр стандартной прочности для блоков
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // Неуничтожаемые блоки (прочность 15)
        registerBlock(Blocks.BEDROCK, 15);
        registerBlock(Blocks.ANCIENT_DEBRIS, 15);
        registerBlock(Blocks.OBSIDIAN, 15);
        registerBlock(Blocks.CRYING_OBSIDIAN, 15);
        registerBlock(Blocks.REINFORCED_DEEPSLATE, 15);

        // Высокая прочность (10-14)
        registerBlock(Blocks.DEEPSLATE, 14);
        registerBlock(Blocks.DARK_PRISMARINE, 14);
        registerBlock(Blocks.PRISMARINE, 13);
        registerBlock(Blocks.PURPUR_BLOCK, 13);
        registerBlock(Blocks.BLACKSTONE, 12);
        registerBlock(Blocks.BASALT, 12);
        registerBlock(Blocks.STONE, 12);
        registerBlock(Blocks.GRANITE, 12);
        registerBlock(Blocks.DIORITE, 12);
        registerBlock(Blocks.ANDESITE, 12);
        registerBlock(Blocks.POLISHED_BLACKSTONE, 11);
        registerBlock(Blocks.IRON_ORE, 11);
        registerBlock(Blocks.DEEPSLATE_IRON_ORE, 11);
        registerBlock(Blocks.DIAMOND_ORE, 11);
        registerBlock(Blocks.DEEPSLATE_DIAMOND_ORE, 11);
        registerBlock(Blocks.EMERALD_ORE, 11);
        registerBlock(Blocks.DEEPSLATE_EMERALD_ORE, 11);
        registerBlock(Blocks.LAPIS_ORE, 10);
        registerBlock(Blocks.DEEPSLATE_LAPIS_ORE, 10);
        registerBlock(Blocks.REDSTONE_ORE, 10);
        registerBlock(Blocks.DEEPSLATE_REDSTONE_ORE, 10);
        registerBlock(Blocks.GOLD_ORE, 10);
        registerBlock(Blocks.DEEPSLATE_GOLD_ORE, 10);
        registerBlock(Blocks.COPPER_ORE, 10);
        registerBlock(Blocks.DEEPSLATE_COPPER_ORE, 10);
        registerBlock(Blocks.COAL_ORE, 10);
        registerBlock(Blocks.DEEPSLATE_COAL_ORE, 10);

        // Средняя прочность (5-9)
        registerBlock(Blocks.COBBLESTONE, 9);
        registerBlock(Blocks.MOSSY_COBBLESTONE, 9);
        registerBlock(Blocks.COBBLED_DEEPSLATE, 9);
        registerBlock(Blocks.BRICKS, 8);
        registerBlock(Blocks.NETHER_BRICKS, 8);
        registerBlock(Blocks.RED_NETHER_BRICKS, 8);
        registerBlock(Blocks.TERRACOTTA, 8);
        registerBlock(Blocks.NETHERRACK, 7);
        registerBlock(Blocks.END_STONE, 7);
        registerBlock(Blocks.SANDSTONE, 7);
        registerBlock(Blocks.RED_SANDSTONE, 7);
        registerBlock(Blocks.GRAVEL, 6);
        registerBlock(Blocks.DIRT, 6);
        registerBlock(Blocks.ROOTED_DIRT, 6);
        registerBlock(Blocks.MUD, 6);
        registerBlock(Blocks.SAND, 5);
        registerBlock(Blocks.RED_SAND, 5);
        registerBlock(Blocks.SOUL_SAND, 5);

        // Низкая прочность (0-4)
        registerBlock(Blocks.OAK_LOG, 4);
        registerBlock(Blocks.BIRCH_LOG, 4);
        registerBlock(Blocks.SPRUCE_LOG, 4);
        registerBlock(Blocks.JUNGLE_LOG, 4);
        registerBlock(Blocks.ACACIA_LOG, 4);
        registerBlock(Blocks.DARK_OAK_LOG, 4);
        registerBlock(Blocks.MANGROVE_LOG, 4);
        registerBlock(Blocks.OAK_PLANKS, 3);
        registerBlock(Blocks.BIRCH_PLANKS, 3);
        registerBlock(Blocks.SPRUCE_PLANKS, 3);
        registerBlock(Blocks.JUNGLE_PLANKS, 3);
        registerBlock(Blocks.ACACIA_PLANKS, 3);
        registerBlock(Blocks.DARK_OAK_PLANKS, 3);
        registerBlock(Blocks.MANGROVE_PLANKS, 3);
        registerBlock(Blocks.GLASS, 2);
        registerBlock(Blocks.WHITE_CONCRETE, 1);
        registerBlock(Blocks.ORANGE_CONCRETE, 1);
        registerBlock(Blocks.MAGENTA_CONCRETE, 1);
        registerBlock(Blocks.LIGHT_BLUE_CONCRETE, 1);
        registerBlock(Blocks.YELLOW_CONCRETE, 1);
        registerBlock(Blocks.LIME_CONCRETE, 1);
        registerBlock(Blocks.PINK_CONCRETE, 1);
        registerBlock(Blocks.GRAY_CONCRETE, 1);
        registerBlock(Blocks.LIGHT_GRAY_CONCRETE, 1);
        registerBlock(Blocks.CYAN_CONCRETE, 1);
        registerBlock(Blocks.PURPLE_CONCRETE, 1);
        registerBlock(Blocks.BLUE_CONCRETE, 1);
        registerBlock(Blocks.BROWN_CONCRETE, 1);
        registerBlock(Blocks.GREEN_CONCRETE, 1);
        registerBlock(Blocks.RED_CONCRETE, 1);
        registerBlock(Blocks.BLACK_CONCRETE, 1);
        registerBlock(Blocks.GRASS_BLOCK, 1);
        registerBlock(Blocks.PODZOL, 1);
        registerBlock(Blocks.MYCELIUM, 1);
        registerBlock(Blocks.TALL_GRASS, 0);
        registerBlock(Blocks.SNOW, 0);
        registerBlock(Blocks.TALL_SEAGRASS, 0);
        registerBlock(Blocks.SEAGRASS, 0);
    }

    /**
     * Регистрирует прочность блока
     */
    public static void registerBlock(Block block, int resistance) {
        if (resistance < 0 || resistance > 15) {
            throw new IllegalArgumentException("Прочность должна быть от 0 до 15, получено: " + resistance);
        }
        BLOCK_RESISTANCE.put(block, resistance);
    }

    /**
     * Получает прочность блока (0-15)
     */
    public static int getResistance(Block block) {
        return BLOCK_RESISTANCE.getOrDefault(block, 7); // По умолчанию средняя прочность
    }

    /**
     * Проверяет, неуничтожаем ли блок
     */
    public static boolean isIndestructible(Block block) {
        return getResistance(block) >= 15;
    }

    /**
     * Проверяет, имеет ли блок среднюю прочность (может защищать более слабые блоки)
     */
    public static boolean isShieldBlock(Block block) {
        int resistance = getResistance(block);
        return resistance >= 6 && resistance <= 10;
    }

    /**
     * Проверяет, имеет ли блок высокую прочность (редко уничтожается)
     */
    public static boolean isStrongBlock(Block block) {
        int resistance = getResistance(block);
        return resistance >= 10 && resistance < 15;
    }

    /**
     * Получить все зарегистрированные блоки
     */
    public static Map<Block, Integer> getAllBlocks() {
        return new HashMap<>(BLOCK_RESISTANCE);
    }
}