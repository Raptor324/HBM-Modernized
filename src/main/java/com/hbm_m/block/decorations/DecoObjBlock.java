package com.hbm_m.block.decorations;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Deko-Modellbloecke des Originals, deren Modell ein ISBRH/TESR je Meta dreht (DecoTapeRecorder, DecoSteelPoles,
 * DecoPoleTop, DecoPoleSatelliteReceiver, BlockDecoModel, BlockDecoCRT, BlockDecoToaster). Die Drehung steckt in den
 * gebackenen Blockstates; {@link Placement} bildet {@code onBlockPlacedBy} nach:
 * <ul>
 * <li>{@code OPPOSITE}: Meta 2/5/3/4 fuer Blick S/W/N/O - also der Gegenrichtung des Blicks (Tape Recorder, Masten,
 * Satellitenschuessel, BlockDecoModel);</li>
 * <li>{@code LOOK}: {@code meta & 3} = Blickrichtung S/W/N/O (CRT, Toaster);</li>
 * <li>{@code NONE}: ohne Ausrichtung (Antennenspitze).</li>
 * </ul>
 */
public class DecoObjBlock extends Block {

    public enum Placement { NONE, OPPOSITE, LOOK }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final Placement placement;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public DecoObjBlock(Properties properties, Placement placement, Function<Direction, VoxelShape> shape) {
        super(properties);
        this.placement = placement;
        for (Direction d : Direction.Plane.HORIZONTAL) shapes.put(d, shape.apply(d));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public DecoObjBlock(Properties properties, Placement placement) {
        this(properties, placement, d -> Shapes.block());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction look = context.getHorizontalDirection();
        return switch (placement) {
            case NONE -> defaultBlockState();
            case OPPOSITE -> defaultBlockState().setValue(FACING, look.getOpposite());
            case LOOK -> defaultBlockState().setValue(FACING, look);
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return placement == Placement.NONE ? state : state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return placement == Placement.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * {@code BlockDecoModel.setBlockBoundsBasedOnState}: Grundbox (Sued) je Ausrichtung gespiegelt/gedreht.
     * {@code facing} ist die Meta-Richtung (Nord = Blick nach Sueden).
     */
    public static Function<Direction, VoxelShape> decoModelBounds(double mnX, double mnY, double mnZ, double mxX, double mxY, double mxZ) {
        return d -> switch (d) {
            case NORTH -> Shapes.box(1 - mxX, mnY, 1 - mxZ, 1 - mnX, mxY, 1 - mnZ);
            case WEST -> Shapes.box(1 - mxZ, mnY, mnX, 1 - mnZ, mxY, mxX);
            case EAST -> Shapes.box(mnZ, mnY, 1 - mxX, mxZ, mxY, 1 - mnX);
            default -> Shapes.box(mnX, mnY, mnZ, mxX, mxY, mxZ);
        };
    }

    /** {@code BlockDecoToaster}: gerade Meta (Blick S/N) breit in X, ungerade (W/O) breit in Z. */
    public static VoxelShape toasterBounds(Direction look) {
        return look.getAxis() == Direction.Axis.Z
                ? Shapes.box(0.25, 0.0, 0.375, 0.75, 0.325, 0.625)
                : Shapes.box(0.375, 0.0, 0.25, 0.625, 0.325, 0.75);
    }
}
