package com.hbm_m.interfaces;

// Интерфейс capability для хранения ambient-радиации чанка.
// В 1.7.10 ChunkRadiationHandlerSimple хранит одно Float на чанк (нет разделения на block/ambient):
// источники переэмиттят радиацию в это значение через собственный scheduled-tick каждый раз.
public interface IChunkRadiation {
    // Ambient-радиация чанка (источники + события + spread/decay).
    float getAmbientRadiation();
    void setAmbientRadiation(float value);

    // Копирование данных от другого capability (например, при respawn'е игрока).
    void copyFrom(IChunkRadiation source);
}
