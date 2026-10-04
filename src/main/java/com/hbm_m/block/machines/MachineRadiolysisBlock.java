package com.hbm_m.block.machines;


import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineRadiolysisBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineRadiolysis}: {@code getDimensions {2,0,1,1,1,1}}, {@code getOffset 0}; die vier Seitenzellen des Kerns
 * sind Anschluesse (ProxyCombo mit Inventar, Strom und Fluid). Oeffnen markiert den Spieler fuer die FBI-Razzia.
 * Gezeichnet vom {@code RadiolysisRenderer}.
 */
public class MachineRadiolysisBlock extends DummyableMachineBlock {

    public MachineRadiolysisBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 1, 1, 1)
                .extra(1, 0, 0)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineRadiolysisBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADIOLYSIS_BE.get(),
                (lvl, pos, st, be) -> MachineRadiolysisBlockEntity.tick(lvl, pos, st, be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    *///?}

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MachineRadiolysisBlockEntity radiolysis) {
            com.hbm_m.handler.BossSpawnHandler.markFBI((ServerPlayer) player);
            MenuRegistry.openExtendedMenu((ServerPlayer) player, radiolysis, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineRadiolysisBlock> CODEC = simpleCodec(MachineRadiolysisBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
