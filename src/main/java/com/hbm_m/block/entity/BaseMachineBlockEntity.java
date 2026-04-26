package com.hbm_m.block.entity;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.EnergyNetworkManager;
//? if forge {
import com.hbm_m.api.energy.PackedEnergyCapabilityProvider;
import com.hbm_m.capability.ModCapabilities;
//?}
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

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
*///?}

/**
 * Базовый класс для всех машин с энергией.
 * Реализует хранение энергии, инвентарь и синхронизацию.
 */
public abstract class BaseMachineBlockEntity extends BlockEntity implements MenuProvider, IEnergyProvider, IEnergyReceiver {

    // Инвентарь
    protected final ModItemStackHandler inventory;
    //? if forge {
    
    protected LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();//?}

    // Энергия (long для больших значений)
    protected long energy = 0;
    protected final long capacity;
    protected final long maxReceive;
    protected final long maxExtract;

    // Отслеживание изменения энергии (для GUI)
    private long lastEnergy = 0;
    private long energyDelta = 0;

    protected boolean networkInitialized = false;

    // Capability провайдеры (Forge)
    //? if forge {
    private final LazyOptional<IEnergyProvider> hbmProvider = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyReceiver> hbmReceiver = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    private final PackedEnergyCapabilityProvider feCapabilityProvider;//?}

    // Провайдер TeamReborn Energy (Fabric)
    //? if fabric {
    /*private final EnergyStorage energyStorage = new EnergyStorage() {
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

    /^* Возвращает Transfer API Storage для предметов. Переопределяй в подклассах для sided-логики. ^/
    @Nullable
    public Storage<ItemVariant> getItemStorage(@Nullable Direction side) {
        return inventory.getStorage();
    }
    *///?}

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
        this.feCapabilityProvider = new PackedEnergyCapabilityProvider(this);//?}
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

    public ModItemStackHandler getInventory() { return this.inventory; }

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

    //? if forge {
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }//?}

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if forge {
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) {
            load(pkt.getTag());
        }
    }//?}

    protected void sendUpdateToClient() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ═══════════════════════════ Capabilities ════════════════════════════════

    //? if forge {
    @Override
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
    //?}

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
        /*setupFluidCapability();
        *///?}
    }
}