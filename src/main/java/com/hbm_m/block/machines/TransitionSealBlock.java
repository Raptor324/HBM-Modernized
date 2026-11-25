package com.hbm_m.block.machines;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.TransitionSealBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Multiblock blast door. The core sits at the bottom-center of the frame; the 24 tall
 * door slab rises out of the way while redstone powered and blocks the doorway when
 * closed. The multiblock is a 26 wide, 24 tall, 1 thick slab of plain solid blocks;
 * while open, the elliptical opening (semi-axes 10.5 x 9.5, centred in the wall) is
 * made passable so there is always a 2.5 block wall between the opening and the
 * footprint at every point.
 */
public class TransitionSealBlock extends BaseEntityBlock implements IMultiblockController, com.hbm_m.api.bomb.IBomb {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public TransitionSealBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        // 26 wide x 24 tall x 1 deep slab, controller anchored at the bottom-center.
        // Bottom layer must be 26 wide too (12 O + C + 13 O) or the wall gets a stray block.
        String bottom = "OOOOOOOOOOOOCOOOOOOOOOOOOO";
        String fill = "OOOOOOOOOOOOOOOOOOOOOOOOOO";

        String[][] layers = new String[24][];
        for(int y = 0; y < 24; y++) {
            layers[y] = new String[] { y == 0 ? bottom : fill };
        }

        return MultiblockStructureHelper.createFromLayersWithRoles(
                layers,
                Map.of(),
                () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                Map.of('O', PartRole.DEFAULT, 'C', PartRole.CONTROLLER),
                null,
                null
        );
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return PartRole.DEFAULT;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransitionSealBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TRANSITION_SEAL_BE.get(), TransitionSealBlockEntity::tick);
    }

    /** audit10: 1:1 {@code BlockDoorGeneric.explode} - das Uebergangssiegel ist nicht fernsteuerbar (remoteControllable false). */
    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {
        return BombReturnCode.ERROR_INCOMPATIBLE;
    }

    /** audit10: 1:1 {@code BlockDoorGeneric.onBlockActivated} - nicht schleichend schaltet ein Klick das Tor (tryToggle). */
    //? if < 1.21.1 {
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                     net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return hbmOnUse(level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        return hbmOnUse(level, pos, player, net.minecraft.world.InteractionHand.MAIN_HAND);
    }
    *///?}

    private net.minecraft.world.InteractionResult hbmOnUse(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        for (net.minecraft.world.item.ItemStack held : new net.minecraft.world.item.ItemStack[] { player.getMainHandItem(), player.getOffhandItem() }) {
            if (!held.isEmpty() && (held.getItem() instanceof com.hbm_m.item.ItemLock
                    || held.getItem() instanceof com.hbm_m.item.ItemCounterfeitKeys
                    || held.getItem() instanceof com.hbm_m.item.tool.ItemLock
                    || held.getItem() instanceof com.hbm_m.item.tool.ItemCounterfeitKeys)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
        }

        if (level.getBlockEntity(pos) instanceof TransitionSealBlockEntity seal) {
            if (seal.tryToggle(player)) {
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
            }
            return net.minecraft.world.InteractionResult.PASS;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof TransitionSealBlockEntity seal) {
                seal.invalidateRedstoneCache();
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if(!state.is(oldState.getBlock()) && !level.isClientSide()) {
            placeMultiblockStructure(level, pos, state);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if(!state.is(newState.getBlock()) && !level.isClientSide()) {
            structureHelper.destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCellCollisionShape(level, pos, state, pos);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    //? if < 1.21.1 {
    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        if(level.getBlockEntity(pos) instanceof TransitionSealBlockEntity seal) {
            return switch(type) {
                case LAND, AIR -> seal.isOpen();
                default -> false;
            };
        }
        return false;
    }
    //?} else {
    /*@Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        // 1.21.1: BlockGetter/BlockPos из сигнатуры убраны, BE-запрос недоступен.
        // Возвращаем false — поведение по умолчанию (плотный блок).
        return false;
    }
    *///?}

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Collision of a single cell of the 26 x 24 wall. While the door is open, a cell
     * is passable when the centre of its cell falls inside the ellipse centred in the
     * wall (semi-axes 10.5 x 9.5), leaving at least a 2.5 block wall between the
     * opening and the footprint at every point.
     */
    public static VoxelShape getCellCollisionShape(BlockGetter level, BlockPos corePos, BlockState state, BlockPos cellPos) {
        return getCellCollisionShape(level, corePos, state, cellPos, 1.0F);
    }

    /**
     * Collision of a single cell of the 26 x 24 wall. While the door is opening or open,
     * cells inside the elliptical doorway (semi-axes 10.5 x 9.5) become passable
     * progressively from bottom to top as {@code openProgress} rises from 0 to 1.
     */
    public static VoxelShape getCellCollisionShape(BlockGetter level, BlockPos corePos, BlockState state, BlockPos cellPos, float openProgress) {
        if (openProgress <= 0F) return Shapes.block();

        Direction facing = state.getValue(FACING);
        boolean zAxis = facing == Direction.EAST || facing == Direction.WEST;
        int dw = zAxis ? cellPos.getZ() - corePos.getZ() : cellPos.getX() - corePos.getX();
        int dy = cellPos.getY() - corePos.getY();

        double ex = dw / 10.5;
        double ey = (dy - 11.5) / 9.5;
        if(ex * ex + ey * ey <= 1.0 && dy < 2.0 + 20.0 * openProgress) return Shapes.empty();
        return Shapes.block();
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<TransitionSealBlock> CODEC = simpleCodec(TransitionSealBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
