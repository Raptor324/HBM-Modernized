package com.hbm_m.worldgen;

import com.hbm_m.config.MobConfig;
import com.hbm_m.world.feature.GlyphidHive;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1:1 HbmWorldGen (Glyphiden): bei aktivierten Strukturen in der Oberwelt mit Chance 1/{@code hiveSpawn} je Chunk ein
 * kleines Nest auf dem ersten festen Block von oben bis zwei Bloecke unter der Oberflaeche, jedes zehnte befallen.
 */
public class GlyphidHiveFeature extends Feature<NoneFeatureConfiguration> {

    public GlyphidHiveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        RandomSource rand = context.random();

        if (!com.hbm_m.lib.HbmWorldGen.dungeonsEnabled(world)) return false; // Original: enableDungeons-Block

        if (MobConfig.enableHives() && rand.nextInt(MobConfig.hiveSpawn()) == 0) {
            int i = context.origin().getX() & ~15;
            int j = context.origin().getZ() & ~15;
            int x = i + rand.nextInt(16) + 8;
            int z = j + rand.nextInt(16) + 8;
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            for (int k = 3; k >= -1; k--) {
                BlockPos below = new BlockPos(x, y - 1 + k, z);
                if (world.getBlockState(below).isRedstoneConductor(world, below)) {
                    GlyphidHive.generateSmall(world, x, y + k, z, rand, rand.nextInt(10) == 0, true);
                    return true;
                }
            }
        }
        return false;
    }
}
