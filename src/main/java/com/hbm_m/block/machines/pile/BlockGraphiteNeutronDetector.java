package com.hbm_m.block.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.pile.BlockEntityPileNeutronDetector;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import com.hbm_m.platform.LevelHooks;

/**
 * Port of NEXT {@code com.hbm.blocks.machine.pile.BlockGraphiteNeutronDetector}: the CP-1
 * neutron detector. The detector block entity accumulates neutrons and calls
 * {@link #triggerRods} to withdraw/re-insert the whole control-rod run once flux crosses
 * {@code maxNeutrons}; its sneak screwdriver readout and defuser limit adjustment are the
 * hand-held counterparts.
 *
 * <p>NEXT dispatches the tools via {@code IToolable.onScrew}; Modernized has no tool interface,
 * so {@link #onScrew} runs from the version-gated use hook with direct item checks, plus the
 * item-side dispatch in ScrewdriverItem/ItemDefuser for sneak clicks (vanilla skips the block
 * use hooks whenever the player sneaks with a non-empty hand). The translatable keys are kept
 * verbatim from NEXT ({@code desc.shared.*}).</p>
 */
public class BlockGraphiteNeutronDetector extends BlockGraphiteDrilledTE {

    public BlockGraphiteNeutronDetector(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WITHDRAWN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPileNeutronDetector(pos, state);
    }

    /** Port of NEXT {@code triggerRods}: toggle this detector and the whole rod run along the axis. */
    public void triggerRods(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        boolean withdrawn = state.getValue(WITHDRAWN);

        level.setBlock(pos, state.setValue(WITHDRAWN, !withdrawn), 3);

        LevelHooks.playSound(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                ModSounds.TOOL_TECH_BLEEP.get(),
                SoundSource.BLOCKS,
                0.02F,
                1.0F);

        Direction dir =
                Direction.fromAxisAndDirection(
                        state.getValue(AXIS), Direction.AxisDirection.NEGATIVE);
        BlockGraphiteRod.toggleRun(level, pos, state, dir, !withdrawn);
        BlockGraphiteRod.toggleRun(level, pos, state, dir.getOpposite(), !withdrawn);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onScrew(state, level, pos, player, hit.getDirection(), hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // NEXT's useItemOn receives the clicked hand's stack and vanilla dispatches main hand
        // first, offhand only on PASS. The 1.21.1 hook has no hand parameter, so emulate that
        // order - a screwdriver or defuser held only in the offhand must still eject / read the
        // flux / adjust the limit. PASS paths are side-effect-free, so re-dispatch is harmless.
        for (InteractionHand hand : InteractionHand.values()) {
            InteractionResult result = onScrew(state, level, pos, player, hit.getDirection(), hand);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }
    *///?}

    /**
     * Port of NEXT {@code onScrew} override: screwdriver ejects the detector back to a drilled
     * channel (channel face, no sneak) or prints the CP-1 assembly readout (sneak); the defuser
     * adjusts the auto-trigger threshold. NEXT only receives tool items here (IToolable
     * dispatch), so anything else PASSes - same net behavior.
     */
    @Override
    protected InteractionResult onScrew(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            @Nullable Direction side,
            InteractionHand hand) {

        ItemStack held = player.getItemInHand(hand);
        boolean screwdriver = isScrewdriver(held);
        boolean defuser = held.getItem() == ModItems.DEFUSER.get();
        if (!screwdriver && !defuser) return InteractionResult.PASS;

        if (!level.isClientSide()) {

            if (screwdriver) {

                if (!player.isShiftKeyDown()) {
                    if (isChannelFace(state, side)) {
                        level.setBlock(
                                pos,
                                copyShape(
                                        state,
                                        ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()),
                                3);
                        Item inserted = getInsertedItem();
                        ejectItem(
                                level,
                                pos,
                                side,
                                inserted == null ? ItemStack.EMPTY : new ItemStack(inserted));
                    }
                } else if (level.getBlockEntity(pos)
                        instanceof BlockEntityPileNeutronDetector pile) {
                    player.sendSystemMessage(
                            Component.translatable(
                                            "desc.shared.cp1FuelAssembly",
                                            pos.getX(),
                                            pos.getY(),
                                            pos.getZ())
                                    .withStyle(ChatFormatting.GOLD));
                    player.sendSystemMessage(
                            Component.translatable(
                                            "desc.shared.flux",
                                            pile.lastNeutrons + "/" + pile.maxNeutrons)
                                    .withStyle(ChatFormatting.YELLOW));
                }
            }

            if (defuser
                    && level.getBlockEntity(pos) instanceof BlockEntityPileNeutronDetector pile) {
                if (player.isShiftKeyDown()) {
                    if (pile.maxNeutrons > 1) pile.maxNeutrons--;
                } else {
                    pile.maxNeutrons++;
                }
                pile.setChanged();
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.PILE_ROD_DETECTOR.get();
    }
}
