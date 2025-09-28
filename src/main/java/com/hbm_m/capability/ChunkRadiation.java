package com.hbm_m.capability;

// Реализация IChunkRadiation для хранения данных о радиации в чанке.
import net.minecraft.util.Mth;
import com.hbm_m.config.ModClothConfig;

public class ChunkRadiation implements IChunkRadiation {
    private float blockRadiation = 0.0F;
    private float ambientRadiation = 0.0f;

    private final float MAX_RAD = ModClothConfig.get().maxRad;


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
}