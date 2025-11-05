package com.hbm_m.block;

import com.hbm_m.block.entity.IronCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Iron Crate - блок с инвентарём на 36 слотов
 * Работает как Shulker Box - сохраняет содержимое при разрушении
 */
public class IronCrateBlock extends BaseEntityBlock {

    public IronCrateBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronCrateBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof IronCrateBlockEntity crateEntity) {
            NetworkHooks.openScreen((ServerPlayer) player, crateEntity, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Вызывается при установке блока
     * Восстанавливаем содержимое из ItemStack
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        if (stack.hasTag()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof IronCrateBlockEntity crateEntity) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("BlockEntityTag")) {
                    CompoundTag beTag = tag.getCompound("BlockEntityTag");
                    crateEntity.load(beTag);
                }
            }
        }
    }

    /**
     * Переопределяем чтобы предметы НЕ выпадали при разрушении
     * Они сохранятся в самом блоке
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Не дропаем содержимое - оно сохранится в блоке
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Переопределяем получение дропа - блок дропается с сохранённым содержимым
     */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof IronCrateBlockEntity crateEntity) {
            if (!level.isClientSide) {
                // Создаём ItemStack блока
                ItemStack stack = new ItemStack(this);

                // Сохраняем содержимое в блок
                crateEntity.saveToItem(stack);

                // Дропаем блок с содержимым
                popResource(level, pos, stack);
            }
        }

        super.playerWillDestroy(level, pos, state, player);
    }
}