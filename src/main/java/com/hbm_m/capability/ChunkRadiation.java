package com.hbm_m.capability;

// Реализация IChunkRadiation для хранения ambient-радиации чанка (1.7.10 parity — одно Float на чанк).
import net.minecraft.util.Mth;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.interfaces.IChunkRadiation;

public class ChunkRadiation implements IChunkRadiation {
    private float ambientRadiation = 0.0f;

    private final float MAX_RAD = ModClothConfig.get().maxRad;

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
        this.setAmbientRadiation(source.getAmbientRadiation());
    }
}
