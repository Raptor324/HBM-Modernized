//? if forge || neoforge {
package com.hbm_m.api.energy;

import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;

//? if forge {
/*import net.minecraftforge.energy.IEnergyStorage;
*///?}
//? if neoforge {
import net.neoforged.neoforge.energy.IEnergyStorage;
//?}

/**
 * Мост между HBM Energy (long) и Forge/NeoForge Energy (int): один к одному, с насыщением
 * на {@link Integer#MAX_VALUE}.
 *
 * The wrapper used to have a second "HIGH" mode handed out on Direction.DOWN that treated one FE
 * as 2^32 HE. Any FE cable under a battery then minted energy out of nothing (and extraction
 * destroyed it), so the packed-bits idea is gone: an energy unit means the same thing on every
 * face. Reporting saturates instead of truncating, otherwise a battery above 2^31 HE reports a
 * wrapped or negative charge to other mods.
 */
public class LongEnergyWrapper implements IEnergyStorage {

    private final IEnergyConnector handler;

    public LongEnergyWrapper(IEnergyConnector handler) {
        this.handler = handler;
    }

    private static int saturate(long value) {
        return (int) Math.max(0, Math.min(value, Integer.MAX_VALUE));
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!(handler instanceof IEnergyReceiver receiver) || !receiver.canReceive()) {
            return 0;
        }

        long free = receiver.getMaxEnergyStored() - receiver.getEnergyStored();
        long toReceive = Math.min(maxReceive, free);
        if (toReceive <= 0) return 0;

        return saturate(receiver.receiveEnergy(toReceive, simulate));
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!(handler instanceof IEnergyProvider provider) || !provider.canExtract()) {
            return 0;
        }

        long toExtract = Math.min(maxExtract, provider.getEnergyStored());
        if (toExtract <= 0) return 0;

        return saturate(provider.extractEnergy(toExtract, simulate));
    }

    @Override
    public int getEnergyStored() {
        long energy = 0;

        if (handler instanceof IEnergyProvider p) {
            energy = p.getEnergyStored();
        } else if (handler instanceof IEnergyReceiver r) {
            energy = r.getEnergyStored();
        }

        return saturate(energy);
    }

    @Override
    public int getMaxEnergyStored() {
        long maxEnergy = 0;

        if (handler instanceof IEnergyProvider p) {
            maxEnergy = p.getMaxEnergyStored();
        } else if (handler instanceof IEnergyReceiver r) {
            maxEnergy = r.getMaxEnergyStored();
        }

        return saturate(maxEnergy);
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
//?}
