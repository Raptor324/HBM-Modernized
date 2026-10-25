package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFoundryTankBlockEntity;
import com.hbm_m.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 FoundryTank block. Right-click with a shovel empties its contents into a
 * Schrott-Item des Materials ({@code ItemScraps} mit Menge), wie im Original.
 */
public class MachineFoundryTankBlock extends BaseEntityBlock {

    public MachineFoundryTankBlock(Properties props) { super(props); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineFoundryTankBlockEntity tank)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (FoundryBlockUtil.isShovel(held)) {
            FoundryBlockUtil.shovelOut(level, pos, player, tank, 1.0);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineFoundryTankBlockEntity tank)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (FoundryBlockUtil.isShovel(held)) {
            FoundryBlockUtil.shovelOut(level, pos, player, tank, 1.0);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
        }
    *///?}


    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MachineFoundryTankBlockEntity tank) {
            FoundryBlockUtil.dropContents(level, pos, tank, 1.0);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFoundryTankBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FOUNDRY_TANK_BE.get(),
                MachineFoundryTankBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFoundryTankBlock> CODEC = simpleCodec(MachineFoundryTankBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
