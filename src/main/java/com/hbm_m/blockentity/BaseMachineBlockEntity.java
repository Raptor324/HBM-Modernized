package com.hbm_m.blockentity;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.energy.ItemEnergyAccess;

import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.PlatformHooks;

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
import com.hbm_m.api.energy.PackedEnergyCapabilityProvider;
import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?}

//? if neoforge {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?}

/**
 * Базовый класс для всех машин с энергией.
 * Реализует хранение энергии, инвентарь и синхронизацию.
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class BaseMachineBlockEntity extends BaseHbmBlockEntity implements MenuProvider, IEnergyProvider, IEnergyReceiver {

    // Инвентарь
    protected final ModItemStackHandler inventory;
    //? if forge {
    
    protected LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();//?}

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
    private final LazyOptional<IEnergyProvider> hbmProvider = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyReceiver> hbmReceiver = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    private final PackedEnergyCapabilityProvider feCapabilityProvider;

    // Fluid-капабилити (Forge). Подклассы регистрируют обработчик через setFluidHandler().
    protected LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluidHandlerOpt = LazyOptional.empty();
    //?}
    //? if neoforge {
    /*// Fluid-капабилити (NeoForge): без LazyOptional, храним сам объект-обработчик.
    protected Object fluidHandlerNeo;
    *///?}


    /** Общий доступ к инвентарю машины (loader-agnostic). */
    public ModItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel serverLevel && !serverLevel.isClientSide()) {
            BlockPos pos = this.getBlockPos();
            // Ставим тик-задачу на СЛЕДУЮЩИЙ тик, а не выполняем прямо сейчас
            serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                    serverLevel.getServer().getTickCount() + 1,
                    () -> {
                        if (serverLevel.isLoaded(pos)) {
                            BlockState state = serverLevel.getBlockState(pos);
                            if (state.getBlock() instanceof IMultiblockController controller) {
                                controller.getStructureHelper().attemptAutoRepair(serverLevel, pos, state, controller);
                            }
                        }
                    }
            ));
        }
    }

    /**
     * Loader-agnostic дроп содержимого инвентаря.
     * Используй это вместо Forge-only доступа через ITEM_HANDLER capability.
     */
    public void dropInventoryContents() {
        if (level == null) return;
        // Во время переноса блоков движком сборки (Create/Sable) содержимое уезжает
        // в NBT-снимок контрапшена; высыпание на пол здесь = дюп предметов.
        if (com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) {
            com.hbm_m.main.MainRegistry.LOGGER.info(
                "[HBM] высыпание инвентаря подавлено (окно сборки контрапшена), BE {}",
                getClass().getSimpleName());
            return;
        }
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

    /**
     * Регистрирует обработчик жидкости, который BaseMachineBlockEntity будет отдавать через
     * {@code ForgeCapabilities.FLUID_HANDLER}. Вызывать из {@link #setupFluidCapability()} подкласса.
     * <p>Можно передать:
     * <ul>
     *   <li>HBM {@link com.hbm_m.inventory.fluid.tank.FluidTank} — тогда переиспользуется его
     *       внутренний {@code LazyOptional<IFluidHandler>};</li>
     *   <li>любой {@code net.minecraftforge.fluids.capability.IFluidHandler} — кастомный враппер,
     *       Forge {@code FluidTank} и т.п.</li>
     * </ul>
     */
    protected void setFluidHandler(Object handler) {
        //? if forge {
        if (handler instanceof com.hbm_m.inventory.fluid.tank.FluidTank tank) {
            this.fluidHandlerOpt = (net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler>) tank.getCapability();
        } else {
            this.fluidHandlerOpt = LazyOptional.of(() -> (net.minecraftforge.fluids.capability.IFluidHandler) handler);
        }
        //?}
        //? if neoforge {
        /*// На NeoForge нет LazyOptional — капабилити это обычный объект.
        // HBM FluidTank отдаёт свой backend напрямую, иначе храним переданный handler как есть.
        if (handler instanceof com.hbm_m.inventory.fluid.tank.FluidTank tank) {
            this.fluidHandlerNeo = tank.getCapability();
        } else {
            this.fluidHandlerNeo = handler;
        }
        *///?}
    }

    protected void setupFluidCapability() {
        // Переопределяется в подклассах при необходимости
    }

    // --- NBT ---
    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        tag.putLong("energy", energy);
        tag.putLong("capacity", capacity);
        tag.putLong("lastEnergy", lastEnergy);
        tag.putLong("energyDelta", energyDelta);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag inventoryTag = tag.getCompound("inventory");
        if (inventoryTag.contains("Size")) {
            inventoryTag.putInt("Size", inventory.getSlots());
        }
        com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, inventoryTag, registries);
        energy = tag.getLong("energy");
        if (tag.contains("capacity")) {
            capacity = Math.max(0L, tag.getLong("capacity"));
        }
        lastEnergy = tag.getLong("lastEnergy");
        energyDelta = tag.getLong("energyDelta");
    }

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
        if (cap == ForgeCapabilities.FLUID_HANDLER && fluidHandlerOpt.isPresent()) return fluidHandlerOpt.cast();

        LazyOptional<T> feCap = feCapabilityProvider.getCapability(cap, side);
        if (feCap.isPresent()) return feCap;

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        itemHandler = LazyOptional.of(this::getAutomationItemHandler);
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
        fluidHandlerOpt.invalidate();
    }
    //?}

    /**
     * Handler, который видит автоматика (воронки/трубы) через ITEM_HANDLER-капабилити и
     * полиморфный {@link #getItemHandler}. GUI работает с полным инвентарём через
     * {@code ModItemStackHandlerContainer} и ограничений не имеет.
     * Переопределяется машинами с оригинальной slot-логикой автоматизации
     * (аналог {@code getAccessibleSlotsFromSide}/{@code canExtractItem} 1.7.10).
     */
    protected ModItemStackHandler getAutomationItemHandler() {
        return inventory;
    }

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
        batteryStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
            if (!itemEnergy.canExtract()) return;
            int intTransfer = (int) Math.min(Integer.MAX_VALUE, maxTransfer);
            if (intTransfer <= 0) return;
            int extracted = itemEnergy.extractEnergy(intTransfer, false);
            if (extracted > 0) {
                setEnergyStored(energy + extracted);
            }
        });
        //?}

        //? if neoforge {
        /*IEnergyStorage itemEnergy = batteryStack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (itemEnergy == null || !itemEnergy.canExtract()) return;
        int intTransfer = (int) Math.min(Integer.MAX_VALUE, maxTransfer);
        if (intTransfer <= 0) return;
        int extracted = itemEnergy.extractEnergy(intTransfer, false);
        if (extracted > 0) {
            setEnergyStored(energy + extracted);
        }
        *///?}
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
        var hbmCap = itemToCharge.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER);
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
        //?}

        //? if neoforge {
        /*// Сначала HBM-приёмник (кастомная capability предмета), затем FE через NeoForge Capabilities.
        var hbm = ItemEnergyAccess.getHbmReceiver(itemToCharge);
        if (hbm.isPresent()) {
            var target = hbm.get();
            if (!target.canReceive()) return;
            long accepted = target.receiveEnergy(toTransfer, false);
            if (accepted > 0) setEnergyStored(energy - accepted);
            return;
        }
        IEnergyStorage target = itemToCharge.getCapability(Capabilities.EnergyStorage.ITEM);
        if (target == null || !target.canReceive()) return;
        int maxTransfer = (int) Math.min(toTransfer, Integer.MAX_VALUE);
        if (maxTransfer <= 0) return;
        int accepted = target.receiveEnergy(maxTransfer, false);
        if (accepted > 0) setEnergyStored(energy - accepted);
        *///?}
    }

    /**
     * Проверяет, является ли предмет источником энергии (для валидации батарейного слота).
     * Используй в {@link #isItemValidForSlot} вместо платформенных проверок.
     */
    protected static boolean isEnergyProviderItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ItemEnergyAccess.getHbmProvider(stack).map(p -> p.canExtract()).orElse(false)) return true;
        //? if forge {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(net.minecraftforge.energy.IEnergyStorage::canExtract).orElse(false);
        //?}
        //? if neoforge {
        /*IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canExtract();
        *///?}
    }

    /**
     * Проверяет, является ли предмет приёмником энергии (для валидации зарядного слота).
     * Используй в {@link #isItemValidForSlot} вместо платформенных проверок.
     */
    protected static boolean isEnergyReceiverItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ItemEnergyAccess.getHbmReceiver(stack).isPresent()) return true;
        //? if forge {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(net.minecraftforge.energy.IEnergyStorage::canReceive).orElse(false);
        //?}
        //? if neoforge {
        /*IEnergyStorage cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return cap != null && cap.canReceive();
        *///?}
    }

    /**
     * Рендер-баундинг бокс по умолчанию.
     * На Forge это {@code @Override} метода из BlockEntity, на Fabric — обычный public метод.
     * Подклассы могут переопределить для мультиблоков с увеличенным размером.
     */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.5D);
    }

    // ═══════════════════════════ Network initialization ════════════════════════════════

    /**
     * Поддержка подписки на энергосеть (energymk2). Вызывается каждый серверный тик:
     * либо из тика самой машины, либо из централизованного драйвера
     * {@link com.hbm_m.api.energy.EnergySubscriptions#tickAll}.
     * Машина периодически подписывается как receiver/provider к сетям соседних проводников.
     */
    public void ensureNetworkInitialized() {
        if (level != null && !level.isClientSide) {
            com.hbm_m.api.energy.EnergySubscriptions.update(this, getExtraEnergyPorts());
        }
    }

    /**
     * Дополнительные позиции портов подписки для мультиблоков.
     * У каждой такой позиции проверяются все 6 граней на предмет проводов.
     */
    protected net.minecraft.core.BlockPos[] getExtraEnergyPorts() {
        return new net.minecraft.core.BlockPos[0];
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Снимаем подписки при удалении, включая порты частей мультиблока
        // (сети сами пересоберутся через recentlyChanged)
        if (this.level != null && !this.level.isClientSide) {
            com.hbm_m.api.energy.EnergySubscriptions.unsubscribeAll(this, getExtraEnergyPorts());
        }
    }



    // И при загрузке/установке блока:
    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
        // Регистрируемся в централизованном драйвере подписок энергосети
        // (setLevel вызывается vanilla и при загрузке чанка, и при установке блока)
        if (pLevel != null && !pLevel.isClientSide) {
            com.hbm_m.api.energy.EnergySubscriptions.register(this);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════════
    //  Polymorphic Capability Providers (для NeoForge автоматической регистрации)
    // ════════════════════════════════════════════════════════════════════════════════════════════

    @Override
    public @Nullable Object getItemHandler(@Nullable net.minecraft.core.Direction side) {
        return getAutomationItemHandler();
    }

    @Override
    public @Nullable Object getEnergyStorage(@Nullable net.minecraft.core.Direction side) {
        if (!canConnectEnergy(side)) return null;
        //? if neoforge {
        /*return new com.hbm_m.api.energy.LongEnergyWrapper(this,
                side == Direction.DOWN
                        ? com.hbm_m.api.energy.LongEnergyWrapper.BitMode.HIGH
                        : com.hbm_m.api.energy.LongEnergyWrapper.BitMode.LOW);
        *///?} else {
        return null;
        //?}
    }

    @Override
    public @Nullable Object getFluidHandler(@Nullable net.minecraft.core.Direction side) {
        // Если машина реализует IFluidUserMK2 — super вернёт NeoForgeFluidHandlerMK2
        Object superHandler = super.getFluidHandler(side);
        if (superHandler != null) return superHandler;

        // Если у машины есть локальный бак через setFluidHandler
        //? if neoforge {
        /*if (this.fluidHandlerNeo instanceof net.neoforged.neoforge.fluids.capability.IFluidHandler neoHandler) {
            return neoHandler;
        }
        *///?}
        return null;
    }
}