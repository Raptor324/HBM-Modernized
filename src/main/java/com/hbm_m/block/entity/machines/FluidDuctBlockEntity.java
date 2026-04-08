package com.hbm_m.block.entity.machines;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machines.FluidDuctBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Block Entity for the Fluid Duct. Stores the fluid type that this duct transports
 * and handles tick-based fluid transfer between connected machines and pipes.
 *
 * Each pipe has a small internal buffer (1000 mB). Every tick it:
 *   1. Pulls fluid from adjacent machines (not other pipes) into its buffer.
 *   2. Pushes fluid from its buffer to all connected neighbors (pipes and machines).
 * This creates a natural flow:  Source machine → pipe chain → Sink machine.
 */
public class FluidDuctBlockEntity extends BlockEntity {

    private static final String NBT_FLUID_TYPE = "FluidType";
    private static final int CAPACITY = 1000;
    private static final int TRANSFER_RATE = 100; // mB per tick per connection

    private Fluid fluidType = Fluids.EMPTY;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return !stack.isEmpty() && stack.getFluid() == fluidType;
        }
    };
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> tank);

    public FluidDuctBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_DUCT_BE.get(), pos, state);
    }

    public Fluid getFluidType() {
        return fluidType;
    }

    public void setFluidType(Fluid fluid) {
        setFluidTypeSilent(fluid);
        syncFluidToClients();
    }

    /** Sets type without notifying clients (batch identifier / network pass). */
    public void setFluidTypeSilent(Fluid fluid) {
        this.fluidType = fluid != null ? fluid : Fluids.EMPTY;
        tank.drain(tank.getFluidAmount(), FluidAction.EXECUTE);
        setChanged();
    }

    public void syncFluidToClients() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public FluidTank getTank() {
        return tank;
    }

    // --- Tick-based fluid transport ---

    public static void tick(Level level, BlockPos pos, BlockState state, FluidDuctBlockEntity entity) {
        if (level.isClientSide) return;
        if (entity.fluidType == Fluids.EMPTY) return;

        // Collect connected directions from blockstate
        List<Direction> connections = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            if (state.getValue(FluidDuctBlock.PROPERTY_BY_DIRECTION.get(dir))) {
                connections.add(dir);
            }
        }
        if (connections.isEmpty()) return;

        // Phase 1: Pull fluid from connected machines (skip other pipes — they push themselves)
        for (Direction dir : connections) {
            if (entity.tank.getFluidAmount() >= CAPACITY) break;

            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null || neighbor instanceof FluidDuctBlockEntity) continue;

            LazyOptional<IFluidHandler> cap = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite());
            cap.ifPresent(handler -> {
                int space = CAPACITY - entity.tank.getFluidAmount();
                int pullAmount = Math.min(TRANSFER_RATE, space);
                if (pullAmount <= 0) return;

                FluidStack simulated = handler.drain(new FluidStack(entity.fluidType, pullAmount), FluidAction.SIMULATE);
                if (!simulated.isEmpty() && simulated.getFluid() == entity.fluidType) {
                    FluidStack drained = handler.drain(
                            new FluidStack(entity.fluidType, simulated.getAmount()), FluidAction.EXECUTE);
                    if (!drained.isEmpty()) {
                        entity.tank.fill(drained, FluidAction.EXECUTE);
                    }
                }
            });
        }

        // Phase 2: Push fluid to all connected neighbors (pipes and machines)
        if (entity.tank.getFluidAmount() <= 0) return;

        for (Direction dir : connections) {
            if (entity.tank.getFluidAmount() <= 0) break;

            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null) continue;

            LazyOptional<IFluidHandler> cap = neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite());
            cap.ifPresent(handler -> {
                int pushAmount = Math.min(TRANSFER_RATE, entity.tank.getFluidAmount());
                if (pushAmount <= 0) return;

                FluidStack toPush = new FluidStack(entity.fluidType, pushAmount);
                int filled = handler.fill(toPush, FluidAction.EXECUTE);
                if (filled > 0) {
                    entity.tank.drain(filled, FluidAction.EXECUTE);
                }
            });
        }
    }

    // --- NBT Save/Load ---

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(fluidType);
        if (loc != null) {
            tag.putString(NBT_FLUID_TYPE, loc.toString());
        }
        tank.writeToNBT(tag);
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_FLUID_TYPE)) {
            Fluid f = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(tag.getString(NBT_FLUID_TYPE)));
            this.fluidType = f != null ? f : Fluids.EMPTY;
        }
        tank.readFromNBT(tag);
    }

    // --- Client sync ---

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- Capabilities ---

    @Override
    public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && fluidType != Fluids.EMPTY) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }
}
