package com.hbm_m.block.generic;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code com.hbm.blocks.generic.BlockFissure} (1.7.10) — the {@code ore_volcano}
 * "Geothermal Vent" created by the Fissure Bomb in a crater.
 *
 * <p>Original behaviour: unbreakable ({@code setBlockUnbreakable()}, resistance 1 000 000),
 * light level 1, two-pass render (bedrock + molten_overlay), and {@code updateTick} places
 * {@code volcanic_lava_block} above itself whenever the spot is replaceable ({@code rad_lava_block}
 * when its metadata != 0). The companion {@code TileEntityFissure} additionally pushes 1000 mB of
 * lava into the fluid network each tick — the port's TileEntity-less variant relies on the
 * random-tick eruption plus the Hephaestus block-scan heat (300, tripled for 20 ticks after a
 * scan, 1:1 from the original {@code heatFromBlock}).</p>
 *
 * <p>The rad variant is a separate registration ({@code ore_volcano_rad}) — the original's rad
 * flag was block metadata, the port uses two blocks so blockstates stay simple.</p>
 */
public class BlockFissure extends Block {

    private final boolean rad;

    public BlockFissure(Properties properties, boolean rad) {
        super(properties);
        this.rad = rad;
    }

    public boolean isRad() {
        return rad;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        BlockPos above = pos.above();
        if (level.getBlockState(above).canBeReplaced()) {
            Block lava = rad ? ModBlocks.RAD_LAVA_BLOCK.get() : ModBlocks.VOLCANIC_LAVA_BLOCK.get();
            level.setBlock(above, lava.defaultBlockState(), 3);
        }
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockFissure> CODEC =
            simpleCodec(props -> new BlockFissure(props, false));
    @Override protected com.mojang.serialization.MapCodec<? extends Block> codec() { return CODEC; }
    *///?}
}
