package com.hbm_m.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.main.MainRegistry;

public class ChunkRadiationProvider implements ICapabilitySerializable<CompoundTag> {
    public static Capability<IChunkRadiation> CHUNK_RADIATION_CAPABILITY = null;

    private IChunkRadiation chunkRadiation = null;
    private final LazyOptional<IChunkRadiation> optional = LazyOptional.of(this::createChunkRadiation);

    private IChunkRadiation createChunkRadiation() {
        if (this.chunkRadiation == null) {
            this.chunkRadiation = new ChunkRadiation();
        }
        return this.chunkRadiation;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CHUNK_RADIATION_CAPABILITY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        IChunkRadiation radiation = createChunkRadiation();
        CompoundTag nbt = radiation.serializeNBT();
        if (radiation.getBlockRadiation() > 1e-8F) {
            MainRegistry.LOGGER.info("ChunkRadiationProvider: Serializing NBT for chunk. blockRadiation: {}", radiation.getBlockRadiation());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        IChunkRadiation radiation = createChunkRadiation();
        radiation.deserializeNBT(nbt);
        if (radiation.getBlockRadiation() > 1e-8F) {
            MainRegistry.LOGGER.info("ChunkRadiationProvider: Deserializing NBT for chunk. blockRadiation: {}", radiation.getBlockRadiation());
        }
    }
}