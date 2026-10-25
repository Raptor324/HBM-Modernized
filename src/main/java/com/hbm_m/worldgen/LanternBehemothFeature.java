package com.hbm_m.worldgen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.decorations.DecoLootBlockEntity;
import com.hbm_m.blockentity.decorations.LanternBehemothBlockEntity;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.item.special.ItemBookLore;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1:1 {@code HbmWorldGen} (Oberwelt, 1/2000 pro Chunk): eine kaputte Signallaterne ({@code lantern_behemoth}, Meta 12,
 * {@code fillSpace {4,0,0,0,0,0}} nach Norden) auf festem Boden; mit 50 % zwei Bloecke noerdlich eine Fundstelle mit
 * dem Beacon-Handbuch ({@code LootGenerator.lootBooklet}).
 */
public class LanternBehemothFeature extends Feature<NoneFeatureConfiguration> {

    public LanternBehemothFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        RandomSource rand = context.random();

        // Original: innerhalb des enableDungeons-Blocks (nur Oberwelt)
        if (!com.hbm_m.lib.HbmWorldGen.dungeonsEnabled(world) || world.getLevel().dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
        if (rand.nextInt(2000) != 0) return false;

        int i = context.origin().getX() & ~15;
        int j = context.origin().getZ() & ~15;
        int x = i + rand.nextInt(16); // Original ohne +8
        int z = j + rand.nextInt(16);
        int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();

        if (!Block.canSupportCenter(world, below, Direction.UP) || !world.getBlockState(pos).canBeReplaced()) return false;

        world.setBlock(pos, ModBlocks.LANTERN_BEHEMOTH.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.NORTH), 3);
        for (int k = 1; k <= 4; k++) {
            BlockPos part = pos.above(k);
            world.setBlock(part, ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 3);
            if (world.getBlockEntity(part) instanceof IMultiblockPart partBe) {
                partBe.setControllerPos(pos);
                partBe.setLocalOffsetFromController(new BlockPos(0, k, 0));
            }
        }

        if (world.getBlockEntity(pos) instanceof LanternBehemothBlockEntity lantern) {
            lantern.isBroken = true;
        }

        if (rand.nextInt(2) == 0) {
            BlockPos lootPos = pos.north(2);
            world.setBlock(lootPos, ModBlocks.DECO_LOOT.get().defaultBlockState(), 3);
            if (world.getBlockEntity(lootPos) instanceof DecoLootBlockEntity loot && loot.getItems().isEmpty()) {
                loot.addItem(ItemBookLore.generateBeaconBook(), 0, 0, 0);
            }
        }

        return true;
    }
}
