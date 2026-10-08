package com.hbm_m.block.machines;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineHydrotreaterBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineHydrotreater}: {@code getDimensions {6,0,1,1,1,1}} (3x3x7), {@code getOffset 1},
 * vier Anschlusszellen ({@code makeExtra}) in den Ecken der untersten Ebene.
 * w16b: vorher Einzelblock-Platzhalter (MultiblockStructureStubs) - nur der Kern hatte eine Hitbox.
 */
public class MachineHydrotreaterBlock extends DummyableMachineBlock {

    public MachineHydrotreaterBlock(BlockBehaviour.Properties p) {
        super(p);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(6, 0, 1, 1, 1, 1)
                .extra(1, 0, 1)
                .extra(1, 0, -1)
                .extra(-1, 0, 1)
                .extra(-1, 0, -1)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new MachineHydrotreaterBlockEntity(p, s); }

    //? if < 1.21.1 {
    @Override public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult r) {
        if (pl.isShiftKeyDown()) return InteractionResult.sidedSuccess(l.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return openMenu(l, p, pl);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult r) {
        if (pl.isShiftKeyDown()) return InteractionResult.sidedSuccess(l.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return openMenu(l, p, pl);
    }
    *///?}

    private InteractionResult openMenu(Level l, BlockPos p, Player pl) {
        if (!l.isClientSide() && l.getBlockEntity(p) instanceof MenuProvider mp) MenuRegistry.openExtendedMenu((ServerPlayer) pl, mp, buf -> buf.writeBlockPos(p));
        return InteractionResult.sidedSuccess(l.isClientSide());
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return createTickerHelper(t, ModBlockEntities.HYDROTREATER_BE.get(), MachineHydrotreaterBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineHydrotreaterBlock> CODEC = simpleCodec(MachineHydrotreaterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
