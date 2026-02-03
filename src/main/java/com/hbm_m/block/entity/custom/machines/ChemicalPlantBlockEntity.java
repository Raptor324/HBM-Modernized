package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChemicalPlantBlockEntity extends BlockEntity {

    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    private final FluidTank fluidTank = new FluidTank(8_000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.of(() -> fluidTank);

    public ChemicalPlantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_PLANT_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ChemicalPlantBlockEntity entity) {
        entity.prevAnim = entity.anim;

        // Placeholder animation (client-side only). Replace with real process-driven anim later.
        if (level.isClientSide) {
            entity.anim += 0.05F;
            if (entity.anim > (float) (Math.PI * 2.0)) {
                entity.anim -= (float) (Math.PI * 2.0);
            }
        }
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    public FluidStack getFluid() {
        return fluidTank.getFluid();
    }

    public float getFluidFillFraction() {
        if (fluidTank.getCapacity() <= 0) return 0.0F;
        return Math.min(1.0F, Math.max(0.0F, fluidTank.getFluidAmount() / (float) fluidTank.getCapacity()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Fluid")) {
            fluidTank.readFromNBT(tag.getCompound("Fluid"));
        }
        anim = tag.getFloat("Anim");
        prevAnim = tag.getFloat("PrevAnim");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Fluid", fluidTank.writeToNBT(new CompoundTag()));
        tag.putFloat("Anim", anim);
        tag.putFloat("PrevAnim", prevAnim);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        lazyFluidHandler.invalidate();
    }
}
