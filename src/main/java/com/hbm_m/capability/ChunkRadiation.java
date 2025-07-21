package com.hbm_m.capability;

// import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import com.hbm_m.config.ModClothConfig;
// import com.hbm_m.main.MainRegistry;

public class ChunkRadiation implements IChunkRadiation {
    private float blockRadiation = 0.0F;
    private float ambientRadiation = 0.0f;

    private final float MAX_RAD = ModClothConfig.get().maxRad;

    // public static final String NBT_KEY_BLOCK = "blockRadiation";
    // public static final String NBT_KEY_AMBIENT = "ambientRadiation";

    @Override
    public float getBlockRadiation() {
        return this.blockRadiation;
    }
    
    @Override
    public void setBlockRadiation(float value) {
        this.blockRadiation = Mth.clamp(value, 0, MAX_RAD);
    }

    @Override
    public float getAmbientRadiation() {
        return this.ambientRadiation;
    }

    @Override
    public void setAmbientRadiation(float value) {
        this.ambientRadiation = Mth.clamp(value, 0, MAX_RAD);
    }

    @Override
    public void copyFrom(IChunkRadiation source) {
        this.setBlockRadiation(source.getBlockRadiation());
        this.setAmbientRadiation(source.getAmbientRadiation());
    }


    // @Override
    // public CompoundTag serializeNBT() {
    //     CompoundTag tag = new CompoundTag();
    //     if (this.blockRadiation > 1e-6F) {
    //         tag.putFloat(NBT_KEY_BLOCK, this.blockRadiation);
    //     }
    //     if (this.ambientRadiation > 1e-6F) {
    //         tag.putFloat(NBT_KEY_AMBIENT, this.ambientRadiation);
    //     }
    //     // Логирование можно убрать или оставить для отладки
    //     if (tag.size() > 0) {
    //         MainRegistry.LOGGER.debug("ChunkRadiation: Serializing NBT. block: {}, ambient: {}", this.blockRadiation, this.ambientRadiation);
    //     }
    //     return tag;
    // }

    // @Override
    // public void deserializeNBT(CompoundTag nbt) {
    //     this.blockRadiation = nbt.contains(NBT_KEY_BLOCK) ? nbt.getFloat(NBT_KEY_BLOCK) : 0.0F;
    //     this.ambientRadiation = nbt.contains(NBT_KEY_AMBIENT) ? nbt.getFloat(NBT_KEY_AMBIENT) : 0.0F;
        
    //     if (this.blockRadiation > 1e-6F || this.ambientRadiation > 1e-6F) {
    //         MainRegistry.LOGGER.debug("ChunkRadiation: Deserializing NBT. block: {}, ambient: {}", this.blockRadiation, this.ambientRadiation);
    //     }
    // }
}