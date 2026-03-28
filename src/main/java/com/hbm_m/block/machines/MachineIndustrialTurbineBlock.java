package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraftforge.network.NetworkHooks;

/**
 * Industrial Turbine - converts steam to energy (HE).
 * Multiblock structure: 3x3x7
 * Model parts: Turbine (body), Flywheel (animated), Gauge
 */
public class MachineIndustrialTurbineBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public MachineIndustrialTurbineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            MultiblockStructureHelper helper = getStructureHelper();
            Direction facing = state.getValue(FACING);

            helper.placeStructure(level, pos, facing, this);

            EnergyNetworkManager.get((ServerLevel) level).addNode(pos);

            for (BlockPos localPos : helper.getStructureMap().keySet()) {
                if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
                    BlockPos worldPos = helper.getRotatedPos(pos, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).addNode(worldPos);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide()) {
            MultiblockStructureHelper helper = getStructureHelper();
            Direction facing = state.getValue(FACING);

            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);

            for (BlockPos localPos : helper.getStructureMap().keySet()) {
                if (getPartRole(localPos) == PartRole.ENERGY_CONNECTOR) {
                    BlockPos worldPos = helper.getRotatedPos(pos, localPos, facing);
                    EnergyNetworkManager.get((ServerLevel) level).removeNode(worldPos);
                }
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineIndustrialTurbineBlockEntity turbine) {
                turbine.drops();
            }

            helper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineIndustrialTurbineBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                NetworkHooks.openScreen((ServerPlayer) player, provider, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), MachineIndustrialTurbineBlockEntity::tick);
    }

    // --- IMultiblockController ---

    private static MultiblockStructureHelper STRUCTURE_HELPER;

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    /**
     * 3x3x7 multiblock. Controller at front center bottom (0,0,0).
     * Structure extends x=-1..1, y=0..2, z=0..6 behind the controller.
     */
    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> builder = ImmutableMap.builder();
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = 0; z <= 6; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip controller
                    builder.put(new BlockPos(x, y, z), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
                }
            }
        }
        return builder.build();
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        // Energy connectors: back corners at bottom (y=0, z=6, x=-1 or x=1)
        if (localOffset.getY() == 0 && localOffset.getZ() == 6
                && (localOffset.getX() == -1 || localOffset.getX() == 1)) {
            return PartRole.ENERGY_CONNECTOR;
        }
        // Fluid connectors: sides at front (y=0, z=0, x=-1 or x=1)
        if (localOffset.getY() == 0 && localOffset.getZ() == 0
                && (localOffset.getX() == -1 || localOffset.getX() == 1)) {
            return PartRole.FLUID_CONNECTOR;
        }
        return PartRole.DEFAULT;
    }
}
