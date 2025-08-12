package com.hbm_m.block.entity;

import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MachineAssemblerPartBlockEntity extends BlockEntity {

    public static Supplier<BlockEntityType<MachineAssemblerPartBlockEntity>> TYPE_SUPPLIER;
        
    private BlockPos controllerPos;

    public MachineAssemblerPartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(TYPE_SUPPLIER.get(), pPos, pBlockState);
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        if (this.controllerPos != null) {
            nbt.put("controller", NbtUtils.writeBlockPos(this.controllerPos));
        }
        super.saveAdditional(nbt);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("controller")) {
            this.controllerPos = NbtUtils.readBlockPos(nbt.getCompound("controller"));
            // MainRegistry.LOGGER.info("[CLIENT] Loaded controllerPos from NBT. Controller is at {}", this.controllerPos);

             if (level != null && level.isClientSide) {
                 level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                //  MainRegistry.LOGGER.info("[CLIENT] Requested block update after loading NBT.");
             }
        }
    }
}