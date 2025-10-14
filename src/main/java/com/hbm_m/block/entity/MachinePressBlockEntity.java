package com.hbm_m.block.entity;

import com.hbm_m.recipe.PressRecipe;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.menu.MachinePressMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
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

public class MachinePressBlockEntity extends BlockEntity implements MenuProvider {

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

    // Константы как в 1.7.10
    private static final int MAX_SPEED = 400;
    private static final int PROGRESS_AT_MAX = 25;
    private static final int MAX_PRESS = 200;
    private static final int FUEL_PER_OPERATION = 200;

    // Переменные состояния
    private int speed = 0; // 0-400, как в 1.7.10
    private int burnTime = 0; // накопленное топливо
    private int press = 0; // прогресс пресса 0-200
    private boolean isRetracting = false;
    private int delay = 0; // задержка между сменами направления

    // Для рендера и синхронизации
    private int heatState = 0;
    private int pressPosition = 0;

    protected final ContainerData data;

    public MachinePressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESS_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> MachinePressBlockEntity.this.press;
                    case 1 -> MAX_PRESS;
                    case 2 -> MachinePressBlockEntity.this.burnTime;
                    case 3 -> FUEL_PER_OPERATION;
                    case 4 -> MachinePressBlockEntity.this.speed;
                    case 5 -> MAX_SPEED;
                    case 6 -> MachinePressBlockEntity.this.heatState;
                    case 7 -> MachinePressBlockEntity.this.pressPosition;
                    case 8 -> MachinePressBlockEntity.this.isRetracting ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> MachinePressBlockEntity.this.press = value;
                    case 2 -> MachinePressBlockEntity.this.burnTime = value;
                    case 4 -> MachinePressBlockEntity.this.speed = value;
                    case 6 -> MachinePressBlockEntity.this.heatState = value;
                    case 7 -> MachinePressBlockEntity.this.pressPosition = value;
                    case 8 -> MachinePressBlockEntity.this.isRetracting = value == 1;
                }
            }

            @Override
            public int getCount() {
                return 9;
            }
        };
    }

    public int getHeatState() {
        return heatState;
    }

    public boolean isHeated() {
        return burnTime >= FUEL_PER_OPERATION;
    }

    public boolean isCrafting() {
        return press > 0 || isRetracting;
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

    public int getBurnTime(Item item) {
        ItemStack stack = new ItemStack(item);
        // Получаем ванильное время горения в тиках
        int vanillaBurnTime = net.minecraftforge.common.ForgeHooks.getBurnTime(stack, null);

        if (vanillaBurnTime <= 0) return 0;
        // Конвертируем тики в секунды (делим на 20)
        return (vanillaBurnTime / 20);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm_m.press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachinePressMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("press", press);
        tag.putInt("burnTime", burnTime);
        tag.putInt("speed", speed);
        tag.putBoolean("isRetracting", isRetracting);
        tag.putInt("delay", delay);
        tag.putInt("heatState", heatState);
        tag.putInt("pressPosition", pressPosition);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        press = tag.getInt("press");
        burnTime = tag.getInt("burnTime");
        speed = tag.getInt("speed");
        isRetracting = tag.getBoolean("isRetracting");
        delay = tag.getInt("delay");
        heatState = tag.getInt("heatState");
        pressPosition = tag.getInt("pressPosition");
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if(level.isClientSide) return;

        boolean needsSync = false;

        // Добавление топлива (как у генератора)
        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        if(!fuelStack.isEmpty() && burnTime < FUEL_PER_OPERATION) {
            int burnTime = getBurnTime(fuelStack.getItem());
            if(burnTime > 0) {
                this.burnTime += burnTime * 20; // Конвертируем секунды обратно в тики для хранения
                fuelStack.shrink(1);
                needsSync = true;
            }
        }

        boolean canProcess = canProcess();
        boolean preheated = false; // TODO: проверка на press_preheater если нужно

        // Логика ускорения/замедления (как в 1.7.10)
        if((canProcess || isRetracting) && burnTime >= FUEL_PER_OPERATION) {
            speed += preheated ? 4 : 1;
            if(speed > MAX_SPEED) {
                speed = MAX_SPEED;
            }
        } else {
            speed -= 1;
            if(speed < 0) {
                speed = 0;
            }
        }

        // Обновляем heatState для визуализации (0-12)
        heatState = Math.min(12, speed / 33);

        // Логика работы пресса (как в 1.7.10)
        if(delay <= 0) {
            int stampSpeed = speed * PROGRESS_AT_MAX / MAX_SPEED;

            if(isRetracting) {
                press -= stampSpeed;
                if(press <= 0) {
                    press = 0;
                    isRetracting = false;
                    delay = 5;
                }
            } else if(canProcess) {
                press += stampSpeed;
                if(press >= MAX_PRESS) {
                    // Завершение операции
                    craftItem();

                    isRetracting = true;
                    delay = 5;

                    // ВАЖНО: вычитаем топливо только при успешной операции
                    if(burnTime >= FUEL_PER_OPERATION) {
                        burnTime -= FUEL_PER_OPERATION;
                    }

                    needsSync = true;
                }
            } else if(press > 0) {
                isRetracting = true;
            }
        } else {
            delay--;
        }

        // Обновляем позицию для рендера
        pressPosition = Math.min(20, (press * 20) / MAX_PRESS);

        if(needsSync) {
            setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private void craftItem() {
        Optional<PressRecipe> recipe = getCurrentRecipe();
        if (recipe.isPresent()) {
            ItemStack output = recipe.get().getResultItem(getLevel().registryAccess());

            // Вычитаем материал
            itemHandler.extractItem(MATERIAL_SLOT, 1, false);

            // Добавляем результат
            ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if(outputSlot.isEmpty()) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, output.copy());
            } else {
                outputSlot.grow(output.getCount());
            }

            // Изнашиваем штамп (если он изнашиваемый)
            ItemStack stamp = itemHandler.getStackInSlot(STAMP_SLOT);

            // Проверяем, что штамп не бесконечный (Desh штампы)
            if(stamp.isDamageableItem()) {
                stamp.setDamageValue(stamp.getDamageValue() + 1);

                // Если штамп сломался - удаляем его
                if(stamp.getDamageValue() >= stamp.getMaxDamage()) {
                    itemHandler.setStackInSlot(STAMP_SLOT, ItemStack.EMPTY);

                    // Можно добавить звук ломающегося предмета
                    level.playSound(null, worldPosition,
                            net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                            SoundSource.BLOCKS, 1.5f, 0.8f);
                }
            }

            // Воспроизведение звука работы пресса
            level.playSound(null, worldPosition, ModSounds.PRESS_OPERATE.get(),
                    SoundSource.BLOCKS, 1.5f, 1.0f);
        }
    }

    private boolean canProcess() {
        if(burnTime < FUEL_PER_OPERATION) return false;
        if(itemHandler.getStackInSlot(STAMP_SLOT).isEmpty() ||
                itemHandler.getStackInSlot(MATERIAL_SLOT).isEmpty()) return false;

        Optional<PressRecipe> recipe = getCurrentRecipe();
        if(recipe.isEmpty()) return false;

        ItemStack result = recipe.get().getResultItem(getLevel().registryAccess());
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if(outputSlot.isEmpty()) return true;

        return outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize() &&
                outputSlot.is(result.getItem()) &&
                outputSlot.getDamageValue() == result.getDamageValue();
    }

    private Optional<PressRecipe> getCurrentRecipe() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        return level.getRecipeManager().getRecipeFor(PressRecipe.Type.INSTANCE, inventory, level);
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
