package com.hbm_m.block.machines.pile;

import com.hbm_m.item.ModItems;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import com.hbm_m.platform.LevelHooks;

/**
 * Port of NEXT {@code com.hbm.blocks.machine.pile.BlockGraphiteRod}: the boron control rod
 * seated in a pile channel. Clicking a channel face toggles {@link #WITHDRAWN} for the whole
 * unbroken run of rods marching both ways along the clicked face ({@link #toggleRun}) - the
 * manual override for the detector's automatic trigger.
 *
 * <p>NEXT reaches this through {@code useWithoutItem}; Modernized uses the project's
 * version-gated use hook. NEXT additionally ejects an inserted rod via screwdriver+sneak
 * (base {@code onScrew} through the IToolable item dispatch); Modernized reaches the same
 * path through its item-side dispatch (ScrewdriverItem/ItemDefuser useOn ->
 * onScrewForTool -> this inherited base {@code onScrew}), which - like NEXT's - is never
 * sneak-gated.</p>
 */
public class BlockGraphiteRod extends BlockGraphiteDrilledBase {

    public BlockGraphiteRod(Properties props) {
        super(props);
    }

    /** NEXT {@code toggleRun}: marches along {@code dir} while identical rods continue the run. */
    static void toggleRun(
            Level level, BlockPos pos, BlockState match, Direction dir, boolean withdrawn) {
        BlockPos.MutableBlockPos cursor = pos.mutable().move(dir);
        while (true) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.getBlock() instanceof BlockGraphiteRod)) return;
            if (state.getValue(AXIS) != match.getValue(AXIS)) return;
            if (state.getValue(SHROUDED) != match.getValue(SHROUDED)) return;
            if (state.getValue(WITHDRAWN) != match.getValue(WITHDRAWN)) return;
            level.setBlock(cursor, state.setValue(WITHDRAWN, withdrawn), 3);
            cursor.move(dir);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WITHDRAWN);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return toggleRunFromClick(state, level, pos, player, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggleRunFromClick(state, level, pos, player, hit);
    }
    *///?}

    /** Port of NEXT {@code useWithoutItem}: sneak PASS, channel face only, LEVER_CLICK, run toggles both ways. */
    private InteractionResult toggleRunFromClick(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        Direction side = hit.getDirection();
        if (!isChannelFace(state, side)) return InteractionResult.PASS;

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean withdrawn = state.getValue(WITHDRAWN);
        level.setBlock(pos, state.setValue(WITHDRAWN, !withdrawn), 3);

        // Vanilla LEVER_CLICK: a SoundEvent on 1.20.1, a Holder on 1.21.1 - route through PlatformHooks.
        LevelHooks.playSound(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                0.3F,
                withdrawn ? 0.65F : 0.75F);

        toggleRun(level, pos, state, side, !withdrawn);
        toggleRun(level, pos, state, side.getOpposite(), !withdrawn);

        return InteractionResult.SUCCESS;
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.PILE_ROD_BORON.get();
    }
}
