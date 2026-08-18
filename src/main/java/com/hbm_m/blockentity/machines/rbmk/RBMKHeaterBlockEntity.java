package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.RBMKHeaterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKHeaterBlockEntity extends RBMKColumnBlockEntity implements MenuProvider {

    public final FluidTank inputTank  = new FluidTank(10_000);
    public final FluidTank outputTank = new FluidTank(10_000);
    private static final double HEAT_TRANSFER_RATE = 0.1;
    private static final double MIN_COLUMN_HEAT    = 100;

    public RBMKHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_HEATER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKHeaterBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;
        if (be.heat > MIN_COLUMN_HEAT && be.inputTank.getFill() > 0) {
            int transfer = (int) Math.min(
                Math.floor((be.heat - MIN_COLUMN_HEAT) * HEAT_TRANSFER_RATE),
                Math.min(be.inputTank.getFill(), be.outputTank.getMaxFill() - be.outputTank.getFill())
            );
            if (transfer > 0) {
                be.inputTank.setFill(be.inputTank.getFill() - transfer);
                be.outputTank.setFill(be.outputTank.getFill() + transfer);
                be.heat -= transfer / HEAT_TRANSFER_RATE;
                be.setChanged();
            }
        }
    }

    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.HEATER; }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_heater"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKHeaterMenu(id, inv, this); }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        inputTank.writeToNBT(tag, "input");
        outputTank.writeToNBT(tag, "output");
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        inputTank.writeToNBT(tag, "input");
        outputTank.writeToNBT(tag, "output");
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inputTank.readFromNBT(tag, "input");
        outputTank.readFromNBT(tag, "output");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        inputTank.readFromNBT(tag, "input");
        outputTank.readFromNBT(tag, "output");
    
    }
    *///?}
}

