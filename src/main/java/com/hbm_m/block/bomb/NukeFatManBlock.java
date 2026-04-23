package com.hbm_m.block.bomb;

import java.util.Map;

import javax.annotation.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.bomb.NukeFatManBlockEntity;
import com.hbm_m.block.explosives.IDetonatable;
import com.hbm_m.explosion.NuclearExplosionAPI;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class NukeFatManBlock extends BaseEntityBlock implements IMultiblockController, IBomb, IDetonatable {

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return false;
        BombReturnCode result = explode(level, pos);
        return result != null && result.wasSuccessful();
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public NukeFatManBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            structureHelper.placeStructure(level, pos, state.getValue(FACING), this);
        }
        if (level.hasNeighborSignal(pos)) {
            explode(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockState(pos).getBlock() == this && level.hasNeighborSignal(pos)) {
            explode(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof NukeFatManBlockEntity nukeBe) {
                    Containers.dropContents(level, pos, nukeBe);
                }
                structureHelper.destroyStructure(level, pos, state.getValue(FACING));
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeFatManBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            NetworkHooks.openScreen((ServerPlayer) player, provider, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (structureHelper != null) {
            return structureHelper.generateShapeFromParts(state.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof NukeFatManBlockEntity nukeBe) {
            if (nukeBe.isReady()) {
                Containers.dropContents(level, pos, nukeBe);
                nukeBe.clearContent();
                Direction facing = level.getBlockState(pos).getValue(FACING);
                structureHelper.destroyStructure(level, pos, facing);
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                NuclearExplosionAPI.startFatMan(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                return BombReturnCode.DETONATED;
            }
            if (nukeBe.isFilled()) {
                Containers.dropContents(level, pos, nukeBe);
                nukeBe.clearContent();
                Direction facing = level.getBlockState(pos).getValue(FACING);
                structureHelper.destroyStructure(level, pos, facing);
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                NuclearExplosionAPI.startFatMan(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                return BombReturnCode.DETONATED;
            }
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        return BombReturnCode.UNDEFINED;
    }

    /**
     * Structure 3 blocks wide (X) x 2 blocks long (Z) x 2 blocks high (Y).
     * One controller (C), rest are parts (A). Controller at center of bottom layer.
     */
    private static MultiblockStructureHelper defineStructure() {
        String[] layer0 = {
            "ACA",  // bottom: 3 wide, 2 deep; C = controller at center
            "AAA"
        };
        String[] layer1 = {
            "AAA",
            "AAA"
        };

        Map<Character, PartRole> roleMap = Map.of(
            'A', PartRole.DEFAULT,
            'C', PartRole.CONTROLLER
        );

        Map<Character, VoxelShape> shapeMap = Map.of(
            'A', Shapes.block(),
            'C', Shapes.block()
        );
        Map<Character, VoxelShape> collisionMap = Map.of(
            'A', Shapes.block(),
            'C', Shapes.block()
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][] { layer0, layer1 },
            Map.of(),
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            shapeMap,
            collisionMap
        );
    }
}
