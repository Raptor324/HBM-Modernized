package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code TileEntityFoundryBasin}: tiefes Giessbecken fuer die grossen Formen (Groesse 1), nur von oben befuellbar. */
public class MachineFoundryBasinBlockEntity extends MachineFoundryCastingBaseBlockEntity implements IRenderFoundry {

    public MachineFoundryBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_BASIN_BE.get(), pos, state, 2);
    }

    @Override
    public int getMoldSize() {
        return 1;
    }

    /* Basin can't accept sideways flowing */
    @Override public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override @Nullable public MaterialStack flow(Level world, BlockPos pos, Direction side, MaterialStack stack) { return stack; }

    @Override
    public boolean shouldRender() {
        return this.type != null && this.amount > 0;
    }

    @Override
    public double getMoltenLevel() {
        return 0.125 + this.amount * 0.75D / this.getCapacity();
    }

    @Override
    public NTMMaterial getMat() {
        return this.type;
    }

    @Override public double minX() { return 0.125D; }
    @Override public double maxX() { return 0.875D; }
    @Override public double minZ() { return 0.125D; }
    @Override public double maxZ() { return 0.875D; }
    @Override public double moldHeight() { return 0.13D; }
    @Override public double outHeight() { return 0.875D; }
}
