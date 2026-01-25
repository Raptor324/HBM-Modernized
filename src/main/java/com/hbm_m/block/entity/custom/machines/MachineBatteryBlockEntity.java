package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.api.energy.*;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.api.energy.MachineBatteryBlock;
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
    private long lastEnergy = 0;

    // Режимы работы (0 = BOTH, 1 = INPUT, 2 = OUTPUT, 3 = DISABLED)
    public int modeOnNoSignal = 0;
    public int modeOnSignal = 0;
    private Priority priority = Priority.LOW;
    private long energyDelta = 0;

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

    // [ИСПРАВЛЕНИЕ] Убрали final и инициализацию в конструкторе (кроме empty)
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyProvider> hbmProvider = LazyOptional.empty();
    private LazyOptional<IEnergyReceiver> hbmReceiver = LazyOptional.empty();
    private LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.empty();

    // Forge Energy враппер тоже должен пересоздаваться, если он зависит от полей,
    // но поскольку PackedEnergyCapabilityProvider обычно создает свой LazyOptional внутри,
    // проверь его реализацию. Если он хранит LazyOptional как поле, его тоже надо обновлять.
    // Для надежности пересоздадим и его, если он не является простым прокси.
    private PackedEnergyCapabilityProvider feCapabilityProvider;

    protected final ContainerData data;

    public MachineBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_BATTERY_BE.get(), pos, state);
        this.capacity = state.getBlock() instanceof MachineBatteryBlock b ? b.getCapacity() : 9_000_000_000_000_000_000L;
        this.transferRate = 100_000_000_000L;

        // Инициализируем провайдер FE (но сам LazyOptional внутри него должен быть валидным)
        this.feCapabilityProvider = new PackedEnergyCapabilityProvider(this);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> modeOnNoSignal;       // Сдвинули на 0
                    case 1 -> modeOnSignal;         // Сдвинули на 1
                    case 2 -> priority.ordinal();   // Сдвинули на 2
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> modeOnNoSignal = value;
                    case 1 -> modeOnSignal = value;
                    case 2 -> priority = Priority.values()[Math.max(0, Math.min(value, Priority.values().length - 1))];
                }
            }

            @Override
            public int getCount() {
                return 3; // Теперь у нас всего 3 int-параметра
            }
        };
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // [ИСПРАВЛЕНИЕ] Создаем новые LazyOptional при загрузке чанка/мира
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        hbmProvider = LazyOptional.of(() -> this);
        hbmReceiver = LazyOptional.of(() -> this);
        hbmConnector = LazyOptional.of(() -> this);

        // Если PackedEnergyCapabilityProvider хранит LazyOptional внутри себя,
        // убедись, что он не закэшировал старый invalid optional.
        // Обычно лучше пересоздать сам враппер, если он легкий.
        // this.feCapabilityProvider = new PackedEnergyCapabilityProvider(this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        // [ИСПРАВЛЕНИЕ] Инвалидируем текущие optional
        if (hbmConnector.isPresent()) hbmConnector.invalidate();
        if (lazyItemHandler.isPresent()) lazyItemHandler.invalidate();
        if (hbmProvider.isPresent()) hbmProvider.invalidate();
        if (hbmReceiver.isPresent()) hbmReceiver.invalidate();

        // Не забываем про FE провайдер
        if (feCapabilityProvider != null) feCapabilityProvider.invalidate();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBatteryBlockEntity be) {
        if (level.isClientSide) return;

        EnergyNetworkManager manager = EnergyNetworkManager.get((ServerLevel) level);
        if (!manager.hasNode(pos)) {
            manager.addNode(pos);
        }

        long gameTime = level.getGameTime();

        if (gameTime % 10 == 0) {
            be.energyDelta = (be.energy - be.lastEnergy) / 10;
            be.lastEnergy = be.energy;
        }

        be.chargeFromItem();
        be.dischargeToItem();
    }

    // =========================================================
    // ФИКС БАТАРЕЙ: Гибридная логика (Native Long -> Forge Int)
    // =========================================================

    private void chargeFromItem() {
        // 0. Проверки на наличие предмета и места
        var stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;

        long spaceAvailable = capacity - energy;
        if (spaceAvailable <= 0) return;

        // 1. ПОПЫТКА HBM (Родная система)
        // Если это твоя батарейка, используем long на полную катушку.
        // Это позволяет заряжать Шрабидиевую батарею со скоростью 100 млрд/тик.
        var hbmCap = stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER);
        if (hbmCap.isPresent()) {
            hbmCap.ifPresent(source -> {
                if (!source.canExtract()) return;

                // Math.min с long - никаких переполнений
                long toExtract = Math.min(transferRate, spaceAvailable);
                long extracted = source.extractEnergy(toExtract, false);

                if (extracted > 0) {
                    this.energy += extracted;
                    setChanged();
                }
            });
            return; // Успех, выходим. Forge не нужен.
        }

        // 2. ФОЛЛБЭК НА FORGE (Чужие моды / Совместимость)
        // Если это батарейка из Mekanism/Thermal, используем int с защитой.
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(source -> {
            if (!source.canExtract()) return;

            long wanted = Math.min(transferRate, spaceAvailable);

            // [ВАЖНО] ЗАЩИТА ОТ ПЕРЕПОЛНЕНИЯ
            // Если мы хотим больше 2.14 млрд, обрезаем до Integer.MAX_VALUE.
            // Без этого (int) превратит 25 млрд в отрицательное число.
            int maxTransfer = (int) Math.min(wanted, Integer.MAX_VALUE);

            int extracted = source.extractEnergy(maxTransfer, false);
            if (extracted > 0) {
                this.energy += extracted;
                setChanged();
            }
        });
    }

    private void dischargeToItem() {
        // 0. Проверки
        var stack = itemHandler.getStackInSlot(1);
        if (stack.isEmpty()) return;

        long availableEnergy = energy;
        if (availableEnergy <= 0) return;

        // 1. ПОПЫТКА HBM (Родная система)
        var hbmCap = stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER);
        if (hbmCap.isPresent()) {
            hbmCap.ifPresent(target -> {
                if (!target.canReceive()) return;

                long toTransfer = Math.min(transferRate, availableEnergy);
                long accepted = target.receiveEnergy(toTransfer, false);

                if (accepted > 0) {
                    this.energy -= accepted;
                    setChanged();
                }
            });
            return;
        }

        // 2. ФОЛЛБЭК НА FORGE (Совместимость)
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(target -> {
            if (!target.canReceive()) return;

            long wanted = Math.min(transferRate, availableEnergy);

            // [ВАЖНО] Защита от переполнения int
            int maxTransfer = (int) Math.min(wanted, Integer.MAX_VALUE);

            int accepted = target.receiveEnergy(maxTransfer, false);
            if (accepted > 0) {
                this.energy -= accepted;
                setChanged();
            }
        });
    }

    public int getCurrentMode() {
        if (level == null) return modeOnNoSignal;
        return level.hasNeighborSignal(this.worldPosition) ? modeOnSignal : modeOnNoSignal;
    }
    public long getEnergyDelta() {
        return this.energyDelta;
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
        return (mode == 0 || mode == 1) && this.energy < this.capacity;
    }

    // --- Capabilities ---
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }

        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return hbmConnector.cast();
        }

        int mode = getCurrentMode();

        if (cap == ModCapabilities.HBM_ENERGY_PROVIDER && (mode == 0 || mode == 2)) {
            return hbmProvider.cast();
        }

        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER && (mode == 0 || mode == 1)) {
            return hbmReceiver.cast();
        }

        LazyOptional<T> feCap = feCapabilityProvider.getCapability(cap, side);
        if (feCap.isPresent()) return feCap;

        return super.getCapability(cap, side);
    }

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
        // [ИСПРАВЛЕНИЕ] Обновили индексы для кнопок
        switch (buttonId) {
            case 0 -> this.data.set(0, (this.modeOnNoSignal + 1) % 4); // Индекс 0
            case 1 -> this.data.set(1, (this.modeOnSignal + 1) % 4);   // Индекс 1
            case 2 -> {
                Priority[] priorities = Priority.values();
                int currentIndex = this.priority.ordinal();
                int nextIndex = (currentIndex + 1) % priorities.length;
                this.data.set(2, nextIndex); // Индекс 2
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) this.level).removeNode(this.getBlockPos());
        }
    }
}