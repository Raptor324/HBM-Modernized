package com.hbm_m.block.network;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.api.pneumatic.IPneumaticConnector;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.pneumatic.PneumoTubeBlockEntity;
import com.hbm_m.item.ModItems;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 1:1-Port von {@code PneumoTube} (1.7.10): das Druckluftrohr.
 *
 * <p>Die Hitbox ist der Kern von 5/16 bis 11/16 plus je ein Arm zu jeder Seite, an der etwas
 * angeschlossen ist - dazu zaehlen andere Rohre, Bloecke mit einem Druckluftanschluss und die
 * beiden eingestellten Arbeitsseiten des Rohrs selbst.</p>
 *
 * <p>Bedienung wie im Original: <b>Rechtsklick mit dem Schraubenzieher</b> stellt die Einzugsseite
 * eine Position weiter, <b>schleichend</b> die Auswurfseite. Durchgegangen werden alle sechs
 * Richtungen plus "aus"; Seiten, an denen ein anderes Rohr sitzt, werden uebersprungen, und bei der
 * ersten Seite mit einem Inventar bleibt der Umlauf stehen. Ein gewoehnlicher Rechtsklick oeffnet
 * die Oberflaeche - aber nur, wenn das Rohr eine der beiden Rollen hat.</p>
 */
public class PneumoTubeBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = ImmutableMap.of(
            Direction.NORTH, NORTH, Direction.SOUTH, SOUTH,
            Direction.WEST, WEST, Direction.EAST, EAST,
            Direction.UP, UP, Direction.DOWN, DOWN);

    /** Original: Kern von 0.3125 bis 0.6875, also 5/16 bis 11/16. */
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = ImmutableMap.of(
            Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5),
            Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16),
            Direction.WEST, Block.box(0, 5, 5, 5, 11, 11),
            Direction.EAST, Block.box(11, 5, 5, 16, 11, 11),
            Direction.UP, Block.box(5, 11, 5, 11, 16, 11),
            Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11));

    public PneumoTubeBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
                shape = Shapes.or(shape, ARMS.get(dir));
            }
        }
        return shape;
    }

    // ── Verbindungen ────────────────────────────────────────────────────────

    /** Original: {@code canConnectTo} plus die beiden Arbeitsseiten aus {@code setBlockBoundsBasedOnState}. */
    private boolean connects(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockEntity self = level.getBlockEntity(pos);
        if (self instanceof PneumoTubeBlockEntity tube) {
            if (tube.getInsertionDir() == dir || tube.getEjectionDir() == dir) return true;
        }

        BlockPos neighborPos = pos.relative(dir);
        if (level.getBlockState(neighborPos).is(this)) return true;

        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof IPneumaticConnector connector) {
            return connector.canConnectPneumatic(dir.getOpposite());
        }
        return false;
    }

    public BlockState getConnectionState(LevelAccessor level, BlockPos pos) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), connects(level, pos, dir));
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return getConnectionState(ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return getConnectionState(level, currentPos);
    }

    // ── BlockEntity ─────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumoTubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PNEUMO_TUBE_BE.get(),
                (lvl, pos, st, be) -> PneumoTubeBlockEntity.tick(lvl, pos, st, (PneumoTubeBlockEntity) be));
    }

    /**
     * Die fuenfzehn Plaetze halten blosse Vorlagen, keine echten Gegenstaende - beim Abbau darf
     * darum nichts herausfallen, sonst liesse sich damit vervielfaeltigen.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ── Bedienung ───────────────────────────────────────────────────────────

    /**
     * 1:1-Port von {@code onScrew}: die Arbeitsseite eine Position weiterdrehen.
     *
     * <p>Der Umlauf geht ueber die sechs Richtungen und endet bei "aus" - deshalb sieben Schritte.
     * Rohre werden uebersprungen, weil zwei aneinandergrenzende Rohre ohnehin dasselbe Netz sind,
     * und beim ersten Inventar bleibt er stehen.</p>
     */
    private InteractionResult screw(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof PneumoTubeBlockEntity tube)) return InteractionResult.PASS;

        boolean sneaking = player.isShiftKeyDown();
        Direction rot = sneaking ? tube.getEjectionDir() : tube.getInsertionDir();
        Direction other = sneaking ? tube.getInsertionDir() : tube.getEjectionDir();

        for (int i = 0; i < 7; i++) {
            rot = nextDirection(rot);
            if (rot == null) break;              // "aus" ist immer gueltig
            if (rot == other) continue;          // die andere Rolle sitzt schon dort
            BlockEntity neighbor = level.getBlockEntity(pos.relative(rot));
            if (neighbor instanceof PneumoTubeBlockEntity) continue;
            if (neighbor != null) break;         // ein Inventar - hier bleibt der Umlauf stehen
        }

        if (sneaking) tube.setEjectionDir(rot); else tube.setInsertionDir(rot);

        level.setBlock(pos, getConnectionState(level, pos), 3);
        return InteractionResult.CONSUME;
    }

    /** Der Umlauf des Originals: {@code (ordinal + 1) % 7}, wobei 6 fuer "aus" steht. */
    @Nullable
    private static Direction nextDirection(@Nullable Direction current) {
        int next = (current == null ? 6 : current.ordinal()) + 1;
        next %= 7;
        return next == 6 ? null : Direction.values()[next];
    }

    private InteractionResult interact(BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() == ModItems.SCREWDRIVER.get()) {
            return screw(level, pos, player);
        }

        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        // Original: ohne Rolle hat das Rohr nichts einzustellen, der Klick geht ins Leere.
        if (!(level.getBlockEntity(pos) instanceof PneumoTubeBlockEntity tube)) return InteractionResult.PASS;
        if (!tube.isCompressor() && !tube.isEndpoint()) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, tube, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}
}
