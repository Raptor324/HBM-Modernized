package com.hbm_m.item.tags_and_tiers;

import com.hbm_m.main.MainRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    private static ResourceLocation modIdPath(String name) {
        //? if fabric && < 1.21.1 {
        return new ResourceLocation(MainRegistry.MOD_ID, name);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, name);
        *///?}
    }

    public static class Blocks {

        public static final TagKey<Block> NEEDS_ALLOY_TOOL = tag("needs_alloy_tool");

        public static final TagKey<Block> NEEDS_STEEL_TOOL = tag("needs_steel_tool");

        public static final TagKey<Block> NEEDS_STARMETAL_TOOL = tag("needs_starmetal_tool");

        public static final TagKey<Block> NEEDS_TITANIUM_TOOL = tag("needs_titanium_tool");

        public static final TagKey<Block> NON_OCCLUDING = tag("non_occluding");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, modIdPath(name));
        }
    }

    public static class Items {

        // --- ТЕГИ ДЛЯ СЛОТОВ БРОНИ ---
        public static final TagKey<Item> UPGRADE_MODULES = tag("upgrade_modules");
        public static final TagKey<Item> SLOT_HELMET_MODS = tag("mods/slot_helmet");
        public static final TagKey<Item> SLOT_CHESTPLATE_MODS = tag("mods/slot_chestplate");
        public static final TagKey<Item> SLOT_LEGGINGS_MODS = tag("mods/slot_leggings");
        public static final TagKey<Item> SLOT_BOOTS_MODS = tag("mods/slot_boots");
        public static final TagKey<Item> SLOT_SERVOS_MODS = tag("mods/slot_servos");
        public static final TagKey<Item> SLOT_CLADDING_MODS = tag("mods/slot_cladding");
        public static final TagKey<Item> SLOT_SPECIAL_MODS = tag("mods/slot_special");
        public static final TagKey<Item> SLOT_BATTERY_MODS = tag("mods/slot_battery");
        public static final TagKey<Item> SLOT_INSERT_MODS = tag("mods/slot_insert");

        // --- ТЕГИ ДЛЯ ТИПОВ БРОНИ ---
        public static final TagKey<Item> REQUIRES_HELMET = tag("mods/requires_helmet");
        public static final TagKey<Item> REQUIRES_CHESTPLATE = tag("mods/requires_chestplate");
        public static final TagKey<Item> REQUIRES_LEGGINGS = tag("mods/requires_leggings");
        public static final TagKey<Item> REQUIRES_BOOTS = tag("mods/requires_boots");

        // --- ПРОЧЕЕ ---
        public static final TagKey<Item> BLADES = tag("blades");
        public static final TagKey<Item> STAMPS_PLATE = tag("stamps/plate");
        public static final TagKey<Item> STAMPS_WIRE = tag("stamps/wire");
        public static final TagKey<Item> STAMPS_CIRCUIT = tag("stamps/circuit");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, modIdPath(name));
        }
    }
}
