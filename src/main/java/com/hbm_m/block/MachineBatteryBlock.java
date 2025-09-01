// Файл: com/hbm_m/block/MachineBatteryBlock.java

package com.hbm_m.block;

import com.hbm_m.block.entity.MachineBatteryBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MachineBatteryBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public MachineBatteryBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new MachineBatteryBlockEntity(pPos, pState);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof MachineBatteryBlockEntity) {
                ((MachineBatteryBlockEntity) blockEntity).drops();
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }
    
    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (pLevel.getBlockEntity(pPos) instanceof MachineBatteryBlockEntity battery) {
            // Восстанавливаем кастомное имя, если оно есть
            if (pStack.hasCustomHoverName()) {
                battery.setCustomName(pStack.getHoverName());
            }
            // Восстанавливаем энергию из NBT предмета (логика IPersistentNBT)
            CompoundTag nbt = pStack.getTagElement("BlockEntityTag");
            if (nbt != null) {
                battery.load(nbt);
            }
        }
    }
    
    @Override
    public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof MachineBatteryBlockEntity battery) {
            if (!pLevel.isClientSide && !pPlayer.isCreative()) {
                // Создаем ItemStack, который выпадет
                ItemStack itemstack = new ItemStack(this);
                // Сохраняем NBT нашего BlockEntity в этот ItemStack
                blockentity.saveToItem(itemstack);
                // Если у блока было кастомное имя, добавляем его предмету
                if (battery.hasCustomName()) {
                    itemstack.setHoverName(battery.getCustomName());
                }
                // Спавним предмет
                popResource(pLevel, pPos, itemstack);
            }
        }
        super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if (entity instanceof MachineBatteryBlockEntity) {
                NetworkHooks.openScreen(((ServerPlayer) pPlayer), (MachineBatteryBlockEntity) entity, pPos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.MACHINE_BATTERY_BE.get(), MachineBatteryBlockEntity::tick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState pState) {
        // Говорим игре, что этот блок МОЖЕТ выдавать сигнал компаратора.
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        // Здесь мы получаем BlockEntity и вызываем его метод для расчета мощности сигнала.
        // Этот метод заменяет собой и hasComparatorInputOverride, и getComparatorInputOverride.
        BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
        if (blockEntity instanceof MachineBatteryBlockEntity battery) {
            return battery.getComparatorPower();
        }
        return 0;
    }
}