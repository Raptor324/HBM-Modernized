package com.hbm_m.block.decorations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code BlockScaffold} ({@code steel_scaffold}, Farben als eigene Bloecke). Vier Lagen wie Meta 0/4/8/12:
 * auf Boden/Decke gesetzt stehend, je nach Blickachse laengs X (Meta 0) oder Z (Meta 8); an eine Nord/Sued-Seite
 * gesetzt liegend (Meta 4), an eine West/Ost-Seite liegend gedreht (Meta 12).
 */
public class SteelScaffoldBlock extends Block {

    public enum Orient implements StringRepresentable {
        UPRIGHT_X("upright_x"), FLAT_NS("flat_ns"), UPRIGHT_Z("upright_z"), FLAT_WE("flat_we");

        private final String name;
        Orient(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }

        Orient turned() {
            return switch (this) {
                case UPRIGHT_X -> UPRIGHT_Z;
                case UPRIGHT_Z -> UPRIGHT_X;
                case FLAT_NS -> FLAT_WE;
                case FLAT_WE -> FLAT_NS;
            };
        }
    }

    public static final EnumProperty<Orient> ORIENT = EnumProperty.create("orient", Orient.class);

    private static final double F = 1.0 / 16.0;
    private static final VoxelShape UPRIGHT_X = Shapes.box(0, 0, 2 * F, 1, 1, 14 * F);
    private static final VoxelShape UPRIGHT_Z = Shapes.box(2 * F, 0, 0, 14 * F, 1, 1);
    private static final VoxelShape FLAT = Shapes.box(0, 2 * F, 0, 1, 14 * F, 1);

    public SteelScaffoldBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(ORIENT, Orient.UPRIGHT_X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        Orient o;
        if (side.getAxis() == Direction.Axis.Y) {
            o = ctx.getHorizontalDirection().getAxis() == Direction.Axis.Z ? Orient.UPRIGHT_X : Orient.UPRIGHT_Z;
        } else if (side.getAxis() == Direction.Axis.Z) {
            o = Orient.FLAT_NS;
        } else {
            o = Orient.FLAT_WE;
        }
        return defaultBlockState().setValue(ORIENT, o);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(ORIENT)) {
            case UPRIGHT_X -> UPRIGHT_X;
            case UPRIGHT_Z -> UPRIGHT_Z;
            default -> FLAT;
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? state.setValue(ORIENT, state.getValue(ORIENT).turned()) : state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }
}
