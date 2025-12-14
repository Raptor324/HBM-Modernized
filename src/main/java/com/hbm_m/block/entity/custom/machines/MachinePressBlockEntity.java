package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.recipe.PressRecipe;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.menu.MachinePressMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MachinePressBlockEntity extends BaseMachineBlockEntity {
    
    // Слоты
    private static final int SLOT_COUNT = 4;
    private static final int FUEL_SLOT = 0;
    private static final int STAMP_SLOT = 1;
    private static final int MATERIAL_SLOT = 2;
    private static final int OUTPUT_SLOT = 3;
    
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
    private float visualPressPosition = 0F;
    private float prevVisualPressPosition = 0F;
    private boolean clientPressInitialized = false;
    
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> press;
                case 1 -> MAX_PRESS;
                case 2 -> burnTime;
                case 3 -> FUEL_PER_OPERATION;
                case 4 -> speed;
                case 5 -> MAX_SPEED;
                case 6 -> heatState;
                case 7 -> pressPosition;
                case 8 -> isRetracting ? 1 : 0;
                default -> 0;
            };
        }
        
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> press = value;
                case 2 -> burnTime = value;
                case 4 -> speed = value;
                case 6 -> heatState = value;
                case 7 -> pressPosition = value;
                case 8 -> isRetracting = value == 1;
            }
        }
        
        @Override
        public int getCount() {
            return 9;
        }
    };

    public MachinePressBlockEntity(BlockPos pos, BlockState state) {
        // ИСПРАВЬ: добавь capacity и transferRate
        super(ModBlockEntities.PRESS_BE.get(), pos, state,
                SLOT_COUNT,   // inventorySize
                50_000L,      // capacity (энергия)
                1000L);       // transferRate (скорость приёма/отдачи)
    }


    
    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.press");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }
    
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> getBurnTime(stack.getItem()) > 0;
            case STAMP_SLOT -> true; // Проверка на stamp type может быть добавлена
            case MATERIAL_SLOT -> true;
            case OUTPUT_SLOT -> false; // Выходной слот только для результатов
            default -> false;
        };
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachinePressMenu(containerId, playerInventory, this, this.data);
    }
    
    // ==================== TICK LOGIC ====================
    
    public static void tick(Level level, BlockPos pos, BlockState state, MachinePressBlockEntity entity) {
        if (level.isClientSide) {
            entity.clientTick();
        } else {
            entity.serverTick();
        }
    }

    private void serverTick() {
        if (level == null) {
            return;
        }
    
        boolean needsSync = false;
    
        int previousPress = this.press;
        int previousPressPosition = this.pressPosition;
        int previousBurnTime = this.burnTime;
        int previousSpeed = this.speed;
        boolean previousRetracting = this.isRetracting;

        // Добавление топлива (как у генератора)
        ItemStack fuelStack = inventory.getStackInSlot(FUEL_SLOT);
        if (!fuelStack.isEmpty() && burnTime < FUEL_PER_OPERATION) {
            int fuelValue = getBurnTime(fuelStack.getItem());
            if (fuelValue > 0) {
                burnTime += fuelValue * 20; // Конвертируем секунды в тики
                fuelStack.shrink(1);
                needsSync = true;
            }
        }

        boolean canProcess = canProcess();
        boolean preheated = false; // TODO: проверка на press_preheater если нужно

        // Логика ускорения/замедления (как в 1.7.10)
        if ((canProcess || isRetracting) && burnTime >= FUEL_PER_OPERATION) {
            speed += preheated ? 4 : 1;
            if (speed > MAX_SPEED) {
                speed = MAX_SPEED;
            }
        } else {
            speed -= 1;
            if (speed < 0) {
                speed = 0;
            }
        }

        // Обновляем heatState для визуализации (0-12)
        heatState = Math.min(12, speed / 33);

        // Логика работы пресса (как в 1.7.10)
        if (delay <= 0) {
            int stampSpeed = speed * PROGRESS_AT_MAX / MAX_SPEED;

            if (isRetracting) {
                press -= stampSpeed;
                if (press <= 0) {
                    press = 0;
                    isRetracting = false;
                    delay = 5;
                }
            } else if (canProcess) {
                press += stampSpeed;
                if (press >= MAX_PRESS) {
                    // Завершение операции
                    craftItem();
                    isRetracting = true;
                    delay = 5;

                    // ВАЖНО: вычитаем топливо только при успешной операции
                    if (burnTime >= FUEL_PER_OPERATION) {
                        burnTime -= FUEL_PER_OPERATION;
                    }
                    needsSync = true;
                }
            } else if (press > 0) {
                isRetracting = true;
            }
        } else {
            delay--;
        }

        // Обновляем позицию для рендера
        pressPosition = Math.min(20, (press * 20) / MAX_PRESS);

        if (press != previousPress
                || pressPosition != previousPressPosition
                || burnTime != previousBurnTime
                || speed != previousSpeed
                || isRetracting != previousRetracting) {
            needsSync = true;
        }

        if (needsSync) {
            setChanged();
            sendUpdateToClient();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void clientTick() {
        float target = convertPressToProgress();
        if (!clientPressInitialized) {
            visualPressPosition = target;
            prevVisualPressPosition = target;
            clientPressInitialized = true;
        }
        prevVisualPressPosition = visualPressPosition;
        visualPressPosition = Mth.lerp(0.25F, visualPressPosition, target);
    }

    @OnlyIn(Dist.CLIENT)
    public float getPressAnimationProgress(float partialTick) {
        float interpolated = Mth.lerp(partialTick, prevVisualPressPosition, visualPressPosition);
        return Mth.clamp(interpolated, 0.0F, 1.0F);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
            worldPosition.getX(),
            worldPosition.getY(),
            worldPosition.getZ(),
            worldPosition.getX() + 1,
            worldPosition.getY() + 3,
            worldPosition.getZ() + 1
        );
    }
    
    private void craftItem() {
        Optional<PressRecipe> recipe = getCurrentRecipe();
        if (recipe.isEmpty()) return;
        
        ItemStack output = recipe.get().getResultItem(level.registryAccess());
        
        // Вычитаем материал
        inventory.extractItem(MATERIAL_SLOT, 1, false);
        
        // Добавляем результат
        ItemStack outputSlot = inventory.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, output.copy());
        } else {
            outputSlot.grow(output.getCount());
        }
        
        // Изнашиваем штамп (если он изнашиваемый)
        ItemStack stamp = inventory.getStackInSlot(STAMP_SLOT);
        // Проверяем, что штамп не бесконечный (Desh штампы)
        if (stamp.isDamageableItem()) {
            stamp.setDamageValue(stamp.getDamageValue() + 1);
            
            // Если штамп сломался - удаляем его
            if (stamp.getDamageValue() >= stamp.getMaxDamage()) {
                inventory.setStackInSlot(STAMP_SLOT, ItemStack.EMPTY);
                
                // Звук ломающегося предмета
                level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK,
                    SoundSource.BLOCKS, 1.5f, 0.8f);
            }
        }
        
        // Воспроизведение звука работы пресса
        level.playSound(null, worldPosition, ModSounds.PRESS_OPERATE.get(),
            SoundSource.BLOCKS, 1.5f, 1.0f);
    }
    
    private boolean canProcess() {
        if (burnTime < FUEL_PER_OPERATION) return false;
        if (inventory.getStackInSlot(STAMP_SLOT).isEmpty() ||
            inventory.getStackInSlot(MATERIAL_SLOT).isEmpty()) return false;
        
        Optional<PressRecipe> recipe = getCurrentRecipe();
        if (recipe.isEmpty()) return false;
        
        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        ItemStack outputSlot = inventory.getStackInSlot(OUTPUT_SLOT);
        
        if (outputSlot.isEmpty()) return true;
        
        return outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize() &&
               outputSlot.is(result.getItem()) &&
               outputSlot.getDamageValue() == result.getDamageValue();
    }
    
    private Optional<PressRecipe> getCurrentRecipe() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        
        return level.getRecipeManager().getRecipeFor(PressRecipe.Type.INSTANCE, container, level);
    }
    
    // ==================== UTILITY ====================
    
    public int getBurnTime(Item item) {
        ItemStack stack = new ItemStack(item);
        // Получаем ванильное время горения в тиках
        int vanillaBurnTime = net.minecraftforge.common.ForgeHooks.getBurnTime(stack, null);
        if (vanillaBurnTime <= 0) return 0;
        
        // Конвертируем тики в секунды (делим на 20)
        return (vanillaBurnTime / 20);
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
    
    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, container);
        }
    }
    
    public ContainerData getBlockEntityData() {
        return this.data;
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); // ОБЯЗАТЕЛЬНО ПЕРВЫМ
        tag.putInt("press", press);
        tag.putInt("burnTime", burnTime);
        tag.putInt("speed", speed);
        tag.putBoolean("isRetracting", isRetracting);
        tag.putInt("delay", delay);
        tag.putInt("heatState", heatState);
        tag.putInt("pressPosition", pressPosition);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        press = tag.getInt("press");
        burnTime = tag.getInt("burnTime");
        speed = tag.getInt("speed");
        isRetracting = tag.getBoolean("isRetracting");
        delay = tag.getInt("delay");
        heatState = tag.getInt("heatState");
        pressPosition = tag.getInt("pressPosition");

        float progress = convertPressToProgress(); // press / (float) MAX_PRESS

        clientPressInitialized = true;
        visualPressPosition = progress;
        prevVisualPressPosition = progress;
    }


    public ItemStack getMaterialStack() {
        return inventory.getStackInSlot(MATERIAL_SLOT);
    }

    public ItemStack getStampStack() {
        return inventory.getStackInSlot(STAMP_SLOT);
    }

    private float convertPressToProgress() {
        return press / (float) MAX_PRESS;
    }
}
