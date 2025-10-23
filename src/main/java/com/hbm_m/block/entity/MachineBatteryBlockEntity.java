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

// [ИЗМЕНЕНИЕ] Импортируем твои новые классы
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.energy.LongToForgeWrapper;
import com.hbm_m.energy.ForgeToLongWrapper;


public class MachineBatteryBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(2);
    // [ИЗМЕНЕНИЕ] Убедись, что BATTERY_CAPACITY имеет тип long
    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(com.hbm_m.item.ModItems.BATTERY_CAPACITY, 5000L, 5000L);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // [ИЗМЕНЕНИЕ] Разделяем lazy handlers для long и int(forge)
    private LazyOptional<ILongEnergyStorage> lazyLongEnergyHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyForgeEnergyHandler = LazyOptional.empty(); // Это будет обертка

    protected final ContainerData data;
    private long energyDelta = 0; // [ИЗМЕНЕНИЕ] long
    private long lastEnergy = 0; // [ИЗМЕНЕНИЕ] long
    private Component customName;

    // Режим, когда НЕТ сигнала (настраивается верхней кнопкой)
    public int modeOnNoSignal = 0;
    // Режим, когда ЕСТЬ сигнал (настраивается нижней кнопкой)
    public int modeOnSignal = 0;

    // --- [ИЗМЕНЕНИЕ] ОБЕРТКА ДЛЯ ILONGENERGYSTORAGE, УЧИТЫВАЮЩАЯ РЕДСТОУН ---
    private final ILongEnergyStorage longEnergyWrapper = createEnergyWrapper();

    private ILongEnergyStorage createEnergyWrapper() {
        // [ИЗМЕНЕНИЕ] Реализуем ILongEnergyStorage
        return new ILongEnergyStorage() {
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

            // [ИЗМЕНЕНИЕ] Все методы теперь используют long
            @Override public long receiveEnergy(long maxReceive, boolean simulate) { return !isInputAllowed() ? 0L : energyStorage.receiveEnergy(maxReceive, simulate); }
            @Override public long extractEnergy(long maxExtract, boolean simulate) { return !isOutputAllowed() ? 0L : energyStorage.extractEnergy(maxExtract, simulate); }
            @Override public long getEnergyStored() { return energyStorage.getEnergyStored(); }
            @Override public long getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
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
                // [ИЗМЕНЕНИЕ] Разбиваем long на два int для синхронизации
                return switch (pIndex) {
                    case 0 -> (int)(MachineBatteryBlockEntity.this.energyStorage.getEnergyStored() & 0xFFFFFFFFL); // Младшая часть энергии
                    case 1 -> (int)(MachineBatteryBlockEntity.this.energyStorage.getEnergyStored() >> 32);         // Старшая часть энергии
                    case 2 -> (int)(MachineBatteryBlockEntity.this.energyStorage.getMaxEnergyStored() & 0xFFFFFFFFL); // Младшая часть ёмкости
                    case 3 -> (int)(MachineBatteryBlockEntity.this.energyStorage.getMaxEnergyStored() >> 32);         // Старшая часть ёмкости
                    case 4 -> (int)MachineBatteryBlockEntity.this.energyDelta; // (Дельта тоже может быть long, но для GUI int хватит)
                    case 5 -> MachineBatteryBlockEntity.this.modeOnNoSignal;
                    case 6 -> MachineBatteryBlockEntity.this.modeOnSignal;
                    case 7 -> MachineBatteryBlockEntity.this.priority.ordinal();
                    default -> 0;
                };
            }
            @Override
            public void set(int pIndex, int pValue) {
                // [ИЗМЕНЕНИЕ] Эта строка КРИТИЧЕСКИ ВАЖНА!
                // Она получает ТЕКУЩЕЕ значение, чтобы мы могли изменить только одну его часть (старшую или младшую).
                long energy = MachineBatteryBlockEntity.this.energyStorage.getEnergyStored();

                // [ИСПРАВЛЕНО] Весь switch переведен на синтаксис "->"
                switch (pIndex) {
                    case 0 -> { // Младшая часть энергии
                        long low = pValue & 0xFFFFFFFFL;
                        MachineBatteryBlockEntity.this.energyStorage.setEnergy((energy & 0xFFFFFFFF00000000L) | low);
                    }
                    case 1 -> { // Старшая часть энергии
                        long high = ((long)pValue) << 32;
                        MachineBatteryBlockEntity.this.energyStorage.setEnergy((energy & 0x00000000FFFFFFFFL) | high);
                    }
                    // Ёмкость (case 2, 3) устанавливается только сервером, set() не нужен
                    case 4 -> MachineBatteryBlockEntity.this.energyDelta = pValue;
                    case 5 -> MachineBatteryBlockEntity.this.modeOnNoSignal = pValue;
                    case 6 -> MachineBatteryBlockEntity.this.modeOnSignal = pValue;
                    case 7 -> MachineBatteryBlockEntity.this.priority = Priority.values()[pValue];
                    default -> {} // Хорошая практика - иметь default
                }
            }
            @Override
            public int getCount() { return 8; } // [ИЗМЕНЕНИЕ] Увеличили кол-во
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
            case 0: canInput = true; canOutput = true; break;
            case 1: canInput = true; canOutput = false; break;
            case 2: canInput = false; canOutput = true; break;
            case 3: canInput = false; canOutput = false; break;
        }

        // 3. Выполняем разрешенные операции
        if (canInput) {
            pBlockEntity.chargeFromItem();
        }
        if (canOutput) {
            pBlockEntity.dischargeToItem();
            pBlockEntity.pushEnergyToNeighbors();
        }

        // [ИЗМЕНЕНИЕ] Используем getEnergyStored() из 'longEnergyWrapper', т.к. он учитывает redstone
        long currentEnergy = pBlockEntity.longEnergyWrapper.getEnergyStored();
        pBlockEntity.energyDelta = currentEnergy - pBlockEntity.lastEnergy;
        pBlockEntity.lastEnergy = currentEnergy;

        setChanged(pLevel, pPos, pState);
    }

    // [ИЗМЕНЕНИЕ] Логика зарядки от предмета (с поддержкой long и fallback'ом на int)
    private void chargeFromItem() {
        var stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        // 1. Пытаемся найти нашу ILongEnergyStorage
        LazyOptional<ILongEnergyStorage> longCap = stack.getCapability(ModCapabilities.LONG_ENERGY);

        if (longCap.isPresent()) {
            longCap.ifPresent(source -> {
                long canExtract = source.extractEnergy(energyStorage.getMaxReceive(), true);
                if (canExtract > 0) {
                    // Используем longEnergyWrapper, чтобы учесть redstone
                    long received = this.longEnergyWrapper.receiveEnergy(canExtract, false);
                    source.extractEnergy(received, false);
                }
            });
        } else {
            // 2. Если не нашли, ищем старую IEnergyStorage
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(source -> {
                // Оборачиваем int-хранилище в long-обертку
                ILongEnergyStorage wrappedSource = new ForgeToLongWrapper(source);

                // Используем long-методы (обертка сама усечет до int)
                long canExtract = wrappedSource.extractEnergy(energyStorage.getMaxReceive(), true);
                if (canExtract > 0) {
                    // Используем longEnergyWrapper, чтобы учесть redstone
                    long received = this.longEnergyWrapper.receiveEnergy(canExtract, false);
                    wrappedSource.extractEnergy(received, false);
                }
            });
        }
    }

    // [ИЗМЕНЕНИЕ] Логика разрядки в предмет (с поддержкой long и fallback'ом на int)
    private void dischargeToItem() {
        var stack = itemHandler.getStackInSlot(1);
        if (stack.isEmpty()) return;

        // 1. Пытаемся найти нашу ILongEnergyStorage
        LazyOptional<ILongEnergyStorage> longCap = stack.getCapability(ModCapabilities.LONG_ENERGY);

        if (longCap.isPresent()) {
            longCap.ifPresent(target -> {
                // Используем longEnergyWrapper, чтобы учесть redstone
                long canReceive = target.receiveEnergy(energyStorage.getMaxExtract(), true);
                if (canReceive > 0) {
                    long extracted = longEnergyWrapper.extractEnergy(canReceive, false);
                    target.receiveEnergy(extracted, false);
                }
            });
        } else {
            // 2. Если не нашли, ищем старую IEnergyStorage
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(target -> {
                // Оборачиваем int-хранилище в long-обертку
                ILongEnergyStorage wrappedTarget = new ForgeToLongWrapper(target);

                // Используем long-методы (обертка сама усечет до int)
                long canReceive = wrappedTarget.receiveEnergy(energyStorage.getMaxExtract(), true);
                if (canReceive > 0) {
                    long extracted = longEnergyWrapper.extractEnergy(canReceive, false);
                    wrappedTarget.receiveEnergy(extracted, false);
                }
            });
        }
    }

    // [ИЗМЕНЕНИЕ] Логика передачи соседям (с поддержкой long и fallback'ом на int)
    private void pushEnergyToNeighbors() {
        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[BATTERY >>>] pushEnergyToNeighbors at {} currentEnergy={}", this.worldPosition, this.energyStorage.getEnergyStored());
        }

        // [ИЗМЕНЕНИЕ] long. Используем longEnergyWrapper для redstone
        long energyToSend = this.longEnergyWrapper.extractEnergy(this.energyStorage.getMaxExtract(), true);
        UUID pushId = UUID.randomUUID();

        if (energyToSend <= 0L) {
            return;
        }

        Level lvl = this.level;
        if (lvl == null) return;

        final long[] totalSent = {0L}; // [ИЗМЕНЕНИЕ] long

        for (Direction direction : Direction.values()) {
            if (totalSent[0] >= energyToSend) break;

            BlockEntity neighbor = lvl.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) continue;

            // НОВОЕ: Пропускаем другие батареи чтобы избежать взаимной перекачки
            if (neighbor instanceof MachineBatteryBlockEntity otherBattery) {
                // [ИЗМЕНЕНИЕ] long
                long myEnergy = this.energyStorage.getEnergyStored();
                long theirEnergy = otherBattery.energyStorage.getEnergyStored();

                if (myEnergy <= theirEnergy) {
                    // ... (debug)
                    continue;
                }

                // [ИЗМЕНЕНИЕ] long
                long difference = myEnergy - theirEnergy;
                long toSend = Math.min(difference / 2, energyToSend - totalSent[0]);

                if (toSend > 0) {
                    // [ИЗМЕНЕНИЕ] long. Используем long-обертку соседа
                    long accepted = otherBattery.longEnergyWrapper.receiveEnergy(toSend, false);
                    if (accepted > 0) {
                        this.longEnergyWrapper.extractEnergy(accepted, false); // Извлекаем из нашей (с redstone)
                        totalSent[0] += accepted;
                        // ... (debug)
                    }
                }
                continue;
            }

            // Для проводов используем acceptEnergy
            if (neighbor instanceof WireBlockEntity wire) {
                // [ИЗМЕНЕНИЕ] long. (Предполагаем, что acceptEnergy теперь принимает long)
                long remaining = energyToSend - totalSent[0];
                long accepted = wire.acceptEnergy(remaining, pushId, this.worldPosition);
                if (accepted > 0) {
                    this.longEnergyWrapper.extractEnergy(accepted, false); // Извлекаем из нашей (с redstone)
                    totalSent[0] += accepted;
                }
                continue;
            }

            // [ИЗМЕНЕНИЕ] Для остальных устройств ищем СНАЧАЛА long cap
            LazyOptional<ILongEnergyStorage> longCap = neighbor.getCapability(ModCapabilities.LONG_ENERGY, direction.getOpposite());

            if (longCap.isPresent()) {
                // --- 1. Нашли ILongEnergyStorage ---
                longCap.ifPresent(neighborStorage -> {
                    if (neighborStorage.canReceive()) {
                        long remaining = energyToSend - totalSent[0];
                        long accepted = neighborStorage.receiveEnergy(remaining, false);
                        if (accepted > 0) {
                            this.longEnergyWrapper.extractEnergy(accepted, false);
                            totalSent[0] += accepted;
                        }
                    }
                });
            } else {
                // --- 2. Не нашли, ищем старый IEnergyStorage ---
                neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(neighborStorage -> {
                    // Оборачиваем его
                    ILongEnergyStorage wrappedStorage = new ForgeToLongWrapper(neighborStorage);
                    if (wrappedStorage.canReceive()) {
                        long remaining = energyToSend - totalSent[0];
                        // Передаем long, обертка сама усечет до int
                        long accepted = wrappedStorage.receiveEnergy(remaining, false);
                        if (accepted > 0) {
                            this.longEnergyWrapper.extractEnergy(accepted, false);
                            totalSent[0] += accepted;
                        }
                    }
                });
            }
        }
    }

    public void handleButtonPress(int buttonId) {
        // ... (эта логика не меняется)
        switch (buttonId) {
            case 0: modeOnNoSignal = (modeOnNoSignal + 1) % 4; break;
            case 1: modeOnSignal = (modeOnSignal + 1) % 4; break;
            case 2: priority = Priority.values()[(priority.ordinal() + 1) % Priority.values().length]; break;
        }
        setChanged();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @Nonnull Inventory pPlayerInventory, @Nonnull Player pPlayer) {
        return new MachineBatteryMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    // [ИЗМЕНЕНИЕ] Предоставляем ОБА capability
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.LONG_ENERGY) {
            return lazyLongEnergyHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyForgeEnergyHandler.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    // [ИЗМЕНЕНИЕ] Инициализируем ОБА energy handler'а
    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        // "Настоящий" handler - это наша long-обертка с redstone
        lazyLongEnergyHandler = LazyOptional.of(() -> this.longEnergyWrapper);
        // "Старый" (forge) handler - это обертка над нашей long-оберткой
        lazyForgeEnergyHandler = lazyLongEnergyHandler.lazyMap(LongToForgeWrapper::new);
    }

    // [ИЗМЕНЕНИЕ] Инвалидируем ОБА energy handler'а
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyLongEnergyHandler.invalidate();
        lazyForgeEnergyHandler.invalidate();
    }

    // ... (Методы customName, getDisplayName - не меняются)
    public void setCustomName(Component name) { this.customName = name; }
    public Component getCustomName() { return this.customName; }
    public boolean hasCustomName() { return this.customName != null; }
    @Override public Component getDisplayName() { return this.hasCustomName() ? this.customName : Component.translatable("block.hbm_m.machine_battery"); }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("inventory", itemHandler.serializeNBT());
        // [ИЗМЕНЕНИЕ] Сохраняем как long
        pTag.putLong("energy", energyStorage.getEnergyStored());
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
        // [ИЗМЕНЕНИЕ] Читаем как long
        energyStorage.setEnergy(pTag.getLong("energy"));
        modeOnNoSignal = pTag.getInt("modeOnNoSignal");
        modeOnSignal = pTag.getInt("modeOnSignal");
        priority = Priority.values()[pTag.getInt("priority")];
        if (pTag.contains("CustomName", 8)) {
            this.customName = Component.Serializer.fromJson(pTag.getString("CustomName"));
        }
        // [ИЗМЕНЕНИЕ] Обновляем lastEnergy после загрузки, чтобы дельта была корректной
        this.lastEnergy = energyStorage.getEnergyStored();
    }

    public void drops() {
        // ... (этот метод не меняется)
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    public int getComparatorPower() {
        // [ИЗМЕНЕНИЕ] Приводим long к double для корректного деления
        return (int) Math.floor(((double)this.energyStorage.getEnergyStored() / this.energyStorage.getMaxEnergyStored()) * 15.0);
    }
}