package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.machines.BatterySocketBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

/**
 * 2×1×2 footprint (controller + 3 phantom energy ports), same layout as 1.7.10 battery socket.
 */
public class MachineBatterySocketBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static MultiblockStructureHelper STRUCTURE_HELPER;

    public MachineBatterySocketBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static MultiblockStructureHelper getStructureHelperStatic() {
        if (STRUCTURE_HELPER == null) {
            STRUCTURE_HELPER = new MultiblockStructureHelper(defineStructure(), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return STRUCTURE_HELPER;
    }

    public static PartRole getPartRoleStatic(BlockPos localOffset) {
        return PartRole.ENERGY_CONNECTOR;
    }

    private static Map<BlockPos, Supplier<BlockState>> defineStructure() {
        ImmutableMap.Builder<BlockPos, Supplier<BlockState>> b = ImmutableMap.builder();
        // 1.7 fillSpace: behind facing, to the right, and diagonal — relative to NORTH-facing controller at origin
        b.put(new BlockPos(0, 0, 1), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        b.put(new BlockPos(1, 0, 0), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        b.put(new BlockPos(1, 0, 1), () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        return b.build();
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return getStructureHelperStatic();
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return getPartRoleStatic(localOffset);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            MultiblockStructureHelper helper = getStructureHelper();
            helper.placeStructure(level, pos, facing, this);
            EnergyNetworkManager mgr = EnergyNetworkManager.get((ServerLevel) level);
            mgr.addNode(pos);
            for (BlockPos local : helper.getStructureMap().keySet()) {
                if (getPartRole(local) == PartRole.ENERGY_CONNECTOR) {
                    mgr.addNode(helper.getRotatedPos(pos, local, facing));
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            MultiblockStructureHelper helper = getStructureHelper();
            Direction facing = state.getValue(FACING);
            EnergyNetworkManager mgr = EnergyNetworkManager.get((ServerLevel) level);
            mgr.removeNode(pos);
            for (BlockPos local : helper.getStructureMap().keySet()) {
                if (getPartRole(local) == PartRole.ENERGY_CONNECTOR) {
                    mgr.removeNode(helper.getRotatedPos(pos, local, facing));
                }
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
                    for (int i = 0; i < h.getSlots(); i++) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), h.getStackInSlot(i));
                    }
                });
            }
            helper.destroyStructure(level, pos, facing);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            NetworkHooks.openScreen((ServerPlayer) player, provider, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BatterySocketBlockEntity socket) {
            return socket.getComparatorOutput();
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatterySocketBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BATTERY_SOCKET_BE.get(), BatterySocketBlockEntity::tick);
    }
}
