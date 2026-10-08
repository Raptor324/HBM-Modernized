package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
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
 * 1:1 {@code MachineOreSlopper}: {@code getDimensions {3,0,3,3,1,1}}, {@code getOffset 3}; Anschluesse an beiden
 * Stirnseiten, links/rechts der Mitte und an den vier Ecken der Laengsseiten. Gezeichnet vom
 * {@code MachineOreSlopperRenderer}.
 */
public class MachineOreSlopperBlock extends DummyableMachineBlock {

    public MachineOreSlopperBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(3, 0, 3, 3, 1, 1)
                .extra(3, 0, 0)
                .extra(-3, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .extra(2, 0, 1)
                .extra(2, 0, -1)
                .extra(-2, 0, 1)
                .extra(-2, 0, -1)
                .placementOffset(3)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    /**
     * audit13/w16d: Original {@code bounding} - Kollision und Trefferbox jeder Zelle (keine Vollbloecke). Drehung 1:1
     * {@code getAABBRotationOffset} mit {@code rot = dir.getRotation(UP)} (Forge: NORTH->EAST, SOUTH->WEST, WEST->NORTH,
     * EAST->SOUTH) ueber {@link MultiblockStructureHelper#boundingMasters}; Kern- und Zellform macht DummyableMachineBlock.
     */
    private static final java.util.Map<net.minecraft.core.Direction, net.minecraft.world.phys.shapes.VoxelShape> BOUNDING =
            MultiblockStructureHelper.boundingMasters(new double[][] {
                    { -3.5, 0, -1.5, 3.5, 1, 1.5 },
                    { 0.5, 1, -1.5, 3.5, 3.25, 1.5 },
                    { -2.25, 1, -1.5, 0.25, 3.25, -0.75 },
                    { -2.25, 1, 0.75, 0.25, 3.25, 1.5 },
                    { -2.25, 1, -1.5, -2, 3.25, 1.5 },
                    { 0, 1, -1.5, 0.25, 3.25, 1.5 },
                    { -2, 1, -0.75, 0, 2, 0.75 },
                    { -3.25, 1, -1, -2.25, 3, 1 } });

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return BOUNDING.get(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineOreSlopperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ORE_SLOPPER_BE.get(),
                (lvl, pos, st, be) -> MachineOreSlopperBlockEntity.tick(lvl, pos, st, (MachineOreSlopperBlockEntity) be));
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
        if (!level.isClientSide() && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MachineOreSlopperBlockEntity machine) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, machine, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineOreSlopperBlock> CODEC = simpleCodec(MachineOreSlopperBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
