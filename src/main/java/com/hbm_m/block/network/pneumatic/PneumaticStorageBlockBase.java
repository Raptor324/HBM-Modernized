package com.hbm_m.block.network.pneumatic;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Gemeinsame Grundlage der fuenf Geraete des Druckluft-Lagernetzes: voller Wuerfel, Rechtsklick (nicht schleichend)
 * oeffnet die Oberflaeche. audit10: was beim Entfernen herausfaellt, legt jede Unterklasse wie ihr Original fest
 * ({@link #spillFrom()}, {@link #spillOnRemove}).
 */
public abstract class PneumaticStorageBlockBase extends BaseEntityBlock {

    protected PneumaticStorageBlockBase(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Erster Slot, der beim Entfernen herausfaellt (Original {@code breakBlock}-Schleife). */
    protected int spillFrom() {
        return 0;
    }

    /** Ob beim Entfernen ueberhaupt etwas herausfaellt. */
    protected boolean spillOnRemove(Level level, BlockPos pos) {
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock() && spillOnRemove(level, pos)
                && level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.BaseMachineBlockEntity machine) {
            com.hbm_m.block.BlockDropUtil.dropSlots(level, pos, machine.getInventory(), spillFrom(), Integer.MAX_VALUE);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openGui(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openGui(level, pos, player);
    }
    *///?}

    /** Original {@code onBlockActivated}: schleichend passiert nichts (der gehaltene Gegenstand darf handeln). */
    private InteractionResult openGui(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
