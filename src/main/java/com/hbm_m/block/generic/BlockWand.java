package com.hbm_m.block.generic;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code BlockWand} (wand_air): Platzhalter fuer NBT-Strukturen, der beim Speichern als {@link #exportAs}
 * exportiert wird. Glasartig, ohne Kollision, Grenzen 1/16 bis 15/16, Flaechen zu gleichartigen Nachbarn entfallen.
 */
public class BlockWand extends Block {

    private static final VoxelShape SHAPE = Block.box(1, 1, 1, 15, 15, 15);

    public final Supplier<Block> exportAs;

    public BlockWand(Properties properties, Supplier<Block> exportAs) {
        super(properties);
        this.exportAs = exportAs;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }
}
