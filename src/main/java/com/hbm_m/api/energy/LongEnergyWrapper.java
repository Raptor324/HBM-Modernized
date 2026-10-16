package com.hbm_m.api.energy;

import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;

//? if forge {
import net.minecraftforge.energy.IEnergyStorage;
//?}
//? if neoforge {
/*import net.neoforged.neoforge.energy.IEnergyStorage;
*///?}

/**
 * FE-обёртка над HBM-энергией (long) для чужих модов: FE-уровень = feFromHe(HE),
 * перенос выравнивается по {@link EnergyConversion#feQuantum()}, чтобы конверсия
 * не теряла доли HE. Запросы ограничены комнатой/скоростью машины — как в оригинале.
 */
public class LongEnergyWrapper implements IEnergyStorage {

    private final IEnergyConnector handler;

    public LongEnergyWrapper(IEnergyConnector handler) {
        this.handler = handler;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!(handler instanceof IEnergyReceiver receiver) || !receiver.canReceive()) {
            return 0;
        }

        long room = receiver.getMaxEnergyStored() - receiver.getEnergyStored();
        long offered = Math.min(room, receiver.getReceiveSpeed());

        long acceptableFe = EnergyConversion.feFromHe(offered);
        long quantum = EnergyConversion.feQuantum();
        long fits = Math.min(maxReceive, acceptableFe) / quantum * quantum;
        int accepted = (int) Math.max(0L, fits);
        if (accepted > 0 && !simulate) {
            receiver.transferPower(EnergyConversion.heFromFe(accepted));
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!(handler instanceof IEnergyProvider provider) || !provider.canExtract()) {
            return 0;
        }

        long available = Math.min(provider.getEnergyStored(), provider.getProvideSpeed());
        long extractableFe = EnergyConversion.feFromHe(available);
        long quantum = EnergyConversion.feQuantum();
        long gives = Math.min(maxExtract, extractableFe) / quantum * quantum;
        int extracted = (int) Math.max(0L, gives);
        if (extracted > 0 && !simulate) {
            provider.usePower(EnergyConversion.heFromFe(extracted));
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        long energy = 0;

        if (handler instanceof IEnergyProvider p) {
            energy = p.getEnergyStored();
        } else if (handler instanceof IEnergyReceiver r) {
            energy = r.getEnergyStored();
        }

        return (int) Math.min(Integer.MAX_VALUE, EnergyConversion.feFromHe(energy));
    }

    @Override
    public int getMaxEnergyStored() {
        long maxEnergy = 0;

        if (handler instanceof IEnergyProvider p) {
            maxEnergy = p.getMaxEnergyStored();
        } else if (handler instanceof IEnergyReceiver r) {
            maxEnergy = r.getMaxEnergyStored();
        }

        return (int) Math.min(Integer.MAX_VALUE, EnergyConversion.feFromHe(maxEnergy));
    }

    @Override
    public boolean canExtract() {
        return handler instanceof IEnergyProvider p && p.canExtract();
    }

    @Override
    public boolean canReceive() {
        return handler instanceof IEnergyReceiver r && r.canReceive();
    }
}
