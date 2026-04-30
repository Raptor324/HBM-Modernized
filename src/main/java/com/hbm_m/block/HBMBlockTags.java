package com.hbm_m.block;
// Этот класс содержит пользовательские теги блоков для мода HBM-Modernized.
// Теги позволяют группировать блоки по определенным характеристикам. TODO На данный момент частицы не излучаются радиоактивными блоками
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

public class HBMBlockTags {
    /**
     * Используем прямое создание TagKey вместо BlockTags.create(...),
     * потому что сигнатуры BlockTags.create менялись между версиями/маппингами.
     */
    public static final TagKey<Block> EMIT_DARK_PARTICLES =
            TagKey.create(Registries.BLOCK, new ResourceLocation(RefStrings.MODID, "emit_dark_particles"));

}
