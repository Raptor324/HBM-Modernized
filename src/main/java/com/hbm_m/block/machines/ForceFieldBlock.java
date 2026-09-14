package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.ForceFieldBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Port von {@code MachineForceField} (1.7.10, Block-ID {@code machine_forcefield}).
 *
 * <p>Wie im Original oeffnet ein Rechtsklick die Oberflaeche, ein Schleichklick tut nichts.
 * Ueber dem Block steigen Funken auf, solange das Feld laeuft, und Rauch, waehrend es abkuehlt.</p>
 *
 * <p>Der Block hat wie im Original <b>kein eigenes Modell</b> ({@code getRenderType() == -1}):
 * Sockel, Kopf und die Feldkugel zeichnet
 * {@link com.hbm_m.client.render.implementations.ForceFieldRenderer}.</p>
 */
public class ForceFieldBlock extends BaseEntityBlock {

    public ForceFieldBlock(Properties properties) {
        super(properties);
    }

    /** Original: {@code getRenderType() == -1} - gezeichnet wird ausschliesslich im Renderer. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForceFieldBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FORCE_FIELD_BE.get(),
                (lvl, pos, st, be) -> ForceFieldBlockEntity.tick(lvl, pos, st, (ForceFieldBlockEntity) be));
    }

    /** 1:1-Port von {@code randomDisplayTick}: vier Teilchen zwei Bloecke ueber dem Emitter. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof ForceFieldBlockEntity be)) return;

        boolean running = be.isOn() && be.getCooldown() == 0 && be.getEnergyStored() > 0;
        if (!running && be.getCooldown() == 0) return;

        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextFloat();
            double y = pos.getY() + 2.0D;
            double z = pos.getZ() + random.nextFloat();

            if (running) {
                level.addParticle(ParticleTypes.CRIT, x, y, z, 0.0D, 0.0D, 0.0D);
            } else {
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.BaseMachineBlockEntity machine) {
            machine.dropInventoryContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Original: beim Schleichen passiert nichts.
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide());

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide());

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    *///?}
}
