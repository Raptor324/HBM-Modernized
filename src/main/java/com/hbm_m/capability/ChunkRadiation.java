package com.hbm_m.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import com.hbm_m.config.RadiationConfig;
import com.hbm_m.main.MainRegistry;

public class ChunkRadiation implements IChunkRadiation {
    private float blockRadiation = 0.0F;

    @Override
    public float getRadiation() {
        return blockRadiation;
    }

    @Override
    public float getBlockRadiation() {
        return blockRadiation;
    }
    
    @Override
    public void setBlockRadiation(float blockRadiation) {
        float clamped = Mth.clamp(blockRadiation, 0, RadiationConfig.maxRad);
        this.blockRadiation = clamped;
    }

    @Override
    public void copyFrom(IChunkRadiation source) {
        this.blockRadiation = source.getBlockRadiation();
    }

    public static final String NBT_KEY_BLOCK = "blockRadiation";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(NBT_KEY_BLOCK, blockRadiation);
        if (blockRadiation > 1e-8F) {
            MainRegistry.LOGGER.debug("ChunkRadiation: Serializing NBT. blockRadiation: {}", blockRadiation);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        blockRadiation = nbt.getFloat(NBT_KEY_BLOCK);
        if (blockRadiation > 1e-8F) {
            MainRegistry.LOGGER.debug("ChunkRadiation: Deserializing NBT. blockRadiation: {}", blockRadiation);
        }
    }
}