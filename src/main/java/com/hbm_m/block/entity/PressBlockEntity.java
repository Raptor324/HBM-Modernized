package com.hbm_m.block.entity;

import com.hbm_m.block.PressBlock;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.recipe.PressRecipe;
import com.hbm_m.menu.PressMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PressBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private static final int FUEL_SLOT = 0;
    private static final int STAMP_SLOT = 1;
    private static final int MATERIAL_SLOT = 2;
    private static final int OUTPUT_SLOT = 3;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // Система нагрева с 13 состояниями (0-12)
    private static final int MAX_HEAT_STATES = 13;
    private static final int WORKING_HEAT_THRESHOLD = 3;

    // Переменные состояния
    private int progress = 0;
    private int maxProgress = 40; // 2 секунды (40 тиков)
    private int fuelTime = 0;
    private int maxFuelTime = 0;
    private int heatLevel = 0; // 0-2400 (увеличено для более медленного нагрева)
    private int maxHeatLevel = 2400;
    private int heatState = 0; // 0-12
    private int pressPosition = 0; // 0-20
    private int pressingDown = 0; // 0 или 1
    private int pressAnimationSpeed = 1;
    private int efficiency = 0;

    // Вспомогательные поля
    private long lastWorkTime = 0;
    private int pressAnimationTimer = 0; // Таймер для анимации пресса

    protected final ContainerData data;

    public PressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESS_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> PressBlockEntity.this.progress;
                    case 1 -> PressBlockEntity.this.maxProgress;
                    case 2 -> PressBlockEntity.this.fuelTime;
                    case 3 -> PressBlockEntity.this.maxFuelTime;
                    case 4 -> PressBlockEntity.this.heatLevel;
                    case 5 -> PressBlockEntity.this.maxHeatLevel;
                    case 6 -> PressBlockEntity.this.heatState;
                    case 7 -> PressBlockEntity.this.pressPosition;
                    case 8 -> PressBlockEntity.this.pressingDown;
                    case 9 -> PressBlockEntity.this.efficiency;
                    case 10 -> PressBlockEntity.this.pressAnimationSpeed;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> PressBlockEntity.this.progress = value;
                    case 1 -> PressBlockEntity.this.maxProgress = value;
                    case 2 -> PressBlockEntity.this.fuelTime = value;
                    case 3 -> PressBlockEntity.this.maxFuelTime = value;
                    case 4 -> PressBlockEntity.this.heatLevel = value;
                    case 5 -> PressBlockEntity.this.maxHeatLevel = value;
                    case 6 -> PressBlockEntity.this.heatState = value;
                    case 7 -> PressBlockEntity.this.pressPosition = value;
                    case 8 -> PressBlockEntity.this.pressingDown = value;
                    case 9 -> PressBlockEntity.this.efficiency = value;
                    case 10 -> PressBlockEntity.this.pressAnimationSpeed = value;
                }
            }

            @Override
            public int getCount() {
                return 11;
            }
        };
    }

    // Геттеры для удобства
    public int getHeatState() {
        return heatState;
    }

    public boolean isHeated() {
        return heatState >= WORKING_HEAT_THRESHOLD;
    }

    public boolean isCrafting() {
        return progress > 0 && hasValidIngredients() && isHeated();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
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
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm_m.press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PressMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("press.progress", progress);
        tag.putInt("press.max_progress", maxProgress);
        tag.putInt("press.fuel_time", fuelTime);
        tag.putInt("press.max_fuel_time", maxFuelTime);
        tag.putInt("press.heat_level", heatLevel);
        tag.putInt("press.max_heat_level", maxHeatLevel);
        tag.putInt("press.heat_state", heatState);
        tag.putInt("press.press_position", pressPosition);
        tag.putInt("press.pressing_down", pressingDown);
        tag.putInt("press.efficiency", efficiency);
        tag.putInt("press.animation_speed", pressAnimationSpeed);
        tag.putLong("press.last_work_time", lastWorkTime);
        tag.putInt("press.animation_timer", pressAnimationTimer);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("press.progress");
        maxProgress = tag.getInt("press.max_progress");
        fuelTime = tag.getInt("press.fuel_time");
        maxFuelTime = tag.getInt("press.max_fuel_time");
        heatLevel = tag.getInt("press.heat_level");
        maxHeatLevel = tag.getInt("press.max_heat_level");
        heatState = tag.getInt("press.heat_state");
        pressPosition = tag.getInt("press.press_position");
        pressingDown = tag.getInt("press.pressing_down");
        efficiency = tag.getInt("press.efficiency");
        pressAnimationSpeed = tag.getInt("press.animation_speed");
        lastWorkTime = tag.getLong("press.last_work_time");
        pressAnimationTimer = tag.getInt("press.animation_timer");
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        boolean wasLit = isLit();
        boolean needsSync = false;

        // Добавление топлива
        if (hasValidIngredients() && canAddFuel()) {
            addFuel();
            needsSync = true;
        }

        boolean hasValidIngredients = hasValidIngredients();
        boolean hasRecipe = hasRecipe();

        // Нагрев - МЕДЛЕННЕЕ!
        if (hasValidIngredients && isBurning()) {
            if (heatLevel < maxHeatLevel) {
                int heatGain = calculateHeatGain();
                heatLevel = Math.min(maxHeatLevel, heatLevel + heatGain);

                // Обновляем heatState (теперь каждые 200 единиц вместо 100)
                int newHeatState = Math.min(MAX_HEAT_STATES - 1, heatLevel / 200);
                if (this.heatState != newHeatState) {
                    this.heatState = newHeatState;
                    needsSync = true;
                }
            }

            // Работа - ТЕПЕРЬ РАБОТАЕТ ДАЖЕ ПРИ ОХЛАЖДЕНИИ, ЕСЛИ ЕСТЬ ДОСТАТОЧНО ТЕПЛА
            if (isHeated() && hasRecipe) {
                updateWorkingState();
                updatePressAnimation();
                updateEfficiency();

                progress++;
                needsSync = true;

                if (progress >= maxProgress) {
                    craftItem();
                    progress = 0;
                    // Сбрасываем анимацию пресса после создания предмета
                    pressAnimationTimer = 0;
                    pressingDown = 0;
                    pressPosition = 0;
                    needsSync = true;
                }
            }
        } else {
            // Остывание - МЕДЛЕННЕЕ!
            if (heatLevel > 0) {
                int heatLoss = calculateHeatLoss();
                heatLevel = Math.max(0, heatLevel - heatLoss);

                // Обновляем heatState
                int newHeatState = Math.min(MAX_HEAT_STATES - 1, heatLevel / 200);
                if (this.heatState != newHeatState) {
                    this.heatState = newHeatState;
                    needsSync = true;
                }
            }

            // ВАЖНО: если есть достаточно тепла, продолжаем работать даже при остывании!
            if (isHeated() && hasRecipe && hasValidIngredients) {
                updateWorkingState();
                updatePressAnimation();
                updateEfficiency();

                progress++;
                needsSync = true;

                if (progress >= maxProgress) {
                    craftItem();
                    progress = 0;
                    pressAnimationTimer = 0;
                    pressingDown = 0;
                    pressPosition = 0;
                    needsSync = true;
                }
            } else {
                // Останавливаем работу только если нет достаточного тепла
                if (efficiency > 0) {
                    efficiency = Math.max(0, efficiency - 2);
                    needsSync = true;
                }

                if (progress > 0 && !isHeated()) {
                    progress = 0;
                    pressAnimationTimer = 0;
                    pressingDown = 0;
                    pressPosition = 0;
                    needsSync = true;
                }
            }
        }


        // Расходование топлива
        if (fuelTime > 0) {
            fuelTime--;
            needsSync = true;
        }

        if (needsSync) {
            setChanged();
        }
    }

    private int calculateHeatGain() {
        // Медленнее нагрев: 1-2 единицы вместо 2-1
        return heatState > 8 ? 1 : 2;
    }

    private int calculateHeatLoss() {
        // Медленнее остывание: 1-2 единицы вместо 3-4
        return heatState > 8 ? 2 : 1;
    }

    private void updateWorkingState() {
        // Новая система скорости: от 2 секунд (40 тиков) до 0.6 секунды (12 тиков)
        if (heatState <= 3) {
            maxProgress = 40; // 2 секунды
        } else if (heatState <= 6) {
            maxProgress = 30; // 1.5 секунды
        } else if (heatState <= 9) {
            maxProgress = 20; // 1 секунда
        } else {
            maxProgress = 12; // 0.6 секунды (максимальная скорость)
        }
    }

    private void updatePressAnimation() {
        pressAnimationTimer++;

        // Новая логика анимации: делим время пополам
        int halfProgress = maxProgress / 2;

        if (pressAnimationTimer <= halfProgress) {
            // Первая половина: стрелка идет вверх (или остается наверху после создания предмета)
            pressingDown = 0;
            pressPosition = Math.max(0, 20 - (pressAnimationTimer * 20 / halfProgress));
        } else {
            // Вторая половина: стрелка идет вниз
            pressingDown = 1;
            int secondHalfTimer = pressAnimationTimer - halfProgress;
            pressPosition = Math.min(20, (secondHalfTimer * 20 / halfProgress));
        }

        // Сбрасываем таймер если достигли максимума
        if (pressAnimationTimer >= maxProgress) {
            pressAnimationTimer = 0;
        }
    }

    private void updateEfficiency() {
        if (efficiency < 100) {
            efficiency = Math.min(100, efficiency + 1);
        }
    }

    private boolean isLit() {
        return fuelTime > 0 && heatLevel > 0;
    }

    private boolean isBurning() {
        return fuelTime > 0;
    }

    private boolean canAddFuel() {
        ItemStack fuelStack = this.itemHandler.getStackInSlot(FUEL_SLOT);
        return fuelStack.getItem() == Items.COAL && fuelStack.getCount() > 0 && this.fuelTime <= 0;
    }

    private void addFuel() {
        ItemStack fuelStack = this.itemHandler.getStackInSlot(FUEL_SLOT);
        if (fuelStack.getItem() == Items.COAL) {
            this.fuelTime = 1600;
            this.maxFuelTime = this.fuelTime;
            fuelStack.shrink(1);
        }
    }

    private void craftItem() {
        Optional<PressRecipe> recipe = getCurrentRecipe();
        if (recipe.isPresent()) {
            ItemStack output = recipe.get().getResultItem(getLevel().registryAccess());

            this.itemHandler.extractItem(MATERIAL_SLOT, 1, false);
            // Штамп НЕ расходуется в процессе работы

            this.itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(output.getItem(),
                    this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + output.getCount()));
        }
    }

    private boolean hasValidIngredients() {
        return !itemHandler.getStackInSlot(STAMP_SLOT).isEmpty() &&
                !itemHandler.getStackInSlot(MATERIAL_SLOT).isEmpty();
    }

    private boolean hasRecipe() {
        Optional<PressRecipe> recipe = getCurrentRecipe();

        if(recipe.isEmpty()) {
            return false;
        }
        ItemStack result = recipe.get().getResultItem(getLevel().registryAccess());

        return canInsertAmountIntoOutputSlot(result.getCount()) &&
                canInsertItemIntoOutputSlot(result.getItem());
    }

    private Optional<PressRecipe> getCurrentRecipe() {
        SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, this.itemHandler.getStackInSlot(i));
        }

        return this.level.getRecipeManager().getRecipeFor(PressRecipe.Type.INSTANCE, inventory, level);
    }

    private boolean canInsertItemIntoOutputSlot(net.minecraft.world.item.Item item) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ||
                this.itemHandler.getStackInSlot(OUTPUT_SLOT).is(item);
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count <=
                this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    public ContainerData getBlockEntityData() {
        return this.data;
    }
}