package com.hbm_m.capability;

import net.minecraft.nbt.CompoundTag;

public interface IChunkRadiation {
    // Получить общую радиацию (только block)
    float getRadiation();
    // Получить/установить только радиацию от блоков
    float getBlockRadiation();
    void setBlockRadiation(float blockRadiation);
    // Копирование поля blockRadiation
    void copyFrom(IChunkRadiation source);
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag nbt);
}