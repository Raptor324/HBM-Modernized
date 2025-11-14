package com.hbm_m.block.entity;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.energy.LongToForgeWrapper;
import com.hbm_m.energy.LongDataPacker;
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Батарея машины - аккумулятор энергии для мультиблочной системы.
 * Адаптирована для long-энергосистемы с поддержкой редстоун-управления.
 */
public class MachineBatteryBlockEntity extends BaseMachineBlockEntity {
    
    // Слоты
    private static final int SLOT_COUNT = 2;
    private static final int CHARGE_SLOT = 0;
    private static final int DISCHARGE_SLOT = 1;
    
    // Long-энергия
    private final BlockEntityEnergyStorage energyStorage = 
        new BlockEntityEnergyStorage(com.hbm_m.item.ModItems.BATTERY_CAPACITY, 100_000L);
    
    // Режимы работы
    public int modeOnNoSignal = 0;
    public int modeOnSignal = 0;
    public Priority priority = Priority.NORMAL;
    
    public enum Priority { LOW, NORMAL, HIGH }
    
    // Обёртка IEnergyStorage с поддержкой редстоун-режимов
    private final IEnergyStorage energyWrapper = createEnergyWrapper();
    
    // ContainerData с упаковкой long для энергии
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int pIndex) {
            long currentEnergy = energyStorage.getEnergyStored();
            long maxEnergy = energyStorage.getMaxEnergyStored();
            long delta = getEnergyDelta();
            return switch (pIndex) {
                case 0 -> LongDataPacker.packHigh(currentEnergy);
                case 1 -> LongDataPacker.packLow(currentEnergy);
                case 2 -> LongDataPacker.packHigh(maxEnergy);
                case 3 -> LongDataPacker.packLow(maxEnergy);
                case 4 -> LongDataPacker.packHigh(delta);
                case 5 -> LongDataPacker.packLow(delta);
                case 6 -> modeOnNoSignal;
                case 7 -> modeOnSignal;
                case 8 -> priority.ordinal();
                default -> 0;
            };
        }
        
        @Override
        public void set(int pIndex, int pValue) {
            switch (pIndex) {
                case 6 -> modeOnNoSignal = pValue;
                case 7 -> modeOnSignal = pValue;
                case 8 -> priority = Priority.values()[pValue];
            }
        }
        
        @Override
        public int getCount() { 
            return 9; 
        }
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
        // Используем long-энергохранилище с обёрткой редстоун-управления
        longEnergyHandler = LazyOptional.of(() -> energyStorage);
        // Обёртка Forge Energy поверх long-хранилища с поддержкой редстоун-режимов
        forgeEnergyHandler = LazyOptional.of(() -> energyWrapper);
    }
    
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new MachineBatteryMenu(pContainerId, pPlayerInventory, this, this.data);
    }
    
    // ==================== ENERGY WRAPPER (Redstone-aware) ====================
    
    private IEnergyStorage createEnergyWrapper() {
        return new IEnergyStorage() {
            private boolean isInputAllowed() {
                if (level == null) return false;
                boolean hasSignal = level.hasNeighborSignal(worldPosition);
                int activeMode = hasSignal ? modeOnSignal : modeOnNoSignal;
                return activeMode == 0 || activeMode == 1;
            }
            
            private boolean isOutputAllowed() {
                if (level == null) return false;
                boolean hasSignal = level.hasNeighborSignal(worldPosition);
                int activeMode = hasSignal ? modeOnSignal : modeOnNoSignal;
                return activeMode == 0 || activeMode == 2;
            }
            
            @Override 
            public int receiveEnergy(int maxReceive, boolean simulate) { 
                if (!isInputAllowed()) return 0;
                long maxReceiveLong = Math.min(maxReceive, Integer.MAX_VALUE);
                return (int) energyStorage.receiveEnergy(maxReceiveLong, simulate); 
            }
            
            @Override 
            public int extractEnergy(int maxExtract, boolean simulate) { 
                if (!isOutputAllowed()) return 0;
                long maxExtractLong = Math.min(maxExtract, Integer.MAX_VALUE);
                return (int) energyStorage.extractEnergy(maxExtractLong, simulate); 
            }
            
            @Override 
            public int getEnergyStored() { 
                return (int) Math.min(energyStorage.getEnergyStored(), Integer.MAX_VALUE); 
            }
            
            @Override 
            public int getMaxEnergyStored() { 
                return (int) Math.min(energyStorage.getMaxEnergyStored(), Integer.MAX_VALUE); 
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
        
        long gameTime = pLevel.getGameTime();
        
        // Ежедневная обработка энергии
        if (gameTime % 1 == 0) {
            boolean hasSignal = pLevel.hasNeighborSignal(pPos);
            int activeMode = hasSignal ? pBlockEntity.modeOnSignal : pBlockEntity.modeOnNoSignal;
            
            boolean canInput = activeMode == 0 || activeMode == 1;
            boolean canOutput = activeMode == 0 || activeMode == 2;
            
            if (canInput) {
                pBlockEntity.chargeFromItem();
            }
            
            if (canOutput) {
                pBlockEntity.dischargeToItem();
                pBlockEntity.pushEnergyToNeighbors();
            }
        }
        
        // Обновление энергетической дельты
        if (gameTime % 10 == 0) {
            pBlockEntity.updateEnergyDelta(pBlockEntity.energyStorage.getEnergyStored());
        }
        
        pBlockEntity.setChanged();
    }
    
    private void chargeFromItem() {
        ItemStack chargeStack = inventory.getStackInSlot(CHARGE_SLOT);
        if (chargeStack.isEmpty()) return;
        
        chargeStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(source -> {
            if (!source.canExtract()) return;
            
            long spaceAvailable = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
            if (spaceAvailable <= 0) return;
            
            int maxCanExtract = (int) Math.min(energyStorage.getMaxReceive(), spaceAvailable);
            if (maxCanExtract <= 0) return;
            
            int extracted = source.extractEnergy(maxCanExtract, true);
            if (extracted > 0) {
                long received = energyStorage.receiveEnergy(extracted, false);
                source.extractEnergy((int) received, false);
                setChanged();
            }
        });
    }
    
    private void dischargeToItem() {
        ItemStack dischargeStack = inventory.getStackInSlot(DISCHARGE_SLOT);
        if (dischargeStack.isEmpty()) return;
        
        dischargeStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(target -> {
            if (!target.canReceive()) return;
            
            long availableEnergy = energyStorage.getEnergyStored();
            if (availableEnergy <= 0) return;
            
            int maxCanTransfer = (int) Math.min(energyStorage.getMaxExtract(), availableEnergy);
            if (maxCanTransfer <= 0) return;
            
            int canReceive = target.receiveEnergy(maxCanTransfer, true);
            if (canReceive > 0) {
                long extracted = energyStorage.extractEnergy(canReceive, false);
                target.receiveEnergy((int) extracted, false);
                setChanged();
            }
        });
    }
    
    private void pushEnergyToNeighbors() {
        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[BATTERY >>>] pushEnergyToNeighbors at {} currentEnergy={}", 
                this.worldPosition, this.energyStorage.getEnergyStored());
        }
        
        long energyToSend = Math.min(energyStorage.getMaxExtract(), energyStorage.getEnergyStored());
        UUID pushId = UUID.randomUUID();
        
        if (energyToSend <= 0) {
            return;
        }
        
        Level lvl = this.level;
        if (lvl == null) return;
        
        final long[] totalSent = {0};
        
        for (Direction direction : Direction.values()) {
            if (totalSent[0] >= energyToSend) break;
            
            BlockEntity neighbor = lvl.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) continue;
            
            // Балансировка между батареями
            if (neighbor instanceof MachineBatteryBlockEntity otherBattery) {
                long myEnergy = this.energyStorage.getEnergyStored();
                long theirEnergy = otherBattery.energyStorage.getEnergyStored();
                
                if (myEnergy <= theirEnergy) {
                    if (ModClothConfig.get().enableDebugLogging) {
                        MainRegistry.LOGGER.debug("[BATTERY >>>] Skipping battery at {} (my: {}, their: {})",
                            neighbor.getBlockPos(), myEnergy, theirEnergy);
                    }
                    continue;
                }
                
                long difference = myEnergy - theirEnergy;
                long toSend = Math.min(difference / 2, energyToSend - totalSent[0]);
                
                if (toSend > 0) {
                    int toSendInt = (int) Math.min(toSend, Integer.MAX_VALUE);
                    long accepted = otherBattery.energyStorage.receiveEnergy(toSendInt, false);
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
            
            // Для проводов
            if (neighbor instanceof WireBlockEntity wire) {
                long remaining = energyToSend - totalSent[0];
                int remainingInt = (int) Math.min(remaining, Integer.MAX_VALUE);
                long accepted = wire.acceptEnergy(remainingInt, pushId, this.worldPosition);
                if (accepted > 0) {
                    this.energyStorage.extractEnergy(accepted, false);
                    totalSent[0] += accepted;
                }
                continue;
            }
            
            // Для остальных устройств
            LazyOptional<IEnergyStorage> neighborCapability = 
                neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());
            
            neighborCapability.ifPresent(neighborStorage -> {
                if (neighborStorage.canReceive()) {
                    long remaining = energyToSend - totalSent[0];
                    int remainingInt = (int) Math.min(remaining, Integer.MAX_VALUE);
                    int accepted = neighborStorage.receiveEnergy(remainingInt, false);
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
            case 0:
                modeOnNoSignal = (modeOnNoSignal + 1) % 4;
                break;
            case 1:
                modeOnSignal = (modeOnSignal + 1) % 4;
                break;
            case 2:
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
        super.saveAdditional(pTag);
        pTag.putLong("EnergyStored", energyStorage.getEnergyStored());
        pTag.putInt("modeOnNoSignal", modeOnNoSignal);
        pTag.putInt("modeOnSignal", modeOnSignal);
        pTag.putInt("priority", priority.ordinal());
    }
    
    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        energyStorage.setEnergy(pTag.getLong("EnergyStored"));
        modeOnNoSignal = pTag.getInt("modeOnNoSignal");
        modeOnSignal = pTag.getInt("modeOnSignal");
        priority = Priority.values()[pTag.getInt("priority")];
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong("EnergyStored", energyStorage.getEnergyStored());
        tag.putInt("modeOnNoSignal", modeOnNoSignal);
        tag.putInt("modeOnSignal", modeOnSignal);
        tag.putInt("priority", priority.ordinal());
        return tag;
    }
    
    // ==================== CAPABILITY ====================
    
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return forgeEnergyHandler.cast();
        }
        return super.getCapability(cap, side);
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
        long energy = this.energyStorage.getEnergyStored();
        long maxEnergy = this.energyStorage.getMaxEnergyStored();
        return (int) Math.floor(((double) energy / (double) maxEnergy) * 15.0);
    }
}
