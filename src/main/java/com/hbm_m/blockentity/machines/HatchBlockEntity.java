package com.hbm_m.blockentity.machines;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.SealBlocks;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityHatch}: verifies controller liveness and frame integrity;
 * automatically dissolves if the controller or the surrounding frame is broken.
 */
public class HatchBlockEntity extends BlockEntity {

    protected BlockPos controller = BlockPos.ZERO;

    public HatchBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SEAL_HATCH.get(), pos, state);
    }

    public HatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setControllerPos(BlockPos p) {
        this.controller = p.immutable();
        setChanged();
    }

    public BlockPos getControllerPos() {
        return controller;
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, HatchBlockEntity te) {
        if (te.controller == null || te.controller.equals(BlockPos.ZERO)) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }
        if (!world.isLoaded(te.controller)) {
            if (pos.closerThan(te.controller, 32)) {
                return; // Adjacent chunk not loaded yet
            }
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }
        if (!world.getBlockState(te.controller).is(ModBlocks.SEAL_CONTROLLER.get())) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        } else if (SealBlocks.Controller.getFrameSize(world, te.controller) == 0) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    @Override
    //? if < 1.21.1 {
    public void load(CompoundTag nbt) {
    //?} else {
    /*public void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        //? if < 1.21.1 {
        super.load(nbt);
        //?} else {
        /*super.loadAdditional(nbt, registries);
        *///?}
        controller = new BlockPos(nbt.getInt("x1"), nbt.getInt("y1"), nbt.getInt("z1"));
    }

    @Override
    //? if < 1.21.1 {
    protected void saveAdditional(CompoundTag nbt) {
    //?} else {
    /*protected void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    *///?}
        //? if < 1.21.1 {
        super.saveAdditional(nbt);
        //?} else {
        /*super.saveAdditional(nbt, registries);
        *///?}
        nbt.putInt("x1", controller.getX());
        nbt.putInt("y1", controller.getY());
        nbt.putInt("z1", controller.getZ());
    }
}
