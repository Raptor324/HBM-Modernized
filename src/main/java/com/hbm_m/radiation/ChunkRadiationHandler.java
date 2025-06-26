package com.hbm_m.radiation;

import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Абстрактный класс для обработки радиации в чанках
 */
public abstract class ChunkRadiationHandler {

    /**
     * Обновляет систему радиации для всех миров
     */
    public abstract void updateSystem();
    
    /**
     * Получает уровень радиации в указанной позиции
     * @param level мир
     * @param x координата X
     * @param y координата Y
     * @param z координата Z
     * @return уровень радиации в рад/с
     */
    public abstract float getRadiation(Level level, int x, int y, int z);
    
    /**
     * Устанавливает уровень радиации в указанной позиции
     * @param level мир
     * @param x координата X
     * @param y координата Y
     * @param z координата Z
     * @param rad уровень радиации в рад/с
     */
    public abstract void setRadiation(Level level, int x, int y, int z, float rad);
    
    /**
     * Увеличивает уровень радиации в указанной позиции
     * @param level мир
     * @param x координата X
     * @param y координата Y
     * @param z координата Z
     * @param rad величина увеличения радиации в рад/с
     */
    public abstract void incrementRad(Level level, int x, int y, int z, float rad);
    
    /**
     * Уменьшает уровень радиации в указанной позиции
     * @param level мир
     * @param x координата X
     * @param y координата Y
     * @param z координата Z
     * @param rad величина уменьшения радиации в рад/с
     */
    public abstract void decrementRad(Level level, int x, int y, int z, float rad);
    
    /**
     * Очищает систему радиации для указанного мира
     * @param level мир
     */
    public abstract void clearSystem(Level level);

    /**
     * Обработчики событий
     */
    public void receiveWorldLoad(LevelEvent.Load event) { }
    public void receiveWorldUnload(LevelEvent.Unload event) { }
    public void receiveWorldTick(TickEvent.ServerTickEvent event) { }
    
    public void receiveChunkLoad(ChunkDataEvent.Load event) { }
    public void receiveChunkSave(ChunkDataEvent.Save event) { }
    public void receiveChunkUnload(ChunkEvent.Unload event) { }
    
    /**
     * Обрабатывает эффекты радиации на мир (мутации блоков)
     */
    public void handleWorldDestruction() { }
}