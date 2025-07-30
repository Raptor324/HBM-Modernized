package com.hbm_m.capability;

public interface IChunkRadiation {
    // Получает ОБЩУЮ радиацию (блоки + эмбиентная)
    // float getTotalRadiation();

    // Радиация от блоков
    float getBlockRadiation();
    void setBlockRadiation(float value);

    // Радиация, пришедшая извне (эмбиентная)
    float getAmbientRadiation();
    void setAmbientRadiation(float value);
    
    // Копирование данных от другого capability (например, при смерти игрока)
    void copyFrom(IChunkRadiation source);
}