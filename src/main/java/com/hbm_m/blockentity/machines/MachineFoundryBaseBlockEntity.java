package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityFoundryBase}: Basis aller Giesserei-Bloecke (Rinnen, Formen, Becken, Tanks). Haelt genau ein
 * Material in Quanten und gibt Aenderungen an den Client weiter.
 */
public abstract class MachineFoundryBaseBlockEntity extends BaseHbmBlockEntity implements ICrucibleAcceptor {

    public NTMMaterial type;
    protected NTMMaterial lastType;
    public int amount;
    protected int lastAmount;

    protected MachineFoundryBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFoundryBaseBlockEntity be) {
        be.updateEntity();
    }

    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (this.lastType != this.type || this.lastAmount != this.amount) {
            markForUpdate();
            this.lastType = this.type;
            this.lastAmount = this.amount;
        }
    }

    /** Original {@code markBlockForUpdate}. */
    public void markForUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.type = Mats.matById.get(nbt.getInt("type"));
        this.amount = nbt.getInt("amount");
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putInt("type", this.type == null ? -1 : this.type.id);
        nbt.putInt("amount", this.amount);
    }

    public abstract int getCapacity();

    /**
     * Standard check for testing if this material stack can be added to the casting block. Checks:<br>
     * - type matching<br>
     * - amount being at max<br>
     */
    public boolean standardCheck(Level world, BlockPos pos, Direction side, MaterialStack stack) {
        if (this.type != null && this.type != stack.material && this.amount > 0) return false; //reject if there's already a different material
        if (this.amount >= this.getCapacity()) return false; //reject if the buffer is already full
        return true;
    }

    /**
     * Standardized adding of material via pouring or flowing. Does:<br>
     * - sets material to match the input
     * - adds the amount, not exceeding the maximum
     * - returns the amount that cannot be added
     */
    @Nullable
    public MaterialStack standardAdd(Level world, BlockPos pos, Direction side, MaterialStack stack) {
        this.type = stack.material;

        if (stack.amount + this.amount <= this.getCapacity()) {
            this.amount += stack.amount;
            return null;
        }

        int required = this.getCapacity() - this.amount;
        this.amount = this.getCapacity();

        stack.amount -= required;

        return stack;
    }

    /** Standard check with no additional limitations added */
    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, MaterialStack stack) {
        return this.standardCheck(world, pos, side, stack);
    }

    /** Standard flow, no special handling required */
    @Override
    @Nullable
    public MaterialStack flow(Level world, BlockPos pos, Direction side, MaterialStack stack) {
        return standardAdd(world, pos, side, stack);
    }

    /** Standard check, but with the additional limitation that the only valid source direction is UP */
    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) {
        if (side != Direction.UP) return false;
        return this.standardCheck(world, pos, side, stack);
    }

    /** Standard flow, no special handling required */
    @Override
    @Nullable
    public MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) {
        return standardAdd(world, pos, side, stack);
    }
}
