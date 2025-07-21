package com.hbm_m.block;

import com.hbm_m.block.entity.ArmorTableBlockEntity;
// import com.hbm_m.menu.ArmorTableMenu;
import net.minecraft.core.BlockPos;
// import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
// import net.minecraft.world.MenuProvider;
// import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockArmorTable extends BaseEntityBlock {

    public BlockArmorTable(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ArmorTableBlockEntity(pos, state);
    }
    
    @Override
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, 
    @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ArmorTableBlockEntity) {
                // Открываем GUI через BlockEntity, который является MenuProvider
                NetworkHooks.openScreen((ServerPlayer) player, (ArmorTableBlockEntity) be, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof ArmorTableBlockEntity be) {
                // Получаем capability
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    // ИСПРАВЛЕНО: Вручную перебираем слоты и выбрасываем предметы
                    for (int i = 0; i < handler.getSlots(); ++i) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                });
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}