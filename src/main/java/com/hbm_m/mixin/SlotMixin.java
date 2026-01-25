package com.hbm_m.mixin;

// Используется для корректного взаимодействия со слотами брони в инвентаре стола модификации брони.

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import com.hbm_m.util.IMixinSlot;


// Указываем, что наша цель - ванильный класс Slot
@Mixin(Slot.class)
public abstract class SlotMixin implements IMixinSlot {

    // С помощью @Shadow мы получаем доступ к приватным/финальным полям целевого класса.
    // С помощью @Mutable мы сообщаем Mixin, что хотим снять с этого поля модификатор 'final'.
    // @Final здесь нужен, чтобы сигнатура точно совпала с оригинальным полем.
    
    @Mutable
    @Shadow
    @Final
    public int x;

    @Mutable
    @Shadow
    @Final
    public int y;

    /**
     * Реализуем метод из нашего интерфейса.
     * Теперь мы можем изменять поля x и y, так как они помечены как @Mutable.
     */
    @Override
    public void setPos(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }
}