package com.hbm_m.block.fluid;

import java.util.function.Supplier;

import com.hbm_m.damagesource.ModDamageSources;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * 1:1-Port von {@code AcidBlock}: toetet alles, was hineingeraet (10000 Schaden), und loest jeden
 * angrenzenden Block auf, der nicht selbst Saeure ist - bei jedem Fliess-Tick und jeder
 * Nachbaraenderung.
 */
public class AcidBlock extends HbmFluidBlock {

    public AcidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public Boolean canDisplace(BlockGetter level, BlockPos pos, BlockState target) {
        if (!target.getFluidState().isEmpty()) return false;
        return null;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        setInWeb(entity, state);
        entity.hurt(ModDamageSources.acid(level), 10000F);
    }

    @Override
    public void onFluidTick(Level level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) reactToBlocks(level, pos.relative(d));
    }

    @Override
    protected void onNeighborChange(Level level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) reactToBlocks(level, pos.relative(d));
    }

    public void reactToBlocks(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() != this && !s.isAir()) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }
}
