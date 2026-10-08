package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.blocks.generic.BlockDirt} ({@code impact_dirt}) und {@code BlockNTMDirt} ({@code ntm_dirt}):
 * wird zu Erde, sobald Gras daneben liegt, waechst bei genug Licht wieder zu Gras (Tom-Staub/-Brand aus
 * {@code TomSaveData} daempfen das Licht), wirft Erde ab.
 * {@code ntm_dirt} heisst wie Erde und tickt nicht.
 */
public class BlockDirtHbm extends Block {

    private final boolean tick;
    private final boolean namedDirt;

    public BlockDirtHbm(Properties properties, boolean tick, boolean namedDirt) {
        super(properties);
        this.tick = tick;
        this.namedDirt = namedDirt;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return tick;
    }

    /** BlockNTMDirt.getLocalizedName: der Name der Vanilla-Erde. */
    @Override
    public String getDescriptionId() {
        return namedDirt ? Blocks.DIRT.getDescriptionId() : super.getDescriptionId();
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.isClientSide) return;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                for (int k = -1; k < 2; k++) {
                    if (world.getBlockState(pos.offset(i, j, k)).getBlock() instanceof GrassBlock) {
                        world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        com.hbm_m.saveddata.TomSaveData data = com.hbm_m.saveddata.TomSaveData.forWorld(world);
        float dust = data.dust;
        float fire = data.fire;
        int light = Math.max(world.getBrightness(LightLayer.BLOCK, pos.above()), (int) (world.getMaxLocalRawBrightness(pos.above()) * (1 - dust)));
        if (light >= 9 && fire == 0) {
            world.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }
    }
}
