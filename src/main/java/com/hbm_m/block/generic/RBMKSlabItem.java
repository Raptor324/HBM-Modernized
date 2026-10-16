package com.hbm_m.block.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Item form of the single {@link RBMKSlabBlock}, 1:1 with CE's {@code ItemRBMKSlab}.
 *
 * <p>Using it on a panel slab that is already there replaces that slab with the four-pixel double
 * variant instead of placing a second block next to it - the same "slabs merge" interaction vanilla
 * slabs have, except the two heights are separate blocks here.</p>
 */
public class RBMKSlabItem extends BlockItem {

    public RBMKSlabItem(RBMKSlabBlock block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        BlockState state = level.getBlockState(clicked);

        // Merge only into our own single slab; anything else falls through to normal placement.
        if (state.is(getBlock()) && getBlock() instanceof RBMKSlabBlock single && !single.isDouble) {
            Block doubleBlock = single.getCounterpart();
            if (doubleBlock != null) {
                BlockState merged = doubleBlock.defaultBlockState();

                // CE checks the double block's collision box is clear before swapping it in, so a
                // player or mob standing on the panel cannot be shoved into it.
                if (level.isUnobstructed(merged, clicked, net.minecraft.world.phys.shapes.CollisionContext.empty())) {
                    if (!level.isClientSide) {
                        level.setBlock(clicked, merged, Block.UPDATE_ALL);
                    }
                    SoundType sound = merged.getSoundType();
                    level.playSound(ctx.getPlayer(), clicked, sound.getPlaceSound(), SoundSource.BLOCKS,
                            (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

                    ItemStack stack = ctx.getItemInHand();
                    Player player = ctx.getPlayer();
                    if (player == null || !player.getAbilities().instabuild) stack.shrink(1);

                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                return InteractionResult.FAIL;
            }
        }

        return super.useOn(ctx);
    }

    /**
     * Placing against the top face of an existing single slab is handled by {@link #useOn} above;
     * everything else uses the default behaviour.
     */
    @Override
    protected boolean canPlace(BlockPlaceContext ctx, BlockState state) {
        return super.canPlace(ctx, state);
    }
}
