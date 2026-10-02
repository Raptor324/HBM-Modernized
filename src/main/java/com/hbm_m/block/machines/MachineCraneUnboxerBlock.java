package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.network.CraneBaseBlock;
import com.hbm_m.block.network.IEnterablePackageBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineCraneUnboxerBlockEntity;
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
 * 1:1 {@code CraneUnboxer}: nimmt Pakete nur ueber die Ausgangsseite an (Teile gar nicht), entpackt sie in seine 21 Plaetze
 * und gibt den Inhalt am Eingang auf das Band. Komparator: Fuellstand.
 */
public class MachineCraneUnboxerBlock extends CraneBaseBlock implements IEnterablePackageBlock {

    public MachineCraneUnboxerBlock(Properties properties) {
        super(properties);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineCraneUnboxerBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CRANE_UNBOXER_BE.get(), MachineCraneUnboxerBlockEntity::tick);
    }

    @Override
    public boolean canPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, MovingConveyorPackageEntity entity) {
        return getOutputSide(level, pos) == dir;
    }

    @Override
    public void onPackageEnter(Level level, BlockPos pos, MovingConveyorPackageEntity entity) {
        if (!(level.getBlockEntity(pos) instanceof MachineCraneUnboxerBlockEntity unboxer)) return;
        //? if forge {
        net.minecraftforge.items.IItemHandler view = new com.hbm_m.blockentity.network.CraneInventoryUtil.SlotView(unboxer.getInventory(),
                new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 });
        for (ItemStack stack : entity.getContents()) {
            if (stack.isEmpty()) continue;
            ItemStack remainder = com.hbm_m.blockentity.network.CraneInventoryUtil.addToInventory(view, stack.copy());
            if (!remainder.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder.copy()));
        }
        //?}
        unboxer.setChanged();
    }

    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        //? if forge {
        if (level.getBlockEntity(pos) instanceof MachineCraneUnboxerBlockEntity be) return com.hbm_m.blockentity.network.CraneInventoryUtil.comparator(be.getInventory());
        //?}
        return 0;
    }

    @Override protected int[] getDropRange() { return new int[] { 0, 23 }; }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCraneUnboxerBlock> CODEC = simpleCodec(MachineCraneUnboxerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
