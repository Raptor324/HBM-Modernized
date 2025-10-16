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

/**
 * Абстрактный базовый класс для всех машин в моде.
 * Предоставляет общую логику для инвентаря, энергии, жидкостей и синхронизации.
 */
public abstract class BaseMachineBlockEntity extends LoadedMachineBlockEntity implements MenuProvider {
    
    protected final ItemStackHandler inventory;
    protected Component customName;
    
    // Для отслеживания изменения энергии
    protected int energyDelta = 0;
    protected int previousEnergy = 0;
    
    // Lazy capabilities
    protected LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();
    protected LazyOptional<IEnergyStorage> energyHandler = LazyOptional.empty();
    protected LazyOptional<IFluidHandler> fluidHandler = LazyOptional.empty();
    
    public BaseMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.inventory = createInventoryHandler(slotCount);
    }
    
    /**
     * Создает ItemStackHandler с автоматическим вызовом setChanged()
     */
    protected ItemStackHandler createInventoryHandler(int slotCount) {
        return new ItemStackHandler(slotCount) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                sendUpdateToClient();
            }
            
            @Override
            public boolean isItemValid(int slot, @NotNull net.minecraft.world.item.ItemStack stack) {
                return BaseMachineBlockEntity.this.isItemValidForSlot(slot, stack);
            }
        };
    }
    
    /**
     * Переопределите в дочерних классах для валидации предметов в слотах
     */
    protected boolean isItemValidForSlot(int slot, net.minecraft.world.item.ItemStack stack) {
        return true;
    }
    
    /**
     * Получить ItemStackHandler для работы с инвентарем
     */
    public ItemStackHandler getInventory() {
        return inventory;
    }
    
    /**
     * Установить кастомное имя машины
     */
    public void setCustomName(Component name) {
        this.customName = name;
        setChanged();
    }
    
    /**
     * Получить отображаемое имя машины
     */
    @Override
    public Component getDisplayName() {
        return customName != null ? customName : getDefaultName();
    }
    
    /**
     * Получить кастомное имя (может быть null)
     */
    public Component getCustomName() {
        return customName;
    }

    /**
     * Проверка наличия кастомного имени
     */
    public boolean hasCustomName() {
        return customName != null;
    }
    
    /**
     * Переопределите в дочерних классах для указания дефолтного имени
     */
    protected abstract Component getDefaultName();
    
    /**
     * Проверка доступа игрока к машине
     */
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, 
                                     worldPosition.getY() + 0.5, 
                                     worldPosition.getZ() + 0.5) <= 64.0;
    }
    
    /**
     * Расчет заполненности индикатора для GUI (аналог getGaugeScaled)
     */
    public int getScaledProgress(int pixels, int current, int max) {
        if (max == 0) return 0;
        return current * pixels / max;
    }
    
    /**
     * Расчет заполненности для FluidStack
     */
    public int getFluidScaled(int pixels, FluidStack fluid, int capacity) {
        if (capacity == 0) return 0;
        return fluid.getAmount() * pixels / capacity;
    }
    
    /**
     * Расчет дельты энергии (для анимации потребления/генерации)
     */
    protected void updateEnergyDelta(int currentEnergy) {
        energyDelta = currentEnergy - previousEnergy;
        previousEnergy = currentEnergy;
    }
    
    public int getEnergyDelta() {
        return energyDelta;
    }
    
    /**
     * Обновление редстоун-соединений с соседними блоками
     */
    protected void updateNeighborRedstone(BlockPos neighborPos) {
        if (level != null && !level.isClientSide) {
            BlockState neighborState = level.getBlockState(neighborPos);
            level.neighborChanged(neighborPos, getBlockState().getBlock(), worldPosition);
            level.updateNeighborsAt(neighborPos, neighborState.getBlock());
        }
    }
    
    /**
     * Обновление всех соседних редстоун-соединений
     */
    protected void updateAllNeighborRedstone() {
        if (level != null && !level.isClientSide) {
            for (Direction direction : Direction.values()) {
                updateNeighborRedstone(worldPosition.relative(direction));
            }
        }
    }

    /**
     * Возвращает список призрачных предметов для отображения в GUI.
     * Переопределите в дочерних классах для специфической логики.
     */
    public NonNullList<ItemStack> getGhostItems() {
        return NonNullList.create();
    }

    /**
     * Вспомогательный СТАТИЧЕСКИЙ метод для создания списка призрачных предметов из ингредиентов рецепта
     * С ГРУППИРОВКОЙ одинаковых ингредиентов и подсчетом количества
     */
    public static NonNullList<ItemStack> createGhostItemsFromIngredients(NonNullList<Ingredient> ingredients) {
        NonNullList<ItemStack> ghostItems = NonNullList.create();
        
        // ГРУППИРОВКА одинаковых ингредиентов
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            
            ItemStack[] stacks = ingredient.getItems();
            if (stacks.length == 0) {
                ghostItems.add(ItemStack.EMPTY);
                continue;
            }
            
            // Проверяем, есть ли уже такой ингредиент в ghostItems
            ItemStack firstItem = stacks[0].copy();
            boolean found = false;
            
            for (ItemStack existingGhost : ghostItems) {
                if (!existingGhost.isEmpty() && ItemStack.isSameItemSameTags(existingGhost, firstItem)) {
                    // Увеличиваем количество существующего стака
                    existingGhost.grow(1);
                    found = true;
                    break;
                }
            }
            
            // Если не нашли, добавляем новый предмет с количеством 1
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
    
    /**
     * Переопределите в дочерних классах для настройки энергетической capability
     */
    protected void setupEnergyCapability() {
        // Переопределить в дочерних классах, если требуется энергия
    }
    
    /**
     * Переопределите в дочерних классах для настройки fluid capability
     */
    protected void setupFluidCapability() {
        // Переопределить в дочерних классах, если требуются жидкости
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        energyHandler.invalidate();
        fluidHandler.invalidate();
    }
}
