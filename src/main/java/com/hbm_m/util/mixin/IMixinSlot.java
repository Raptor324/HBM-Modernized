package com.hbm_m.util.mixin;

/**
 * Этот интерфейс будет "примешан" к ванильному классу Slot.
 * Он предоставит нам публичный метод для изменения координат.
 */
public interface IMixinSlot {
    void setPos(int x, int y);
}