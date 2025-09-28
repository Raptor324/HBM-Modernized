package com.hbm_m.block;
// Этот класс содержит пользовательские теги блоков для мода HBM-Modernized.
// Теги позволяют группировать блоки по определенным характеристикам. TODO На данный момент частицы не излучаются радиоактивными блоками
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;

public class HBMBlockTags {
    public static final TagKey<Block> EMIT_DARK_PARTICLES = BlockTags.create(ResourceLocation.fromNamespaceAndPath("hbm_m", "emit_dark_particles"));
}
