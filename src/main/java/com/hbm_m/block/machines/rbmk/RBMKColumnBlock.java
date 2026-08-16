package com.hbm_m.block.machines.rbmk;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.rbmk.RBMKLidItem;
import com.hbm_m.item.rbmk.RBMKToolItem;
import com.hbm_m.item.tools_and_armor.ScrewdriverItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public abstract class RBMKColumnBlock extends BaseEntityBlock {

    /** Set to false to suppress lid drops globally (e.g. during world gen). */
    public static boolean dropLids = true;

    protected RBMKColumnBlock(Properties props) {
        super(props);
    }

    // ─── Block visuals ────────────────────────────────────────────────────────

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // ENTITYBLOCK_ANIMATED: the BESR (RBMKColumnRenderer) renders the full column height.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RBMKColumnBlockEntity col && col.hasLid())
            return Shapes.box(0, 0, 0, 1, 1.25, 1);
        return Shapes.block();
    }

    // ─── Interaction ─────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RBMKColumnBlockEntity col)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // RBMK linking tool: store this column's position for the next console right-click
        if (held.getItem() instanceof RBMKToolItem) {
            RBMKToolItem.storeColumnPosition(held, level, pos, player);
            return InteractionResult.SUCCESS;
        }

        // Screwdriver removes lid
        if (held.getItem() instanceof ScrewdriverItem && col.hasLid() && col.isLidRemovable()) {
            if (!level.isClientSide) dropLid(col, level, pos);
            col.setLidState(0);
            return InteractionResult.SUCCESS;
        }

        // Place lid with lid item
        if (held.getItem() instanceof RBMKLidItem lid && col.isLidRemovable() && !col.hasLid()) {
            col.setLidState(lid.lidType);
            if (!player.isCreative()) held.shrink(1);
            return InteractionResult.SUCCESS;
        }

        // Open GUI (if this block has a menu)
        if (!player.isShiftKeyDown() && col instanceof MenuProvider mp) {
            NetworkHooks.openScreen((ServerPlayer) player, mp, pos);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // ─── Placement ────────────────────────────────────────────────────────────

    /**
     * Places invisible {@link RBMKColumnFillerBlock} filler in the 2 positions above this column
     * so the whole thing has a real 1x3 hitbox (see that class's javadoc) - matches the original
     * 1.7.10 mod, which is a true 3-tall multiblock rather than a 1-tall block with an overdrawn
     * visual.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        int height = RBMKDials.getColumnHeight(level);
        BlockState filler = ModBlocks.RBMK_COLUMN_FILLER.get().defaultBlockState();
        for (int i = 1; i < height; i++) {
            level.setBlock(pos.above(i), filler, 3);
        }
    }

    // ─── Break ────────────────────────────────────────────────────────────────

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RBMKColumnBlockEntity col) {
                onColumnRemoved(col, level, pos);
            }
            if (!level.isClientSide) {
                int height = RBMKDials.getColumnHeight(level);
                for (int i = 1; i < height; i++) {
                    BlockPos fillerPos = pos.above(i);
                    if (level.getBlockState(fillerPos).getBlock() instanceof RBMKColumnFillerBlock) {
                        level.removeBlock(fillerPos, false);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    protected void onColumnRemoved(RBMKColumnBlockEntity col, Level level, BlockPos pos) {
        if (!level.isClientSide && dropLids && col.hasLid() && col.isLidRemovable()) {
            dropLid(col, level, pos);
        }
    }

    private static void dropLid(RBMKColumnBlockEntity col, Level level, BlockPos pos) {
        Item lidItem = col.getLidState() == 2 ? ModItems.RBMK_LID_GLASS.get() : ModItems.RBMK_LID.get();
        int topY = RBMKDials.getColumnHeight(level);
        Block.popResource(level, pos.above(topY), new ItemStack(lidItem));
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);
}
