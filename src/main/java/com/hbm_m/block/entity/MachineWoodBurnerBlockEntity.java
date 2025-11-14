package com.hbm_m.block.entity;

import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.api.energy.IEnergyConnector;
import com.hbm_m.block.MachineWoodBurnerBlock;
import com.hbm_m.api.energy.IEnergyProvider;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.api.energy.PackedEnergyCapabilityProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.menu.MachineWoodBurnerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

/**
 * Дровяной генератор энергии.
 * Сжигает топливо и производит энергию.
 */
public class MachineWoodBurnerBlockEntity extends BlockEntity implements MenuProvider, IEnergyProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == FUEL_SLOT) {
                // Только топливо в слот топлива
                return ForgeHooks.getBurnTime(stack, null) > 0;
            }
            if (slot == ASH_SLOT) {
                // Ничего нельзя положить в слот пепла
                return false;
            }
            if (slot == CHARGE_SLOT) {
                // Только заряжаемые предметы в слот зарядки
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
            }
            return false;
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // Энергия
    private long energy = 0;
    private final long capacity = 100_000L;
    private final long generationRate = 50L; // HE за тик

    // Capabilities
    private final LazyOptional<IEnergyProvider> hbmProvider = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyConnector> hbmConnector = LazyOptional.of(() -> this);
    private final PackedEnergyCapabilityProvider feCapabilityProvider;

    // GUI данные
    protected final ContainerData data;

    // Состояние горения
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean enabled = true;

    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;
    private static final int CHARGE_SLOT = 2;

    public MachineWoodBurnerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.WOOD_BURNER_BE.get(), pPos, pBlockState);
        this.feCapabilityProvider = new PackedEnergyCapabilityProvider(this);

        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> (int) (energy & 0xFFFFFFFFL);      // energy low
                    case 1 -> (int) (energy >> 32);              // energy high
                    case 2 -> (int) (capacity & 0xFFFFFFFFL);    // capacity low
                    case 3 -> (int) (capacity >> 32);            // capacity high
                    case 4 -> burnTime;
                    case 5 -> maxBurnTime;
                    case 6 -> isBurning() ? 1 : 0;
                    case 7 -> enabled ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int v) {
                if (i == 7) enabled = v != 0;
            }

            @Override
            public int getCount() {
                return 8;
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineWoodBurnerBlockEntity be) {
        if (level.isClientSide()) return;

        boolean wasBurning = be.isBurning();
        boolean canCurrentlyBurn = be.canBurn();

        // Если можем гореть и не горим - начинаем
        if (be.enabled && be.burnTime <= 0 && canCurrentlyBurn) {
            be.startBurning();
        }

        // Процесс горения
        if (be.isBurning() && be.enabled) {
            be.burnTime--;

            // Просто генерируем энергию. Сеть сама её заберёт.
            be.energy = Math.min(be.capacity, be.energy + be.generationRate);

            be.chargeItem();

            // Топливо закончилось
            if (be.burnTime == 0) {
                be.maxBurnTime = 0;

                // 50% шанс получить пепел
                if (level.random.nextFloat() < 0.5f) {
                    ItemStack ashStack = be.itemHandler.getStackInSlot(ASH_SLOT);
                    if (ashStack.isEmpty()) {
                        be.itemHandler.setStackInSlot(ASH_SLOT, new ItemStack(ModItems.WOOD_ASH_POWDER.get()));
                    } else if (ashStack.is(ModItems.WOOD_ASH_POWDER.get()) && ashStack.getCount() < ashStack.getMaxStackSize()) {
                        ashStack.grow(1);
                    }
                }
            }
        } else if (be.isBurning()) {
            // Если выключили во время горения - прекращаем
            be.burnTime = 0;
            be.maxBurnTime = 0;
        }

        // Обновляем визуальное состояние блока
        if (wasBurning != be.isBurning()) {
            level.setBlock(pos, state.setValue(MachineWoodBurnerBlock.LIT, be.isBurning()), 3);
        }

        setChanged(level, pos, state);
    }

    private boolean canBurn() {
        return !this.itemHandler.getStackInSlot(FUEL_SLOT).isEmpty() && this.energy < this.capacity;
    }

    private void startBurning() {
        ItemStack fuelStack = this.itemHandler.getStackInSlot(FUEL_SLOT);
        int burnTicks = ForgeHooks.getBurnTime(fuelStack, null);

        if (burnTicks > 0) {
            this.maxBurnTime = burnTicks;
            this.burnTime = burnTicks;

            // Особая обработка для лава ведра
            if (fuelStack.getItem() == Items.LAVA_BUCKET) {
                this.itemHandler.setStackInSlot(FUEL_SLOT, new ItemStack(Items.BUCKET));
            } else {
                fuelStack.shrink(1);
            }
        }
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    // --- IEnergyProvider базовые методы ---
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
        return this.generationRate * 2; // Можем отдавать в 2 раза больше чем генерируем
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    // --- IEnergyProvider новые методы ---
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;

        long energyExtracted = Math.min(this.energy, Math.min(getProvideSpeed(), maxExtract));
        if (!simulate && energyExtracted > 0) {
            setEnergyStored(this.energy - energyExtracted);
        }
        return energyExtracted;
    }

    @Override
    public boolean canExtract() {
        return this.energy > 0;
    }

    // --- Capabilities ---
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.HBM_ENERGY_PROVIDER) {
            return hbmProvider.cast();
        }

        if (cap == ModCapabilities.HBM_ENERGY_CONNECTOR) {
            return hbmConnector.cast();
        }

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }

        LazyOptional<T> feCap = feCapabilityProvider.getCapability(cap, side);
        if (feCap.isPresent()) return feCap;

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        hbmProvider.invalidate();
        feCapabilityProvider.invalidate();
        hbmConnector.invalidate();
    }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putLong("energy", energy);
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("enabled", enabled);

        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        energy = tag.getLong("energy");
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        enabled = tag.getBoolean("enabled");
    }

    // --- GUI ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.wood_burner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new MachineWoodBurnerMenu(id, inv, this, this.data);
    }

    public void drops() {
        if (level != null) {
            SimpleContainer simpleContainer = new SimpleContainer(itemHandler.getSlots());
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                simpleContainer.setItem(i, itemHandler.getStackInSlot(i));
            }
            Containers.dropContents(this.level, this.worldPosition, simpleContainer);
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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



    private void chargeItem() {
        if (this.energy <= 0) return; // Нечего заряжать

        ItemStack itemToCharge = this.itemHandler.getStackInSlot(CHARGE_SLOT);
        if (itemToCharge.isEmpty()) return;

        // === 1. ПРОВЕРКА HBM API (Твои батарейки) ===
        // Сначала пытаемся зарядить через нашу "родную" HBM-систему (которая использует long)
        LazyOptional<com.hbm_m.api.energy.IEnergyReceiver> hbmEnergy =
                itemToCharge.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER);

        if (hbmEnergy.isPresent()) {
            com.hbm_m.api.energy.IEnergyReceiver itemReceiver = hbmEnergy.resolve().get();

            if (itemReceiver.canReceive()) {
                long maxTransfer = Math.min(this.energy, getProvideSpeed());

                // Наша HBM API работает с long, что идеально
                long accepted = itemReceiver.receiveEnergy(maxTransfer, false);

                if (accepted > 0) {
                    this.setEnergyStored(this.energy - accepted);
                    setChanged();
                }
            }
            // Мы нашли HBM-совместимый предмет, выходим, даже если он полный
            return;
        }

        // === 2. ПРОВЕРКА FORGE API (Для модов) ===
        // Если HBM API не найдено, ищем Forge Energy
        LazyOptional<net.minecraftforge.energy.IEnergyStorage> forgeEnergy =
                itemToCharge.getCapability(ForgeCapabilities.ENERGY);

        if (forgeEnergy.isPresent()) {
            net.minecraftforge.energy.IEnergyStorage itemEnergy = forgeEnergy.resolve().get();

            if (itemEnergy.canReceive()) {
                long maxTransfer = Math.min(this.energy, getProvideSpeed());

                // Конвертируем long в int для Forge API
                int transferInt = (int) Math.min(Integer.MAX_VALUE, maxTransfer);
                if (transferInt <= 0) return;

                int accepted = itemEnergy.receiveEnergy(transferInt, false);

                if (accepted > 0) {
                    this.setEnergyStored(this.energy - accepted);
                    setChanged();
                }
            }
        }
    }
}
