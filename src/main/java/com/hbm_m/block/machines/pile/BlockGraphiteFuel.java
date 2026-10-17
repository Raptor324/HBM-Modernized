package com.hbm_m.block.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.pile.BlockEntityPileFuel;
import com.hbm_m.interfaces.IBlowable;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Port of NEXT {@code com.hbm.blocks.machine.pile.BlockGraphiteFuel}: the CP-1 uranium fuel
 * channel, the only pile block whose BE heats up, transmutes and eventually breeds into
 * BLOCK_GRAPHITE_PLUTONIUM.
 *
 * <p>This block carries its own {@link #getTicker} override on purpose: the base
 * {@link BlockGraphiteDrilledTE#getTicker} type-checks {@code PILE_NEUTRONDETECTOR} and returns
 * null for any other type, so without this override the fuel block would SILENTLY NEVER TICK
 * (no heat, no progress, no neutrons, no completion, no overheat - no crash).</p>
 *
 * <p>NEXT dispatches tools via {@code IToolable.onScrew}; Modernized has no tool interface, so
 * the version-gated use hook compares held items directly (screwdriver extract, hand drill
 * readout). NEXT's 4-arg {@code getAnalogOutputSignal(state, level, pos, direction)} overload
 * is 1.21.4+ API - both Modernized targets use the 3-arg form (MachinePWRControllerBlock
 * convention). Implements {@link IBlowable} (NEXT verbatim); no fan exists yet, so
 * {@link #applyFan} is unreachable scaffold.</p>
 */
public class BlockGraphiteFuel extends BlockGraphiteDrilledTE implements IBlowable {

    public BlockGraphiteFuel(Properties props) {
        super(props);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PU239);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntityPileFuel pile = new BlockEntityPileFuel(pos, state);
        if (state.getValue(PU239)) pile.progress = BlockEntityPileFuel.maxProgress() - 1000;
        return pile;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPileFuel pile)) return 0;
        return Mth.clamp((pile.progress * 15) / (BlockEntityPileFuel.maxProgress() - 1000), 0, 15);
    }

    /**
     * The base class' ticker type-guard only knows the detector; fuel needs its own mapping or
     * the block silently never ticks (see class javadoc).
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type == ModBlockEntities.PILE_FUEL.get()) {
            // Hand-written ticker check: createTickerHelper lives on BaseEntityBlock, these blocks extend Block.
            return (BlockEntityTicker<T>) (BlockEntityTicker<BlockEntityPileFuel>) (lvl, pos, st, be) -> be.tickServer();
        }
        return null;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onScrew(state, level, pos, player, hit.getDirection(), hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // NEXT dispatches tools per hand (vanilla useItemOn: main hand first, offhand only on
        // PASS). The 1.21.1 hook has no hand parameter, so emulate the same order here - a
        // screwdriver or hand drill held only in the offhand must still drive extraction or
        // readout. PASS paths are side-effect-free, so re-dispatch is harmless.
        for (InteractionHand hand : InteractionHand.values()) {
            InteractionResult result = onScrew(state, level, pos, player, hit.getDirection(), hand);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }
    *///?}

    /**
     * Port of NEXT {@code onScrew} override: screwdriver on a channel face ejects the rod back
     * to a drilled channel; the hand drill prints the CP-1 assembly readout. NEXT returns true
     * for any recognized tool unconditionally (even off-face), anything else PASSes - same net
     * behavior via sidedSuccess/PASS.
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
        boolean handDrill = held.getItem() == ModItems.HAND_DRILL.get()
                || held.getItem() == ModItems.HAND_DRILL_DESH.get();
        if (!screwdriver && !handDrill) return InteractionResult.PASS;

        if (!level.isClientSide()) {

            if (screwdriver && isChannelFace(state, side)) {
                Item inserted = getInsertedItem(state);
                level.setBlock(
                        pos,
                        copyShape(
                                state, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()),
                        3);
                ejectItem(level, pos, side, inserted == null ? ItemStack.EMPTY : new ItemStack(inserted));
            }

            if (handDrill && level.getBlockEntity(pos) instanceof BlockEntityPileFuel pile) {
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.shared.cp1FuelAssembly",
                                        pos.getX(),
                                        pos.getY(),
                                        pos.getZ())
                                .withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.block.graphiteFuel.heat",
                                        pile.heat + "/" + BlockEntityPileFuel.maxHeat)
                                .withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.block.graphiteFuel.depletion",
                                        pile.progress + "/" + BlockEntityPileFuel.maxProgress())
                                .withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(
                        Component.translatable("desc.shared.flux", pile.lastNeutrons)
                                .withStyle(ChatFormatting.YELLOW));
                if (state.getValue(PU239)) {
                    player.sendSystemMessage(
                            Component.translatable("desc.block.graphiteFuel.pu239Rich")
                                    .withStyle(ChatFormatting.DARK_GREEN));
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Item getInsertedItem(BlockState state) {
        return state.getValue(PU239)
                ? ModItems.PILE_ROD_PU239.get()
                : ModItems.PILE_ROD_URANIUM.get();
    }

    /**
     * NEXT registers the pile family flammable via {@code FlammableBlocks.set(BLOCK_GRAPHITE_FUEL,
     * 30, 5)} → {@code Services.PLATFORM.setFlammable} → the FireBlock odds maps
     * (FlammableBlocks.java:18, NeoForgePlatformHelper.java:290-292). The direct
     * {@code FireBlock#setFlammable} call is not portable: it is {@code private} on 1.20.1-forge
     * (Forge patch moved the odds maps to instance fields). Modernized instead overrides the
     * block-state extension pair that vanilla fire actually queries on BOTH targets
     * (checkBurnOut → getFlammability, getIgniteOdds → getFireSpreadSpeed; house convention:
     * BlockCoke) — getFireSpreadSpeed = encouragement 30, getFlammability = burn-away odds 5.
     */
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 30;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 5;
    }

    /** Port of NEXT {@code applyFan}: cooling airflow sheds 2.5% of stored heat per blow. */
    @Override
    public void applyFan(Level level, BlockPos pos, Direction dir, int dist) {
        if (level.getBlockEntity(pos) instanceof BlockEntityPileFuel pile)
            pile.heat -= pile.heat * 0.025;
    }
}
