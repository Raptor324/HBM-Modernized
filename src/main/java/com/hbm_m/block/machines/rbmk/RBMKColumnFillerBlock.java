package com.hbm_m.block.machines.rbmk;

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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return InteractionResult.PASS;
        BlockState baseState = level.getBlockState(basePos);
        return baseState.use(level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), basePos, hit.isInside()));
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);
        if (level.isClientSide) return;
        BlockPos basePos = findBase(level, pos);
        if (basePos != null && level.getBlockState(basePos).getBlock() instanceof RBMKColumnBlock) {
            // Cascade: breaking any filler destroys the whole column (base + remaining fillers),
            // matching vanilla multi-part blocks (doors, beds, tall flowers).
            level.destroyBlock(basePos, !player.isCreative());
        }
    }
}
