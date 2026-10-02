package com.hbm_m.block.decorations;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code DecoBlock} als {@code steel_corner}: drei Platten je Ausrichtung ({@code addCollisionBoxesToList} und
 * {@code RenderSteelCorner} verwenden dieselben Boxen). Ausrichtung und Schraubenzieher wie {@link SteelWallBlock};
 * {@code FACING} = Gegenrichtung der Meta.
 */
public class SteelCornerBlock extends SteelWallBlock {

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // Meta 2 (Nord) -> FACING Sued
        SHAPES.put(Direction.SOUTH, Shapes.or(Shapes.box(0.25, 0, 0.875, 1, 1, 1), Shapes.box(0, 0, 0.75, 0.25, 1, 1), Shapes.box(0, 0, 0, 0.125, 1, 0.75)));
        // Meta 3 (Sued) -> FACING Nord
        SHAPES.put(Direction.NORTH, Shapes.or(Shapes.box(0, 0, 0, 0.75, 1, 0.125), Shapes.box(0.75, 0, 0, 1, 1, 0.25), Shapes.box(0.875, 0, 0.25, 1, 1, 1)));
        // Meta 4 (West) -> FACING Ost
        SHAPES.put(Direction.EAST, Shapes.or(Shapes.box(0.875, 0, 0, 1, 1, 0.75), Shapes.box(0.75, 0, 0.75, 1, 1, 1), Shapes.box(0, 0, 0.875, 0.75, 1, 1)));
        // Meta 5 (Ost) -> FACING West
        SHAPES.put(Direction.WEST, Shapes.or(Shapes.box(0, 0, 0.25, 0.125, 1, 1), Shapes.box(0, 0, 0, 0.25, 1, 0.25), Shapes.box(0.25, 0, 0, 1, 1, 0.125)));
    }

    public SteelCornerBlock(Properties props) {
        super(props);
    }

    /** Auswahlbox: {@code setBlockBoundsBasedOnState} setzt fuer die Ecke den vollen Block. */
    @Override
    public VoxelShape getShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
                               net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level,
                                        net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape shapeFor(Direction facing) {
        return SHAPES.get(facing);
    }
}
