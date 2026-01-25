package com.hbm_m.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class OilClasterSurroundedFeature extends Feature<NoneFeatureConfiguration> {

    public OilClasterSurroundedFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        LevelAccessor level = context.level();

        // Размер твоей структуры (замени на реальные размеры)
        int sizeX = 50;
        int sizeY = 50;
        int sizeZ = 25;

        // Проверяем, что вокруг структуры только камень (или другой нужный блок)
        for (int x = -1; x <= sizeX; x++) {
            for (int y = -1; y <= sizeY; y++) {
                for (int z = -1; z <= sizeZ; z++) {
                    // Только внешняя оболочка
                    if (x == -1 || x == sizeX || y == -1 || y == sizeY || z == -1 || z == sizeZ) {
                        BlockPos checkPos = origin.offset(x, y, z);
                        if (!level.getBlockState(checkPos).is(Blocks.STONE)) {
                            return false; // Не окружено камнем — не спавним
                        }
                    }
                }
            }
        }

        // Если всё ок — разместить структуру (через StructureTemplate или как у тебя реализовано)
        // Например:
        // StructureTemplate template = ...;
        // template.placeInWorld(...);

        // Верни true, если разместил, иначе false
        return true;
    }
}