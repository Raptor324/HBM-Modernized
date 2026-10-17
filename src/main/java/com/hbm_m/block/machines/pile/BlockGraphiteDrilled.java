package com.hbm_m.block.machines.pile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Port of NEXT {@code com.hbm.blocks.machine.pile.BlockGraphiteDrilled}: the empty pile channel.
 * Right-clicking a channel face with a rod item inserts it; an unshrouded channel also accepts
 * an aluminium shell (shroud) or a graphite ingot (seals back to plain graphite). A screwdriver
 * removes the shell again.
 *
 * <p>NEXT dispatches insertion in {@code useItemOn} and the screwdriver via {@code IToolable};
 * Modernized has no tool interface, so both run in one version-gated use hook with the project's
 * direct item-comparison convention. INTERIM: only the tritium insertion is still unmapped
 * (breeding-product port pending); uranium/pu239/plutonium/source/boron/lithium/detector rods
 * all insert (NEXT :44-92).</p>
 */
public class BlockGraphiteDrilled extends BlockGraphiteDrilledBase {

    public BlockGraphiteDrilled(Properties props) {
        super(props);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return insertIntoChannel(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // NEXT's useItemOn receives the clicked hand's stack and vanilla dispatches main hand
        // first, offhand only on PASS. The 1.21.1 hook has no hand parameter, so emulate that
        // order - a rod or screwdriver held only in the offhand must still insert / remove the
        // shell. PASS paths are side-effect-free, so re-dispatch is harmless.
        for (InteractionHand hand : InteractionHand.values()) {
            InteractionResult result = insertIntoChannel(state, level, pos, player, hand, hit);
            if (result != InteractionResult.PASS) return result;
        }
        return InteractionResult.PASS;
    }
    *///?}

    private InteractionResult insertIntoChannel(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResult.PASS;
        if (!isChannelFace(state, hit.getDirection())) return InteractionResult.PASS;

        // TODO(port): tritium branch (CELL_TRITIUM -> BLOCK_GRAPHITE_TRITIUM, NEXT :74-78)
        // when the breeding-product port lands. Plutonium/source landed with the
        // block_graphite_source port (the stub-era AXIS blockers are gone).
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_URANIUM.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_PU239.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState())
                        .setValue(BlockGraphiteDrilledBase.PU239, true)))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_PLUTONIUM.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_SOURCE.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_SOURCE.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_BORON.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_ROD.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_LITHIUM.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_LITHIUM.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_DETECTOR.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_DETECTOR.get().defaultBlockState())))
            return InteractionResult.SUCCESS;

        if (!state.getValue(SHROUDED)) {

            if (checkInteraction(
                    level, pos, stack, aluminiumShell(), state.setValue(SHROUDED, true)))
                return InteractionResult.SUCCESS;

            if (checkInteraction(
                    level,
                    pos,
                    stack,
                    ModMaterialItems.item(ModMaterials.GRAPHITE, MaterialShape.INGOT),
                    ModBlocks.BLOCK_GRAPHITE.get().defaultBlockState()))
                return InteractionResult.SUCCESS;
        }

        // NEXT: useItemOn PASSED -> item.useOn dispatches IToolable.onScrew (screwdriver).
        return onScrew(state, level, pos, player, hit.getDirection(), hand);
    }

    /** Port of NEXT {@code checkInteraction}: consume 1, set the result block, UPGRADE_PLUG 1.0F/1.0F. */
    private boolean checkInteraction(
            Level level, BlockPos pos, ItemStack stack, @Nullable Item item, BlockState result) {

        if (item == null || stack.getItem() != item) return false;

        stack.shrink(1);
        level.setBlock(pos, result, 3);

        PlatformHooks.playSound(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 1.5D,
                pos.getZ() + 0.5D,
                ModSounds.UPGRADE_PLUG.get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F);

        return true;
    }

    /** Port of NEXT {@code onScrew} override: screwdriver on a shrouded channel face removes the shell. */
    @Override
    protected InteractionResult onScrew(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            @Nullable Direction side,
            InteractionHand hand) {

        if (!isScrewdriver(player.getItemInHand(hand))) return InteractionResult.PASS;

        if (!level.isClientSide() && isChannelFace(state, side) && state.getValue(SHROUDED)) {
            level.setBlock(pos, state.setValue(SHROUDED, false), 3);
            PlatformHooks.playSound(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.5D,
                    pos.getZ() + 0.5D,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    0.85F);

            ejectItem(level, pos, side, new ItemStack(aluminiumShell()));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
