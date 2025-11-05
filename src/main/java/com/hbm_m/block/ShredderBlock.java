package com.hbm_m.block;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.ShredderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Блок измельчителя (Shredder)
 *
 * Функционал:
 * - 9 входных слотов для предметов
 * - 2 слота для лезвий (требуется BLADE_TEST)
 * - 18 выходных слотов
 * - Обработка по рецептам или в металлолом
 */
public class ShredderBlock extends BaseEntityBlock {

    public ShredderBlock(Properties properties) {
        super(properties);
    }

    /**
     * Создание BlockEntity для этого блока
     */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShredderBlockEntity(pos, state);
    }

    /**
     * Тип рендеринга блока (модель)
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Открытие GUI при клике правой кнопкой мыши
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof ShredderBlockEntity shredderEntity) {
                NetworkHooks.openScreen((ServerPlayer) player, shredderEntity, pos);
            } else {
                throw new IllegalStateException("Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Настройка тикера для обработки предметов
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(type, ModBlockEntities.SHREDDER.get(),
                (level1, pos, state1, blockEntity) -> ShredderBlockEntity.tick(level1, pos, state1, blockEntity));
    }

    /**
     * Дроп предметов при разрушении блока
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ShredderBlockEntity shredderEntity) {
                shredderEntity.drops();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}