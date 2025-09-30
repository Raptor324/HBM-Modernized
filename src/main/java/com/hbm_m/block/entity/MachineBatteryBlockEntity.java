package com.hbm_m.block.entity;

// Это блок-энтити для энергохранилища, которое может заряжать предметы и машины по проводам
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.menu.MachineBatteryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.level.block.entity.BlockEntity;
import javax.annotation.Nonnull;
import com.hbm_m.main.MainRegistry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class MachineBatteryBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(2);
    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(com.hbm_m.item.ModItems.BATTERY_CAPACITY, 5000, 5000);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int energyDelta = 0;
    private int lastEnergy = 0;
    private Component customName;
    
    // Режим, когда НЕТ сигнала (настраивается верхней кнопкой)
    public int modeOnNoSignal = 0;
    // Режим, когда ЕСТЬ сигнал (настраивается нижней кнопкой)
    public int modeOnSignal = 0;

    // --- ОБЕРТКА ДЛЯ IENERGYSTORAGE, УЧИТЫВАЮЩАЯ РЕДСТОУН ---
    private final IEnergyStorage energyWrapper = createEnergyWrapper();

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

            @Override public int receiveEnergy(int maxReceive, boolean simulate) { return !isInputAllowed() ? 0 : energyStorage.receiveEnergy(maxReceive, simulate); }
            @Override public int extractEnergy(int maxExtract, boolean simulate) { return !isOutputAllowed() ? 0 : energyStorage.extractEnergy(maxExtract, simulate); }
            @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
            @Override public boolean canExtract() { return isOutputAllowed() && energyStorage.canExtract(); }
            @Override public boolean canReceive() { return isInputAllowed() && energyStorage.canReceive(); }
        };
    }
    // Режимы: 0 = Приём и Передача, 1 = Только Приём, 2 = Только Передача, 3 = Заблокировано
    public Priority priority = Priority.NORMAL;
    
    public enum Priority { LOW, NORMAL, HIGH }

    public MachineBatteryBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MACHINE_BATTERY_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> MachineBatteryBlockEntity.this.energyStorage.getEnergyStored();
                    case 1 -> MachineBatteryBlockEntity.this.energyStorage.getMaxEnergyStored();
                    case 2 -> MachineBatteryBlockEntity.this.energyDelta;
                    case 3 -> MachineBatteryBlockEntity.this.modeOnNoSignal;
                    case 4 -> MachineBatteryBlockEntity.this.modeOnSignal;
                    case 5 -> MachineBatteryBlockEntity.this.priority.ordinal();
                    default -> 0;
                };
            }
            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> MachineBatteryBlockEntity.this.energyStorage.setEnergy(pValue);
                    case 2 -> MachineBatteryBlockEntity.this.energyDelta = pValue;
                    case 3 -> MachineBatteryBlockEntity.this.modeOnNoSignal = pValue;
                    case 4 -> MachineBatteryBlockEntity.this.modeOnSignal = pValue;
                    case 5 -> MachineBatteryBlockEntity.this.priority = Priority.values()[pValue];
                }
            }
            @Override
            public int getCount() { return 6; }
        };
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, MachineBatteryBlockEntity pBlockEntity) {
        if (pLevel.isClientSide()) return;

        boolean hasSignal = pLevel.hasNeighborSignal(pPos);
        
        // 1. Определяем, какой режим активен СЕЙЧАС, в зависимости от сигнала
        int activeMode = hasSignal ? pBlockEntity.modeOnSignal : pBlockEntity.modeOnNoSignal;

        // 2. Определяем, какие операции разрешены в этом режиме
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

        // 3. Выполняем разрешенные операции
        if (canInput) {
            pBlockEntity.chargeFromItem();
        }
        if (canOutput) {
            pBlockEntity.dischargeToItem();
            pBlockEntity.pushEnergyToNeighbors();
        }

        pBlockEntity.energyDelta = pBlockEntity.energyStorage.getEnergyStored() - pBlockEntity.lastEnergy;
        pBlockEntity.lastEnergy = pBlockEntity.energyStorage.getEnergyStored();
        
        setChanged(pLevel, pPos, pState);
    }
    
    private void chargeFromItem() {
        itemHandler.getStackInSlot(0).getCapability(ForgeCapabilities.ENERGY).ifPresent(source -> {
            int canExtract = source.extractEnergy(energyStorage.getMaxReceive(), true);
            if (canExtract > 0) {
                int received = energyStorage.receiveEnergy(canExtract, false);
                source.extractEnergy(received, false);
            }
        });
    }

    private void dischargeToItem() {
        itemHandler.getStackInSlot(1).getCapability(ForgeCapabilities.ENERGY).ifPresent(target -> {
            int canReceive = target.receiveEnergy(energyStorage.getMaxExtract(), true);
            if (canReceive > 0) {
                int extracted = energyStorage.extractEnergy(canReceive, false);
                target.receiveEnergy(extracted, false);
            }
        });
    }

    private void pushEnergyToNeighbors() {
        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[BATTERY >>>] pushEnergyToNeighbors at {} currentEnergy={}", this.worldPosition, this.energyStorage.getEnergyStored());
        }
    AtomicInteger energyToSend = new AtomicInteger(this.energyStorage.extractEnergy(this.energyStorage.getMaxExtract(), true));
    UUID pushId = UUID.randomUUID();

            if (energyToSend.get() <= 0) {
                return; // Нечего отправлять
            }

        Level lvl = this.level;
        if (lvl == null) return;

        for (Direction direction : Direction.values()) {
            if (energyToSend.get() <= 0) {
                break; // Вся энергия роздана
            }

            BlockEntity neighbor = lvl.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) {
                continue;
            }

            // Если сосед — провод, используем его проксирующий метод acceptEnergy
            if (neighbor instanceof WireBlockEntity wire) {
                int accepted = wire.acceptEnergy(energyToSend.get(), pushId, this.worldPosition);
                if (accepted > 0) {
                    this.energyStorage.extractEnergy(accepted, false);
                    energyToSend.addAndGet(-accepted);
                }
                continue;
            }

            LazyOptional<IEnergyStorage> neighborCapability = neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());

            neighborCapability.ifPresent(neighborStorage -> {
                if (neighborStorage.canReceive()) {
                    int accepted = neighborStorage.receiveEnergy(energyToSend.get(), false);
                    if (accepted > 0) {
                        this.energyStorage.extractEnergy(accepted, false);
                        energyToSend.addAndGet(-accepted);
                    }
                }
            });
        }
    }
    
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
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @Nonnull Inventory pPlayerInventory, @Nonnull Player pPlayer) {
        return new MachineBatteryMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> this.energyWrapper);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    public void setCustomName(Component name) {
        this.customName = name;
    }
    
    public Component getCustomName() {
        return this.customName;
    }

    public boolean hasCustomName() {
        return this.customName != null;
    }
    
    @Override
    public Component getDisplayName() {
        return this.hasCustomName() ? this.customName : Component.translatable("block.hbm_m.machine_battery");
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("energy", energyStorage.getEnergyStored());
        pTag.putInt("modeOnNoSignal", modeOnNoSignal);
        pTag.putInt("modeOnSignal", modeOnSignal);
        pTag.putInt("priority", priority.ordinal());
        if (this.customName != null) {
            pTag.putString("CustomName", Component.Serializer.toJson(this.customName));
        }
    }

    @Override
    public void load(@Nonnull CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        energyStorage.setEnergy(pTag.getInt("energy"));
        modeOnNoSignal = pTag.getInt("modeOnNoSignal");
        modeOnSignal = pTag.getInt("modeOnSignal");
        priority = Priority.values()[pTag.getInt("priority")];
        if (pTag.contains("CustomName", 8)) {
            this.customName = Component.Serializer.fromJson(pTag.getString("CustomName"));
        }
    }
    
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    public int getComparatorPower() {
        return (int) Math.floor(((double)this.energyStorage.getEnergyStored() / this.energyStorage.getMaxEnergyStored()) * 15.0);
    }
}