package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code TileEntityFoundryBasin}: tiefes Giessbecken fuer die grossen Formen (Groesse 1), nur von oben befuellbar. */
public class MachineFoundryBasinBlockEntity extends MachineFoundryCastingBaseBlockEntity implements IRenderFoundry, com.hbm_m.interfaces.ICopiable {

    public MachineFoundryBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_BASIN_BE.get(), pos, state, 2);
    }

    protected MachineFoundryBasinBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2);
    }

    @Override
    public int getMoldSize() {
        return 1;
    }

    /* Basin can't accept sideways flowing */
    @Override public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override @Nullable public MaterialStack flow(Level world, BlockPos pos, Direction side, MaterialStack stack) { return stack; }

    /* Pour-Seite: das Basen-Default (MachineFoundryBaseBlockEntity#canAcceptPartialPour/#pour)
       gate't bereits auf Direction.UP - hier keine zusaetzlichen Overrides noetig. */

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

    // Methods for MachineFoundryRenderer
    public ItemStack getMoldSlot() { return slots[0]; }
    public ItemStack getOutputSlot() { return slots[1]; }
    public float getFillLevel() { return this.getCapacity() > 0 ? (float) this.amount / this.getCapacity() : 0f; }
    public int getFillColor() { return this.type != null ? (0xFF000000 | this.type.moltenColor) : 0xFFC18336; }
    public float getSurfaceRange() { return 0.75f; }
    public float getMoldHeight() { return (float) moldHeight(); }
    public float getOutputHeight() { return (float) outHeight(); }

    @Override
    public net.minecraft.nbt.CompoundTag getSettings(Level level, BlockPos pos) {
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        if (type != null) nbt.putIntArray("matFilter", new int[]{ type.id });
        return nbt;
    }

    @Override
    public void pasteSettings(net.minecraft.nbt.CompoundTag nbt, int index, Level level, net.minecraft.world.entity.player.Player player, BlockPos pos) { }
}
