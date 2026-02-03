package com.hbm_m.block.custom.machines;

import com.hbm_m.block.entity.custom.machines.OreAcidizerBlockEntity;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.function.Supplier;

public class OreAcidizerBlock extends BaseEntityBlock implements IMultiblockController {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static MultiblockStructureHelper STRUCTURE_HELPER;

    public OreAcidizerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // Legacy-inspired layout (old "MachineCrystallizer" dimensions):
    // forward length 5, left/right 1, up 2 (3 tall). Controller at back-bottom-center (0,0,0).
    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();

        Supplier<BlockState> phantom = () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState();

        // 3 (wide) x 3 (tall) x 5 (long)
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = 0; z <= 4; z++) {
                    if (y == 0 && x == 0 && z == 0) continue; // controller
                    builder.put(new BlockPos(x, y, z), phantom);
                }
            }
        }

        return builder.build();
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        int x = localOffset.getX();
        int y = localOffset.getY();
        int z = localOffset.getZ();

        // Provide at least one fluid connector for piping.
        if (x == 0 && y == 2 && z == 4) {
            return PartRole.FLUID_CONNECTOR;
        }

        // Simple item IO on the sides roughly mid-body.
        if (y == 0 && z == 2) {
            if (x == -1) return PartRole.ITEM_INPUT;
            if (x == 1) return PartRole.ITEM_OUTPUT;
        }

        return PartRole.DEFAULT;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreAcidizerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            getStructureHelper().placeStructure(level, pos, facing, this);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            getStructureHelper().destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider) {
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHooks.openScreen(serverPlayer, (MenuProvider) entity, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
}
