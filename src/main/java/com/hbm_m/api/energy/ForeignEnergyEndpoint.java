// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm_m.api.energy;

import com.hbm_m.api.network.NodeNet;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Чужое FE-хранилище (loader block-capability), увиденное сетью через грань проводника.
 * Участвует в распределении как получатель с низшим приоритетом и как поставщик
 * в последнюю очередь; конверсия HE↔FE идёт через {@link EnergyConversion.Carry}.
 * Двунаправленные хранилища работают только как получатель — иначе возможна
 * бесконечная перекачка по кругу.
 */
final class ForeignEnergyEndpoint implements IEnergyReceiver, IEnergyProvider, NodeNet.ILoadedEntry {

    static final boolean BIDIRECTIONAL_IS_RECEIVER_ONLY = true;

    static final IEnergyReceiver.Priority FOREIGN_PRIORITY = IEnergyReceiver.Priority.LOWEST;

    final long timestamp = System.currentTimeMillis();

    private final net.minecraft.world.level.block.entity.BlockEntity be;
    private final Direction side;

    // Loader-FE view соседа: общий shim над loader-IEnergyStorage (сигнатуры идентичны).
    private FEView view;

    private final EnergyConversion.Carry carry = new EnergyConversion.Carry();
    private long availableHe;
    private long roomHe;
    private boolean provider;
    private boolean receiver;

    private boolean bidirectionalSeen;

    private long owedFe;

    /** Создать endpoint, если у соседа есть loader-FE view; иначе null. */
    static ForeignEnergyEndpoint probe(net.minecraft.world.level.block.entity.BlockEntity be, Direction side) {
        ForeignEnergyEndpoint ep = new ForeignEnergyEndpoint(be, side);
        ep.resolveView();
        return ep.view == null ? null : ep;
    }

    ForeignEnergyEndpoint(net.minecraft.world.level.block.entity.BlockEntity be, Direction side) {
        this.be = be;
        this.side = side;
    }

    net.minecraft.world.level.block.entity.BlockEntity be() { return be; }
    Direction side() { return side; }

    void refresh() {
        resolveView();
        if (view == null) {
            provider = false;
            receiver = false;
            availableHe = 0;
            roomHe = 0;
            return;
        }
        long insertableFe = view.canReceive() ? Math.max(0, view.getMaxEnergyStored() - view.getEnergyStored()) : 0;
        long extractableFe = view.canExtract() ? Math.max(0, view.getEnergyStored()) : 0;
        boolean canInsert = insertableFe > 0;
        boolean canExtract = extractableFe > 0;
        if (canInsert && canExtract) bidirectionalSeen = true;

        receiver = canInsert;
        provider = canExtract && !(BIDIRECTIONAL_IS_RECEIVER_ONLY && bidirectionalSeen);

        availableHe = provider ? EnergyConversion.heFromFe(Math.max(0, extractableFe - owedFe)) : 0;
        roomHe = receiver ? EnergyConversion.heFromFe(insertableFe) : 0;
    }

    boolean servesAsProvider() {
        return provider && availableHe > 0;
    }

    boolean servesAsReceiver() {
        return receiver && roomHe > 0;
    }

    @Override
    public boolean isLoaded() {
        return !be.isRemoved();
    }

    // --- IEnergyConnector ---

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    // --- Общий уровень: сеть читает и как приёмник, и как поставщик ---

    @Override
    public long getEnergyStored() {
        return availableHe;
    }

    @Override
    public long getMaxEnergyStored() {
        long total = availableHe + roomHe;
        return total < 0 ? Long.MAX_VALUE : total;
    }

    @Override
    public void setEnergyStored(long energy) {
        // У чужого FE-хранилища нет абсолютного HE-уровня: перенос идёт
        // через transferPower/extractEnergy.
    }

    @Override
    public long getProvideSpeed() {
        return availableHe;
    }

    @Override
    public long getReceiveSpeed() {
        return roomHe;
    }

    @Override
    public IEnergyReceiver.Priority getPriority() {
        return FOREIGN_PRIORITY;
    }

    // --- Приём: HE-запрос сети конвертируется в FE и вставляется в чужое хранилище ---

    @Override
    public long transferPower(long power) {
        if (power <= 0 || !receiver) return power;
        long fe = carry.feFromHe(power);
        long acceptedFe = fe > 0 ? viewInsert(fe, false) : 0;
        long unplacedHe = acceptedFe < fe ? carry.refundFe(fe - acceptedFe, power) : 0;
        roomHe = Math.max(0, roomHe - (power - unplacedHe));
        return unplacedHe;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive() || maxReceive <= 0) return 0;
        long fits = Math.min(maxReceive, roomHe);
        return simulate ? fits : fits - transferPower(fits);
    }

    @Override
    public boolean canReceive() {
        return receiver;
    }

    // --- Отдача: FE чужого хранилища конвертируется в HE и уходит в сеть ---

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (maxExtract <= 0 || !provider) return 0;
        long taken = Math.min(maxExtract, availableHe);
        if (simulate) return taken;
        availableHe = Math.max(0, availableHe - taken);
        long fe = carry.feFromHe(taken) + owedFe;
        if (fe <= 0) return taken;
        owedFe = fe - viewExtract(fe);
        return taken;
    }

    @Override
    public boolean canExtract() {
        return provider;
    }

    private long viewInsert(long fe, boolean simulate) {
        if (view == null) return 0;
        int amount = (int) Math.min(fe, Integer.MAX_VALUE);
        return amount <= 0 ? 0 : view.receiveEnergy(amount, simulate);
    }

    private long viewExtract(long fe) {
        if (view == null) return 0;
        int amount = (int) Math.min(fe, Integer.MAX_VALUE);
        return amount <= 0 ? 0 : view.extractEnergy(amount, false);
    }

    /** Loader-FE view соседнего BlockEntity: тонкий лоадерный клей + общий shim. */
    private void resolveView() {
        FEView found = null;
        //? if forge {
        net.minecraftforge.energy.IEnergyStorage fe = be.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, side).resolve().orElse(null);
        found = fe == null ? null : shim(fe::receiveEnergy, fe::extractEnergy, fe::getEnergyStored,
                fe::getMaxEnergyStored, fe::canReceive, fe::canExtract);
        //?}
        //? if neoforge {
        /*net.neoforged.neoforge.energy.IEnergyStorage fe = be.getLevel() == null ? null
                : net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK.getCapability(
                        be.getLevel(), be.getBlockPos(), be.getBlockState(), be, side);
        found = fe == null ? null : shim(fe::receiveEnergy, fe::extractEnergy, fe::getEnergyStored,
                fe::getMaxEnergyStored, fe::canReceive, fe::canExtract);
        *///?}
        view = found;
    }

    private FEView shim(java.util.function.ToIntBiFunction<Integer, Boolean> receive,
                        java.util.function.ToIntBiFunction<Integer, Boolean> extract,
                        java.util.function.IntSupplier stored,
                        java.util.function.IntSupplier max,
                        java.util.function.BooleanSupplier canReceive,
                        java.util.function.BooleanSupplier canExtract) {
        return new FEView() {
            @Override public int receiveEnergy(int amount, boolean simulate) { return receive.applyAsInt(amount, simulate); }
            @Override public int extractEnergy(int amount, boolean simulate) { return extract.applyAsInt(amount, simulate); }
            @Override public int getEnergyStored() { return stored.getAsInt(); }
            @Override public int getMaxEnergyStored() { return max.getAsInt(); }
            @Override public boolean canReceive() { return canReceive.getAsBoolean(); }
            @Override public boolean canExtract() { return canExtract.getAsBoolean(); }
        };
    }

    /** Минимальный общий FE-контракт: сигнатуры forge/neoforge IEnergyStorage идентичны. */
    private interface FEView {
        int receiveEnergy(int amount, boolean simulate);
        int extractEnergy(int amount, boolean simulate);
        int getEnergyStored();
        int getMaxEnergyStored();
        boolean canReceive();
        boolean canExtract();
    }

    EnergyConversion.Carry carry() {
        return carry;
    }
}
