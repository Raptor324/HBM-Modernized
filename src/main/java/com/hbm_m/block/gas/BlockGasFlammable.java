package com.hbm_m.block.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockGasFlammable} (1.7.10): brennbares Gas. Liegt eine Zuendquelle daneben
 * oder laeuft eine brennende Kreatur hinein, faengt es Feuer. Ueber Luft loest es sich mit 1/20
 * auf, ansonsten steigt es auf und verteilt sich.
 */
public class BlockGasFlammable extends BlockGasBase {

    public BlockGasFlammable() {
        super(0.8F, 0.8F, 0.2F);
    }

    /** Original: {@code fireSources} - Feuer, Lava, Fackeln, Kuerbislaternen. */
    protected static boolean isFireSource(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.JACK_O_LANTERN)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE);
    }

    /** Original: {@code combust} - der Gasblock wird zu Feuer. */
    protected void combust(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
    }

    @Override
    protected boolean onGasTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        for (Direction dir : Direction.values()) {
            if (isFireSource(level.getBlockState(pos.relative(dir)))) {
                combust(level, pos);
                return true;
            }
        }

        if (random.nextInt(20) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.removeBlock(pos, false);
            return true;
        }

        return false;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Original: eine brennende Entitaet zuendet das Gas.
        if (!level.isClientSide() && entity.isOnFire() && level instanceof ServerLevel serverLevel) {
            combust(serverLevel, pos);
            return;
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    protected void affect(LivingEntity living) {
        // Das Gas selbst ist ungiftig - gefaehrlich wird erst die Zuendung.
    }

    @Override
    public Direction getFirstDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) return randomVertical(random);
        return randomHorizontal(random);
    }

    @Override
    public Direction getSecondDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        return randomHorizontal(random);
    }
}
