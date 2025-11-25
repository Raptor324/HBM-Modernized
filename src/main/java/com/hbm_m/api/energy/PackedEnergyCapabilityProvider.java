package com.hbm_m.api.energy;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import javax.annotation.Nullable;

/**
 * Провайдер Forge Energy для BlockEntity с упаковкой long в два int.
 *
 * ФИШКА: Позволяет работать с long энергией через стандартный Forge Energy API!
 * - Direction.DOWN = старшие биты (HIGH) - для значений выше 2 млрд
 * - Остальные стороны = младшие биты (LOW) - для обычных значений
 *
 * Таким образом другие моды могут взаимодействовать с огромными значениями энергии!
 */
public final class PackedEnergyCapabilityProvider {
    private final LazyOptional<IEnergyStorage> feLow;
    private final LazyOptional<IEnergyStorage> feHigh;

    public PackedEnergyCapabilityProvider(IEnergyConnector handler) {
        // LOW биты (0 - 2,147,483,647) - для большинства модов
        this.feLow = LazyOptional.of(() -> new LongEnergyWrapper(handler, LongEnergyWrapper.BitMode.LOW));

        // HIGH биты (множитель 2^32) - для огромных значений
        this.feHigh = LazyOptional.of(() -> new LongEnergyWrapper(handler, LongEnergyWrapper.BitMode.HIGH));
    }

    /**
     * Раздаёт capability в зависимости от стороны:
     * - DOWN = HIGH биты (для работы с большими значениями)
     * - Остальные = LOW биты (стандартная совместимость)
     */
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            // DOWN сторона = HIGH биты, остальные = LOW биты
            return (side == Direction.DOWN ? feHigh : feLow).cast();
        }
        return LazyOptional.empty();
    }

    /**
     * Получить LOW биты напрямую (для особых случаев)
     */
    public LazyOptional<IEnergyStorage> getLowBitsCapability() {
        return feLow;
    }

    /**
     * Получить HIGH биты напрямую (для особых случаев)
     */
    public LazyOptional<IEnergyStorage> getHighBitsCapability() {
        return feHigh;
    }

    public void invalidate() {
        feLow.invalidate();
        feHigh.invalidate();
    }
}