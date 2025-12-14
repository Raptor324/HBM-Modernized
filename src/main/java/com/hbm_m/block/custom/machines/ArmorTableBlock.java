package com.hbm_m.block.custom.machines;

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
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hbm_m.block.custom.machines.armormod.menu.ArmorTableMenu;

public class ArmorTableBlock extends BaseEntityBlock {

    public ArmorTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(@Nonnull BlockState pState) {
        return RenderShape.MODEL;
    }
    
    // BlockEntity нам не нужен для хранения инвентаря, поэтому метод может возвращать null
    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pPos, @Nonnull BlockState pState) {
        return null; 
    }

    @Override
    public InteractionResult use(@Nonnull BlockState pState, @Nonnull Level pLevel, @Nonnull BlockPos pPos, @Nonnull Player pPlayer, @Nonnull InteractionHand pHand, @Nonnull BlockHitResult pHit) {
        if (!pLevel.isClientSide()) {
            // Создаем анонимный MenuProvider
            MenuProvider menuProvider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.hbm_m.armor_table");
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(int pContainerId, @Nonnull Inventory pPlayerInventory, @Nonnull Player pPlayer) {
                    // Передаем в конструктор КООРДИНАТЫ БЛОКА (pPos)
                    return new ArmorTableMenu(pContainerId, pPlayerInventory, pPos);
                }
            };
            // Открываем GUI с помощью NetworkHooks, который правильно обрабатывает передачу данных
            NetworkHooks.openScreen((ServerPlayer) pPlayer, menuProvider, pPos);
        }
        return InteractionResult.SUCCESS;
    }
}