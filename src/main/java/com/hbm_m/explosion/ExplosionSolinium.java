package com.hbm_m.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.hbm_m.block.ModBlocks;

/**
 * «Синяя стирка» солиния: органика удаляется, трава/мицелий превращаются в землю.
 * Наследует спиральную геометрию Fleija, заменяя только обработку колонки.
 */
public class ExplosionSolinium extends ExplosionFleija {

    public ExplosionSolinium(int x, int y, int z, Level level, int rad, float coefficient, float coefficient2) {
        super(x, y, z, level, rad, coefficient, coefficient2);
    }

    @Override
    protected void breakColumn(int x, int z) {
        if (!(level instanceof ServerLevel server)) return;
        int dist = this.radius2 - (x * x + z * z);
        if (dist <= 0) return;
        dist = (int) Math.sqrt(dist);
        // Без защиты нижнего слоя: бедрок переживёт и это.
        for (int y = dist; y > -dist; y--) {
            cleanse(server, new BlockPos(this.posX + x, this.posY + y, this.posZ + z));
        }
    }

    /** Земля остаётся землёй, растения перестают существовать. */
    private static void cleanse(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        var block = state.getBlock();
        if (block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM
                || block == ModBlocks.WASTE_EARTH.get() || block == ModBlocks.WASTE_MYCELIUM.get()) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            return;
        }
        if (isCleansableMaterial(state)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static boolean isCleansableMaterial(BlockState state) {
        if (state.is(BlockTags.LEAVES)) return true;
        // Дерево, тыквы и всё, что режется топором.
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return true;
        if (state.is(BlockTags.CORALS) || state.is(BlockTags.CORAL_BLOCKS)
                || state.is(BlockTags.WALL_CORALS)) return true;
        var block = state.getBlock();
        if (block instanceof BushBlock) return true;
        if (block instanceof CactusBlock) return true;
        if (block instanceof SugarCaneBlock) return true;
        if (block instanceof VineBlock || block instanceof GrowingPlantHeadBlock) return true;
        return state.is(Blocks.SPONGE) || state.is(Blocks.WET_SPONGE);
    }
}
