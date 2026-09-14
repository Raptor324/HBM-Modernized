package com.hbm_m.block.decorations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.decorations.DecoLootBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт {@code BlockLoot} (1.7.10) — «груда лута» из структур (deco_loot).
 * Сам блок невидим (рендер содержимого берёт на себя BER), хитбокс —
 * «коврик» высотой 1/16. Ломание/ПКМ выбрасывает лежащие предметы;
 * пустыми руками (не крадучись) блок исчезает — как в оригинале.
 */
public class DecoLootBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public DecoLootBlock(Properties properties) {
        super(properties);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<DecoLootBlock> CODEC = simpleCodec(DecoLootBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecoLootBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return takeLoot(state, level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return takeLoot(state, level, pos, player);
    }
    *///?}

    private static InteractionResult takeLoot(BlockState state, Level level, BlockPos pos, Player player) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DecoLootBlockEntity loot) {
            for (DecoLootBlockEntity.LootEntry entry : loot.getItems()) {
                if (entry.stack().isEmpty()) continue;
                ItemStack stack = entry.stack().copy();
                ItemEntity entity = new ItemEntity(level,
                        pos.getX() + 0.5D + entry.dx(),
                        pos.getY() + entry.dy(),
                        pos.getZ() + 0.5D + entry.dz(),
                        stack);
                level.addFreshEntity(entity);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
