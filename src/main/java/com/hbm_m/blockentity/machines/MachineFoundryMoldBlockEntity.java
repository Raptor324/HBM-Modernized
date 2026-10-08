package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.NTMMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code TileEntityFoundryMold}: flache Giessform fuer die kleinen Formen (Groesse 0). */
public class MachineFoundryMoldBlockEntity extends MachineFoundryCastingBaseBlockEntity implements IRenderFoundry {

    public MachineFoundryMoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_MOLD_BE.get(), pos, state, 2);
    }

    @Override
    public int getMoldSize() {
        return 0;
    }

    @Override
    public boolean shouldRender() {
        return this.type != null && this.amount > 0;
    }

    @Override
    public double getMoltenLevel() {
        return 0.125 + this.amount * 0.25D / this.getCapacity();
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
    @Override public double outHeight() { return 0.25D; }
}
