package com.hbm_m.blockentity.network;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code TileEntityCraneSplitter}: teilt ankommende Stapel im Verhaeltnis links:rechts auf. */
public class MachineCraneSplitterBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity {

    /* false: linkes Band bevorzugt, true: rechtes Band bevorzugt */
    private boolean position;
    private byte remaining; // Zaehler bis zum Seitenwechsel

    public byte leftRatio = 1;
    public byte rightRatio = 1;

    public MachineCraneSplitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_SPLITTER_BE.get(), pos, state);
    }

    /** Teilt den Eingangsstapel nach aktuellem Verhaeltnis und internem Zustand in zwei. */
    public ItemStack[] splitStack(ItemStack stack) {
        int left = 0;
        int right = 0;
        int count = stack.getCount();

        if (remaining <= 0) remaining = position ? rightRatio : leftRatio;

        while (count > 0) {
            int toExtract = Math.min(remaining, count);

            remaining -= toExtract;
            count -= toExtract;
            if (position) right += toExtract; else left += toExtract;

            if (remaining <= 0) {
                position = !position;
                remaining = position ? rightRatio : leftRatio;
            }
        }

        ItemStack leftStack = stack.copy();
        ItemStack rightStack = stack.copy();
        leftStack.setCount(left);
        rightStack.setCount(right);

        setChanged();
        return new ItemStack[] { leftStack, rightStack };
    }

    /** Original: {@code networkPackNT} - Verhaeltnis fuer das Look-Overlay. */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        nbt.putBoolean("pos", position);
        nbt.putByte("count", remaining);
        nbt.putByte("left", leftRatio);
        nbt.putByte("right", rightRatio);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        position = nbt.getBoolean("pos");
        remaining = nbt.getByte("count");
        // bestehende Baender mit Verhaeltnis initialisieren
        leftRatio = (byte) Math.max(nbt.getByte("left"), 1);
        rightRatio = (byte) Math.max(nbt.getByte("right"), 1);
    }
}
