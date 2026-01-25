package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.custom.machines.MachineWoodBurnerBlock;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.item.ModItems;
import com.hbm_m.menu.MachineWoodBurnerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import javax.annotation.Nullable;

/**
 * Дровяной генератор энергии.
 * Сжигает топливо и производит энергию.
 */
public class MachineWoodBurnerBlockEntity extends BaseMachineBlockEntity {

    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;
    private static final int CHARGE_SLOT = 2;
    private static final int INVENTORY_SIZE = 3;
    
    private static final long CAPACITY = 100_000L;
    private static final long GENERATION_RATE = 50L; // HE за тик
    private static final long MAX_EXTRACT = GENERATION_RATE * 2; // Можем отдавать в 2 раза больше чем генерируем

    // GUI данные
    protected final ContainerData data;

    // Состояние горения
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean enabled = true;

    public MachineWoodBurnerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.WOOD_BURNER_BE.get(), pPos, pBlockState, 
              INVENTORY_SIZE, CAPACITY, 0L, MAX_EXTRACT); // Не принимает энергию, но может отдавать

        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> burnTime;
                    case 1 -> maxBurnTime;
                    case 2 -> isBurning() ? 1 : 0;
                    case 3 -> enabled ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int v) {
                if (i == 3) enabled = v != 0;
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineWoodBurnerBlockEntity be) {
        if (level.isClientSide()) return;

        // Инициализация сети через базовый класс
        be.ensureNetworkInitialized();

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
            be.setEnergyStored(Math.min(be.getMaxEnergyStored(), be.getEnergyStored() + GENERATION_RATE));

            be.chargeItem();

            // Топливо закончилось
            if (be.burnTime == 0) {
                be.maxBurnTime = 0;

                // 50% шанс получить пепел
                if (level.random.nextFloat() < 0.5f) {
                    ItemStack ashStack = be.inventory.getStackInSlot(ASH_SLOT);
                    if (ashStack.isEmpty()) {
                        be.inventory.setStackInSlot(ASH_SLOT, new ItemStack(ModItems.WOOD_ASH_POWDER.get()));
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

        be.setChanged();
    }

    private boolean canBurn() {
        return !this.inventory.getStackInSlot(FUEL_SLOT).isEmpty() && this.energy < this.capacity;
    }

    private void startBurning() {
        ItemStack fuelStack = this.inventory.getStackInSlot(FUEL_SLOT);
        int burnTicks = ForgeHooks.getBurnTime(fuelStack, null);

        if (burnTicks > 0) {
            this.maxBurnTime = burnTicks;
            this.burnTime = burnTicks;

            // Особая обработка для лава ведра
            if (fuelStack.getItem() == Items.LAVA_BUCKET) {
                this.inventory.setStackInSlot(FUEL_SLOT, new ItemStack(Items.BUCKET));
            } else {
                fuelStack.shrink(1);
            }
        }
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    // Переопределяем скорость отдачи энергии
    @Override
    public long getProvideSpeed() {
        return MAX_EXTRACT; // Можем отдавать в 2 раза больше чем генерируем
    }

    // --- NBT ---
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); // Сохраняет inventory и energy из базового класса
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("enabled", enabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag); // Загружает inventory и energy из базового класса
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
            SimpleContainer simpleContainer = new SimpleContainer(inventory.getSlots());
            for (int i = 0; i < inventory.getSlots(); i++) {
                simpleContainer.setItem(i, inventory.getStackInSlot(i));
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



    private void chargeItem() {
        if (this.energy <= 0) return; // Нечего заряжать

        ItemStack itemToCharge = this.inventory.getStackInSlot(CHARGE_SLOT);
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

    // --- Реализация абстрактных методов ---
    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.wood_burner");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
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
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent() ||
                   stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).isPresent();
        }
        return false;
    }
}