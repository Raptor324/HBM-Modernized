package com.hbm_m.blockentity.machines;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 BlockDynamicSlag.TileEntitySlag - a world-placed "puddle" of molten material
 * dumped by a {@link MachineFoundrySlagtapBlockEntity} when its downstream target is full/missing.
 * Holds a single {@link MaterialType} + amount, up to one {@link MaterialStack#BUCKET}'s worth.
 * <p>
 * SCOPE-Vereinfachung: The original slag puddle slowly decays/spreads to neighbouring replaceable
 * blocks via scheduled block ticks. Here it's a static, non-spreading material store - it still
 * accepts pours/flows from the slagtap and can be mined back (see {@code BlockSlag}), but doesn't
 * expand into a growing puddle on its own.
 */
public class SlagBlockEntity extends BlockEntity implements ICrucibleAcceptor {

    @Nullable public MaterialType type = null;
    public int amount = 0;

    public SlagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLAG_BE.get(), pos, state);
    }

    public boolean isFull() {
        return amount >= MaterialStack.BUCKET;
    }

    @Nullable
    public MaterialStack tryAdd(MaterialStack stack) {
        if (type != null && type != stack.type && amount > 0) return stack;

        type = stack.type;
        int space = MaterialStack.BUCKET - amount;
        int toAdd = Math.min(space, stack.amount);
        amount += toAdd;
        stack.amount -= toAdd;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return stack.amount > 0 ? stack : null;
    }

    /* ── ICrucibleAcceptor ──────────────────────────────────────────────── */

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return side == Direction.UP && !isFull() && (type == null || type == stack.type);
    }

    @Override
    public @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return tryAdd(stack);
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return !isFull() && (type == null || type == stack.type);
    }

    @Override
    public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return tryAdd(stack);
    }

    /* ── NBT / sync ─────────────────────────────────────────────────────── */

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        type = tag.contains("mat_type") ? MaterialType.byName(tag.getString("mat_type")) : null;
        amount = tag.getInt("mat_amount");
    }

    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
