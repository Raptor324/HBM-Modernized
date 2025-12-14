package com.hbm_m.item.tags_and_tiers;
import com.hbm_m.main.MainRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {

        public static final TagKey<Block> NEEDS_ALLOY_TOOL = tag("needs_alloy_tool");

        public static final TagKey<Block> NEEDS_STEEL_TOOL = tag("needs_steel_tool");

        public static final TagKey<Block> NEEDS_STARMETAL_TOOL = tag("needs_starmetal_tool");

        public static final TagKey<Block> NEEDS_TITANIUM_TOOL = tag("needs_steel_tool");

        public static final TagKey<Block> NON_OCCLUDING = create("non_occluding");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, name));
        }

        private static TagKey<Block> create(String name) {
            return TagKey.create(Registries.BLOCK, 
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, name));
        }
    }

    public static class Items {

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, name));
        }
    }
}
