package com.hbm_m.blockentity.decorations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityBobble} / {@code TileEntitySnowglobe} / {@code TileEntityPlushie}: der Figurentyp (Ordnungszahl,
 * als Byte "type" gespeichert). Plueschtiere haben zusaetzlich den clientseitigen Quetsch-Zaehler.
 */
public class TrinketBlockEntity extends BaseHbmBlockEntity {

    public int type;
    public int squishTimer;

    public TrinketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRINKET.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TrinketBlockEntity te) {
        if (te.squishTimer > 0) te.squishTimer--;
    }

    public void setType(int type) {
        this.type = type;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        this.type = Math.abs(nbt.getByte("type"));
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putByte("type", (byte) type);
    }
}
