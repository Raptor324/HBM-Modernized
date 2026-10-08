package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineMiningDrillBlockEntity;
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
 * 1:1 {@code MachineExcavator}: {@code getDimensions {3,0,3,3,3,3}} plus die drei Beine aus {@code getAllDimensions}
 * (zwei 2x2-Pfeiler hinten, eine Querwand vorne, je drei nach unten), {@code getOffset 3}, {@code getHeightOffset 3}.
 * Strom/Fluid an vier Zellen eine Ebene ueber dem Kern. Gezeichnet vom {@code MachineMiningDrillRenderer}.
 */
public class MachineMiningDrillBlock extends DummyableMachineBlock {

    public MachineMiningDrillBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(3, 0, 3, 3, 3, 3)
                .box(-1, 3, 3, -2, 3, -2)
                .box(-1, 3, 3, -2, -2, 3)
                .box(-1, 3, -2, 3, 3, 3)
                .extra(3, 1, 1)
                .extra(3, 1, -1)
                .extra(0, 1, 3)
                .extra(0, 1, -3)
                .placementOffset(3)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return 3;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineMiningDrillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MINING_DRILL_BE.get(),
                (lvl, pos, st, be) -> MachineMiningDrillBlockEntity.tick(lvl, pos, st, (MachineMiningDrillBlockEntity) be));
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
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MachineMiningDrillBlockEntity machine) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, machine, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineMiningDrillBlock> CODEC = simpleCodec(MachineMiningDrillBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
