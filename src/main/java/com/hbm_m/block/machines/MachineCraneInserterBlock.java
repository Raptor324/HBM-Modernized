package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.network.CraneBaseBlock;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.block.network.IEnterablePackageBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineCraneInserterBlockEntity;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.entity.conveyor.MovingConveyorPackageEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code CraneInserter}: Teile und Pakete kommen nur ueber die Eingangsseite herein und werden sofort in das Inventar an
 * der Ausgangsseite eingesetzt (ausser bei Redstonesignal); was nicht passt, geht in den eigenen Puffer, der Rest faellt
 * heraus, sofern der Zerstoerer aus ist. Komparator: Fuellstand des Puffers.
 */
public class MachineCraneInserterBlock extends CraneBaseBlock implements IEnterableBlock, IEnterablePackageBlock {

    public MachineCraneInserterBlock(Properties properties) {
        super(properties);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineCraneInserterBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CRANE_INSERTER_BE.get(), MachineCraneInserterBlockEntity::tick);
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, MovingConveyorItemEntity entity) {
        return level.getBlockState(pos).getValue(FACING) == dir;
    }

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity entity) {
        entity.discard();
        if (!(level.getBlockEntity(pos) instanceof MachineCraneInserterBlockEntity inserter)) return;
        ItemStack toAdd = entity.getItem().copy();
        if (toAdd.isEmpty()) return;
        inserter.accept(level, pos, toAdd);
    }

    @Override
    public void onPackageEnter(Level level, BlockPos pos, MovingConveyorPackageEntity entity) {
        if (!(level.getBlockEntity(pos) instanceof MachineCraneInserterBlockEntity inserter)) return;
        ItemStack[] toAdd = entity.getContents();
        if (toAdd == null || toAdd.length == 0) return;
        inserter.acceptAll(level, pos, toAdd);
    }

    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        //? if forge {
        if (level.getBlockEntity(pos) instanceof MachineCraneInserterBlockEntity be) return com.hbm_m.blockentity.network.CraneInventoryUtil.comparator(be.getInventory());
        //?}
        return 0;
    }

    @Override protected int[] getDropRange() { return new int[] { 0, 21 }; }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCraneInserterBlock> CODEC = simpleCodec(MachineCraneInserterBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
