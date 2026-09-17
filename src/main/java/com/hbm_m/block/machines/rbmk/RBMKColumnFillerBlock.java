package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible solid filler placed in the two block positions above an {@link RBMKColumnBlock} so the
 * column has a real 1x3 hitbox (collision + selection), matching the original 1.7.10 mod. The
 * column's own {@code BlockEntityRenderer} already draws the full 3-tall visual from the base
 * position; before this block existed, the upper 2/3 of every column was walk-through air with no
 * collision or click target at all, and mining/placing there had no relation to the column below.
 * <p>
 * Every interaction (right-click, breaking) is forwarded to the real column at the base position,
 * found by scanning downward (bounded by {@code RBMKDials.getColumnHeight}).
 */
public class RBMKColumnFillerBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.block();
    /** Matches the original's {@code getCollisionBoundingBoxFromPool} extra 0.25 lid height. */
    private static final VoxelShape SHAPE_WITH_LID = Shapes.box(0, 0, 0, 1, 1.25, 1);

    public RBMKColumnFillerBlock(Properties props) {
        super(props);
    }

    /** Scans downward for the real column block this filler belongs to. */
    private static BlockPos findBase(BlockGetter level, BlockPos pos) {
        BlockPos cursor = pos;
        // getColumnHeight() ignores its parameter (a static dial constant); null is safe here and
        // avoids needing a real Level in contexts where only a BlockGetter is available.
        int maxHeight = com.hbm_m.handler.rbmk.RBMKDials.getColumnHeight(null);
        for (int i = 0; i < maxHeight; i++) {
            cursor = cursor.below();
            if (level.getBlockState(cursor).getBlock() instanceof RBMKColumnBlock) {
                return cursor;
            }
        }
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Selection outline follows the collision box, so the lid is click- and highlight-able too. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getCollisionShape(state, level, pos, ctx);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // 1:1 with the original's getCollisionBoundingBoxFromPool: the lid slab pokes up an extra
        // 0.25 blocks above the column's topmost segment. Only the topmost filler (nothing of this
        // column continues above it) gets the taller box - the ones below stay a plain full block.
        if (!(level.getBlockState(pos.above()).getBlock() instanceof RBMKColumnFillerBlock)) {
            BlockPos basePos = findBase(level, pos);
            if (basePos != null && level.getBlockEntity(basePos) instanceof RBMKColumnBlockEntity col && col.hasLid()) {
                return SHAPE_WITH_LID;
            }
        }
        return SHAPE;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {

        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return InteractionResult.PASS;
        BlockState baseState = level.getBlockState(basePos);
        return baseState.use(level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), basePos, hit.isInside()));
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return InteractionResult.PASS;
        BlockState baseState = level.getBlockState(basePos);
        return baseState.useWithoutItem(level, player, new BlockHitResult(hit.getLocation(), hit.getDirection(), basePos, hit.isInside()));
        }
    *///?}


    /**
     * Breaking any part of the column breaks the whole column, exactly as {@code BlockDummyable}
     * does in the original by routing every dummy block's break back to the core. Without this the
     * filler was the only thing that broke: mining the top of a column punched a hole in its
     * hitbox and left the base - and the full-height visual - standing, and the column could only
     * ever be removed by digging out its bottom block.
     * <p>
     * Destroying the base runs {@link RBMKColumnBlock#onRemove}, which drops the lid, handles the
     * column's own teardown and clears every remaining filler above it (this one included).
     */
    private static void destroyWholeColumn(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return;
        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return;
        level.destroyBlock(basePos, !player.isCreative(), player);
    }

    /** Pick-block on the filler hands over the column item, not nothing. */
    //? if < 1.21.1 {
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return ItemStack.EMPTY;
        BlockState baseState = level.getBlockState(basePos);
        return baseState.getBlock().getCloneItemStack(level, basePos, baseState);
    }
    //?} else {
    /*@Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return ItemStack.EMPTY;
        BlockState baseState = level.getBlockState(basePos);
        return baseState.getBlock().getCloneItemStack(level, basePos, baseState, includeData);
    }
    *///?}

    /** Mining time follows the real column, so the filler cannot be dug faster than the block it stands for. */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return super.getDestroyProgress(state, player, level, pos);
        BlockState baseState = level.getBlockState(basePos);
        return baseState.getDestroyProgress(player, level, basePos);
    }

    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        destroyWholeColumn(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        destroyWholeColumn(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}
}
