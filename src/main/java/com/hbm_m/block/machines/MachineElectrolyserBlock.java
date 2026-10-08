package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity;
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
 * 1:1 {@code MachineElectrolyser}: {@code getAllDimensions} mit Sockel, Mittelsaeule, fuenf Zellenreihen und fuenf
 * Deckbruecken, {@code getOffset 5}; je drei Anschluesse an beiden Enden. Oeffnet die zuletzt gewaehlte GUI-Seite.
 */
public class MachineElectrolyserBlock extends DummyableMachineBlock {

    public MachineElectrolyserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(0, 0, 5, 5, 1, 3)
                .box(2, -1, 5, 5, 1, 1)
                .box(3, -3, 5, 5, 0, 0)
                .box(3, -1, 4, -4, -3, 3)
                .box(3, -1, 2, -2, -3, 3)
                .box(3, -1, 0, 0, -3, 3)
                .box(3, -1, -2, 2, -3, 3)
                .box(3, -1, -4, 4, -3, 3)
                .boxAt(4, 3, 0, 0, 0, 0, 0, -1, 2)
                .boxAt(2, 3, 0, 0, 0, 0, 0, -1, 2)
                .boxAt(0, 3, 0, 0, 0, 0, 0, -1, 2)
                .boxAt(-2, 3, 0, 0, 0, 0, 0, -1, 2)
                .boxAt(-4, 3, 0, 0, 0, 0, 0, -1, 2)
                .extra(-5, 0, 0)
                .extra(-5, 0, 1)
                .extra(-5, 0, -1)
                .extra(5, 0, 0)
                .extra(5, 0, 1)
                .extra(5, 0, -1)
                .placementOffset(5)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineElectrolyserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ELECTROLYSER_BE.get(), MachineElectrolyserBlockEntity::tick);
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

    /** Original {@code standardOpenBehavior(..., -1)}: ohne Schleichen die zuletzt gewaehlte Seite. */
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        if (level.getBlockEntity(pos) instanceof MachineElectrolyserBlockEntity e) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, e, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineElectrolyserBlock> CODEC = simpleCodec(MachineElectrolyserBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
