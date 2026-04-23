package com.hbm_m.interfaces;

/**
 * Этот интерфейс будет "примешан" к ванильному классу Slot.
 * Он предоставит нам публичный метод для изменения координат.
 */
public interface IMixinSlot {
    void setPos(int x, int y);
}