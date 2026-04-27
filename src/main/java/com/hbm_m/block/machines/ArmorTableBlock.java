package com.hbm_m.block.machines;

// Этот класс реализует блок стола модификации брони
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.hbm_m.platform.NetworkHooksCompat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.menu.ArmorTableMenu;

public class ArmorTableBlock extends BaseEntityBlock {

    public ArmorTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState pState) {
        return RenderShape.MODEL;
    }
    
    // BlockEntity нам не нужен для хранения инвентаря, поэтому метод может возвращать null
    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pPos, @NotNull BlockState pState) {
        return null; 
    }

    @Override
    public InteractionResult use(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull Player pPlayer, @NotNull InteractionHand pHand, @NotNull BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            // Создаем анонимный MenuProvider
            MenuProvider menuProvider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.hbm_m.armor_table");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int pContainerId, @NotNull Inventory pPlayerInventory, @NotNull Player pPlayer) {
                    // Передаем в конструктор КООРДИНАТЫ БЛОКА (pPos)
                    return new ArmorTableMenu(pContainerId, pPlayerInventory, pPos);
                }
            };
            // Открываем GUI с помощью NetworkHooks, который правильно обрабатывает передачу данных
            NetworkHooksCompat.openScreen((ServerPlayer) pPlayer, menuProvider, pPos);

        }
        return InteractionResult.SUCCESS;
    }
}