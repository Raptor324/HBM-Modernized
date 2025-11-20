package com.hbm_m.block;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.SwitchBlockEntity;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;

public class SwitchBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;



    public SwitchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, false);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 1. Toggle state
        boolean isPowered = !state.getValue(POWERED);
        BlockState newState = state.setValue(POWERED, isPowered);

        // 2. Update Block and BE
        level.setBlock(pos, newState, 3);

        // 3. Force Capability invalidation on the TE so neighbors re-query
        if(level.getBlockEntity(pos) instanceof SwitchBlockEntity be) {
            be.updateState(isPowered);
        }

        // 4. Handle Network Logic
        EnergyNetworkManager manager = EnergyNetworkManager.get((ServerLevel) level);
        if (isPowered) {
            level.playSound(null, pos, ModSounds.SWITCH_ON.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            manager.addNode(pos);
        } else {
            manager.removeNode(pos);
            level.playSound(null, pos, ModSounds.SWITCH_ON.get(), SoundSource.BLOCKS, 1.0f, 0.7f);
        }

        // 5. CRITICAL: Notify neighbors to update their connections
        // This forces the WireBlock to run its updateShape/getConnectionState logic again
        level.updateNeighborsAt(pos, this);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            if (state.getValue(POWERED)) {
                EnergyNetworkManager.get((ServerLevel) level).addNode(pos);
            }
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            EnergyNetworkManager.get((ServerLevel) level).removeNode(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwitchBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        // Убедитесь, что тип BE совпадает
        return createTickerHelper(type, ModBlockEntities.SWITCH_BE.get(), SwitchBlockEntity::tick);
    }
}