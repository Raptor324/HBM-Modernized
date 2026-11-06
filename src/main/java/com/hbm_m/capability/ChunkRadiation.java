package com.hbm_m.capability;

// Реализация IChunkRadiation для хранения ambient-радиации чанка (1.7.10 parity — одно Float на чанк).
import net.minecraft.util.Mth;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.IChunkRadiation;

public class ChunkRadiation implements IChunkRadiation {
    private float ambientRadiation = 0.0f;

    public static final String NBT_KEY_AMBIENT = "ambientRadiation";

    @Override
    public float getAmbientRadiation() {
        return this.ambientRadiation;
    }

    @Override
    public void setAmbientRadiation(float value) {
        float maxRad = ModClothConfig.get().maxRad;
        this.ambientRadiation = Mth.clamp(value, 0, maxRad);
    }

    @Override
    public void copyFrom(IChunkRadiation source) {
        this.setAmbientRadiation(source.getAmbientRadiation());
    }

    public net.minecraft.nbt.CompoundTag saveNBT(net.minecraft.nbt.CompoundTag tag) {
        if (this.ambientRadiation > 1e-6F) {
            tag.putFloat(NBT_KEY_AMBIENT, this.ambientRadiation);
        }
        return tag;
    }

    public void loadNBT(net.minecraft.nbt.CompoundTag tag) {
        if (tag != null && tag.contains(NBT_KEY_AMBIENT)) {
            setAmbientRadiation(tag.getFloat(NBT_KEY_AMBIENT));
        } else {
            setAmbientRadiation(0F);
        }
    }
}
