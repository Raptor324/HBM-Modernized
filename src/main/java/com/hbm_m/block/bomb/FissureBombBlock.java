package com.hbm_m.block.bomb;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.explosion.ExplosionNukeSmall;
import com.hbm_m.world.biome.CraterBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;

/**
 * Port of {@code com.hbm.blocks.bomb.BlockFissureBomb} (1.7.10).
 *
 * <p>Detonation (1:1 from the original): a small nuclear explosion
 * ({@code ExplosionNukeSmall.PARAMS_MEDIUM}), then in an 11x11x11 cube around the blast every
 * {@code ore_bedrock_mineral} becomes {@code ore_volcano} (rad variant when detonated inside a
 * crater biome), and every {@code ore_bedrock_oil} becomes plain bedrock. The resulting fissures
 * (ore_volcano) spew volcanic/rad lava and act as a Hephaestus heat source.</p>
 *
 * <p>Ignition follows the original {@code BlockTNTBase}: redstone signal (on place or neighbor
 * change) detonates immediately.</p>
 */
public class FissureBombBlock extends Block implements IBomb {

    private static final int RANGE = 5;

    public FissureBombBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            detonate(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            detonate(level, pos);
        }
    }

    private void detonate(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
        explode(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        level.removeBlock(pos, false);
        explode(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return BombReturnCode.DETONATED;
    }

    private void explode(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        ExplosionNukeSmall.explode(serverLevel, x, y, z, ExplosionNukeSmall.PARAMS_MEDIUM);

        boolean crater = isCraterBiome(serverLevel, BlockPos.containing(x, y, z));

        BlockPos center = BlockPos.containing(x, y, z);
        for (int i = -RANGE; i <= RANGE; i++) {
            for (int j = -RANGE; j <= RANGE; j++) {
                for (int k = -RANGE; k <= RANGE; k++) {
                    BlockPos target = center.offset(i, j, k);
                    Block block = serverLevel.getBlockState(target).getBlock();
                    if (block == ModBlocks.ORE_BEDROCK.get()) {
                        // Rad ore_volcano in the original is metadata != 0; the port keeps two
                        // state properties - see BlockFissure.
                        serverLevel.setBlock(target,
                                (crater ? ModBlocks.ORE_VOLCANO_RAD : ModBlocks.ORE_VOLCANO).get().defaultBlockState(), 3);
                    } else if (block == ModBlocks.ORE_BEDROCK_OIL.get()) {
                        serverLevel.setBlock(target, Blocks.BEDROCK.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static boolean isCraterBiome(ServerLevel level, BlockPos pos) {
        ResourceKey<Biome> key = level.getBiome(pos).unwrapKey().orElse(null);
        return key == CraterBiomes.INNER_CRATER_KEY || key == CraterBiomes.CRATER_KEY
                || key == CraterBiomes.OUTER_CRATER_KEY;
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<FissureBombBlock> CODEC = simpleCodec(FissureBombBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends Block> codec() { return CODEC; }
    *///?}
}

