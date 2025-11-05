package com.hbm_m.block;

import com.hbm_m.block.entity.AnvilBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
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

public class AnvilBlock extends BaseEntityBlock {

    public AnvilBlock(Properties properties) {
        super(properties);
    }

    // Создание Block Entity
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnvilBlockEntity(pos, state);
    }

    // Рендер тип (модель блока)
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AnvilBlockEntity be) {
            NetworkHooks.openScreen((ServerPlayer) player, be, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }


}