// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm_m.api.energy;

import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Мост чужой Forge Energy на энергосеть: сеть публикует бюджеты (избыток и нехватку
 * нативной энергии), а FE-хранилища, приложенные к проводам сети, пьют/льют FE
 * против этих бюджетов с конверсией через {@link EnergyConversion.Carry}.
 * Latch на соседа не даёт одному соседу посчитаться дважды в окно публикации.
 */
public final class ForeignEnergyBridge {

    public static final int RECEIVE_LATCH_TICKS = 4;

    private final Long2LongOpenHashMap latchByNeighbour = new Long2LongOpenHashMap();

    private final IEnergyProvider injectedSource = new InjectedSource();

    private final EnergyConversion.Carry carry = new EnergyConversion.Carry();
    private long injectedHe;
    private long insertBudgetHe;
    private long extractBudgetHe;
    private long owedHe;

    private boolean distributing;

    public ForeignEnergyBridge() {
        latchByNeighbour.defaultReturnValue(Long.MIN_VALUE);
    }

    public long insertableFe() {
        return distributing ? 0 : EnergyConversion.feFromHe(insertBudgetHe);
    }

    public long extractableFe() {
        return distributing ? 0 : EnergyConversion.feFromHe(extractBudgetHe);
    }

    public long insertFe(long fe, long neighbourKey, long gameTime) {
        if (fe <= 0 || distributing) return 0;
        long accepted = Math.min(fe, EnergyConversion.feFromHe(insertBudgetHe));
        if (accepted <= 0) return 0;
        long he = carry.heFromFe(accepted);
        if (he > insertBudgetHe) he = insertBudgetHe;
        insertBudgetHe -= he;
        injectedHe += he;
        latchByNeighbour.put(neighbourKey, gameTime);
        return accepted;
    }

    public long extractFe(long fe) {
        if (fe <= 0 || distributing) return 0;
        long given = Math.min(fe, EnergyConversion.feFromHe(extractBudgetHe));
        if (given <= 0) return 0;
        long he = carry.heFromFe(given);
        if (he > extractBudgetHe) he = extractBudgetHe;
        extractBudgetHe -= he;
        owedHe += he;
        return given;
    }

    public long amountFe() {
        return EnergyConversion.feFromHe(extractBudgetHe);
    }

    public long capacityFe() {
        long total = amountFe() + EnergyConversion.feFromHe(insertBudgetHe);
        return total < 0 ? Long.MAX_VALUE : total;
    }

    public void absorb(ForeignEnergyBridge other) {
        injectedHe += other.injectedHe;
        owedHe += other.owedHe;
        carry.absorb(other.carry);
        for (ObjectIterator<Long2LongMap.Entry> it =
                        other.latchByNeighbour.long2LongEntrySet().fastIterator();
                it.hasNext(); ) {
            Long2LongMap.Entry e = it.next();
            long mine = latchByNeighbour.get(e.getLongKey());
            if (mine == Long.MIN_VALUE || e.getLongValue() > mine) {
                latchByNeighbour.put(e.getLongKey(), e.getLongValue());
            }
        }
    }

    public boolean beginDistribute() {
        if (distributing) return false;
        distributing = true;
        return true;
    }

    public void endDistribute() {
        distributing = false;
    }

    public long injected() {
        return injectedHe;
    }

    public IEnergyProvider injectedSource() {
        return injectedSource;
    }

    public long drainOwed() {
        long owed = owedHe;
        owedHe = 0;
        return owed;
    }

    public void carryOwed(long he) {
        if (he > 0) owedHe += he;
    }

    public boolean latched(long neighbourKey, long gameTime) {
        long stamp = latchByNeighbour.get(neighbourKey);
        return stamp != Long.MIN_VALUE && gameTime - stamp <= RECEIVE_LATCH_TICKS;
    }

    /** Опубликовать бюджеты этого тика: избыток сети (отдача наружу) и нехватку (приём снаружи). */
    public void publish(long surplusHe, long unmetNativeHe, long gameTime) {
        extractBudgetHe = Math.max(0, surplusHe);
        insertBudgetHe = Math.max(0, unmetNativeHe);
        if (latchByNeighbour.isEmpty()) return;
        for (ObjectIterator<Long2LongMap.Entry> it =
                        latchByNeighbour.long2LongEntrySet().fastIterator();
                it.hasNext(); ) {
            if (gameTime - it.next().getLongValue() > RECEIVE_LATCH_TICKS) it.remove();
        }
    }

    /**
     * Виртуальный поставщик, через который влитая снаружи FE энергия входит
     * в распределение сети наравне с нативными генераторами.
     */
    private final class InjectedSource implements IEnergyProvider {

        @Override public long getEnergyStored() { return injectedHe; }

        @Override
        public void setEnergyStored(long energy) {
            injectedHe = Math.max(0, energy);
        }

        @Override public long getMaxEnergyStored() { return injectedHe; }
        @Override public long getProvideSpeed() { return injectedHe; }
        @Override public boolean canExtract() { return injectedHe > 0; }

        @Override
        public long extractEnergy(long maxExtract, boolean simulate) {
            if (maxExtract <= 0 || injectedHe <= 0) return 0;
            long taken = Math.min(maxExtract, injectedHe);
            if (!simulate) injectedHe -= taken;
            return taken;
        }

        @Override
        public boolean canConnectEnergy(net.minecraft.core.Direction side) { return true; }
    }
}
