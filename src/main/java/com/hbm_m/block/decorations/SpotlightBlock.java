package com.hbm_m.block.decorations;

import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code com.hbm.blocks.machine.Spotlight} und {@code SpotlightModular}: Lampe an einer Flaeche (Metadaten
 * Seite &lt;&lt; 1 = {@link #FACING}, Bit 0 = kaputt = {@link #BROKEN}). Anders als eine Redstonelampe AN ohne Signal und
 * aus mit Signal (wie eine Redstonefackel, 4 Ticks Verzoegerung beim Ausschalten). Wirft einen Strahl aus
 * {@link SpotlightBeamBlock}-Lichtbloecken in Blickrichtung ({@link #beamLength}). Kaputte Lampen (aus Strukturen)
 * leuchten nicht und werden per Rechtsklick samt verbundener gleicher Lampen repariert. Die Neonroehre
 * ({@link LightType#FLUORESCENT}) verbindet sich seitlich zu Endstueck/Mittelstueck ({@link #PART}/{@link #CONN}).
 */
public class SpotlightBlock extends Block implements ISpotlight {

    public static final int META_YELLOW = 0;
    public static final int META_GREEN = 1;
    public static final int META_BLUE = 2;

    public static boolean disableOnGeneration = true;

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final DirectionProperty CONN = DirectionProperty.create("conn");

    public enum LightType { INCANDESCENT, FLUORESCENT, HALOGEN }

    public enum Part implements StringRepresentable {
        SINGLE, CAP, MID;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public final boolean isOn;
    public final int beamLength;
    public final LightType type;
    private final Supplier<Block> off;
    private final Supplier<Block> on;

    public SpotlightBlock(Properties properties, int beamLength, LightType type, boolean isOn, Supplier<Block> off, Supplier<Block> on) {
        super(isOn ? properties.lightLevel(s -> s.getValue(BROKEN) ? 0 : 15) : properties);
        this.beamLength = beamLength;
        this.type = type;
        this.isOn = isOn;
        this.off = off;
        this.on = on;
        BlockState def = this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(BROKEN, false);
        if (type == LightType.FLUORESCENT) def = def.setValue(PART, Part.SINGLE).setValue(CONN, Direction.NORTH);
        this.registerDefaultState(def);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BROKEN, PART, CONN);
    }

    // --- Form: halbe Groessen je Typ, an der Befestigungsseite ---

    private float[] getBounds() {
        switch (type) {
            case FLUORESCENT: return new float[] { 0.5F, 0.5F, 0.1F };
            case HALOGEN: return new float[] { 0.35F, 0.25F, 0.2F };
            default: return new float[] { 0.25F, 0.2F, 0.15F };
        }
    }

    private float[] swizzleBounds(Direction dir) {
        float[] bounds = getBounds();
        switch (dir) {
            case EAST:
            case WEST: return new float[] { bounds[2], bounds[1], bounds[0] };
            case UP:
            case DOWN: return new float[] { bounds[1], bounds[2], bounds[0] };
            default: return bounds;
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        Direction dir = state.getValue(FACING);
        float[] b = swizzleBounds(dir);
        double ox = 0.5 - dir.getStepX() * (0.5 - b[0]);
        double oy = 0.5 - dir.getStepY() * (0.5 - b[1]);
        double oz = 0.5 - dir.getStepZ() * (0.5 - b[2]);
        return Shapes.box(ox - b[0], oy - b[1], oz - b[2], ox + b[0], oy + b[1], oz + b[2]);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter world, BlockPos pos, PathComputationType type) {
        return true;
    }

    // --- Setzen / Halt ---

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState().setValue(FACING, ctx.getClickedFace());
        if (!canSurvive(s, ctx.getLevel(), ctx.getClickedPos())) return null;
        return connect(s, ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return canPlace(world, pos, state.getValue(FACING));
    }

    // BlockSlab doesn't actually properly return isSideSolid
    private static boolean canPlace(LevelReader world, BlockPos pos, Direction dir) {
        BlockPos support = pos.relative(dir.getOpposite());
        BlockState s = world.getBlockState(support);
        if (s.getBlock() instanceof SlabBlock && s.hasProperty(SlabBlock.TYPE)) {
            SlabType t = s.getValue(SlabBlock.TYPE);
            return dir == (t == SlabType.TOP ? Direction.UP : Direction.DOWN) || t == SlabType.DOUBLE;
        }
        return s.isFaceSturdy(world, support, dir);
    }

    // --- Neonroehren-Verbindung (RenderLight) ---

    public boolean canConnectTo(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == this;
    }

    private BlockState connect(BlockState state, BlockGetter world, BlockPos pos) {
        if (type != LightType.FLUORESCENT) return state;
        Direction dir = state.getValue(FACING);
        Direction connectionDirection = null;
        int connectionCount = 0;
        for (Direction availableDir : new Direction[] { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST }) {
            if (availableDir == dir || availableDir == dir.getOpposite()) continue;
            if (canConnectTo(world, pos.relative(availableDir))) {
                connectionCount++;
                connectionDirection = availableDir;
                break;
            }
        }
        if (connectionDirection != null && canConnectTo(world, pos.relative(connectionDirection.getOpposite()))) {
            connectionCount++;
        }
        return state.setValue(PART, connectionCount == 0 ? Part.SINGLE : connectionCount == 1 ? Part.CAP : Part.MID)
                .setValue(CONN, connectionDirection == null ? Direction.NORTH : connectionDirection);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return connect(state, world, pos);
    }

    // --- Strom / Strahl ---

    protected Block getOff() { return off.get(); }
    protected Block getOn() { return on.get(); }

    private BlockState copyTo(BlockState from, Block to) {
        BlockState s = to.defaultBlockState().setValue(FACING, from.getValue(FACING)).setValue(BROKEN, from.getValue(BROKEN));
        return s.setValue(PART, from.getValue(PART)).setValue(CONN, from.getValue(CONN));
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (world.isClientSide) return;
        if (oldState.getBlock() == this) return;
        if (updatePower(world, pos, state)) return;
        updateBeam(world, pos, state);
    }

    private boolean updatePower(Level world, BlockPos pos, BlockState state) {
        if (state.getValue(BROKEN)) return false;
        boolean isPowered = world.hasNeighborSignal(pos);
        if (isOn && isPowered) {
            world.scheduleTick(pos, this, 4);
            return true;
        } else if (!isOn && !isPowered) {
            world.setBlock(pos, copyTo(state, getOn()), 2);
            return true;
        }
        return false;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        Direction dir = state.getValue(FACING);
        super.onRemove(state, world, pos, newState, isMoving);
        if (world.isClientSide || newState.getBlock() == this) return;
        unpropagateBeam(world, pos, dir);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        if (isOn && world.hasNeighborSignal(pos)) {
            world.setBlock(pos, copyTo(state, getOff()), 2);
        }
    }

    // Repropagate the beam if we've become unblocked
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
        if (world.isClientSide) return;
        if (neighborBlock instanceof SpotlightBeamBlock) return;
        if (neighborBlock == Blocks.AIR) return;

        Direction dir = state.getValue(FACING);
        if (!canPlace(world, pos, dir)) {
            dropResources(state, world, pos);
            world.removeBlock(pos, false);
            return;
        }

        if (updatePower(world, pos, state)) return;
        updateBeam(world, pos, state);
    }

    private void updateBeam(Level world, BlockPos pos, BlockState state) {
        if (!isOn || state.getValue(BROKEN)) return;
        propagateBeam(world, pos, state.getValue(FACING), beamLength, META_YELLOW);
    }

    // Replace bulbs on broken lights with a click
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(BROKEN)) return InteractionResult.PASS;
        repair(world, pos);
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    private void repair(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof SpotlightBlock) || !state.getValue(BROKEN)) return;
        world.setBlock(pos, copyTo(state, getOn()).setValue(BROKEN, false), 2);

        for (Direction dir : Direction.values()) {
            BlockPos o = pos.relative(dir);
            if (world.getBlockState(o).getBlock() == this) repair(world, o);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
        return new ItemStack(getOn());
    }

    @Override
    public int getBeamLength() {
        return this.beamLength;
    }

    // Recursively add beam blocks, updating any that already exist with new incoming light directions
    public static void propagateBeam(Level world, BlockPos pos, Direction dir, int distance, int meta) {
        distance--;
        if (distance <= 0)
            return;

        pos = pos.relative(dir);

        BlockState state = world.getBlockState(pos);
        if (!state.isAir())
            return;

        if (!(state.getBlock() instanceof SpotlightBeamBlock)) {
            world.setBlock(pos, ModBlocks.SPOTLIGHT_BEAM.get().defaultBlockState().setValue(SpotlightBeamBlock.COLOR, meta), 3);
        }

        // If we encounter an existing beam, add a new INCOMING direction to the
        // metadata, and cancel propagation if something goes wrong
        if (SpotlightBeamBlock.setDirection(world, pos, dir, true) == 0)
            return;

        propagateBeam(world, pos, dir, distance, meta);
    }

    // Recursively delete beam blocks, if they aren't still illuminated from a different direction
    public static void unpropagateBeam(Level world, BlockPos pos, Direction dir) {
        pos = pos.relative(dir);

        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof SpotlightBeamBlock))
            return;

        // Remove the metadata associated with this direction
        // If all directions are set to zero, delete the beam
        if (SpotlightBeamBlock.setDirection(world, pos, dir, false) == 0) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }

        unpropagateBeam(world, pos, dir);
    }

    // Travels back through a beam to the source, and if found, repropagates the beam
    public static void backPropagate(Level world, BlockPos pos, Direction dir, int meta) {
        pos = pos.relative(dir.getOpposite());

        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof ISpotlight spot) {
            propagateBeam(world, pos, dir, spot.getBeamLength(), meta);
        } else if (!(block instanceof SpotlightBeamBlock)) {
            return;
        }

        backPropagate(world, pos, dir, meta);
    }
}
