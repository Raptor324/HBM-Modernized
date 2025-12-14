package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.api.energy.*;
import com.hbm_m.capability.ModCapabilities;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Базовый класс для всех машин с энергией.
 * Реализует хранение энергии, инвентарь и синхронизацию.
 */
public abstract class BaseMachineBlockEntity extends BlockEntity implements MenuProvider, IEnergyProvider, IEnergyReceiver {

    // Инвентарь
    protected final ItemStackHandler inventory;
    protected LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();

    // Энергия (long для больших значений)
    protected long energy = 0;
    protected final long capacity;
    protected final long maxReceive;
    protected final long maxExtract;

    // Отслеживание изменения энергии (для GUI)
    private long lastEnergy = 0;
    private long energyDelta = 0;

    protected boolean networkInitialized = false;

    // Capability провайдеры
    private final LazyOptional<IEnergyProvider> hbmProvider = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyReceiver> hbmReceiver = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    private final PackedEnergyCapabilityProvider feCapabilityProvider;

    /**
     * ✅ ОСНОВНОЙ КОНСТРУКТОР для машин-потребителей.
     * По умолчанию, maxExtract = 0, потому что нехуй высасывать энергию из того, что
     * должно её жрать. Машина - не батарейка. Запомни это, или я приду к тебе во сне.
     */
    public BaseMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                  int inventorySize, long capacity, long receiveRate) {
        this(type, pos, state, inventorySize, capacity, receiveRate, 0L);
    }

    /**
     * ✅ ПОЛНЫЙ КОНСТРУКТОР. Используй это только для тех ебанутых случаев, когда
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
        this.feCapabilityProvider = new PackedEnergyCapabilityProvider(this);
    }

    protected ItemStackHandler createInventoryHandler(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (isCriticalSlot(slot)) {
                    sendUpdateToClient();
                }
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }
        };
    }

    public ItemStackHandler getInventory() {
        return this.inventory;
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
        tag.putLong("lastEnergy", lastEnergy);
        tag.putLong("energyDelta", energyDelta);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        energy = tag.getLong("energy");
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

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(pkt.getTag());
    }

    protected void sendUpdateToClient() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- Capabilities ---
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_PROVIDER) {
            return hbmProvider.cast();
        }
        if (cap == ModCapabilities.HBM_ENERGY_RECEIVER) {
            return hbmReceiver.cast();
        }
        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return hbmConnector.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }

        LazyOptional<T> feCap = feCapabilityProvider.getCapability(cap, side);
        if (feCap.isPresent()) return feCap;

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        itemHandler = LazyOptional.of(() -> inventory);
        setupFluidCapability();
        // Сеть инициализируем позже, в тике
    }

    protected void ensureNetworkInitialized() {
        if (!networkInitialized && level != null && !level.isClientSide) {
            EnergyNetworkManager.get((ServerLevel) level).addNode(this.getBlockPos());
            networkInitialized = true;
        }
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
    }
}