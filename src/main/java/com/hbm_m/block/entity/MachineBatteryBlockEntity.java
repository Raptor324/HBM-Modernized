package com.hbm_m.block.entity;

import com.hbm_m.api.energy.*;
import com.hbm_m.block.MachineBatteryBlock;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.menu.MachineBatteryMenu;
import com.hbm_m.util.LongDataPacker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

/**
 * Энергохранилище с настраиваемыми режимами работы.
 * Режимы: 0 = BOTH, 1 = INPUT, 2 = OUTPUT, 3 = DISABLED
 */
public class MachineBatteryBlockEntity extends BlockEntity implements MenuProvider, IEnergyProvider, IEnergyReceiver {

    private final long capacity;
    private final long transferRate;
    private long energy = 0;
    private long lastEnergy = 0; // Для расчёта дельты

    // Режимы работы (0 = BOTH, 1 = INPUT, 2 = OUTPUT, 3 = DISABLED)
    public int modeOnNoSignal = 0; // По умолчанию BOTH
    public int modeOnSignal = 0;   // По умолчанию BOTH
    private Priority priority = Priority.LOW;
    private long energyDelta = 0; // [🔥 ДОБАВЬ ЭТО ПОЛЕ 🔥]

    // Инвентарь для предметов (2 слота)
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull net.minecraft.world.item.ItemStack stack) {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final LazyOptional<IEnergyProvider> hbmProvider = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyReceiver> hbmReceiver = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    private final PackedEnergyCapabilityProvider feCapabilityProvider;

    protected final ContainerData data;

    public MachineBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_BATTERY_BE.get(), pos, state);
        this.capacity = state.getBlock() instanceof MachineBatteryBlock b ? b.getCapacity() : 9_000_000_000_000_000_000L;
        this.transferRate = 100_000_000_000L; // 0.5% за тик
        this.feCapabilityProvider = new PackedEnergyCapabilityProvider(this);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> LongDataPacker.packHigh(energy);      // energy high
                    case 1 -> LongDataPacker.packLow(energy);       // energy low
                    case 2 -> LongDataPacker.packHigh(capacity);    // capacity high
                    case 3 -> LongDataPacker.packLow(capacity);     // capacity low
                    case 4 -> (int) energyDelta;                          // energy delta
                    case 5 -> modeOnNoSignal;
                    case 6 -> modeOnSignal;
                    case 7 -> priority.ordinal();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 5 -> modeOnNoSignal = value;
                    case 6 -> modeOnSignal = value;
                    case 7 -> priority = Priority.values()[Math.max(0, Math.min(value, Priority.values().length - 1))];
                }
            }

            @Override
            public int getCount() {
                return 8;
            }
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBatteryBlockEntity be) {
        if (level.isClientSide) return;

        long gameTime = level.getGameTime();

        // Обновление дельты энергии каждые 10 тиков
        if (gameTime % 10 == 0) {
            // [ИЗМЕНЕНО] Рассчитываем среднюю дельту за последние 10 тиков
            be.energyDelta = (be.energy - be.lastEnergy) / 10;
            // Сохраняем текущую энергию для *следующего* расчета
            be.lastEnergy = be.energy;
        }

        // Зарядка/разрядка предметов каждый тик
        be.chargeFromItem();
        be.dischargeToItem();
    }

    private void chargeFromItem() {
        itemHandler.getStackInSlot(0).getCapability(ForgeCapabilities.ENERGY).ifPresent(source -> {
            if (!source.canExtract()) return;
            long spaceAvailable = capacity - energy;
            if (spaceAvailable <= 0) return;

            int maxTransfer = (int) Math.min(transferRate, spaceAvailable);
            int extracted = source.extractEnergy(maxTransfer, false);
            if (extracted > 0) {
                energy += extracted;
                setChanged();
            }
        });
    }

    private void dischargeToItem() {
        itemHandler.getStackInSlot(1).getCapability(ForgeCapabilities.ENERGY).ifPresent(target -> {
            if (!target.canReceive()) return;
            long availableEnergy = energy;
            if (availableEnergy <= 0) return;

            int maxTransfer = (int) Math.min(transferRate, availableEnergy);
            int received = target.receiveEnergy(maxTransfer, false);
            if (received > 0) {
                energy -= received;
                setChanged();
            }
        });
    }

    /**
     * Получить текущий режим работы с учётом redstone-сигнала
     */
    public int getCurrentMode() {
        if (level == null) return modeOnNoSignal;
        return level.hasNeighborSignal(this.worldPosition) ? modeOnSignal : modeOnNoSignal;
    }

    // --- IEnergyProvider & IEnergyReceiver ---
    @Override
    public long getEnergyStored() {
        return this.energy;
    }

    @Override
    public long getMaxEnergyStored() {
        return this.capacity;
    }

    @Override
    public void setEnergyStored(long energy) {
        this.energy = Math.max(0, Math.min(this.capacity, energy));
        setChanged();
    }

    @Override
    public long getProvideSpeed() {
        return this.transferRate;
    }

    @Override
    public long getReceiveSpeed() {
        return this.transferRate;
    }

    @Override
    public Priority getPriority() {
        return this.priority;
    }

    public void setPriority(Priority p) {
        this.priority = p;
        setChanged();
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        long energyExtracted = Math.min(this.energy, Math.min(this.transferRate, maxExtract));
        if (!simulate && energyExtracted > 0) {
            setEnergyStored(this.energy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public boolean canExtract() {
        int mode = getCurrentMode();
        // BOTH (0) или OUTPUT (2)
        return (mode == 0 || mode == 2) && this.energy > 0;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        long energyReceived = Math.min(this.capacity - this.energy, Math.min(this.transferRate, maxReceive));
        if (!simulate && energyReceived > 0) {
            setEnergyStored(this.energy + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public boolean canReceive() {
        int mode = getCurrentMode();
        // BOTH (0) или INPUT (1)
        return (mode == 0 || mode == 1) && this.energy < this.capacity;
    }

    // --- Capabilities ---
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }

        // [ИЗМЕНЕНО] Сначала проверяем базовый коннектор.
        // Батарея - это *всегда* узел сети, даже в режиме "DISABLED".
        // Это не дает сети "потерять" узел, когда он выключен.
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return hbmConnector.cast(); // <-- Теперь мы возвращаем кэшированное поле
        }

        int mode = getCurrentMode();

        // HBM Provider (только если режим BOTH или OUTPUT)
        if (cap == ModCapabilities.HBM_ENERGY_PROVIDER && (mode == 0 || mode == 2)) {
            return hbmProvider.cast();
        }

        // HBM Receiver (только если режим BOTH или INPUT)
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER && (mode == 0 || mode == 1)) {
            return hbmReceiver.cast();
        }

        // Forge Energy (всегда доступен)
        LazyOptional<T> feCap = feCapabilityProvider.getCapability(cap, side);
        if (feCap.isPresent()) return feCap;

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        hbmConnector.invalidate();
        lazyItemHandler.invalidate();
        hbmProvider.invalidate();
        hbmReceiver.invalidate();
        feCapabilityProvider.invalidate();
    }

    // --- NBT ---
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.energy = tag.getLong("Energy");
        this.lastEnergy = tag.getLong("lastEnergy");
        this.energyDelta = tag.getLong("energyDelta");
        this.modeOnNoSignal = tag.getInt("modeOnNoSignal");
        this.modeOnSignal = tag.getInt("modeOnSignal");
        if (tag.contains("priority")) {
            int priorityIndex = tag.getInt("priority");
            this.priority = Priority.values()[Math.max(0, Math.min(priorityIndex, Priority.values().length - 1))];
        }
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", this.energy);
        tag.putInt("modeOnNoSignal", this.modeOnNoSignal);
        tag.putLong("lastEnergy", this.lastEnergy);
        tag.putLong("energyDelta", this.energyDelta);
        tag.putInt("modeOnSignal", this.modeOnSignal);
        tag.putInt("priority", this.priority.ordinal());
        tag.put("Inventory", itemHandler.serializeNBT());
    }

    // --- GUI ---
    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new MachineBatteryMenu(windowId, playerInventory, this, this.data);
    }

    public void handleButtonPress(int buttonId) {
        switch (buttonId) {
            // [ФИКС] Меняем значения через this.data.set(...)
            case 0 -> this.data.set(5, (this.modeOnNoSignal + 1) % 4);
            case 1 -> this.data.set(6, (this.modeOnSignal + 1) % 4);

            // [ФИКС] Теперь мы меняем data, которую видит GUI
            case 2 -> {
                Priority[] priorities = Priority.values();
                int currentIndex = this.priority.ordinal();
                int nextIndex = (currentIndex + 1) % priorities.length;
                this.data.set(7, nextIndex); // <-- Вот и всё
            }
        }

        // Эти строки нужны для NBT, оставляем их
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
        if (!pLevel.isClientSide) {
            // [ВАЖНО!] Сообщаем сети, что мы добавлены (при загрузке чанка/мира)
            EnergyNetworkManager.get((ServerLevel) pLevel).addNode(this.getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // [ВАЖНО!] Сообщаем сети, что мы удалены
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        // [ВАЖНО!] Также сообщаем при выгрузке чанка
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }
}


