package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

    private static BlockPos findBase(BlockGetter level, BlockPos pos) {
        return findBase(level, pos, false);
    }

    /**
     * Scans downward for the real column block this filler belongs to.
     *
     * <p>contiguousOnly stops at the first block that is neither a filler nor the column. The
     * break path needs that: without it the scan walks through the gap left by a column that is
     * already gone and reaches a second reactor stacked underneath.
     */
    private static BlockPos findBase(BlockGetter level, BlockPos pos, boolean contiguousOnly) {
        // The break path scans one deeper: it starts from a filler and has to reach the column
        // through the whole stack, while the interaction paths start one segment lower.
        int maxHeight = com.hbm_m.handler.rbmk.RBMKDials.getColumnHeight(
                level instanceof net.minecraft.world.level.Level lvl ? lvl : null) + (contiguousOnly ? 1 : 0);
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < maxHeight; i++) {
            cursor.move(net.minecraft.core.Direction.DOWN);
            Block below = level.getBlockState(cursor).getBlock();
            if (below instanceof RBMKColumnBlock) return cursor.immutable();
            if (contiguousOnly && !(below instanceof RBMKColumnFillerBlock)) return null;
        }
        return null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
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
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {

        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return InteractionResult.PASS;
        BlockState baseState = level.getBlockState(basePos);
        return baseState.use(level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), basePos, hit.isInside()));
        }
    *///?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return InteractionResult.PASS;
        BlockState baseState = level.getBlockState(basePos);
        return baseState.useWithoutItem(level, player, new BlockHitResult(hit.getLocation(), hit.getDirection(), basePos, hit.isInside()));
        }
    //?}


    // The class contract is that breaking a filler breaks the column, but this only ever called
    // super: the filler has no loot table, so mining one left a permanent hole in the reactor.
    private static void breakColumn(Level level, BlockPos pos, boolean drop) {
        if (level.isClientSide || com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) return;
        BlockPos base = findBase(level, pos, true);
        if (base != null) {
            level.destroyBlock(base, drop);
        }
    }

    //? if < 1.21.1 {
    /*@Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        breakColumn(level, pos, !player.getAbilities().instabuild);
        super.playerWillDestroy(level, pos, state, player);
    }
    *///?} else {
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        breakColumn(level, pos, !player.getAbilities().instabuild);
        return super.playerWillDestroy(level, pos, state, player);
    }
    //?}

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Explosions and other non-player removals take the column with them, without a drop -
        // upstream BlockDummyable.breakBlock does the same.
        if (!state.is(newState.getBlock()) && newState.isAir()) {
            breakColumn(level, pos, false);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
