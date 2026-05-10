package com.hbm_m.block.entity;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.api.energy.ItemEnergyAccess;
//? if forge {
/*import com.hbm_m.api.energy.PackedEnergyCapabilityProvider;
import com.hbm_m.capability.ModCapabilities;
*///?}
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

//? if forge {
/*import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
*///?}

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
//?}

/**
 * Базовый класс для всех машин с энергией.
 * Реализует хранение энергии, инвентарь и синхронизацию.
 */
public abstract class BaseMachineBlockEntity extends BlockEntity implements MenuProvider, IEnergyProvider, IEnergyReceiver {

    // Инвентарь
    protected final ModItemStackHandler inventory;
    //? if forge {
    
    /*protected LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();*///?}

    // Энергия (long для больших значений)
    protected long energy = 0;
    protected long capacity;
    protected final long maxReceive;
    protected final long maxExtract;

    // Отслеживание изменения энергии (для GUI)
    private long lastEnergy = 0;
    private long energyDelta = 0;

    protected boolean networkInitialized = false;

    // Capability провайдеры (Forge)
    //? if forge {
    /*private final LazyOptional<IEnergyProvider> hbmProvider = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyReceiver> hbmReceiver = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    private final PackedEnergyCapabilityProvider feCapabilityProvider;*///?}

    // Провайдер TeamReborn Energy (Fabric)
    //? if fabric {
    private final EnergyStorage energyStorage = new EnergyStorage() {
        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            if (!canReceive()) return 0;
            long amount = Math.min(capacity - energy, Math.min(maxReceive, maxAmount));
            if (amount > 0) {
                transaction.addCloseCallback((ctx, result) -> {
                    if (result.wasCommitted()) setEnergyStored(energy + amount);
                });
            }
            return amount;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            if (!canExtract()) return 0;
            long amount = Math.min(energy, Math.min(maxExtract, maxAmount));
            if (amount > 0) {
                transaction.addCloseCallback((ctx, result) -> {
                    if (result.wasCommitted()) setEnergyStored(energy - amount);
                });
            }
            return amount;
        }

        @Override public long getAmount()   { return energy; }
        @Override public long getCapacity() { return capacity; }
    };

    public EnergyStorage getEnergyStorage() { return energyStorage; }

    /** Возвращает Transfer API Storage для предметов. Переопределяй в подклассах для sided-логики. */
    @Nullable
    public Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        return inventory.getStorage();
    }
    //?}

    /** Общий доступ к инвентарю машины (loader-agnostic). */
    public ModItemStackHandler getInventory() {
        return inventory;
    }

    /**
     * Loader-agnostic дроп содержимого инвентаря.
     * Используй это вместо Forge-only доступа через ITEM_HANDLER capability.
     */
    public void dropInventoryContents() {
        if (level == null) return;
        net.minecraft.world.SimpleContainer c = new net.minecraft.world.SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            c.setItem(i, inventory.getStackInSlot(i));
        }
        net.minecraft.world.Containers.dropContents(level, worldPosition, c);
    }

    /**
     *  ОСНОВНОЙ КОНСТРУКТОР для машин-потребителей.
     * По умолчанию, maxExtract = 0, потому что нехуй высасывать энергию из того, что не
     * должно её жрать. Машина - не батарейка. Запомни это, или я приду к тебе во сне.
     */
    public BaseMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                  int inventorySize, long capacity, long receiveRate) {
        this(type, pos, state, inventorySize, capacity, receiveRate, 0L);
    }

    /**
     *  ПОЛНЫЙ КОНСТРУКТОР. Используй это только для тех ебанутых случаев, когда
     * машина должна ВДРУГ начать отдавать энергию. Для батарей, например.
     * Хотя ты же сказал, что они ничего не наследуют. Ну, пусть будет. На всякий.
     */
    public BaseMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                  int inventorySize, long capacity, long maxReceive, long maxExtract) {
        super(type, pos, state);
        this.inventory = createInventoryHandler(inventorySize);
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        //? if forge {
        /*this.feCapabilityProvider = new PackedEnergyCapabilityProvider(this);*///?}
    }

    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (isCriticalSlot(slot)) sendUpdateToClient();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }
        };
    }

    // --- Абстрактные методы ---
    protected abstract Component getDefaultName();

    protected abstract boolean isItemValidForSlot(int slot, ItemStack stack);

    protected boolean isCriticalSlot(int slot) {
        return false;
    }

    // --- IEnergyProvider & IEnergyReceiver базовые методы ---
    @Override
    public long getEnergyStored() {
        return this.energy;
    }

    @Override
    public long getMaxEnergyStored() {
        return this.capacity;
    }

    /** Меняет энергетический кап машины (нужно некоторым портам, например химмашине). */
    protected final void setEnergyCapacity(long newCapacity) {
        long cap = Math.max(0L, newCapacity);
        if (cap == this.capacity) return;
        this.capacity = cap;
        if (this.energy > this.capacity) {
            this.energy = this.capacity;
        }
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public void setEnergyStored(long energy) {
        this.energy = Math.max(0, Math.min(this.capacity, energy));
        setChanged();
    }

    @Override
    public long getProvideSpeed() {
        return this.maxExtract;
    }

    @Override
    public long getReceiveSpeed() {
        return this.maxReceive;
    }

    @Override
    public IEnergyReceiver.Priority getPriority() {
        return Priority.NORMAL;
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    // --- IEnergyProvider методы ---
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;

        long energyExtracted = Math.min(this.energy, Math.min(this.maxExtract, maxExtract));
        if (!simulate && energyExtracted > 0) {
            setEnergyStored(this.energy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public boolean canExtract() {
        return this.maxExtract > 0 && this.energy > 0;
    }

    // --- IEnergyReceiver методы ---
    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;

        long energyReceived = Math.min(this.capacity - this.energy, Math.min(this.maxReceive, maxReceive));
        if (!simulate && energyReceived > 0) {
            setEnergyStored(this.energy + energyReceived);
        }
        return energyReceived;
    }

    @Override
    public boolean canReceive() {
        return this.maxReceive > 0 && this.energy < this.capacity;
    }

    // --- Отслеживание дельты энергии ---
    protected void updateEnergyDelta(long currentEnergy) {
        this.energyDelta = currentEnergy - this.lastEnergy;
        this.lastEnergy = currentEnergy;
    }

    public long getEnergyDelta() {
        return this.energyDelta;
    }

    // --- Ghost Items (для JEI) ---
    public NonNullList<ItemStack> getGhostItems() {
        return NonNullList.create();
    }

    public static NonNullList<ItemStack> createGhostItemsFromIngredients(List<Ingredient> ingredients) {
        NonNullList<ItemStack> ghostItems = NonNullList.create();
        for (Ingredient ingredient : ingredients) {
            ItemStack[] matchingStacks = ingredient.getItems();
            if (matchingStacks.length > 0) {
                ghostItems.add(matchingStacks[0].copy());
            }
        }
        return ghostItems;
    }

    // --- Настройка Fluid Capability (опционально) ---
    protected void setupFluidCapability() {
        // Переопределяется в подклассах при необходимости
    }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putLong("energy", energy);
        tag.putLong("capacity", capacity);
        tag.putLong("lastEnergy", lastEnergy);
        tag.putLong("energyDelta", energyDelta);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        energy = tag.getLong("energy");
        if (tag.contains("capacity")) {
            capacity = Math.max(0L, tag.getLong("capacity"));
        }
        lastEnergy = tag.getLong("lastEnergy");
        energyDelta = tag.getLong("energyDelta");
    }

    // --- Синхронизация ---
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    //? if forge {
    /*@Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }*///?}

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if forge {
    /*@Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) {
            load(pkt.getTag());
        }
    }*///?}

    protected void sendUpdateToClient() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ═══════════════════════════ Capabilities ════════════════════════════════

    //? if forge {
    /*@Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_PROVIDER)  return hbmProvider.cast();
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER)  return hbmReceiver.cast();
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) return hbmConnector.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)        return itemHandler.cast();

        LazyOptional<T> feCap = feCapabilityProvider.getCapability(cap, side);
        if (feCap.isPresent()) return feCap;

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        itemHandler = LazyOptional.of(() -> inventory);
        setupFluidCapability();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        hbmProvider.invalidate();
        hbmReceiver.invalidate();
        hbmConnector.invalidate();
        feCapabilityProvider.invalidate();
    }
    *///?}

    // ═══════════════════════════ Platform-abstracted energy helpers ════════════════════════════════

    /**
     * Извлекает энергию из предмета-батарейки в указанном слоте и заряжает машину.
     * Вся платформенная логика (Forge Capabilities / Fabric Transfer API) скрыта здесь.
     * Конкретные машины просто вызывают {@code chargeFromBatterySlot(BATTERY_SLOT)}.
     */
    protected void chargeFromBatterySlot(int slot) {
        ItemStack batteryStack = inventory.getStackInSlot(slot);
        if (batteryStack.isEmpty()) return;

        long energyNeeded = this.capacity - this.energy;
        if (energyNeeded <= 0) return;
        long maxTransfer = Math.min(energyNeeded, this.maxReceive);
        if (maxTransfer <= 0) return;

        boolean transferred = ItemEnergyAccess.getHbmProvider(batteryStack).map(itemEnergy -> {
            if (!itemEnergy.canExtract()) return false;
            long extracted = itemEnergy.extractEnergy(maxTransfer, false);
            if (extracted > 0) {
                setEnergyStored(this.energy + extracted);
                return true;
            }
            return false;
        }).orElse(false);

        if (transferred) return;

        //? if forge {
        /*batteryStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
            if (!itemEnergy.canExtract()) return;
            int intTransfer = (int) Math.min(Integer.MAX_VALUE, maxTransfer);
            if (intTransfer <= 0) return;
            int extracted = itemEnergy.extractEnergy(intTransfer, false);
            if (extracted > 0) {
                setEnergyStored(energy + extracted);
            }
        });
        *///?}

        //? if fabric {
        var fabricEnergy = EnergyStorage.ITEM.find(batteryStack, null);
        if (fabricEnergy == null || !fabricEnergy.supportsExtraction()) return;
        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long extracted = fabricEnergy.extract(maxTransfer, tx);
            if (extracted > 0) {
                setEnergyStored(energy + extracted);
                tx.commit();
            }
        }
        //?}
    }

    /**
     * Передаёт энергию из машины в предмет в указанном слоте (для генераторов).
     * Вся платформенная логика скрыта здесь.
     * Конкретные машины-генераторы вызывают {@code chargeItemInSlot(CHARGE_SLOT)}.
     */
    protected void chargeItemInSlot(int slot) {
        if (this.energy <= 0) return;
        ItemStack itemToCharge = inventory.getStackInSlot(slot);
        if (itemToCharge.isEmpty()) return;

        long toTransfer = Math.min(this.energy, this.maxExtract > 0 ? this.maxExtract : this.maxReceive);
        if (toTransfer <= 0) return;

        //? if forge {
        /*var hbmCap = itemToCharge.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER);
        if (hbmCap.isPresent()) {
            hbmCap.ifPresent(target -> {
                if (!target.canReceive()) return;
                long accepted = target.receiveEnergy(toTransfer, false);
                if (accepted > 0) setEnergyStored(energy - accepted);
            });
            return;
        }
        itemToCharge.getCapability(ForgeCapabilities.ENERGY).ifPresent(target -> {
            if (!target.canReceive()) return;
            int maxTransfer = (int) Math.min(toTransfer, Integer.MAX_VALUE);
            if (maxTransfer <= 0) return;
            int accepted = target.receiveEnergy(maxTransfer, false);
            if (accepted > 0) setEnergyStored(energy - accepted);
        });
        *///?}

        //? if fabric {
        var hbm = ItemEnergyAccess.getHbmReceiver(itemToCharge);
        if (hbm.isPresent()) {
            var target = hbm.get();
            if (!target.canReceive()) return;
            long accepted = target.receiveEnergy(toTransfer, false);
            if (accepted > 0) setEnergyStored(energy - accepted);
            return;
        }
        var target = EnergyStorage.ITEM.find(itemToCharge, null);
        if (target == null || !target.supportsInsertion()) return;
        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long accepted = target.insert(toTransfer, tx);
            if (accepted > 0) {
                setEnergyStored(energy - accepted);
                tx.commit();
            }
        }
        //?}
    }

    /**
     * Проверяет, является ли предмет источником энергии (для валидации батарейного слота).
     * Используй в {@link #isItemValidForSlot} вместо платформенных проверок.
     */
    protected static boolean isEnergyProviderItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ItemEnergyAccess.getHbmProvider(stack).map(p -> p.canExtract()).orElse(false)) return true;
        //? if forge {
        /*return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(net.minecraftforge.energy.IEnergyStorage::canExtract).orElse(false);
        *///?}
        //? if fabric {
        var es = EnergyStorage.ITEM.find(stack, null);
        return es != null && es.supportsExtraction();
        //?}
    }

    /**
     * Проверяет, является ли предмет приёмником энергии (для валидации зарядного слота).
     * Используй в {@link #isItemValidForSlot} вместо платформенных проверок.
     */
    protected static boolean isEnergyReceiverItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ItemEnergyAccess.getHbmReceiver(stack).isPresent()) return true;
        //? if forge {
        /*return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(net.minecraftforge.energy.IEnergyStorage::canReceive).orElse(false);
        *///?}
        //? if fabric {
        var es = EnergyStorage.ITEM.find(stack, null);
        return es != null && es.supportsInsertion();
        //?}
    }

    /**
     * Рендер-баундинг бокс по умолчанию.
     * На Forge это {@code @Override} метода из BlockEntity, на Fabric — обычный public метод.
     * Подклассы могут переопределить для мультиблоков с увеличенным размером.
     */
    //? if forge {
    /*@Override
    *///?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.5D);
    }

    // ═══════════════════════════ Network initialization ════════════════════════════════

    protected void ensureNetworkInitialized() {
        if (!networkInitialized && level != null && !level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) level).addNode(this.getBlockPos());
            networkInitialized = true;
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



    // И при загрузке/установке блока:
    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
        //? if fabric {
        setupFluidCapability();
        //?}
    }
}