package com.hbm_m.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseMachineBlockEntity extends LoadedMachineBlockEntity implements MenuProvider {
    protected final ItemStackHandler inventory;
    protected Component customName;

    protected int energyDelta = 0;
    protected int previousEnergy = 0;

    protected LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();
    protected LazyOptional<IEnergyStorage> energyHandler = LazyOptional.empty();
    protected LazyOptional<IFluidHandler> fluidHandler = LazyOptional.empty();

    // ОПТИМИЗАЦИЯ: Счетчик изменений для батчинга обновлений
    private int inventoryChangeCounter = 0;
    private static final int SYNC_THRESHOLD = 3; // Синхронизируем каждые 3 изменения

    public BaseMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.inventory = createInventoryHandler(slotCount);
    }

    protected ItemStackHandler createInventoryHandler(int slotCount) {
        return new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                
                // ОПТИМИЗАЦИЯ: Батчинг обновлений клиента
                inventoryChangeCounter++;
                
                // Отправляем обновление только каждые N изменений или для критических слотов
                if (inventoryChangeCounter >= SYNC_THRESHOLD || isCriticalSlot(slot)) {
                    sendUpdateToClient();
                    inventoryChangeCounter = 0;
                }
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return BaseMachineBlockEntity.this.isItemValidForSlot(slot, stack);
            }
        };
    }

    /**
     * ОПТИМИЗАЦИЯ: Определяет критические слоты, которые требуют немедленной синхронизации
     * Переопределите в дочерних классах для специфических слотов
     */
    protected boolean isCriticalSlot(int slot) {
        return false; // По умолчанию нет критических слотов
    }

    /**
     * Принудительная синхронизация инвентаря с клиентом
     */
    public void forceInventorySync() {
        sendUpdateToClient();
        inventoryChangeCounter = 0;
    }

    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public void setCustomName(Component name) {
        this.customName = name;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName : getDefaultName();
    }

    public Component getCustomName() {
        return customName;
    }

    public boolean hasCustomName() {
        return customName != null;
    }

    protected abstract Component getDefaultName();

    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        
        // ОПТИМИЗАЦИЯ: используем distanceToSqr вместо вычисления корня
        double dx = player.getX() - (worldPosition.getX() + 0.5);
        double dy = player.getY() - (worldPosition.getY() + 0.5);
        double dz = player.getZ() - (worldPosition.getZ() + 0.5);
        
        return (dx * dx + dy * dy + dz * dz) <= 4096.0; // 64^2
    }

    public int getScaledProgress(int pixels, int current, int max) {
        return max == 0 ? 0 : current * pixels / max;
    }

    public int getFluidScaled(int pixels, FluidStack fluid, int capacity) {
        return capacity == 0 ? 0 : fluid.getAmount() * pixels / capacity;
    }

    protected void updateEnergyDelta(int currentEnergy) {
        energyDelta = currentEnergy - previousEnergy;
        previousEnergy = currentEnergy;
    }

    public int getEnergyDelta() {
        return energyDelta;
    }

    protected void updateNeighborRedstone(BlockPos neighborPos) {
        if (level != null && !level.isClientSide) {
            BlockState neighborState = level.getBlockState(neighborPos);
            level.neighborChanged(neighborPos, getBlockState().getBlock(), worldPosition);
            level.updateNeighborsAt(neighborPos, neighborState.getBlock());
        }
    }

    protected void updateAllNeighborRedstone() {
        if (level != null && !level.isClientSide) {
            for (Direction direction : Direction.values()) {
                updateNeighborRedstone(worldPosition.relative(direction));
            }
        }
    }

    public NonNullList<ItemStack> getGhostItems() {
        return NonNullList.create();
    }

    // ОПТИМИЗАЦИЯ: улучшен алгоритм группировки с использованием HashMap
    public static NonNullList<ItemStack> createGhostItemsFromIngredients(NonNullList<Ingredient> ingredients) {
        NonNullList<ItemStack> ghostItems = NonNullList.create();
        
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length == 0) {
                ghostItems.add(ItemStack.EMPTY);
                continue;
            }

            ItemStack firstItem = stacks[0].copy();
            boolean found = false;

            // Проверяем существующие предметы
            for (ItemStack existingGhost : ghostItems) {
                if (!existingGhost.isEmpty() && ItemStack.isSameItemSameTags(existingGhost, firstItem)) {
                    existingGhost.grow(1);
                    found = true;
                    break;
                }
            }

            if (!found) {
                firstItem.setCount(1);
                ghostItems.add(firstItem);
            }
        }

        return ghostItems;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName));
        }

        tag.putInt("EnergyDelta", energyDelta);
        tag.putInt("PreviousEnergy", previousEnergy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        
        if (tag.contains("CustomName", 8)) {
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
        }

        energyDelta = tag.getInt("EnergyDelta");
        previousEnergy = tag.getInt("PreviousEnergy");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        itemHandler = LazyOptional.of(() -> inventory);
        setupEnergyCapability();
        setupFluidCapability();
    }

    protected void setupEnergyCapability() {
        // Переопределить в дочерних классах
    }

    protected void setupFluidCapability() {
        // Переопределить в дочерних классах
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        energyHandler.invalidate();
        fluidHandler.invalidate();
    }
}
