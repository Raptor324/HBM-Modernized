package com.hbm_m.block.entity;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.menu.MachineBatteryMenu;
import com.hbm_m.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MachineBatteryBlockEntity extends BaseMachineBlockEntity {
    
    // Слоты
    private static final int SLOT_COUNT = 2;
    private static final int CHARGE_SLOT = 0;
    private static final int DISCHARGE_SLOT = 1;
    
    // Энергия
    private final BlockEntityEnergyStorage energyStorage = 
        new BlockEntityEnergyStorage(com.hbm_m.item.ModItems.BATTERY_CAPACITY,  5000);
    
    // Режимы работы
    public int modeOnNoSignal = 0; // Режим, когда НЕТ сигнала (настраивается верхней кнопкой)
    public int modeOnSignal = 0;   // Режим, когда ЕСТЬ сигнал (настраивается нижней кнопкой)
    public Priority priority = Priority.NORMAL;
    
    // Режимы: 0 = Приём и Передача, 1 = Только Приём, 2 = Только Передача, 3 = Заблокировано
    public enum Priority { LOW, NORMAL, HIGH }
    
    // Обертка для IEnergyStorage, учитывающая редстоун-режимы
    private final IEnergyStorage energyWrapper = createEnergyWrapper();
    
    // ContainerData для GUI
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int pIndex) {
            return switch (pIndex) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                case 2 -> energyDelta;
                case 3 -> modeOnNoSignal;
                case 4 -> modeOnSignal;
                case 5 -> priority.ordinal();
                default -> 0;
            };
        }
        
        @Override
        public void set(int pIndex, int pValue) {
            switch (pIndex) {
                case 0 -> energyStorage.setEnergy(pValue);
                case 2 -> energyDelta = pValue;
                case 3 -> modeOnNoSignal = pValue;
                case 4 -> modeOnSignal = pValue;
                case 5 -> priority = Priority.values()[pValue];
            }
        }
        
        @Override
        public int getCount() { return 6; }
    };
    
    public MachineBatteryBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MACHINE_BATTERY_BE.get(), pPos, pBlockState, SLOT_COUNT);
    }
    
    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_battery");
    }
    
    @Override
    protected void setupEnergyCapability() {
        // Используем обертку вместо прямого energyStorage
        energyHandler = LazyOptional.of(() -> energyWrapper);
    }
    
    @Override
    protected boolean isItemValidForSlot(int slot, net.minecraft.world.item.ItemStack stack) {
        // Разрешаем предметы с energy capability в обоих слотах
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new MachineBatteryMenu(pContainerId, pPlayerInventory, this, this.data);
    }
    
    // ==================== ENERGY WRAPPER ====================
    
    private IEnergyStorage createEnergyWrapper() {
        return new IEnergyStorage() {
            private boolean isInputAllowed() {
                if (level == null) return false;
                boolean hasSignal = level.hasNeighborSignal(worldPosition);
                int activeMode = hasSignal ? modeOnSignal : modeOnNoSignal;
                // Режимы, разрешающие ПРИЁМ: 0 (Приём и Передача), 1 (Только Приём)
                return activeMode == 0 || activeMode == 1;
            }
            
            private boolean isOutputAllowed() {
                if (level == null) return false;
                boolean hasSignal = level.hasNeighborSignal(worldPosition);
                int activeMode = hasSignal ? modeOnSignal : modeOnNoSignal;
                // Режимы, разрешающие ПЕРЕДАЧУ: 0 (Приём и Передача), 2 (Только Передача)
                return activeMode == 0 || activeMode == 2;
            }
            
            @Override 
            public int receiveEnergy(int maxReceive, boolean simulate) { 
                return !isInputAllowed() ? 0 : energyStorage.receiveEnergy(maxReceive, simulate); 
            }
            
            @Override 
            public int extractEnergy(int maxExtract, boolean simulate) { 
                return !isOutputAllowed() ? 0 : energyStorage.extractEnergy(maxExtract, simulate); 
            }
            
            @Override 
            public int getEnergyStored() { 
                return energyStorage.getEnergyStored(); 
            }
            
            @Override 
            public int getMaxEnergyStored() { 
                return energyStorage.getMaxEnergyStored(); 
            }
            
            @Override 
            public boolean canExtract() { 
                return isOutputAllowed() && energyStorage.canExtract(); 
            }
            
            @Override 
            public boolean canReceive() { 
                return isInputAllowed() && energyStorage.canReceive(); 
            }
        };
    }
    
    // ==================== TICK LOGIC ====================
    
    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, MachineBatteryBlockEntity pBlockEntity) {
        if (pLevel.isClientSide()) return;
        
        boolean hasSignal = pLevel.hasNeighborSignal(pPos);
        
        // Определяем активный режим в зависимости от сигнала
        int activeMode = hasSignal ? pBlockEntity.modeOnSignal : pBlockEntity.modeOnNoSignal;
        
        // Определяем разрешенные операции
        boolean canInput = false;
        boolean canOutput = false;
        
        switch (activeMode) {
            case 0: // Приём и Передача
                canInput = true;
                canOutput = true;
                break;
            case 1: // Только Приём
                canInput = true;
                canOutput = false;
                break;
            case 2: // Только Передача
                canInput = false;
                canOutput = true;
                break;
            case 3: // Заблокировано
                canInput = false;
                canOutput = false;
                break;
        }
        
        // Выполняем разрешенные операции
        if (canInput) {
            pBlockEntity.chargeFromItem();
        }
        
        if (canOutput) {
            pBlockEntity.dischargeToItem();
            pBlockEntity.pushEnergyToNeighbors();
        }
        
        // Обновление энергетической дельты (каждый тик)
        pBlockEntity.updateEnergyDelta(pBlockEntity.energyStorage.getEnergyStored());
        
        pBlockEntity.setChanged();
    }
    
    private void chargeFromItem() {
        inventory.getStackInSlot(CHARGE_SLOT).getCapability(ForgeCapabilities.ENERGY).ifPresent(source -> {
            int canExtract = source.extractEnergy(energyStorage.getMaxReceive(), true);
            if (canExtract > 0) {
                int received = energyStorage.receiveEnergy(canExtract, false);
                source.extractEnergy(received, false);
            }
        });
    }
    
    private void dischargeToItem() {
        inventory.getStackInSlot(DISCHARGE_SLOT).getCapability(ForgeCapabilities.ENERGY).ifPresent(target -> {
            int canReceive = target.receiveEnergy(energyStorage.getMaxExtract(), true);
            if (canReceive > 0) {
                int extracted = energyStorage.extractEnergy(canReceive, false);
                target.receiveEnergy(extracted, false);
            }
        });
    }
    
    private void pushEnergyToNeighbors() {
        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[BATTERY >>>] pushEnergyToNeighbors at {} currentEnergy={}", 
                this.worldPosition, this.energyStorage.getEnergyStored());
        }
        
        int energyToSend = this.energyStorage.extractEnergy(this.energyStorage.getMaxExtract(), true);
        UUID pushId = UUID.randomUUID();
        
        if (energyToSend <= 0) {
            return;
        }
        
        Level lvl = this.level;
        if (lvl == null) return;
        
        final int[] totalSent = {0};
        
        for (Direction direction : Direction.values()) {
            if (totalSent[0] >= energyToSend) break;
            
            BlockEntity neighbor = lvl.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) continue;
            
            // Пропускаем другие батареи для избежания взаимной перекачки
            if (neighbor instanceof MachineBatteryBlockEntity otherBattery) {
                int myEnergy = this.energyStorage.getEnergyStored();
                int theirEnergy = otherBattery.energyStorage.getEnergyStored();
                
                if (myEnergy <= theirEnergy) {
                    if (ModClothConfig.get().enableDebugLogging) {
                        MainRegistry.LOGGER.debug("[BATTERY >>>] Skipping battery at {} (my: {}, their: {})",
                            neighbor.getBlockPos(), myEnergy, theirEnergy);
                    }
                    continue;
                }
                
                // Отдаём только половину разницы для плавного выравнивания
                int difference = myEnergy - theirEnergy;
                int toSend = Math.min(difference / 2, energyToSend - totalSent[0]);
                
                if (toSend > 0) {
                    int accepted = otherBattery.energyWrapper.receiveEnergy(toSend, false);
                    if (accepted > 0) {
                        this.energyStorage.extractEnergy(accepted, false);
                        totalSent[0] += accepted;
                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.debug("[BATTERY >>>] Balanced {} FE to battery at {}",
                                accepted, neighbor.getBlockPos());
                        }
                    }
                }
                continue;
            }
            
            // Для проводов используем acceptEnergy
            if (neighbor instanceof WireBlockEntity wire) {
                int remaining = energyToSend - totalSent[0];
                int accepted = wire.acceptEnergy(remaining, pushId, this.worldPosition);
                if (accepted > 0) {
                    this.energyStorage.extractEnergy(accepted, false);
                    totalSent[0] += accepted;
                }
                continue;
            }
            
            // Для остальных устройств используем capability
            LazyOptional<IEnergyStorage> neighborCapability = 
                neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());
            
            neighborCapability.ifPresent(neighborStorage -> {
                if (neighborStorage.canReceive()) {
                    int remaining = energyToSend - totalSent[0];
                    int accepted = neighborStorage.receiveEnergy(remaining, false);
                    if (accepted > 0) {
                        this.energyStorage.extractEnergy(accepted, false);
                        totalSent[0] += accepted;
                    }
                }
            });
        }
    }
    
    // ==================== BUTTON HANDLING ====================
    
    public void handleButtonPress(int buttonId) {
        switch (buttonId) {
            case 0: // Верхняя кнопка (нет сигнала)
                modeOnNoSignal = (modeOnNoSignal + 1) % 4;
                break;
            case 1: // Нижняя кнопка (есть сигнал)
                modeOnSignal = (modeOnSignal + 1) % 4;
                break;
            case 2: // Кнопка приоритета
                priority = Priority.values()[(priority.ordinal() + 1) % Priority.values().length];
                break;
        }
        
        setChanged();
        if (level != null && !level.isClientSide()) {
            sendUpdateToClient();
        }
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag); // ОБЯЗАТЕЛЬНО ПЕРВЫМ
        pTag.putInt("energy", energyStorage.getEnergyStored());
        pTag.putInt("modeOnNoSignal", modeOnNoSignal);
        pTag.putInt("modeOnSignal", modeOnSignal);
        pTag.putInt("priority", priority.ordinal());
    }
    
    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag); // ОБЯЗАТЕЛЬНО ПЕРВЫМ
        energyStorage.setEnergy(pTag.getInt("energy"));
        modeOnNoSignal = pTag.getInt("modeOnNoSignal");
        modeOnSignal = pTag.getInt("modeOnSignal");
        priority = Priority.values()[pTag.getInt("priority")];
    }
    
    // ==================== UTILITY ====================
    
    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, container);
        }
    }
    
    public int getComparatorPower() {
        return (int) Math.floor(((double)this.energyStorage.getEnergyStored() / 
            this.energyStorage.getMaxEnergyStored()) * 15.0);
    }
}
