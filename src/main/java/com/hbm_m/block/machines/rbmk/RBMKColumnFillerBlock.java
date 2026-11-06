package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RBMKColumnFillerBlock extends Block implements com.hbm_m.interfaces.ILookOverlay {

    private static final VoxelShape SHAPE = Shapes.block();
    private static final VoxelShape SHAPE_WITH_LID = Block.box(0, 0, 0, 16, 20, 16);

    public RBMKColumnFillerBlock(Properties properties) {
        super(properties);
    }

    /**
     * The original's DODD overlay (RBMKBase.printHook → diagnosticPrintHook) reached the core
     * from any column segment via findCore - and the crosshair spends most of its time on a
     * filler segment, not on the base block. Resolve the base and let the column block draw.
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return;
        if (level.getBlockState(basePos).getBlock() instanceof com.hbm_m.interfaces.ILookOverlay ilo) {
            ilo.printHook(guiGraphics, level, basePos);
        }
    }

    /** Scans downward for the real column block this filler belongs to. */
    public static BlockPos findBase(BlockGetter level, BlockPos pos) {
        return findBase(level, pos, false);
    }

    public static BlockPos findBase(BlockGetter level, BlockPos pos, boolean contiguousOnly) {
        int maxHeight = com.hbm_m.handler.rbmk.RBMKDials.getColumnHeight(
                level instanceof Level lvl ? lvl : null) + (contiguousOnly ? 1 : 0);
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
        // Weltgenerierung/Licht fragen die Form vom Arbeitsthread mit dem ServerLevel ab: dort keine Nachbarabfrage
        // (Level.getBlockState wartete auf den Server-Thread -> Verklemmung beim Erzeugen von RBMK-Strukturen).
        if (level instanceof net.minecraft.server.level.ServerLevel sl && !sl.getServer().isSameThread()) return SHAPE;
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
     * does in the original by routing every dummy block's break back to the core.
     */
    private static void destroyWholeColumn(Level level, BlockPos pos, Player player) {
        if (level.isClientSide || com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) return;
        BlockPos basePos = findBase(level, pos, true);
        if (basePos == null) basePos = findBase(level, pos, false);
        if (basePos == null) return;
        level.destroyBlock(basePos, player == null || !player.isCreative(), player);
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
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        BlockPos basePos = findBase(level, pos);
        if (basePos == null) return ItemStack.EMPTY;
        BlockState baseState = level.getBlockState(basePos);
        return baseState.getBlock().getCloneItemStack(level, basePos, baseState);
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

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Explosions and other non-player removals take the column with them, without a drop -
        // upstream BlockDummyable.breakBlock does the same.
        if (!state.is(newState.getBlock()) && newState.isAir()) {
            destroyWholeColumn(level, pos, null);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
