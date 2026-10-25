package com.hbm_m.worldgen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.SoyuzCapsuleBlock;
import com.hbm_m.blockentity.machines.SoyuzCapsuleBlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.ModItems;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1:1 {@code HbmWorldGen} (Strand, 1/{@code capsuleStructure} je Chunk): eine verrostete Landekapsel (Meta 3), vier
 * Bloecke unter der Oberflaeche halb eingegraben, mit der Glas-Schallplatte in einem zufaelligen Platz.
 */
public class SoyuzCapsuleFeature extends Feature<NoneFeatureConfiguration> {

    public SoyuzCapsuleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        RandomSource rand = context.random();

        // Original: innerhalb des enableDungeons-Blocks
        if (!com.hbm_m.lib.HbmWorldGen.dungeonsEnabled(world)) return false;
        int chance = ModClothConfig.get().capsuleStructure;
        if (chance <= 0 || rand.nextInt(chance) != 0) return false;

        int i = context.origin().getX() & ~15;
        int j = context.origin().getZ() & ~15;
        int x = i + rand.nextInt(16);
        int z = j + rand.nextInt(16);
        int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 4;

        BlockPos pos = new BlockPos(x, y, z);

        // canPlaceTorchOnTop des Blocks darueber
        if (!Block.canSupportCenter(world, pos.above(), Direction.UP)) return false;

        world.setBlock(pos, ModBlocks.SOYUZ_CAPSULE.get().defaultBlockState().setValue(SoyuzCapsuleBlock.RUSTED, true), 2);

        if (world.getBlockEntity(pos) instanceof SoyuzCapsuleBlockEntity cap) {
            cap.setItem(rand.nextInt(cap.getContainerSize()), new ItemStack(ModItems.RECORD_GLASS.get()));
        }

        return true;
    }
}
