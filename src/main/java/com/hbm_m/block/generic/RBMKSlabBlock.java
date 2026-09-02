package com.hbm_m.block.generic;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 port of CE's {@code BlockRBMKSlab} - the thin RBMK panel slab.
 *
 * <p>Nothing about it matches a vanilla slab: it is a <b>two-pixel</b> plate, and stacking a second
 * one on top produces a separate four-pixel "double" block rather than a full cube. CE registers
 * the pair as {@code deco_rbmk_panel_slab2} (the one you craft and carry) and
 * {@code deco_rbmk_panel_slab4} (hidden from the creative menu, only ever produced by stacking, and
 * dropping two singles when broken). Both names are kept so world data and resource packs line up
 * with CE.</p>
 *
 * @see RBMKSlabItem for the stacking behaviour
 */
public class RBMKSlabBlock extends Block {

    /** 4px when this is the stacked variant, 2px otherwise. */
    public final boolean isDouble;

    /** Single -> its double, double -> its single. Resolved lazily; the pair is circular. */
    private Supplier<Block> counterpart = () -> null;

    private final VoxelShape shape;

    public RBMKSlabBlock(boolean isDouble, Properties props) {
        super(props);
        this.isDouble = isDouble;
        this.shape = box(0, 0, 0, 16, isDouble ? 4 : 2, 16);
    }

    public RBMKSlabBlock setCounterpart(Supplier<Block> counterpart) {
        this.counterpart = counterpart;
        return this;
    }

    /** The double form of a single slab, or the single form of a double one. */
    public Block getCounterpart() {
        return counterpart.get();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    /**
     * CE marks the slab {@code isReplaceable}, which is what lets a second slab be placed "into"
     * the first one's space. Only the single form is replaceable, and only by its own kind - so a
     * stray block placement cannot silently delete a panel.
     */
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        if (isDouble) return false;
        return ctx.getItemInHand().getItem() instanceof RBMKSlabItem item
                && item.getBlock() == this
                && ctx.getClickedFace() == Direction.UP;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }
}
