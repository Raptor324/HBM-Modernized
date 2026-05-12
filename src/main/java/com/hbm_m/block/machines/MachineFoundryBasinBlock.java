package com.hbm_m.block.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MachineFoundryBasinBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D),
            Block.box(0.0D, 2.0D, 0.0D, 2.0D, 16.0D, 16.0D),
            Block.box(14.0D, 2.0D, 0.0D, 16.0D, 16.0D, 16.0D),
            Block.box(2.0D, 2.0D, 0.0D, 14.0D, 16.0D, 2.0D),
            Block.box(2.0D, 2.0D, 14.0D, 14.0D, 16.0D, 16.0D)
    );

    public MachineFoundryBasinBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}