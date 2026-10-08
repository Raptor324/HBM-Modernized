package com.hbm_m.block.bomb;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code CrystalPulsar}: bei jeder Nachbaraenderung verhaertet er alle Dunkelkristalle in einer Kugel mit Radius
 * ca. 7 ({@code ExplosionChaos.hardenVirus(world, x, y, z, 10)}). Die Bedingung des Originals ist durch das
 * {@code || !world.isRemote} serverseitig immer erfuellt - das bleibt so.
 */
public class CrystalPulsarBlock extends Block {

    public CrystalPulsarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (!world.isClientSide) {
            hardenVirus(world, pos, 10);
        }
    }

    /** 1:1 {@code ExplosionChaos.hardenVirus}. */
    public static void hardenVirus(Level world, BlockPos pos, int bombStartStrength) {

        int r = bombStartStrength;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        BlockPos p = pos.offset(xx, yy, zz);
                        if (world.getBlockState(p).is(ModBlocks.CRYSTAL_VIRUS.get()))
                            world.setBlockAndUpdate(p, ModBlocks.CRYSTAL_HARDENED.get().defaultBlockState());
                    }
                }
            }
        }
    }
}
