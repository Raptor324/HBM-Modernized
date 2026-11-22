package com.hbm_m.block.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.pile.BlockEntityPileBreedingFuel;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Port of NEXT {@code com.hbm.blocks.machine.pile.BlockGraphiteBreedingFuel}: the CP-1 lithium
 * breeding channel. Receives neutron flux into its BE ({@link BlockEntityPileBreedingFuel}),
 * breeds to maxProgress and is then swapped into BLOCK_GRAPHITE_TRITIUM by the BE.
 *
 * <p>This block carries its own {@link #getTicker} override on purpose: the base
 * {@link BlockGraphiteDrilledTE#getTicker} type-checks {@code PILE_NEUTRONDETECTOR} and returns
 * null for any other type, so without this override the block would SILENTLY NEVER TICK (same
 * reason as {@link BlockGraphiteFuel}).</p>
 *
 * <p>NEXT dispatches tools via {@code IToolable.onScrew}; Modernized has no tool interface, so
 * the version-gated use hook compares held items directly (screwdriver extract, hand drill
 * readout) exactly like {@link BlockGraphiteFuel}. NEXT's onScrew returns {@code true}
 * unconditionally (even for other tools); here non-tools PASS like the fuel port.</p>
 */
public class BlockGraphiteBreedingFuel extends BlockGraphiteDrilledTE {

    public BlockGraphiteBreedingFuel(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPileBreedingFuel(pos, state);
    }

    /**
     * The base class' ticker type-guard only knows the detector; the breeding fuel needs its own
     * mapping or the block silently never ticks (see class javadoc).
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type == ModBlockEntities.PILE_BREEDINGFUEL.get()) {
            // Hand-written ticker check: createTickerHelper lives on BaseEntityBlock, these blocks extend Block.
            return (BlockEntityTicker<T>) (BlockEntityTicker<BlockEntityPileBreedingFuel>) (lvl, pos, st, be) -> be.tickServer();
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
     * Port of NEXT {@code onScrew} override: screwdriver on a channel face ejects the lithium rod
     * back to a drilled channel; the hand drill prints the CP-1 assembly readout (depletion as a
     * NEXT-literal DEPLETION line, not a lang key). NEXT returns true for any recognized tool
     * unconditionally (even off-face), anything else PASSes - same net behavior via
     * sidedSuccess/PASS.
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

            if (handDrill && level.getBlockEntity(pos) instanceof BlockEntityPileBreedingFuel pile) {
                player.sendSystemMessage(
                        Component.translatable(
                                        "desc.shared.cp1FuelAssembly",
                                        pos.getX(),
                                        pos.getY(),
                                        pos.getZ())
                                .withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(
                        Component.literal(
                                        "DEPLETION: "
                                                + pile.progress
                                                + "/"
                                                + BlockEntityPileBreedingFuel.maxProgress())
                                .withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(
                        Component.translatable("desc.shared.flux", pile.lastNeutrons)
                                .withStyle(ChatFormatting.YELLOW));
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Item getInsertedItem() {
        return ModItems.PILE_ROD_LITHIUM.get();
    }

    /**
     * NEXT registers the pile family flammable via {@code FlammableBlocks.set(BLOCK_GRAPHITE_LITHIUM,
     * 30, 5)} (FlammableBlocks.java:22) - same override pair as {@link BlockGraphiteFuel}, because
     * the direct {@code FireBlock#setFlammable} call is not portable (private on 1.20.1-forge).
     */
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 30;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return 5;
    }
}
