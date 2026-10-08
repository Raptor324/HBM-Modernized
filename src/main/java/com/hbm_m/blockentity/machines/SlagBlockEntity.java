package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.NTMMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockDynamicSlag.TileEntitySlag}: Schlackepfuetze aus einem Material, bis 16 Bloecke Menge; die Hoehe
 * des Blocks folgt der Menge. Fliessen/Zerlaufen macht der Block per geplantem Tick.
 */
public class SlagBlockEntity extends BaseHbmBlockEntity {

    public NTMMaterial mat;
    public int amount;
    public static int maxAmount = MaterialShapes.BLOCK.q(16);

    public SlagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLAG_BE.get(), pos, state);
    }

    public void markForUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.mat = nbt.contains("mat") ? Mats.matById.get(nbt.getInt("mat")) : null;
        this.amount = nbt.getInt("amount");
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        if (this.mat != null) nbt.putInt("mat", this.mat.id);
        nbt.putInt("amount", this.amount);
    }
}
