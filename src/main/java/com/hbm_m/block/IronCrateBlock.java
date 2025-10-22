package com.hbm_m.block;

import com.hbm_m.block.entity.IronCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;


public class IronCrateBlock extends BaseEntityBlock {

    public IronCrateBlock(Properties properties) {
        super(properties);
    }

    // ==================== BLOCK ENTITY ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronCrateBlockEntity(pos, state);
    }

    // ==================== RENDERING ====================


    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ==================== INTERACTION ====================


    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IronCrateBlockEntity crateEntity) {
            // Открываем GUI только на сервере
            NetworkHooks.openScreen((ServerPlayer) player, crateEntity, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // ==================== BLOCK REMOVAL ====================


    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        // Проверяем, что блок действительно был удалён (а не просто изменён)
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IronCrateBlockEntity crateEntity) {
                crateEntity.drops(); // Выбрасываем предметы в мир
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}