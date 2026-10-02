package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.network.CraneBaseBlock;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.block.network.IEnterablePackageBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineCraneBoxerBlockEntity;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.entity.conveyor.MovingConveyorPackageEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code CraneBoxer}: nimmt Teile ueber den Eingang und Pakete von jeder Seite (entpackt) in seine 21 Plaetze auf und
 * verpackt sie; was nicht passt, faellt heraus. Komparator: Fuellstand.
 */
public class MachineCraneBoxerBlock extends CraneBaseBlock implements IEnterableBlock, IEnterablePackageBlock {

    public MachineCraneBoxerBlock(Properties properties) {
        super(properties);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineCraneBoxerBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CRANE_BOXER_BE.get(), MachineCraneBoxerBlockEntity::tick);
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, MovingConveyorItemEntity entity) {
        return getInputSide(level, pos) == dir;
    }

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity entity) {
        entity.discard();
        if (!(level.getBlockEntity(pos) instanceof MachineCraneBoxerBlockEntity boxer)) return;
        //? if forge {
        ItemStack remainder = com.hbm_m.blockentity.network.CraneInventoryUtil.addToInventory(boxer.getInventory(), entity.getItem().copy());
        if (!remainder.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder.copy()));
        //?}
        boxer.setChanged();
    }

    @Override
    public void onPackageEnter(Level level, BlockPos pos, MovingConveyorPackageEntity entity) {
        if (!(level.getBlockEntity(pos) instanceof MachineCraneBoxerBlockEntity boxer)) return;
        //? if forge {
        for (ItemStack stack : entity.getContents()) {
            if (stack.isEmpty()) continue;
            ItemStack remainder = com.hbm_m.blockentity.network.CraneInventoryUtil.addToInventory(boxer.getInventory(), stack.copy());
            if (!remainder.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder.copy()));
        }
        //?}
        boxer.setChanged();
    }

    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        //? if forge {
        if (level.getBlockEntity(pos) instanceof MachineCraneBoxerBlockEntity be) return com.hbm_m.blockentity.network.CraneInventoryUtil.comparator(be.getInventory());
        //?}
        return 0;
    }

    @Override protected int[] getDropRange() { return new int[] { 0, 21 }; }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCraneBoxerBlock> CODEC = simpleCodec(MachineCraneBoxerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
