//? if forge {
/*package com.hbm_m.api.energy;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IEnergyConnector;

/^*
 * Провайдер Forge Energy для BlockEntity: одна и та же обёртка на все стороны, один к одному.
 *
 * The DOWN face used to hand out a wrapper scaled by 2 to the 32nd, which let any FE cable
 * mint HE out of nothing.
 ^/
public final class PackedEnergyCapabilityProvider {
    private final LazyOptional<IEnergyStorage> fe;

    public PackedEnergyCapabilityProvider(IEnergyConnector handler) {
        this.fe = LazyOptional.of(() -> new LongEnergyWrapper(handler));
    }

    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return fe.cast();
        }
        return LazyOptional.empty();
    }

    public LazyOptional<IEnergyStorage> getEnergyCapability() {
        return fe;
    }

    public void invalidate() {
        fe.invalidate();
    }
}
*///?}

//? if neoforge {
package com.hbm_m.api.energy;

import com.hbm_m.interfaces.IEnergyConnector;

/**
 * NeoForge stub (исторический): на 1.21.1 NeoForge полностью убрал getCapability/invalidateCaps
 * у BlockEntity, поэтому этот Forge-style провайдер здесь не применяется. Реальная FE-интеграция
 * для NeoForge живёт в ModCapabilities.registerEnergyForType (цикл по всем BlockEntityType с
 * LongEnergyWrapper/HbmForgeWrapper). Класс сохранён как no-op заглушка для любого кода, который
 * всё ещё ссылается на него через import на neoforge-ветке.
 */
public final class PackedEnergyCapabilityProvider {
    public PackedEnergyCapabilityProvider(IEnergyConnector handler) {
    }

    public void invalidate() {
    }
}
//?}