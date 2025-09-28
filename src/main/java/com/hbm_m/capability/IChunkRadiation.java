package com.hbm_m.capability;

// Интерфейс capability для хранения данных о радиации в чанке.
// Содержит методы для получения и установки радиации от блоков и фоновой радиации.
// Также включает метод для копирования данных от другого экземпляра (например, при смерти игрока).
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